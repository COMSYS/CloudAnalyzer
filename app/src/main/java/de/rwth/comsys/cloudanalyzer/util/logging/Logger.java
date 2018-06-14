package de.rwth.comsys.cloudanalyzer.util.logging;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Logger
{
    private static ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private static Class<?> concreteLogger;

    static
    {
        concreteLogger = LoggerImpl.class;
    }

    protected String prefix;

    protected Logger(String prefix)
    {
        this.prefix = prefix;
    }

    public static Logger getLogger(String prefix)
    {
        Logger l = loggers.get(prefix);
        if (l == null)
        {
            try
            {
                Constructor<?> c = concreteLogger.getConstructor(String.class);
                l = (Logger) c.newInstance(prefix);
            }
            catch (ReflectiveOperationException e)
            {
                e.printStackTrace();
            }
            loggers.put(prefix, l);
        }
        return l;
    }

    public abstract void e(String msg);

    public abstract void w(String msg);

    public abstract void i(String msg);

    public abstract void d(String msg);

    public abstract void v(String msg);
}
