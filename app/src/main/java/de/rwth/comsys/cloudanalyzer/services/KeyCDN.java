package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class KeyCDN extends AbstractService
{

    public static final int ID = 44;
    public static final String NAME = "KeyCDN";
    private static final long serialVersionUID = 4201331207709045397L;
    private static final String regexFile = "storage/domainRegex/KeyCDN_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/KeyCDN_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/KeyCDN_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.PaaS};
    private static final String[] groups = new String[]{};

    public KeyCDN()
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