package de.rwth.comsys.cloudanalyzer.handlers;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.IpRangeStorage;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.information.IpPacket;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.IdentifiedService;
import de.rwth.comsys.cloudanalyzer.util.ServiceProperties;
import de.rwth.comsys.cloudanalyzer.util.Subnet;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

public class IpAddressHandler extends AbstractInformationHandler
{

    private static final String[] supportedHTypes = {"FullIpPacket", "TruncatedIpPacket"};
    private static Logger logger = Logger.getLogger(IpAddressHandler.class.getName());
    private IpRangeStorage irs;

    @Override
    public List<Information> processInformation(Information i)
    {
        List<Information> newInfs = null;
        switch (i.getType())
        {
            case "TruncatedIpPacket":
            case "FullIpPacket":
            {
                IpPacket ip = (IpPacket) i;
                InetAddress[] addrs = new InetAddress[]{ip.getSourceAddress(), ip.getDestinationAddress()};
                for (int a = 0; a < 2; a++)
                {
                    LinkedList<Subnet> sn = new LinkedList<>();
                    irs.getSubnets(addrs[a], sn);
                    for (Subnet s : sn)
                    {
                        for (Service serv : s.getServices())
                        {
                            ServiceProperties sprops = new ServiceProperties(serv);
                            sprops.setRegion(s.getRegion());
                            IdentifiedService service = new IdentifiedService(sprops, getId());
                            service.setComment(s.toString());
                            ip.getPacket().addIdentifiedService(service);
                        }
                    }
                }
                ip.getPacket().handlerFinished(getId());
                break;
            }
            default:
                logger.w("Received information object with an unsupported type");
                break;
        }
        return newInfs;
    }

    @Override
    public boolean setupHandler()
    {
        irs = MainHandler.getHandlerByClass(IpRangeStorage.class);
        return (irs != null);
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedHTypes;
    }

}
