package de.rwth.comsys.cloudanalyzer.gui.util;

import java.io.Serializable;

public class RegionEntry implements Serializable, Comparable<RegionEntry>
{
    private int regionID;
    private String regionName;
    private int position;
    private float regionUpTraffic;
    private float regionDownTraffic;


    public RegionEntry(int regionID, String regionName, int pos, float upTraffic, float downTraffic)
    {
        this.regionID = regionID;
        this.regionName = regionName;
        this.regionUpTraffic = upTraffic;
        this.regionDownTraffic = downTraffic;
        this.position = pos;
    }

    public int getRegionID()
    {
        return regionID;
    }

    public String getRegionName()
    {
        return regionName;
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
        return regionUpTraffic;
    }

    public float getDownTraffic()
    {
        return regionDownTraffic;
    }

    @Override
    public int compareTo(RegionEntry entry)
    {
        return Integer.compare(position, entry.getPosition());
    }

}