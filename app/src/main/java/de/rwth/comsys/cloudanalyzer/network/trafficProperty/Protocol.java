package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public enum Protocol
{
    // AGGREGATED must be always the first constant
    AGGREGATED, TLS_ENCRYPTED, QUIC_ENCRYPTED;

    public static Protocol getProtocol(int id)
    {
        return values()[id + 1];
    }

    public int getId()
    {
        return ordinal() - 1;
    }
}
