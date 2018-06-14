package de.rwth.comsys.cloudanalyzer.gui.fragments.util;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import de.rwth.comsys.cloudanalyzer.R;

import java.util.Calendar;

public class MonthYearPickerDialog extends DialogFragment
{
    //private static final int MAX_YEAR = 2099;
    private static final int MIN_YEAR = 2015;
    private DatePickerDialog.OnDateSetListener listener;

    public void setCallBack(DatePickerDialog.OnDateSetListener listener)
    {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Calendar cal = Calendar.getInstance();

        View dialog = View.inflate(getActivity(), R.layout.month_year_picker_dialog, null);
        final NumberPicker monthPicker = dialog.findViewById(R.id.picker_month);
        final NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(cal.get(Calendar.MONTH) + 1);

        int year = cal.get(Calendar.YEAR);
        yearPicker.setMinValue(MIN_YEAR);
        yearPicker.setMaxValue(year + 1);
        yearPicker.setValue(year);

        builder.setView(dialog)
                // Add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        listener.onDateSet(null, yearPicker.getValue(), monthPicker.getValue(), 0);
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                MonthYearPickerDialog.this.getDialog().cancel();
            }
        });
        return builder.create();
    }


}
