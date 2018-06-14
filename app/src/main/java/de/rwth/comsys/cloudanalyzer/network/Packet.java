package de.rwth.comsys.cloudanalyzer.network;

import de.rwth.comsys.cloudanalyzer.information.DnsPacket;
import de.rwth.comsys.cloudanalyzer.information.FullTcpPacket;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.information.IpPacket;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Protocol;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.TrafficProperty;
import de.rwth.comsys.cloudanalyzer.util.Consumer;
import de.rwth.comsys.cloudanalyzer.util.IdentifiedService;
import de.rwth.comsys.cloudanalyzer.util.PacketServiceInformation;

import java.util.List;

public interface Packet
{

    int getPacketWirelen();

    int getCaptureLength();

    long getFrameNumber();

    long getOriginalFrameNumber();

    long getTimestampInMillis();

    boolean hasHeader(NetworkProtocol prot);

    boolean hasAnyHeader(NetworkProtocol... prots);

    int findHeaderIndex(NetworkProtocol prot);

    int findHeaderIndex(NetworkProtocol prot, int instance);

    int getHeaderInstanceCount(NetworkProtocol prot);

    int getHeaderOffsetByIndex(int index);

    int getHeaderLengthByIndex(int index);

    int getUByte(int index);

    int getUShort(int index);

    long getUInt(int index);

    byte[] getByteArray(int index, int size);

    byte[] getByteArray(int index, byte[] array, int offset, int length);

    String getString(int index, int length);

    Information getProtocolInformation(NetworkProtocol prot);

    void addProtocolInformation(NetworkProtocol prot, Information i);

    void registerAllHandlerFinishedCallback(Consumer<Packet> cb);

    void handlerFinished(int id);

    void addPendingHandler(int id);

    void addIdentifiedService(IdentifiedService is);

    void addIdentifiedServices(List<IdentifiedService> isl);

    PacketServiceInformation getPacketServiceInformation();

    IpPacket getIpPacket();

    FullTcpPacket getTcpPacket();

    DnsPacket getDnsPacket();

    TrafficProperty getTrafficProperty();

    int getApp();

    void updateTrafficProperty(Protocol protocol);
}
