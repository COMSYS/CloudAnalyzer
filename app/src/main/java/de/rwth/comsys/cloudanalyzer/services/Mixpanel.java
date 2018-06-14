package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Mixpanel extends AbstractService
{

    public static final int ID = 53;
    public static final String NAME = "Mixpanel";
    private static final long serialVersionUID = -1952835742392260590L;
    private static final String regexFile = "storage/domainRegex/Mixpanel_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Mixpanel_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/Mixpanel_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Mixpanel()
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