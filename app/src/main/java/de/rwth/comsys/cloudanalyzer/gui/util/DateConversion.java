package de.rwth.comsys.cloudanalyzer.gui.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateConversion
{
    private static DateFormat mDate = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
    private static DateFormat mMonth = new SimpleDateFormat("MM/yy", Locale.getDefault());

    private static Calendar getCalendarObject(int dayOfYear, int year)
    {
        Calendar calendar = Calendar.getInstance();
        if (dayOfYear < 0)
        {
            // data is flattened
            calendar.set(Calendar.MONTH, (-dayOfYear) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        else
        {
            calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        }
        calendar.set(Calendar.YEAR, year);

        return calendar;
    }

    private static Date getTime(int dayOfYear, int year)
    {
        return getCalendarObject(dayOfYear, year).getTime();
    }

    public static String getDateString(int dayOfYear, int year)
    {
        if (dayOfYear < 0)
            return mMonth.format(getTime(dayOfYear, year));
        else
            return mDate.format(getTime(dayOfYear, year));
    }
}
