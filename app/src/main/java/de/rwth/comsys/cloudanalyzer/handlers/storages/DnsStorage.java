package de.rwth.comsys.cloudanalyzer.handlers.storages;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket.AddressRR;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket.ResourceRecord;
import de.rwth.comsys.cloudanalyzer.information.DnsPacket.StringRR;
import de.rwth.comsys.cloudanalyzer.information.DnsRRInformation;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.util.*;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class DnsStorage extends AbstractInformationStorage
{

    private static final String[] supportedHTypes = {"DnsRRInformation"};
    private static final int VERTEX_ADDR = 0;
    private static final int VERTEX_NAME = 1;
    private static Logger logger = Logger.getLogger(DnsStorage.class.getName());
    private static int INIT_CAPACITY = 1000;
    private HashMap<String, DnsVertex> vertices;
    private HashMap<Service, RegexList> services;
    private DatabaseConnection dbConn;
    private PrintWriter niDnWriter;
    private boolean printNiDomainNames;
    private SqlBatchExecuteThread exThread;
    private int bufferThreshold;
    private int bufferSize;
    private boolean storeDnsGraph;
    private long cleanupInterval = -1;
    private Date lastCleanupCheck;
    public DnsStorage()
    {
        services = new HashMap<>();
        vertices = new HashMap<>(INIT_CAPACITY);
    }

    public void saveDName(final AssignedServices name) throws InterruptedException
    {
        if (!storeDnsGraph)
            return;

        exThread.setStatement(0, new Consumer<DatabaseStatement>()
        {
            @Override
            public void accept(DatabaseStatement stSaveDName)
            {
                try
                {
                    for (ServiceProperties sp : name.getServiceProperties())
                    {
                        stSaveDName.setString(1, name.getAssignedTo());
                        stSaveDName.setInt(2, sp.getService().getId());
                        stSaveDName.setInt(3, sp.getRegion());
                        stSaveDName.execute();
                    }
                }
                catch (SQLException e)
                {
                    logger.w(e.toString());
                }
            }
        });
    }

    public void deleteDName(final String key) throws SQLException, InterruptedException
    {
        if (!storeDnsGraph)
            return;

        exThread.setStatement(3, new Consumer<DatabaseStatement>()
        {
            @Override
            public void accept(DatabaseStatement stmt)
            {
                try
                {
                    stmt.setString(1, key);
                    stmt.execute();
                }
                catch (SQLException e)
                {
                    logger.w(e.toString());
                }
            }
        });
    }

    public boolean addRegexes(String file)
    {
        String path = MainHandler.getDataDir() + file;
        return RegexXMLHandler.parseXML(path, services, true);
    }

    @SuppressWarnings("unused")
    private void addResourceRecord(DnsRRInformation rri)
    {
        Date cDate = rri.getCaptureDate();
        ResourceRecord rr = rri.getResourceRecord();
        DnsVertex source = null;
        DnsVertex target = null;

        switch (rr.getType())
        {
            case CNAME:
            {
                StringRR srr = (StringRR) rr;
                target = getNameVertex(rr.getName());
                source = getNameVertex(srr.getString());
                break;
            }
            case A:
            case AAAA:
            {
                AddressRR arr = (AddressRR) rr;
                source = getAddressVertex(arr.getAddress());
                target = getNameVertex(rr.getName());
                break;
            }
            case PTR:
            {
                String str = rr.getName();
                if (str.matches("([0-9]{1,3}\\.){4}in-addr.arpa") || str.matches("([\\w^_]\\.){32}ip6.arpa"))
                {
                    InetAddress address = parsePtrAddress(str);
                    source = getAddressVertex(address);
                    StringRR srr = (StringRR) rr;
                    target = getNameVertex(srr.getString());
                }
                break;
            }
            default:
                logger.w("Received unsupported ResourceRecord with type: " + rr.getType().getDescription());
                break;
        }

        TTL ttl = new TTL(cDate, rr.getTTL());
        addEdge(source, target, ttl);

    }

    private InetAddress parsePtrAddress(String str)
    {
        InetAddress address = null;
        String[] tmp = str.split("\\.");
        boolean ip4 = (tmp.length == 6);
        StringBuilder addr = new StringBuilder();
        for (int i = tmp.length - 3; i >= 0; i--)
        {
            addr.append(tmp[i]);
            if (i > 0)
            {
                if (ip4)
                    addr.append(".");
                else if (i % 4 == 0)
                {
                    addr.append(":");
                }
            }
        }

        try
        {
            address = InetAddress.getByName(addr.toString());
        }
        catch (UnknownHostException e)
        {
            logger.w(e.toString());
        }

        return address;
    }

    private void addEdge(DnsVertex source, DnsVertex target, TTL ttl)
    {
        if (source != null && target != null)
        {
            DnsEdge e = new DnsEdge();
            e.ttl = ttl;
            e.to = target;

            if (source.edges.add(e))
            {
                try
                {
                    e.saveToDb(source);
                }
                catch (InterruptedException e1)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private DnsVertex getNameVertex(String name)
    {
        DnsVertex v = null;
        if (!name.equals(""))
        {
            v = vertices.get(name);
            if (v == null)
            {
                v = new DnsVertex(VERTEX_NAME);
                v.value = getAssignedServices(name);
                saveNewVertex(v);
            }
        }
        return v;
    }

    private DnsVertex getAddressVertex(InetAddress addr)
    {
        DnsVertex v = null;
        if (addr != null)
        {
            v = vertices.get(addr.getHostAddress());
            if (v == null)
            {
                v = new DnsVertex(VERTEX_ADDR);
                v.value = addr;
                saveNewVertex(v);
            }
        }
        return v;
    }

    private void saveNewVertex(DnsVertex v)
    {
        vertices.put(v.getId(), v);
        try
        {
            v.saveToDb();
        }
        catch (SQLException e)
        {
            logger.w(e.toString());
        }
        catch (InterruptedException e1)
        {
            Thread.currentThread().interrupt();
        }
    }

    public DnsInformation getDnsInformation(InetAddress addr, Date date)
    {
        DnsInformation res = null;
        DnsVertex v = vertices.get(addr.getHostAddress());
        if (v != null)
        {
            List<AssignedServices> names = new LinkedList<>();
            getAssignedServices(names, v, date);
            res = new DnsInformation(addr, names);
        }

        return res;
    }

    private void getAssignedServices(List<AssignedServices> names, DnsVertex start, Date date)
    {
        LinkedList<DnsVertex> stack = new LinkedList<>();
        HashSet<DnsVertex> processed = new HashSet<>();
        stack.push(start);

        while (!stack.isEmpty())
        {
            DnsVertex v = stack.pop();
            processed.add(v);
            for (DnsEdge e : v.edges)
            {
                if (!processed.contains(e.to))
                {
                    if (e.ttl.isAvailable(date))
                    {
                        stack.push(e.to);
                    }
                }
                else
                {
                    if (e.ttl.isAvailable(date))
                    {
                        continue;
                    }
                    else
                        break;
                }
            }
            if (v.isNameVertex())
            {
                AssignedServices n = (AssignedServices) v.value;
                names.add(n);
            }
        }
    }

    public AssignedServices getAssignedServices(String name)
    {
        AssignedServices n = new AssignedServices(name);
        for (Entry<Service, RegexList> s : services.entrySet())
        {
            RegexList.Regex dr = s.getValue().matches(name);
            if (dr != null)
            {
                ServiceProperties sprops = new ServiceProperties(s.getKey());
                sprops.setRegion(dr.getRegion());
                n.getServiceProperties().add(sprops);
            }
        }
        if (printNiDomainNames && n.getServiceProperties().size() == 0)
            niDnWriter.println(name);
        return n;
    }

    @Override
    public void resetHandler()
    {
        String file = "storage/dbSQL/dnsStorage.sql";
        boolean trunning = (exThread != null && exThread.isAlive());
        if (trunning)
            waitForThread();
        try
        {
            dbConn.executeFile(MainHandler.getAssets().open(file));
            services = new HashMap<>();
            vertices = new HashMap<>(INIT_CAPACITY);
        }
        catch (SQLException | IOException e)
        {
            logger.e(e.toString());
        }
        if (trunning)
        {
            prepareThread();
            exThread.start();
        }
    }

    private void waitForThread()
    {
        exThread.exit();
        try
        {
            exThread.join();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * @Override public Information getInformation(String type, String
     * identifier) { return null; }
     */

    private void prepareThread()
    {
        String sql1 = "INSERT INTO dns_dnames(name, service, region) VALUES(?, ?, ?)";
        String sql2 = "INSERT INTO dns_vertices(id, type) VALUES(?, ?)";
        String sql3 = "INSERT INTO dns_edges(`from`, `to`, ttl_since, ttl) VALUES(?, ?, ?, ?)";
        String sql4 = "DELETE FROM dns_dnames WHERE name = ?";
        String sql5 = "DELETE FROM dns_vertices WHERE id = ?";
        String sql6 = "DELETE FROM dns_edges WHERE `from` = ? AND `to` = ? AND ttl_since = ? AND ttl = ?";
        exThread = new SqlBatchExecuteThread(bufferThreshold, bufferSize, 60, dbConn, sql1, sql2, sql3, sql4, sql5, sql6);
    }

    private void cleanUpGraph(Date now)
    {
        if (lastCleanupCheck == null)
        {
            lastCleanupCheck = now;
            return;
        }
        if (now.getTime() - lastCleanupCheck.getTime() >= cleanupInterval)
        {
            HashMap<String, DnsVertex> newVertexMap = new HashMap<>(vertices.size());
            for (Entry<String, DnsVertex> e : vertices.entrySet())
            {
                DnsVertex vertex = e.getValue();
                List<DnsEdge> edgesToBeDeleted = new ArrayList<>();
                for (DnsEdge edge : vertex.edges)
                {
                    if (edge.ttl.getRemainingTTL(now) == 0)
                        edgesToBeDeleted.add(edge);
                    else if (edge.to.isNameVertex())
                        newVertexMap.put(edge.to.getId(), edge.to);
                }
                for (DnsEdge edge : edgesToBeDeleted)
                {
                    vertex.edges.remove(edge);
                    try
                    {
                        edge.deleteFromDb(vertex);
                    }
                    catch (SQLException e1)
                    {
                        logger.w(e1.toString());
                    }
                    catch (InterruptedException e2)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
                if (vertex.isAddrVertex() && !vertex.edges.isEmpty())
                    newVertexMap.put(vertex.getId(), vertex);

            }

            for (Entry<String, DnsVertex> e : vertices.entrySet())
            {
                if (!newVertexMap.containsKey(e.getKey()))
                {
                    try
                    {
                        e.getValue().deleteFromDb();
                    }
                    catch (SQLException e1)
                    {
                        logger.w(e1.toString());
                    }
                    catch (InterruptedException e2)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            logger.d("Graph cleanup finished. Removed " + (vertices.size() - newVertexMap.size()) + " vertices.");
            vertices = newVertexMap;
            lastCleanupCheck = now;
        }
    }

    @Override
    public List<Information> processInformation(Information i)
    {
        addResourceRecord((DnsRRInformation) i);
        if (cleanupInterval >= 0)
        {
            Date now = ((DnsRRInformation) i).getCaptureDate();
            cleanUpGraph(now);
        }
        return null;
    }

    @Override
    public String[] getSupportedHInformationTypes()
    {
        return supportedHTypes;
    }

    @Override
    public boolean setupStorage(DatabaseConnection dbConn, boolean reset)
    {
        boolean res = true;
        this.dbConn = dbConn;
        printNiDomainNames = Boolean.valueOf(MainHandler.getProperties().getProperty("DnsStorage.print_unknown_domain_names", String.valueOf(false)));
        bufferThreshold = Integer.valueOf(MainHandler.getProperties().getProperty("DnsStorage.buffer_threshold", "100"));
        bufferSize = Integer.valueOf(MainHandler.getProperties().getProperty("DnsStorage.buffer_size", "10000"));
        storeDnsGraph = Boolean.parseBoolean(MainHandler.getProperties().getProperty("DnsStorage.store_dns_graph", "true"));
        cleanupInterval = Long.parseLong(MainHandler.getProperties().getProperty("DnsStorage.cleanup_interval", String.valueOf(cleanupInterval)));

        if (reset)
        {
            resetHandler();
        }
        if (!loadVerticesFromDb() || !loadEdgesFromDb())
        {
            res = false;
        }
        prepareThread();

        if (printNiDomainNames)
        {
            try
            {
                niDnWriter = new PrintWriter(new FileWriter("niDomainNames.txt", true));
            }
            catch (IOException e)
            {
                logger.w(e.toString());
            }
        }

        exThread.setName("DnsStorageThread");
        exThread.start();

        return res;
    }

    private boolean loadVerticesFromDb()
    {
        boolean res = true;
        try (DatabaseStatement s = dbConn.createStatement("SELECT COUNT(*) FROM dns_vertices"); DatabaseStatement s2 = dbConn.createStatement("SELECT service, region FROM dns_dnames WHERE name = ?"); DatabaseStatement s3 = dbConn.createStatement("SELECT id, type FROM dns_vertices"))
        {
            int c = INIT_CAPACITY;
            DatabaseResultSet rs = s.executeQuery();
            if (rs.next())
            {
                int tmp = rs.getInt(1);
                tmp *= 1.0 / 0.6;
                if (tmp > c)
                    c = tmp;
            }
            rs.close();
            vertices = new HashMap<>(c);
            rs = s3.executeQuery();
            while (rs.next())
            {
                String id = rs.getString(1);
                int type = rs.getInt(2);
                Object o;
                if (type == VERTEX_NAME)
                {
                    s2.setString(1, id);
                    DatabaseResultSet rs2 = s2.executeQuery();
                    AssignedServices n = new AssignedServices(id);
                    o = n;
                    while (rs2.next())
                    {
                        int srv = rs2.getInt(1);
                        int region = rs2.getInt(2);
                        ServiceProperties sp = new ServiceProperties(MainHandler.getService(srv));
                        sp.setRegion(region);
                        n.getServiceProperties().add(sp);
                    }
                }
                else
                {
                    try
                    {
                        o = InetAddress.getByName(id);
                    }
                    catch (UnknownHostException e)
                    {
                        logger.e(e.toString());
                        break;
                    }
                }
                DnsVertex v = new DnsVertex(type);
                v.value = o;
                vertices.put(id, v);
            }
        }
        catch (SQLException e)
        {
            logger.e(e.toString());
            res = false;
        }
        return res;
    }

    private boolean loadEdgesFromDb()
    {
        boolean res = true;
        String sql = "SELECT `from`, `to`, ttl_since, ttl FROM dns_edges WHERE `from` = ?";
        try (DatabaseStatement s = dbConn.createStatement(sql))
        {
            for (DnsVertex v : vertices.values())
            {
                s.setString(1, v.getId());
                DatabaseResultSet rs = s.executeQuery();
                while (rs.next())
                {
                    DnsVertex from = vertices.get(rs.getString(1));
                    DnsVertex to = vertices.get(rs.getString(2));
                    Date since = new Date(rs.getLong(3));
                    long ttl = rs.getLong(4);
                    DnsEdge e = new DnsEdge();
                    e.ttl = new TTL(since, ttl);
                    e.to = to;
                    from.edges.add(e);
                }
            }
        }
        catch (SQLException e)
        {
            logger.e(e.toString());
            res = false;
        }
        return res;
    }

    @Override
    public void shutdownHandler()
    {
        exThread.exit();
        try
        {
            exThread.join();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        if (niDnWriter != null)
            niDnWriter.close();
    }

    @Override
    public void deletePersonalData()
    {
        resetHandler();
    }

    public void analyzeDomains(PrintStream out)
    {
        for (DnsVertex v : vertices.values())
        {
            if (!v.isAddrVertex())
                continue;
            InetAddress addr = (InetAddress) v.value;
            List<Subnet> subnets = new LinkedList<>();
            MainHandler.getHandlerByClass(IpRangeStorage.class).getSubnets(addr, subnets);
            boolean interesting = !subnets.isEmpty();
            List<AssignedServices> as = new LinkedList<>();
            getAssignedServices(as, v, null);
            if (!interesting)
            {
                for (AssignedServices s : as)
                {
                    if (!s.getServiceProperties().isEmpty())
                    {
                        interesting = true;
                        break;
                    }
                }
            }
            if (interesting)
            {
                for (AssignedServices s : as)
                {
                    if (s.getServiceProperties().isEmpty())
                    {
                        printInterestingDN(out, s.getAssignedTo(), subnets, as);
                    }
                }
            }
        }
    }

    private void printInterestingDN(PrintStream out, String dn, List<Subnet> subnets, List<AssignedServices> as)
    {
        out.println("Domain name: " + dn);
        out.println("  Subnets:");
        for (Subnet sn : subnets)
        {
            for (Service s : sn.getServices())
            {
                out.println("    " + s.getName() + " " + sn.toString());
            }
        }
        out.println("  Related domain names:");
        HashSet<String> h = new HashSet<>();
        for (AssignedServices s : as)
        {
            String str = s.getAssignedTo().toLowerCase();
            if (!s.getServiceProperties().isEmpty() && !h.contains(str))
            {
                out.println("    " + str);
                h.add(str);
            }
        }
        out.println("");
    }

    private class DnsVertex
    {
        public TreeSet<DnsEdge> edges;
        public Object value;
        private int type;

        public DnsVertex(int type)
        {
            this.type = type;
            edges = new TreeSet<>();
        }

        public int getType()
        {
            return type;
        }

        public boolean isAddrVertex()
        {
            return type == VERTEX_ADDR;
        }

        public boolean isNameVertex()
        {
            return type == VERTEX_NAME;
        }

        public String getId()
        {
            return (isNameVertex() ? ((AssignedServices) value).getAssignedTo() : ((InetAddress) value).getHostAddress());
        }

        public void saveToDb() throws SQLException, InterruptedException
        {
            if (!storeDnsGraph)
                return;

            if (isNameVertex())
            {
                AssignedServices n = (AssignedServices) value;
                saveDName(n);
            }
            exThread.setStatement(1, new Consumer<DatabaseStatement>()
            {
                @Override
                public void accept(DatabaseStatement stSaveVertex)
                {
                    try
                    {
                        stSaveVertex.setString(1, getId());
                        stSaveVertex.setInt(2, getType());
                        stSaveVertex.execute();
                    }
                    catch (SQLException e)
                    {
                        logger.w(e.toString());
                    }
                }
            });
        }

        public void deleteFromDb() throws SQLException, InterruptedException
        {
            if (!storeDnsGraph)
                return;

            if (isNameVertex())
                deleteDName(getId());

            for (DnsEdge edge : edges)
                edge.deleteFromDb(this);

            exThread.setStatement(4, new Consumer<DatabaseStatement>()
            {
                @Override
                public void accept(DatabaseStatement stmt)
                {
                    try
                    {
                        stmt.setString(1, getId());
                        stmt.execute();
                    }
                    catch (SQLException e)
                    {
                        logger.w(e.toString());
                    }
                }
            });
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + type;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
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
            DnsVertex other = (DnsVertex) obj;
            if (type != other.type)
                return false;
            if (value == null)
            {
                if (other.value != null)
                    return false;
            }
            else if (!value.equals(other.value))
                return false;
            return true;
        }
    }

    private class DnsEdge implements Comparable<DnsEdge>
    {
        public TTL ttl;
        public DnsVertex to;

        @Override
        public int compareTo(DnsEdge other)
        {
            int c = other.ttl.compareTo(ttl);
            return c == 0 ? other.to.getId().compareTo(to.getId()) : c;
        }

        public void saveToDb(final DnsVertex from) throws InterruptedException
        {
            if (!storeDnsGraph)
                return;

            exThread.setStatement(2, new Consumer<DatabaseStatement>()
            {
                @Override
                public void accept(DatabaseStatement stSaveEdge)
                {
                    try
                    {
                        stSaveEdge.setString(1, from.getId());
                        stSaveEdge.setString(2, to.getId());
                        stSaveEdge.setLong(3, ttl.getStartDate().getTime());
                        stSaveEdge.setLong(4, ttl.getTTL());
                        stSaveEdge.execute();
                    }
                    catch (SQLException e)
                    {
                        logger.w(e.toString());
                    }
                }
            });
        }

        public void deleteFromDb(final DnsVertex from) throws SQLException, InterruptedException
        {
            if (!storeDnsGraph)
                return;

            exThread.setStatement(5, new Consumer<DatabaseStatement>()
            {
                @Override
                public void accept(DatabaseStatement stmt)
                {
                    try
                    {
                        stmt.setString(1, from.getId());
                        stmt.setString(2, to.getId());
                        stmt.setLong(3, ttl.getStartDate().getTime());
                        stmt.setLong(4, ttl.getTTL());
                        stmt.execute();
                    }
                    catch (SQLException e)
                    {
                        logger.w(e.toString());
                    }
                }
            });
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((ttl == null) ? 0 : ttl.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
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
            DnsEdge other = (DnsEdge) obj;
            if (ttl == null)
            {
                if (other.ttl != null)
                    return false;
            }
            else if (!ttl.equals(other.ttl))
                return false;
            if (to == null)
            {
                if (other.to != null)
                    return false;
            }
            else if (!to.equals(other.to))
                return false;
            return true;
        }
    }
}
