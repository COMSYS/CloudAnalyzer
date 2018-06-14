package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class GmoCloud extends AbstractService
{

    public static final int ID = 36;
    public static final String NAME = "GmoCloud";
    private static final long serialVersionUID = -4951854623887267631L;
    private static final String regexFile = "storage/domainRegex/GmoCloud_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/GmoCloud_cert.xml";
    private static final String rangesFile = "storage/ipRanges/GmoCloud_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/GmoCloud_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public GmoCloud()
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