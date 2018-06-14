package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Subnet
{

    private static Logger logger = Logger.getLogger(Subnet.class.getName());
    private Set<Service> services;
    private int region;
    private InetAddress addr;
    private short plength;

    private Subnet(InetAddress a, short l)
    {
        addr = a;
        plength = l;
        this.services = new TreeSet<>();
    }

    public static Subnet getSubnet(String cidrstr)
    {
        String[] str = cidrstr.split("/");
        if (str.length != 2)
        {
            logger.w("getSubnet(str): wrong array length");
            return null;
        }
        InetAddress addr;
        short l;
        try
        {
            addr = InetAddress.getByName(str[0]);
            l = Short.parseShort(str[1].trim());
        }
        catch (UnknownHostException | NumberFormatException e)
        {
            logger.w(e.toString());
            return null;
        }
        if (l > addr.getAddress().length * 8 || l < 0)
            return null;
        return new Subnet(addr, l);
    }

    public static Subnet getSubnet(String cidrstr, int loc)
    {
        Subnet sub = getSubnet(cidrstr);
        if (sub == null)
            return null;
        sub.setRegion(loc);
        return sub;
    }

    public static boolean getSubnetsForAddr(List<Subnet> allSubnets, byte[] addr, List<Subnet> subnets)
    {
        boolean b = false;
        for (Subnet s : allSubnets)
        {
            if (s.isInRange(addr))
            {
                subnets.add(s);
                if (!b)
                    b = true;
            }
        }
        return b;
    }

    public Set<Service> getServices()
    {
        return services;
    }

    public boolean isInRange(byte[] address)
    {
        byte[] a = addr.getAddress();
        if (a.length != address.length)
            return false;
        int i = 0;
        for (; i < plength / 8; i++)
        {
            if (address[i] != a[i])
                return false;
        }
        byte b = (byte) 0xFF;
        b <<= 8 - (plength % 8);
        return (a[i] & b) == (address[i] & b);
    }

    @Override
    public String toString()
    {
        return addr.getHostAddress() + "/" + plength;
    }

    public InetAddress getAddress()
    {
        return addr;
    }

    public short getPrefixLength()
    {
        return plength;
    }

    public int getRegion()
    {
        return region;
    }

    public void setRegion(int l)
    {
        region = l;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addr == null) ? 0 : addr.hashCode());
        result = prime * result + plength;
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
        Subnet other = (Subnet) obj;
        if (addr == null)
        {
            if (other.addr != null)
                return false;
        }
        else if (!addr.equals(other.addr))
            return false;
        if (plength != other.plength)
            return false;
        return true;
    }

    public void merge(Subnet s)
    {
        if (s == null)
            return;
        services.addAll(s.getServices());
        if (getRegion() == 0)
            setRegion(s.getRegion());
        else if (s.getRegion() != 0)
        {
            IndexedTreeNode<Integer, String> reg1 = MainHandler.getRegions().getNode(getRegion());
            IndexedTreeNode<Integer, String> reg2 = MainHandler.getRegions().getNode(s.getRegion());
            int rel = MainHandler.getRegions().getRelation(reg1, reg2);
            if (rel > 0)
                setRegion(s.getRegion());
            else if (rel == 0)
                logger.w("Could not merge regions: " + reg1.getValue() + ", " + reg2.getValue());
        }
    }
}
