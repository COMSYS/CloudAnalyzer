package de.rwth.comsys.cloudanalyzer.util;

import java.net.InetAddress;
import java.util.List;

public class DnsInformation
{

    private InetAddress addr;
    private List<AssignedServices> names;

    public DnsInformation(InetAddress addr, List<AssignedServices> names)
    {
        this.addr = addr;
        this.names = names;
    }

    public InetAddress getAddress()
    {
        return addr;
    }

    public List<AssignedServices> getNames()
    {
        return names;
    }
}
