package de.rwth.comsys.cloudanalyzer.gui.util;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class SizeValueFormatter implements IValueFormatter
{
    private DecimalFormat mFormat;

    public SizeValueFormatter(Locale locale)
    {
        NumberFormat nf = NumberFormat.getInstance(locale);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#.##");
        mFormat = df;
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler)
    {
        if (entry.getY() > 1024)
            return mFormat.format(entry.getY() / 1024) + " GB";
        return mFormat.format(entry.getY()) + " MB";
    }
}
