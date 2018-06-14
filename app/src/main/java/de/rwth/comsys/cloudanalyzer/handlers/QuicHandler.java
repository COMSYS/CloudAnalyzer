package de.rwth.comsys.cloudanalyzer.handlers;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.information.FullUdpPacket;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.network.FlowKey;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Protocol;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.IdentifiedService;
import de.rwth.comsys.cloudanalyzer.util.RegexList;
import de.rwth.comsys.cloudanalyzer.util.RegexXMLHandler;
import de.rwth.comsys.cloudanalyzer.util.ServiceProperties;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class QuicHandler extends AbstractInformationHandler
{
    private static final String[] supportedHTypes = {"FullUdpPacket"};
    private static final int[] packetNumberLengths = {1, 2, 4, 6};
    private static final int[] offsetLengths = {0, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] streamIdLengths = {1, 2, 3, 4};
    private static final byte[] clientHelloTag = {'C', 'H', 'L', 'O'};
    private static final byte[] sniTag = {'S', 'N', 'I', 0x0};
    private static Logger logger = Logger.getLogger(QuicHandler.class.getName());
    private HashMap<FlowKey, QuicStream> streamByFlowKey = new HashMap<>();
    private HashMap<Long, QuicStream> streamByCID = new HashMap<>();
    private HashMap<Service, RegexList> servNameRegexes;
    private boolean printNiServerNames;
    private PrintWriter niSniWriter;
    private long timeoutMillis = 600000;
    private long timeoutCheckInterval = 10000;
    private Date lastTimeoutCheck;

    public QuicHandler()
    {
        resetHandler();
    }

    @Override
    public List<Information> processInformation(Information i)
    {
        FullUdpPacket udp = (FullUdpPacket) i;
        if (checkPort(udp))
        {
            QuicStream s = streamByFlowKey.get(udp.getFlowKey());

            if (s == null)
            {
                QuicPacketParser parser = new QuicPacketParser(udp);
                try
                {
                    if (parser.parseCid())
                    {
                        long cid = parser.getCid();
                        s = streamByCID.get(cid);
                        if (s == null && parser.parseSNI())
                        {
                            List<IdentifiedService> l = identifyServices(parser.getServerName());
                            if (l != null)
                            {
                                s = new QuicStream(cid, l);
                                streamByFlowKey.put(udp.getFlowKey(), s);
                                streamByCID.put(cid, s);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    logger.w("An unexpected exception occurred (tried to parse packet): " + e);
                }
            }

            if (s != null)
            {
                s.update(udp);
            }

            Date now = new Date(udp.getPacket().getTimestampInMillis());
            cleanupStreams(now);
        }
        udp.getPacket().handlerFinished(getId());
        return null;
    }

    private void cleanupStreams(Date now)
    {
        if (lastTimeoutCheck == null)
        {
            lastTimeoutCheck = now;
            return;
        }
        if (now.getTime() - lastTimeoutCheck.getTime() >= timeoutCheckInterval)
        {
            List<FlowKey> keys = new ArrayList<>();
            for (Map.Entry<FlowKey, QuicStream> e : streamByFlowKey.entrySet())
            {
                if (e.getValue().timeout(now))
                {
                    keys.add(e.getKey());
                    streamByCID.remove(e.getValue().getCid());
                }
            }
            for (FlowKey k : keys)
                streamByFlowKey.remove(k);
            lastTimeoutCheck = now;
            logger.d("Cleanup finished. Removed " + (keys.size()) + " streams.");
        }
    }

    @Override
    public boolean setupHandler()
    {
        Properties p = MainHandler.getProperties();
        printNiServerNames = Boolean.valueOf(p.getProperty("QuicHandler.print_unknown_server_names", String.valueOf(false)));
        try
        {
            if (printNiServerNames)
            {
                niSniWriter = new PrintWriter(new FileWriter("niSni_quic.txt", true));
            }
        }
        catch (IOException e)
        {
            logger.w(e.toString());
        }
        timeoutCheckInterval = Long.parseLong(p.getProperty("QuicHandler.timeout_check_interval", String.valueOf(timeoutCheckInterval)));
        timeoutMillis = Long.parseLong(p.getProperty("QuicHandler.timeout_milliseconds", String.valueOf(timeoutMillis)));
        return true;
    }

    @Override
    public void shutdownHandler()
    {
        if (niSniWriter != null)
            niSniWriter.close();
    }

    @Override
    public void resetHandler()
    {
        servNameRegexes = new HashMap<>();
    }

    private List<IdentifiedService> identifyServices(String serverName)
    {
        List<IdentifiedService> res = new LinkedList<>();
        for (Map.Entry<Service, RegexList> s : servNameRegexes.entrySet())
        {
            RegexList.Regex dr = s.getValue().matches(serverName);
            if (dr != null)
            {
                ServiceProperties sprops = new ServiceProperties(s.getKey());
                sprops.setRegion(dr.getRegion());
                IdentifiedService sv = new IdentifiedService(sprops, getId());
                sv.setComment("QUIC SNI: " + serverName);
                res.add(sv);
            }
        }
        if (printNiServerNames && res.size() == 0)
            niSniWriter.println(serverName);
        return res;
    }

    private boolean checkPort(FullUdpPacket udp)
    {
        return udp.getDestinationPort() == 443 || udp.getSourcePort() == 443;
    }

    public boolean addServerNameRegexes(String file)
    {
        file = MainHandler.getDataDir() + file;
        return RegexXMLHandler.parseXML(file, servNameRegexes, true);
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedHTypes;
    }

    private static class QuicPacketParser
    {
        private final Packet p;
        private int pos;
        private String serverName;
        private long cid;

        private QuicPacketParser(FullUdpPacket udp)
        {
            this.p = udp.getPacket();
            pos = udp.getPayloadOffset();
        }

        public boolean parseCid()
        {
            int publicFlags = p.getUByte(pos);
            if (!increasePos(1))
                return false;

            boolean cidPresent = (publicFlags & 0x08) == 0x08;

            if (!cidPresent)
                return false;

            boolean verPresent = (publicFlags & 0x01) == 0x01;
            int packetNumberLength = packetNumberLengths[(publicFlags & 0x30) >> 4];
            cid = readLongLE(8);
            if (!increasePos(8))
                return false;
            if (verPresent)
            {
                // check version? According to Google document a client can include arbitrary data here ...
                if (!increasePos(4))
                    return false;
            }
            if (!increasePos(packetNumberLength))
                return false;

            return true;
        }

        public boolean parseSNI()
        {
            if (!increasePos(12)) // skip hash value. Wireshark says 12 bytes, some other sources say 16 bytes ...
                return false;
            int frameType = p.getUByte(pos);
            if ((frameType & 0xC0) != 0x80) // stream flag must be set and fin flag must not be set
                return false;
            if (!increasePos(1))
                return false;

            int streamIdLength = streamIdLengths[frameType & 0x03];
            byte[] streamId = p.getByteArray(pos, streamIdLength);
            // handshake has stream id 1
            for (int i = 0; i < streamIdLength; ++i)
            {
                if (i + 1 < streamIdLength && streamId[i] != 0)
                    return false;
                else if (i + 1 == streamIdLength && streamId[i] != 1)
                    return false;
            }
            if (!increasePos(streamIdLength))
                return false;

            int offsetLength = offsetLengths[(frameType & 0x1C) >> 2];
            if (!increasePos(offsetLength))
                return false;

            boolean dataLengthPresent = ((frameType & 0x20) == 0x20);
            if (dataLengthPresent)
            {
                if (!increasePos(2))
                    return false;
            }

            byte[] tag = p.getByteArray(pos, 4);
            if (!increasePos(4))
                return false;
            if (!Arrays.equals(tag, clientHelloTag))
                return false;

            int tagNumber = p.getUByte(pos + 1) << 8 | p.getUByte(pos);
            if (!increasePos(4)) // tagNumber is processed and skip padding bytes
                return false;

            long lastOffsetEnd = 0;
            long offsetEnd = 0;
            for (int i = 0; i < tagNumber; ++i)
            {
                tag = p.getByteArray(pos, 4);
                if (!increasePos(4))
                    return false;
                offsetEnd = readLongLE(4);
                if (!increasePos(4))
                    return false;

                if (Arrays.equals(tag, sniTag))
                {
                    if (!increasePos((int) ((tagNumber - i - 1) * 8 + lastOffsetEnd)))
                        return false;
                    break;
                }
                lastOffsetEnd = offsetEnd;
            }

            byte[] sni = p.getByteArray(pos, (int) (offsetEnd - lastOffsetEnd));
            serverName = new String(sni);

            return true;
        }

        private long readLongLE(int bytes)
        {
            long l = 0;
            for (int i = 0; i < bytes; ++i)
            {
                l |= (((long) p.getUByte(pos + i)) << i * 8);
            }
            return l;
        }

        private boolean increasePos(int amount)
        {
            pos += amount;
            return p.getCaptureLength() > pos;
        }

        public String getServerName()
        {
            return serverName;
        }

        public long getCid()
        {
            return cid;
        }
    }

    private class QuicStream
    {

        private final long cid;
        private final List<IdentifiedService> services;
        private Date lastUpdate;

        private QuicStream(long cid, List<IdentifiedService> services)
        {
            this.cid = cid;
            this.services = services;
        }

        public boolean timeout(Date now)
        {
            return (now.getTime() - lastUpdate.getTime() > timeoutMillis);
        }

        public long getCid()
        {
            return cid;
        }

        public List<IdentifiedService> getServices()
        {
            return services;
        }

        public void update(FullUdpPacket udp)
        {
            lastUpdate = new Date(udp.getPacket().getTimestampInMillis());
            udp.getPacket().addIdentifiedServices(getServices());
            udp.getPacket().updateTrafficProperty(Protocol.QUIC_ENCRYPTED);
        }
    }
}
