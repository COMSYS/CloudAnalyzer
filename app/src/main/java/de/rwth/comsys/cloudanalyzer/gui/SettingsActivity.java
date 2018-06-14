package de.rwth.comsys.cloudanalyzer.gui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.fragments.*;
import de.rwth.comsys.cloudanalyzer.gui.util.SettingsListAdapter;

public class SettingsActivity extends Activity
{
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getActionBar().setTitle(R.string.title_results_settings);
        getActionBar().setIcon(R.drawable.ic_settings);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        final TextView title = findViewById(R.id.resultsHeader);
        title.setText(mSectionsPagerAdapter.getPageTitle(0));

        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                title.setText(mSectionsPagerAdapter.getPageTitle(position));
                getActionBar().setSubtitle(mSectionsPagerAdapter.getPageTitle(position));
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!CaptureCentral.isMainHandlerInitialized())
            SettingsActivity.this.finish();
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
        ResultsActivity.updateIcon(menu, CaptureCentral.getInstance().isActive());
        menu.removeItem(R.id.action_calendar_stats);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_toggle_vpn)
        {
            ResultsActivity.toggleCapturing(this, menu);
        }
        return super.onOptionsItemSelected(item);
    }

    public class SectionsPagerAdapter extends android.support.v13.app.FragmentPagerAdapter
    {
        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if (!CaptureCentral.isMainHandlerInitialized())
                SettingsActivity.this.finish();

            Fragment fragment = null;
            Bundle bundle = new Bundle();

            String fragmentClicked = ((SettingsListAdapter.SettingsEntry) getIntent().getSerializableExtra("settingsEntry")).getFragmentName();

            if (fragmentClicked == null || fragmentClicked.equals("SettingsNotificationFragment"))
            {
                fragment = new SettingsNotificationFragment();
                mViewPager.setCurrentItem(0);
            }
            else if (fragmentClicked.equals("SettingsMiscFragment"))
            {
                fragment = new SettingsMiscFragment();
                mViewPager.setCurrentItem(1);
            }
            else if (fragmentClicked.equals("SettingsDatabaseFragment"))
            {
                fragment = new SettingsDatabaseFragment();
                mViewPager.setCurrentItem(2);
            }
            else if (fragmentClicked.equals("DebugStatisticsFragment"))
            {
                fragment = new DebugStatisticsFragment();
                mViewPager.setCurrentItem(3);
            }
            else if (fragmentClicked.equals("DebugConnectionsFragment"))
            {
                fragment = new DebugConnectionsFragment();
                mViewPager.setCurrentItem(4);
            }

            bundle.putString("settingsEntry", fragmentClicked);
            return fragment;
        }


        @Override
        public int getCount()
        {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            String fragmentClicked = ((SettingsListAdapter.SettingsEntry) getIntent().getSerializableExtra("settingsEntry")).getFragmentName();

            if (fragmentClicked == null || fragmentClicked.equals("SettingsNotificationFragment"))
                return getString(R.string.headerSettings_notifications);
            if (fragmentClicked.equals("SettingsMiscFragment"))
                return getString(R.string.headerSettings_misc);
            if (fragmentClicked.equals("SettingsDatabaseFragment"))
                return getString(R.string.headerSettings_database);
            if (fragmentClicked.equals("DebugStatisticsFragment"))
                return getString(R.string.headerDebug_statistics);
            if (fragmentClicked.equals("DebugConnectionsFragment"))
                return getString(R.string.headerDebug_connections);

            return null;
        }
    }
}

