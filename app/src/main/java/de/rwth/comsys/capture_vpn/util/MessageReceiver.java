package de.rwth.comsys.capture_vpn.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.CaptureService;

import java.io.File;

/**
 * triggers auto start of notification service and handles alarms
 */

public class MessageReceiver extends BroadcastReceiver
{
    private static final String TAG = "MessageReceiver";

    private static void startCapturing(Context context)
    {
        CaptureService cs = CaptureService.getCaptureService();
        if (cs == null)
        {
            if (!CaptureCentral.isMainHandlerInitialized())
            {
                CaptureCentral.initializeMainHandler(context);
            }

            Intent serviceIntent = new Intent(context, CaptureService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                context.startForegroundService(serviceIntent);
            }
            else
            {
                context.startService(serviceIntent);
            }
        }
    }

    private static void stopCapturing(Context context)
    {
        if (CaptureCentral.getInstance().isActive())
        {
            CaptureService cs = CaptureService.getCaptureService();
            if (cs != null)
            {
                //cs.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
                cs.closeCaptureInterface();
                context.stopService(new Intent(context, CaptureService.class));
            }
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean("pref_running", false);
            editor.apply();
        }
    }

    private static void logIssue(Context context)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean pcap = sharedPrefs.getBoolean("pref_debugging_pcap", CaptureConstants.PCAP_ENABLED);

        String filepath = CaptureConstants.getIssuePath();
        // Dump Log file
        CaptureLogger.dumpLogcat(filepath + ".log");

        if (CaptureCentral.getInstance().isActive())
        {
            CaptureService cs = CaptureService.getCaptureService();
            if (cs != null)
            {
                cs.closeCaptureInterface();
                context.stopService(new Intent(context, CaptureService.class));
            }

            // Save PCAP file
            if (pcap)
            {
                File from = new File(CaptureConstants.getPCAPPath());
                File to = new File(filepath + ".pcap");
                if (from.exists() && !from.isDirectory())
                    from.renameTo(to);
            }

            Intent intent = new Intent(context, MessageReceiver.class);
            intent.putExtra("action", "START");
            context.sendBroadcast(intent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "Received " + intent.toString());

        String action = intent.getAction();
        if (action != null && action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
        {
            Log.d(TAG, "Received ACTION_BOOT_COMPLETED intent");
            Intent serviceIntent = new Intent(context, NotificationService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                context.startForegroundService(serviceIntent);
            }
            else
            {
                context.startService(serviceIntent);
            }
            return;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        Bundle extras = intent.getExtras();
        if (extras == null)
        {
            Log.d(TAG, "No Extras");
            return;
        }
        action = extras.getString("action");
        if (action == null)
        {
            // trying fallback
            String fallback = sharedPrefs.getString("pref_action", null);
            if (fallback == null)
            {
                Log.d(TAG, "No Action found");
                return;
            }
            action = fallback;
        }

        if (action.equalsIgnoreCase("START"))
        {
            Log.d(TAG, "Received START intent");
            startCapturing(context);
            return;
        }

        if (action.equalsIgnoreCase("STOP"))
        {
            Log.d(TAG, "Received STOP intent");
            stopCapturing(context);
            return;
        }

        if (action.equalsIgnoreCase("ISSUE"))
        {
            Log.d(TAG, "Received ISSUE intent");
            logIssue(context);
            return;
        }

        Log.w(TAG, "Action not found");
    }
}