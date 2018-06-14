package de.rwth.comsys.cloudanalyzer.network;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket;
import de.rwth.comsys.cloudanalyzer.information.FullTcpPacket;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.information.IpPacket;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Protocol;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.TrafficPropertiesManager;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.TrafficProperty;
import de.rwth.comsys.cloudanalyzer.util.Consumer;
import de.rwth.comsys.cloudanalyzer.util.IdentifiedService;
import de.rwth.comsys.cloudanalyzer.util.PacketServiceInformation;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPacket implements Packet
{
    private static Logger logger = Logger.getLogger(AbstractPacket.class.getName());
    protected long id;
    protected Information[] protocols;
    protected byte[] pendingHandlerCounts;
    protected PacketServiceInformation psi;
    protected List<Consumer<Packet>> callbacks;
    protected TrafficProperty property;

    public AbstractPacket(TrafficProperty property)
    {
        this.protocols = new Information[NetworkProtocol.values().length];
        this.pendingHandlerCounts = new byte[MainHandler.getHandlerCount()];
        this.psi = new PacketServiceInformation(this);
        this.callbacks = new LinkedList<>();
        this.property = property;
    }

    @Override
    public long getFrameNumber()
    {
        return id;
    }

    @Override
    public void addProtocolInformation(NetworkProtocol prot, Information i)
    {
        protocols[prot.ordinal()] = i;
    }

    @Override
    public Information getProtocolInformation(NetworkProtocol prot)
    {
        return protocols[prot.ordinal()];
    }

    @Override
    public void registerAllHandlerFinishedCallback(Consumer<Packet> cb)
    {
        callbacks.add(cb);
    }

    @Override
    public void handlerFinished(int id)
    {
        pendingHandlerCounts[id - 1]--;
        if (allHandlerFinished())
            executeCallbacks();
    }

    private boolean allHandlerFinished()
    {
        for (byte pendingHandlerCount : pendingHandlerCounts)
        {
            if (pendingHandlerCount > 0)
                return false;
        }
        return true;
    }

    private void executeCallbacks()
    {
        if (callbacks != null)
        {
            for (Consumer<Packet> r : callbacks)
                r.accept(this);
            callbacks = null;
        }
        else
            logger.w("Callbacks already executed.");
    }

    @Override
    public void addPendingHandler(int id)
    {
        pendingHandlerCounts[id - 1]++;
    }

    @Override
    public void addIdentifiedService(IdentifiedService is)
    {
        psi.addIdentifiedService(is);
    }

    @Override
    public void addIdentifiedServices(List<IdentifiedService> isl)
    {
        psi.addIdentifiedServices(isl);
    }

    @Override
    public PacketServiceInformation getPacketServiceInformation()
    {
        return psi;
    }

    @Override
    public IpPacket getIpPacket()
    {
        IpPacket ip = (IpPacket) getProtocolInformation(NetworkProtocol.IPv4);
        if (ip == null)
            ip = (IpPacket) getProtocolInformation(NetworkProtocol.IPv6);
        return ip;
    }

    @Override
    public FullTcpPacket getTcpPacket()
    {
        return (FullTcpPacket) getProtocolInformation(NetworkProtocol.TCP);
    }

    @Override
    public DnsPacket getDnsPacket()
    {
        return (DnsPacket) getProtocolInformation(NetworkProtocol.DNS);
    }

    @Override
    public TrafficProperty getTrafficProperty()
    {
        return property;
    }

    @Override
    public void updateTrafficProperty(Protocol protocol)
    {
        property = TrafficPropertiesManager.getProperty(property.getDirection(), property.getImportance(), property.getLink(), protocol);
    }
}
