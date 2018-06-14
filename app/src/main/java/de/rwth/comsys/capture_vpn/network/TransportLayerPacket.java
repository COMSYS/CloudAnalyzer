package de.rwth.comsys.capture_vpn.network;

import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

/**
 * common functionality of transport packets
 */

public abstract class TransportLayerPacket
{
    private final ByteBufferWrapper rawPacket;
    private final int pseudoHeaderLength;
    private IPPacket ipPacket;
    private int transportHeaderOffset;
    private Protocol prot;

    protected TransportLayerPacket(Protocol prot, int pseudoHeaderLength, IPPacket ipPacket, ByteBufferWrapper rawPacket, int transportHeaderOffset)
    {
        this.pseudoHeaderLength = pseudoHeaderLength;
        this.ipPacket = ipPacket;
        this.rawPacket = rawPacket;
        this.transportHeaderOffset = transportHeaderOffset;
        this.prot = prot;
    }

    public ByteBufferWrapper getRawPacket()
    {
        return rawPacket;
    }

    public int getTransportHeaderOffset()
    {
        return transportHeaderOffset;
    }

    public int getSrcPort()
    {
        return rawPacket.getUnsignedShort(transportHeaderOffset);
    }

    public void setSrcPort(int srcPort)
    {
        rawPacket.putUnsignedShort(srcPort, transportHeaderOffset);
    }

    public int getDstPort()
    {
        return rawPacket.getUnsignedShort(transportHeaderOffset + 2);
    }

    public void setDstPort(int dstPort)
    {
        rawPacket.putUnsignedShort(dstPort, transportHeaderOffset + 2);
    }

    public IPPacket getIPPacket()
    {
        return ipPacket;
    }

    public void setIPPacket(IPPacket ipPacket)
    {
        this.ipPacket = ipPacket;
    }

    public abstract int getHeaderLength();

    public int getPseudoHeaderLength()
    {
        return pseudoHeaderLength;
    }

    public abstract int getChecksum();

    public abstract ByteBufferWrapper getPayload();

    public abstract int getPayloadStart();

    public abstract void calculateChecksum();

    public Protocol getProtocol()
    {
        return prot;
    }

    @Override
    public String toString()
    {
        return "srcPort: " + getSrcPort() + "\n" + "dstPort: " + getDstPort() + "\n";
    }

    public enum Protocol
    {
        UDP, TCP, ICMP, TransportLayerProtocol;

        public static Protocol fromProtocolID(int ipProtocolID)
        {
            switch (ipProtocolID)
            {
                case 1:
                    return ICMP;

                case 6:
                    return TCP;

                case 17:
                    return UDP;
            }

            return TransportLayerProtocol;
        }

        public static short protocolToID(Protocol protocol)
        {
            switch (protocol)
            {
                case TCP:
                    return 6;

                case UDP:
                    return 17;

                case ICMP:
                    return 1;

                case TransportLayerProtocol:
                    return -1;
            }

            return -1;
        }
    }

}
