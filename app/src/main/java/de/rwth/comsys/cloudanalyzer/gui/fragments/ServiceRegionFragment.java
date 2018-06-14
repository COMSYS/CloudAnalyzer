package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.gui.util.AsyncTaskProgressDialog;
import de.rwth.comsys.cloudanalyzer.gui.util.CloudUsageAccessor;
import de.rwth.comsys.cloudanalyzer.gui.util.PieChart;

import java.util.ArrayList;
import java.util.Calendar;

import static java.util.Calendar.DAY_OF_YEAR;

public class ServiceRegionFragment extends Fragment
{
    private final Calendar maxCal = Calendar.getInstance();
    private final Calendar minCal = Calendar.getInstance();
    private PieChart mChart;
    private TextView textView;

    public ServiceRegionFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_service_region, container, false);

        textView = view.findViewById(R.id.serviceRegionChartText);
        mChart = view.findViewById(R.id.serviceRegionChart);
        mChart.setLocale(getResources().getConfiguration().locale);

        minCal.setTimeInMillis(getArguments().getLong("start"));
        maxCal.setTimeInMillis(getArguments().getLong("end"));

        new DatabaseGetStats(getActivity()).execute(getArguments().getString("name"));

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

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

    public void update(long start, long end)
    {
        minCal.setTimeInMillis(start);
        maxCal.setTimeInMillis(end);
        new DatabaseGetStats(getActivity()).execute(getArguments().getString("name"));
    }


    private class DatabaseGetStats extends AsyncTaskProgressDialog<String, Void, PieData>
    {
        private DatabaseGetStats(Activity activity)
        {
            super(activity);
        }

        @Override
        protected PieData doInBackground(String... strings)
        {
            try
            {
                DatabaseResultSet res = CloudUsageAccessor.getServiceRegionPieData(strings[0], minCal.get(DAY_OF_YEAR), minCal.get(Calendar.YEAR), maxCal.get(DAY_OF_YEAR), maxCal.get(Calendar.YEAR));

                ArrayList<PieEntry> yVals = new ArrayList<>();

                for (; res.next(); )
                {
                    if (res.getInt(1) == null)
                        break;

                    yVals.add(new PieEntry(((float) res.getLong(2)) / 1048576, MainHandler.getRegions().getNode(res.getInt(1)).getValue()));
                }

                return new PieData(new PieDataSet(yVals, ""));
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(PieData pieData)
        {
            super.onPostExecute(pieData);

            if (pieData.getYValueSum() == 0)
            {
                textView.setVisibility(View.VISIBLE);
            }
            else
            {
                textView.setVisibility(View.INVISIBLE);
            }

            mChart.setData(pieData);

            mChart.postInvalidate();
        }
    }
}
