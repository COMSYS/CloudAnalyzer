package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.handlers.InformationHandler;
import de.rwth.comsys.cloudanalyzer.network.Packet;

public class NetworkPacket extends AbstractInformation
{

    public static final int PRIORITY = 5;
    public static final String TYPE = "NetworkPacket";
    private static final long serialVersionUID = -8580556454527943927L;
    private Packet packet;

    public NetworkPacket(Packet packet)
    {
        super(TYPE, PRIORITY);
        this.packet = packet;
        for (InformationHandler h : MainHandler.getHandlers(TYPE))
        {
            packet.addPendingHandler(h.getId());
        }
    }

    public Packet getPacket()
    {
        return packet;
    }

    public String getIdentifier()
    {
        return Long.toString(packet.getFrameNumber());
    }
}
