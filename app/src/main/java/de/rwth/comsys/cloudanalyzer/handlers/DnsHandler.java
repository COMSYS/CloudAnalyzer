package de.rwth.comsys.cloudanalyzer.handlers;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket.DnsType;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket.ResourceRecord;
import de.rwth.comsys.cloudanalyzer.information.DnsRRInformation;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.information.IpPacket;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.util.*;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DnsHandler extends AbstractInformationHandler
{

    private static final String[] supportedTypes = {"FullIpPacket", "TruncatedIpPacket", "DnsPacket"};
    private DnsStorage ds;

    @Override
    public List<Information> processInformation(Information i)
    {

        List<Information> newInfs;
        if (i.getType().equals("DnsPacket"))
        {
            newInfs = processDnsPacket((DnsPacket) i);
        }
        else
        {
            newInfs = checkAddresses((IpPacket) i);
            ((IpPacket) i).getPacket().handlerFinished(getId());
        }

        return newInfs;
    }

    private List<Information> processDnsPacket(DnsPacket dns)
    {
        List<Information> newInfs = null;
        if (dns != null && dns.isResponse() && dns.qcount() > 0 && dns.ancount() > 0)
        {
            newInfs = new LinkedList<>();
            for (int a = 0; a < dns.ancount(); a++)
            {
                ResourceRecord rr = dns.dnsAnswers()[a];
                DnsType t = rr.getType();
                if (t == DnsType.A || t == DnsType.AAAA || t == DnsType.CNAME || t == DnsType.PTR)
                {
                    DnsRRInformation rri = new DnsRRInformation(dns.getIdentifier() + "." + a, rr, new Date(dns.getPacket().getTimestampInMillis()));
                    newInfs.add(rri);
                }
            }
        }
        return newInfs;
    }

    private List<Information> checkAddresses(IpPacket ip)
    {
        List<Information> newInfs = null;
        Packet p = ip.getPacket();
        InetAddress[] addrs = new InetAddress[]{ip.getSourceAddress(), ip.getDestinationAddress()};
        PacketServiceInformation psi = null;
        for (int a = 0; a < 2; a++)
        {
            Date d = new Date(p.getTimestampInMillis());
            DnsInformation dns = ds.getDnsInformation(addrs[a], d);
            if (dns != null)
            {
                for (AssignedServices name : dns.getNames())
                {
                    for (ServiceProperties sp : name.getServiceProperties())
                    {
                        if (psi == null)
                            psi = new PacketServiceInformation(p);
                        IdentifiedService service = new IdentifiedService(sp, getId());
                        service.setComment(name.getAssignedTo());
                        p.addIdentifiedService(service);
                    }
                }
            }
        }
        return newInfs;
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedTypes;
    }

    @Override
    public boolean setupHandler()
    {
        ds = MainHandler.getHandlerByClass(DnsStorage.class);
        return (ds != null);
    }
}
