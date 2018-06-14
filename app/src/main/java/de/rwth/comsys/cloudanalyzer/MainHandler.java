package de.rwth.comsys.cloudanalyzer;

import android.content.Context;
import android.content.res.AssetManager;
import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.database.DatabaseConnectionImpl;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.handlers.InformationHandler;
import de.rwth.comsys.cloudanalyzer.handlers.storages.AggregationStorage;
import de.rwth.comsys.cloudanalyzer.handlers.storages.InformationStorage;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.services.Service;
import de.rwth.comsys.cloudanalyzer.services.UnknownService;
import de.rwth.comsys.cloudanalyzer.sources.NetworkPacketSource;
import de.rwth.comsys.cloudanalyzer.util.*;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class MainHandler
{

    private static final String propFile = "CloudAnalyzer.properties";
    private static final String handlersRegFile = "handlers";
    private static final String servicesRegFile = "services";
    private static final String regionsFile = "storage/regions.xml";

    private static final String[] essentialProps = {"db.file", "ca.dataDir"};
    private static int dbVersion = 14;
    private static Logger logger = Logger.getLogger(MainHandler.class.getName());
    private static int BUFFER_THRESHOLD = 100000;
    private static MainHandler instance = null;
    private HashMap<String, LinkedHashSet<InformationHandler>> handlers;
    private HashMap<String, InformationHandler> handlersByCName;
    private HashMap<String, HashSet<Service>> groups;
    private LayeredQueue<Integer, Information> informationQueue;
    private volatile boolean stop;
    private Properties prop;
    private DatabaseConnection dbConn = null;
    private String dataDir;
    private DatabaseStatement stLoadProperty;
    private DatabaseStatement stSaveProperty;
    private boolean error;
    private HashMap<Integer, Service> services;
    private volatile NetworkPacketSource cSource;
    private IndexedTree<Integer, String> regions;
    private TreeMap<Integer, String> appIds;
    private TreeMap<String, Integer> appToId;

    private Context context;

    private MainHandler()
    {
        handlers = new HashMap<>();
        handlersByCName = new HashMap<>();
        services = new HashMap<>();
        groups = new HashMap<>();
        informationQueue = new LayeredQueue<>();
        stop = false;
        prop = null;
        error = false;
        Comparator<IndexedTreeNode<Integer, String>> com = new Comparator<IndexedTreeNode<Integer, String>>()
        {
            @Override
            public int compare(IndexedTreeNode<Integer, String> n1, IndexedTreeNode<Integer, String> n2)
            {
                return n1.getValue().compareToIgnoreCase(n2.getValue());
            }
        };
        regions = new IndexedTree<>(com);
    }

    private static MainHandler getInstance()
    {
        if (instance == null)
        {
            instance = new MainHandler();
        }
        return instance;
    }

    public static void deleteSingleton()
    {
        instance = null;
    }

    public static DatabaseConnection getDbConn()
    {
        return getInstance().dbConn;
    }

    public static void initDatabase(Context context)
    {
        getInstance()._initDatabase(context);
    }

    public static boolean init(Context context)
    {
        return getInstance()._init(context);
    }

    public static int getHandlerCount()
    {
        return getInstance()._getHandlerCount();
    }

    public static String getDataDir()
    {
        return getInstance()._getDataDir();
    }

    public static Properties getProperties()
    {
        return getInstance()._getProperties();
    }

    public static void startAnalysis(NetworkPacketSource src)
    {
        getInstance()._startAnalysis(src);
    }

    public static void shutdown()
    {
        getInstance()._shutdown();
    }

    public static <T extends InformationHandler> T getHandlerByClass(Class<T> clazz)
    {
        return getInstance()._getHandlerByClass(clazz);
    }

    public static LinkedHashSet<InformationHandler> getHandlers(String type)
    {
        return getInstance()._getHandlers(type);
    }

    public static Service getService(int id)
    {
        return getInstance()._getService(id);
    }

    public static HashSet<Service> getGroup(String group)
    {
        return getInstance()._getGroup(group);
    }

    public static void stopAnalysis()
    {
        getInstance()._stopAnalysis();
    }

    public static void reset()
    {
        getInstance()._reset();
    }

    public static void deletePersonalDataFromDb()
    {
        getInstance()._deletePersonalDataFromDb();
    }

    public static void removeAppFromAggregationStorage(int id)
    {
        getInstance()._removeAppFromAggregationStorage(id);

    }

    public static void flattenAggregationStorageUntil(int year, int month)
    {
        getInstance()._flattenAggregationStorageUntil(year, month);

    }

    public static Map<Integer, Service> getServices()
    {
        return getInstance()._getServices();
    }

    public static IndexedTree<Integer, String> getRegions()
    {
        return getInstance()._getRegions();
    }

    public static Boolean removeApp(int id)
    {
        return getInstance()._removeApp(id);
    }

    public static String getApp(int id)
    {
        return getInstance()._getApp(id);
    }

    public static int getAppId(String app)
    {
        return getInstance()._getAppId(app);
    }

    public static int addApp(String app)
    {
        return getInstance()._addApp(app);
    }

    public static AssetManager getAssets()
    {
        return getContext().getAssets();
    }

    public static Context getContext()
    {
        return getInstance()._getContext();
    }

    public static String getPropFile()
    {
        return propFile;
    }

    private void _removeAppFromAggregationStorage(int id)
    {
        AggregationStorage aggregationStorage = getHandlerByClass(AggregationStorage.class);
        if (aggregationStorage != null)
        {
            aggregationStorage.removeAppData(id);
            removeApp(id);
        }
        else
            logger.w("Aggregation storage not set up yet!");
    }

    private void _flattenAggregationStorageUntil(int year, int month)
    {
        AggregationStorage aggregationStorage = getHandlerByClass(AggregationStorage.class);
        if (aggregationStorage != null)
            aggregationStorage.flattenData(year, month);
        else
            logger.w("Aggregation storage not set up yet!");
    }

    private void _initDatabase(Context context)
    {
        this.context = context;

        loadProperties();
        connectToDb();
    }

    private boolean _init(Context context)
    {
        this.context = context;

        loadProperties();
        int ret;
        if (!essentialPropsAvailable())
        {
            logger.e("Essential properties are missing");
            error = true;
        }
        else if ((ret = connectToDb()) > -1 && loadRegions() && loadApps())
        {
            registerServices();
            if (!registerHandlers(ret == 1) || !setupServices() || !checkHandlers(ret == 1) || !checkServices(ret == 1))
            {
                _shutdown();
                error = true;
            }
        }
        else
        {
            error = true;
        }
        return !error;
    }

    private boolean checkHandlers(boolean reset)
    {
        boolean res = true;
        if (!reset)
        {
            try (DatabaseStatement s1 = dbConn.createStatement("SELECT id, class, storage FROM handlers"); DatabaseStatement s2 = dbConn.createStatement("SELECT type FROM handler_htypes WHERE handler = ?"))
            {
                DatabaseResultSet rs = s1.executeQuery();
                int c = 0;
                loop:
                while (rs.next())
                {
                    int id = rs.getInt(1);
                    String clazz = rs.getString(2);
                    boolean storage = rs.getBoolean(3);
                    c++;

                    InformationHandler h = handlersByCName.get(clazz);
                    if (h == null || storage != (h instanceof InformationStorage))
                    {
                        res = false;
                        break;
                    }
                    h.setId(id);

                    s2.setInt(1, id);
                    DatabaseResultSet rs2 = s2.executeQuery();
                    HashSet<String> ths = new HashSet<>(Arrays.asList(h.getSupportedHInformationTypes()));
                    int c2 = 0;

                    while (rs2.next())
                    {
                        String t = rs2.getString(1);
                        if (!ths.contains(t))
                        {
                            res = false;
                            logger.e("checkHandlers failed: Handler " + clazz + " is not registered for type: " + t);
                            break loop;
                        }
                        c2++;
                    }

                    if (ths.size() != c2)
                    {
                        res = false;
                        logger.e("checkHandlers failed: Handler " + clazz + " (count check)");
                        break;
                    }
                }
                if (c != handlersByCName.size())
                {
                    res = false;
                    logger.e("checkHandlers failed (count check handlers)");
                }
            }
            catch (SQLException e)
            {
                logger.w(e.toString());
            }
        }
        else
        {
            res = saveHandlers();
        }
        return res;
    }

    private boolean saveHandlers()
    {
        String sql1 = "INSERT INTO handlers (id, class, storage) VALUES(?, ?, ?)";
        String sql2 = "INSERT INTO handler_htypes (handler, type) VALUES(?, ?)";
        try (DatabaseStatement s1 = dbConn.createStatement(sql1); DatabaseStatement s2 = dbConn.createStatement(sql2))
        {
            int id = 1;
            for (Entry<String, InformationHandler> e : handlersByCName.entrySet())
            {
                String c = e.getKey();
                InformationHandler h = e.getValue();
                h.setId(id);
                boolean storage = (h instanceof InformationStorage);
                s1.setInt(1, id);
                s1.setString(2, c);
                s1.setBoolean(3, storage);
                s1.executeUpdate();
                for (String ht : h.getSupportedHInformationTypes())
                {
                    s2.setInt(1, id);
                    s2.setString(2, ht);
                    s2.executeUpdate();
                }
                id++;
            }
        }
        catch (SQLException ex)
        {
            logger.w(ex.toString());
            return false;
        }
        return true;
    }

    private boolean checkServices(boolean reset)
    {
        boolean res = true;
        if (!reset)
        {
            HashMap<Integer, Service> newServices = new HashMap<>(services);
            int c = 0;
            try (DatabaseStatement s = dbConn.createStatement("SELECT id, class FROM SERVICES"))
            {
                DatabaseResultSet rs = s.executeQuery();
                loop:
                while (rs.next())
                {
                    int id = rs.getInt(1);
                    String clazz = rs.getString(2);

                    Service srv = services.get(id);
                    newServices.remove(id);
                    if (srv == null)
                    {
                        logger.i("checkServices: Skipping removed Service " + clazz);
                        continue;
                    }
                    if (!srv.getClass().getName().equals(clazz))
                    {
                        res = false;
                        logger.e("checkServices failed: Service " + clazz + " is not registered");
                        break;
                    }

                    try (DatabaseStatement s2 = dbConn.createStatement("SELECT layer FROM service_layers WHERE service=" + id); DatabaseStatement s3 = dbConn.createStatement("SELECT `group` FROM service_groups WHERE service=" + id))
                    {
                        DatabaseResultSet rs2 = s2.executeQuery();
                        int c2 = 0;
                        while (rs2.next())
                        {
                            CloudLayer layer = CloudLayer.values()[rs2.getInt(1)];
                            if (!srv.getLayers().contains(layer))
                            {
                                logger.e("checkServices failed: Layers of " + clazz + " differ");
                                res = false;
                                break loop;
                            }
                            c2++;
                        }
                        if (c2 != srv.getLayers().size())
                        {
                            logger.e("checkServices failed: Layers of " + clazz + " differ (count check)");
                            res = false;
                            break;
                        }
                        rs2 = s3.executeQuery();
                        Set<String> grps = srv.getGroups();
                        c2 = 0;
                        while (rs2.next())
                        {
                            String group = rs2.getString(1);
                            if (!grps.contains(group))
                            {
                                logger.e("checkServices failed: Groups of " + clazz + " differ");
                                res = false;
                                break loop;
                            }
                            c2++;
                        }
                        if (c2 != srv.getGroups().size())
                        {
                            logger.e("checkServices failed: Groups of " + clazz + " differ (count check)");
                            res = false;
                            break;
                        }
                    }
                    catch (SQLException e)
                    {
                        logger.w(e.toString());
                    }
                    c++;
                }
            }
            catch (SQLException e)
            {
                logger.w(e.toString());
            }

            if (newServices.size() > 0)
            {
                if (!saveServices(newServices))
                {
                    res = false;
                    logger.e("saveServices of new services failed");
                }
                else
                {
                    c += newServices.size();
                }
            }

            if (res && c != services.size())
            {
                res = false;
                logger.e("checkServices failed (count check)");
            }
        }
        else
        {
            res = saveServices(services);
        }
        return res;
    }

    private boolean saveServices(HashMap<Integer, Service> services)
    {
        String sqlService = "INSERT INTO services (id, class, name) VALUES(?, ?, ?)";
        String sqlLayer = "INSERT INTO service_layers (service, layer) VALUES(?, ?)";
        String sqlGroup = "INSERT INTO service_groups (service, `group`) VALUES(?, ?)";

        try (DatabaseStatement stService = dbConn.createStatement(sqlService); DatabaseStatement stLayer = dbConn.createStatement(sqlLayer); DatabaseStatement stGroup = dbConn.createStatement(sqlGroup))
        {
            for (Entry<Integer, Service> e : services.entrySet())
            {
                int id = e.getKey();
                Service sv = e.getValue();
                stService.setInt(1, id);
                stService.setString(2, sv.getClass().getName());
                stService.setString(3, sv.getName());
                stService.execute();
                for (CloudLayer layer : sv.getLayers())
                {
                    stLayer.setInt(1, id);
                    stLayer.setInt(2, layer.ordinal());
                    stLayer.execute();
                }
                for (String group : sv.getGroups())
                {
                    stGroup.setInt(1, id);
                    stGroup.setString(2, group);
                    stGroup.execute();
                }
            }
        }
        catch (SQLException ex)
        {
            logger.w(ex.toString());
            return false;
        }
        return true;
    }

    private int connectToDb()
    {
        String dbFile = prop.getProperty("db.file");
        int res = 0;
        try
        {
            dbConn = new DatabaseConnectionImpl(context, dbFile, dbVersion);
            if (dbConn.createdDatabase())
                res = 1;
            createStatements();
        }
        catch (SQLException e)
        {
            logger.e(e.toString());
            try (DatabaseStatement s = dbConn.createStatement("DROP TABLE IF EXISTS version"))
            {
                s.execute();
            }
            catch (SQLException e2)
            {
                logger.w(e2.toString());
            }
            res = -1;
        }
        return res;
    }

    private int _getHandlerCount()
    {
        return handlersByCName.size();
    }

    private void createStatements() throws SQLException
    {
        String sql = "SELECT value FROM properties WHERE key = ?";
        stLoadProperty = dbConn.createStatement(sql);
        sql = "INSERT OR REPLACE INTO properties (key, value) VALUES (?, ?)";
        stSaveProperty = dbConn.createStatement(sql);
    }

    private void closeStatements()
    {
        stSaveProperty.close();
        stLoadProperty.close();
    }

    private boolean essentialPropsAvailable()
    {
        boolean res = true;
        for (String p : essentialProps)
        {
            if (!prop.containsKey(p))
            {
                res = false;
            }
        }
        return res;
    }

    private String _getDataDir()
    {
        return dataDir;
    }

    private void loadProperties()
    {
        try
        {
            prop = PropertyReader.getProperties(propFile, context);
            logger.i("Loaded properties file");
            setProperties();
        }
        catch (IOException e)
        {
            logger.e(e.toString());
        }
    }

    private void setProperties()
    {
        BUFFER_THRESHOLD = Integer.parseInt(prop.getProperty("ca.buffer_threshold", Integer.toString(BUFFER_THRESHOLD)));
        dataDir = prop.getProperty("ca.dataDir", System.getProperty("user.dir"));
    }

    private boolean setupServices()
    {
        boolean res = true;
        for (Service s : services.values())
        {
            if (!s.setupService())
            {
                logger.e("Setting up service '" + s.getClass().getName() + "' failed");
                res = false;
                break;
            }
            logger.d("Successfully set up service '" + s.getClass().getName() + "'");
        }
        if (!res)
            logger.e("setupServices() failed");
        return res;
    }

    private void registerServices()
    {
        logger.v("entering registerServices");
        UnknownService us = new UnknownService();
        services.put(0, us);
        String path = getDataDir() + servicesRegFile;
        List<String> classNames = getClassNames(path);
        int hc = 0;
        for (String className : classNames)
        {
            try
            {
                logger.i("Registering service '" + className + "'");
                Class<?> clazz = Class.forName(className);
                Object o = clazz.getConstructor().newInstance();
                if (o instanceof Service)
                {
                    Service s = (Service) o;
                    if (services.containsKey(s.getId()))
                    {
                        logger.w("Service " + className + " is registered already");
                    }
                    else
                    {
                        services.put(s.getId(), s);
                        for (String group : s.getGroups())
                        {
                            HashSet<Service> g = groups.get(group);
                            if (g == null)
                            {
                                g = new HashSet<>();
                                groups.put(group, g);
                            }
                            g.add(s);
                        }
                        hc++;
                    }
                }
                else
                {
                    logger.w("Class: '" + className + "' does not implement the Service interface");
                }
            }
            catch (NoClassDefFoundError | ReflectiveOperationException e)
            {
                logger.w(e.toString());
            }
        }
        logger.i("Registered " + hc + " services");
        logger.v("exiting registerServices");
    }

    private boolean registerHandlers(boolean resetStorages)
    {
        logger.v("entering registerHandlers");
        String path = getDataDir() + handlersRegFile;
        List<String> classNames = getClassNames(path);
        int hc = 0;
        boolean res = true;
        for (String className : classNames)
        {
            try
            {
                logger.i("Registering handler '" + className + "'");
                if (handlersByCName.containsKey(className))
                {
                    logger.w("Handler " + className + " is registered already");
                }
                else
                {
                    Class<?> clazz = Class.forName(className);
                    Object o = clazz.getConstructor().newInstance();
                    if (o instanceof InformationHandler)
                    {
                        InformationHandler h = (InformationHandler) o;
                        if (!h.setupHandler())
                        {
                            logger.e("Setting up handler '" + h.getClass().getName() + "' failed");
                            res = false;
                            break;
                        }
                        handlersByCName.put(className, h);
                        logger.d("Successfully set up handler '" + h.getClass().getName() + "'");
                        hc++;
                        if (h.getSupportedHInformationTypes() != null)
                        {
                            for (String t : h.getSupportedHInformationTypes())
                            {
                                LinkedHashSet<InformationHandler> hl = handlers.get(t);
                                if (hl == null)
                                {
                                    hl = new LinkedHashSet<>();
                                    handlers.put(t, hl);
                                }
                                hl.add(h);
                                logger.d("Added handler '" + h.getClass().getName() + "' for type '" + t + "'");
                            }
                        }
                        if (h instanceof InformationStorage)
                        {
                            InformationStorage s = (InformationStorage) h;
                            if (!s.setupStorage(dbConn, resetStorages))
                            {
                                logger.e("Setting up storage '" + s.getClass().getName() + "' failed");
                                res = false;
                                break;
                            }
                        }
                    }
                    else
                    {
                        logger.w("Class: '" + className + "' does not implement the InformationHandler interface");
                    }
                }
            }
            catch (NoClassDefFoundError | ReflectiveOperationException e)
            {
                logger.e(e.toString());
                res = false;
            }
        }
        if (res)
            logger.i("Registered " + hc + " handlers");
        else
            logger.e("registerHandlers() failed");
        logger.v("exiting registerHandlers");
        return res;
    }

    private List<String> getClassNames(String file)
    {
        List<String> names = new LinkedList<>();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open(file))))
        {
            String line;
            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (!line.startsWith("#") && line.length() > 0)
                    names.add(line);
            }
        }
        catch (IOException e)
        {
            logger.w(e.toString());
        }

        return names;
    }

    private void analysisLoop(NetworkPacketSource ndSource)
    {
        if (cSource != null)
            return;
        boolean flush = false;
        boolean finished = false;
        cSource = ndSource;

        for (InformationHandler handler : handlersByCName.values())
            handler.onStart(ndSource.isLive());
        while (!error && !finished)
        {
            if (!flush && informationQueue.size() > BUFFER_THRESHOLD)
                flush = true;
            else if (informationQueue.size() == 0)
                flush = false;
            if (!ndSource.entireInformationProcessed() && !flush && !stop)
            {
                Information i = ndSource.nextInformation();

                if (i != null)
                {
                    informationQueue.add(i, i.getPriority());
                }
                else
                {
                    flush = true;
                }
            }

            Information i = informationQueue.poll();
            if (i == null && (ndSource.entireInformationProcessed() || stop))
            {
                finished = true;
            }
            else if (i == null)
            {
                synchronized (ndSource.getSyncObject())
                {
                    try
                    {
                        ndSource.getSyncObject().wait();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            else
            {
                HashSet<InformationHandler> thandlers = handlers.get(i.getType());
                if (thandlers != null)
                {
                    for (InformationHandler h : thandlers)
                    {
                        List<Information> newInf = h.processInformation(i);
                        if (newInf != null)
                        {
                            for (Information ni : newInf)
                            {
                                informationQueue.add(ni, ni.getPriority());
                            }
                        }
                    }
                }
                else
                {
                    logger.w("No handler registered supporting type '" + i.getType() + "'");
                }
            }
        }
        for (InformationHandler handler : handlersByCName.values())
            handler.onStop();
        ndSource.close();
        synchronized (ndSource.getSyncObject())
        {
            cSource = null;
        }
    }

    private Properties _getProperties()
    {
        return prop;
    }

    private void _startAnalysis(NetworkPacketSource src)
    {
        if (!error)
        {
            stop = false;
            logger.i("Analyzing started");
            analysisLoop(src);
            logger.i("Analyzing finished");
        }
    }

    private void _shutdown()
    {
        if (!error && dbConn != null)
        {
            shutdownHandlers();
            closeStatements();

            dbConn.close();
            dbConn = null;
        }
    }

    private void shutdownHandlers()
    {
        logger.i("Shutting down handlers");
        for (Entry<String, InformationHandler> entry : handlersByCName.entrySet())
        {
            logger.d("Shutting down handler " + entry.getKey() + "");
            entry.getValue().shutdownHandler();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends InformationHandler> T _getHandlerByClass(Class<T> clazz)
    {
        return (T) handlersByCName.get(clazz.getName());
    }

    private Service _getService(int id)
    {
        return services.get(id);
    }

    private LinkedHashSet<InformationHandler> _getHandlers(String type)
    {
        return handlers.get(type);
    }

    private HashSet<Service> _getGroup(String group)
    {
        return groups.get(group);
    }

    private void _stopAnalysis()
    {
        stop = true;
        synchronized (cSource.getSyncObject())
        {
            cSource.getSyncObject().notifyAll();
        }
    }

    private void _reset()
    {
        if (error)
            return;
        logger.i("Resetting handlers");
        for (Entry<String, InformationHandler> entry : handlersByCName.entrySet())
        {
            logger.d("Resetting handler " + entry.getKey() + "");
            InformationHandler h = entry.getValue();
            h.resetHandler();
        }
        logger.i("Setting up services");
        for (Service sv : services.values())
        {
            logger.d("Setting up service " + sv.getName());
            sv.setupService();
        }
    }

    private void _deletePersonalDataFromDb()
    {
        for (InformationHandler h : handlersByCName.values())
        {
            if (h instanceof InformationStorage)
            {
                ((InformationStorage) h).deletePersonalData();
            }
        }
    }

    private Map<Integer, Service> _getServices()
    {
        return Collections.unmodifiableMap(services);
    }

    private IndexedTree<Integer, String> _getRegions()
    {
        return regions;
    }

    private boolean loadRegions()
    {
        String path = getDataDir() + regionsFile;
        if (!RegionXMLHandler.parseXML(path, regions))
        {
            logger.e("Error loading " + path);
            return false;
        }
        return true;
    }

    private boolean _removeApp(int id)
    {
        if (appIds == null || appToId == null)
        {
            logger.w("MainHandler not initialized: apps == null");
            return false;
        }
        if (!appIds.containsKey(id))
        {
            logger.w("App id not in database, removing failed");
            return false;
        }

        String sql = "DELETE FROM apps WHERE id = ?";
        try (DatabaseStatement s = dbConn.createStatement(sql))
        {
            s.setInt(1, id);
            s.execute();
        }
        catch (SQLException e)
        {
            logger.w(e.toString());
            return false;
        }

        // process loaded information
        String appName = appIds.get(id);
        appIds.remove(id);
        appToId.remove(appName);
        return true;
    }

    private String _getApp(int id)
    {
        if (appIds == null || appToId == null)
        {
            logger.w("MainHandler not initialized: apps == null");
            return null;
        }
        return ((id < 0 || !appIds.containsKey(id)) ? null : appIds.get(id));
    }

    private int _getAppId(String app)
    {
        Integer id = null;
        if (appToId != null)
            id = appToId.get(app);
        else
            logger.w("MainHandler not initialized: appToId == null");
        if (id == null)
            id = -1;
        return id;
    }

    private boolean loadApps()
    {
        String sqlCount = "SELECT COUNT(id) FROM apps";
        try (DatabaseStatement s = dbConn.createStatement(sqlCount); DatabaseStatement s2 = dbConn.createStatement("SELECT id, app FROM apps"))
        {
            DatabaseResultSet rs = s.executeQuery();
            if (!rs.moveToFirst())
                throw new SQLException("Query '" + sqlCount + "' returned an empty result set.");
            appIds = new TreeMap<>();
            appToId = new TreeMap<>();
            DatabaseResultSet rs2 = s2.executeQuery();
            while (rs2.next())
            {
                int id = rs2.getInt(1);
                String app = rs2.getString(2);
                appIds.put(id, app);
                appToId.put(app, id);
            }
        }
        catch (SQLException e)
        {
            logger.e("Could not load apps: " + e);
            return false;
        }
        return true;
    }

    private int _addApp(String app)
    {
        Integer id = appToId.get(app);
        if (id == null)
        {
            // a more sophisticated approach would be possible here
            id = appIds.lastKey();
            if (id == null)
                id = -1;
            // next id
            id++;
            appIds.put(id, app);
            appToId.put(app, id);

            try (DatabaseStatement s = dbConn.createStatement("INSERT INTO apps (id, app) VALUES (?, ?)"))
            {
                s.setInt(1, id);
                s.setString(2, app);
                s.execute();
            }
            catch (SQLException e)
            {
                logger.w("Could not insert app " + id + " into db: " + e);
                error = true;
            }
        }
        return id;
    }

    private Context _getContext()
    {
        return context;
    }
}
