package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public enum FlowDirection
{
    // AGGREGATED must be always the first constant
    AGGREGATED, IN, OUT;

    public static FlowDirection getDirection(int id)
    {
        return values()[id + 1];
    }

    public int getId()
    {
        return ordinal() - 1;
    }
}
