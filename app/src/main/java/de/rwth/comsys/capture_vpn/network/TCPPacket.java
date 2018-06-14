package de.rwth.comsys.capture_vpn.network;

import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

import java.nio.ByteBuffer;

/**
 * implementation to store TCP packet information
 */

public class TCPPacket extends TransportLayerPacket
{
    private static final String TAG = "TCPPacket";

    public TCPPacket(IPPacket ipPacket, ByteBufferWrapper rawPacket, int dataOffset)
    {
        super(Protocol.TCP, 12, ipPacket, rawPacket, dataOffset);
    }

    public static String tcpFlagsToString(boolean[] flags)
    {
        StringBuilder result = new StringBuilder();

        for (Flag flag : Flag.values())
        {
            if (flags[flag.ordinal()])
                result.append(" ").append(flag.toString());
        }

        return result.toString();
    }

    private static void defineHeader(TCPPacket tcpPacket, int srcPort, int dstPort, long seqNr, long ackNr, boolean[] flags, int windowSize, int urgPtr)
    {
        tcpPacket.setSrcPort(srcPort);
        tcpPacket.setDstPort(dstPort);
        tcpPacket.setSeqNr(seqNr);
        tcpPacket.setAckNr(ackNr);
        tcpPacket.setDataOffset((byte) 5);
        tcpPacket.setFlags(flags);
        tcpPacket.setWindowSize(windowSize);
    }

    public static TCPPacket createTCPPacket(int srcPort, int dstPort, long seqNr, long ackNr, boolean[] flags, int windowSize, int urgPtr, int dataSize)
    {
        TCPPacket tcpPacket = new TCPPacket(null, new ByteBufferWrapper(ByteBuffer.allocate(20 + dataSize)), 0);

        defineHeader(tcpPacket, srcPort, dstPort, seqNr, ackNr, flags, windowSize, urgPtr);

        return tcpPacket;
    }

    public static TCPPacket createTCPPacket(int srcPort, int dstPort, long seqNr, long ackNr, boolean[] flags, int windowSize, int urgPtr, ByteBuffer data)
    {
        TCPPacket tcpPacket = new TCPPacket(null, new ByteBufferWrapper(ByteBuffer.allocate(20 + data.limit() - data.position())), 0);

        defineHeader(tcpPacket, srcPort, dstPort, seqNr, ackNr, flags, windowSize, urgPtr);

        for (int i = 0; data.limit() != data.position(); i++)
        {
            tcpPacket.getRawPacket().put(tcpPacket.getHeaderLength() + i, data.get());
        }

        return tcpPacket;
    }

    public static TCPPacket createSynAckPacket(int srcPort, int dstPort, long ackNr, int windowSize)
    {
        return createTCPPacket(srcPort, dstPort, 0, ackNr, Flag.getArrayFromFlags(Flag.SYN, Flag.FIN), windowSize, 0, 0);
    }

    public static TCPPacket createAckPacket(int srcPort, int dstPort, long seqNr, long ackNr, int windowSize)
    {
        return createTCPPacket(srcPort, dstPort, seqNr, ackNr, Flag.getArrayFromFlags(Flag.SYN, Flag.FIN), windowSize, 0, 0);
    }

    @Override
    public String toString()
    {
        //        return super.toString();
        return "HeaderOffset: " + getTransportHeaderOffset() + "\n" + "srcPort: " + getSrcPort() + "\n" + "dstPort: " + getDstPort() + "\n" + "sequenceNumber: " + getSequenceNumber() + "\n" + "acknowledgementNumber: " + getAcknowledgmentNumber() + "\n" + "dataOffset: " + getDataOffset() + "\n" + "flags: " + TCPPacket.tcpFlagsToString(getFlags()) + "\n" + "windowSize: " + getWindowSize() + "\n" + "checksum: " + getChecksum() + "\n" + "urgentPointer: " + getUrgentPointer() + "\n";
    }

    public long getSequenceNumber()
    {
        return getRawPacket().getUnsignedInt(getTransportHeaderOffset() + 4);
    }

    public long getAcknowledgmentNumber()
    {
        return getRawPacket().getUnsignedInt(getTransportHeaderOffset() + 8);
    }

    public byte getDataOffset()
    {
        return getRawPacket().getRightShiftedByte(getTransportHeaderOffset() + 12, 4);
    }

    public void setDataOffset(byte dataOffset)
    {
        getRawPacket().putUnsignedByte((short) (getRawPacket().getUnsignedByte(12) | ((short) ((byte) (dataOffset & 0x7) << 4))), 12);
    }

    public byte getReserved()
    {
        return 0;
    }

    public boolean[] getFlags()
    {
        boolean[] flags = new boolean[9];
        int rawFlags = getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 12) & 0x1FF;

        for (int i = 8; i >= 0; i--)
        {
            if (rawFlags >= Math.pow(2, i))
            {
                flags[i] = true;
                rawFlags = rawFlags - (int) Math.pow(2, i);
            }
        }

        return flags;
    }

    public void setFlags(boolean[] flags)
    {
        int tmp = 0;

        for (int i = 0; i <= 8; i++)
        {
            if (flags[i])
            {
                tmp = (short) ((tmp << 1) | 1);
            }
            else
            {
                tmp = (short) ((tmp << 1) & 0);
            }
        }

        tmp = (tmp & 0x1FF) | 0xFE;

        getRawPacket().putUnsignedShort(getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 12) | tmp, 12);
    }

    public int getWindowSize()
    {
        return getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 14);
    }

    public void setWindowSize(int windowSize)
    {
        getRawPacket().putUnsignedShort(windowSize, 14);
    }

    public int getChecksum()
    {
        return getRawPacket().getUnsignedShort(getTransportHeaderOffset() + 16);
    }

    public void setChecksum(int checksum)
    {
        getRawPacket().putUnsignedShort(checksum, getTransportHeaderOffset() + 16);
    }

    public int getUrgentPointer()
    {
        return 0;
    }

    public int getLength()
    {
        return getIPPacket().getTotalLength() - getTransportHeaderOffset();
    }

    public ByteBufferWrapper getPayload()
    {
        return getRawPacket().getSlice(getTransportHeaderOffset() + (getDataOffset() * 4), getLength() - (getDataOffset() * 4));
    }

    public int getPayloadLength()
    {
        return getIPPacket().getTotalLength() - getTransportHeaderOffset() - (getDataOffset() * 4);
    }

    public int getPayloadStart()
    {
        return getTransportHeaderOffset() + (getDataOffset() * 4);
    }

    public int getHeaderLength()
    {
        return getDataOffset() * 4;
    }

    public void setSeqNr(long seqNr)
    {
        getRawPacket().putUnsignedInt(seqNr, getTransportHeaderOffset() + 4);
    }

    public void setAckNr(long ackNr)
    {
        getRawPacket().putUnsignedInt(ackNr, getTransportHeaderOffset() + 8);
    }

    public void setUrgPtr(int urgPtr)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
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
        checksum = checksum & 0xFFFF;

        setChecksum(checksum);
    }

    //Convenience methods
    public boolean isNSFlagSet()
    {
        return getFlags()[8];
    }

    public boolean isCWRFlagSet()
    {
        return getFlags()[7];
    }

    public boolean isECEFlagSet()
    {
        return getFlags()[6];
    }

    public boolean isURGFlagSet()
    {
        return getFlags()[5];
    }

    public boolean isACKFlagSet()
    {
        return getFlags()[4];
    }

    public boolean isPSHFlagSet()
    {
        return getFlags()[3];
    }

    public boolean isRSTFlagSet()
    {
        return getFlags()[2];
    }

    public boolean isSYNFlagSet()
    {
        return getFlags()[1];
    }

    public boolean isFINFlagSet()
    {
        return getFlags()[0];
    }

    public enum Flag
    {
        FIN, SYN, RST, PSH, ACK, URG, ECE, CWR, NS;

        public static boolean[] getArrayFromFlags(Flag... flags)
        {
            boolean[] flagArray = new boolean[9];

            for (Flag flag : flags)
            {
                flagArray[flag.ordinal()] = true;
            }

            return flagArray;
        }
    }
}