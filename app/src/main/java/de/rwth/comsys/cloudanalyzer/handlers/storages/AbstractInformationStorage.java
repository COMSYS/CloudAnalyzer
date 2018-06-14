package de.rwth.comsys.cloudanalyzer.handlers.storages;

import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.handlers.AbstractInformationHandler;

public abstract class AbstractInformationStorage extends AbstractInformationHandler implements InformationStorage
{

    @Override
    public boolean setupStorage(DatabaseConnection dbConn, boolean reset)
    {
        if (reset)
        {
            resetHandler();
        }
        return true;
    }
}
