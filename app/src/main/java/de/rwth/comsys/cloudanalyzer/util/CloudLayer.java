package de.rwth.comsys.cloudanalyzer.util;

public enum CloudLayer
{

    IaaS("Infrastructure as a Service"), PaaS("Platform as a Service"), SaaS("Software as a Service");

    private String name;

    CloudLayer(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
