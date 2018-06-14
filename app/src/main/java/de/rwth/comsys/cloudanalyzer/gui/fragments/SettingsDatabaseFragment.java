package de.rwth.comsys.cloudanalyzer.gui.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.AppListPickerAnalysisActivity;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.DatabaseSizeDialog;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.MonthYearPickerDialog;
import de.rwth.comsys.cloudanalyzer.gui.util.AsyncTaskProgressDialogWithLock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class SettingsDatabaseFragment extends PreferenceFragment
{
    DatePickerDialog.OnDateSetListener onDate = new DatePickerDialog.OnDateSetListener()
    {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
        {
            Log.i("DateDialog", "Year: " + year + " Month: " + monthOfYear);
            new FlattenDatabaseTask(getActivity()).execute(year, monthOfYear);
        }
    };
    private String enableDebugging;


    public SettingsDatabaseFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        enableDebugging = MainHandler.getProperties().getProperty("ca.enableDebugging");

        addPreferencesFromResource(R.xml.preferences_database_export);
        Preference dbAppPref = findPreference("pref_db_appremoval");
        dbAppPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                if (CaptureCentral.getInstance().isActive())
                {
                    Toast.makeText(getActivity(), R.string.active, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (CaptureCentral.getInstance().isLocked())
                {
                    Toast.makeText(getActivity(), R.string.locked, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent appIntent;
                appIntent = new Intent(getActivity(), AppListPickerAnalysisActivity.class);
                startActivityForResult(appIntent, 3);
                return true;
            }
        });
        Preference dbSizePref = findPreference("pref_db_size");
        dbSizePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                DialogFragment dialog = new DatabaseSizeDialog();
                dialog.show(getActivity().getFragmentManager(), "DatabaseSizeFragmentTag");
                return true;
            }
        });
        Preference dbBackupPref = findPreference("pref_db_backup");
        dbBackupPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                // check if permissions are granted
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    // Request Write Permissions
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }

                new ExportDatabaseTask(getActivity()).execute();
                return true;
            }
        });

        if (enableDebugging.equalsIgnoreCase("true"))
        {
            addPreferencesFromResource(R.xml.preferences_database_debugging);

            Preference debugPathPref = findPreference("pref_db_flatten");
            debugPathPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    if (!sharedPrefs.getBoolean("pref_tweak_eval_mode", !CaptureConstants.EVAL_MODE))
                    {
                        Toast.makeText(getActivity(), R.string.no_support_eval_mode, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    MonthYearPickerDialog dateDialogFragment = new MonthYearPickerDialog();
                    dateDialogFragment.setCallBack(onDate);
                    dateDialogFragment.show(getFragmentManager(), "MonthPicker");
                    return true;
                }
            });
            Preference dbPersonalPref = findPreference("pref_db_personal");
            dbPersonalPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    new RemovePersonalDataTask(getActivity()).execute();
                    return true;
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        super.onActivityResult(requestCode, resultCode, data);

        if (null != data)
        {
            if (requestCode == 3)
            {
                Boolean process = data.getBooleanExtra("process", true);
                String appName = data.getStringExtra("name");
                if (process)
                    new RemoveAppFromDatabaseTask(getActivity()).execute(appName);
                else
                    Toast.makeText(getActivity(), appName + getString(R.string.app_filtered), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class RemoveAppFromDatabaseTask extends AsyncTaskProgressDialogWithLock<String, String, String>
    {

        RemoveAppFromDatabaseTask(Activity activity)
        {
            super(activity);
        }

        @Override
        protected String doInBackground(String... strings)
        {
            if (super.noaction)
                return null;
            int app = MainHandler.getAppId(strings[0]);
            if (app == -1)
                return "not.stored";
            MainHandler.removeAppFromAggregationStorage(app);
            return strings[0];
        }

        @Override
        protected void onPostExecute(String param)
        {
            if (param != null)
            {
                if (param.equals("not.stored"))
                {
                    Toast.makeText(super.getActivity(), R.string.app_not_stored, Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(super.getActivity(), getString(R.string.app_removed) + param + ".", Toast.LENGTH_LONG).show();
                }
            }
            super.onPostExecute(param);
        }
    }

    private class FlattenDatabaseTask extends AsyncTaskProgressDialogWithLock<Integer, Integer, Boolean>
    {

        FlattenDatabaseTask(Activity activity)
        {
            super(activity);
        }

        @Override
        protected Boolean doInBackground(Integer... ints)
        {
            if (super.noaction)
                return false;
            int year = ints[0];
            int monthOfYear = ints[1];
            MainHandler.flattenAggregationStorageUntil(year, monthOfYear + 1);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool)
        {
            if (bool)
            {
                Toast.makeText(super.getActivity(), R.string.flatten_success, Toast.LENGTH_LONG).show();
            }
            super.onPostExecute(bool);
        }
    }

    private class ExportDatabaseTask extends AsyncTaskProgressDialogWithLock<Void, Void, Boolean>
    {
        ExportDatabaseTask(Activity activity)
        {
            super(activity);
        }

        @Override
        protected Boolean doInBackground(Void... voids)
        {
            if (super.noaction)
                return false;

            MainHandler.deletePersonalDataFromDb();
            MainHandler.shutdown();
            MainHandler.deleteSingleton();

            File from = new File(super.getActivity().getApplicationInfo().dataDir + "/databases", "caDb");
            File to = new File(CaptureConstants.getDatabasePath());
            if (from.exists() && !from.isDirectory())
            {
                try
                {
                    FileChannel src = new FileInputStream(from).getChannel();
                    FileChannel dst = new FileOutputStream(to).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
                catch (IOException e)
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool)
        {
            if (bool)
            {
                Toast.makeText(super.getActivity(), R.string.export_success, Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(super.getActivity(), R.string.export_failed, Toast.LENGTH_SHORT).show();
            }
            MainHandler.init(super.getActivity());
            super.onPostExecute(bool);
        }
    }

    private class RemovePersonalDataTask extends AsyncTaskProgressDialogWithLock<Void, Void, Boolean>
    {
        RemovePersonalDataTask(Activity activity)
        {
            super(activity);
        }

        @Override
        protected Boolean doInBackground(Void... voids)
        {
            if (super.noaction)
                return false;

            MainHandler.deletePersonalDataFromDb();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool)
        {
            if (bool)
            {
                Toast.makeText(getActivity(), R.string.personal_information, Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(bool);
        }
    }
}
