package de.rwth.comsys.cloudanalyzer.handlers;

import de.rwth.comsys.cloudanalyzer.information.Information;

import java.util.List;

public interface InformationHandler
{

    List<Information> processInformation(Information i);

    String[] getSupportedHInformationTypes();

    boolean setupHandler();

    void shutdownHandler();

    void resetHandler();

    int getId();

    void setId(int id);

    void onStart(boolean live);

    void onStop();
}
