package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.gui.util.AsyncTaskProgressDialog;
import de.rwth.comsys.cloudanalyzer.gui.util.ChartDataSet;
import de.rwth.comsys.cloudanalyzer.gui.util.DateConversion;
import de.rwth.comsys.cloudanalyzer.gui.util.StatsChart;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public abstract class StatsDataAmountAbstract extends Fragment
{
    private final Calendar maxCal = Calendar.getInstance();
    private final Calendar minCal = Calendar.getInstance();
    protected String yAxis, sql_total, sql_cloud, legend_total, legend_cloud;
    private StatsChart mChart;
    private ArrayList<String> xVals = null;
    private ArrayList<Entry> yVals_total = null, yVals_cloud = null;
    private boolean cloudData = false, detailView = false, regionData = false;

    public StatsDataAmountAbstract()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_app_data_amount_general, container, false);
        if (yAxis != null)
        {
            TextView text = view.findViewById(R.id.appDataAmountLegendY);
            text.setText(yAxis);
        }

        mChart = view.findViewById(R.id.appDataAmountChart);

        minCal.setTimeInMillis(getArguments().getLong("start"));
        maxCal.setTimeInMillis(getArguments().getLong("end"));
        cloudData = getArguments().getBoolean("cloudData", false);
        detailView = getArguments().getBoolean("detailView", false);
        regionData = getArguments().getBoolean("regionData", false);

        if (detailView)
        {
            if (regionData)
            {
                String[] params = {getArguments().getString("name"), getArguments().getString("service"), getArguments().getString("region")};
                new DatabaseGetStat(getActivity()).execute(params);
            }
            else
            {
                String[] params = {getArguments().getString("name"), getArguments().getString("service")};
                new DatabaseGetStat(getActivity()).execute(params);
            }
        }
        else
        {
            new DatabaseGetStat(getActivity()).execute(getArguments().getString("name"));
        }

        return view;
    }

    public void update(long start, long end)
    {
        minCal.setTimeInMillis(start);
        maxCal.setTimeInMillis(end);
        setData();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null)
        {
            Long minCalVal = savedInstanceState.getLong("minCalVal");
            Long maxCalVal = savedInstanceState.getLong("maxCalVal");
            if (minCalVal != 0 && maxCalVal != 0)
            {
                // set global calendars to new times
                minCal.setTimeInMillis(minCalVal);
                maxCal.setTimeInMillis(maxCalVal);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putLong("minCalVal", minCal.getTimeInMillis());
        outState.putLong("maxCalVal", maxCal.getTimeInMillis());
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private void setData()
    {
        LineData lineData = new LineData();
        ArrayList<String> xVals_line = new ArrayList<>();
        ArrayList<Entry> yVals_total_line = new ArrayList<>();
        ArrayList<Entry> yVals_cloud_line = new ArrayList<>();
        Calendar lastCal = Calendar.getInstance();
        Calendar curCal = Calendar.getInstance();
        curCal.setTimeInMillis(minCal.getTimeInMillis());
        lastCal.setTimeInMillis(maxCal.getTimeInMillis());

        curCal.set(Calendar.MILLISECOND, 0);
        curCal.add(Calendar.HOUR_OF_DAY, 0);
        int index = 0;
        while (!curCal.after(lastCal))
        {
            xVals_line.add(DateConversion.getDateString(curCal.get(Calendar.DAY_OF_YEAR), curCal.get(Calendar.YEAR)));
            int cur = -1;
            if (xVals != null && xVals.size() > 0)
                cur = xVals.indexOf(DateConversion.getDateString(curCal.get(Calendar.DAY_OF_YEAR), curCal.get(Calendar.YEAR)));
            if (cur != -1 && yVals_total != null && yVals_total.size() > cur && yVals_total.get(cur) != null && yVals_cloud != null && yVals_cloud.size() > cur && yVals_cloud.get(cur) != null)
            {
                if (index > 0)
                {
                    yVals_total_line.add(new Entry(index, yVals_total_line.get(index - 1).getY() + yVals_total.get(cur).getY()));
                    yVals_cloud_line.add(new Entry(index, yVals_cloud_line.get(index - 1).getY() + yVals_cloud.get(cur).getY()));
                }
                else
                {
                    yVals_total_line.add(new Entry(index, yVals_total.get(cur).getY()));
                    yVals_cloud_line.add(new Entry(index, yVals_cloud.get(cur).getY()));
                }

            }
            else if (xVals != null && yVals_cloud != null && yVals_total != null && yVals_cloud_line.size() > 0 && yVals_total_line.size() > 0)
            {
                yVals_total_line.add(new Entry(index, yVals_total_line.get(yVals_total_line.size() - 1).getY()));
                yVals_cloud_line.add(new Entry(index, yVals_cloud_line.get(yVals_cloud_line.size() - 1).getY()));
            }
            else
            {
                yVals_total_line.add(new Entry(index, 0));
                yVals_cloud_line.add(new Entry(index, 0));
            }
            curCal.add(Calendar.DATE, 1);
            index++;
        }
        if (!yVals_cloud_line.isEmpty() && !yVals_total_line.isEmpty())
        {

            LineDataSet lineDataSetBase, lineDataSetCloud;
            if (cloudData)
            {
                lineDataSetBase = ChartDataSet.newStatLineDataSet(yVals_total_line, legend_total, "#318BC1");
                lineDataSetCloud = ChartDataSet.newStatLineDataSet(yVals_cloud_line, legend_cloud, "#7FC5EE");
            }
            else
            {
                lineDataSetBase = ChartDataSet.newStatLineDataSet(yVals_total_line, legend_total, "#434348");
                lineDataSetCloud = ChartDataSet.newStatLineDataSet(yVals_cloud_line, legend_cloud, "#318BC1");
            }
            lineDataSetBase.setDrawValues(false);
            lineDataSetCloud.setDrawValues(false);

            if (yVals_cloud_line.size() == 1 && yVals_total_line.size() == 1)
            {
                lineDataSetBase.setCircleRadius(2);
                lineDataSetCloud.setCircleRadius(2);
            }
            lineData.addDataSet(lineDataSetBase);
            lineData.addDataSet(lineDataSetCloud);
        }

        mChart.setData(lineData, xVals_line);
        if (yVals_total_line.size() == 1)
        {
            mChart.setLabelCout(3);
        }
        else if (yVals_total_line.size() < 5)
        {
            mChart.setLabelCout(yVals_total_line.size());
        }

        mChart.getLegend().setEnabled(true);
        List<LegendEntry> list = new ArrayList<>();
        if (cloudData)
        {
            LegendEntry entry_total = new LegendEntry(legend_total, Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, ColorTemplate.rgb("#318BC1"));
            list.add(entry_total);
            LegendEntry entry_cloud = new LegendEntry(legend_cloud, Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, ColorTemplate.rgb("#7FC5EE"));
            list.add(entry_cloud);
        }
        else
        {
            LegendEntry entry_total = new LegendEntry(legend_total, Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, ColorTemplate.rgb("#434348"));
            list.add(entry_total);
            LegendEntry entry_cloud = new LegendEntry(legend_cloud, Legend.LegendForm.DEFAULT, Float.NaN, Float.NaN, null, ColorTemplate.rgb("#318BC1"));
            list.add(entry_cloud);
        }

        mChart.getLegend().setEntries(list);
        mChart.postInvalidate();
    }

    private class DatabaseGetStat extends AsyncTaskProgressDialog<String, Void, Boolean>
    {

        private DatabaseGetStat(Activity activity)
        {
            super(activity);
        }

        @Override
        protected Boolean doInBackground(String... strings)
        {
            try
            {
                DatabaseStatement query;
                DatabaseResultSet res_cloud;
                boolean next_res;
                query = MainHandler.getDbConn().createStatement(sql_cloud);
                if (cloudData)
                {
                    query.setString(1, strings[0]);
                    res_cloud = query.executeQuery();
                    next_res = res_cloud.next();
                }
                else
                {
                    if (detailView)
                    {
                        query.setInt(1, Integer.parseInt(strings[1]));
                        query.setInt(2, MainHandler.getAppId(strings[0]));
                        if (regionData)
                        {
                            query.setInt(3, Integer.parseInt(strings[2]));
                        }
                    }
                    else
                    {
                        query.setInt(1, MainHandler.getAppId(strings[0]));
                    }
                    res_cloud = query.executeQuery();
                    next_res = res_cloud.next();
                }


                query = MainHandler.getDbConn().createStatement(sql_total);
                if (!cloudData)
                {
                    query.setInt(1, MainHandler.getAppId(strings[0]));

                }
                DatabaseResultSet res_total = query.executeQuery();

                yVals_total = new ArrayList<>();
                yVals_cloud = new ArrayList<>();
                xVals = new ArrayList<>();

                for (int i = 0; res_total.next(); i++)
                {
                    if (res_total.getInt(1) == null)
                        break;

                    xVals.add(DateConversion.getDateString(res_total.getInt(1), res_total.getInt(2)));
                    yVals_total.add(new Entry(i, ((float) res_total.getLong(3)) / 1048576));
                    if (next_res && res_cloud.getInt(1) != null && xVals.get(i).equals(DateConversion.getDateString(res_cloud.getInt(1), res_cloud.getInt(2))))
                    {
                        yVals_cloud.add(new Entry(i, ((float) res_cloud.getLong(3)) / 1048576));
                        next_res = res_cloud.next();
                    }
                    else
                    {
                        yVals_cloud.add(new Entry(i, 0));
                    }
                }
                return true;
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean res)
        {
            super.onPostExecute(res);
            setData();
        }
    }


}
