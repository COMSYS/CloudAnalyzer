package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public class TrafficPropertiesManager
{
    public static final int PROPERTY_COUNT = 4;
    private static TrafficPropertiesManager instance;
    private TrafficProperty[][][][] properties;

    private TrafficPropertiesManager()
    {
        properties = new TrafficProperty[FlowDirection.values().length][Importance.values().length][Link.values().length][Protocol.values().length];
        initializeProperties();
    }

    private static TrafficPropertiesManager getInstance()
    {
        if (instance == null)
        {
            instance = new TrafficPropertiesManager();
        }
        return instance;
    }

    public static TrafficProperty getProperty(FlowDirection direction, Importance importance, Link link, Protocol protocol)
    {
        return getInstance()._getProperty(direction, importance, link, protocol);
    }

    static TrafficProperty[][][][] getProperties()
    {
        return getInstance()._getProperties();
    }

    private void initializeProperties()
    {
        for (int d = 0; d < FlowDirection.values().length; ++d)
        {
            for (int i = 0; i < Importance.values().length; ++i)
            {
                for (int l = 0; l < Link.values().length; ++l)
                {
                    for (int p = 0; p < Protocol.values().length; ++p)
                    {
                        FlowDirection direction = FlowDirection.values()[d];
                        Importance importance = Importance.values()[i];
                        Link link = Link.values()[l];
                        Protocol protocol = Protocol.values()[p];
                        properties[d][i][l][p] = new TrafficProperty(direction, importance, link, protocol);
                    }
                }
            }
        }
    }

    private TrafficProperty _getProperty(FlowDirection direction, Importance importance, Link link, Protocol protocol)
    {
        return properties[direction.ordinal()][importance.ordinal()][link.ordinal()][protocol.ordinal()];
    }

    TrafficProperty[][][][] _getProperties()
    {
        return properties;
    }
}
