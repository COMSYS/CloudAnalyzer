package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.information.DnsPacket.ResourceRecord;

import java.util.Date;

public class DnsRRInformation extends AbstractInformation
{

    public static final int PRIORITY = 7;
    public static final String TYPE = "DnsRRInformation";
    private static final long serialVersionUID = 6297215737778267572L;
    private String id;
    private ResourceRecord rr;
    private Date capDate;

    public DnsRRInformation(String id, ResourceRecord rr, Date capDate)
    {
        super(TYPE, PRIORITY);
        this.rr = rr;
        this.capDate = capDate;
        this.id = id;
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }

    public ResourceRecord getResourceRecord()
    {
        return rr;
    }

    public Date getCaptureDate()
    {
        return capDate;
    }
}
