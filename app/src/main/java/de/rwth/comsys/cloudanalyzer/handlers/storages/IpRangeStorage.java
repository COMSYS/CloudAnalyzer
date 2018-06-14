package de.rwth.comsys.cloudanalyzer.handlers.storages;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.IpRangeSearchTree;
import de.rwth.comsys.cloudanalyzer.util.Subnet;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;

public class IpRangeStorage extends AbstractInformationStorage
{

    private static final String[] supportedHTypes = {};
    private static Logger logger = Logger.getLogger(IpRangeStorage.class.getName());
    private IpRangeSearchTree<Subnet> ip4Ranges;
    private IpRangeSearchTree<Subnet> ip6Ranges;

    public IpRangeStorage()
    {
        ip4Ranges = new IpRangeSearchTree<>();
        ip6Ranges = new IpRangeSearchTree<>();
    }

    public boolean addRanges(String file)
    {
        InputStream is = null;
        try
        {
            is = MainHandler.getAssets().open(file);
        }
        catch (IOException e)
        {
            logger.e(e.toString());
        }
        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            XmlHandler handler = new XmlHandler(ip4Ranges);
            saxParser.parse(is, handler);
            logger.d("Loaded ranges: " + file);
        }
        catch (Exception e)
        {
            logger.w(e.toString());
            return false;
        }
        return true;
    }

    public void getSubnets(InetAddress addr, List<Subnet> subnets)
    {
        if (addr == null || subnets == null)
            return;
        int len = addr.getAddress().length * 8;
        if (addr instanceof Inet4Address)
        {
            ip4Ranges.getValues(subnets, addr.getAddress(), 0, len);
        }
        else
        {
            ip6Ranges.getValues(subnets, addr.getAddress(), 0, len);
        }
    }

    /*
     * @Override public Information getInformation(String type, String
     * identifier) { return null; }
     */

    @Override
    public List<Information> processInformation(Information i)
    {
        logger.w("Received information object with an unsupported type");
        return null;
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedHTypes;
    }

    @Override
    public void resetHandler()
    {
        ip4Ranges = new IpRangeSearchTree<>();
        ip6Ranges = new IpRangeSearchTree<>();
    }

    @Override
    public void deletePersonalData()
    {
        // Nothing to do here
    }

    private class XmlHandler extends DefaultHandler
    {

        private IpRangeSearchTree<Subnet> ranges;
        private Service cService;
        private int cRegion;

        public XmlHandler(IpRangeSearchTree<Subnet> ranges)
        {
            this.ranges = ranges;
            cService = null;
            cRegion = -1;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            switch (qName)
            {
                case "Service":
                    cService = MainHandler.getService(Integer.parseInt(attributes.getValue("Id")));
                    break;
                case "Region":
                    cRegion = Integer.parseInt(attributes.getValue("ID"));
                    break;
                case "IpRange":
                    Subnet s = Subnet.getSubnet(attributes.getValue("Subnet"), cRegion);
                    s.getServices().add(cService);
                    Subnet s2 = ranges.addValue(s, s.getAddress().getAddress(), 0, s.getPrefixLength());
                    s.merge(s2);
            }
        }
    }

}
