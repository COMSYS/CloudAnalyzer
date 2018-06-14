package de.rwth.comsys.cloudanalyzer.information;

import de.rwth.comsys.cloudanalyzer.network.NetworkProtocol;
import de.rwth.comsys.cloudanalyzer.network.Packet;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class DnsPacket extends AbstractInformation
{

    public static final int PRIORITY = 6;
    public static final String TYPE = "DnsPacket";
    private static final long serialVersionUID = 783196247889879625L;
    private static int BYTE = 8;
    private String id;
    private Packet packet;
    private int dnsId;
    private int flags;
    private byte RCODE;
    private byte Z;
    private byte RA;
    private byte RD;
    private byte TC;
    private byte AA;
    private byte OPCODE;
    private byte QR;
    private int qcount;
    private int ancount;
    private int nscount;
    private int arcount;
    private int answersOffset;
    private int authorityRecordsOffset;
    private int additionalRecordsOffset;
    private boolean tcp;
    private int headerOffset;
    private DnsQuestion[] questions;
    private ResourceRecord[] answers;
    private ResourceRecord[] authorityRecords;
    private ResourceRecord[] additionalRecords;
    public DnsPacket(Packet packet, String id)
    {
        super(TYPE, PRIORITY);
        this.id = id;
        this.packet = packet;
        decodeHeader();
        packet.addProtocolInformation(NetworkProtocol.DNS, this);
    }

    @Override
    public String getIdentifier()
    {
        return id;
    }

    public int getDnsId()
    {
        return dnsId;
    }

    public boolean isResponse()
    {
        return QR == 1;
    }

    public boolean isQuery()
    {
        return QR == 0;
    }

    public int flags()
    {
        return flags;
    }

    public int qr()
    {
        return QR;
    }

    public String qrDescription()
    {
        return (isQuery() ? "query" : "response");
    }

    public int opcode()
    {
        return OPCODE;
    }

    public int aa()
    {
        return AA;
    }

    public int tc()
    {
        return TC;
    }

    public int rd()
    {
        return RD;
    }

    public int ra()
    {
        return RA;
    }

    public int z()
    {
        return Z;
    }

    public int rcode()
    {
        return RCODE;
    }

    public String rcodeDescription()
    {
        return (rcode() >= ResponseCode.values().length ? "Unkown Response Code" : ResponseCode.values()[rcode()].toString());
    }

    public int qcount()
    {
        return qcount;
    }

    public int ancount()
    {
        return ancount;
    }

    public int nscount()
    {
        return nscount;
    }

    public int arcount()
    {
        return arcount;
    }

    public DnsQuestion[] dnsQuestions()
    {
        return questions;
    }

    public int dnsQuestionsLength()
    {
        int length = 0;
        for (DnsQuestion q : questions)
            length += q.getLength();
        return length * BYTE;
    }

    public ResourceRecord[] dnsAnswers()
    {
        return answers;
    }

    public int dnsAnswersOffset()
    {
        return answersOffset * BYTE;
    }

    public int dnsAnswersLength()
    {
        int length = 0;
        for (ResourceRecord a : answers)
            length += a.getLength();
        return length * BYTE;
    }

    public ResourceRecord[] dnsAuthorityRecords()
    {
        return authorityRecords;
    }

    public int dnsAuthorityRecordsOffset()
    {
        return authorityRecordsOffset * BYTE;
    }

    public int dnsAuthorityRecordsLength()
    {
        int length = 0;
        for (ResourceRecord a : authorityRecords)
            length += a.getLength();
        return length * BYTE;
    }

    public ResourceRecord[] dnsAdditionalRecords()
    {
        return additionalRecords;
    }

    public int dnsAdditionalRecordsOffset()
    {
        return additionalRecordsOffset * BYTE;
    }

    public int dnsAdditionalRecordsLength()
    {
        int length = 0;
        for (ResourceRecord a : additionalRecords)
            length += a.getLength();
        return length * BYTE;
    }

    public Packet getPacket()
    {
        return packet;
    }

    private void decodeHeader()
    {
        tcp = (packet.hasHeader(NetworkProtocol.TCP));
        int headerIndex = packet.findHeaderIndex(NetworkProtocol.DNS);
        headerOffset = packet.getHeaderOffsetByIndex(headerIndex);
        int index = getCorrectOffset(0);
        dnsId = packet.getUShort(index);
        flags = packet.getUShort(index + 2);
        RCODE = (byte) (flags & 0b1111);
        Z = (byte) ((flags >>>= 4) & 0b111);
        RA = (byte) ((flags >>>= 3) & 0b1);
        RD = (byte) ((flags >>>= 1) & 0b1);
        TC = (byte) ((flags >>>= 1) & 0b1);
        AA = (byte) ((flags >>>= 1) & 0b1);
        OPCODE = (byte) ((flags >>>= 1) & 0b1111);
        QR = (byte) ((flags >>>= 4) & 0b1);

        qcount = packet.getUShort(index + 4);
        ancount = packet.getUShort(index + 6);
        nscount = packet.getUShort(index + 8);
        arcount = packet.getUShort(index + 10);

        questions = new DnsQuestion[qcount];
        answers = new ResourceRecord[ancount];
        authorityRecords = new ResourceRecord[nscount];
        additionalRecords = new ResourceRecord[arcount];

        index += 12;
        int tindex;
        //Questions
        for (int q = 0; q < qcount; q++)
        {
            tindex = index;
            StringBuilder domain = new StringBuilder();
            index = getDomainName(index, domain);
            questions[q] = new DnsQuestion(domain.toString(), DnsType.getDnsTypeFromId(packet.getUShort(index)), DnsClass.getDnsClassFromId(packet.getUShort(index + 2)), index - tindex + 4);
            index += 4;
        }
        answersOffset = index;
        //Answers (Resource Records)
        for (int a = 0; a < ancount; a++)
        {
            answers[a] = getNextResourceRecord(index);
            index += answers[a].getLength();
        }
        authorityRecordsOffset = index;
        //Name Servers (Resource Records)
        for (int ns = 0; ns < nscount; ns++)
        {
            authorityRecords[ns] = getNextResourceRecord(index);
            index += authorityRecords[ns].getLength();
        }
        additionalRecordsOffset = index;
        //Additional Records (Resource Records)
        for (int a = 0; a < arcount; a++)
        {
            additionalRecords[a] = getNextResourceRecord(index);
            index += additionalRecords[a].getLength();
        }
    }

    private int getCorrectOffset(int offset)
    {
        return headerOffset + (tcp ? offset + 2 : offset);
    }

    private ResourceRecord getNextResourceRecord(int index)
    {
        int tindex = index;
        StringBuilder domain = new StringBuilder();
        index = getDomainName(index, domain);
        int itype = packet.getUShort(index);
        DnsType type = DnsType.getDnsTypeFromId(itype);
        DnsClass dnsclass = DnsClass.getDnsClassFromId(packet.getUShort(index + 2));
        long ttl = packet.getUInt(index + 4);
        int rdlength = packet.getUShort(index + 8);
        index += 10;
        int length = (index - tindex) + rdlength;
        ResourceRecord rr;
        if (rdlength <= 0)
        {
            rr = new ResourceRecord(domain.toString(), type, dnsclass, ttl, rdlength, length);
            return rr;
        }
        switch (type)
        {
            case A:
            case AAAA:
                InetAddress addr = getInetAddress(index, rdlength);
                rr = new AddressRR(domain.toString(), type, dnsclass, ttl, rdlength, addr, length);
                break;
            case NS:
                StringBuilder ns = new StringBuilder();
                getDomainName(index, ns);
                rr = new StringRR(domain.toString(), type, dnsclass, ttl, rdlength, ns.toString(), length);
                break;
            case CNAME:
                StringBuilder cname = new StringBuilder();
                getDomainName(index, cname);
                rr = new StringRR(domain.toString(), type, dnsclass, ttl, rdlength, cname.toString(), length);
                break;
            case SOA:
                StringBuilder sb = new StringBuilder();
                index = getDomainName(index, sb);
                String mname = sb.toString();
                sb.delete(0, sb.length());
                index = getDomainName(index, sb);
                String rname = sb.toString();
                long serial = packet.getUInt(index);
                long refresh = packet.getUInt(index + 4);
                long retry = packet.getUInt(index + 8);
                long expire = packet.getUInt(index + 12);
                long minimum = packet.getUInt(index + 16);
                rr = new SOA_RR(domain.toString(), type, dnsclass, ttl, rdlength, mname, rname, serial, refresh, retry, expire, minimum, length);
                break;
            case PTR:
                StringBuilder ptr = new StringBuilder();
                getDomainName(index, ptr);
                rr = new StringRR(domain.toString(), type, dnsclass, ttl, rdlength, ptr.toString(), length);
                break;
            case MX:
                int preference = packet.getUShort(index);
                StringBuilder ex = new StringBuilder();
                getDomainName(index + 2, ex);
                rr = new MailExchangeRR(domain.toString(), type, dnsclass, ttl, rdlength, preference, ex.toString(), length);
                break;
            default:
                rr = new ResourceRecord(domain.toString(), type, dnsclass, ttl, rdlength, length);
                //System.err.println("Unknown DNS Type: " + itype);
                break;
        }
        return rr;
    }

    private int getDomainName(int index, StringBuilder domain)
    {
        int nlen;
        boolean pointer;
        do
        {
            nlen = packet.getUByte(index++);
            pointer = ((nlen & 0xc0) == 0xc0);
            if (pointer)
            {
                int tindex = getCorrectOffset((packet.getUShort(index - 1) & 0x3fff));
                getDomainName(tindex, domain);
                nlen = 0;
                index++;
            }
            else
            {
                if (nlen != 0 && domain.length() > 0)
                    domain.append('.');
                domain.append(packet.getString(index, nlen));
                index += nlen;
            }
        }
        while (nlen != 0);

        return index;
    }

    private InetAddress getInetAddress(int index, int size)
    {
        try
        {
            return InetAddress.getByAddress(packet.getByteArray(index, size));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public enum ResponseCode
    {
        NO_ERROR("No Error"), FORMAT_ERROR("Format Error"), SERVER_FAILURE("Server Failure"), NAME_ERROR("Name Error"), NOT_IMPLEMENTED("Not Implemented"), REFUSED("Refused");

        private String desc;

        ResponseCode(String desc)
        {
            this.desc = desc;
        }

        public String getDescription()
        {
            return desc;
        }

        @Override
        public String toString()
        {
            return getDescription();
        }
    }

    public enum DnsType
    {
        UNKNOWN(0, "Unknown type"), A(1, "1 IPv4 Host Address (A)"), NS(2, "2 Name Server (NS)"), CNAME(5, "5 Canonical Name"), SOA(6, "6 Start of zone of authority (SOA)"), PTR(12, "12 Pointer (PTR)"), MX(15, "15 Mail Exchange (MX)"), AAAA(28, "28 IPv6 Host Address (AAAA)");

        private static final Map<Integer, DnsType> intToType = new HashMap<>();

        static
        {
            for (DnsType type : values())
                intToType.put(type.getId(), type);
        }

        private int id;
        private String desc;
        DnsType(int id, String desc)
        {
            this.id = id;
            this.desc = desc;
        }

        public static DnsType getDnsTypeFromId(int id)
        {
            return (intToType.containsKey(id) ? intToType.get(id) : UNKNOWN);
        }

        public int getId()
        {
            return id;
        }

        public String getDescription()
        {
            return desc;
        }

        @Override
        public String toString()
        {
            return getDescription();
        }
    }

    public enum DnsClass
    {
        UNKNOWN(0, "Unknown class"), IN(1, "1 Internet"), CS(2, "2 CSNET"), CH(3, "3 CHAOS"), HS(4, "4 Hesiod");

        private static final Map<Integer, DnsClass> intToClass = new HashMap<>();

        static
        {
            for (DnsClass dnsclass : values())
                intToClass.put(dnsclass.getId(), dnsclass);
        }

        private int id;
        private String desc;
        DnsClass(int id, String desc)
        {
            this.id = id;
            this.desc = desc;
        }

        public static DnsClass getDnsClassFromId(int id)
        {
            return (intToClass.containsKey(id) ? intToClass.get(id) : UNKNOWN);
        }

        public int getId()
        {
            return id;
        }

        public String getDescription()
        {
            return desc;
        }

        @Override
        public String toString()
        {
            return getDescription();
        }
    }

    public class DnsQuestion
    {
        private String name;
        private DnsType type;
        private DnsClass dnsclass;
        private int length;

        public DnsQuestion(String name, DnsType type, DnsClass dnsclass, int length)
        {
            this.name = name;
            this.type = type;
            this.dnsclass = dnsclass;
            this.length = length;
        }

        public String getName()
        {
            return name;
        }

        public DnsType getType()
        {
            return type;
        }

        public DnsClass getDnsClass()
        {
            return dnsclass;
        }

        public int getLength()
        {
            return length;
        }

        @Override
        public String toString()
        {
            return "Name: " + name + "    Type: " + type + "    Class: " + dnsclass;
        }
    }

    public class ResourceRecord
    {
        protected String name;
        protected DnsType type;
        protected DnsClass dnsclass;
        protected long ttl;
        protected int rdlength;
        protected int length;

        public ResourceRecord(String name, DnsType type, DnsClass dnsclass, long ttl, int rdlength, int length)
        {
            this.name = name;
            this.type = type;
            this.dnsclass = dnsclass;
            this.ttl = ttl;
            this.rdlength = rdlength;
            this.length = length;
        }

        public String getName()
        {
            return name;
        }

        public DnsType getType()
        {
            return type;
        }

        public DnsClass getDnsClass()
        {
            return dnsclass;
        }

        public long getTTL()
        {
            return ttl;
        }

        public int getRDLength()
        {
            return rdlength;
        }

        public int getLength()
        {
            return length;
        }

        @Override
        public String toString()
        {
            return "Name: " + name + "    Type: " + type + "    Class: " + dnsclass + "    TTL: " + ttl + "    RDATA: unknown type";
        }
    }

    public class AddressRR extends ResourceRecord
    {

        private InetAddress addr;

        public AddressRR(String name, DnsType type, DnsClass dnsclass, long ttl, int rdlength, InetAddress addr, int length)
        {
            super(name, type, dnsclass, ttl, rdlength, length);
            this.addr = addr;
        }

        @Override
        public String toString()
        {
            return "Name: " + name + "    Type: " + type + "    Class: " + dnsclass + "    TTL: " + ttl + "    RDATA: " + addr.getHostAddress();
        }

        public InetAddress getAddress()
        {
            return addr;
        }
    }

    public class StringRR extends ResourceRecord
    {

        private String rdata;

        public StringRR(String name, DnsType type, DnsClass dnsclass, long ttl, int rdlength, String rdata, int length)
        {
            super(name, type, dnsclass, ttl, rdlength, length);
            this.rdata = rdata;
        }

        @Override
        public String toString()
        {
            return "Name: " + name + "    Type: " + type + "    Class: " + dnsclass + "    TTL: " + ttl + "    RDATA: " + rdata;
        }

        public String getString()
        {
            return rdata;
        }
    }

    public class MailExchangeRR extends ResourceRecord
    {

        private String exchange;
        private int preference;

        public MailExchangeRR(String name, DnsType type, DnsClass dnsclass, long ttl, int rdlength, int preference, String exchange, int length)
        {
            super(name, type, dnsclass, ttl, rdlength, length);
            this.exchange = exchange;
            this.preference = preference;
        }

        @Override
        public String toString()
        {
            return "Name: " + name + "    Type: " + type + "    Class: " + dnsclass + "    TTL: " + ttl + "    Preference: " + preference + "    Exchange: " + exchange;
        }

        public String getExchange()
        {
            return exchange;
        }

        public int getPreference()
        {
            return preference;
        }
    }

    public class SOA_RR extends ResourceRecord
    {

        private String mname;
        private String rname;
        private long serial;
        private long refresh;
        private long retry;
        private long expire;
        private long minimum;

        public SOA_RR(String name, DnsType type, DnsClass dnsclass, long ttl, int rdlength, String mname, String rname, long serial, long refresh, long retry, long expire, long minimum, int length)
        {
            super(name, type, dnsclass, ttl, rdlength, length);
            this.mname = mname;
            this.rname = rname;
            this.serial = serial;
            this.refresh = refresh;
            this.retry = retry;
            this.expire = expire;
            this.minimum = minimum;
        }

        @Override
        public String toString()
        {
            return "Name: " + name + "    Type: " + type + "    Class: " + dnsclass + "    TTL: " + ttl + "    Primary NS: " + mname + "    Mailbox: " + rname + "    Serial: " + serial + "    Refresh Interval: " + refresh + "    Retry Interval: " + retry + "    Expire: " + expire + "    Minimum TTL: " + minimum;
        }

        public String getPrimaryNS()
        {
            return mname;
        }

        public String getMailbox()
        {
            return rname;
        }

        public long getSerial()
        {
            return serial;
        }

        public long getRefresh()
        {
            return refresh;
        }

        public long getRetry()
        {
            return retry;
        }

        public long getExpire()
        {
            return expire;
        }

        public long getMinimumTTL()
        {
            return minimum;
        }
    }
}
