package de.rwth.comsys.cloudanalyzer.services;

import de.rwth.comsys.cloudanalyzer.util.CloudLayer;

import java.io.Serializable;
import java.util.Set;

public interface Service extends Serializable, Comparable<Service>
{

    String getName();

    int getId();

    boolean setupService();

    boolean equals(Object other);

    int hashCode();

    Set<CloudLayer> getLayers();

    Set<String> getGroups();

    int compareTo(Service other);
}
