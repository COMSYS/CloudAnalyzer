package de.rwth.comsys.cloudanalyzer.gui.util;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;

public class AxisValueFormatter implements IAxisValueFormatter
{
    private ArrayList<String> mValues;

    public AxisValueFormatter(ArrayList<String> values)
    {
        this.mValues = values;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis)
    {
        // "value" represents the position of the label on the axis (x or y)
        if (mValues == null)
            return "-";
        int round = Math.round(value);
        if (round < 0 || round >= mValues.size())
            return "";
        return mValues.get(round);
    }
}
