package de.rwth.comsys.cloudanalyzer.gui.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.utils.ColorTemplate;
import de.rwth.comsys.cloudanalyzer.R;

import java.util.ArrayList;

public class StatsChart extends com.github.mikephil.charting.charts.LineChart
{
    public StatsChart(Context context)
    {

        super(context);
        setSettings();
    }

    public StatsChart(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setSettings();
    }

    public StatsChart(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        setSettings();
    }

    public void setSettings()
    {
        setDescription();

        this.setNoDataText(getResources().getString(R.string.loading));
        this.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);

        this.setBackgroundColor(ColorTemplate.rgb("#FFFFFF"));

        // enable touch gestures
        this.setTouchEnabled(true);
        this.setClickable(false);

        // enable scaling and dragging
        this.setDragEnabled(true);
        this.setScaleEnabled(true);
        this.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        this.setPinchZoom(true);

        // set an alternative background color
        this.setBackgroundColor(Color.WHITE);

        // get the legend (only possible after setting data)
        Legend l = this.getLegend();
        l.setEnabled(true);
        l.setWordWrapEnabled(true);
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);
        l.setTextSize(12f);

        XAxis xl = this.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(true);
        xl.setLabelCount(5, true);
        xl.setAvoidFirstLastClipping(false);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setTextSize(12f);

        YAxis leftAxis = this.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0);
        leftAxis.setTextSize(12f);
        YAxis rightAxis = this.getAxisRight();
        rightAxis.setEnabled(false);

    }

    public void setData(LineData data, ArrayList<String> xVals)
    {
        setData(data);
        XAxis xAxis = getXAxis();
        xAxis.setValueFormatter(new AxisValueFormatter(xVals));
    }

    public void setDescription()
    {
        Description des = new Description();
        des.setText("");
        this.setDescription(des);
    }

    public void setLabelCout(int count)
    {
        XAxis xl = this.getXAxis();
        xl.setLabelCount(count, true);
    }


}
