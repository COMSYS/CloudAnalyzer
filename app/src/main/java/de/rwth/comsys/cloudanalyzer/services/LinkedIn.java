package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class LinkedIn extends AbstractService
{

    public static final int ID = 47;
    public static final String NAME = "LinkedIn";
    private static final long serialVersionUID = 3802484423163702048L;
    private static final String regexFile = "storage/domainRegex/LinkedIn_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/LinkedIn_cert.xml";
    private static final String rangesFile = "storage/ipRanges/LinkedIn_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/LinkedIn_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public LinkedIn()
    {
        super(ID, NAME, cls, groups);
    }

    @Override
    public boolean setupService()
    {
        boolean res = true;
        res &= MainHandler.getHandlerByClass(DnsStorage.class).addRegexes(regexFile);
        res &= MainHandler.getHandlerByClass(TlsHandler.class).addCertificateRegexes(certRegexFile);
        res &= MainHandler.getHandlerByClass(IpRangeStorage.class).addRanges(rangesFile);
        res &= MainHandler.getHandlerByClass(TlsHandler.class).addServerNameRegexes(sniRegexFile);

        return res;
    }
}