package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public enum Link
{
    // AGGREGATED must be always the first constant
    AGGREGATED, CELLULAR, WIFI;

    public static Link getLink(int id)
    {
        return values()[id + 1];
    }

    public int getId()
    {
        return ordinal() - 1;
    }
}
