package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Cisco extends AbstractService
{

    public static final int ID = 18;
    public static final String NAME = "Cisco";
    private static final long serialVersionUID = -8339698787901844147L;
    private static final String regexFile = "storage/domainRegex/Cisco_dns.xml";
    private static final String rangesFile = "storage/ipRanges/Cisco_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Cisco_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Cisco()
    {
        super(ID, NAME, cls, groups);
    }

    @Override
    public boolean setupService()
    {
        boolean res = true;
        res &= MainHandler.getHandlerByClass(DnsStorage.class).addRegexes(regexFile);
        res &= MainHandler.getHandlerByClass(IpRangeStorage.class).addRanges(rangesFile);
        res &= MainHandler.getHandlerByClass(TlsHandler.class).addServerNameRegexes(sniRegexFile);

        return res;
    }
}