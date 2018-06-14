package de.rwth.comsys.capture_vpn.network;

import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

/**
 * implementation to store UDP datagram information
 */

public class UDPPacket extends TransportLayerPacket
{
    private static final String TAG = "UDPPacket";
    private static final byte UDPHEADERLENGTH = 8;

    public UDPPacket(IPPacket ipPacket, ByteBufferWrapper rawPacket, int dataOffset)
    {
        super(Protocol.UDP, 12, ipPacket, rawPacket, dataOffset);
    }

    public String toString()
    {
        return super.toString();
        //        return "localPort: " + getLocalPort() + "\n"
        //                + "remotePort: " + getRemotePort() + "\n"
        //                + "length: " + getLength() + "\n"
        //                + "checksum: " + getChecksum() + "\n";
    }

    public int getLength()
    {
        return getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 4);
    }

    public void setLength(int length)
    {
        getRawPacket().putUnsignedShort(length, getTransportHeaderOffset() + 4);
    }

    public int getChecksum()
    {
        return getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 6);
    }

    public void setChecksum(int checksum)
    {
        getRawPacket().putUnsignedShort(checksum, getTransportHeaderOffset() + 6);
    }

    public ByteBufferWrapper getPayload()
    {
        return getRawPacket().getSlice(getTransportHeaderOffset() + UDPHEADERLENGTH, getLength() - UDPHEADERLENGTH);
    }

    public int getHeaderLength()
    {
        return UDPHEADERLENGTH;
    }

    public void setSrcPort(int srcPort)
    {
        getRawPacket().putUnsignedShort(srcPort, getTransportHeaderOffset());
    }

    public void setDstPort(int dstPort)
    {
        getRawPacket().putUnsignedShort(dstPort, getTransportHeaderOffset() + 2);
    }

    @Override
    public int getPayloadStart()
    {
        return getTransportHeaderOffset() + UDPHEADERLENGTH;
    }

    @Override
    public void calculateChecksum()
    {
        int checksum = 0;

        setChecksum(0);

        //        Src-IP
        checksum += getRawPacket().getUnsignedShort(12);
        checksum += getRawPacket().getUnsignedShort(14);
        //        Dst-IP
        checksum += getRawPacket().getUnsignedShort(16);
        checksum += getRawPacket().getUnsignedShort(18);

        //        Reserved + Protocol
        checksum += 0x0000 + getIPPacket().getProtocol();

        //        Length
        checksum += getLength();

        //        header + data
        for (int i = 0; i < getLength() - 1; i = i + 2)
        {
            checksum += getRawPacket().getUnsignedShort(getTransportHeaderOffset() + i);
        }

        //        padding
        if (getLength() % 2 != 0)
        {
            checksum += getRawPacket().getUnsignedByte(getTransportHeaderOffset() + getLength() - 1) << 8;
        }

        while ((checksum & 0xFFFF0000) != 0)
        {
            checksum = ((checksum & 0xFFFF0000) >>> 16) + (checksum & 0x0000FFFF);
        }

        checksum = ~checksum;

        if (checksum == 0)
            checksum = ~checksum;

        checksum = checksum & 0xFFFF;
        setChecksum(checksum);
    }
}
