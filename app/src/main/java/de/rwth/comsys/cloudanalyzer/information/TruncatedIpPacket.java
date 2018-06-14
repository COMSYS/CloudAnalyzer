package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.network.Packet;

public class TruncatedIpPacket extends IpPacket
{

    public static final int PRIORITY = 6;
    public static final String TYPE = "TruncatedIpPacket";
    private static final long serialVersionUID = -1231749876871656836L;

    public TruncatedIpPacket(Packet packet, int version, String id)
    {
        super(TYPE, PRIORITY, packet, version, id);
    }

    public boolean isTruncated()
    {
        return true;
    }
}
