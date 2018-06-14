package de.rwth.comsys.capture_vpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.network.Forwarder;
import de.rwth.comsys.capture_vpn.network.ForwarderManager;
import de.rwth.comsys.capture_vpn.network.IPPacket;
import de.rwth.comsys.capture_vpn.network.TransportLayerPacket;
import de.rwth.comsys.capture_vpn.network.threads.IONotificationThread;
import de.rwth.comsys.capture_vpn.network.threads.UpdateThread;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;
import de.rwth.comsys.capture_vpn.util.PCAPFileWriter;
import de.rwth.comsys.cloudanalyzer.MainHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class that handles threads (including CloudAnalyzer analysis service)
 */

public class CaptureCentral
{
    private static final String TAG = "CaptureCentral";

    private static boolean mainHandlerInitialized = false;
    private static boolean threadsInitialized = false;
    private static CaptureCentral captureCentral = new CaptureCentral();
    private boolean active = false;

    private boolean upload = false;
    private boolean screenOn;
    private int networkType;
    private Context context;
    private SharedPreferences mSharedPref;
    private LinkedBlockingQueue<IPPacket> packetQueue;
    private LinkedBlockingQueue<IPPacket> tunInQueue;
    private Object syncPacketSource;
    private List<UpdateThread> updateThreads;
    private Queue<Forwarder> updateQueue;
    private IONotificationThread ioNotificationThread;
    private ForwarderManager forwarderManager;
    private ConcurrentHashMap<IPPacket.IPKey, String> appNameManager;
    private PCAPFileWriter pcapWriter;


    private CaptureCentral()
    {
        appNameManager = new ConcurrentHashMap<>();
        tunInQueue = new LinkedBlockingQueue<>();
        packetQueue = new LinkedBlockingQueue<>();
        syncPacketSource = new Object();

        updateQueue = new ConcurrentLinkedQueue<>();
    }

    public static CaptureCentral getInstance(Context context)
    {
        captureCentral.context = context;
        return captureCentral;
    }

    public static CaptureCentral getInstance()
    {
        return captureCentral;
    }

    public static void shutdownMainHandler()
    {
        CaptureService cs = CaptureService.getCaptureService();
        while (cs != null)
        {
            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            cs = CaptureService.getCaptureService();
        }
        MainHandler.shutdown();
        MainHandler.deleteSingleton();
        mainHandlerInitialized = false;
    }

    public static Object getSyncPacketSource()
    {
        return captureCentral.syncPacketSource;
    }

    public static boolean isScreenOn()
    {
        return getInstance().screenOn;
    }

    public static void setScreenOn(boolean screenOn)
    {
        getInstance().screenOn = screenOn;
    }

    public static int getNetworkType()
    {
        return getInstance().networkType;
    }

    public static void setNetworkType(int networkType)
    {
        getInstance().networkType = networkType;
    }

    public static void addPacketToPCAP(IPPacket ipPacket)
    {
        captureCentral._addPacketToPCAP(ipPacket);
    }

    public static void addPacketToPacketQueue(IPPacket packet)
    {
        captureCentral._addPacket(packet);
        synchronized (getSyncPacketSource())
        {
            getSyncPacketSource().notifyAll();
        }
    }

    public static void onCapturingStart()
    {
        captureCentral.active = true;

    }

    public static void onCapturingStop()
    {
        captureCentral.active = false;

        captureCentral.forwarderManager.resetForwarderManager();
        captureCentral.appNameManager.clear();
        captureCentral.getTunInQueue().clear();

    }

    public static void initializeMainHandler(Context context)
    {
        CaptureCentral.getInstance().context = context.getApplicationContext();
        if (!isMainHandlerInitialized())
        {
            // Bootstrap CloudAnalyzer
            Log.i(TAG, "----------------------------------");
            Log.i(TAG, "Cloud Analyzer");
            Log.i(TAG, "----------------------------------");
            Log.i(TAG, "Initializing... ");

            if (MainHandler.init(context))
            {
                mainHandlerInitialized = true;
                Log.i(TAG, "done.");
            }
        }
        if (!areThreadsInitialized())
            CaptureCentral.getInstance().initializeThreads();
    }

    public static boolean isMainHandlerInitialized()
    {
        return mainHandlerInitialized;
    }

    public static boolean areThreadsInitialized()
    {
        return threadsInitialized;
    }

    private void initializeThreads()
    {
        forwarderManager = new ForwarderManager();

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int threadCount = Integer.valueOf(mSharedPref.getString("pref_tweaks_threads", String.valueOf(CaptureConstants.UPDATE_THREADS)));
        if (threadCount == 0 || threadCount > Runtime.getRuntime().availableProcessors())
            threadCount = CaptureConstants.UPDATE_THREADS;
        updateThreads = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; ++i)
        {
            UpdateThread updateThread = new UpdateThread(updateQueue, "UpdateThread-" + i, context);
            updateThread.start();
            updateThreads.add(updateThread);
        }

        try
        {
            ioNotificationThread = new IONotificationThread();
            ioNotificationThread.start();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Could not create IONotificationThread:\n" + e);
        }
        threadsInitialized = true;
    }

    public boolean isActive()
    {
        return captureCentral.active;
    }

    public Context getContext()
    {
        return context;
    }

    public void addForwarder(Forwarder forwarder, String appName)
    {
        forwarderManager.addForwarder(forwarder);
        appNameManager.put(forwarder.getKey(), appName);

        MainHandler.addApp(appName);
    }

    public void registerForwarder(Forwarder forwarder, int operations)
    {
        ioNotificationThread.registerForwarder(forwarder, operations);
    }

    public void addForwarderToUpdateQueue(Forwarder forwarder)
    {
        updateQueue.offer(forwarder);
    }

    public void accessedForwarder(Forwarder forwarder)
    {
        forwarderManager.accessedForwarder(forwarder);
    }

    public long getTcp_forwarders()
    {
        return forwarderManager.getNumberOfForwarder(TransportLayerPacket.Protocol.TCP);
    }

    public long getUdp_forwarders()
    {
        return forwarderManager.getNumberOfForwarder(TransportLayerPacket.Protocol.UDP);
    }

    public long getData_tcp_incoming()
    {
        return forwarderManager.getData_incoming(TransportLayerPacket.Protocol.TCP);
    }

    public long getData_tcp_outgoing()
    {
        return forwarderManager.getData_outgoing(TransportLayerPacket.Protocol.TCP);
    }

    public long getData_udp_incoming()
    {
        return forwarderManager.getData_incoming(TransportLayerPacket.Protocol.UDP);
    }

    public long getData_udp_outgoing()
    {
        return forwarderManager.getData_outgoing(TransportLayerPacket.Protocol.UDP);
    }

    public long getPackets_tcp_incoming()
    {
        return forwarderManager.getPackets_incoming(TransportLayerPacket.Protocol.TCP);
    }

    public long getPackets_tcp_outgoing()
    {
        return forwarderManager.getPackets_outgoing(TransportLayerPacket.Protocol.TCP);
    }

    public long getPackets_udp_incoming()
    {
        return forwarderManager.getPackets_incoming(TransportLayerPacket.Protocol.UDP);
    }

    public long getPackets_udp_outgoing()
    {
        return forwarderManager.getPackets_outgoing(TransportLayerPacket.Protocol.UDP);
    }

    public LinkedBlockingQueue<IPPacket> getTunInQueue()
    {
        return tunInQueue;
    }

    public LinkedBlockingQueue<IPPacket> getPacketQueue()
    {
        return packetQueue;
    }

    public ConcurrentHashMap<IPPacket.IPKey, String> getAppNameManager()
    {
        return appNameManager;
    }

    public ForwarderManager getForwarderManager()
    {
        return forwarderManager;
    }


    public void disablePcapWriter()
    {
        pcapWriter = null;
    }

    public void initializePcapWriter()
    {
        if (pcapWriter != null)
        {
            pcapWriter.close();
            pcapWriter = null;
        }
        if (mSharedPref == null)
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if (mSharedPref.getBoolean("pref_debugging_output", false) && mSharedPref.getBoolean("pref_debugging_pcap", CaptureConstants.PCAP_ENABLED))
        {
            CaptureConstants.createDebugDir();
            pcapWriter = new PCAPFileWriter(CaptureConstants.getPCAPPath());
        }
    }

    public PCAPFileWriter getPcapWriter()
    {
        return pcapWriter;
    }

    private void _addPacket(IPPacket packet)
    {
        try
        {
            packetQueue.put(packet);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        // Data Aggregation
        forwarderManager.addPacketInformation(packet);
    }

    private void _addPacketToPCAP(IPPacket ipPacket)
    {
        if (pcapWriter != null)
            pcapWriter.addPacket(ipPacket, !mSharedPref.getBoolean("pref_debugging_pcap_header", false));
    }

    public void resetForwarder()
    {
        forwarderManager.resetForwarderManager();
    }

    public void lock()
    {
        upload = true;
    }

    public void unlock()
    {
        upload = false;
    }

    public boolean isLocked()
    {
        return upload;
    }
}