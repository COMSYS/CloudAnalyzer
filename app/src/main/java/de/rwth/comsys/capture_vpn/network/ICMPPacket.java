package de.rwth.comsys.capture_vpn.network;

import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

/**
 *
 *
 */

public class ICMPPacket extends TransportLayerPacket
{
    private static final String TAG = "ICMPPacket";
    private static final byte ICMPHEADERLENGTH = 8;

    public ICMPPacket(IPPacket ipPacket, ByteBufferWrapper rawPacket, int dataOffset)
    {
        super(Protocol.ICMP, 12, ipPacket, rawPacket, dataOffset);
    }

    public String toString()
    {
        return super.toString();
        //        return "type: " + getType() + "\n"
        //                + "code: " + getCode() + "\n"
        //                + "checksum: " + getChecksum() + "\n";
    }

    private int getLength()
    {
        return getIPPacket().getTotalLength() - getTransportHeaderOffset();
    }

    public byte getType()
    {
        return getRawPacket().getNibbles(getTransportHeaderOffset())[0];
    }

    public void setType(byte type)
    {
        getRawPacket().putUnsignedByte(type, getTransportHeaderOffset());
    }

    public byte getCode()
    {
        return getRawPacket().getNibbles(getTransportHeaderOffset())[1];
    }

    public void setCode(byte code)
    {
        getRawPacket().putUnsignedByte(code, getTransportHeaderOffset() + 1);
    }

    public int getChecksum()
    {
        return getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 2);
    }

    public void setChecksum(int checksum)
    {
        getRawPacket().putUnsignedShort(checksum, getTransportHeaderOffset() + 2);
    }

    public ByteBufferWrapper getPayload()
    {
        return getRawPacket().getSlice(getTransportHeaderOffset() + ICMPHEADERLENGTH, getLength() - ICMPHEADERLENGTH);
    }

    public int getHeaderLength()
    {
        return ICMPHEADERLENGTH;
    }

    public void setSrcPort(int srcPort)
    {
        throw new UnsupportedOperationException("Not available!");
    }

    public void setDstPort(int dstPort)
    {
        throw new UnsupportedOperationException("Not available!");
    }

    @Override
    public int getPayloadStart()
    {
        return getTransportHeaderOffset() + ICMPHEADERLENGTH;
    }

    @Override
    public void calculateChecksum()
    {
        int checksum = 0;

        setChecksum(0);

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
