package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;

public class SettingsMiscFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private String enableDebugging;

    public SettingsMiscFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        enableDebugging = MainHandler.getProperties().getProperty("ca.enableDebugging");

        if (enableDebugging.equalsIgnoreCase("true"))
        {
            addPreferencesFromResource(R.xml.preferences_main_debugging);

            Preference debugPathPref = findPreference("pref_debugging_path");
            debugPathPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    Toast.makeText(getActivity(), R.string.not_implemented, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        addPreferencesFromResource(R.xml.preferences_main_tweaks);

        // disable eval mode overwrite, just in case ;-)
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putBoolean("pref_tweak_eval_mode", !CaptureConstants.EVAL_MODE);
        editor.apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals("pref_debugging_output") || key.equals("pref_debugging_pcap"))
        {
            if (sharedPreferences.getBoolean(key, false))
            {
                // check if permissions are granted
                if ((ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                {
                    // Request Write Permissions for logging / PCAP output
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }
            }
            CaptureCentral.getInstance().initializePcapWriter();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause()
    {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
