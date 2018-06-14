package de.rwth.comsys.cloudanalyzer.gui.util;

import de.rwth.comsys.cloudanalyzer.network.trafficProperty.FlowDirection;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Importance;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Link;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.Protocol;

public class TrafficPropertiesSQL
{
    private static String direction = " AND direction = " + FlowDirection.AGGREGATED.getId();
    private static String importance = " AND importance = " + Importance.AGGREGATED.getId();
    private static String link = " AND link = " + Link.AGGREGATED.getId();
    private static String protocol = " AND protocol = " + Protocol.AGGREGATED.getId();

    public static String trafficPropertyILP = importance + link + protocol;
    public static String trafficPropertyDILP = direction + importance + link + protocol;

}
