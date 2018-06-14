package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.content.Context;
import de.rwth.comsys.cloudanalyzer.R;

import static de.rwth.comsys.cloudanalyzer.gui.util.TrafficPropertiesSQL.trafficPropertyILP;

public class AppServiceDataAmountFragmentGeneralDown extends StatsDataAmountAbstract
{
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        sql_total = "SELECT day, year, SUM(value) FROM aggregation_statistics WHERE app = ? AND direction = 0 AND name = 'totalIpPacketLength' " + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
        sql_cloud = "SELECT day, year, SUM(bytes) FROM aggregation_view WHERE serviceID = ? AND appID = ? AND direction = 0 " + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
        legend_total = getResources().getString(R.string.overall_traffic);
        legend_cloud = getResources().getString(R.string.identified_traffic);
    }
}
