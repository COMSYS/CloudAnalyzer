package de.rwth.comsys.capture_vpn.network.threads;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.network.Forwarder;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import static de.rwth.comsys.capture_vpn.util.CaptureConstants.DEBUG;

/**
 * Thread that notifies about IO interaction
 */

public class IONotificationThread extends Thread
{
    private static final String TAG = "IONotificationThread";
    private volatile boolean exit;
    private Selector selector;
    private LinkedBlockingQueue<RegisteredTask> registeredTaskQueue;
    private int sleepTime;

    public IONotificationThread() throws IOException
    {
        super(TAG);
        this.exit = false;
        this.selector = Selector.open();
        this.registeredTaskQueue = new LinkedBlockingQueue<>();
        this.setDaemon(true);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(CaptureCentral.getInstance().getContext());
        sleepTime = Integer.parseInt(sharedPrefs.getString("pref_tweaks_sleeptime", CaptureConstants.SLEEP_TIME));
    }

    public void registerForwarder(Forwarder forwarder, int operations)
    {
        registeredTaskQueue.add(new RegisteredTask(forwarder, operations));
    }

    private SelectionKey registerChannel(RegisteredTask task)
    {
        SelectionKey key = null;
        try
        {
            key = task.forwarder.getChannel().register(selector, task.operations, task.forwarder);
        }
        catch (ClosedChannelException e)
        {
            if (DEBUG)
                Log.w(TAG, "Should register channel that is already closed.");
        }
        return key;
    }

    private void registerChannels() throws InterruptedException
    {
        SelectionKey key = null;
        RegisteredTask task;
        // wait in case of:
        // a) selector has no keys registered AND
        // b) task queue is empty
        while (selector.keys().isEmpty() && key == null && !exit)
        {
            task = registeredTaskQueue.take();
            // task == null means that we want to exit thread
            if (task.forwarder == null)
                break;
            key = registerChannel(task);
        }

        while ((task = registeredTaskQueue.poll()) != null)
        {
            registerChannel(task);
        }
    }

    private int selectNow()
    {
        try
        {
            return selector.selectNow();
        }
        catch (IOException | CancelledKeyException e)
        {
            Log.w(TAG, e.toString());
        }
        return 0;
    }

    @Override
    public void run()
    {
        try
        {
            while (!exit)
            {
                registerChannels();

                int count = selectNow();
                if (DEBUG)
                {
                    if (count > 0 || selector.selectedKeys().size() > 0)
                        Log.d(TAG, "selectNow() updated " + count + " keys, selected-keys set size: " + selector.selectedKeys().size());
                }

                if (!selector.selectedKeys().isEmpty())
                {
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext())
                    {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isValid())
                        {
                            key.cancel();
                            Forwarder forwarder = (Forwarder) key.attachment();
                            forwarder.registerForUpdate();
                        }
                    }
                }
                else
                {
                    Thread.sleep(sleepTime);
                }
                // Process canceled-keys set
                selectNow();
            }
        }
        catch (InterruptedException e)
        {
            Log.e(TAG, e.toString());
        }
    }

    public void exit()
    {
        this.exit = true;
        registeredTaskQueue.offer(new RegisteredTask(null, 0));
        selector.wakeup();
    }

    private static class RegisteredTask
    {
        private final Forwarder forwarder;
        private final int operations;

        private RegisteredTask(Forwarder forwarder, int operations)
        {
            this.forwarder = forwarder;
            this.operations = operations;
        }
    }
}
