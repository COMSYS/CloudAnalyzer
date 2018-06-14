package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Fronter extends AbstractService
{

    public static final int ID = 33;
    public static final String NAME = "Fronter";
    private static final long serialVersionUID = 6254839317965590683L;
    private static final String rangesFile = "storage/ipRanges/Fronter_ip4.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Fronter()
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