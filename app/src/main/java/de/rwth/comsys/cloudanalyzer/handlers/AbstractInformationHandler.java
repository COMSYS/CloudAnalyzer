package de.rwth.comsys.cloudanalyzer.handlers;

public abstract class AbstractInformationHandler implements InformationHandler
{

    protected int id;

    @Override
    public boolean setupHandler()
    {
        return true;
    }

    @Override
    public void shutdownHandler()
    {

    }

    @Override
    public void resetHandler()
    {

    }

    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public void setId(int id)
    {
        this.id = id;
    }

    @Override
    public void onStart(boolean live)
    {

    }

    @Override
    public void onStop()
    {

    }
}
