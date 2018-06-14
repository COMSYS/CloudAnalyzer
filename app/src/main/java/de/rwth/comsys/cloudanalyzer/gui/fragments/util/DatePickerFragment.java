package de.rwth.comsys.cloudanalyzer.gui.fragments.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Toast;
import de.rwth.comsys.cloudanalyzer.R;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
{
    // listener for callback function
    private OnDateSetListenerCustom listener;

    private long start = 0L;
    private long end = 0L;

    public void setCallBack(OnDateSetListenerCustom listener)
    {
        this.listener = listener;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View dialogView = View.inflate(getActivity(), R.layout.date_picker_dialog, null);

        final CalendarView calPickerStart = dialogView.findViewById(R.id.cal_picker_start);
        final CalendarView calPickerEnd = dialogView.findViewById(R.id.cal_picker_end);

        // define first and last dates shown in the calendar view

        calPickerStart.setMinDate(getArguments().getLong("minDate", System.currentTimeMillis()));
        calPickerStart.setMaxDate(System.currentTimeMillis());
        calPickerStart.setDate(Math.max(getArguments().getLong("startDate"), getArguments().getLong("minDate", System.currentTimeMillis())));
        calPickerStart.setFirstDayOfWeek(1);

        calPickerEnd.setMinDate(getArguments().getLong("minDate", System.currentTimeMillis()));
        calPickerEnd.setMaxDate(System.currentTimeMillis());
        calPickerEnd.setDate(Math.max(getArguments().getLong("endDate"), getArguments().getLong("minDate", System.currentTimeMillis())));
        calPickerEnd.setFirstDayOfWeek(1);

        start = calPickerStart.getDate();
        end = calPickerEnd.getDate();

        calPickerStart.setOnDateChangeListener(new CalendarView.OnDateChangeListener()
        {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth)
            {
                final Calendar cal2 = Calendar.getInstance();
                cal2.setTimeInMillis(0);
                cal2.set(Calendar.YEAR, year);
                cal2.set(Calendar.MONTH, month);
                cal2.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                cal2.set(Calendar.HOUR_OF_DAY, 0);
                cal2.set(Calendar.MINUTE, 0);
                cal2.set(Calendar.SECOND, 0);
                cal2.set(Calendar.MILLISECOND, 0);
                cal2.add(Calendar.MILLISECOND, 0);
                start = cal2.getTimeInMillis();
            }
        });

        calPickerEnd.setOnDateChangeListener(new CalendarView.OnDateChangeListener()
        {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth)
            {
                final Calendar cal3 = Calendar.getInstance();
                cal3.setTimeInMillis(0);
                cal3.set(Calendar.YEAR, year);
                cal3.set(Calendar.MONTH, month);
                cal3.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                cal3.set(Calendar.HOUR_OF_DAY, 0);
                cal3.set(Calendar.MINUTE, 0);
                cal3.set(Calendar.SECOND, 0);
                cal3.set(Calendar.MILLISECOND, 0);
                cal3.add(Calendar.MILLISECOND, 0);
                end = cal3.getTimeInMillis();
            }
        });

        builder.setView(dialogView).setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                DatePickerFragment.this.getDialog().cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (start <= end)
                {
                    //if(startCal.get(Calendar.DATE) <= endCal.get(Calendar.DATE)) {
                    // callback function returning the chosen dates
                    listener.onDateSet(start, end);
                    dialog.dismiss();
                }
                else
                {
                    Toast.makeText(getActivity().getApplicationContext(), "Chosen end date is before start date", Toast.LENGTH_LONG).show();
                }
            }
        });

        return dialog;

    }

    @Override
    public void onResume()
    {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

}