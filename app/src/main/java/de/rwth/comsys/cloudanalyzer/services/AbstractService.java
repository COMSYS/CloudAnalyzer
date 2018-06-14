package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

import java.util.*;

public abstract class AbstractService implements Service
{

    private static final long serialVersionUID = -8167554532777119123L;
    private String name;
    private int id;
    private EnumSet<CloudLayer> clSet;
    private HashSet<String> groups;

    public AbstractService(Integer id, String name, CloudLayer[] cls, String[] groups)
    {
        this(id, name, Arrays.asList(cls), Arrays.asList(groups));
    }

    public AbstractService(Integer id, String name, Collection<CloudLayer> cls, Collection<String> groups)
    {
        this.id = id;
        this.name = name;
        this.clSet = cls.isEmpty() ? EnumSet.noneOf(CloudLayer.class) : EnumSet.copyOf(cls);
        this.groups = new HashSet<>(groups);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public boolean setupService()
    {
        return true;
    }

    @Override
    public Set<CloudLayer> getLayers()
    {
        return clSet;
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public Set<String> getGroups()
    {
        return groups;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Service))
            return false;
        return this.getId() == ((Service) obj).getId();
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    public int compareTo(Service other)
    {
        return Integer.compare(getId(), other.getId());
    }
}
