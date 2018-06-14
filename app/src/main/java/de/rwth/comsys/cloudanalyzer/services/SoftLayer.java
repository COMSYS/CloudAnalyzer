package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class SoftLayer extends AbstractService
{

    public static final int ID = 66;
    public static final String NAME = "SoftLayer";
    private static final long serialVersionUID = 5246443818868737716L;
    private static final String regexFile = "storage/domainRegex/SoftLayer_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/SoftLayer_cert.xml";
    private static final String rangesFile = "storage/ipRanges/SoftLayer_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/SoftLayer_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS};
    private static final String[] groups = new String[]{};

    public SoftLayer()
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