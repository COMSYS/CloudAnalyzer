package de.rwth.comsys.cloudanalyzer.handlers;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.information.FullTcpPacket;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.network.FlowKey;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Protocol;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.IdentifiedService;
import de.rwth.comsys.cloudanalyzer.util.RegexList;
import de.rwth.comsys.cloudanalyzer.util.RegexXMLHandler;
import de.rwth.comsys.cloudanalyzer.util.ServiceProperties;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.io.*;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;

public class TlsHandler extends AbstractInformationHandler
{

    private static final String[] supportedTypes = {"FullTcpPacket"};
    private static final byte[] version3Element = new byte[]{(byte) 0xA0, (byte) 0x03, (byte) 0x02, (byte) 0x01, (byte) 0x02};
    private static Logger logger = Logger.getLogger(TlsHandler.class.getName());
    private static CertificateFactory certFactory;
    private HashMap<Service, RegexList> servNameRegexes;
    private HashMap<Service, RegexList> certRegexes;
    private HashMap<FlowKey, TlsStream> tlsStreams;
    private HashMap<Session, List<IdentifiedService>> sessions;
    private Date lastTimeoutCheck;
    private PrintWriter niCertWriter;
    private PrintWriter niSniWriter;
    private boolean printNiCertificates;
    private boolean printNiServerNames;
    private long timeoutMillis = 10000;
    private long timeoutCheckInterval = 10000;
    public TlsHandler()
    {
        resetHandler();
    }

    @SuppressWarnings("unused")
    @Override
    public List<Information> processInformation(Information i)
    {
        FullTcpPacket tcp = (FullTcpPacket) i;
        List<Information> newInfs = null;
        if (isTlsPacket(tcp))
        {
            newInfs = processTlsPacket(tcp);
        }
        else
        {
            tcp.getPacket().handlerFinished(TlsHandler.this.getId());
        }
        return newInfs;
    }

    private List<Information> processTlsPacket(FullTcpPacket tcp)
    {
        List<Information> newInfs = null;
        FlowKey k = tcp.getFlowKey();

        if (tcp.isSyn())
        {
            TlsStream s;
            if (!tcp.isAck())
            {
                s = new TlsStream(k, tcp.getPacket().getIpPacket().getSourceAddress(), tcp.getPacket().getIpPacket().getDestinationAddress());
                s.addPacket(tcp);
                tlsStreams.put(k, s);
            }
            else
            {
                s = tlsStreams.get(k);
                if (s == null || s.receivedServerSyn())
                {
                    logger.w("Received SYN ACK before SYN");
                }
                else
                    s.addPacket(tcp);
            }
        }
        else
        {
            TlsStream s = tlsStreams.get(k);
            if (s != null && s.processedHandshake())
            {
                s.submitIdentifiedServices(tcp);
            }
            else if (s != null)
            {
                s.addPacket(tcp);
                if (s.error())
                {
                    tlsStreams.remove(k);
                }
                else if (s.processedHandshake())
                {
                    s.submitIdentifiedServices();
                }
            }
            else
            {
                tcp.getPacket().handlerFinished(TlsHandler.this.getId());
            }
        }
        Date now = new Date(tcp.getPacket().getTimestampInMillis());
        cleanupStreams(now);
        return newInfs;
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
            for (TlsStream s : tlsStreams.values())
            {
                if (s.error() || s.timeout(now))
                {
                    boolean error = s.error();
                    keys.add(s.key);
                    if (error)
                        logger.i("SslStream error: " + s);
                    else
                        logger.v("SslStream timeout: " + s);
                }
            }
            lastTimeoutCheck = now;
            for (FlowKey k : keys)
                tlsStreams.remove(k);
            logger.d("Cleanup finished. Removed " + (keys.size()) + " streams.");
        }
    }

    private boolean isTlsPacket(FullTcpPacket tcp)
    {
        return tcp.getSourcePort() == 443 || tcp.getDestinationPort() == 443;
    }

    private byte[] readASN1TypeOctets(ByteBuffer buffer)
    {
        byte tmp = buffer.get();
        boolean oneOctet = ((int) tmp & 0x1F) != 0x1F;
        byte[] typeOctets = new byte[oneOctet ? 1 : 2];
        typeOctets[0] = tmp;
        if (!oneOctet)
        {
            typeOctets[1] = buffer.get();
        }
        return typeOctets;
    }

    private long[] readASN1Length(ByteBuffer buffer)
    {
        long[] length = new long[2];
        byte tmp = buffer.get();
        if ((tmp & 0x80) != 0)
        {
            int octetsCount = (tmp & 0x7F);
            length[1] = 1 + octetsCount;
            byte[] lengthOctets = new byte[octetsCount];
            buffer.get(lengthOctets);
            for (int i = 0; i < octetsCount; ++i)
            {
                length[0] |= (lengthOctets[i] & 0xFF);
                length[0] <<= ((octetsCount - i - 1) * 8);
            }
        }
        else
            length[0] = (tmp & 0x7F);
        return length;
    }

    private int calcLengthOctetsCount(long length)
    {
        if (length <= 127)
            return 1;

        int neededOctets = 0;
        while ((1 << (neededOctets * 8)) <= length)
        {
            neededOctets++;
        }
        return neededOctets + 1;
    }

    private byte[] encodeASN1Length(long length)
    {
        byte[] result = new byte[calcLengthOctetsCount(length)];
        if (result.length > 1)
        {
            result[0] |= 0x80;
            result[0] |= ((result.length - 1) & 0xFF);
            for (int i = 0; i < result.length - 1; ++i)
            {
                result[result.length - i - 1] |= ((length >>> i * 8) & 0xFF);
            }
        }
        else
        {
            result[0] = (byte) length;
        }
        return result;
    }

    private byte[] adjustCertificateTypeLengthFields(ByteBuffer certBuffer)
    {
        byte[] result = null;
        byte[] typeOctetsCert = readASN1TypeOctets(certBuffer);
        long[] lengthCert = readASN1Length(certBuffer);
        byte[] typeOctetsTbs = readASN1TypeOctets(certBuffer);
        long[] lengthTbs = readASN1Length(certBuffer);

        // check whether version element is missing
        if (certBuffer.get(certBuffer.position()) != (byte) 0xA0)
        {
            // calculate and encode new length values
            long newTbsLength = lengthTbs[0] + version3Element.length;
            byte[] newTbsLengthOctets = encodeASN1Length(newTbsLength);
            long newCertLength = lengthCert[0] - lengthTbs[1] + newTbsLengthOctets.length + version3Element.length;
            byte[] newCertLengthOctets = encodeASN1Length(newCertLength);
            ByteBuffer buffer = ByteBuffer.wrap(new byte[typeOctetsCert.length + typeOctetsTbs.length + newCertLengthOctets.length + newTbsLengthOctets.length]);
            buffer.put(typeOctetsCert);
            buffer.put(newCertLengthOctets);
            buffer.put(typeOctetsTbs);
            buffer.put(newTbsLengthOctets);
            result = buffer.array();
        }
        else
        {
            // set version 3 (in place)
            certBuffer.put(certBuffer.position() + 4, (byte) 0x02);
        }
        return result;
    }

    private byte[] setCertificateVersion3(byte[] certBuffer)
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(certBuffer);
        ByteBuffer newCertificateBytes = byteBuffer;

        try
        {
            byte[] newTypeLengthFields = adjustCertificateTypeLengthFields(byteBuffer);
            if (newTypeLengthFields != null)
            {
                newCertificateBytes = ByteBuffer.wrap(new byte[newTypeLengthFields.length + version3Element.length + byteBuffer.remaining()]);
                newCertificateBytes.put(newTypeLengthFields);
                newCertificateBytes.put(version3Element);
                newCertificateBytes.put(byteBuffer);
            }
        }
        catch (BufferUnderflowException | IndexOutOfBoundsException ex)
        {
            logger.w(ex.toString());
            return null;
        }

        return newCertificateBytes.array();
    }

    private List<IdentifiedService> identifyServices(byte[] certBuffer)
    {
        List<IdentifiedService> res = new LinkedList<>();
        X509Certificate cert = null;
        try
        {
            cert = getCertificate(certBuffer);
        }
        catch (CertificateException e)
        {
            logger.i(e.toString());
            certBuffer = setCertificateVersion3(certBuffer);
            if (certBuffer == null)
            {
                logger.w("Could not adjust certificate version.");
            }
            else
            {
                try
                {
                    cert = getCertificate(certBuffer);
                }
                catch (CertificateException e2)
                {
                    logger.e("Parsing certificate with adjusted version failed:\n" + e2.toString());
                }
            }
        }
        if (cert != null)
        {
            String subject = cert.getSubjectDN().toString();
            List<String> altNames = getAlternativeNames(cert);
            for (Entry<Service, RegexList> s : certRegexes.entrySet())
            {
                RegexList.Regex dr = s.getValue().matches(subject, "Subject");
                if (dr == null)
                    continue;
                if (s.getValue().matchesAll(altNames, "AlternativeName"))
                {
                    ServiceProperties sprops = new ServiceProperties(s.getKey());
                    sprops.setRegion(dr.getRegion());
                    IdentifiedService sv = new IdentifiedService(sprops, getId());
                    sv.setComment("Certificate: " + subject);
                    res.add(sv);
                }
            }
            if (printNiCertificates && res.size() == 0)
                niCertWriter.println(subject);
        }
        return res;
    }

    private List<IdentifiedService> identifyServices(String serverName)
    {
        List<IdentifiedService> res = new LinkedList<>();
        for (Entry<Service, RegexList> s : servNameRegexes.entrySet())
        {
            RegexList.Regex dr = s.getValue().matches(serverName);
            if (dr != null)
            {
                ServiceProperties sprops = new ServiceProperties(s.getKey());
                sprops.setRegion(dr.getRegion());
                IdentifiedService sv = new IdentifiedService(sprops, getId());
                sv.setComment("SNI: " + serverName);
                res.add(sv);
            }
        }
        if (printNiServerNames && res.size() == 0)
            niSniWriter.println(serverName);
        return res;
    }

    private X509Certificate getCertificate(byte[] certBuffer) throws CertificateException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(certBuffer);
        return getSubjectDN(bis);
    }

    private X509Certificate getSubjectDN(InputStream is) throws CertificateException
    {
        X509Certificate res = null;
        if (certFactory == null)
            return res;
        res = (X509Certificate) certFactory.generateCertificate(is);
        return res;
    }

    private List<String> getAlternativeNames(X509Certificate cert)
    {
        List<String> res = new LinkedList<>();
        Collection<List<?>> altSubjs;
        try
        {
            altSubjs = cert.getSubjectAlternativeNames();
            if (altSubjs != null)
            {
                for (List<?> altSubj : altSubjs)
                {
                    int type = (Integer) altSubj.get(0);
                    if (type == 2)
                        res.add((String) altSubj.get(1));
                }
            }
        }
        catch (CertificateParsingException e)
        {
            logger.w(e.toString());
        }
        return res;
    }

    public boolean addServerNameRegexes(String file)
    {
        file = MainHandler.getDataDir() + file;
        return RegexXMLHandler.parseXML(file, servNameRegexes, true);
    }

    public boolean addCertificateRegexes(String file)
    {
        file = MainHandler.getDataDir() + file;
        return RegexXMLHandler.parseXML(file, certRegexes, false);
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedTypes;
    }

    @Override
    public boolean setupHandler()
    {
        boolean res = true;
        try
        {
            certFactory = CertificateFactory.getInstance("X.509");
        }
        catch (CertificateException e)
        {
            logger.e(e.toString());
            res = false;
        }
        Properties p = MainHandler.getProperties();
        timeoutCheckInterval = Long.parseLong(p.getProperty("TLSHandler.timeout_check_interval", String.valueOf(timeoutCheckInterval)));
        timeoutMillis = Long.parseLong(p.getProperty("TLSHandler.timeout_milliseconds", String.valueOf(timeoutMillis)));
        printNiServerNames = Boolean.valueOf(MainHandler.getProperties().getProperty("TLSHandler.print_unknown_server_names", String.valueOf(false)));
        printNiCertificates = Boolean.valueOf(MainHandler.getProperties().getProperty("TLSHandler.print_unknown_certificates", String.valueOf(false)));
        try
        {
            if (printNiCertificates)
            {
                niCertWriter = new PrintWriter(new FileWriter("niCertificates.txt", true));
            }
            if (printNiServerNames)
            {
                niSniWriter = new PrintWriter(new FileWriter("niSni.txt", true));
            }
        }
        catch (IOException e)
        {
            logger.w(e.toString());
        }
        return res;
    }

    @Override
    public void shutdownHandler()
    {
        if (niCertWriter != null)
            niCertWriter.close();
        if (niSniWriter != null)
            niSniWriter.close();
    }

    @Override
    public void resetHandler()
    {
        tlsStreams = new HashMap<>();
        servNameRegexes = new HashMap<>();
        certRegexes = new HashMap<>();
        sessions = new HashMap<>();
    }

    private enum Status
    {
        UNINITIALIZED, RECEIVED_SYN, PROCESS_SSL_HEADER, PROCESS_SSL_RECORD_CONTENT, PROCESS_SSL_HANDSHAKE_BODY, PROCESS_SSL_CERTIFICATES, PROCESS_SSL_CERTIFICATE, CERTIFICATE_PROCESSED, PROCESSED_HANDSHAKE,

        ERROR
    }

    private enum StreamSource
    {
        UNKNOWN, SERVER, CLIENT
    }

    private class TlsStream
    {

        private FlowKey key;
        private List<IdentifiedService> sv;
        private HashSet<DirectedTlsStream.TcpPacket> allPackets;
        private Date lastUpdate;
        private DirectedTlsStream stream1;
        private DirectedTlsStream stream2;
        private String firstId;
        private String serverName;
        public TlsStream(FlowKey k, InetAddress h1, InetAddress h2)
        {
            key = k;
            allPackets = new HashSet<>();
            stream1 = new DirectedTlsStream(h1);
            stream2 = new DirectedTlsStream(h2);
        }

        @SuppressWarnings("unused")
        public void addPacket(FullTcpPacket tcp)
        {
            if (allPackets == null)
                return;
            DirectedTlsStream.TcpPacket p;
            lastUpdate = new Date(tcp.getPacket().getTimestampInMillis());
            if (sameDirection(tcp.getFlowKey()))
            {
                p = stream1.new TcpPacket(tcp);
                stream1.addPacket(p);
                stream1.processStream();
            }
            else
            {
                p = stream2.new TcpPacket(tcp);
                stream2.addPacket(p);
                stream2.processStream();
            }
            allPackets.add(p);
            if (processedHandshake())
            {
                DirectedTlsStream server = stream2.isServerStream() ? stream2 : stream1;
                DirectedTlsStream client = !stream1.isServerStream() ? stream1 : stream2;
                Session tmp = client.sId.length > 0 ? new Session(server.fromAddr, client.sId) : null;
                if (sv == null)
                {
                    if (client.sTicket != null && client.sTicket.length > 0)
                        sv = sessions.get(new Session(client.sTicket));
                    else if (tmp != null)
                        sv = sessions.get(tmp);
                }
                if (sv != null)
                {
                    if (server.sTicket != null && server.sTicket.length > 0)
                        sessions.put(new Session(server.sTicket), sv);
                    if (server.sId.length > 0)
                    {
                        if (tmp != null)
                            sessions.remove(tmp);
                        sessions.put(new Session(server.fromAddr, server.sId), sv);
                    }
                }
                if (serverName != null)
                {
                    if (sv == null)
                        sv = identifyServices(serverName);
                    else
                        sv.addAll(identifyServices(serverName));
                }
            }
        }

        public boolean error()
        {
            return stream1.error() || stream2.error();
        }

        public boolean receivedServerSyn()
        {
            return stream1.receivedServerSyn() || stream2.receivedServerSyn();
        }

        public boolean sameDirection(FlowKey k)
        {
            return k.match(key) == 1;
        }

        public boolean processedHandshake()
        {
            return stream1.processedHandshake() && stream2.processedHandshake();
        }

        public boolean timeout(Date now)
        {
            return (now.getTime() - lastUpdate.getTime() > timeoutMillis);
        }

        public void submitIdentifiedServices()
        {
            for (DirectedTlsStream.TcpPacket p : allPackets)
            {
                submitIdentifiedServices(p.tcp);
            }
            clearTlsStream();
        }

        public void submitIdentifiedServices(FullTcpPacket packet)
        {
            lastUpdate = new Date(packet.getPacket().getTimestampInMillis());
            packet.getPacket().updateTrafficProperty(Protocol.TLS_ENCRYPTED);
            if (sv != null && sv.size() > 0)
            {
                packet.getPacket().addIdentifiedServices(sv);
            }
            packet.getPacket().handlerFinished(TlsHandler.this.getId());
        }

        public void clearTlsStream()
        {
            allPackets = null;
            stream1.clearStream();
            stream2.clearStream();
        }

        @Override
        public String toString()
        {
            return "Id and frame number of first packet: " + firstId;
        }

        private class DirectedTlsStream
        {
            private static final int SSL_CHANGE_CHIPHER_SPEC = 20;
            private static final int SSL3_RT_HANDSHAKE = 22;
            private static final int SSL3_VERSION = 0x0300;
            private static final int TLS1_VERSION = 0x0301;
            private static final int TLS1_1_VERSION = 0x0302;
            private static final int TLS1_2_VERSION = 0x0303;
            private static final int SSL_CLIENT_HELLO = 1;
            private static final int SSL_SERVER_HELLO = 2;
            private static final int SSL3_MT_CERTIFICATE = 11;
            private static final int SSL_NEW_SESSION_TICKET = 4;
            private static final int SSL_EXTENSION_SNI = 0x0000;
            private static final int SSL_EXTENSION_ST = 0x0023;
            private static final int SSL_EXTENSION_SNI_SN = 0x00;
            private TreeSet<TcpPacket> packets;
            private int cOffset;
            private long cSeq;
            private Status status;
            private int nLength;
            private int hLength;
            private int cLength;
            private int hType;
            private int sslType;
            private long seqStart;
            private int sslOffset;
            private StreamSource src;
            private byte[] sId;
            private byte[] sTicket;
            private InetAddress fromAddr;
            public DirectedTlsStream(InetAddress fromAddr)
            {
                packets = new TreeSet<>();
                cOffset = 0;
                status = Status.UNINITIALIZED;
                seqStart = 0;
                src = StreamSource.UNKNOWN;
                this.fromAddr = fromAddr;
            }

            public void addPacket(TcpPacket p)
            {
                if (packets == null || processedHandshake())
                    return;
                FullTcpPacket tcp = p.tcp;
                if (tcp.isSyn())
                {
                    if (status != Status.UNINITIALIZED)
                    {
                        logger.i("Received multiple SYN packets: " + p.getIdentifier() + ", " + tcp.getPacket().getFrameNumber());
                    }
                    else
                    {
                        status = Status.RECEIVED_SYN;
                        if (!tcp.isAck())
                        {
                            firstId = p.getIdentifier() + ", " + tcp.getPacket().getFrameNumber();
                        }
                        seqStart = tcp.getSeqNr();
                        cSeq = 1;
                    }
                }
                else if (tcp.getPayloadLength() > 0)
                {
                    if (!packets.isEmpty() && packets.first().getSeqNr() >= p.getSeqNr())
                    {
                        if (packets.first().getSeqNr() >= p.getSeqNr() + p.tcp.getPayloadLength())
                        {
                            logger.i("Received retransmission tcp packet, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                        }
                        else if (packets.first().getSeqNr() > p.getSeqNr())
                        {
                            logger.w("Received overlapping tcp packet, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                            status = Status.ERROR;
                        }
                    }
                    else
                    {
                        packets.add(p);
                    }
                }
            }

            private boolean processTlsHeader()
            {
                if (!isAvailable(5))
                {
                    return false;
                }
                sslType = read(1);
                int ver = read(2);
                nLength = read(2);
                if (!checkVersion(ver))
                {
                    status = Status.ERROR;
                    return false;
                }
                sslOffset = 0;
                status = Status.PROCESS_SSL_RECORD_CONTENT;
                return true;
            }

            private boolean processTlsRecordContent()
            {
                if (sslOffset >= nLength)
                {
                    status = Status.PROCESS_SSL_HEADER;
                    return true;
                }
                switch (sslType)
                {
                    case SSL3_RT_HANDSHAKE:
                        if (!isAvailable(4))
                        {
                            return false;
                        }
                        hType = read(1);
                        hLength = read(3);
                        if (hLength > nLength)
                        {
                            TcpPacket p = packets.first();
                            logger.w("SSL record length smaller than handshake length, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                            status = Status.ERROR;
                            return false;
                        }
                        sslOffset += 4;
                        status = Status.PROCESS_SSL_HANDSHAKE_BODY;
                        break;
                    case SSL_CHANGE_CHIPHER_SPEC:
                        status = Status.PROCESSED_HANDSHAKE;
                        return false;
                    default:
                        if (!isAvailable(nLength))
                        {
                            return false;
                        }
                        skip(nLength);
                        status = Status.PROCESS_SSL_HEADER;
                }
                return true;
            }

            private boolean processTlsHello()
            {
                if (!isAvailable(hLength))
                {
                    return false;
                }
                src = (hType == SSL_SERVER_HELLO ? StreamSource.SERVER : StreamSource.CLIENT);
                sslOffset += hLength;
                int toff = 0;
                int ver = read(2);
                if (!checkVersion(ver))
                {
                    status = Status.ERROR;
                    return false;
                }
                skip(32);
                // Session Id
                int len = read(1);
                sId = new byte[len];
                readBytes(sId);
                toff += 35 + len;

                if (isServerStream())
                {
                    skip(3);
                    toff += 3;
                }
                else
                {
                    // Cipher Suites
                    len = read(2);
                    skip(len);
                    toff += (2 + len);
                    // Compress Methods
                    len = read(1);
                    skip(len);
                    toff += (1 + len);
                }
                if (toff < hLength)
                {
                    // Extensions
                    len = read(2);
                    toff += 2;
                    while (len > 0)
                    {
                        int exType = read(2);
                        int exLen = read(2);
                        len -= (exLen + 4);
                        toff += (4 + exLen);
                        if (exLen == 0)
                            continue;
                        switch (exType)
                        {
                            case SSL_EXTENSION_SNI:
                            {
                                int tl = read(2);
                                while (tl > 0)
                                {
                                    int t = read(1);
                                    int l = read(2);
                                    tl -= (l + 3);
                                    if (serverName == null && t == SSL_EXTENSION_SNI_SN && !isServerStream())
                                    {
                                        byte[] n = new byte[l];
                                        readBytes(n);
                                        serverName = new String(n, StandardCharsets.US_ASCII);
                                    }
                                    else
                                    {
                                        skip(l);
                                    }
                                }
                                break;
                            }
                            case SSL_EXTENSION_ST:
                            {
                                if (!isServerStream())
                                {
                                    sTicket = new byte[exLen];
                                    readBytes(sTicket);
                                }
                                else
                                {
                                    skip(exLen);
                                }
                                break;
                            }
                            default:
                                skip(exLen);
                        }
                    }
                    skip(hLength - toff);
                }
                status = Status.PROCESS_SSL_RECORD_CONTENT;
                return true;
            }

            private boolean processTLsHandshakeBody()
            {
                switch (hType)
                {
                    case SSL3_MT_CERTIFICATE:
                    {
                        if (!isAvailable(3))
                        {
                            return false;
                        }
                        int len = read(3);
                        sslOffset += 3;
                        if (len <= 3)
                        {
                            TcpPacket p = packets.first();
                            logger.w("No certificate in certificate record, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                            status = Status.ERROR;
                            return false;
                        }
                        status = Status.PROCESS_SSL_CERTIFICATES;
                        break;
                    }
                    case SSL_NEW_SESSION_TICKET:
                    {
                        if (!isAvailable(hLength))
                        {
                            return false;
                        }
                        // skip lifetime hint and length
                        skip(6);
                        sTicket = new byte[hLength - 6];
                        readBytes(sTicket);
                        sslOffset += hLength;
                        status = Status.PROCESS_SSL_RECORD_CONTENT;
                        break;
                    }
                    case SSL_SERVER_HELLO:
                    case SSL_CLIENT_HELLO:
                    {
                        if (!processTlsHello())
                            return false;
                        break;
                    }
                    default:
                        if (!isAvailable(hLength))
                        {
                            return false;
                        }
                        skip(hLength);
                        sslOffset += hLength;
                        status = Status.PROCESS_SSL_RECORD_CONTENT;
                        break;
                }
                return true;
            }

            public void processStream()
            {
                if (packets == null || processedHandshake())
                    return;
                loop:
                while (!packets.isEmpty() && status != Status.ERROR)
                {
                    switch (status)
                    {
                        case RECEIVED_SYN:
                        {
                            TcpPacket p = packets.first();
                            if (p.getSeqNr() != cSeq)
                            {
                                break loop;
                            }

                            cOffset = p.tcp.getPayloadOffset();
                            status = Status.PROCESS_SSL_HEADER;
                        }
                        case PROCESS_SSL_HEADER:
                        {
                            if (!processTlsHeader())
                                break loop;
                        }
                        case PROCESS_SSL_RECORD_CONTENT:
                        {
                            if (!processTlsRecordContent())
                                break loop;
                            break;
                        }
                        case PROCESS_SSL_HANDSHAKE_BODY:
                        {
                            if (!processTLsHandshakeBody())
                                break loop;
                            break;
                        }
                        case PROCESS_SSL_CERTIFICATES:
                        {
                            if (!isAvailable(3))
                            {
                                break loop;
                            }
                            cLength = read(3);
                            sslOffset += 3;
                            status = Status.PROCESS_SSL_CERTIFICATE;
                        }
                        case PROCESS_SSL_CERTIFICATE:
                        {
                            if (!isAvailable(cLength))
                            {
                                break loop;
                            }
                            byte[] certBuffer = new byte[cLength];
                            readBytes(certBuffer);
                            sslOffset += cLength;
                            sv = identifyServices(certBuffer);
                            status = Status.CERTIFICATE_PROCESSED;
                        }
                        case CERTIFICATE_PROCESSED:
                            int tmp = hLength - cLength - 6;
                            if (!isAvailable(tmp))
                            {
                                break loop;
                            }
                            skip(tmp);
                            sslOffset += tmp;
                            status = Status.PROCESS_SSL_RECORD_CONTENT;
                            break;
                        default:
                            break loop;
                    }
                }
            }

            public boolean error()
            {
                return status == Status.ERROR;
            }

            private boolean isAvailable(int len)
            {
                long seq = cSeq;
                int offset = cOffset;
                Iterator<TcpPacket> iter = packets.iterator();
                TcpPacket p;
                while (iter.hasNext())
                {
                    p = iter.next();
                    if (len <= 0)
                        break;
                    if (offset == -1)
                        offset = p.tcp.getPayloadOffset();
                    if (seq == p.getSeqNr())
                    {
                        len -= (p.getLength() - offset);
                    }
                    else if (seq < p.getSeqNr())
                    {
                        logger.d("TCP out-of-order packet, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                        break;
                    }
                    else
                    {
                        logger.w("This should not happen, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                        status = Status.ERROR;
                        break;
                    }
                    seq = p.getSeqNr() + p.tcp.getPayloadLength();
                    offset = -1;
                }
                return len <= 0 ? true : false;
            }

            /**
             * Skips bytes and updates current tcp packet and offset. Assumes
             * that isAvailable(len) was called before.
             *
             * @param len bytes to skip
             */
            private void skip(int len)
            {
                while (len > 0)
                {
                    TcpPacket p = packets.first();
                    int left = p.getLength() - cOffset;
                    int t = len;
                    len -= left;
                    if (len > 0)
                    {
                        packets.pollFirst();
                        cOffset = packets.first().tcp.getPayloadOffset();
                        cSeq = p.getSeqNr() + p.tcp.getPayloadLength();
                    }
                    else
                    {
                        cOffset += t;
                    }
                }
            }

            private int read(int len)
            {
                int res = 0;
                TcpPacket p = packets.first();
                if (p.getLength() == cOffset)
                {
                    packets.pollFirst();
                    cSeq = p.getSeqNr() + p.tcp.getPayloadLength();
                    p = packets.first();
                    cOffset = p.tcp.getPayloadOffset();
                }
                for (int i = 1; i <= len; i++)
                {
                    res |= p.tcp.getPacket().getUByte(cOffset);
                    res <<= (len - i) * 8;
                    cOffset++;
                    if (cOffset == p.getLength() && i != len)
                    {
                        packets.pollFirst();
                        cSeq = p.getSeqNr() + p.tcp.getPayloadLength();
                        cOffset = packets.first().tcp.getPayloadOffset();
                        p = packets.first();
                    }
                }
                return res;
            }

            private void readBytes(byte[] bytes)
            {
                int len = bytes.length;
                int offset = 0;
                TcpPacket p;
                int l;
                int rLength;
                int rOffset;
                while (len > 0)
                {
                    p = packets.first();
                    l = p.getLength() - cOffset;
                    rOffset = cOffset;
                    if (l >= len)
                    {
                        cOffset += len;
                        rLength = len;
                        len = 0;
                    }
                    else
                    {
                        len -= l;
                        packets.pollFirst();
                        cOffset = packets.first().tcp.getPayloadOffset();
                        rLength = l;
                        cSeq = p.getSeqNr() + p.tcp.getPayloadLength();
                    }
                    p.tcp.getPacket().getByteArray(rOffset, bytes, offset, rLength);
                    offset += rLength;
                }
            }

            private boolean checkVersion(int ver)
            {
                if (ver == SSL3_VERSION || ver == TLS1_VERSION || ver == TLS1_1_VERSION || ver == TLS1_2_VERSION)
                    return true;
                else
                {
                    TcpPacket p = packets.first();
                    logger.d("Got unknown SSL version, Id, Frame: " + p.getIdentifier() + ", " + p.tcp.getPacket().getFrameNumber());
                    return false;
                }
            }

            public boolean isServerStream()
            {
                return src == StreamSource.SERVER;
            }

            public boolean receivedSyn()
            {
                return status.compareTo(Status.RECEIVED_SYN) >= 0;
            }

            public boolean receivedServerSyn()
            {
                return isServerStream() && receivedSyn();
            }

            public boolean processedHandshake()
            {
                return status == Status.PROCESSED_HANDSHAKE;
            }

            public void clearStream()
            {
                packets = null;
            }

            public class TcpPacket implements Comparable<TcpPacket>
            {
                public static final long MAX_SEQ = 0x100000000L;
                public FullTcpPacket tcp;
                private long seq;

                public TcpPacket(FullTcpPacket tcp)
                {
                    this.tcp = tcp;
                    seq = tcp.getSeqNr() - seqStart;
                    if (seq < 0)
                        seq += MAX_SEQ;
                }

                public int getLength()
                {
                    return tcp.getPacket().getPacketWirelen();
                }

                public String getIdentifier()
                {
                    return tcp.getIdentifier();
                }

                @Override
                public int compareTo(TcpPacket o)
                {
                    return Long.compare(getSeqNr(), o.getSeqNr());
                }

                public long getSeqNr()
                {
                    return seq;
                }
            }
        }
    }

    private class Session
    {
        private InetAddress srvAddr;
        private byte[] sessionId;
        private byte[] sessionTicket;

        public Session(byte[] sessionTicket)
        {
            this.sessionTicket = sessionTicket;
            this.sessionId = null;
            this.srvAddr = null;
        }

        public Session(InetAddress serverAddress, byte[] sessionId)
        {
            this.sessionId = sessionId;
            this.srvAddr = serverAddress;
            this.sessionTicket = null;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            // result = prime * result + getOuterType().hashCode();
            result = prime * result + Arrays.hashCode(sessionId);
            result = prime * result + Arrays.hashCode(sessionTicket);
            result = prime * result + ((srvAddr == null) ? 0 : srvAddr.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Session other = (Session) obj;
            /*
             * if (!getOuterType().equals(other.getOuterType())) return false;
             */
            if (!Arrays.equals(sessionId, other.sessionId))
                return false;
            if (!Arrays.equals(sessionTicket, other.sessionTicket))
                return false;
            if (srvAddr == null)
            {
                if (other.srvAddr != null)
                    return false;
            }
            else if (!srvAddr.equals(other.srvAddr))
                return false;
            return true;
        }
    }
}
