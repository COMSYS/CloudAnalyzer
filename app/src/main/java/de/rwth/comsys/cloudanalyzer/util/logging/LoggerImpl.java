package de.rwth.comsys.cloudanalyzer.util.logging;

import android.util.Log;

public class LoggerImpl extends Logger
{
    public LoggerImpl(String prefix)
    {
        super(prefix);
    }

    public void e(String msg)
    {
        Log.e(prefix, msg);
    }

    @Override
    public void w(String msg)
    {
        Log.w(prefix, msg);
    }

    @Override
    public void i(String msg)
    {
        Log.i(prefix, msg);
    }

    @Override
    public void d(String msg)
    {
        Log.d(prefix, msg);
    }

    @Override
    public void v(String msg)
    {
        Log.v(prefix, msg);
    }
}
