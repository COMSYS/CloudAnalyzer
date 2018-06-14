package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.InformationHandler;
import de.rwth.comsys.cloudanalyzer.network.NetworkProtocol;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class IpPacket extends AbstractInformation
{

    private static final long serialVersionUID = -3474027368452795685L;
    private static final int IPV6_HEADER_LENGTH = 40;
    private static Logger logger = Logger.getLogger(IpPacket.class.getName());
    private Packet packet;
    private int version;
    private InetAddress dest;
    private InetAddress src;
    private int nextHeader;
    private String id;
    private int headerOffset;
    private int length;
    private int payloadLength;

    public IpPacket(String type, int priority, Packet packet, int version, String id)
    {
        super(type, priority);
        this.packet = packet;
        this.version = version;
        this.id = id;
        src = null;
        dest = null;
        nextHeader = -1;
        decodeNeededFields();
        packet.addProtocolInformation(version == 4 ? NetworkProtocol.IPv4 : NetworkProtocol.IPv6, this);

        for (InformationHandler h : MainHandler.getHandlers(type))
        {
            packet.addPendingHandler(h.getId());
        }
    }

    public static IpPacket getIpPacket(Packet packet, String id)
    {
        int ver = packet.hasHeader(NetworkProtocol.IPv4) ? 4 : 6;
        boolean truncated = packet.getPacketWirelen() > packet.getCaptureLength();
        return truncated ? new TruncatedIpPacket(packet, ver, id) : new FullIpPacket(packet, ver, id);
    }

    private void decodeNeededFields()
    {
        if (version != 4 && version != 6)
        {
            logger.w("Unknown IP version");
            return;
        }
        int index = packet.findHeaderIndex(version == 4 ? NetworkProtocol.IPv4 : NetworkProtocol.IPv6);

        if (index != -1)
        {
            headerOffset = packet.getHeaderOffsetByIndex(index);
            decodeAddresses(headerOffset);
            decodeNextHeader(headerOffset);
            decodeLength(headerOffset);
        }
        else
        {
            logger.w("No ip header found");
        }
    }

    private void decodeLength(int offset)
    {
        if (version == 4)
        {
            length = packet.getUShort(offset + 2);
            payloadLength = length - (packet.getUByte(offset) & 0b00001111) * 4;
        }
        else
        {
            payloadLength = packet.getUShort(offset + 4);
            length = payloadLength + +IPV6_HEADER_LENGTH;
        }
    }

    private void decodeAddresses(int offset)
    {
        byte[] source;
        byte[] destination;
        int sOffset;
        int dOffset;
        int len;
        if (version == 4)
        {
            sOffset = 12;
            dOffset = 16;
            len = 4;
        }
        else
        {
            sOffset = 8;
            dOffset = 24;
            len = 16;
        }
        source = packet.getByteArray(offset + sOffset, len);
        destination = packet.getByteArray(offset + dOffset, len);
        try
        {
            src = InetAddress.getByAddress(source);
            dest = InetAddress.getByAddress(destination);
        }
        catch (UnknownHostException e)
        {
            logger.w(e.getMessage());
        }
    }

    private void decodeNextHeader(int offset)
    {
        int off = version == 4 ? 9 : 6;
        nextHeader = packet.getUByte(offset + off);
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }

    public int getIpVersion()
    {
        return version;
    }

    public Packet getPacket()
    {
        return packet;
    }

    public int getNextHeader()
    {
        return nextHeader;
    }

    public InetAddress getSourceAddress()
    {
        return src;
    }

    public InetAddress getDestinationAddress()
    {
        return dest;
    }

    public abstract boolean isTruncated();

    public int getLength()
    {
        return length;
    }

    public int getPayloadLength()
    {
        return payloadLength;
    }
}
