package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Locale;

import static android.graphics.Color.rgb;

public class PieChart extends com.github.mikephil.charting.charts.PieChart
{
    private static ArrayList<Integer> colors;
    private Locale locale;

    public PieChart(Context context)
    {
        super(context);

        //set Style
        this.setHoleRadius(40f);
        this.setTransparentCircleRadius(100);
        this.getLegend().setWordWrapEnabled(true);
        this.getLegend().setTextSize(12f);

        Description descr = new Description();
        descr.setText("");
        this.setDescription(descr);

        this.setDrawCenterText(true);
        this.setEntryLabelColor(Color.BLACK);
        this.setDrawEntryLabels(false);

        this.setDrawHoleEnabled(true);

        this.setRotationAngle(0);

        // enable rotation of the chart by touch
        this.setRotationEnabled(false);

        // display percentage values
        this.setUsePercentValues(true);
    }

    public PieChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        //set Style
        this.setHoleRadius(40f);
        this.setTransparentCircleRadius(100);

        this.getLegend().setWordWrapEnabled(true);
        this.getLegend().setTextSize(12f);

        Description descr = new Description();
        descr.setText("");
        this.setDescription(descr);

        this.setDrawCenterText(true);
        this.setEntryLabelColor(Color.BLACK);
        this.setDrawEntryLabels(false);

        this.setDrawHoleEnabled(true);

        this.setRotationAngle(0);

        // enable rotation of the chart by touch
        this.setRotationEnabled(false);

        // display percentage values
        this.setUsePercentValues(true);
    }

    public PieChart(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        //set Style
        this.setHoleRadius(40f);
        this.setTransparentCircleRadius(100);
        this.getLegend().setWordWrapEnabled(true);
        this.getLegend().setTextSize(12f);

        Description descr = new Description();
        descr.setText("");
        this.setDescription(descr);

        this.setDrawCenterText(true);
        this.setDrawEntryLabels(false);
        this.setEntryLabelColor(Color.BLACK);

        this.setDrawHoleEnabled(true);

        this.setRotationAngle(0);

        // enable rotation of the chart by touch
        this.setRotationEnabled(false);

        // display percentage values
        this.setUsePercentValues(true);
    }

    public static ArrayList<Integer> getColors()
    {
        int[] customColors = {rgb(211, 211, 211)};
        ArrayList<Integer> colors = new ArrayList<>();

        for (int c : customColors)
        {
            colors.add(c);
        }
        colors.add(ColorTemplate.getHoloBlue());

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);


        PieChart.colors = colors;

        return colors;
    }

    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    @Override
    public void setData(PieData data)
    {
        IPieDataSet iDataSet = data.getDataSet();
        PieDataSet dataSet = (PieDataSet) iDataSet;
        dataSet.setColors(getColors());

        dataSet.setValueFormatter(new SizeValueFormatter(locale));
        dataSet.setValueTextSize(12f);

        super.setData(data);
    }

}
