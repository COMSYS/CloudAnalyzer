package de.rwth.comsys.cloudanalyzer.network.trafficProperty;

public class TrafficProperty
{
    private final FlowDirection direction;
    private final Importance importance;
    private final Link link;
    private final Protocol protocol;

    // use TrafficPropertyManager to create objects
    TrafficProperty(FlowDirection direction, Importance importance, Link link, Protocol protocol)
    {
        this.direction = direction;
        this.importance = importance;
        this.link = link;
        this.protocol = protocol;
    }

    public FlowDirection getDirection()
    {
        return direction;
    }

    public Importance getImportance()
    {
        return importance;
    }

    public Link getLink()
    {
        return link;
    }

    public Protocol getProtocol()
    {
        return protocol;
    }
}
