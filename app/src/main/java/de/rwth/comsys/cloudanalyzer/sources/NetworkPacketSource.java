package de.rwth.comsys.cloudanalyzer.sources;

import de.rwth.comsys.cloudanalyzer.information.Information;

public interface NetworkPacketSource
{
    Information nextInformation();

    boolean entireInformationProcessed();

    void close();

    Object getSyncObject();

    boolean isLive();
}
