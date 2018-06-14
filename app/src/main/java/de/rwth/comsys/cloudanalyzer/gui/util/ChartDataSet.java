package de.rwth.comsys.cloudanalyzer.gui.util;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class ChartDataSet
{
    public static LineDataSet newStatLineDataSet(ArrayList<Entry> array, String name, String color)
    {
        LineDataSet datasetBorder = new LineDataSet(array, name);
        datasetBorder.setCircleColor(ColorTemplate.rgb(color));
        datasetBorder.setColor(ColorTemplate.rgb(color));
        datasetBorder.setCircleColorHole(ColorTemplate.rgb(color));
        datasetBorder.setLineWidth(1);
        datasetBorder.setDrawCircleHole(false);
        datasetBorder.setDrawCircles(true);
        datasetBorder.setDrawFilled(true);
        datasetBorder.setCircleRadius(1.0f);
        datasetBorder.setFillAlpha(255);
        datasetBorder.setFillColor(ColorTemplate.rgb(color));
        return datasetBorder;
    }

}
