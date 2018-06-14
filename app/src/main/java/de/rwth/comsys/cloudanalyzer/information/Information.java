package de.rwth.comsys.cloudanalyzer.information;

import java.io.Serializable;

public interface Information extends Serializable
{

    int getPriority();

    String getType();

    String getIdentifier();

}
