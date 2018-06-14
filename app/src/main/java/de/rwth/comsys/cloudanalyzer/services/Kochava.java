package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class Kochava extends AbstractService
{

    public static final int ID = 45;
    public static final String NAME = "Kochava";
    private static final long serialVersionUID = -7100709869027149553L;
    private static final String regexFile = "storage/domainRegex/Kochava_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/Kochava_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/Kochava_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.SaaS};
    private static final String[] groups = new String[]{};

    public Kochava()
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