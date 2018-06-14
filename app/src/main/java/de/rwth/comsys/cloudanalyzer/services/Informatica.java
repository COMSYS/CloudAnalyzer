package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Informatica extends AbstractService
{

    public static final int ID = 41;
    public static final String NAME = "Informatica";
    private static final long serialVersionUID = -8213255757652255988L;
    private static final String rangesFile = "storage/ipRanges/Informatica_ip4.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Informatica()
    {
        super(ID, NAME, cls, groups);
    }

    @Override
    public boolean setupService()
    {
        boolean res = true;
        res &= MainHandler.getHandlerByClass(IpRangeStorage.class).addRanges(rangesFile);

        return res;
    }
}