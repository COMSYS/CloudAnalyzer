package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import java.io.Serializable;

public class AppEntry implements Serializable, Comparable<AppEntry>
{
    private String packageName, appName;
    private int id, position;
    private float totalTraffic, cloudTraffic, upTraffic, downTraffic;


    public AppEntry(String packageName, int id, int pos, float totalTraffic, float cloudTraffic, float upCloudTraffic, float downCloudTraffic, Context context)
    {
        this.packageName = packageName;
        this.totalTraffic = totalTraffic;
        this.cloudTraffic = cloudTraffic;
        this.upTraffic = upCloudTraffic;
        this.downTraffic = downCloudTraffic;
        this.position = pos;
        this.id = id;

        appName = getApplicationName(context);
        if (appName == null)
            appName = packageName;

    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    public String getAppName()
    {
        return appName != null ? appName : packageName;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int pos)
    {
        this.position = pos;
    }

    public int getId()
    {
        return id;
    }

    public float getTotalTraffic()
    {
        return totalTraffic;
    }

    public float getCloudTraffic()
    {
        return cloudTraffic;
    }

    public void setCloudTraffic(float cloudTraffic)
    {
        this.cloudTraffic = cloudTraffic;
    }

    public float getUpTraffic()
    {
        return upTraffic;
    }

    public float getDownTraffic()
    {
        return downTraffic;
    }

    public ApplicationInfo getApplicationInfo(Context context)
    {
        try
        {
            return context.getPackageManager().getApplicationInfo(packageName, 0);
        }
        catch (PackageManager.NameNotFoundException | NullPointerException e)
        {
            return null;
        }
    }

    public String getApplicationName(Context context)
    {
        try
        {
            return (String) context.getPackageManager().getApplicationLabel(getApplicationInfo(context));
        }
        catch (NullPointerException e)
        {
            return null;
        }
    }

    public Drawable getAppIcon(Context context)
    {
        ApplicationInfo appInfo = getApplicationInfo(context);
        if (appInfo != null)
            return context.getPackageManager().getApplicationIcon(appInfo);

        return ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon);
    }

    @Override
    public int compareTo(AppEntry appEntry)
    {
        // using custom getAppName function, so that apps with appname occur first (sorted) and packagename sorted apps afterwards
        //return (appName != null ? appName.toUpperCase() : packageName).compareTo(appEntry.appName != null ? appEntry.appName.toUpperCase() : appEntry.packageName);
        return Integer.compare(position, appEntry.getPosition());
    }

}