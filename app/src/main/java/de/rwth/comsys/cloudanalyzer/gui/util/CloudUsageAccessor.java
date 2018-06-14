package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Pair;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.FlowDirection;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import static de.rwth.comsys.cloudanalyzer.gui.util.TrafficPropertiesSQL.trafficPropertyDILP;
import static de.rwth.comsys.cloudanalyzer.gui.util.TrafficPropertiesSQL.trafficPropertyILP;

public class CloudUsageAccessor
{
    private static final String TAG = "CloudUsageAccessor";

    public static DatabaseResultSet getAppList()
    {
        String sql = "SELECT app FROM apps";
        DatabaseResultSet res = query(sql);
        if (res != null)
        {
            return res;
        }
        return null;
    }

    public static String getAppName(int app)
    {
        String sql = "SELECT app FROM apps WHERE id == " + app;
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst())
        {
            String resString = res.getString(1);
            res.close();
            return resString;
        }
        else
        {
            if (res != null)
            {
                res.close();
            }
            return null;
        }
    }

    public static int getAppID(String app)
    {
        String sql = "SELECT id FROM apps WHERE app == '" + app + "'";
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst())
        {
            int resInt = res.getInt(1);
            res.close();
            return resInt;
        }
        else
        {
            if (res != null)
            {
                res.close();
            }
            return -1;
        }
    }

    public static long getAppCloudTraffic(int app, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(bytes) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND aggregation_data.app == " + Integer.toString(app) + " AND service == -1 AND handler == -1 AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY apps.app";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(bytes) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND '366' AND year == " + startYear + ") OR (day BETWEEN '0' AND " + endDay + " AND year == " + endYear + ")) AND aggregation_data.app == " + Integer.toString(app) + " AND service == -1 AND handler == -1 AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY apps.app";
        }
        else
        {
            sql = "SELECT SUM(bytes) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND '366' AND year == " + startYear + ") OR (day BETWEEN '0' AND '366' AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN '0' AND " + endDay + " AND year == " + endYear + ")) AND aggregation_data.app == " + Integer.toString(app) + " AND service == -1 AND handler == -1 AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY apps.app";
        }
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst())
        {
            Long resLong = Long.parseLong(res.getString(1));
            res.close();
            return resLong;
        }
        else
        {
            if (res != null)
            {
                res.close();
            }
            return 0L;
        }
    }

    public static long getAppTotalTraffic(int app, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(value) FROM aggregation_statistics INNER JOIN apps ON aggregation_statistics.app == apps.id WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND aggregation_statistics.app == " + app + " AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 " + "AND name == 'totalIpPacketLength'  GROUP BY apps.id ORDER BY SUM(value) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(value) FROM aggregation_statistics INNER JOIN apps ON aggregation_statistics.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND aggregation_statistics.app == " + app + " AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 " + "AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY SUM(value) DESC";
        }
        else
        {
            sql = "SELECT SUM(value) FROM aggregation_statistics INNER JOIN apps ON aggregation_statistics.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND aggregation_statistics.app == " + app + " AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 " + "AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY SUM(value) DESC";
        }
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getString(1) != null)
        {
            Long resLong = Long.parseLong(res.getString(1));
            res.close();
            return resLong;
        }
        else
        {
            if (res != null)
            {
                res.close();
            }
            return 0L;
        }
    }

    public static Pair<Float, Float> getAppTraffic(int appID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN value ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN value ELSE 0 END) as down FROM aggregation_statistics WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND app == " + appID + " AND importance == -1 AND link == -1 AND protocol == -1 AND name == 'totalIpPacketLength'";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN value ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN value ELSE 0 END) as down FROM aggregation_statistics WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND app == " + appID + "  importance == -1 AND link == -1 AND protocol == -1 AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY total DESC";
        }
        else
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN value ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN value ELSE 0 END) as down FROM aggregation_statistics WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND app == " + appID + " AND importance == -1 AND link == -1 AND protocol == -1 AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY total DESC";
        }
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getFloat(1) != null)
        {
            Float up = res.getFloat(1) / 1024f / 1024f;
            Float down = res.getFloat(2) / 1024f / 1024f;
            res.close();
            return new Pair<>(up, down);
        }
        return new Pair<>(0f, 0f);
    }

    public static Pair<Float, Float> getServiceTraffic(int serviceID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND service == " + serviceID + " AND importance == -1 AND link == -1 AND protocol == -1 AND handler == -1 AND app == -1 GROUP BY service";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND service == " + serviceID + "  importance == -1 AND link == -1 AND protocol == -1 AND handler == -1 AND app == -1 GROUP BY service";
        }
        else
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND service == " + serviceID + " AND importance == -1 AND link == -1 AND protocol == -1 AND handler == -1 AND app == -1 GROUP BY service";
        }
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getFloat(1) != null)
        {
            Float up = res.getFloat(1) / 1024f / 1024f;
            Float down = res.getFloat(2) / 1024f / 1024f;
            res.close();
            return new Pair<>(up, down);
        }
        return new Pair<>(0f, 0f);
    }

    public static Pair<Float, Float> getRegionTraffic(int regionID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND region == " + regionID + " AND importance == -1 AND link == -1 AND protocol == -1 AND handler == -1 AND app == -1 AND service == -1 GROUP BY region";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND region == " + regionID + " AND importance == -1 AND link == -1 AND protocol == -1 AND handler == -1 AND app == -1 AND service == -1 GROUP BY region";
        }
        else
        {
            sql = "SELECT SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND region == " + regionID + " AND importance == -1 AND link == -1 AND protocol == -1 AND handler == -1 AND app == -1 AND service == -1 GROUP BY region";
        }
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getFloat(1) != null)
        {
            Float up = res.getFloat(1) / 1024f / 1024f;
            Float down = res.getFloat(2) / 1024f / 1024f;
            res.close();
            return new Pair<>(up, down);
        }
        return new Pair<>(0f, 0f);
    }

    public static ArrayList<AppEntry> getSortedApps(int startDay, int startYear, int endDay, int endYear, Context context)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> filterApps = sharedPrefs.getStringSet("pref_db_debug_information", CaptureConstants.DEBUG_APPS);

        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT apps.app, apps.id, SUM(CASE WHEN direction == -1 THEN value ELSE 0 END) as total,  SUM(CASE WHEN direction == 1 THEN value ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN value ELSE 0 END) as down FROM aggregation_statistics INNER JOIN apps ON aggregation_statistics.app == apps.id WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND importance == -1 AND link == -1 AND protocol == -1 AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY total DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT apps.app, apps.id, SUM(CASE WHEN direction == -1 THEN value ELSE 0 END) as total,  SUM(CASE WHEN direction == 1 THEN value ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN value ELSE 0 END) as down FROM aggregation_statistics INNER JOIN apps ON aggregation_statistics.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND importance == -1 AND link == -1 AND protocol == -1 AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY total DESC";
        }
        else
        {
            sql = "SELECT apps.app, apps.id, SUM(CASE WHEN direction == -1 THEN value ELSE 0 END) as total,  SUM(CASE WHEN direction == 1 THEN value ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN value ELSE 0 END) as down FROM aggregation_statistics INNER JOIN apps ON aggregation_statistics.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND importance == -1 AND link == -1 AND protocol == -1 AND name == 'totalIpPacketLength' GROUP BY apps.id ORDER BY total DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<AppEntry> sortedApps = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            String app = res.getString(1);
            if (app == null)
                break;
            if (filterApps.contains(app))
                continue;
            sortedApps.add(new AppEntry(app, res.getInt(2), i, res.getFloat(3) / 1024f / 1024f, 0f, res.getFloat(4) / 1024f / 1024f, res.getFloat(5) / 1024f / 1024f, context));
        }
        res.close();
        if (startYear == endYear)
        {
            sql = "SELECT aggregation_data.app, SUM(bytes) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND service == -1 AND handler == -1 AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY apps.app";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT aggregation_data.app, SUM(bytes) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND '366' AND year == " + startYear + ") OR (day BETWEEN '0' AND " + endDay + " AND year == " + endYear + ")) AND service == -1 AND handler == -1 AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY apps.app";
        }
        else
        {
            sql = "SELECT aggregation_data.app, SUM(bytes) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE ((day BETWEEN " + startDay + "  AND '366' AND year == " + startYear + ") OR (day BETWEEN '0' AND '366' AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN '0' AND " + endDay + " AND year == " + endYear + ")) AND service == -1 AND handler == -1 AND direction == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY apps.app";
        }
        res = query(sql);
        for (int i = 0; res.next(); i++)
        {
            if (res.getInt(1) == null)
                break;
            for (AppEntry entry : sortedApps)
            {
                if (entry.getId() == res.getInt(1))
                {
                    entry.setCloudTraffic(res.getFloat(2) / 1024f / 1024f);
                }
            }
        }
        res.close();
        return sortedApps;
    }

    public static ArrayList<RegionEntry> getSortedRegions(int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT region, SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) as total, SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND region != -1 AND service == -1 AND handler == -1 AND app == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY region ORDER BY SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT region, SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) as total, SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND region != -1 AND service == -1 AND handler == -1 AND app == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY region ORDER BY SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT region, SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) as total, SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND region != -1 AND service == -1 AND handler == -1 AND app == -1 AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY region ORDER BY SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<RegionEntry> sortedRegions = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            if (res.getString(1) == null)
                break;
            sortedRegions.add(new RegionEntry(res.getInt(1), MainHandler.getRegions().getNode(res.getInt(1)).getValue(), i, res.getFloat(3) / 1024f / 1024f, res.getFloat(4) / 1024f / 1024f));
        }
        res.close();
        return sortedRegions;
    }

    public static ArrayList<CloudAppEntry> getSortedRegionsApps(int id, int startDay, int startYear, int endDay, int endYear, Context context)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> filterApps = sharedPrefs.getStringSet("pref_db_debug_information", CaptureConstants.DEBUG_APPS);

        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT apps.app, apps.id, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_data INNER JOIN apps ON aggregation_data.app == apps.id WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND service == -1 AND handler == -1 AND region = " + id + trafficPropertyILP + " GROUP BY apps.id ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT appName, appID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND regionID = " + id + trafficPropertyILP + " GROUP BY appID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT appName, appID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND regionID = " + id + trafficPropertyILP + " GROUP BY appID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<CloudAppEntry> sortedApps = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            String app = res.getString(1);
            if (app == null)
                break;
            if (filterApps.contains(app))
                continue;
            if (res.getString(1) == null)
                break;
            sortedApps.add(new CloudAppEntry(res.getString(1), res.getInt(2), i, res.getFloat(4) / 1024f / 1024f, res.getFloat(3) / 1024f / 1024f, context));
        }
        res.close();
        return sortedApps;
    }

    public static ArrayList<ServiceEntry> getSortedRegionsServices(int id, int startDay, int startYear, int endDay, int endYear, Context context)
    {
        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT serviceID, serviceName, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND regionID = " + id + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT serviceID, serviceName, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND regionID = " + id + trafficPropertyILP + " GROUP BY regionID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT serviceID, serviceName, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND regionID = " + id + trafficPropertyILP + " GROUP BY regionID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<ServiceEntry> sortedServices = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            if (res.getString(1) == null)
                break;
            sortedServices.add(new ServiceEntry(res.getInt(1), res.getString(2), i, res.getFloat(4) / 1024f / 1024f, res.getFloat(3) / 1024f / 1024f, -1));
        }
        res.close();
        return sortedServices;
    }

    public static ArrayList<ServiceEntry> getSortedServices(int startDay, int startYear, int endDay, int endYear)
    {
        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT serviceID, serviceName, SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) as total, SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY serviceID ORDER BY total DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT serviceID, serviceName, SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) as total, SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY serviceID ORDER BY total DESC";
        }
        else
        {
            sql = "SELECT serviceID, serviceName, SUM(CASE WHEN direction == -1 THEN bytes ELSE 0 END) as total, SUM(CASE WHEN direction == 1 THEN bytes ELSE 0 END) as up, SUM(CASE WHEN direction == 0 THEN bytes ELSE 0 END) as down FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND importance == -1 AND link == -1 AND protocol == -1 GROUP BY serviceID ORDER BY total DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<ServiceEntry> sortedServices = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            if (res.getString(1) == null)
                break;
            sortedServices.add(new ServiceEntry(res.getInt(1), res.getString(2), i, res.getFloat(4) / 1024f / 1024f, res.getFloat(5) / 1024f / 1024f, -1));
        }
        res.close();
        return sortedServices;
    }

    public static ArrayList<CloudAppEntry> getSortedServicesApps(int id, int startDay, int startYear, int endDay, int endYear, Context context)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> filterApps = sharedPrefs.getStringSet("pref_db_debug_information", CaptureConstants.DEBUG_APPS);

        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT appName, appID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND serviceID = " + id + trafficPropertyILP + " GROUP BY appID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT appName, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + "))  AND serviceID = " + id + trafficPropertyILP + " GROUP BY appID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT appName, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + "))  AND serviceID = " + id + trafficPropertyILP + " GROUP BY appID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<CloudAppEntry> sortedApps = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            String app = res.getString(1);
            if (app == null)
                break;
            if (filterApps.contains(app))
                continue;
            sortedApps.add(new CloudAppEntry(app, res.getInt(2), i, res.getFloat(4) / 1024f / 1024f, res.getFloat(3) / 1024f / 1024f, context));
        }
        res.close();
        return sortedApps;
    }

    public static ArrayList<ServiceEntry> getSortedAppServicesRegion(int appID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT serviceName, serviceID, regionID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND appID = " + appID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID, regionID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT serviceName, serviceID, regionID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND appID = " + appID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID, regionID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT serviceName, serviceID, regionID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND appID = " + appID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID, regionID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<ServiceEntry> sortedServices = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            if (res.getString(1) == null)
                break;
            sortedServices.add(new ServiceEntry(res.getInt(2), res.getString(1) + " (" + MainHandler.getRegions().getNode(res.getInt(3)).getValue() + ")", i, res.getFloat(5) / 1024f / 1024f, res.getFloat(4) / 1024f / 1024f, res.getInt(3)));
        }
        res.close();
        return sortedServices;
    }

    public static ArrayList<ServiceEntry> getSortedAppServices(int appID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT serviceName, serviceID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND appID = " + appID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT serviceName, serviceID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND appID = " + appID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT serviceName, serviceID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND appID = " + appID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        DatabaseResultSet res = query(sql);
        ArrayList<ServiceEntry> sortedServices = new ArrayList<>();
        for (int i = 0; res.next(); i++)
        {
            if (res.getString(1) == null)
                break;
            sortedServices.add(new ServiceEntry(res.getInt(2), res.getString(1), i, res.getFloat(4) / 1024f / 1024f, res.getFloat(3) / 1024f / 1024f, -1));
        }
        res.close();
        return sortedServices;
    }

    public static Pair<Float, Float> getAppServiceTraffic(int appID, int serviceID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;

        if (startYear == endYear)
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND appID = " + appID + " AND serviceID = " + serviceID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND appID = " + appID + " AND serviceID = " + serviceID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }
        else
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND appID = " + appID + " AND serviceID = " + serviceID + " AND regionID != -1" + trafficPropertyILP + " GROUP BY serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC";
        }

        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getFloat(1) != null)
        {
            Float up = res.getFloat(2) / 1024f / 1024f;
            Float down = res.getFloat(1) / 1024f / 1024f;
            res.close();
            return new Pair<>(up, down);
        }
        return new Pair<>(0f, 0f);
    }

    public static ArrayList<ServiceEntry> getSortedAppServiceCoherence(int appID, int serviceID, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        String service = MainHandler.getService(serviceID).getName();
        ArrayList<ServiceEntry> sortedServices = new ArrayList<>();

        try
        {
            DatabaseStatement querySet = MainHandler.getDbConn().createStatement("SELECT `set` FROM service_set_members WHERE service == " + Integer.toString(serviceID));
            DatabaseResultSet resSet = querySet.executeQuery();

            int i = 0;
            while (resSet.next())
            {
                if (resSet.getInt(1) == null)
                    break;

                if (startYear == endYear)
                {
                    sql = "SELECT serviceName, serviceID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) FROM service_sets_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND serviceSet = ? AND serviceID != ? AND appID = ?" + trafficPropertyILP + " GROUP BY serviceSet, serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC ";
                }
                else if (startYear == endYear - 1)
                {
                    sql = "SELECT serviceName, serviceID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) FROM service_sets_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND serviceSet = ? AND serviceID != ? AND appID = ?" + trafficPropertyILP + " GROUP BY serviceSet, serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC ";
                }
                else
                {
                    sql = "SELECT serviceName, serviceID, SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) FROM service_sets_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND serviceSet = ? AND serviceID != ? AND appID = ?" + trafficPropertyILP + " GROUP BY serviceSet, serviceID ORDER BY SUM(CASE WHEN direction = -1 THEN bytes ELSE 0 END) DESC ";
                }

                DatabaseStatement query = MainHandler.getDbConn().createStatement(sql);
                System.out.println(resSet.getInt(1));
                query.setInt(1, resSet.getInt(1));
                query.setInt(2, serviceID);
                query.setInt(3, appID);

                DatabaseResultSet res = query.executeQuery();
                while (res.next())
                {
                    if (res.getString(1) == null)
                        break;

                    if (sortedServices.isEmpty())
                    {
                        sortedServices.add(new ServiceEntry(res.getInt(2), res.getString(1) + " + " + service, i, res.getFloat(4) / 1024f / 1024f, res.getFloat(3) / 1024f / 1024f, -1));
                    }
                    else
                    {
                        int j = 0;
                        while (j < sortedServices.size() && (res.getFloat(5) / 1024f / 1024f) < (sortedServices.get(j).getUpTraffic() + sortedServices.get(j).getDownTraffic()))
                        {
                            j++;
                        }
                        sortedServices.add(j, new ServiceEntry(res.getInt(2), res.getString(1) + " + " + service, i, res.getFloat(4) / 1024f / 1024f, res.getFloat(3) / 1024f / 1024f, -1));
                    }
                    i++;
                }
                res.close();
            }

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return sortedServices;

    }


    public static Pair<Float, Float> getTotalTraffic(int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN value ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN value ELSE 0 END) FROM aggregation_statistics WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND app == -1 AND importance == -1 and link == -1 AND protocol == -1 AND name == 'totalIpPacketLength'";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN value ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN value ELSE 0 END) FROM aggregation_statistics WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND app == -1 AND importance == -1 and link == -1 AND protocol == -1 AND name == 'totalIpPacketLength'";
        }
        else
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN value ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN value ELSE 0 END) FROM aggregation_statistics WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND app == -1 AND importance == -1 and link == -1 AND protocol == -1 AND name == 'totalIpPacketLength'";
        }

        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getFloat(1) != null)
        {
            Float up = res.getFloat(2) / 1024f / 1024f;
            Float down = res.getFloat(1) / 1024f / 1024f;
            res.close();
            return new Pair<>(up, down);
        }
        return new Pair<>(0f, 0f);
    }

    public static Pair<Float, Float> getTotalCloudTraffic(int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT SUM(CASE WHEN direction == " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction == " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_data WHERE day BETWEEN " + startDay + " AND " + endDay + " AND year == " + startYear + " AND app == -1 AND handler == -1 AND service == -1 " + trafficPropertyILP;
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND app == -1 AND handler == -1 AND service == -1 " + trafficPropertyILP;
        }
        else
        {
            sql = "SELECT SUM(CASE WHEN direction = " + FlowDirection.IN.getId() + " THEN bytes ELSE 0 END), SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND app == -1 AND handler == -1 AND service == -1 " + trafficPropertyILP;
        }

        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst() && res.getFloat(1) != null)
        {
            Float up = res.getFloat(2) / 1024f / 1024f;
            Float down = res.getFloat(1) / 1024f / 1024f;
            res.close();
            return new Pair<>(up, down);
        }
        return new Pair<>(0f, 0f);
    }

    public static DatabaseResultSet getAppRegionPieData(int app, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT region, SUM(bytes), SUM(packets) FROM aggregation_data WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND region != -1 AND app = " + app + " AND handler = -1 AND service = -1" + trafficPropertyDILP + " GROUP BY region ORDER BY region ASC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT region, SUM(bytes), SUM(packets) FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND region != -1 AND app = " + app + " AND handler = -1 AND service = -1" + trafficPropertyDILP + " GROUP BY region ORDER BY region ASC";
        }
        else
        {
            sql = "SELECT region, SUM(bytes), SUM(packets) FROM aggregation_data WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND region != -1 AND app = " + app + " AND handler = -1 AND service = -1" + trafficPropertyDILP + " GROUP BY region ORDER BY region ASC";
        }

        DatabaseResultSet res = query(sql);
        return res;
    }

    public static DatabaseResultSet getServiceRegionPieData(String service, int startDay, int startYear, int endDay, int endYear)
    {
        String sql;
        if (startYear == endYear)
        {
            sql = "SELECT regionID, SUM(bytes) FROM aggregation_view WHERE day BETWEEN " + startDay + "  AND " + endDay + " AND year == " + startYear + " AND serviceName = '" + service + "' AND regionID != -1" + trafficPropertyDILP + " GROUP BY regionID ORDER BY regionID ASC";
        }
        else if (startYear == endYear - 1)
        {
            sql = "SELECT regionID, SUM(bytes) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND serviceName = '" + service + "' AND regionID != -1" + trafficPropertyDILP + " GROUP BY regionID ORDER BY regionID ASC";
        }
        else
        {
            sql = "SELECT regionID, SUM(bytes) FROM aggregation_view WHERE ((day BETWEEN " + startDay + "  AND 366 AND year == " + startYear + ") OR (day BETWEEN 0 AND 366 AND year BETWEEN " + Integer.toString(startYear + 1) + " AND " + Integer.toString(endYear - 1) + ") OR (day BETWEEN 0 AND " + endDay + " AND year == " + endYear + ")) AND AND serviceName = '" + service + "' AND regionID != -1" + trafficPropertyDILP + " GROUP BY regionID ORDER BY regionID ASC";
        }

        DatabaseResultSet res = query(sql);
        return res;
    }

    public static long getFirstDate()
    {
        Calendar cal = Calendar.getInstance();
        String sql = "SELECT year, day FROM aggregation_statistics ORDER BY year, day";
        DatabaseResultSet res = query(sql);
        if (res != null && res.moveToFirst())
        {
            cal.set(Calendar.YEAR, Integer.parseInt(res.getString(1)));
            cal.set(Calendar.DAY_OF_YEAR, Integer.parseInt(res.getString(2)));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.MILLISECOND, 0);
            res.close();
            return cal.getTimeInMillis();
        }
        if (res != null)
        {
            res.close();
        }
        return 0;
    }

    public static DatabaseResultSet query(String sql)
    {
        try
        {
            DatabaseConnection dbConn = MainHandler.getDbConn();
            if (dbConn == null)
            {
                return null;
            }
            DatabaseStatement query = dbConn.createStatement(sql);
            return query.executeQuery();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatTraffic(float megabytes, Locale locale)
    {
        NumberFormat nf = NumberFormat.getInstance(locale);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#.##");
        if (megabytes == 0.0f)
        {
            return df.format(megabytes) + " MB";
        }
        else if (megabytes < 0.01f)
        {
            return df.format(megabytes * 1024f) + " KB";
        }
        else if (megabytes >= 100f && megabytes / 1024f < 0.01f)
        {
            return df.format(megabytes / 1024f) + " GB";
        }
        else if (megabytes / 1024f >= 100f)
        {
            return df.format(megabytes / 1024f / 1024f) + " TB";
        }
        else
        {
            return df.format(megabytes) + " MB";
        }
    }
}
