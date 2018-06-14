package de.rwth.comsys.cloudanalyzer.gui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.widget.*;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.CaptureService;
import de.rwth.comsys.capture_vpn.util.MessageReceiver;
import de.rwth.comsys.capture_vpn.util.NotificationService;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.database.DatabaseResultSet;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.gui.fragments.StatsListFragment;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.DatePickerFragment;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.OnDateSetListenerCustom;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.SettingsViewPager;
import de.rwth.comsys.cloudanalyzer.gui.util.ChartDataSet;
import de.rwth.comsys.cloudanalyzer.gui.util.CloudUsageAccessor;
import de.rwth.comsys.cloudanalyzer.gui.util.DateConversion;
import de.rwth.comsys.cloudanalyzer.gui.util.StatsChart;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static de.rwth.comsys.cloudanalyzer.gui.util.TrafficPropertiesSQL.trafficPropertyILP;

public class ResultsActivity extends AppCompatActivity
{

    final private Calendar maxCal = Calendar.getInstance();
    final private Calendar minCal = Calendar.getInstance();
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    boolean data = false;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private String[] mPageTitles;
    private String[] entries;
    private String enableDebugging;
    private Menu menu;
    private boolean menuFlag = true;
    public OnDateSetListenerCustom listener = new OnDateSetListenerCustom()
    {
        @Override
        public void onDateSet(long minDate, long maxDate)
        {
            // set global calendars to new times
            minCal.setTimeInMillis(minDate);
            maxCal.setTimeInMillis(maxDate);

            StatsListFragment frag = (StatsListFragment) (mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
            frag.update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), false);
            if (menuFlag)
            {
                new UpdateDataTask().execute();
            }
        }
    };
    private boolean calRunning = false;
    private long startDate = System.currentTimeMillis();
    private ArrayList<String> xVals = null;
    private ArrayList<Entry> yVals_total = null, yVals_cloud = null;
    private AlertDialog exitAlert;

    public static void updateIcon(Menu menu, Boolean active)
    {
        if (menu == null)
        {
            return;
        }
        for (int i = 0; i < menu.size(); i++)
        {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.action_toggle_vpn)
            {
                if (active)
                {
                    item.setIcon(R.drawable.ic_action_stop);
                }
                else
                {
                    item.setIcon(R.drawable.ic_action_play_arrow);
                }
            }
        }
    }

    public static void toggleCapturing(Activity activity, Menu menu)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

        if (!CaptureCentral.getInstance().isActive())
        {

            if (sharedPrefs.getBoolean("pref_debugging_output", false))
            {
                // check if permissions are granted
                if ((ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                {
                    // Request Write Permissions for logging / PCAP output
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }
            }

            // Initialize VPN interface
            Intent intent = VpnService.prepare(activity);

            if (intent != null)
            {
                activity.startActivityForResult(intent, 0);
            }
            else
            {
                intent = new Intent(activity, MessageReceiver.class);
                intent.putExtra("action", "START");
                activity.sendBroadcast(intent);
            }
            updateIcon(menu, true);
        }
        else
        {
            Intent intent = new Intent(activity, MessageReceiver.class);
            intent.putExtra("action", "STOP");
            activity.sendBroadcast(intent);

            updateIcon(menu, false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getBoolean("exitAlertShowing"))
        {
            onBackPressed();
        }

        setContentView(R.layout.activity_results);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        getSupportActionBar().setTitle(mSectionsPagerAdapter.getPageTitle(0));

        mPageTitles = getResources().getStringArray(R.array.menu_array);

        // remove options from menu if disabled
        boolean changed = false;
        if (MainHandler.getProperties() != null)
        {
            entries = getResources().getStringArray(R.array.menu_array);
            enableDebugging = MainHandler.getProperties().getProperty("ca.enableDebugging");

            ArrayList<String> titleList = new ArrayList<>(Arrays.asList(mPageTitles));
            if (enableDebugging.equalsIgnoreCase("false"))
            {
                if (titleList.contains(entries[entries.length - 1]))
                    titleList.remove(entries[entries.length - 1]);
                changed = true;
            }

            if (changed)
                mPageTitles = titleList.toArray(new String[titleList.size()]);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, mPageTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close)
        {
            public void onDrawerClosed(View view)
            {
                getSupportActionBar().setTitle(mTitle);
                int position = mViewPager.getCurrentItem();
                if (position == 3 || position == 4)
                {
                    findViewById(R.id.tab_layout).setVisibility(View.GONE);
                }
                else
                {
                    findViewById(R.id.tab_layout).setVisibility(View.VISIBLE);
                }
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView)
            {
                getSupportActionBar().setTitle(getString(R.string.drawer));
                int position = mViewPager.getCurrentItem();
                if (position == 3 || position == 4)
                {
                    findViewById(R.id.tab_layout).setVisibility(View.GONE);
                }
                else
                {
                    findViewById(R.id.tab_layout).setVisibility(View.INVISIBLE);
                }
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);


        // Set up the ViewPager with the sections adapter.
        mViewPager = (SettingsViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);
        //Hide settings tab
        ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(3).setVisibility(View.GONE);
        ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(4).setVisibility(View.GONE);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                TabLayout tabLayout = findViewById(R.id.tab_layout);
                switch (position)
                {
                    case 2:
                        ((SettingsViewPager) mViewPager).setAllowedSwipeDirection(SettingsViewPager.Direction.left);
                        tabLayout.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                    case 4:
                        ((SettingsViewPager) mViewPager).setAllowedSwipeDirection(SettingsViewPager.Direction.none);
                        tabLayout.setVisibility(View.GONE);
                        break;
                    default:
                        ((SettingsViewPager) mViewPager).setAllowedSwipeDirection(SettingsViewPager.Direction.all);
                        tabLayout.setVisibility(View.VISIBLE);
                        break;
                }
                if (!CaptureCentral.isMainHandlerInitialized())
                {
                    ResultsActivity.this.startActivity(new Intent(ResultsActivity.this, SplashActivity.class));
                    ResultsActivity.this.finish();
                }

                getSupportActionBar().setTitle(mSectionsPagerAdapter.getPageTitle(position));
                updateMenu(position);
                mTitle = mSectionsPagerAdapter.getPageTitle(position);
                if (menuFlag)
                {
                    new UpdateDataTask().execute();
                }
                StatsListFragment frag = (StatsListFragment) (mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
                if (frag != null)
                    frag.update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), false);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });

        if (savedInstanceState == null)
        {
            mViewPager.setCurrentItem(0);
            mTitle = mSectionsPagerAdapter.getPageTitle(0);
        }

        CaptureService cs = CaptureService.getCaptureService();
        if (cs == null)
            // Android O StartForeground is not required
            startService(new Intent(this, NotificationService.class));

        if (savedInstanceState != null)
        {
            Long minCalVal = savedInstanceState.getLong("minCalVal");
            Long maxCalVal = savedInstanceState.getLong("maxCalVal");
            startDate = savedInstanceState.getLong("startDate");
            if (minCalVal != 0 && maxCalVal != 0)
            {
                // set global calendars to new times
                minCal.setTimeInMillis(minCalVal);
                maxCal.setTimeInMillis(maxCalVal);
                mSectionsPagerAdapter.setCurrentFragment(mViewPager.getCurrentItem(), getSupportFragmentManager().getFragment(savedInstanceState, Integer.toString(mViewPager.getCurrentItem())));
                StatsListFragment frag = (StatsListFragment) (mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
                if (frag != null)
                    frag.update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), false);
                if (menuFlag)
                {
                    new GetDataTask().execute();
                }
            }
        }
        else
        {
            new GetDateTask().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_stats_list, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_stats_list, menu);
        if (!menuFlag)
        {
            menu.removeItem(R.id.action_calendar_stats);
        }
        updateIcon(menu, CaptureCentral.getInstance().isActive());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed()
    {
        exitAlert = new AlertDialog.Builder(this).setTitle(R.string.exit).setMessage(getString(R.string.exit_content)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                finish();
            }
        }).setNegativeButton(android.R.string.no, null).create();
        exitAlert.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean("exitAlertShowing", exitAlert != null && exitAlert.isShowing());
        outState.putLong("minCalVal", minCal.getTimeInMillis());
        outState.putLong("maxCalVal", maxCal.getTimeInMillis());
        outState.putLong("startDate", startDate);
        getSupportFragmentManager().putFragment(outState, Integer.toString(mViewPager.getCurrentItem()), mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (exitAlert != null && exitAlert.isShowing())
        {
            exitAlert.cancel();
        }
    }

    private void setData()
    {
        try
        {
            Fragment fragment = mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem());
            if (fragment == null || fragment.getView() == null)
            {
                return;
            }
            ListView view = fragment.getView().findViewById(R.id.lVStatsList);
            if (view.getHeaderViewsCount() == 0)
            {
                LayoutInflater inflater = getLayoutInflater();
                ViewGroup header = (ViewGroup) inflater.inflate(R.layout.fragment_stats_list_header, view, false);
                header.setTag(this.getClass().getSimpleName() + "header");
                view.addHeaderView(header, null, false);
            }
            View header = view.findViewWithTag(this.getClass().getSimpleName() + "header");
            if (startDate == 0)
            {
                header.findViewById(R.id.stats_date).setVisibility(View.INVISIBLE);
                header.findViewById(R.id.legendYStats).setVisibility(View.INVISIBLE);
                header.findViewById(R.id.statsChart).setVisibility(View.INVISIBLE);
                header.findViewById(R.id.hintGraph1).setVisibility(View.VISIBLE);
                header.findViewById(R.id.hintText1).setVisibility(View.VISIBLE);
                header.findViewById(R.id.hintGraph2).setVisibility(View.VISIBLE);
                header.findViewById(R.id.hintText2).setVisibility(View.VISIBLE);
                header.getBackground().setAlpha(100);
            }
            else
            {
                header.findViewById(R.id.stats_date).setVisibility(View.VISIBLE);
                header.findViewById(R.id.legendYStats).setVisibility(View.VISIBLE);
                header.findViewById(R.id.statsChart).setVisibility(View.VISIBLE);
                header.findViewById(R.id.hintGraph1).setVisibility(View.INVISIBLE);
                header.findViewById(R.id.hintText1).setVisibility(View.INVISIBLE);
                header.findViewById(R.id.hintGraph2).setVisibility(View.INVISIBLE);
                header.findViewById(R.id.hintText2).setVisibility(View.INVISIBLE);

                TextView title = header.findViewById(R.id.stats_date);
                if (minCal.get(Calendar.DATE) == maxCal.get(Calendar.DATE))
                {
                    title.setText(String.format("%s:", DateFormat.getDateInstance().format(minCal.getTime())));
                }
                else
                {
                    title.setText(String.format("%s - %s:", DateFormat.getDateInstance().format(minCal.getTime()), DateFormat.getDateInstance().format(maxCal.getTime())));
                }


                LineData lineData = new LineData();
                ArrayList<String> xVals_line = new ArrayList<>();
                ArrayList<Entry> yVals_total_line = new ArrayList<>();
                ArrayList<Entry> yVals_cloud_line = new ArrayList<>();
                Calendar lastCal = Calendar.getInstance();
                Calendar curCal = Calendar.getInstance();
                curCal.setTimeInMillis(minCal.getTimeInMillis());
                lastCal.setTimeInMillis(maxCal.getTimeInMillis());

                int index = 0;
                while (!curCal.after(lastCal))
                {
                    xVals_line.add(DateConversion.getDateString(curCal.get(Calendar.DAY_OF_YEAR), curCal.get(Calendar.YEAR)));
                    int cur = -1;
                    if (xVals != null && xVals.size() > 0)
                        cur = xVals.indexOf(DateConversion.getDateString(curCal.get(Calendar.DAY_OF_YEAR), curCal.get(Calendar.YEAR)));
                    if (cur != -1)
                    {
                        if (yVals_total != null && yVals_total.get(cur) != null && yVals_cloud != null && yVals_cloud.get(cur) != null)
                        {
                            if (index > 0)
                            {
                                yVals_total_line.add(new Entry(index, yVals_total_line.get(index - 1).getY() + yVals_total.get(cur).getY()));
                                yVals_cloud_line.add(new Entry(index, yVals_cloud_line.get(index - 1).getY() + yVals_cloud.get(cur).getY()));
                            }
                            else
                            {
                                yVals_total_line.add(new Entry(index, yVals_total.get(cur).getY()));
                                yVals_cloud_line.add(new Entry(index, yVals_cloud.get(cur).getY()));
                            }
                        }
                    }
                    else if (yVals_total_line.size() > 0)
                    {
                        yVals_total_line.add(new Entry(index, yVals_total_line.get(yVals_total_line.size() - 1).getY()));
                        yVals_cloud_line.add(new Entry(index, yVals_cloud_line.get(yVals_cloud_line.size() - 1).getY()));
                    }
                    else
                    {
                        yVals_total_line.add(new Entry(index, 0));
                        yVals_cloud_line.add(new Entry(index, 0));
                    }
                    curCal.add(Calendar.DATE, 1);
                    index++;
                }
                xVals_line.add("");
                if (!yVals_cloud_line.isEmpty() && !yVals_total_line.isEmpty())
                {
                    LineDataSet lineDataSetUpDownload = ChartDataSet.newStatLineDataSet(yVals_cloud_line, getResources().getString(R.string.identified_traffic), "#318BC1");
                    LineDataSet lineDataSetUpDownloadBase = ChartDataSet.newStatLineDataSet(yVals_total_line, getResources().getString(R.string.overall_traffic), "#434348");
                    lineDataSetUpDownload.setDrawValues(false);
                    lineDataSetUpDownloadBase.setDrawValues(false);
                    if (yVals_cloud_line.size() == 1 && yVals_total_line.size() == 1)
                    {
                        lineDataSetUpDownload.setCircleRadius(2);
                        lineDataSetUpDownloadBase.setCircleRadius(2);
                    }
                    lineData.addDataSet(lineDataSetUpDownloadBase);
                    lineData.addDataSet(lineDataSetUpDownload);
                }

                StatsChart chart = header.findViewById(R.id.statsChart);
                chart.setData(lineData, xVals_line);
                if (yVals_cloud_line.size() == 1 && yVals_total_line.size() == 1)
                {
                    chart.setLabelCout(3);
                }
                else if (yVals_cloud_line.size() < 5 && yVals_total_line.size() < 5)
                {
                    chart.setLabelCout(yVals_total_line.size());
                }

                chart.getLegend().setEnabled(true);
                chart.postInvalidate();
            }
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
        updateIcon(menu, CaptureCentral.getInstance().isActive());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
        updateIcon(menu, CaptureCentral.getInstance().isActive());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        else if (item.getItemId() == R.id.action_calendar_stats && !calRunning)
        {
            calRunning = true;
            showDialogFragment(startDate != 0 ? startDate : System.currentTimeMillis());
            calRunning = false;
            return true;
        }
        else if (item.getItemId() == R.id.action_toggle_vpn)
        {
            toggleCapturing(this, menu);
        }

        // Handle action buttons
        return super.onOptionsItemSelected(item);
    }

    private void showDialogFragment(Long date)
    {
        Bundle calBundle = new Bundle();
        calBundle.putLong("minDate", date);
        calBundle.putLong("startDate", minCal.getTimeInMillis());
        calBundle.putLong("endDate", maxCal.getTimeInMillis());
        DatePickerFragment dateDialogFragment = new DatePickerFragment();
        dateDialogFragment.setArguments(calBundle);
        dateDialogFragment.setCallBack(listener);
        dateDialogFragment.show(getFragmentManager(), "DatePicker");
    }

    @Override
    public void onActivityResult(int request, int result, Intent data)
    {
        switch (result)
        {
            case RESULT_OK:
                Intent intent = new Intent(this, MessageReceiver.class);
                updateIcon(menu, true);
                intent.putExtra("action", "START");
                sendBroadcast(intent);
                break;
            case 2:
                long start = data.getLongExtra("start", 0);
                long end = data.getLongExtra("end", 0);
                if (start != 0 || end != 0)
                {
                    minCal.setTimeInMillis(start);
                    maxCal.setTimeInMillis(end);
                    StatsListFragment frag = (StatsListFragment) (mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
                    if (frag != null)
                        frag.update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), false);
                    if (menuFlag)
                    {
                        if (xVals != null)
                        {
                            new UpdateDataTask().execute();
                        }
                        else
                        {
                            new GetDataTask().execute();
                        }
                    }
                }
                break;
            default:
                Toast.makeText(ResultsActivity.this, R.string.error_service, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!CaptureCentral.isMainHandlerInitialized())
        {
            ResultsActivity.this.startActivity(new Intent(ResultsActivity.this, SplashActivity.class));
            ResultsActivity.this.finish();
        }

        updateIcon(menu, CaptureCentral.getInstance().isActive());
        if (data)
        {
            new UpdateDataTask().execute();
            StatsListFragment frag = (StatsListFragment) (mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
            frag.update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), false);
        }
        else
        {
            new GetDataTask().execute();
        }
    }

    private void logIssue()
    {
        // check if permissions are granted
        if ((ContextCompat.checkSelfPermission(ResultsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(ResultsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
        {
            // Request Write Permissions for logging / PCAP output
            ActivityCompat.requestPermissions(ResultsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        Intent intent = new Intent(this, MessageReceiver.class);
        intent.putExtra("action", "ISSUE");
        sendBroadcast(intent);
    }

    private void updateMenu(int position)
    {
        // position 3 and 4 due to respective entries in SectionsPagerAdapter
        if (position == 3 || position == 4)
        {
            menuFlag = false;
        }
        else
        {
            menuFlag = true;
        }
        invalidateOptionsMenu();
        updateIcon(menu, CaptureCentral.getInstance().isActive());
    }

    private class UpdateDataTask extends AsyncTask<Boolean, Boolean, Boolean>
    {
        @Override
        protected Boolean doInBackground(Boolean... params)
        {
            try
            {
                Calendar curCal = Calendar.getInstance();
                String sql_total = "SELECT day, year, SUM(value) FROM aggregation_statistics WHERE day = " + curCal.get(Calendar.DAY_OF_YEAR) + " AND year = " + curCal.get(Calendar.YEAR) + " AND app = -1 AND direction = -1 AND name = 'totalIpPacketLength' " + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
                String sql_cloud = "SELECT day, year, SUM(bytes) FROM aggregation_data WHERE day = " + curCal.get(Calendar.DAY_OF_YEAR) + " AND year = " + curCal.get(Calendar.YEAR) + " AND app = -1 AND direction = -1 AND handler = -1 AND service = -1" + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";

                DatabaseStatement query = MainHandler.getDbConn().createStatement(sql_cloud);
                DatabaseResultSet res_cloud = query.executeQuery();

                query = MainHandler.getDbConn().createStatement(sql_total);
                DatabaseResultSet res_total = query.executeQuery();

                boolean next_res_total = res_total.next();
                boolean next_res_cloud = res_cloud.next();
                if (next_res_total && res_total.getInt(1) != null)
                {
                    if (startDate == 0)
                    {
                        startDate = System.currentTimeMillis();
                    }

                    String date = DateConversion.getDateString(res_total.getInt(1), res_total.getInt(2));
                    int datePos = xVals.indexOf(date);
                    if (datePos == -1)
                    {
                        xVals.add(DateConversion.getDateString(res_total.getInt(1), res_total.getInt(2)));
                        yVals_total.add(new Entry(xVals.size() - 1, ((float) res_total.getLong(3)) / 1048576));
                        if (next_res_cloud && res_cloud.getInt(1) != null)
                        {
                            yVals_cloud.add(new Entry(xVals.size() - 1, ((float) res_cloud.getLong(3)) / 1048576));
                        }
                        else
                        {
                            yVals_cloud.add(new Entry(xVals.size() - 1, 0));
                        }
                    }
                    else
                    {
                        yVals_total.set(datePos, new Entry(datePos, ((float) res_total.getLong(3)) / 1048576));
                        if (next_res_cloud && res_cloud.getInt(1) != null)
                        {
                            yVals_cloud.set(datePos, new Entry(datePos, ((float) res_cloud.getLong(3)) / 1048576));
                        }
                        else
                        {
                            yVals_cloud.set(datePos, new Entry(datePos, 0));
                        }
                    }
                }
                return true;
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean result)
        {
            if (result && menuFlag)
            {
                setData();
            }
        }
    }

    private class GetDataTask extends AsyncTask<Boolean, Boolean, Boolean>
    {
        @Override
        protected Boolean doInBackground(Boolean... params)
        {
            try
            {
                String sql_total = "SELECT day, year, SUM(value) FROM aggregation_statistics WHERE app = -1 AND direction = -1 AND name = 'totalIpPacketLength' " + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";
                String sql_cloud = "SELECT day, year, SUM(bytes) FROM aggregation_data WHERE app = -1 AND direction = -1 AND handler = -1 AND service = -1" + trafficPropertyILP + " GROUP BY day, year ORDER BY year, day";

                DatabaseConnection dbConn = MainHandler.getDbConn();
                if (dbConn == null)
                {
                    return false;
                }

                DatabaseStatement query = dbConn.createStatement(sql_cloud);
                DatabaseResultSet res_cloud = query.executeQuery();

                query = MainHandler.getDbConn().createStatement(sql_total);
                DatabaseResultSet res_total = query.executeQuery();

                yVals_total = new ArrayList<>();
                yVals_cloud = new ArrayList<>();
                xVals = new ArrayList<>();

                boolean next_res = res_cloud.next();
                for (int i = 0; res_total.next(); i++)
                {
                    if (res_total.getInt(1) == null)
                        break;

                    xVals.add(DateConversion.getDateString(res_total.getInt(1), res_total.getInt(2)));
                    yVals_total.add(new Entry(i, ((float) res_total.getLong(3)) / 1048576));
                    if (next_res && res_cloud.getInt(1) != null && xVals.get(i).equals(DateConversion.getDateString(res_cloud.getInt(1), res_cloud.getInt(2))))
                    {
                        yVals_cloud.add(new Entry(i, ((float) res_cloud.getLong(3)) / 1048576));
                        next_res = res_cloud.next();
                    }
                    else
                    {
                        yVals_cloud.add(new Entry(i, 0));
                    }
                }
                return true;
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean result)
        {
            if (result && menuFlag)
            {
                data = true;
                setData();
            }
        }
    }

    private class GetDateTask extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... params)
        {
            // get first data to show in calendar
            startDate = CloudUsageAccessor.getFirstDate();
            return true;
        }

        protected void onPostExecute(Boolean result)
        {
            minCal.set(Calendar.HOUR_OF_DAY, 0);
            minCal.set(Calendar.MINUTE, 0);
            minCal.set(Calendar.SECOND, 0);
            minCal.add(Calendar.DAY_OF_YEAR, -6);
            if (startDate == 0)
            {
                minCal.add(Calendar.DAY_OF_YEAR, 6);
            }
            else if (minCal.getTimeInMillis() < startDate)
            {
                minCal.setTimeInMillis(startDate);
                minCal.set(Calendar.HOUR_OF_DAY, 0);
                minCal.set(Calendar.MINUTE, 0);
                minCal.set(Calendar.SECOND, 0);
            }
            minCal.set(Calendar.MILLISECOND, 0);
            minCal.add(Calendar.MILLISECOND, 0);
            maxCal.set(Calendar.HOUR_OF_DAY, 0);
            maxCal.set(Calendar.MINUTE, 0);
            maxCal.set(Calendar.SECOND, 0);
            maxCal.set(Calendar.MILLISECOND, 0);
            maxCal.add(Calendar.MILLISECOND, 0);

            new GetDataTask().execute();
            //setData();
            StatsListFragment frag = (StatsListFragment) (mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
            if (frag != null)
                frag.update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), false);

            if (startDate == 0)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(ResultsActivity.this).create();
                alertDialog.setTitle(getString(R.string.empty_database_title));
                alertDialog.setMessage(getString(R.string.empty_database_content));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener
    {
        private Boolean foundItemAndExecuted(int position)
        {
            String chosenItem = mPageTitles[position];
            int i;
            for (i = entries.length - 1; i >= 0; i--)
            {
                if (chosenItem.equalsIgnoreCase(entries[i]))
                {
                    break;
                }
            }

            switch (i)
            {
                case 6:
                    logIssue();
                    break;

                default:
                    return false;

            }
            return true;

        }


        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            if (!CaptureCentral.isMainHandlerInitialized())
            {
                ResultsActivity.this.startActivity(new Intent(ResultsActivity.this, SplashActivity.class));
                ResultsActivity.this.finish();
            }

            switch (position)
            {
                case 0:
                    toggleCapturing(ResultsActivity.this, menu);
                    break;

                default:
                    foundItemAndExecuted(position);
                    // -1 offset due to capturing toggle
                    mViewPager.setCurrentItem(position - 1);
                    break;
            }

            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        Fragment[] fragments = new Fragment[getCount()];

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if (!CaptureCentral.isMainHandlerInitialized())
            {
                ResultsActivity.this.startActivity(new Intent(ResultsActivity.this, SplashActivity.class));
                ResultsActivity.this.finish();
            }

            Fragment fragment = new StatsListFragment();

            Bundle bundle = new Bundle();
            bundle.putLong("start", minCal.getTimeInMillis());
            bundle.putLong("end", maxCal.getTimeInMillis());
            bundle.putBoolean("emptyData", startDate == 0);

            switch (position)
            {
                case 1:
                    bundle.putString("content", "Services");
                    break;

                case 2:
                    bundle.putString("content", "Regions");
                    break;

                case 3:
                    bundle.putString("content", "Settings");
                    break;

                case 4:
                    bundle.putString("content", "Imprint");
                    break;

                default:
                    bundle.putString("content", "Apps");
                    break;
            }

            fragment.setArguments(bundle);

            return fragment;
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position)
            {
                case 0:
                    fragments[0] = createdFragment;
                    break;
                case 1:
                    fragments[1] = createdFragment;
                    break;
                case 2:
                    fragments[2] = createdFragment;
                    break;
                case 3:
                    fragments[3] = createdFragment;
                    break;
                case 4:
                    fragments[4] = createdFragment;
            }
            return createdFragment;
        }

        public Fragment getCurrentFragment(int position)
        {
            return fragments[position];
        }

        public void setCurrentFragment(int position, Fragment frag)
        {
            fragments[position] = frag;
        }


        @Override
        public int getCount()
        {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return getString(R.string.title_results_apps);
                case 1:
                    return getString(R.string.title_results_services);
                case 2:
                    return getString(R.string.title_results_regions);
                case 3:
                    return getString(R.string.title_results_settings);
                case 4:
                    return getString(R.string.title_results_imprint);
            }
            return null;
        }
    }
}
