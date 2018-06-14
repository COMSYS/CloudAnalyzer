package de.rwth.comsys.cloudanalyzer.gui;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.gui.util.CloudUsageAccessor;

import java.util.*;

public class AppListPickerAnalysisActivity extends ListActivity
{
    AppListAdapter adapter = null;
    Set<String> checkedApps = null;
    String checkedAppsString = "pref_db_filter_apps";
    int positive;
    int negative;

    public void setCheckedApps(Context context, String preferenceName)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.checkedApps = sharedPrefs.getStringSet(preferenceName, new HashSet<String>());
    }

    public void setIcons(int positive, int negative)
    {
        this.positive = positive;
        this.negative = negative;
    }

    public void processApps(Set<String> checkedApps)
    {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);

        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables = pm.queryIntentActivities(main, 0);
        Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(pm));

        List<ResolveInfo> additionalLaunchables = new ArrayList<>();
        DatabaseResultSet resultSet = CloudUsageAccessor.getAppList();

        List<String> applist = new ArrayList<>(checkedApps);
        while (resultSet != null && resultSet.next())
        {
            applist.add(resultSet.getString(1));
        }
        if (resultSet != null)
            resultSet.close();

        ActivityInfo activity;
        ComponentName name;
        String appName;
        Boolean found;
        for (String additionalApp : applist)
        {
            found = false;
            for (ResolveInfo launchable : launchables)
            {
                activity = launchable.activityInfo;
                name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                appName = name.getPackageName();
                if (additionalApp.equalsIgnoreCase(appName))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                ResolveInfo info = new ResolveInfo();

                info.activityInfo = new ActivityInfo();
                info.activityInfo.applicationInfo = new ApplicationInfo();
                info.activityInfo.applicationInfo.packageName = additionalApp;
                info.activityInfo.name = additionalApp;
                info.activityInfo.packageName = additionalApp;

                additionalLaunchables.add(info);
            }
        }
        launchables.addAll(additionalLaunchables);

        String enableDebugging = MainHandler.getProperties().getProperty("ca.enableDebugging");
        if (!enableDebugging.equalsIgnoreCase("true"))
        {
            // clean debug apps
            List<String> debugApps = Arrays.asList(getResources().getStringArray(R.array.pref_db_debug_information_values));

            List<ResolveInfo> removableLaunchables = new ArrayList<>();
            for (ResolveInfo launchable : launchables)
            {
                activity = launchable.activityInfo;
                name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                appName = name.getPackageName();
                if (debugApps.contains(appName))
                {
                    removableLaunchables.add(launchable);
                }
            }
            launchables.removeAll(removableLaunchables);
        }

        adapter = new AppListAdapter(pm, launchables, checkedApps, positive, negative);
        setListAdapter(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applist_picker);

        if (checkedApps == null)
        {
            // super-class
            setCheckedApps(this, "pref_db_filter_apps");
            setIcons(R.drawable.ic_check, R.drawable.ic_clear);
        }
        processApps(checkedApps);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        ResolveInfo launchable = adapter.getItem(position);
        ActivityInfo activity = launchable.activityInfo;
        ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
        String appName = name.getPackageName();

        Boolean processed = checkedApps.contains(appName);
        if (processed)
            checkedApps.remove(appName);
        else
            checkedApps.add(appName);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putStringSet(checkedAppsString, checkedApps);
        editor.apply();

        Intent intent = new Intent();
        intent.putExtra("name", appName);
        intent.putExtra("process", !processed);
        setResult(1, intent);
        finish();
    }

    private class AppListAdapter extends ArrayAdapter<ResolveInfo>
    {
        private PackageManager pm;
        private Set<String> checkedApps;
        private int positive;
        private int negative;

        private AppListAdapter(PackageManager pm, List<ResolveInfo> apps, Set<String> checkedApps, int positive, int negative)
        {
            super(AppListPickerAnalysisActivity.this, R.layout.imagetextmarker_list_adapter_item, apps);

            this.pm = pm;
            this.checkedApps = checkedApps;
            this.positive = positive;
            this.negative = negative;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
                convertView = newView(parent);
            }

            bindView(position, convertView);

            return (convertView);
        }

        private View newView(ViewGroup parent)
        {
            return (getLayoutInflater().inflate(R.layout.imagetextmarker_list_adapter_item, parent, false));
        }

        private void bindView(int position, View row)
        {
            String appName = String.valueOf(getItem(position).loadLabel(pm));
            String packageName = getItem(position).activityInfo.packageName;

            ImageView icon = row.findViewById(R.id.ivLogo);
            icon.setImageDrawable(getItem(position).loadIcon(pm));

            TextView label = row.findViewById(R.id.tvLabel);
            if (appName.equals("null"))
                label.setText(packageName);
            else
                label.setText(appName);

            ImageView check = row.findViewById(R.id.ivCheck);
            if (checkedApps.contains(packageName))
                check.setImageResource(positive);
            else
                check.setImageResource(negative);
        }
    }


}