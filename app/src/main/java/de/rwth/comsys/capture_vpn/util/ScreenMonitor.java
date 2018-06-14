package de.rwth.comsys.capture_vpn.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.rwth.comsys.capture_vpn.CaptureCentral;

/**
 * handles intents about the device's state
 * <p>
 * The ACTION_SCREEN_OFF and ACTION_SCREEN_ON intents are not stored in the manifest!
 */

public class ScreenMonitor extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action == null)
        {
            return;
        }

        if (action.equals(Intent.ACTION_SCREEN_OFF))
        {
            CaptureCentral.setScreenOn(false);
        }
        if (action.equals(Intent.ACTION_SCREEN_ON))
        {
            CaptureCentral.setScreenOn(true);
        }
    }
}
