package de.rwth.comsys.capture_vpn.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.CaptureCentral;

/**
 * reacts towards network changes
 */

public class NetworkMonitor extends BroadcastReceiver
{
    private static final String TAG = "NetworkMonitor";

    private ConnectivityManager connMgr;
    private CaptureCentral captureCentral;
    private int connType;
    private NetworkInfo.State connState;
    private String connExtraInfo;
    private SharedPreferences mSharedPref;

    public NetworkMonitor()
    {
        captureCentral = CaptureCentral.getInstance();
        connMgr = (ConnectivityManager) captureCentral.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(CaptureCentral.getInstance().getContext());
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action == null)
        {
            return;
        }

        if (action.equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE"))
        {
            boolean restartVPN = false;
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null)
            {
                int networkType = networkInfo.getType();
                CaptureCentral.setNetworkType(networkType);
                NetworkInfo.State networkState = networkInfo.getState();
                String networkInfoExtraInfo = networkInfo.getExtraInfo();
                if (networkInfoExtraInfo == null)
                    networkInfoExtraInfo = "";

                if (networkType != connType)
                {
                    // network type changed
                    Log.i(TAG, "type of network changed");
                    restartVPN = true;
                }
                if (networkState != connState)
                {
                    // network state changed
                    Log.i(TAG, "network state changed to " + networkState);
                    //if (networkState == NetworkInfo.State.CONNECTED)
                    {
                        restartVPN = true;
                    }

                }
                if (!networkInfoExtraInfo.equalsIgnoreCase(connExtraInfo))
                {
                    // network extra info changed
                    Log.v(TAG, "network extra info changed to " + networkInfoExtraInfo);
                    restartVPN = true;
                }


                if (mSharedPref.getBoolean("pref_notification_toasts", false))
                {
                    Toast.makeText(context, "Network State: " + networkState + "\n" + "Network Extra Info: " + networkInfoExtraInfo
                            //+ "\n" + "RestartVPN: " + restartVPN
                            , Toast.LENGTH_SHORT).show();
                }

                // update information
                connType = networkType;
                connState = networkState;
                connExtraInfo = networkInfoExtraInfo;
            }
            else
            {
                CaptureCentral.setNetworkType(ConnectivityManager.TYPE_DUMMY);
            }
            if (restartVPN || networkInfo == null)
            {
                captureCentral.resetForwarder();
                Log.i(TAG, "ForwarderMap cleared");
            }

        }
    }
}
