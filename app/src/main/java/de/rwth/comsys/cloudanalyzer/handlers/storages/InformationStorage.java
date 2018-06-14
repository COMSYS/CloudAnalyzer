package de.rwth.comsys.cloudanalyzer.handlers.storages;

import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.handlers.InformationHandler;

public interface InformationStorage extends InformationHandler
{

    boolean setupStorage(DatabaseConnection dbConn, boolean reset);

    void deletePersonalData();
}
