package de.rwth.comsys.capture_vpn;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.network.TransportLayerPacket;
import de.rwth.comsys.capture_vpn.network.threads.TunInThread;
import de.rwth.comsys.capture_vpn.network.threads.TunOutThread;
import de.rwth.comsys.capture_vpn.util.*;
import de.rwth.comsys.cloudanalyzer.AnalysisService;
import de.rwth.comsys.cloudanalyzer.R;

import java.io.IOException;
import java.lang.Process;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

/**
 * Service that manages VPN connection
 */

public class CaptureService extends VpnService implements ComponentCallbacks2
{
    private static final String TAG = "CaptureService";
    private static CaptureService captureService;

    Messenger mService = null;
    boolean mIsBound;
    private NetworkMonitor networkMonitor;
    private ScreenMonitor screenMonitor;
    private CaptureCentral captureCentral;
    private ParcelFileDescriptor captureInterface;
    private Process logProcess;
    private TunOutThread tunOutThread;
    private TunInThread tunInThread;
    private boolean storagePermission = false;
    private SharedPreferences mSharedPref;

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {

            mService = new Messenger(service);

            // register client (vpn service active)
            try
            {
                Message msg = Message.obtain(null, NotificationService.MSG_REGISTER_CLIENT);
                mService.send(msg);
            }
            catch (RemoteException e)
            {
                // do nothing
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            mService = null;
        }
    };

    public CaptureService()
    {
        captureService = this;
        captureCentral = CaptureCentral.getInstance();
    }

    public static CaptureService getCaptureService()
    {
        return captureService;
    }

    public static boolean protect(DatagramChannel channel)
    {
        return captureService.protect(channel.socket());
    }

    public static boolean protect(SocketChannel channel)
    {
        return captureService.protect(channel.socket());
    }

    public void closeCaptureInterface()
    {
        tunOutThread.exit();
        tunInThread.exit();
        try
        {
            captureInterface.close();
        }
        catch (IOException e)
        {
            Log.d(TAG, "Could not close captureInterface");
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(captureCentral.getContext());

        if (captureCentral.isLocked())
        {
            Log.e(TAG, "CaptureCentral locked");
            stopSelf();
            return START_NOT_STICKY;
        }

        String sharedTunAddr = mSharedPref.getString("pref_tweaks_tunipv4", CaptureConstants.TUN_ADDR_STRING);
        String defaultDNS = CaptureConstants.DNS_STRING;
        String additionalDNS = mSharedPref.getString("pref_tweaks_dnsipv4", CaptureConstants.DNS_STRING);
        Boolean extraDNS = mSharedPref.getBoolean("pref_tweaks_dnsextra", false);

        PackageManager pm = captureCentral.getContext().getPackageManager();
        if ((pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, captureCentral.getContext().getPackageName()) == PackageManager.PERMISSION_GRANTED) && (pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, captureCentral.getContext().getPackageName()) == PackageManager.PERMISSION_GRANTED))
        {
            storagePermission = true;
            captureCentral.initializePcapWriter();
        }
        else
        {
            captureCentral.disablePcapWriter();
        }

        if (mSharedPref.getBoolean("pref_debugging_output", CaptureConstants.LOG_ENABLED) && storagePermission)
        {
            //clearLogcat();

            CaptureConstants.createDebugDir();
            logProcess = CaptureLogger.launchLogcat(CaptureConstants.getLogPath());
        }

        //configuring interface + establish VPN
        Builder builder = new Builder();
        builder.setSession("Capturing VPN");
        builder.addRoute("0.0.0.0", 0);
        if (extraDNS)
        {
            builder.addDnsServer(defaultDNS);
        }
        builder.addDnsServer(additionalDNS);
        builder.addAddress(sharedTunAddr, 24);
        captureInterface = builder.establish();

        if (captureInterface == null)
        {
            Toast.makeText(getApplicationContext(), R.string.vpn_permission, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "VPN Permission missing");
            stopSelf();
            return START_NOT_STICKY;
        }

        //Initialize and start thread for capturing
        Log.i(TAG, "Starting");
        tunOutThread = new TunOutThread(captureInterface.getFileDescriptor());
        tunOutThread.start();
        tunInThread = new TunInThread(captureInterface.getFileDescriptor());
        tunInThread.start();

        startService(new Intent(this, AnalysisService.class));
        CaptureCentral.onCapturingStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkMonitor = new NetworkMonitor();
        registerReceiver(this.networkMonitor, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenMonitor = new ScreenMonitor();
        registerReceiver(this.screenMonitor, filter);

        doBindService();

        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean("pref_running", true);
        editor.apply();

        startForeground(1, new Notification());

        return START_NOT_STICKY;
    }

    @Override
    public void onRevoke()
    {
        stopThreads();
        if (captureCentral.getPcapWriter() != null)
            captureCentral.getPcapWriter().close();

        unregisterReceiver(this.networkMonitor);
        unregisterReceiver(this.screenMonitor);

        houseKeeping();
        this.stopSelf();

        Log.i(TAG, "VPN revoked");
    }

    @Override
    public void onDestroy()
    {
        try
        {
            unregisterReceiver(this.networkMonitor);
            unregisterReceiver(this.screenMonitor);
        }
        catch (IllegalArgumentException ex)
        {
            Log.v(TAG, "Capture Service already revoked?");
        }

        houseKeeping();
        Log.i(TAG, "Capture Service destroyed");

        if (mSharedPref.getBoolean("pref_notification_toasts", false))
            Toast.makeText(getApplicationContext(), getString(R.string.inactive) + "\n" + getString(R.string.inactive_content), Toast.LENGTH_SHORT).show();

        if (mSharedPref.getBoolean("pref_debugging_output", CaptureConstants.LOG_ENABLED) && storagePermission && logProcess != null)
        {
            logProcess.destroy();
            CaptureLogger.clearLogcat();
        }

        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean("pref_running", false);
        editor.apply();
    }

    @Override
    public boolean protect(DatagramSocket socket)
    {
        return super.protect(socket);
    }

    @Override
    public boolean protect(Socket socket)
    {
        return super.protect(socket);
    }

    private void stopThreads()
    {
        tunOutThread.exit();
        tunInThread.exit();

        captureCentral.getForwarderManager().resetForwarderManager();
        captureCentral.getTunInQueue().clear();

        tunOutThread = null;
        tunInThread = null;
    }

    private void houseKeeping()
    {
        CaptureCentral.onCapturingStop();
        stopService(new Intent(this, AnalysisService.class));
        doUnbindService();

        captureService = null;
    }

    public void onTrimMemory(int level)
    {

        // Determine which lifecycle or system event was raised.
        switch (level)
        {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:

                break;


            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:

                Log.w(TAG, "cleaned package queue due to low memory");

                // clean queues
                captureCentral.getPacketQueue().clear();

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

                Log.w(TAG, "cleaned forwarder due to low memory");

                // try to clean forwarder
                captureCentral.getForwarderManager().cleanUpExistingForwarder(TransportLayerPacket.Protocol.TCP, 60000);
                captureCentral.getForwarderManager().cleanUpExistingForwarder(TransportLayerPacket.Protocol.UDP, 5000);

                break;

            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:

                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

                // register alarm for database upload
                AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                Intent intent = new Intent(this, MessageReceiver.class);
                intent.putExtra("action", "START");
                PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                if (alarmMgr != null)
                {
                    // Trigger alarm in 90 seconds
                    alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 90 * 1000, alarmIntent);
                    Log.d(TAG, "registered alarm to restart capturing in 90 seconds");
                }

                Log.e(TAG, "stopped VPN service due to critical memory");
                // stop service, before we get killed forcefully
                closeCaptureInterface();
                CaptureCentral.shutdownMainHandler();
                stopSelf();

                break;

            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }


    void doBindService()
    {
        bindService(new Intent(this, NotificationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService()
    {
        if (mIsBound)
        {
            // unregister client (vpn service inactive)
            if (mService != null)
            {
                try
                {
                    Message msg = Message.obtain(null, NotificationService.MSG_UNREGISTER_CLIENT);
                    mService.send(msg);
                }
                catch (RemoteException e)
                {
                    // do nothing
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
}
