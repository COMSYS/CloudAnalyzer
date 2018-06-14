package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Fastly extends AbstractService
{

    public static final int ID = 32;
    public static final String NAME = "Fastly";
    private static final long serialVersionUID = -1412501956161120666L;
    private static final String regexFile = "storage/domainRegex/Fastly_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Fastly_cert.xml";
    private static final String rangesFile = "storage/ipRanges/Fastly_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Fastly_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.PaaS};
    private static final String[] groups = new String[]{};

    public Fastly()
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