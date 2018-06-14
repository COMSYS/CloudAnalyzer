package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.InformationHandler;
import de.rwth.comsys.cloudanalyzer.network.FlowKey;
import de.rwth.comsys.cloudanalyzer.network.NetworkProtocol;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

public class FullTcpPacket extends AbstractInformation
{

    public static final int PRIORITY = 6;
    public static final String TYPE = "FullTcpPacket";
    private static final long serialVersionUID = -8061812110096264071L;
    private static Logger logger = Logger.getLogger(FullTcpPacket.class.getName());
    private Packet packet;
    private int dest;
    private int src;
    private long seq;
    private long ack;
    private boolean flagFin;
    private boolean flagSyn;
    private boolean flagAck;
    private int dataOffset;
    private String id;
    private int headerOffset;
    private int length;
    private FlowKey key;

    public FullTcpPacket(String type, int priority, IpPacket ip)
    {
        super(type, priority);
        this.packet = ip.getPacket();
        this.id = ip.getIdentifier();
        this.length = ip.getPayloadLength();
        decodeNeededFields();
        key = new FlowKey(ip.getSourceAddress(), ip.getDestinationAddress(), src, dest, NetworkProtocol.TCP);
        this.packet.addProtocolInformation(NetworkProtocol.TCP, this);
        for (InformationHandler h : MainHandler.getHandlers(TYPE))
        {
            packet.addPendingHandler(h.getId());
        }
    }

    public static FullTcpPacket getTcpPacket(IpPacket ip)
    {
        return new FullTcpPacket(TYPE, PRIORITY, ip);
    }

    private void decodeNeededFields()
    {
        int index = packet.findHeaderIndex(NetworkProtocol.TCP);

        if (index != -1)
        {
            headerOffset = packet.getHeaderOffsetByIndex(index);
            decodePorts(headerOffset);
            decodeFlags(headerOffset);
            decodeSeqNrs(headerOffset);
            decodeDataOffset(headerOffset);
        }
        else
        {
            logger.w("No tcp header found");
        }
    }

    private void decodeDataOffset(int offset)
    {
        dataOffset = ((packet.getUByte(offset + 12) >>> 4) & 0b1111) * 4;
    }

    private void decodePorts(int offset)
    {
        src = packet.getUShort(offset + 0);
        dest = packet.getUShort(offset + 2);
    }

    private void decodeSeqNrs(int offset)
    {
        seq = packet.getUInt(offset + 4);
        ack = packet.getUInt(offset + 8);
    }

    private void decodeFlags(int offset)
    {
        int tmp = packet.getUByte(offset + 13);
        flagFin = (tmp & 0b1) == 0 ? false : true;
        flagSyn = (tmp & 0b10) == 0 ? false : true;
        flagAck = (tmp & 0b10000) == 0 ? false : true;
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }

    public Packet getPacket()
    {
        return packet;
    }

    public long getSeqNr()
    {
        return seq;
    }

    public long getAckNr()
    {
        return ack;
    }

    public int getSourcePort()
    {
        return src;
    }

    public int getDestinationPort()
    {
        return dest;
    }

    public boolean isSyn()
    {
        return flagSyn;
    }

    public boolean isAck()
    {
        return flagAck;
    }

    public boolean isFin()
    {
        return flagFin;
    }

    public int getLength()
    {
        return length;
    }

    public int getPayloadLength()
    {
        return length - dataOffset;
    }

    public int getDataOffset()
    {
        return dataOffset;
    }

    public int getPayloadOffset()
    {
        return dataOffset + headerOffset;
    }

    public FlowKey getFlowKey()
    {
        return key;
    }
}
