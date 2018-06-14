package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Internap extends AbstractService
{

    public static final int ID = 43;
    public static final String NAME = "Internap";
    private static final long serialVersionUID = 8856180708487147280L;
    private static final String regexFile = "storage/domainRegex/Internap_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Internap_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/Internap_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS, CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Internap()
    {
        super(ID, NAME, cls, groups);
    }

    @Override
    public boolean setupService()
    {
        boolean res = true;
        res &= MainHandler.getHandlerByClass(DnsStorage.class).addRegexes(regexFile);
        res &= MainHandler.getHandlerByClass(TlsHandler.class).addCertificateRegexes(certRegexFile);
        res &= MainHandler.getHandlerByClass(TlsHandler.class).addServerNameRegexes(sniRegexFile);

        return res;
    }
}