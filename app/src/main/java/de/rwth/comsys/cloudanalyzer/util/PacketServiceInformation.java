package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.network.Packet;

import java.util.LinkedList;
import java.util.List;

public class PacketServiceInformation
{

    public static final String TYPE = "PacketServiceInformation";
    private List<IdentifiedService> services;
    private Packet packet;

    public PacketServiceInformation(Packet packet)
    {
        this.packet = packet;
        services = new LinkedList<>();
    }

    public void addIdentifiedService(IdentifiedService service)
    {
        services.add(service);
    }

    public void addIdentifiedServices(List<IdentifiedService> sv)
    {
        services.addAll(sv);
    }

    public List<IdentifiedService> getIdentifiedServices()
    {
        return services;
    }

    public Packet getPacket()
    {
        return packet;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("Identified Services:").append(System.lineSeparator());
        int c = 1;
        for (IdentifiedService s : services)
        {
            str.append("  ");
            str.append(s.toString());
            if (c < services.size())
                str.append(System.lineSeparator());
            c++;
        }
        return str.toString();
    }
}
