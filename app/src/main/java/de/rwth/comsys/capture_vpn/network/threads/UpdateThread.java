package de.rwth.comsys.capture_vpn.network.threads;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.network.Forwarder;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;

import java.io.IOException;
import java.util.Queue;

import static de.rwth.comsys.capture_vpn.util.CaptureConstants.DEBUG;

/**
 * Thread(s) that process forwarders
 */

public class UpdateThread extends Thread
{
    private final String TAG;
    private Queue<Forwarder> queue;
    private volatile boolean exit;
    private int sleepTime;

    public UpdateThread(Queue<Forwarder> queue, String TAG, Context context)
    {
        super(TAG);
        this.queue = queue;
        this.setDaemon(true);
        this.TAG = TAG;
        this.exit = false;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sleepTime = Integer.parseInt(sharedPrefs.getString("pref_tweaks_sleeptime", CaptureConstants.SLEEP_TIME));
    }

    @Override
    public void run()
    {
        if (queue == null)
            return;

        while (!exit)
        {
            Forwarder forwarder = queue.poll();
            if (forwarder != null)
            {
                if (!forwarder.isClosed())
                {
                    try
                    {
                        if (!forwarder.update())
                        {
                            // Another thread is updating this forwarder.
                            // We re-enqueue the forwarder as some packets might not be processed by the other thread.
                            // waitingForUpdate is still set since we could not acquire the lock and the other thread will not set it to false
                            // (the other thread must have called setWaitingForUpdate(false) already since the forwarder was in the update queue).
                            queue.offer(forwarder);
                        }
                    }
                    catch (IOException e)
                    {
                        if (DEBUG)
                            Log.w(TAG, e.toString());
                    }
                    // update last access
                    CaptureCentral.getInstance().accessedForwarder(forwarder);
                }
            }
            else
            {
                try
                {
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    exit = true;
                }
            }
        }
    }

    public void exit()
    {
        exit = true;
    }
}
