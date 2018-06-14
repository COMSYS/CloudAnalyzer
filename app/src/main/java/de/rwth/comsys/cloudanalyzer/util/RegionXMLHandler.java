package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

public class RegionXMLHandler extends DefaultHandler
{

    private static Logger logger = Logger.getLogger(RegionXMLHandler.class.getName());
    private IndexedTree<Integer, String> regions;
    private IndexedTreeNode<Integer, String> curNode;


    public RegionXMLHandler(IndexedTree<Integer, String> regions)
    {
        this.regions = regions;
    }

    public static boolean parseXML(String file, IndexedTree<Integer, String> regions)
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
            RegionXMLHandler handler = new RegionXMLHandler(regions);
            saxParser.parse(is, handler);
            logger.d("Parsed region: " + file);
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
        if (qName.equals("Region"))
        {
            int id = Integer.parseInt(attributes.getValue("ID"));
            String name = attributes.getValue("Name");
            curNode = regions.insert(id, name, curNode);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        if (qName.equals("Region"))
            curNode = curNode.getPredecessor();
    }
}