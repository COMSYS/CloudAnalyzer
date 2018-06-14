package de.rwth.comsys.capture_vpn.network;

import android.util.Log;
import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 *
 */

public class IPPacket
{
    private static final String TAG = "IPPacket";

    private static byte defaultHeaderLength = 20;

    private static int packetCount = 0;
    private final ByteBufferWrapper rawPacket;
    private IPKey key;
    private boolean incoming;
    private boolean foreground;
    private int networkType;
    private TransportLayerPacket payload;
    private long timestamp;
    private long count;

    public IPPacket(ByteBufferWrapper rawPacket)
    {
        this.rawPacket = rawPacket;
        this.timestamp = System.currentTimeMillis();
    }

    public static int getPacketCount()
    {
        return packetCount++;
    }

    public static boolean compareIPAddress(short[] ip1, short[] ip2)
    {
        return ip1[0] == ip2[0] && ip1[1] == ip2[1] && ip1[2] == ip2[2] && ip1[3] == ip2[3];
    }

    public static String ipToString(short[] ip)
    {
        return inetAddressFromArray(ip).getHostAddress();
    }

    public static InetAddress inetAddressFromArray(short[] ip)
    {
        try
        {
            return InetAddress.getByAddress(new byte[]{(byte) ip[0], (byte) ip[1], (byte) ip[2], (byte) ip[3]});
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static IPPacket createIPPacket(short[] srcIP, short[] dstIP, short protocol, TCPPacket tcpPacket)
    {
        IPPacket ipPacket = new IPPacket(new ByteBufferWrapper(ByteBuffer.allocate(defaultHeaderLength + tcpPacket.getRawPacket().limit())));
        ipPacket.setVersion_IHL((short) 0x45);
        ipPacket.setDSCP_ECN((short) 0x00);
        ipPacket.setTotalLength(ipPacket.getRawPacket().limit());
        ipPacket.setFlags_FragmentOffset(0x00);
        ipPacket.setTTL((short) 10);
        ipPacket.setProtocol(protocol);
        ipPacket.setSrcIP(srcIP);
        ipPacket.setDstIP(dstIP);
        ipPacket.setPayload(tcpPacket);

        for (int i = 0; tcpPacket.getRawPacket().limit() != tcpPacket.getRawPacket().position(); i++)
        {
            ipPacket.getRawPacket().put(defaultHeaderLength + i, tcpPacket.getRawPacket().get());
        }

        tcpPacket.setIPPacket(ipPacket);

        ipPacket.calculateChecksum();

        return ipPacket;
    }

    public static IPPacket parse(ByteBufferWrapper rawPacket)
    {
        IPPacket ipPacket = new IPPacket(rawPacket);

        switch (TransportLayerPacket.Protocol.fromProtocolID(ipPacket.getProtocol()))
        {
            case TCP:
                ipPacket.payload = new TCPPacket(ipPacket, rawPacket, ipPacket.getIhl() * 4);
                break;

            case UDP:
                ipPacket.payload = new UDPPacket(ipPacket, rawPacket, ipPacket.getIhl() * 4);
                break;

            case ICMP:
                ipPacket.payload = new ICMPPacket(ipPacket, rawPacket, ipPacket.getIhl() * 4);
                break;

            default:
                Log.e(TAG, "Unknown transport layer protocol: " + Integer.toString(ipPacket.getProtocol() & 0xFF));
        }

        return ipPacket;
    }

    public void calculateChecksum()
    {
        int checksum = 0;

        payload.calculateChecksum();

        setHeaderChecksum(0);
        for (int i = 0; i < (getIhl() * 4); i += 2)
        {
            checksum += rawPacket.getUnsignedShort(i);
        }

        while ((checksum & 0xFFFF0000) != 0)
        {
            checksum = ((checksum & 0xFFFF0000) >>> 16) + (checksum & 0x0000FFFF);
        }

        checksum = ~checksum;
        checksum = checksum & 0xFFFF;

        setHeaderChecksum(checksum);
    }

    public String toString()
    {
        return "Version: " + getVersion() + "\n"
                //                + "IHL: " + getIhl() + "\n"
                //                + "DSCP: " + getDscp() + "\n"
                //                + "ECN: " + getEcn() + "\n"
                //                + "TotalLength: " + getTotalLength() + "\n"
                //                + "Identification: " + getIdentification() + "\n"
                //                + "Flags: " + getFlags() + "\n"
                //                + "FragmentOffset: " + getFragmentOffset() + "\n"
                //                + "TTL: " + getTtl() + "\n"
                //                + "Protocol: " + getProtocol() + "\n"
                //                + "HeaderChecksum: " + getHeaderChecksum() + "\n"
                + "SrcIP: " + getSrcIP()[0] + "." + getSrcIP()[1] + "." + getSrcIP()[2] + "." + getSrcIP()[3] + "\n" + "DstIP: " + getDstIP()[0] + "." + getDstIP()[1] + "." + getDstIP()[2] + "." + getDstIP()[3] + "\n" + "\n" + (payload != null ? payload.toString() : "");
    }

    public byte getVersion()
    {
        return rawPacket.getNibbles(0)[0];
    }

    public byte getIhl()
    {
        return rawPacket.getNibbles(0)[1];
    }

    public byte getDscp()
    {
        return rawPacket.getRightShiftedByte(1, 2);
    }

    public byte getEcn()
    {
        return (byte) (rawPacket.get(1) & 0x03);
    }

    public int getTotalLength()
    {
        return rawPacket.getUnsignedShort(2);
    }

    public void setTotalLength(int totalLength)
    {
        getRawPacket().putUnsignedShort(totalLength, 2);
    }

    public int getIdentification()
    {
        return rawPacket.getUnsignedShort(4);
    }

    public void setIdentification(int identification)
    {
        getRawPacket().putUnsignedShort(identification, 4);
    }

    public byte getFlags()
    {
        return rawPacket.getRightShiftedByte(6, 5);
    }

    public int getFragmentOffset()
    {
        return rawPacket.getUnsignedShort(6) & 0x1FFF;
    }

    public short getTtl()
    {
        return rawPacket.getUnsignedByte(8);
    }

    public short getProtocol()
    {
        return rawPacket.getUnsignedByte(9);
    }

    public void setProtocol(short protocol)
    {
        rawPacket.putUnsignedByte(protocol, 9);
    }

    public int getHeaderChecksum()
    {
        return rawPacket.getUnsignedShort(10);
    }

    public void setHeaderChecksum(int checksum)
    {
        rawPacket.putUnsignedShort(checksum, 10);
    }

    public short[] getSrcIP()
    {
        short[] srcIP = new short[4];

        for (int i = 0; i < srcIP.length; i++)
        {
            srcIP[i] = rawPacket.getUnsignedByte(12 + i);
        }

        return srcIP;
    }

    public void setSrcIP(short[] srcIP)
    {
        for (int i = 0; i < srcIP.length; i++)
        {
            rawPacket.putUnsignedByte(srcIP[i], 12 + i);
        }
    }

    public short[] getDstIP()
    {
        short[] dstIP = new short[4];

        for (int i = 0; i < dstIP.length; i++)
        {
            dstIP[i] = rawPacket.getUnsignedByte(16 + i);
        }

        return dstIP;
    }

    public void setDstIP(short[] dstIP)
    {
        for (int i = 0; i < dstIP.length; i++)
        {
            rawPacket.putUnsignedByte(dstIP[i], 16 + i);
        }
    }

    public TransportLayerPacket getPayload()
    {
        return payload;
    }

    public void setPayload(TransportLayerPacket payload)
    {
        this.payload = payload;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public long getCount()
    {
        return count;
    }

    public void setCount(long count)
    {
        this.count = count;
    }

    public ByteBufferWrapper getRawPacket()
    {
        return rawPacket;
    }

    public void setVersion_IHL(short version_IHL)
    {
        getRawPacket().putUnsignedByte(version_IHL, 0);
    }

    public void setDSCP_ECN(short dSCP_ECN)
    {
        getRawPacket().putUnsignedByte(dSCP_ECN, 1);
    }

    public void setFlags_FragmentOffset(int flags_fragmentOffset)
    {
        getRawPacket().putUnsignedShort(flags_fragmentOffset, 6);
    }

    public void setTTL(short ttl)
    {
        getRawPacket().putUnsignedByte(ttl, 8);
    }

    public InetAddress getInetAddressFromDst()
    {
        return IPPacket.inetAddressFromArray(getDstIP());
    }

    public InetAddress getInetAddressFromSrc()
    {
        return IPPacket.inetAddressFromArray(getSrcIP());
    }

    public IPKey getKey()
    {
        if (key == null)
        {
            if (getPayload() == null)
                IPPacket.parse(getRawPacket());
            key = new IPKey();
        }
        return key;
    }

    public boolean isIncoming()
    {
        return incoming;
    }

    public void setIncoming(boolean incoming)
    {
        this.incoming = incoming;
    }

    public boolean isForeground()
    {
        return foreground;
    }

    public void setForeground(boolean foreground)
    {
        this.foreground = foreground;
    }

    public int getNetworkType()
    {
        return networkType;
    }

    public void setNetworkType(int networkType)
    {
        this.networkType = networkType;
    }

    public class IPKey
    {

        public short[] getLocalIP()
        {
            return (IPPacket.this.isIncoming() ? IPPacket.this.getDstIP() : IPPacket.this.getSrcIP());
        }

        public short[] getRemoteIP()
        {
            return (IPPacket.this.isIncoming() ? IPPacket.this.getSrcIP() : IPPacket.this.getDstIP());
        }

        public int getLocalPort()
        {
            return (IPPacket.this.isIncoming() ? IPPacket.this.getPayload().getDstPort() : IPPacket.this.getPayload().getSrcPort());
        }

        public int getRemotePort()
        {
            return (IPPacket.this.isIncoming() ? IPPacket.this.getPayload().getSrcPort() : IPPacket.this.getPayload().getDstPort());
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            IPKey ipKey = (IPKey) o;

            if (getLocalPort() != ipKey.getLocalPort())
                return false;
            if (getRemotePort() != ipKey.getRemotePort())
                return false;
            if (!Arrays.equals(getLocalIP(), ipKey.getLocalIP()))
                return false;
            return Arrays.equals(getRemoteIP(), ipKey.getRemoteIP());

        }

        @Override
        public int hashCode()
        {
            int result = Arrays.hashCode(getLocalIP());
            result = 31 * result + Arrays.hashCode(getRemoteIP());
            result = 31 * result + getLocalPort();
            result = 31 * result + getRemotePort();
            return result;
        }

        @Override
        public String toString()
        {
            return "";
        }
    }

}