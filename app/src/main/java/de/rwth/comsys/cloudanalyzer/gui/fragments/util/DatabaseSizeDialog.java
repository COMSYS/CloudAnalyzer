package de.rwth.comsys.cloudanalyzer.gui.fragments.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import de.rwth.comsys.cloudanalyzer.R;

import java.io.File;

public class DatabaseSizeDialog extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.db_size);
        builder.setMessage(getString(R.string.db_size_content) + new File(getActivity().getApplicationInfo().dataDir + "/databases", "caDb").length() / 1048576 + " MB.");
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // just dismiss
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
