package de.rwth.comsys.cloudanalyzer.gui;

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
import de.rwth.comsys.cloudanalyzer.gui.fragments.RegionDataAmountFragmentGeneral;
import de.rwth.comsys.cloudanalyzer.gui.fragments.RegionDataAmountFragmentGeneralDown;
import de.rwth.comsys.cloudanalyzer.gui.fragments.RegionDataAmountFragmentGeneralUp;
import de.rwth.comsys.cloudanalyzer.gui.fragments.StatsListFragment;
import de.rwth.comsys.cloudanalyzer.gui.util.RegionEntry;

import java.text.DateFormat;
import java.util.Calendar;

public class RegionsActivity extends StatsActivity
{
    SectionsPagerAdapter mSectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        getSupportActionBar().setTitle(((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getRegionName());
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
        getSupportFragmentManager().putFragment(outState, Integer.toString(mViewPager.getCurrentItem()), mSectionsPagerAdapter.getCurrentFragment(mViewPager.getCurrentItem()));
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
            mSectionsPagerAdapter.setCurrentFragment(mViewPager.getCurrentItem(), getSupportFragmentManager().getFragment(savedInstanceState, Integer.toString(mViewPager.getCurrentItem())));
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
        Fragment frag = mSectionsPagerAdapter.getCurrentFragment(position);
        if (frag instanceof RegionDataAmountFragmentGeneral)
        {
            ((RegionDataAmountFragmentGeneral) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof RegionDataAmountFragmentGeneralDown)
        {
            ((RegionDataAmountFragmentGeneralDown) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof RegionDataAmountFragmentGeneralUp)
        {
            ((RegionDataAmountFragmentGeneralUp) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof StatsListFragment)
        {
            ((StatsListFragment) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis(), true);
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter
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
                RegionsActivity.this.finish();

            Fragment fragment;
            Bundle bundle = new Bundle();
            bundle.putLong("start", minCal.getTimeInMillis());
            bundle.putLong("end", maxCal.getTimeInMillis());
            bundle.putBoolean("cloudData", true);

            switch (position)
            {
                case 1:
                    fragment = new RegionDataAmountFragmentGeneralDown();
                    bundle.putString("name", Integer.toString(((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getRegionID()));
                    break;
                case 2:
                    fragment = new RegionDataAmountFragmentGeneralUp();
                    bundle.putString("name", Integer.toString(((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getRegionID()));
                    break;
                case 3:
                    fragment = new StatsListFragment();
                    bundle.putString("content", "regionapp");
                    bundle.putInt("regionID", ((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getRegionID());
                    bundle.putFloat("upTraffic", ((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getUpTraffic());
                    bundle.putFloat("downTraffic", ((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getDownTraffic());
                    break;

                case 4:
                    fragment = new StatsListFragment();
                    bundle.putString("content", "regionservice");
                    bundle.putInt("regionID", ((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getRegionID());
                    bundle.putFloat("upTraffic", ((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getUpTraffic());
                    bundle.putFloat("downTraffic", ((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getDownTraffic());

                    break;

                default:
                    fragment = new RegionDataAmountFragmentGeneral();
                    bundle.putString("name", Integer.toString(((RegionEntry) getIntent().getSerializableExtra("regionEntry")).getRegionID()));
                    break;
            }

            fragment.setArguments(bundle);

            return fragment;
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
            }
            return createdFragment;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return getString(R.string.headerRegions_dataAmount).replace("$region", getSupportActionBar().getTitle());
                case 1:
                    return getString(R.string.headerRegions_dataAmount_download).replace("$region", getSupportActionBar().getTitle());
                case 2:
                    return getString(R.string.headerRegions_dataAmount_upload).replace("$region", getSupportActionBar().getTitle());
                case 3:
                    return getString(R.string.headerRegions_regionApp).replace("$region", getSupportActionBar().getTitle());
                case 4:
                    return getString(R.string.headerRegions_regionService).replace("$region", getSupportActionBar().getTitle());
            }
            return null;
        }
    }
}
