package de.rwth.comsys.capture_vpn.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.network.TransportLayerPacket;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * match an app name to a given port
 */

public class AppLookup
{
    private static final String TAG = "AppLookup";

    private static String lookupInternUid(int uid)
    {
        switch (uid)
        {
            case 0:
                return "android.uid.root:0";

            case 1000:
                return "android.uid.system:1000";

            case 1001:
                return "android.uid.radio:1001";

            case 1002:
                return "android.uid.bluetooth:1002";

            case 1003:
                return "android.uid.graphics:1003";

            case 1004:
                return "android.uid.input:1004";

            case 1005:
                return "android.uid.audio:1005";

            case 1006:
                return "android.uid.camera:1006";

            case 1007:
                return "android.uid.log:1007";

            case 1008:
                return "android.uid.compass:1008";

            case 1009:
                return "android.uid.mountd:1009";

            case 1010:
                return "android.uid.wifi:1010";

            case 1011:
                return "android.uid.adb:1011";

            case 1012:
                return "android.uid.install:1012";

            case 1013:
                return "android.uid.media:1013";

            case 1014:
                return "android.uid.dhcp:1014";

            case 2000:
                return "android.uid.shell:2000";

            case 2001:
                return "android.uid.cache:2001";

            case 2002:
                return "android.uid.diag:2002";

            case 3001:
                return "android.uid.net_bt_admin:3001";

            case 3002:
                return "android.uid.net_bt:3002";

            case 3003:
                return "android.uid.inet:3003";

            case 3004:
                return "android.uid.net_raw:3004";

            case 9998:
                return "android.uid.misc:9998";

            case 9999:
                return "android.uid.nobody:9999";

            default:
                return "Unknown.Uid:" + uid;
        }
    }

    public static String getAppNameByPort(Context context, int port, TransportLayerPacket.Protocol prot)
    {
        String appName = "Unsupported.Protocol";
        String line = null;

        try
        {
            FileInputStream in;
            switch (prot)
            {
                case TCP:
                    in = new FileInputStream("/proc/net/tcp6");
                    break;
                case UDP:
                    in = new FileInputStream("/proc/net/udp6");
                    break;
                default:
                    return appName;
            }
            Scanner scanner = new Scanner(in);

            line = scanner.findWithinHorizon(Pattern.compile("^.*:" + Integer.toHexString(0x10000 | port).toUpperCase().substring(1) + ".*", Pattern.MULTILINE), 0);

            if (line == null)
            {
                switch (prot)
                {
                    case TCP:
                        in = new FileInputStream("/proc/net/tcp");
                        break;
                    case UDP:
                        in = new FileInputStream("/proc/net/udp");
                        break;
                    default:
                        // should never reach this
                        return appName;
                }
                scanner = new Scanner(in);

                line = scanner.findWithinHorizon(Pattern.compile("^.*:" + Integer.toHexString(0x10000 | port).toUpperCase().substring(1) + ".*", Pattern.MULTILINE), 0);
            }

            if (line == null)
            {
                Log.e(TAG, "Port " + port + " (" + prot.name() + ") not found!");
                return "Unknown.Port";
            }

            String[] attr = line.replaceAll("\\s+", " ").trim().split(" ");

            appName = context.getPackageManager().getNameForUid(Integer.valueOf(attr[7]));
            if (appName == null)
                appName = lookupInternUid(Integer.valueOf(attr[7]));
        }
        catch (NullPointerException | IOException e)
        {
            Log.e(TAG, line);
            e.printStackTrace();
        }

        appName = appName != null ? appName : "Unknown.Uid";
        appName = appName.contains(":") ? appName.substring(0, appName.indexOf(":")) : appName;
        //Log.i(TAG, appName + " on port " + port);

        // replace app name if necessary
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> checkedApps = sharedPrefs.getStringSet("pref_db_filter_apps", new HashSet<String>());
        if (checkedApps.contains(appName))
            appName = "Unknown.Application";

        return appName;
    }
}
