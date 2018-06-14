package de.rwth.comsys.capture_vpn.network;

import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

import java.nio.ByteBuffer;

/**
 * manual IP packet implementation (used by IPPacketFactory)
 */

public class IPPacketBuffer extends ByteBufferWrapper
{
    public static int ipHeaderLength = 20;

    public IPPacketBuffer(ByteBuffer byteBuffer)
    {
        super(byteBuffer);
    }

    ///////////////
    // IP-Header //
    ///////////////

    public byte getVersion()
    {
        return this.getNibbles(0)[0];
    }

    public byte getIhl()
    {
        return this.getNibbles(0)[1];
    }

    public byte getDscp()
    {
        return this.getRightShiftedByte(1, 2);
    }

    public byte getEcn()
    {
        return (byte) (this.get(1) & 0x03);
    }

    public int getTotalLength()
    {
        return this.getUnsignedShort(2);
    }

    public void setTotalLength(int totalLength)
    {
        this.putUnsignedShort(totalLength, 2);
    }

    public int getIdentification()
    {
        return this.getUnsignedShort(4);
    }

    public void setIdentification(int identification)
    {
        this.putUnsignedShort(identification, 4);
    }

    public byte getIPFlags()
    {
        return this.getRightShiftedByte(6, 5);
    }

    public int getFragmentOffset()
    {
        return this.getUnsignedShort(6) & 0x1FFF;
    }

    public short getTtl()
    {
        return this.getUnsignedByte(8);
    }

    public short getProtocol()
    {
        return this.getUnsignedByte(9);
    }

    public void setProtocol(short protocol)
    {
        this.putUnsignedByte(protocol, 9);
    }

    public int getHeaderChecksum()
    {
        return this.getUnsignedShort(10);
    }

    public void setHeaderChecksum(int checksum)
    {
        this.putUnsignedShort(checksum, 10);
    }

    public short[] getSrcIP()
    {
        short[] srcIP = new short[4];

        for (int i = 0; i < srcIP.length; i++)
        {
            srcIP[i] = this.getUnsignedByte(12 + i);
        }

        return srcIP;
    }

    public void setSrcIP(short[] srcIP)
    {
        for (int i = 0; i < srcIP.length; i++)
        {
            this.putUnsignedByte(srcIP[i], 12 + i);
        }
    }

    public short[] getDstIP()
    {
        short[] dstIP = new short[4];

        for (int i = 0; i < dstIP.length; i++)
        {
            dstIP[i] = this.getUnsignedByte(16 + i);
        }

        return dstIP;
    }

    public void setDstIP(short[] dstIP)
    {
        for (int i = 0; i < dstIP.length; i++)
        {
            this.putUnsignedByte(dstIP[i], 16 + i);
        }
    }

    public int getOptions()
    {
        return 0;
    }

    public void setVersion_IHL(short version_IHL)
    {
        this.putUnsignedByte(version_IHL, 0);
    }

    public void setDSCP_ECN(short dSCP_ECN)
    {
        this.putUnsignedByte(dSCP_ECN, 1);
    }

    public void setFlags_FragmentOffset(int flags_fragmentOffset)
    {
        this.putUnsignedShort(flags_fragmentOffset, 6);
    }

    public void setTTL(short ttl)
    {
        this.putUnsignedByte(ttl, 8);
    }

    /////////////////////
    //      General    //
    // Transport-Layer //
    /////////////////////

    public void setSrcPort(int srcPort)
    {
        this.putUnsignedShort(srcPort, ipHeaderLength);
    }

    public void setDstPort(int dstPort)
    {
        this.putUnsignedShort(dstPort, ipHeaderLength + 2);
    }

    ////////////////
    // TCP-Header //
    ////////////////

    public long getSequenceNumber()
    {
        return this.getUnsignedInt(ipHeaderLength + 4);
    }

    public long getAcknowledgmentNumber()
    {
        return this.getUnsignedInt(ipHeaderLength + 8);
    }

    public byte getDataOffset()
    {
        return this.getRightShiftedByte(ipHeaderLength + 12, 4);
    }

    public void setDataOffset(byte dataOffset)
    {
        this.putUnsignedByte((short) (this.getUnsignedByte(ipHeaderLength + 12) | ((short) ((byte) (dataOffset & 0x7) << 4))), ipHeaderLength + 12);
    }

    public byte getReserved()
    {
        return 0;
    }

    public boolean[] getTCPFlags()
    {
        boolean[] flags = new boolean[9];
        int rawFlags = this.getUnsignedShort(ipHeaderLength + 12) & 0x1FF;

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

    public void setTCPFlags(boolean[] flags)
    {
        int tmp = 0;

        for (int i = 8; i >= 0; i--)
        {
            if (flags[i])
            {
                tmp = (short) ((tmp << 1) | 1);
            }
            else
            {
                tmp = (short) ((tmp << 1) & 0xFE);
            }
        }

        tmp = tmp & 0x1FF;
        tmp = this.getUnsignedShort(ipHeaderLength + 12) | tmp;

        this.putUnsignedShort(tmp, ipHeaderLength + 12);
    }

    public int getWindowSize()
    {
        return this.getUnsignedShort(ipHeaderLength + 14);
    }

    public void setWindowSize(int windowSize)
    {
        this.putUnsignedShort(windowSize, ipHeaderLength + 14);
    }

    public int getChecksum()
    {
        return this.getUnsignedShort(ipHeaderLength + 16);
    }

    public int getUrgentPointer()
    {
        return 0;
    }

    public int getTCPOptions()
    {
        return 0;
    }

    public void setSeqNr(long seqNr)
    {
        this.putUnsignedInt(seqNr, ipHeaderLength + 4);
    }

    public void setAckNr(long ackNr)
    {
        this.putUnsignedInt(ackNr, ipHeaderLength + 8);
    }

    public void setUrgPtr(int urgPtr)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public void setTCPChecksum(int checksum)
    {
        this.putUnsignedShort(checksum, ipHeaderLength + 16);
    }


    ////////////////
    // UDP-Header //
    ////////////////

    public void setUDPLength(int length)
    {
        this.putUnsignedShort(length, ipHeaderLength + 4);
    }

    public void setUDPChecksum(int checksum)
    {
        this.putUnsignedShort(checksum, ipHeaderLength + 6);
    }

    ////////////////
    // ICMP-Header //
    ////////////////

    public byte getICMPType()
    {
        return this.getNibbles(ipHeaderLength)[0];
    }

    public void setICMPType(byte type)
    {
        this.putUnsignedByte(type, ipHeaderLength);
    }

    public byte getICMPCode()
    {
        return this.getNibbles(ipHeaderLength)[1];
    }

    public void setICMPCode(short code)
    {
        this.putUnsignedByte(code, ipHeaderLength + 1);
    }

    public int getICMPChecksum()
    {
        return this.getUnsignedShort(ipHeaderLength + 2);
    }

    public void setICMPChecksum(int checksum)
    {
        this.putUnsignedShort(checksum, ipHeaderLength + 2);
    }

    public long getICMPRestHeader()
    {
        return this.getUnsignedInt(ipHeaderLength + 4);
    }

    public void setICMPRestHeader(long restHeader)
    {
        this.putUnsignedInt(restHeader, ipHeaderLength + 4);
    }

}
