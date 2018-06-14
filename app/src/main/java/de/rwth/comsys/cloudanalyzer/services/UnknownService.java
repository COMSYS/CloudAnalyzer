package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class UnknownService extends AbstractService
{

    public static final int ID = 0;
    public static final String NAME = "Unknown Service";
    private static final long serialVersionUID = -1094981181626999256L;
    private static final CloudLayer[] cls = new CloudLayer[]{};
    private static final String[] groups = new String[]{};

    public UnknownService()
    {
        super(ID, NAME, cls, groups);
    }
}
