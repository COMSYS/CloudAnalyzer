package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Cloudflare extends AbstractService
{

    public static final int ID = 19;
    public static final String NAME = "Cloudflare";
    private static final long serialVersionUID = 1419904793307006751L;
    private static final String regexFile = "storage/domainRegex/Cloudflare_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Cloudflare_cert.xml";
    private static final String rangesFile = "storage/ipRanges/Cloudflare_ip4.xml";
    private static final String sniRegexFile = "storage/sniRegex/Cloudflare_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.PaaS};
    private static final String[] groups = new String[]{};

    public Cloudflare()
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