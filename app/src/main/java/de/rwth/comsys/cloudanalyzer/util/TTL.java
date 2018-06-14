package de.rwth.comsys.cloudanalyzer.util;

import java.io.Serializable;
import java.util.Date;

public class TTL implements Serializable, Comparable<TTL>
{

    private static final long serialVersionUID = 4832315345451439130L;
    private Date since;
    private long ttl;

    public TTL(Date since, long ttl)
    {
        this.since = since;
        this.ttl = ttl;
    }

    public long getRemainingTTL()
    {
        Date now = new Date();
        long t = (now.getTime() - since.getTime()) / 1000;
        t = ttl - t;
        return t > 0 ? t : 0;
    }

    public long getRemainingTTL(Date date)
    {
        long t = (date.getTime() - since.getTime()) / 1000;
        t = ttl - t;
        return t > 0 ? t : 0;
    }

    public long getTTL()
    {
        return ttl;
    }

    public Date getStartDate()
    {
        return since;
    }

    public Date getExpirationDate()
    {
        return new Date(since.getTime() + ttl * 1000);
    }

    public boolean isAvailable(Date date)
    {
        if (date == null)
            return true;
        return (date.getTime() >= since.getTime() && date.getTime() < getExpirationDate().getTime());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((since == null) ? 0 : since.hashCode());
        result = prime * result + (int) (ttl ^ (ttl >>> 32));
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
        TTL other = (TTL) obj;
        if (since == null)
        {
            if (other.since != null)
                return false;
        }
        else if (!since.equals(other.since))
            return false;
        if (ttl != other.ttl)
            return false;
        return true;
    }

    @Override
    public int compareTo(TTL other)
    {
        return getExpirationDate().compareTo(other.getExpirationDate());
    }
}
