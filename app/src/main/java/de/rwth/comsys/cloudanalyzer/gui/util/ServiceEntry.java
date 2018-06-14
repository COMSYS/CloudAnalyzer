package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import de.rwth.comsys.cloudanalyzer.R;

import java.io.IOException;
import java.io.Serializable;

public class ServiceEntry implements Serializable, Comparable<ServiceEntry>
{
    private int serviceID;
    private String serviceName;
    private int position;
    private float serviceUpTraffic;
    private float serviceDownTraffic;
    private int region;


    public ServiceEntry(int serviceID, String serviceName, int pos, float upTraffic, float downTraffic, int region)
    {
        this.serviceID = serviceID;
        this.serviceName = serviceName;
        this.serviceUpTraffic = upTraffic;
        this.serviceDownTraffic = downTraffic;
        this.position = pos;
        this.region = region;
    }

    public int getServiceID()
    {
        return serviceID;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public String getServiceLogoPath()
    {
        return "storage/serviceIcons/" + serviceID;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int pos)
    {
        this.position = pos;
    }

    public float getUpTraffic()
    {
        return serviceUpTraffic;
    }

    public float getDownTraffic()
    {
        return serviceDownTraffic;
    }

    public int getRegion()
    {
        return region;
    }

    public Drawable getServiceLogo(Context context)
    {
        try
        {
            return Drawable.createFromResourceStream(context.getResources(), new TypedValue(), context.getResources().getAssets().open(getServiceLogoPath()), null);
        }
        catch (IOException e)
        {
            return ContextCompat.getDrawable(context, R.drawable.ic_cloud);
        }
    }

    @Override
    public int compareTo(ServiceEntry entry)
    {
        return Integer.compare(position, entry.getPosition());
    }
}
