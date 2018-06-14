package de.rwth.comsys.cloudanalyzer.gui.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import de.rwth.comsys.cloudanalyzer.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.graphics.Color.rgb;

public class AppListAdapter extends BaseAdapter implements Serializable
{
    ArrayList<AppEntry> elementList;
    float maxTraffic;
    private Locale locale;

    public AppListAdapter(List<AppEntry> list, Locale locale, float maxTraffic)
    {
        this.locale = locale;
        this.elementList = new ArrayList<>();
        this.maxTraffic = maxTraffic;

        for (AppEntry app : list)
        {
            AppEntry entry = app;
            elementList.add(entry);
        }
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount()
    {
        return elementList.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public AppEntry getItem(int position)
    {
        return elementList.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position)
    {
        return elementList.get(position).getPackageName().hashCode();
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final View result;

        if (convertView == null)
        {
            result = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_list_single_chart_item, parent, false);
        }
        else
        {
            result = convertView;
        }

        AppEntry item = getItem(position);

        ((TextView) result.findViewById(R.id.tvLabel)).setText(item.getAppName());
        ((ImageView) result.findViewById(R.id.ivLogo)).setImageDrawable(item.getAppIcon(result.getContext()));
        StatsBarChart curChart = result.findViewById(R.id.statsBarChart);
        if (curChart != null && item.getPackageName() != null)
        {
            ((TextView) result.findViewById(R.id.tvLabelCloud)).setText(CloudUsageAccessor.formatTraffic(item.getCloudTraffic(), locale));
            ((TextView) result.findViewById(R.id.tvLabelTotal)).setText(CloudUsageAccessor.formatTraffic(item.getTotalTraffic(), locale));
            ArrayList<BarEntry> yValues = new ArrayList<>();
            yValues.add(new BarEntry(0, maxTraffic));
            yValues.add(new BarEntry(0, item.getTotalTraffic()));
            yValues.add(new BarEntry(0, item.getCloudTraffic()));

            BarDataSet set = new BarDataSet(yValues, "Test");
            set.setDrawValues(false);
            set.setColors(rgb(211, 211, 211), rgb(67, 67, 72), rgb(49, 139, 193));

            BarData data = new BarData(set);
            data.setBarWidth(30f);

            curChart.setData(data);
            curChart.setMax(maxTraffic);
            curChart.invalidate();
        }
        return result;
    }
}
