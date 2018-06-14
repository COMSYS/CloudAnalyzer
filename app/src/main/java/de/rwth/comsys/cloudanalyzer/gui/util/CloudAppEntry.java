package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import java.io.Serializable;

public class CloudAppEntry implements Serializable, Comparable<CloudAppEntry>
{
    private String packageName, appName;
    private int id, position;
    private float upTraffic, downTraffic;


    public CloudAppEntry(String packageName, int id, int pos, float upCloudTraffic, float downCloudTraffic, Context context)
    {
        this.packageName = packageName;
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
    public int compareTo(CloudAppEntry appEntry)
    {
        // using custom getAppName function, so that apps with appname occur first (sorted) and packagename sorted apps afterwards
        return Integer.compare(position, appEntry.getPosition());
    }

}