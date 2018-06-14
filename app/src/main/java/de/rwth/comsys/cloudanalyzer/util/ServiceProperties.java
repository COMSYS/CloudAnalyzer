package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.services.UnknownService;

import java.io.Serializable;

public class ServiceProperties implements Serializable
{

    private static final long serialVersionUID = -6539964871564302959L;
    private Service service;
    private int region;

    public ServiceProperties(Service service)
    {
        this.service = service;
        this.region = 0;
    }

    public ServiceProperties()
    {
        this.service = MainHandler.getService(UnknownService.ID);
        this.region = 0;
    }

    public int getRegion()
    {
        return region;
    }

    public void setRegion(int region)
    {
        this.region = region;
    }

    public Service getService()
    {
        return service;
    }

    @Override
    public String toString()
    {
        return "Service: " + service.getName() + ", Region: " + region;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + region;
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceProperties other = (ServiceProperties) obj;
        if (region != other.region)
            return false;
        if (service == null)
        {
            if (other.service != null)
                return false;
        }
        else if (!service.equals(other.service))
            return false;
        return true;
    }
}
