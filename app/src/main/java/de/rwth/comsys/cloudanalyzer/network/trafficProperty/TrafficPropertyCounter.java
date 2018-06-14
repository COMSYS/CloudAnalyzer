package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public class TrafficPropertyCounter
{
    private long[][][][] packetCounters;
    private long[][][][] byteCounters;

    public TrafficPropertyCounter()
    {
        packetCounters = new long[FlowDirection.values().length][Importance.values().length][Link.values().length][Protocol.values().length];
        byteCounters = new long[FlowDirection.values().length][Importance.values().length][Link.values().length][Protocol.values().length];
    }

    private void modifyCounter(long[][][][] counters, TrafficProperty property, long diff)
    {
        int[] indices = new int[TrafficPropertiesManager.PROPERTY_COUNT];
        indices[0] = property.getDirection().ordinal();
        indices[1] = property.getImportance().ordinal();
        indices[2] = property.getLink().ordinal();
        indices[3] = property.getProtocol().ordinal();

        counters[indices[0]][indices[1]][indices[2]][indices[3]] += diff;

        // aggregation
        int propCount = 0;
        int[] propIndices = new int[indices.length];
        for (int i = 0; i < indices.length; ++i)
        {
            if (indices[i] != 0) // 0 -> AGGREGATED
            {
                propIndices[propCount] = i;
                propCount++;
            }
        }
        if (propCount > 0)
        {
            for (int subset = 1; subset <= Math.pow(2, propCount) - 1; ++subset)
            {
                int[] indicesAggregated = new int[TrafficPropertiesManager.PROPERTY_COUNT];
                System.arraycopy(indices, 0, indicesAggregated, 0, indices.length);
                for (int i = 0; i < propCount; ++i)
                {
                    if (((subset >>> i) & 1) == 1)
                    {
                        indicesAggregated[propIndices[i]] = 0;
                    }
                }
                counters[indicesAggregated[0]][indicesAggregated[1]][indicesAggregated[2]][indicesAggregated[3]] += diff;
            }
        }
    }

    public void modifyPacketCounter(TrafficProperty property, long diff)
    {
        modifyCounter(packetCounters, property, diff);
    }

    public long getPacketCounterValue(TrafficProperty property)
    {
        return getPacketCounterValue(property.getDirection(), property.getImportance(), property.getLink(), property.getProtocol());
    }

    public long getPacketCounterValue(FlowDirection direction, Importance importance, Link link, Protocol protocol)
    {
        return packetCounters[direction.ordinal()][importance.ordinal()][link.ordinal()][protocol.ordinal()];
    }

    public void setPacketCounter(TrafficProperty property, long value)
    {
        setPacketCounter(property.getDirection(), property.getImportance(), property.getLink(), property.getProtocol(), value);
    }

    public void setPacketCounter(FlowDirection direction, Importance importance, Link link, Protocol protocol, long value)
    {
        packetCounters[direction.ordinal()][importance.ordinal()][link.ordinal()][protocol.ordinal()] = value;
    }

    public void modifyByteCounter(TrafficProperty property, long diff)
    {
        modifyCounter(byteCounters, property, diff);
    }

    public long getByteCounterValue(TrafficProperty property)
    {
        return getByteCounterValue(property.getDirection(), property.getImportance(), property.getLink(), property.getProtocol());
    }

    public long getByteCounterValue(FlowDirection direction, Importance importance, Link link, Protocol protocol)
    {
        return byteCounters[direction.ordinal()][importance.ordinal()][link.ordinal()][protocol.ordinal()];
    }

    public void setByteCounter(TrafficProperty property, long value)
    {
        setByteCounter(property.getDirection(), property.getImportance(), property.getLink(), property.getProtocol(), value);
    }

    public void setByteCounter(FlowDirection direction, Importance importance, Link link, Protocol protocol, long value)
    {
        byteCounters[direction.ordinal()][importance.ordinal()][link.ordinal()][protocol.ordinal()] = value;
    }

    public void resetCounters()
    {
        for (int d = 0; d < FlowDirection.values().length; ++d)
        {
            for (int i = 0; i < Importance.values().length; ++i)
            {
                for (int l = 0; l < Link.values().length; ++l)
                {
                    for (int p = 0; p < Protocol.values().length; ++p)
                    {
                        packetCounters[d][i][l][p] = 0;
                        byteCounters[d][i][l][p] = 0;
                    }
                }
            }
        }
    }

    public void enumerate(TrafficPropertyCounterHandler handler)
    {
        for (int d = 0; d < FlowDirection.values().length; ++d)
        {
            for (int i = 0; i < Importance.values().length; ++i)
            {
                for (int l = 0; l < Link.values().length; ++l)
                {
                    for (int p = 0; p < Protocol.values().length; ++p)
                    {
                        TrafficProperty prop = TrafficPropertiesManager.getProperties()[d][i][l][p];
                        handler.handleCounter(prop, getPacketCounterValue(prop), getByteCounterValue(prop));
                    }
                }
            }
        }
    }

    public interface TrafficPropertyCounterHandler
    {
        void handleCounter(TrafficProperty property, long packets, long bytes);
    }
}
