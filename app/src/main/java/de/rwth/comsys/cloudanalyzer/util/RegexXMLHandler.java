package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class RegexXMLHandler extends DefaultHandler
{

    private static Logger logger = Logger.getLogger(RegexXMLHandler.class.getName());
    private HashMap<Service, RegexList> services;
    private Service cService;
    private int cRegion;
    private boolean ignoreCase;

    public RegexXMLHandler(HashMap<Service, RegexList> services, boolean ignoreCase)
    {
        this.services = services;
        this.cService = null;
        this.cRegion = -1;
        this.ignoreCase = ignoreCase;
    }

    public static boolean parseXML(String file, HashMap<Service, RegexList> services, boolean ignoreCase)
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
            RegexXMLHandler handler = new RegexXMLHandler(services, ignoreCase);
            saxParser.parse(is, handler);
            logger.d("Loaded regexes: " + file);
        }
        catch (Exception e)
        {
            logger.w(e.toString());
            return false;
        }
        return true;
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
            case "Regex":
                String r = attributes.getValue("Regex");
                String type = attributes.getValue("Type");
                RegexList l = services.get(cService);
                if (l == null)
                {
                    l = new RegexList();
                    services.put(cService, l);
                }
                l.addRegex(r, cRegion, type, ignoreCase);
        }
    }
}