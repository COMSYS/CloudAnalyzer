package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Alibaba extends AbstractService
{

    public static final int ID = 5;
    public static final String NAME = "Alibaba";
    private static final long serialVersionUID = -5796437134885413134L;
    private static final String regexFile = "storage/domainRegex/Alibaba_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Alibaba_cert.xml";
    private static final String rangesFile = "storage/ipRanges/Alibaba_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Alibaba_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS, CloudLayer.PaaS, CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Alibaba()
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