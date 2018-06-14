package de.rwth.comsys.cloudanalyzer.util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader
{
    public static String getProperty(String file, String key, Context context) throws IOException
    {
        return getProperties(file, context).getProperty(key);
    }

    public static Properties getProperties(String file, Context context) throws IOException
    {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(file);
        properties.load(inputStream);
        return properties;
    }
}
