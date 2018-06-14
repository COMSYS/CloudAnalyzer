package de.rwth.comsys.capture_vpn.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;

import java.io.IOException;
import java.util.Set;

/**
 * support to log output to text file
 */

public class CaptureLogger
{
    private static final String TAG = "CaptureLogger";

    public static Process dumpLogcat(String file)
    {
        Process process;
        String cmd = "logcat -d -f " + file + " *:V\n";
        try
        {
            process = Runtime.getRuntime().exec(cmd);
            Log.i(TAG, "Created logging process");
        }
        catch (IOException e)
        {
            process = null;
            Log.w(TAG, "Could not launch logging");
        }
        return process;
    }

    public static Process launchLogcat(String file)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(CaptureCentral.getInstance().getContext());
        Set<String> logFilters = sharedPrefs.getStringSet("pref_debugging_log_level", CaptureConstants.LOG_COMPONENTS);
        String logLevel = sharedPrefs.getString("pref_debugging_level", CaptureConstants.LOG_LEVEL);
        String logString = CaptureConstants.getLogComponents(logFilters, logLevel.toUpperCase());

        Process process;
        // *:S results in filtering of all unrelated messages (!)
        // this might filter too much
        String cmd = "logcat -f " + file + logString + "*:S\n";
        Log.v(TAG, "Logging:" + logString + "to " + file);
        try
        {
            process = Runtime.getRuntime().exec(cmd);
            Log.i(TAG, "Created Logging process");
        }
        catch (IOException e)
        {
            process = null;
            Log.w(TAG, "Cloud not launch Logging");
        }
        return process;
    }

    public static void clearLogcat()
    {
        try
        {
            Runtime.getRuntime().exec("logcat -c");
            Log.d(TAG, "Cleared Logcat output");
        }
        catch (IOException e)
        {
            Log.i(TAG, "Could not clear Logcat");
        }
    }
}
