package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Apple extends AbstractService
{

    public static final int ID = 8;
    public static final String NAME = "Apple";
    private static final long serialVersionUID = -7840565022883629473L;
    private static final String regexFile = "storage/domainRegex/Apple_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Apple_cert.xml";
    private static final String rangesFile = "storage/ipRanges/Apple_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Apple_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Apple()
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