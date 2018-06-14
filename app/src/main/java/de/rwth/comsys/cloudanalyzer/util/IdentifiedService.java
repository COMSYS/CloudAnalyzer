package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.services.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class IdentifiedService
{

    private ServiceProperties sprops;
    private int identifiedBy;
    private String comment;

    public IdentifiedService(ServiceProperties sprops, int identifiedBy)
    {
        this.sprops = sprops;
        this.comment = "";
        this.identifiedBy = identifiedBy;
    }

    public static boolean servicesOfSameLayerInSameGroup(List<IdentifiedService> services)
    {
        ArrayList<HashSet<String>> groups = new ArrayList<>(CloudLayer.values().length);
        ArrayList<HashSet<Service>> srvs = new ArrayList<>(CloudLayer.values().length);
        for (int i = 0; i < CloudLayer.values().length; i++)
        {
            groups.add(null);
            srvs.add(new HashSet<Service>());
        }

        for (IdentifiedService is : services)
        {
            Service s = is.getServiceProperties().getService();
            for (CloudLayer layer : s.getLayers())
            {
                srvs.get(layer.ordinal()).add(s);

                HashSet<String> lg = groups.get(layer.ordinal());
                if (lg == null)
                {
                    lg = new HashSet<>(s.getGroups());
                    groups.add(layer.ordinal(), lg);
                }
                lg.retainAll(s.getGroups());
                if (srvs.get(layer.ordinal()).size() > 1 && lg.size() == 0)
                    return false;
            }
        }
        return true;
    }

    public int identifiedBy()
    {
        return identifiedBy;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public ServiceProperties getServiceProperties()
    {
        return sprops;
    }

    public String toString()
    {
        return sprops.toString() + ", Comment: " + comment;
    }
}
