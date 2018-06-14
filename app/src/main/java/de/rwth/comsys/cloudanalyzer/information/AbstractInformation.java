package de.rwth.comsys.cloudanalyzer.information;

public abstract class AbstractInformation implements Information
{

    private static final long serialVersionUID = -4990271953054434295L;
    // Note: this class has a natural ordering that is inconsistent with equals.

    private String type;
    private int priority;

    public AbstractInformation(String type, int priority)
    {
        this.priority = priority;
        this.type = type;
    }

    public int getPriority()
    {
        return priority;
    }

    public String getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return "Id: " + getIdentifier() + ", Type: " + getType();
    }
}
