package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Google extends AbstractService
{

    public static final int ID = 38;
    public static final String NAME = "Google";
    private static final long serialVersionUID = -6779053477009775278L;
    private static final String regexFile = "storage/domainRegex/Google_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Google_cert.xml";
    private static final String rangesFile = "storage/ipRanges/Google_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Google_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS, CloudLayer.PaaS, CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Google()
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