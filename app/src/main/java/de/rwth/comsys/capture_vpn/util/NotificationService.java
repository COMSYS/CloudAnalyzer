package de.rwth.comsys.capture_vpn.util;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.SplashActivity;
import de.rwth.comsys.cloudanalyzer.util.PropertyReader;

import java.io.IOException;

/**
 * updates notification bar
 */

public class NotificationService extends Service
{
    public static final String CHANNEL_ID = "CloudAnalyzer";
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    private static final int mNotificationID = 1;
    private static final int mNotificationIDwarning = 3;
    private static NotificationManager mNM;
    private static SharedPreferences mSharedPref;
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    private String enableDebugging;


    @Override
    public void onCreate()
    {
        try
        {
            enableDebugging = PropertyReader.getProperty(MainHandler.getPropFile(), "ca.enableDebugging", getApplicationContext());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(false);

            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        // Display a notification about us starting.
        startForeground(mNotificationID, createActiveNotification(false)); //showInactiveNotification();

        super.onCreate();
        //startForeground(1, new Notification());
    }

    @Override
    public void onDestroy()
    {
        // Cancel the persistent notification.
        mNM.cancel(mNotificationID);

        // Tell the user we stopped.
        //Toast.makeText(this, "Notification service stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mMessenger.getBinder();
    }

    private Notification createWarningNotification()
    {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, SplashActivity.class), 0);

        String title = getString(R.string.inactive);
        String text = getString(R.string.inactive_content);


        // Set the info for the views that show in the notification panel.
        NotificationCompat.Builder nB = new NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_attention).setWhen(System.currentTimeMillis()).setContentTitle(title).setContentText(text).setContentIntent(contentIntent);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
        {
            PendingIntent startIntent = PendingIntent.getBroadcast(this, 1, new Intent(this, MessageReceiver.class).putExtra("action", "START"), 0);
            nB.addAction(R.drawable.ic_visibility, getString(R.string.start), startIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            nB.setPriority(Notification.PRIORITY_HIGH).setCategory(Notification.CATEGORY_STATUS).setVisibility(Notification.VISIBILITY_SECRET).setLocalOnly(true).setNumber(1);
        }
        return nB.build();
    }


    private Notification createActiveNotification(Boolean active)
    {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, SplashActivity.class), 0);

        String title = getString(R.string.inactive);
        String text = getString(R.string.inactive_content);
        if (active)
        {
            title = getString(R.string.active);
            text = getString(R.string.active_content);
        }

        // Set the info for the views that show in the notification panel.
        NotificationCompat.Builder nB = new NotificationCompat.Builder(this, CHANNEL_ID).setOngoing(true).setAutoCancel(false).setWhen(System.currentTimeMillis()).setContentTitle(title).setContentText(text).setContentIntent(contentIntent);

        if (active)
        {
            nB.setSmallIcon(R.drawable.ic_stat_notification);
        }
        else
        {
            nB.setSmallIcon(R.drawable.ic_stat_notification_off);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
        {
            PendingIntent startIntent = PendingIntent.getBroadcast(this, 1, new Intent(this, MessageReceiver.class).putExtra("action", "START"), 0);
            PendingIntent stopIntent = PendingIntent.getBroadcast(this, 2, new Intent(this, MessageReceiver.class).putExtra("action", "STOP"), 0);
            PendingIntent issueIntent = PendingIntent.getBroadcast(this, 3, new Intent(this, MessageReceiver.class).putExtra("action", "ISSUE"), 0);
            if (active)
            {
                nB.addAction(R.drawable.ic_visibility_off, getString(R.string.stop), stopIntent);
                if (enableDebugging.equalsIgnoreCase("true"))
                {
                    nB.addAction(R.drawable.ic_error, getString(R.string.issue), issueIntent);
                }
            }
            else
            {
                nB.addAction(R.drawable.ic_visibility, getString(R.string.start), startIntent);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            nB.setPriority(Notification.PRIORITY_MIN).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_SECRET).setLocalOnly(true).setNumber(0);
        }
        return nB.build();
    }

    private void showInactiveNotification()
    {
        if (mSharedPref.getBoolean("pref_notification_warning", true))
        {
            mNM.notify(mNotificationIDwarning, createWarningNotification());
        }
        // Send the notification.
        if (mSharedPref.getBoolean("pref_notification_bar_active", true) && mSharedPref.getBoolean("pref_notification_bar_always", true))
            startForeground(mNotificationID, createActiveNotification(false));
        else
            stopForeground(true);
    }

    private void showActiveNotification()
    {
        mNM.cancel(mNotificationIDwarning);

        // Send the notification.
        StatusBarNotification[] notifications = new StatusBarNotification[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            notifications = mNM.getActiveNotifications();

        boolean found = false;
        for (StatusBarNotification notification : notifications)
            if (notification.getId() == mNotificationID)
                found = true;

        if (mSharedPref.getBoolean("pref_notification_bar_active", true))
            if (found)
                mNM.notify(mNotificationID, createActiveNotification(true));
            else
                startForeground(mNotificationID, createActiveNotification(true));
        else if (found)
            mNM.cancel(mNotificationID);
        else
            stopForeground(true);

    }

    private static class IncomingHandler extends Handler
    {
        private NotificationService service;

        private IncomingHandler(NotificationService service)
        {
            this.service = service;
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_REGISTER_CLIENT:
                    service.showActiveNotification();
                    break;
                case MSG_UNREGISTER_CLIENT:
                    service.showInactiveNotification();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
