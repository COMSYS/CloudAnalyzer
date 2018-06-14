package de.rwth.comsys.cloudanalyzer.network;

import java.net.InetAddress;

public class FlowKey
{

    private InetAddress srcAddr;
    private InetAddress destAddr;
    private int srcPort;
    private int destPort;
    private NetworkProtocol p;
    private int hashCode;

    public FlowKey(InetAddress srcAddr, InetAddress destAddr, int srcPort, int destPort, NetworkProtocol p)
    {
        this.srcAddr = srcAddr;
        this.destAddr = destAddr;
        this.srcPort = srcPort;
        this.destPort = destPort;
        this.p = p;
        this.hashCode = calculateHashCode();
    }

    public int match(FlowKey k)
    {
        if (p == k.p)
        {
            if (srcAddr.equals(k.srcAddr) && destAddr.equals(k.destAddr) && srcPort == k.srcPort && destPort == k.destPort)
            {
                return 1;
            }
            else if (srcAddr.equals(k.destAddr) && destAddr.equals(k.srcAddr) && srcPort == k.destPort && destPort == k.srcPort)
            {
                return -1;
            }
        }
        return 0;
    }

    private int calculateHashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((p == null) ? 0 : p.hashCode());
        result = prime * result + ((destAddr == null) ? 0 : destAddr.hashCode()) + ((srcAddr == null) ? 0 : srcAddr.hashCode());
        result = prime * result + destPort + srcPort;
        return result;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
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
        FlowKey other = (FlowKey) obj;
        return match(other) != 0;
    }
}
