package de.rwth.comsys.cloudanalyzer.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AssignedServices implements Serializable
{

    private static final long serialVersionUID = -7264134148081967990L;
    private String assignedTo;
    private Set<ServiceProperties> sprops;

    public AssignedServices(String name)
    {
        this.assignedTo = name;
        this.sprops = new HashSet<>();
    }

    public String getAssignedTo()
    {
        return assignedTo;
    }

    public Set<ServiceProperties> getServiceProperties()
    {
        return sprops;
    }

    @Override
    public String toString()
    {
        return sprops.toString() + " @ " + assignedTo;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assignedTo == null) ? 0 : assignedTo.hashCode());
        result = prime * result + ((sprops == null) ? 0 : sprops.hashCode());
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
        AssignedServices other = (AssignedServices) obj;
        if (assignedTo == null)
        {
            if (other.assignedTo != null)
                return false;
        }
        else if (!assignedTo.equals(other.assignedTo))
            return false;
        if (sprops == null)
        {
            if (other.sprops != null)
                return false;
        }
        else if (!sprops.equals(other.sprops))
            return false;
        return true;
    }
}
