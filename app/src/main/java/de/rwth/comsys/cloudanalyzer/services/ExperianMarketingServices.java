package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class ExperianMarketingServices extends AbstractService
{

    public static final int ID = 30;
    public static final String NAME = "ExperianMarketingServices";
    private static final long serialVersionUID = -3971598241670175907L;
    private static final String regexFile = "storage/domainRegex/ExperianMarketingServices_dns.xml";
    private static final String rangesFile = "storage/ipRanges/ExperianMarketingServices_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/ExperianMarketingServices_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public ExperianMarketingServices()
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