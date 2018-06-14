package de.rwth.comsys.capture_vpn.network.threads;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.network.IPPacket;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread that handles incoming packets
 * <p>
 * the respective forwarder was created previously
 */

public class TunInThread extends Thread
{

    private static final String TAG = "TunInThread";
    private FileOutputStream tunIn;
    private LinkedBlockingQueue<IPPacket> tunInQueue;
    private volatile boolean exit;
    private int sleepTime;

    public TunInThread(FileDescriptor tunFd)
    {
        super(TAG);
        this.tunIn = new FileOutputStream(tunFd);
        this.tunInQueue = CaptureCentral.getInstance().getTunInQueue();
        this.exit = false;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(CaptureCentral.getInstance().getContext());
        sleepTime = Integer.parseInt(sharedPrefs.getString("pref_tweaks_sleeptime", CaptureConstants.SLEEP_TIME));
    }

    public LinkedBlockingQueue<IPPacket> getTunInQueue()
    {
        return this.tunInQueue;
    }

    public FileOutputStream getTunIn()
    {
        return this.tunIn;
    }

    @Override
    public void run()
    {
        while (!exit)
        {
            // we received a new incoming packet and have to match it to a forwarder
            IPPacket packet = getTunInQueue().poll();
            if (packet != null)
            {
                CaptureCentral.addPacketToPacketQueue(packet);
                CaptureCentral.addPacketToPCAP(packet);
                try
                {
                    getTunIn().getChannel().write(packet.getRawPacket().position(0).getByteBuffer());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    break;
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
                    Log.w(TAG, "interrupted");
                    break;
                }
            }
        }
        try
        {
            getTunIn().close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Log.i(TAG, "shut down");
    }

    public void exit()
    {
        this.exit = true;
    }
}
