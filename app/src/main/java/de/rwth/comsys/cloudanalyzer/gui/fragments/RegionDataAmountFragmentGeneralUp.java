package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.content.Context;
import de.rwth.comsys.cloudanalyzer.R;

import static de.rwth.comsys.cloudanalyzer.gui.util.TrafficPropertiesSQL.trafficPropertyILP;

public class RegionDataAmountFragmentGeneralUp extends StatsDataAmountAbstract
{
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        sql_total = "SELECT day, year, SUM(bytes) FROM aggregation_data WHERE app = -1 AND direction = 1 AND handler = -1 AND service = -1" + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
        sql_cloud = "SELECT day, year, bytes FROM aggregation_data WHERE region = ? AND direction = 1 " + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
        legend_total = getResources().getString(R.string.overall_cloud_traffic);
        legend_cloud = getResources().getString(R.string.identified_region_traffic);
        yAxis = getString(R.string.stats_region_legendY);
    }
}
