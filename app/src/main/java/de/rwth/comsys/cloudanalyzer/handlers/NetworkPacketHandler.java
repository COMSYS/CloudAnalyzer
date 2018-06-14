package de.rwth.comsys.cloudanalyzer.handlers;

import de.rwth.comsys.cloudanalyzer.information.*;
import de.rwth.comsys.cloudanalyzer.network.NetworkProtocol;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;

public class NetworkPacketHandler extends AbstractInformationHandler
{

    private static final String[] supportedTypes = {"NetworkPacket"};
    private static Logger logger = Logger.getLogger(NetworkPacketHandler.class.getName());

    @Override
    public List<Information> processInformation(Information i)
    {
        NetworkPacket pi = ((NetworkPacket) i);
        Packet p = pi.getPacket();
        List<Information> newInfs = null;
        if (p.hasAnyHeader(NetworkProtocol.IPv4, NetworkProtocol.IPv6))
        {
            newInfs = new LinkedList<>();
            IpPacket ip = IpPacket.getIpPacket(p, pi.getIdentifier());
            newInfs.add(ip);
            if (!ip.isTruncated())
            {
                if (p.hasHeader(NetworkProtocol.TCP))
                {
                    FullTcpPacket tcp = FullTcpPacket.getTcpPacket(ip);
                    newInfs.add(tcp);
                }
                else if (p.hasHeader(NetworkProtocol.UDP) && !p.hasHeader(NetworkProtocol.DNS))
                {
                    FullUdpPacket udp = FullUdpPacket.getUdpPacket(ip);
                    newInfs.add(udp);
                }
            }
        }
        if (p.hasHeader(NetworkProtocol.DNS))
        {
            try
            {
                DnsPacket dns = new DnsPacket(p, pi.getIdentifier());
                if (newInfs == null)
                    newInfs = new LinkedList<>();
                newInfs.add(dns);
            }
            catch (Exception | Error e)
            {
                logger.w("Error while parsing dns packet ID: " + pi.getIdentifier() + ", Frame: " + p.getFrameNumber() + " (malformed packet?): " + e.toString());
            }
        }
        pi.getPacket().handlerFinished(getId());
        return newInfs;
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedTypes;
    }
}
