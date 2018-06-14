package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.TlsHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.DnsStorage;
import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

public class StackPath extends AbstractService
{

    public static final int ID = 71;
    public static final String NAME = "StackPath";
    private static final long serialVersionUID = 2198458654234042162L;
    private static final String regexFile = "storage/domainRegex/StackPath_dns.xml";
    private static final String certRegexFile = "storage/certificateRegex/StackPath_cert.xml";
    private static final String sniRegexFile = "storage/sniRegex/StackPath_sni.xml";

    private static final CloudLayer[] cls = new CloudLayer[]{CloudLayer.IaaS};
    private static final String[] groups = new String[]{};

    public StackPath()
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