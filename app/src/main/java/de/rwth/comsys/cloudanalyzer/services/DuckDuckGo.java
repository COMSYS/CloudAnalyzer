package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class DuckDuckGo extends AbstractService
{

    public static final int ID = 27;
    public static final String NAME = "DuckDuckGo";
    private static final long serialVersionUID = 312779188524697225L;
    private static final String regexFile = "storage/domainRegex/DuckDuckGo_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/DuckDuckGo_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/DuckDuckGo_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public DuckDuckGo()
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