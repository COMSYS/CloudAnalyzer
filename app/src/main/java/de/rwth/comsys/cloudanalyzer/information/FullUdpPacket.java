package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.InformationHandler;
import de.rwth.comsys.cloudanalyzer.network.FlowKey;
import de.rwth.comsys.cloudanalyzer.network.NetworkProtocol;
import de.rwth.comsys.cloudanalyzer.network.Packet;

public class FullUdpPacket extends AbstractInformation
{
    public static final int PRIORITY = 6;
    public static final String TYPE = "FullUdpPacket";
    private String id;
    private Packet packet;
    private int length;
    private int headerOffset;
    private int dataOffset;
    private int dest;
    private int src;
    private FlowKey key;

    public FullUdpPacket(String type, int priority, IpPacket ip)
    {
        super(type, priority);
        this.packet = ip.getPacket();
        this.id = ip.getIdentifier();
        this.length = ip.getPayloadLength();
        this.key = new FlowKey(ip.getSourceAddress(), ip.getDestinationAddress(), src, dest, NetworkProtocol.UDP);
        decodeNeededFields();
        this.packet.addProtocolInformation(NetworkProtocol.UDP, this);
        for (InformationHandler h : MainHandler.getHandlers(TYPE))
        {
            packet.addPendingHandler(h.getId());
        }
    }

    public static FullUdpPacket getUdpPacket(IpPacket ip)
    {
        return new FullUdpPacket(TYPE, PRIORITY, ip);
    }

    private void decodeNeededFields()
    {
        int index = packet.findHeaderIndex(NetworkProtocol.UDP);

        if (index != -1)
        {
            headerOffset = packet.getHeaderOffsetByIndex(index);
            dataOffset = 8;
            decodePorts(headerOffset);
        }
    }

    private void decodePorts(int offset)
    {
        src = packet.getUShort(offset + 0);
        dest = packet.getUShort(offset + 2);
    }

    public int getLength()
    {
        return length;
    }

    public int getPayloadLength()
    {
        return length - dataOffset;
    }

    public int getPayloadOffset()
    {
        return dataOffset + headerOffset;
    }

    public int getSourcePort()
    {
        return src;
    }

    public int getDestinationPort()
    {
        return dest;
    }

    public FlowKey getFlowKey()
    {
        return key;
    }

    public Packet getPacket()
    {
        return packet;
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }
}
