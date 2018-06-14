package de.rwth.comsys.cloudanalyzer.gui.fragments.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.SplashActivity;

import java.util.Calendar;

public class TermsOfUseDialog extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.tos);
        builder.setMessage(R.string.tos_content);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                Boolean accepted = sharedPrefs.getBoolean("pref_tos_accepted", false);
                if (!accepted)
                {
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putBoolean("pref_tos_accepted", true);

                    // this is not the place to do so, but what the hell
                    editor.putString("pref_tweaks_threads", String.valueOf(CaptureConstants.UPDATE_THREADS));
                    Calendar c = Calendar.getInstance();
                    editor.putInt("pref_last_upload", c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR) - 1);
                    editor.putInt("pref_last_compUpload", c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR) - 1);

                    editor.apply();
                }
                SplashActivity.setToSAccepted(true);
            }
        });
        builder.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                getActivity().finish();
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
