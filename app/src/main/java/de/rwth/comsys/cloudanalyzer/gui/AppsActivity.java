package de.rwth.comsys.cloudanalyzer.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.TextView;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.fragments.*;
import de.rwth.comsys.cloudanalyzer.gui.util.AppEntry;

import java.text.DateFormat;
import java.util.Calendar;

public class AppsActivity extends StatsActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle(((AppEntry) getIntent().getSerializableExtra("name")).getAppName());
        getSupportActionBar().setIcon(((AppEntry) getIntent().getSerializableExtra("name")).getAppIcon(this));

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        getSupportActionBar().setSubtitle(mSectionsPagerAdapter.getPageTitle(0));

        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                getSupportActionBar().setSubtitle(mSectionsPagerAdapter.getPageTitle(position));
                updateFragment(position);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putLong("minCalVal", minCal.getTimeInMillis());
        outState.putLong("maxCalVal", maxCal.getTimeInMillis());
        getSupportFragmentManager().putFragment(outState, Integer.toString(mViewPager.getCurrentItem()), ((SectionsPagerAdapter) mSectionsPagerAdapter).getCurrentFragment(mViewPager.getCurrentItem()));
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Long minCalVal = savedInstanceState.getLong("minCalVal");
        Long maxCalVal = savedInstanceState.getLong("maxCalVal");
        if (minCalVal != 0 && maxCalVal != 0)
        {
            // set global calendars to new times
            minCal.setTimeInMillis(minCalVal);
            maxCal.setTimeInMillis(maxCalVal);
            TextView title = findViewById(R.id.resultsHeader);
            if (minCal.get(Calendar.DATE) == maxCal.get(Calendar.DATE))
            {
                title.setText(String.format("%s:", DateFormat.getDateInstance().format(minCal.getTime())));
            }
            else
            {
                title.setText(String.format("%s - %s:", DateFormat.getDateInstance().format(minCal.getTime()), DateFormat.getDateInstance().format(maxCal.getTime())));
            }
            ((SectionsPagerAdapter) mSectionsPagerAdapter).setCurrentFragment(mViewPager.getCurrentItem(), getSupportFragmentManager().getFragment(savedInstanceState, Integer.toString(mViewPager.getCurrentItem())));
        }
    }

    @Override
    public void onActivityResult(int request, int result, Intent data)
    {
        if (result == 2)
        {
            long start = data.getLongExtra("start", 0);
            long end = data.getLongExtra("end", 0);
            if (start != 0 || end != 0)
            {
                minCal.setTimeInMillis(start);
                maxCal.setTimeInMillis(end);
                TextView title = findViewById(R.id.resultsHeader);
                if (minCal.get(Calendar.DATE) == maxCal.get(Calendar.DATE))
                {
                    title.setText(String.format("%s:", DateFormat.getDateInstance().format(minCal.getTime())));
                }
                else
                {
                    title.setText(String.format("%s - %s:", DateFormat.getDateInstance().format(minCal.getTime()), DateFormat.getDateInstance().format(maxCal.getTime())));
                }
                updateFragment(mViewPager.getCurrentItem());
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateFragment(mViewPager.getCurrentItem());
    }

    public void updateFragment(int position)
    {
        Fragment frag = ((SectionsPagerAdapter) mSectionsPagerAdapter).getCurrentFragment(position);
        if (frag instanceof AppDataAmountFragmentGeneral)
        {
            ((AppDataAmountFragmentGeneral) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof AppDataAmountFragmentGeneralDown)
        {
            ((AppDataAmountFragmentGeneralDown) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof AppDataAmountFragmentGeneralUp)
        {
            ((AppDataAmountFragmentGeneralUp) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof StatsListFragment)
        {
            ((StatsListFragment) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), true);
        }
        else if (frag instanceof AppRegionFragment)
        {
            ((AppRegionFragment) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        Fragment[] fragments = new Fragment[getCount()];

        private SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if (!CaptureCentral.isMainHandlerInitialized())
                AppsActivity.this.finish();

            Fragment fragment;
            Bundle bundle = new Bundle();
            bundle.putLong("start", minCal.getTimeInMillis());
            bundle.putLong("end", maxCal.getTimeInMillis());
            bundle.putBoolean("cloudData", false);

            switch (position)
            {
                case 1:
                    fragment = new AppDataAmountFragmentGeneralDown();
                    bundle.putString("name", ((AppEntry) getIntent().getSerializableExtra("name")).getPackageName());
                    break;
                case 2:
                    fragment = new AppDataAmountFragmentGeneralUp();
                    bundle.putString("name", ((AppEntry) getIntent().getSerializableExtra("name")).getPackageName());
                    break;

                case 3:
                    fragment = new StatsListFragment();
                    bundle.putString("content", "appservice");
                    bundle.putSerializable("appEntry", getIntent().getSerializableExtra("name"));
                    bundle.putFloat("upTraffic", ((AppEntry) getIntent().getSerializableExtra("name")).getUpTraffic());
                    bundle.putFloat("downTraffic", ((AppEntry) getIntent().getSerializableExtra("name")).getDownTraffic());
                    break;

                case 4:
                    fragment = new StatsListFragment();
                    bundle.putString("content", "appserviceregion");
                    bundle.putSerializable("appEntry", getIntent().getSerializableExtra("name"));
                    bundle.putFloat("upTraffic", ((AppEntry) getIntent().getSerializableExtra("name")).getUpTraffic());
                    bundle.putFloat("downTraffic", ((AppEntry) getIntent().getSerializableExtra("name")).getDownTraffic());
                    break;

                case 5:
                    fragment = new AppRegionFragment();
                    bundle.putString("packageName", ((AppEntry) getIntent().getSerializableExtra("name")).getPackageName());
                    bundle.putString("appName", ((AppEntry) getIntent().getSerializableExtra("name")).getAppName());
                    break;

                default:
                    fragment = new AppDataAmountFragmentGeneral();
                    bundle.putString("name", ((AppEntry) getIntent().getSerializableExtra("name")).getPackageName());
                    break;
            }

            fragment.setArguments(bundle);

            return fragment;
        }

        private Fragment getCurrentFragment(int position)
        {
            return fragments[position];
        }

        private void setCurrentFragment(int position, Fragment frag)
        {
            fragments[position] = frag;
        }


        @Override
        public int getCount()
        {
            return 6;
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
                    break;
                case 5:
                    fragments[5] = createdFragment;
                    break;
            }
            return createdFragment;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return getString(R.string.headerApps_DataAmount);
                case 1:
                    return getString(R.string.headerApps_DataAmount_Download);
                case 2:
                    return getString(R.string.headerApps_DataAmount_Upload);
                case 3:
                    return getString(R.string.headerApps_services);
                case 4:
                    return getString(R.string.headerApps_servicesRegions);
                case 5:
                    return getString(R.string.headerApps_regions);
            }
            return null;
        }
    }

}
