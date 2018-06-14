package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.util.AttributeSet;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

public class StatsBarChart extends com.github.mikephil.charting.charts.HorizontalBarChart
{
    public StatsBarChart(Context context)
    {

        super(context);
        setSettings();
    }

    public StatsBarChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setSettings();
    }

    public StatsBarChart(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        setSettings();
    }

    public void setSettings()
    {
        getDescription().setEnabled(false);
        setDrawGridBackground(false);
        setClickable(false);
        setHighlightPerTapEnabled(false);
        setTouchEnabled(false);
        setDragEnabled(false);

        setPinchZoom(false);

        setDrawBarShadow(false);
        setDrawValueAboveBar(true);
        setHighlightFullBarEnabled(false);

        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(10f);
        xAxis.setDrawLabels(false);
        xAxis.setTextSize(0);

        YAxis yAxis1 = getAxisLeft();
        yAxis1.setDrawGridLines(false);
        yAxis1.setDrawAxisLine(false);
        yAxis1.setAxisMinimum(0f);
        yAxis1.setDrawLabels(false);
        yAxis1.setTextSize(0);
        yAxis1.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        YAxis yAxis2 = getAxisRight();
        yAxis2.setDrawAxisLine(false);
        yAxis2.setDrawGridLines(false);
        yAxis2.setAxisMinimum(0f);
        yAxis2.setDrawLabels(false);
        yAxis2.setEnabled(false);
        yAxis2.setTextSize(0);
        yAxis2.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        setFitBars(true);

        Legend l = getLegend();
        l.setEnabled(false);

    }

    public void setMax(float max)
    {
        getAxisLeft().setAxisMaximum(max);
        getAxisRight().setAxisMaximum(max);
    }

}
