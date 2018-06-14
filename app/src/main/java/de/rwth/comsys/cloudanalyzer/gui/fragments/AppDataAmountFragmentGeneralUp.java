package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.content.Context;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.FlowDirection;

import static de.rwth.comsys.cloudanalyzer.gui.util.TrafficPropertiesSQL.trafficPropertyILP;

public class AppDataAmountFragmentGeneralUp extends StatsDataAmountAbstract
{
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        sql_total = "SELECT day, year, SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN value ELSE 0 END) FROM aggregation_statistics WHERE app = ? AND name = 'totalIpPacketLength' " + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
        sql_cloud = "SELECT day, year, SUM(CASE WHEN direction = " + FlowDirection.OUT.getId() + " THEN bytes ELSE 0 END) FROM aggregation_data WHERE app = ? AND handler = -1 AND service = -1" + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
        legend_total = getResources().getString(R.string.overall_upload);
        legend_cloud = getResources().getString(R.string.identified_upload);
    }
}
