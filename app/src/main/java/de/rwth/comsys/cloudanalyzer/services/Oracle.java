package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Oracle extends AbstractService
{

    public static final int ID = 56;
    public static final String NAME = "Oracle";
    private static final long serialVersionUID = -8056895012075571192L;
    private static final String regexFile = "storage/domainRegex/Oracle_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Oracle_cert.xml";
    private static final String rangesFile = "storage/ipRanges/Oracle_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Oracle_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS};
    private static final String[] groups = new String[]{};

    public Oracle()
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