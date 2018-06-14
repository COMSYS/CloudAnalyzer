package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.network.Packet;

public class FullIpPacket extends IpPacket
{

    public static final int PRIORITY = 6;
    public static final String TYPE = "FullIpPacket";
    private static final long serialVersionUID = -3228539817462353093L;

    public FullIpPacket(Packet packet, int version, String id)
    {
        super(TYPE, PRIORITY, packet, version, id);
    }

    public boolean isTruncated()
    {
        return false;
    }
}
