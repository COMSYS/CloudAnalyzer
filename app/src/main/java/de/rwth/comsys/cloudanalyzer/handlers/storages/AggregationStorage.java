package de.rwth.comsys.cloudanalyzer.handlers.storages;

import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.information.FullIpPacket;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.information.NetworkPacket;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.*;
import de.rwth.comsys.cloudanalyzer.util.*;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AggregationStorage extends AbstractInformationStorage
{

    private static final String[] supportedHTypes = {"NetworkPacket"};
    private static Logger logger = Logger.getLogger(AggregationStorage.class.getName());

    private DatabaseConnection dbConn;
    private DatabaseStatement stUpdateStatistic;
    private DatabaseStatement stUpdateAggregatedData;
    private DatabaseStatement stUpdateServiceSetData;
    private DatabaseStatement stInsertServiceSetMember;
    private DatabaseStatement stSelectMaxServiceId;
    private volatile long lastUpdate;
    private StorageHelperThread exThread;
    private int flushIntervalTime;
    private int flushIntervalBytes;

    // Map: (year, dayOfYear) -> TrafficPropertyCounter
    private HashMap<NTuple<Integer>, TrafficPropertyCounter> totalTrafficCounters;
    // Map: (year, dayOfYear) -> TrafficPropertyCounter
    private HashMap<NTuple<Integer>, TrafficPropertyCounter> totalIPTrafficCounters;
    // Map: (service, handler, app, region, year, dayOfYear) -> TrafficPropertyCounter
    private HashMap<NTuple<Integer>, TrafficPropertyCounter> aggregatedData;
    // Map: (appId, ServiceSet, year, dayOfYear) -> TrafficPropertyCounter
    private HashMap<NTuple<Object>, TrafficPropertyCounter> aggregatedServiceSetData;

    private int lastYear;
    private int lastDayOfYear;
    private volatile long lastTimestamp;
    private boolean checkGroups;
    private int maxSetId;

    public AggregationStorage()
    {
        aggregatedData = new HashMap<>();
        aggregatedServiceSetData = new HashMap<>();
        totalTrafficCounters = new HashMap<>();
        totalIPTrafficCounters = new HashMap<>();
    }

    @Override
    public void resetHandler()
    {
        boolean trunning = (exThread != null && exThread.isAlive());
        if (trunning)
        {
            waitForThread();
        }
        String file = MainHandler.getDataDir() + "storage/dbSQL/aggregationStorage.sql";
        try
        {
            dbConn.executeFile(MainHandler.getAssets().open(file));
        }
        catch (SQLException | IOException e)
        {
            logger.e(e.toString());
        }

        totalTrafficCounters.clear();
        totalIPTrafficCounters.clear();
        aggregatedData.clear();
        aggregatedServiceSetData.clear();
        maxSetId = 0;

        if (trunning)
        {
            prepareThread();
            exThread.start();
        }
    }

    private void waitForThread()
    {
        if (exThread == null)
            return;
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

    private void savePacketServiceInformation(PacketServiceInformation psi)
    {
        Packet p = psi.getPacket();
        int bytes = p.getIpPacket().getLength();
        checkGroups(psi);

        HashSet<NTuple<Integer>> keys = new HashSet<>();
        ServiceSet services = addKeys(keys, psi);
        if (services != null)
        {
            for (NTuple<Integer> key : keys)
            {
                TrafficPropertyCounter counter = aggregatedData.get(key);
                if (counter == null)
                {
                    counter = new TrafficPropertyCounter();
                    aggregatedData.put(key, counter);
                }
                counter.modifyPacketCounter(psi.getPacket().getTrafficProperty(), 1);
                counter.modifyByteCounter(psi.getPacket().getTrafficProperty(), bytes);
            }
            processServiceSet(services, p);
        }
    }

    private void processServiceSet(ServiceSet set, Packet p)
    {
        if (set.size() <= 1)
            return;

        int bytes = p.getIpPacket().getLength();
        int app = p.getApp();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(p.getTimestampInMillis());
        int year = c.get(Calendar.YEAR);
        int dayOfYear = c.get(Calendar.DAY_OF_YEAR);

        NTuple<Object> key = NTuple.<Object>createTuple(app, set, year, dayOfYear);
        TrafficPropertyCounter counter = aggregatedServiceSetData.get(key);

        if (counter == null)
        {
            counter = new TrafficPropertyCounter();
            int setId = getServiceSetId(set);
            if (setId < 0)
                return;
            set.setId(setId);
            aggregatedServiceSetData.put(key, counter);
        }

        counter.modifyPacketCounter(p.getTrafficProperty(), 1);
        counter.modifyByteCounter(p.getTrafficProperty(), bytes);
    }

    private int getServiceSetId(Set<Integer> set)
    {
        StringBuilder sql = null;
        int setId = -1;
        for (Integer service : set)
        {
            if (sql == null)
                sql = new StringBuilder("SELECT `set` FROM `service_set_members` WHERE `service` IN (").append(service);
            else
                sql.append(", ").append(service);
        }
        if (sql != null)
        {
            sql.append(") GROUP BY `set` HAVING COUNT(service) = ").append(set.size());
            sql.append(" INTERSECT SELECT `set` FROM `service_set_members` GROUP BY `set` HAVING COUNT(service) = ").append(set.size());
            try
            {
                DatabaseStatement stmt = dbConn.createStatement(sql.toString());
                DatabaseResultSet rs = stmt.executeQuery();
                if (rs.moveToFirst())
                {
                    setId = rs.getInt(1);
                }
                else
                {
                    setId = ++maxSetId;
                    for (Integer service : set)
                    {
                        stInsertServiceSetMember.setInt(1, setId);
                        stInsertServiceSetMember.setInt(2, service);
                        stInsertServiceSetMember.execute();
                    }
                }
            }
            catch (SQLException e)
            {
                logger.e(e.toString());
            }
        }
        return setId;
    }

    private boolean checkGroups(PacketServiceInformation psi)
    {
        if (checkGroups && !IdentifiedService.servicesOfSameLayerInSameGroup(psi.getIdentifiedServices()))
        {
            StringBuilder services = new StringBuilder();
            for (IdentifiedService is : psi.getIdentifiedServices())
            {
                if (services.length() > 0)
                    services.append(", ");
                services.append(is.getServiceProperties().getService().getName());
            }
            logger.w("Packet " + psi.getPacket().getFrameNumber() + ": Multiple services of the same layer are assigned (all groups differ). Services: " + services);
            return false;
        }
        return true;
    }

    private ServiceSet addKeys(Set<NTuple<Integer>> keys, PacketServiceInformation psi)
    {
        List<IdentifiedService> isl = psi.getIdentifiedServices();
        IndexedTreeNode<Integer, String> mostPreciseRegion = null;
        IndexedTree<Integer, String> regions = MainHandler.getRegions();
        ServiceSet services = new ServiceSet();
        for (IdentifiedService is : isl)
        {
            IndexedTreeNode<Integer, String> r = regions.getNode(is.getServiceProperties().getRegion());
            if (mostPreciseRegion == null || mostPreciseRegion.getKey() == 0 || mostPreciseRegion.isParentOf(r))
                mostPreciseRegion = r;
        }

        if (mostPreciseRegion == null)
        {
            logger.w("Got PacketServiceInformation with an empty list of identified services");
            return null;
        }

        for (IdentifiedService is : isl)
        {
            int service = is.getServiceProperties().getService().getId();
            int region = mostPreciseRegion.getKey();
            int handler = is.identifiedBy();
            int app = psi.getPacket().getApp();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(psi.getPacket().getTimestampInMillis());
            int year = c.get(Calendar.YEAR);
            int dayOfYear = c.get(Calendar.DAY_OF_YEAR);
            services.add(service);
            // Region, year, dayOfYear have to be the last keys, because we do not need to exclude them as they partition the data
            int partitionKeys = 3; // region, year, dayOfYear
            NTuple<Integer> key = NTuple.createTuple(service, handler, app, region, year, dayOfYear);
            keys.add(key);
            for (int subset = 1; subset <= Math.pow(2, key.size() - partitionKeys + 1) - 2; ++subset)
            {
                Integer[] subKey = new Integer[key.size()];
                for (int i = 0; i < subKey.length - partitionKeys; ++i)
                {
                    if (((subset >>> i) & 1) == 1)
                        subKey[i] = key.getEntry(i);
                    else
                        subKey[i] = -1;
                }
                for (int i = 0; i < partitionKeys; ++i)
                {
                    subKey[key.size() - i - 1] = key.getEntry(key.size() - i - 1);
                }
                keys.add(NTuple.createTuple(subKey));
            }
        }
        return services;
    }

    private void setStUpdateAggregatedDataValues(int service, int handler, int app, int region, TrafficProperty property, int year, int dayOfYear, long packets, long bytes)
    {
        // INSERT INTO aggregation_data(service, handler, region, app, day, year, direction, importance, link, protocol, packets, bytes)
        //      VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
        //          ? + COALESCE((SELECT packets FROM aggregation_data WHERE service = ? AND handler = ? AND region = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0),
        //          ? + COALESCE((SELECT bytes FROM aggregation_data WHERE service = ? AND handler = ? AND region = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0))
        stUpdateAggregatedData.setInt(1, service);
        stUpdateAggregatedData.setInt(2, handler);
        stUpdateAggregatedData.setInt(3, region);
        stUpdateAggregatedData.setInt(4, app);
        stUpdateAggregatedData.setInt(5, dayOfYear);
        stUpdateAggregatedData.setInt(6, year);
        stUpdateAggregatedData.setInt(7, property.getDirection().getId());
        stUpdateAggregatedData.setInt(8, property.getImportance().getId());
        stUpdateAggregatedData.setInt(9, property.getLink().getId());
        stUpdateAggregatedData.setInt(10, property.getProtocol().getId());
        stUpdateAggregatedData.setLong(11, packets);
        stUpdateAggregatedData.setInt(12, service);
        stUpdateAggregatedData.setInt(13, handler);
        stUpdateAggregatedData.setInt(14, region);
        stUpdateAggregatedData.setInt(15, app);
        stUpdateAggregatedData.setInt(16, dayOfYear);
        stUpdateAggregatedData.setInt(17, year);
        stUpdateAggregatedData.setInt(18, property.getDirection().getId());
        stUpdateAggregatedData.setInt(19, property.getImportance().getId());
        stUpdateAggregatedData.setInt(20, property.getLink().getId());
        stUpdateAggregatedData.setInt(21, property.getProtocol().getId());
        stUpdateAggregatedData.setLong(22, bytes);
        stUpdateAggregatedData.setInt(23, service);
        stUpdateAggregatedData.setInt(24, handler);
        stUpdateAggregatedData.setInt(25, region);
        stUpdateAggregatedData.setInt(26, app);
        stUpdateAggregatedData.setInt(27, dayOfYear);
        stUpdateAggregatedData.setInt(28, year);
        stUpdateAggregatedData.setInt(29, property.getDirection().getId());
        stUpdateAggregatedData.setInt(30, property.getImportance().getId());
        stUpdateAggregatedData.setInt(31, property.getLink().getId());
        stUpdateAggregatedData.setInt(32, property.getProtocol().getId());
    }

    private void addAggregatedDataToBatch(int service, int handler, int app, int region, TrafficProperty property, int year, int dayOfYear, long packets, long bytes)
    {
        setStUpdateAggregatedDataValues(service, handler, app, region, property, year, dayOfYear, packets, bytes);
        stUpdateAggregatedData.addToBatch();
    }

    private void executeAggregatedDataBatch() throws SQLException
    {
        stUpdateAggregatedData.executeBatch();
    }

    private void executeServiceSetDataBatch() throws SQLException
    {
        stUpdateServiceSetData.executeBatch();
    }

    private void createAggregatedDataBatch()
    {
        for (Map.Entry<NTuple<Integer>, TrafficPropertyCounter> e : aggregatedData.entrySet())
        {
            NTuple<Integer> key = e.getKey();
            TrafficPropertyCounter counter = e.getValue();
            final int service = key.getEntry(0);
            final int handler = key.getEntry(1);
            final int app = key.getEntry(2);
            final int region = key.getEntry(3);
            final int year = key.getEntry(4);
            final int dayOfYear = key.getEntry(5);

            TrafficPropertyCounter.TrafficPropertyCounterHandler counterHandler = new TrafficPropertyCounter.TrafficPropertyCounterHandler()
            {
                @Override
                public void handleCounter(TrafficProperty property, long packets, long bytes)
                {
                    addAggregatedDataToBatch(service, handler, app, region, property, year, dayOfYear, packets, bytes);
                }
            };

            counter.enumerate(counterHandler);
        }
        aggregatedData.clear();
    }

    private void setStUpdateServiceSetDataValues(int appId, int setId, int year, int dayOfYear, TrafficProperty property, long packets, long bytes)
    {
        // INSERT INTO service_sets_data (id, app, day, year, direction, importance, link, protocol, packets, bytes)
        //      VALUES (?, ?, ?, ?, ?, ?, ?, ?,
        //          ? + COALESCE((SELECT packets FROM service_sets_data WHERE id = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0),
        //          ? + COALESCE((SELECT bytes FROM service_sets_data WHERE id = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0))
        stUpdateServiceSetData.setInt(1, setId);
        stUpdateServiceSetData.setInt(2, appId);
        stUpdateServiceSetData.setInt(3, dayOfYear);
        stUpdateServiceSetData.setInt(4, year);
        stUpdateServiceSetData.setInt(5, property.getDirection().getId());
        stUpdateServiceSetData.setInt(6, property.getImportance().getId());
        stUpdateServiceSetData.setInt(7, property.getLink().getId());
        stUpdateServiceSetData.setInt(8, property.getProtocol().getId());
        stUpdateServiceSetData.setLong(9, packets);
        stUpdateServiceSetData.setInt(10, setId);
        stUpdateServiceSetData.setInt(11, appId);
        stUpdateServiceSetData.setInt(12, dayOfYear);
        stUpdateServiceSetData.setInt(13, year);
        stUpdateServiceSetData.setInt(14, property.getDirection().getId());
        stUpdateServiceSetData.setInt(15, property.getImportance().getId());
        stUpdateServiceSetData.setInt(16, property.getLink().getId());
        stUpdateServiceSetData.setInt(17, property.getProtocol().getId());
        stUpdateServiceSetData.setLong(18, bytes);
        stUpdateServiceSetData.setInt(19, setId);
        stUpdateServiceSetData.setInt(20, appId);
        stUpdateServiceSetData.setInt(21, dayOfYear);
        stUpdateServiceSetData.setInt(22, year);
        stUpdateServiceSetData.setInt(23, property.getDirection().getId());
        stUpdateServiceSetData.setInt(24, property.getImportance().getId());
        stUpdateServiceSetData.setInt(25, property.getLink().getId());
        stUpdateServiceSetData.setInt(26, property.getProtocol().getId());
    }

    private void addServiceSetToBatch(int appId, int setId, int year, int dayOfYear, TrafficProperty property, long packets, long bytes)
    {
        setStUpdateServiceSetDataValues(appId, setId, year, dayOfYear, property, packets, bytes);
        stUpdateServiceSetData.addToBatch();
    }

    private void createServiceSetDataBatch()
    {
        for (Map.Entry<NTuple<Object>, TrafficPropertyCounter> e : aggregatedServiceSetData.entrySet())
        {
            TrafficPropertyCounter counter = e.getValue();
            final int appId = (Integer) e.getKey().getEntry(0);
            final int setId = ((ServiceSet) e.getKey().getEntry(1)).getId();
            final int year = (Integer) e.getKey().getEntry(2);
            final int dayOfYear = (Integer) e.getKey().getEntry(3);

            TrafficPropertyCounter.TrafficPropertyCounterHandler handler = new TrafficPropertyCounter.TrafficPropertyCounterHandler()
            {
                @Override
                public void handleCounter(TrafficProperty property, long packets, long bytes)
                {
                    addServiceSetToBatch(appId, setId, year, dayOfYear, property, packets, bytes);
                }
            };

            counter.enumerate(handler);
        }
        aggregatedServiceSetData.clear();
    }

    private void processPacket(Packet p)
    {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(p.getTimestampInMillis());
        int year = c.get(Calendar.YEAR);
        int dayOfYear = c.get(Calendar.DAY_OF_YEAR);
        NTuple<Integer> key = NTuple.createTuple(year, dayOfYear, -1);

        int length;
        synchronized (AggregationStorage.this)
        {
            if (lastUpdate < 0)
            {
                lastUpdate = c.getTimeInMillis();
                lastYear = year;
                lastDayOfYear = dayOfYear;
            }

            if (p.getTimestampInMillis() > lastTimestamp)
                lastTimestamp = p.getTimestampInMillis();
            TrafficPropertyCounter counter = totalTrafficCounters.get(key);
            if (counter == null)
            {
                counter = new TrafficPropertyCounter();
                totalTrafficCounters.put(key, counter);
            }
            counter.modifyPacketCounter(p.getTrafficProperty(), 1);
            counter.modifyByteCounter(p.getTrafficProperty(), p.getPacketWirelen());
            length = p.getPacketWirelen();

            FullIpPacket ip = (FullIpPacket) p.getIpPacket();
            if (ip != null)
            {
                NTuple<Integer> keyApp = ip.getPacket().getApp() >= 0 ? NTuple.createTuple(year, dayOfYear, ip.getPacket().getApp()) : null;

                updateTotalIpTrafficCounter(ip, key);
                if (keyApp != null)
                    updateTotalIpTrafficCounter(ip, keyApp);

                if (!p.getPacketServiceInformation().getIdentifiedServices().isEmpty())
                    savePacketServiceInformation(p.getPacketServiceInformation());

                length = ip.getLength();
            }
        }
        exThread.updateCounter(length);
    }

    private void updateTotalIpTrafficCounter(FullIpPacket ip, NTuple<Integer> key)
    {
        TrafficPropertyCounter counter = totalIPTrafficCounters.get(key);
        if (counter == null)
        {
            counter = new TrafficPropertyCounter();
            totalIPTrafficCounters.put(key, counter);
        }
        counter.modifyPacketCounter(ip.getPacket().getTrafficProperty(), 1);
        counter.modifyByteCounter(ip.getPacket().getTrafficProperty(), ip.getLength());
    }

    @Override
    public List<Information> processInformation(Information i)
    {
        switch (i.getType())
        {
            case "NetworkPacket":
            {
                NetworkPacket np = (NetworkPacket) i;


                Consumer<Packet> consumer = new Consumer<Packet>()
                {
                    @Override
                    public void accept(Packet packet)
                    {
                        processPacket(packet);
                    }
                };
                np.getPacket().registerAllHandlerFinishedCallback(consumer);

                np.getPacket().handlerFinished(getId());
                break;
            }
            default:
                logger.w("Received information object with unknown type: " + i.getType());
                break;
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
        flushIntervalTime = Integer.valueOf(MainHandler.getProperties().getProperty("AggregationStorage.flush_interval_seconds", "300"));
        flushIntervalBytes = Integer.valueOf(MainHandler.getProperties().getProperty("AggregationStorage.flush_interval_bytes", "10000000"));
        checkGroups = Boolean.valueOf(MainHandler.getProperties().getProperty("AggregationStorage.check_groups", "true"));

        if (reset)
        {
            resetHandler();
        }

        if (!prepareStatements())
        {
            res = false;
        }

        if (!reset)
            maxSetId = getMaxServiceId();

        return res;
    }

    private int getMaxServiceId()
    {
        try
        {
            DatabaseResultSet rs = stSelectMaxServiceId.executeQuery();
            if (rs.moveToFirst())
                return rs.getInt(1);
            else
                return 0;
        }
        catch (SQLException e)
        {
            logger.w(e.toString());
            return 0;
        }
    }

    private boolean prepareStatements()
    {
        try
        {
            String sql;
            sql = "INSERT OR REPLACE INTO aggregation_statistics(name, day, year, app, direction, importance, link, protocol, value) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?," + "? + COALESCE((SELECT value FROM aggregation_statistics WHERE name = ? AND day = ? AND year = ? AND app = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?)" + ", 0))";
            stUpdateStatistic = dbConn.createStatement(sql);
            sql = "INSERT OR REPLACE INTO aggregation_data(service, handler, region, app, day, year, direction, importance, link, protocol, packets, bytes) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?," + "? + COALESCE((SELECT packets FROM aggregation_data WHERE service = ? AND handler = ? AND region = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0), " + "? + COALESCE((SELECT bytes FROM aggregation_data WHERE service = ? AND handler = ? AND region = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0))";
            stUpdateAggregatedData = dbConn.createStatement(sql);
            sql = "INSERT OR REPLACE INTO service_sets_data (id, app, day, year, direction, importance, link, protocol, packets, bytes) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?," + "? + COALESCE((SELECT packets FROM service_sets_data WHERE id = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0)," + "? + COALESCE((SELECT bytes FROM service_sets_data WHERE id = ? AND app = ? AND day = ? AND year = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?), 0))";
            stUpdateServiceSetData = dbConn.createStatement(sql);
            sql = "INSERT INTO service_set_members (`set`, service) VALUES (?, ?)";
            stInsertServiceSetMember = dbConn.createStatement(sql);
            sql = "SELECT MAX(`set`) FROM service_set_members";
            stSelectMaxServiceId = dbConn.createStatement(sql);
        }
        catch (SQLException e)
        {
            logger.e(e.toString());
            return false;
        }
        return true;
    }

    private void prepareThread()
    {
        Runnable onExecute = new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (AggregationStorage.this)
                {
                    if (lastUpdate < 0)
                        return;

                    createUpdateStatisticsBatch();
                    createAggregatedDataBatch();
                    createServiceSetDataBatch();
                }

                try
                {
                    dbConn.beginTransaction();
                    executeUpdateStatisticBatch();
                    executeAggregatedDataBatch();
                    executeServiceSetDataBatch();
                    dbConn.commitTransaction();
                }
                catch (SQLException e)
                {
                    logger.w(e.toString());
                }
                finally
                {
                    dbConn.endTransaction();
                }
            }
        };
        exThread = new StorageHelperThread(flushIntervalTime, flushIntervalBytes, onExecute);
        exThread.setName("AggregationStorageThread");
    }

    private void setStUpdateStatisticValues(String name, long diff, int year, int dayOfYear, int app, TrafficProperty property)
    {
        // INSERT OR REPLACE INTO aggregation_statistics(name, day, year, app, direction, importance, link, protocol, value)
        //      VALUES(?, ?, ?, ?, ?, ?, ?, ?,
        //           ? + COALESCE((SELECT value FROM aggregation_statistics WHERE name = ? AND day = ? AND year = ? AND app = ? AND direction = ? AND importance = ? AND link = ? AND protocol = ?)
        //          , 0))
        stUpdateStatistic.setString(1, name);
        stUpdateStatistic.setInt(2, dayOfYear);
        stUpdateStatistic.setInt(3, year);
        stUpdateStatistic.setInt(4, app);
        stUpdateStatistic.setInt(5, property.getDirection().getId());
        stUpdateStatistic.setInt(6, property.getImportance().getId());
        stUpdateStatistic.setInt(7, property.getLink().getId());
        stUpdateStatistic.setInt(8, property.getProtocol().getId());
        stUpdateStatistic.setLong(9, diff);
        stUpdateStatistic.setString(10, name);
        stUpdateStatistic.setInt(11, dayOfYear);
        stUpdateStatistic.setInt(12, year);
        stUpdateStatistic.setInt(13, app);
        stUpdateStatistic.setInt(14, property.getDirection().getId());
        stUpdateStatistic.setInt(15, property.getImportance().getId());
        stUpdateStatistic.setInt(16, property.getLink().getId());
        stUpdateStatistic.setInt(17, property.getProtocol().getId());
    }

    private void addUpdateStatisticToBatch(String name, long diff, int year, int dayOfYear, int app, TrafficProperty property)
    {
        setStUpdateStatisticValues(name, diff, year, dayOfYear, app, property);
        stUpdateStatistic.addToBatch();
    }

    private void addTotalTrafficCounterToBatch(HashMap<NTuple<Integer>, TrafficPropertyCounter> map, final String packetFieldName, final String byteFieldName)
    {
        for (Map.Entry<NTuple<Integer>, TrafficPropertyCounter> entry : map.entrySet())
        {
            final int year = entry.getKey().getEntry(0);
            final int dayOfYear = entry.getKey().getEntry(1);
            final int app = entry.getKey().getEntry(2);
            TrafficPropertyCounter counter = entry.getValue();

            TrafficPropertyCounter.TrafficPropertyCounterHandler handler = new TrafficPropertyCounter.TrafficPropertyCounterHandler()
            {
                @Override
                public void handleCounter(TrafficProperty property, long packets, long bytes)
                {
                    addUpdateStatisticToBatch(packetFieldName, packets, year, dayOfYear, app, property);
                    addUpdateStatisticToBatch(byteFieldName, bytes, year, dayOfYear, app, property);
                }
            };

            counter.enumerate(handler);
        }
    }

    private void executeUpdateStatisticBatch() throws SQLException
    {
        stUpdateStatistic.executeBatch();
    }

    private void createUpdateStatisticsBatch()
    {
        addTotalTrafficCounterToBatch(totalIPTrafficCounters, "ipPacketCount", "totalIpPacketLength");
        addTotalTrafficCounterToBatch(totalTrafficCounters, "packetCount", "totalPacketLength");

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(lastTimestamp);
        int year = c.get(Calendar.YEAR);
        int dayOfYear = c.get(Calendar.DAY_OF_YEAR);

        if (year > lastYear || (year == lastYear && dayOfYear > lastDayOfYear))
        {
            Calendar c2 = Calendar.getInstance();
            c2.clear();
            c2.set(lastYear, 0, 1, 0, 0, 0);
            c2.set(Calendar.DAY_OF_YEAR, lastDayOfYear);
            while (year > lastYear || (year == lastYear && dayOfYear > lastDayOfYear))
            {

                c2.add(Calendar.DAY_OF_YEAR, 1);
                long diff = c2.getTimeInMillis() - lastUpdate;
                setStUpdateStatisticValues("totalTime", diff, year, dayOfYear, -1, TrafficPropertiesManager.getProperty(FlowDirection.AGGREGATED, Importance.AGGREGATED, Link.AGGREGATED, Protocol.AGGREGATED));
                lastDayOfYear = c2.get(Calendar.DAY_OF_YEAR);
                lastYear = c2.get(Calendar.YEAR);
                lastUpdate = c2.getTimeInMillis();
            }
        }

        long diff = lastTimestamp - lastUpdate;
        addUpdateStatisticToBatch("totalTime", diff, year, dayOfYear, -1, TrafficPropertiesManager.getProperty(FlowDirection.AGGREGATED, Importance.AGGREGATED, Link.AGGREGATED, Protocol.AGGREGATED));
        lastUpdate = lastTimestamp;

        totalTrafficCounters.clear();
        totalIPTrafficCounters.clear();
    }

    public void removeAppData(int id)
    {
        logger.i("Removing entries of app " + id + "... ");

        String sqlDeleteData = "DELETE FROM aggregation_data WHERE app = ?";
        String sqlDeleteStatistics = "DELETE FROM aggregation_statistics WHERE app = ?";
        String sqlDeleteServiceSetsData = "DELETE FROM service_sets_data WHERE app = ?";

        try (DatabaseStatement stDeleteData = dbConn.createStatement(sqlDeleteData); DatabaseStatement stDeleteStatistics = dbConn.createStatement(sqlDeleteStatistics); DatabaseStatement stDeleteServiceSetsData = dbConn.createStatement(sqlDeleteServiceSetsData))
        {
            // data
            stDeleteData.setInt(1, id);

            // statistics
            stDeleteStatistics.setInt(1, id);

            // service sets data
            stDeleteServiceSetsData.setInt(1, id);

            // execute
            stDeleteData.execute();
            stDeleteStatistics.execute();
            stDeleteServiceSetsData.execute();
        }
        catch (SQLException ex)
        {
            logger.w(ex.toString());
        }
    }

    private void flattenDataMonth(int processYear, int startDay, int endDay, int month)
    {
        logger.i("Processing entries in year " + processYear + " from " + startDay + " to " + endDay + " (month " + month + ")... ");

        String sqlData = "SELECT service, handler, app, region, direction, importance, link, protocol, SUM(packets), SUM(bytes) " + "FROM aggregation_data WHERE year = ? AND (day >= ? AND day <= ?) " + "GROUP BY service, handler, region, app, direction, importance, link, protocol";
        String sqlStatistics = "SELECT name, app, direction, importance, link, protocol, SUM(value)" + "FROM aggregation_statistics WHERE year = ? AND (day >= ? AND day <= ?) " + "GROUP BY name, app, direction, importance, link, protocol";
        String sqlServiceSetsData = "SELECT app, id, direction, importance, link, protocol, SUM(packets), SUM(bytes) " + "FROM service_sets_data WHERE year = ? AND (day >= ? AND day <= ?) " + "GROUP BY app, id, direction, importance, link, protocol";

        try (DatabaseStatement stData = dbConn.createStatement(sqlData); DatabaseStatement stStatistics = dbConn.createStatement(sqlStatistics); DatabaseStatement stServiceSetsData = dbConn.createStatement(sqlServiceSetsData))
        {
            synchronized (AggregationStorage.this)
            {
                // data
                stData.setInt(1, processYear);
                stData.setInt(2, startDay);
                stData.setInt(3, endDay);

                DatabaseResultSet rs = stData.executeQuery();
                while (rs.next())
                {
                    TrafficProperty trafficProperty = TrafficPropertiesManager.getProperty(FlowDirection.getDirection(rs.getInt(5)), Importance.getImportance(rs.getInt(6)), Link.getLink(rs.getInt(7)), Protocol.getProtocol(rs.getInt(8)));
                    addAggregatedDataToBatch(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), trafficProperty, processYear, -month, rs.getLong(9), rs.getLong(10));
                }

                // statistics
                stStatistics.setInt(1, processYear);
                stStatistics.setInt(2, startDay);
                stStatistics.setInt(3, endDay);

                rs = stStatistics.executeQuery();
                while (rs.next())
                {
                    TrafficProperty trafficProperty = TrafficPropertiesManager.getProperty(FlowDirection.getDirection(rs.getInt(3)), Importance.getImportance(rs.getInt(4)), Link.getLink(rs.getInt(5)), Protocol.getProtocol(rs.getInt(6)));
                    addUpdateStatisticToBatch(rs.getString(1), rs.getLong(7), processYear, -month, rs.getInt(2), trafficProperty);
                }

                // service sets data
                stServiceSetsData.setInt(1, processYear);
                stServiceSetsData.setInt(2, startDay);
                stServiceSetsData.setInt(3, endDay);

                rs = stServiceSetsData.executeQuery();
                while (rs.next())
                {
                    TrafficProperty trafficProperty = TrafficPropertiesManager.getProperty(FlowDirection.getDirection(rs.getInt(3)), Importance.getImportance(rs.getInt(4)), Link.getLink(rs.getInt(5)), Protocol.getProtocol(rs.getInt(6)));
                    addServiceSetToBatch(rs.getInt(1), rs.getInt(2), processYear, -month, trafficProperty, rs.getLong(7), rs.getLong(8));
                }

                // execute
                executeAggregatedDataBatch();
                executeServiceSetDataBatch();
                executeUpdateStatisticBatch();
            }
        }
        catch (SQLException ex)
        {
            logger.w(ex.toString());
            return;
        }

        String sqlDeleteData = "DELETE FROM aggregation_data WHERE year = ? AND (day >= ? AND day <= ?)";
        String sqlDeleteStatistics = "DELETE FROM aggregation_statistics WHERE year = ? AND (day >= ? AND day <= ?)";
        String sqlDeleteServiceSetsData = "DELETE FROM service_sets_data WHERE year = ? AND (day >= ? AND day <= ?)";

        try (DatabaseStatement stDeleteData = dbConn.createStatement(sqlDeleteData); DatabaseStatement stDeleteStatistics = dbConn.createStatement(sqlDeleteStatistics); DatabaseStatement stDeleteServiceSetsData = dbConn.createStatement(sqlDeleteServiceSetsData))
        {
            // data
            stDeleteData.setInt(1, processYear);
            stDeleteData.setInt(2, startDay);
            stDeleteData.setInt(3, endDay);

            // statistics
            stDeleteStatistics.setInt(1, processYear);
            stDeleteStatistics.setInt(2, startDay);
            stDeleteStatistics.setInt(3, endDay);

            // service sets data
            stDeleteServiceSetsData.setInt(1, processYear);
            stDeleteServiceSetsData.setInt(2, startDay);
            stDeleteServiceSetsData.setInt(3, endDay);

            // execute
            stDeleteData.execute();
            stDeleteStatistics.execute();
            stDeleteServiceSetsData.execute();
        }
        catch (SQLException ex)
        {
            logger.w(ex.toString());
        }
    }

    public void flattenData(int keepYear, int keepMonth)
    {
        // extract last day that is flattened
        Calendar c = Calendar.getInstance();

        int lastYearToFlatten = keepYear;
        int lastMonthToFlatten = keepMonth - 1; // off by one
        // adjust if current month/year is reached
        if (lastYearToFlatten > c.get(Calendar.YEAR))
        {
            lastYearToFlatten = c.get(Calendar.YEAR);
            lastMonthToFlatten = c.get(Calendar.MONTH);
        }
        else if (lastMonthToFlatten > c.get(Calendar.MONTH))
        {
            lastMonthToFlatten = c.get(Calendar.MONTH);
        }

        // obtain first date that is not flattened
        int currentYear = c.get(Calendar.YEAR) + 1;
        int currentMonth;
        int minimumDay = c.get(Calendar.DAY_OF_YEAR);
        try
        {
            DatabaseStatement stmt = dbConn.createStatement("SELECT year, MIN(day) FROM aggregation_statistics WHERE day >= 0 GROUP BY year HAVING year = MIN(year)");
            DatabaseResultSet rs = stmt.executeQuery();
            if (rs.moveToFirst())
            {
                currentYear = rs.getInt(1);
                minimumDay = rs.getInt(2);
            }
        }
        catch (SQLException e)
        {
            logger.e(e.toString());
            return;
        }
        c.set(Calendar.YEAR, currentYear);
        c.set(Calendar.DAY_OF_YEAR, minimumDay);
        currentMonth = c.get(Calendar.MONTH);

        // process for each month inbetween
        while ((currentYear == lastYearToFlatten && currentMonth <= lastMonthToFlatten) || currentYear < lastYearToFlatten)
        {
            c.clear();
            c.set(Calendar.YEAR, currentYear);
            c.set(Calendar.MONTH, currentMonth);
            int startDayToFlatten = c.get(Calendar.DAY_OF_YEAR);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            int endDayToFlatten = c.get(Calendar.DAY_OF_YEAR);

            // flatten data
            flattenDataMonth(currentYear, startDayToFlatten, endDayToFlatten, currentMonth + 1);

            // update information for next month
            c.add(Calendar.DAY_OF_MONTH, 1);
            currentYear = c.get(Calendar.YEAR);
            currentMonth = c.get(Calendar.MONTH);
        }
    }

    @Override
    public void shutdownHandler()
    {
        boolean trunning = (exThread != null && exThread.isAlive());
        while (trunning)
        {
            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            trunning = (exThread != null && exThread.isAlive());
        }
    }

    @Override
    public void deletePersonalData()
    {
        // Nothing to do here
    }

    @Override
    public void onStart(boolean live)
    {
        lastUpdate = -1;
        lastYear = -1;
        lastDayOfYear = -1;
        prepareThread();
        exThread.start();
    }

    @Override
    public void onStop()
    {
        waitForThread();
        exThread = null;
    }

    private static class NTuple<V>
    {
        private V[] entries;

        private NTuple(V[] values)
        {
            this.entries = values;
        }

        @SafeVarargs
        public static <V> NTuple<V> createTuple(V... entries)
        {
            return new NTuple<>(entries);
        }

        public V getEntry(int index)
        {
            return entries[index];
        }

        public int size()
        {
            return entries.length;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(entries);
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
            NTuple<?> other = (NTuple<?>) obj;
            return Arrays.equals(entries, other.entries);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < size(); ++i)
            {
                sb.append(getEntry(i).toString());
                if (i + 1 < size())
                    sb.append(',');
            }
            sb.append(')');
            return sb.toString();
        }
    }

    private static class ServiceSet extends HashSet<Integer>
    {
        private int id;

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        // IMPORTANT: Do not use id for hashCode() or equals() !
        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            return super.equals(o);
        }
    }

    private class StorageHelperThread extends Thread
    {
        private int timeInterval;
        private int counterInterval;
        private ReentrantLock lock;
        private Condition exNeeded;
        private Runnable onExecute;
        private volatile int counter;
        private volatile boolean exit;

        public StorageHelperThread(int timeInterval, int counterInterval, Runnable onExecute)
        {
            this.counterInterval = counterInterval;
            this.timeInterval = timeInterval;
            lock = new ReentrantLock();
            exNeeded = lock.newCondition();
            this.onExecute = onExecute;
        }

        public void run()
        {
            while (!exit)
            {
                lock.lock();
                try
                {
                    exNeeded.await(timeInterval, TimeUnit.SECONDS);
                    if (onExecute != null && !exit)
                        onExecute.run();
                }
                catch (InterruptedException e)
                {
                    logger.w(e.toString());
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    lock.unlock();
                }
            }
            if (onExecute != null)
                onExecute.run();
        }

        public void updateCounter(int diff)
        {
            counter += diff;
            if (counter >= counterInterval)
            {
                counter = 0;
                lock.lock();
                try
                {
                    exNeeded.signal();
                }
                finally
                {
                    lock.unlock();
                }
            }
        }

        public void exit()
        {
            exit = true;
            lock.lock();
            try
            {
                exNeeded.signalAll();
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}
