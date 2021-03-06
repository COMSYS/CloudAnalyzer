package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Akamai extends AbstractService
{

    public static final int ID = 4;
    public static final String NAME = "Akamai";
    private static final long serialVersionUID = 1070687130661889313L;
    private static final String regexFile = "storage/domainRegex/Akamai_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Akamai_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/Akamai_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS};
    private static final String[] groups = new String[]{};

    public Akamai()
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