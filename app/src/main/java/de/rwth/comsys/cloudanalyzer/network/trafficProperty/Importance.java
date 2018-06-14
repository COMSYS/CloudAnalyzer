package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public enum Importance
{
    // AGGREGATED must be always the first constant
    AGGREGATED, BACKGROUND, FOREGROUND;

    public static Importance getImportance(int id)
    {
        return values()[id + 1];
    }

    public int getId()
    {
        return ordinal() - 1;
    }
}
