package de.rwth.comsys.capture_vpn.network;

import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;

import java.nio.ByteBuffer;

/**
 * reconstructs the IP header for TCP/UDP packets/datagrams (incoming direction)
 */

public class IPPacketFactory
{
    private static final String TAG = "IPPacketFactory";

    private static final int udpHeaderLength = 8;

    public static IPPacket createTCPFlagPacket(short[] srcIP, int srcPort, short[] dstIP, int dstPort, long seqNr, long ackNr, int windowSize, TCPPacket.Flag... flags)
    {
        IPPacketBuffer rawPacket = new IPPacketBuffer(ByteBuffer.allocate(40));

        ////////
        // IP //
        ////////
        rawPacket.setVersion_IHL((byte) 0x45);
        rawPacket.setDSCP_ECN((byte) 0x00);
        rawPacket.setTotalLength(rawPacket.limit());
        rawPacket.setTTL((short) 64);
        rawPacket.setFlags_FragmentOffset(0x4000);
        rawPacket.setIdentification(0);
        rawPacket.setProtocol(TransportLayerPacket.Protocol.protocolToID(TransportLayerPacket.Protocol.TCP));

        rawPacket.setSrcIP(srcIP);
        rawPacket.setDstIP(dstIP);

        /////////
        // TCP //
        /////////

        rawPacket.setSrcPort(srcPort);
        rawPacket.setDstPort(dstPort);
        rawPacket.setSeqNr(seqNr);
        rawPacket.setAckNr(ackNr);
        rawPacket.setDataOffset((byte) 5);
        rawPacket.setTCPFlags(TCPPacket.Flag.getArrayFromFlags(flags));
        rawPacket.setWindowSize(windowSize);

        IPPacket ipPacket = IPPacket.parse(rawPacket);

        ipPacket.calculateChecksum();

        return ipPacket;
    }

    public static IPPacket createTCPDataPacket(short[] srcIP, int srcPort, short[] dstIP, int dstPort, long seqNr, long ackNr, int windowSize, ByteBuffer payload, int maxPayloadLength, TCPPacket.Flag... flags)
    {
        int payloadLength = Math.min(payload.limit() - payload.position(), maxPayloadLength);
        //if (DEBUG)
        //    Log.v(TAG, "TCP: " + payloadLength);
        IPPacketBuffer rawPacket = new IPPacketBuffer(ByteBuffer.allocate(40 + payloadLength));

        ////////
        // IP //
        ////////
        rawPacket.setVersion_IHL((byte) 0x45);
        rawPacket.setDSCP_ECN((byte) 0x00);
        rawPacket.setTotalLength(rawPacket.limit());
        rawPacket.setTTL((short) 255);
        rawPacket.setProtocol(TransportLayerPacket.Protocol.protocolToID(TransportLayerPacket.Protocol.TCP));

        rawPacket.setSrcIP(srcIP);
        rawPacket.setDstIP(dstIP);

        /////////
        // TCP //
        /////////

        rawPacket.setSrcPort(srcPort);
        rawPacket.setDstPort(dstPort);
        rawPacket.setSeqNr(seqNr);
        rawPacket.setAckNr(ackNr);
        rawPacket.setDataOffset((byte) 5);
        rawPacket.setTCPFlags(TCPPacket.Flag.getArrayFromFlags(flags));
        rawPacket.setWindowSize(windowSize);

        IPPacket ipPacket = IPPacket.parse(rawPacket);

        ByteBufferWrapper wrapper = new ByteBufferWrapper(payload);
        ipPacket.getRawPacket().position(ipPacket.getPayload().getPayloadStart());
        wrapper.putIntoBuffer(ipPacket.getRawPacket().getByteBuffer(), payloadLength);
        ipPacket.getRawPacket().position(0);

        ipPacket.calculateChecksum();

        return ipPacket;
    }

    public static IPPacket createUDPDataPacket(short[] srcIP, int srcPort, short[] dstIP, int dstPort, ByteBuffer payload, int maxPayloadLength)
    {
        int payloadLength = Math.min(payload.limit() - payload.position(), maxPayloadLength);
        IPPacketBuffer rawPacket = new IPPacketBuffer(ByteBuffer.allocate(40 + payloadLength));

        ////////
        // IP //
        ////////
        rawPacket.setVersion_IHL((byte) 0x45);
        rawPacket.setDSCP_ECN((byte) 0x00);
        rawPacket.setTotalLength(rawPacket.limit());
        rawPacket.setTTL((short) 255);
        rawPacket.setProtocol(TransportLayerPacket.Protocol.protocolToID(TransportLayerPacket.Protocol.UDP));

        rawPacket.setSrcIP(srcIP);
        rawPacket.setDstIP(dstIP);

        /////////
        // UDP //
        /////////

        rawPacket.setSrcPort(srcPort);
        rawPacket.setDstPort(dstPort);
        rawPacket.setUDPLength(udpHeaderLength + payloadLength);

        IPPacket ipPacket = IPPacket.parse(rawPacket);

        ByteBufferWrapper wrapper = new ByteBufferWrapper(payload);
        ipPacket.getRawPacket().position(ipPacket.getPayload().getPayloadStart());
        wrapper.putIntoBuffer(ipPacket.getRawPacket().getByteBuffer(), payloadLength);
        ipPacket.getRawPacket().position(0);

        ipPacket.calculateChecksum();

        return ipPacket;
    }

    public static IPPacket createICMPDestUnreachablePacket(short[] srcIP, short[] dstIP, ByteBuffer payload, int maxPayloadLength)
    {
        int payloadLength = Math.min(payload.limit() - payload.position(), maxPayloadLength);
        IPPacketBuffer rawPacket = new IPPacketBuffer(ByteBuffer.allocate(28 + payloadLength));

        ////////
        // IP //
        ////////
        rawPacket.setVersion_IHL((byte) 0x45);
        rawPacket.setDSCP_ECN((byte) 0x00);
        rawPacket.setTotalLength(rawPacket.limit());
        rawPacket.setTTL((short) 255);
        rawPacket.setProtocol(TransportLayerPacket.Protocol.protocolToID(TransportLayerPacket.Protocol.ICMP));

        rawPacket.setSrcIP(srcIP);
        rawPacket.setDstIP(dstIP);

        //////////
        // ICMP //
        //////////

        rawPacket.setICMPType((byte) 0x03); // Destination unreachable message
        rawPacket.setICMPCode((byte) 0x03); // Port unreachable error.

        IPPacket ipPacket = IPPacket.parse(rawPacket);

        ByteBufferWrapper wrapper = new ByteBufferWrapper(payload);
        ipPacket.getRawPacket().position(ipPacket.getPayload().getPayloadStart());
        wrapper.putIntoBuffer(ipPacket.getRawPacket().getByteBuffer(), payloadLength);
        ipPacket.getRawPacket().position(0);

        ipPacket.calculateChecksum();

        return ipPacket;
    }
}
