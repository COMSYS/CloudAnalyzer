package de.rwth.comsys.capture_vpn.util;

import android.os.Environment;
import de.rwth.comsys.capture_vpn.network.IPPacket;
import de.rwth.comsys.cloudanalyzer.BuildConfig;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * maintain default config of VPN-specific values
 */

public final class CaptureConstants
{
    public static final short[] TUN_ADDR = {10, 0, 0, 1};
    public static final String TUN_ADDR_STRING = IPPacket.ipToString(TUN_ADDR);
    public static final String DNS_STRING = "8.8.8.8";

    public static final int USABLE_FORWARDER = (int) Math.round(1024 * 0.8); // Maximum number is 1024, we add a safety threshold for other processes
    public static final int TCP_FORWARDER = 66; // in %
    public static final int UDP_FORWARDER = 100 - TCP_FORWARDER; // in %
    public static final long TCP_BUFFER = 5 * 60000; // in ms
    public static final long UDP_BUFFER = 5 * 60000; // in ms
    public static final int UPDATE_THREADS = Math.max(1, (int) (Runtime.getRuntime().availableProcessors() * 0.5));
    public static final String SLEEP_TIME = "50";

    public static final boolean DEBUG = false; //BuildConfig.DEBUG;
    public static final boolean EVAL_MODE = true;

    public static final int LATEST_UPLOAD = 10;

    public static final boolean DEBUG_TCP = false;
    public static final boolean LOG_ENABLED = false;
    public static final String LOG_LEVEL = "V"; // one of the following: V, D, I, W, E, F
    public static final boolean PCAP_ENABLED = false;
    public static final Set<String> DEBUG_APPS = new HashSet<>(Arrays.asList("de.rwth.comsys.cloudanalyzer", "Unknown.Application", "Unknown.Port", "Unsupported.Protocol"));
    public static final Set<String> LOG_COMPONENTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("CaptureCentral", "CaptureService", "CaptureLogger", "NetworkMonitor", "IONotificationThread", "TunOutThread", "TunInThread", "Forwarder", "ForwarderManager", "TCPForwarder", "UDPForwarder")));
    private static final String DEBUG_DIR = Environment.getExternalStorageDirectory() + "/Download/" + BuildConfig.APPLICATION_ID;
    private static final String LOG_FILE = "output_" + System.currentTimeMillis() + ".log";
    private static final String PCAP_FILE = BuildConfig.APPLICATION_ID + ".pcap";
    private static final String SQLITE_FILE = "caDb";

    private static void _createDir(String path)
    {
        File dir = new File(path);
        if (!dir.exists())
        {
            dir.mkdirs();
        }
    }

    private static String _getPath(String path, String file)
    {
        _createDir(path);
        return new File(path, file).toString();
    }

    public static void createDebugDir()
    {
        _createDir(DEBUG_DIR);
    }

    public static String getLogPath()
    {
        return _getPath(CaptureConstants.DEBUG_DIR, CaptureConstants.LOG_FILE);
    }

    public static String getIssuePath()
    {
        return _getPath(CaptureConstants.DEBUG_DIR, "issue_" + System.currentTimeMillis());
    }

    public static String getPCAPPath()
    {
        return _getPath(CaptureConstants.DEBUG_DIR, CaptureConstants.PCAP_FILE);
    }

    public static String getDatabasePath()
    {
        return _getPath(CaptureConstants.DEBUG_DIR, CaptureConstants.SQLITE_FILE);
    }


    public static String getLogComponents(Set<String> components, String logLevel)
    {
        if (!Arrays.asList("V", "D", "I", "I", "W", "E", "F").contains(logLevel))
            logLevel = LOG_LEVEL;

        StringBuilder sB = new StringBuilder();

        sB.append(" ");
        for (String comp : components)
        {
            if (comp.contains(":"))
            {
                sB.append(comp);
            }
            else
            {
                sB.append(comp).append(":").append(logLevel).append(" ");
            }
        }

        return sB.toString();
    }
}
