package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class TrendMicro extends AbstractService
{

    public static final int ID = 79;
    public static final String NAME = "TrendMicro";
    private static final long serialVersionUID = -5900936744031967362L;
    private static final String regexFile = "storage/domainRegex/TrendMicro_dns.xml";
    private static final String rangesFile = "storage/ipRanges/TrendMicro_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/TrendMicro_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public TrendMicro()
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