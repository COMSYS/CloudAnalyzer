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
import de.rwth.comsys.cloudanalyzer.gui.fragments.AppServiceRegionDataAmountFragmentGeneral;
import de.rwth.comsys.cloudanalyzer.gui.fragments.AppServiceRegionDataAmountFragmentGeneralDown;
import de.rwth.comsys.cloudanalyzer.gui.fragments.AppServiceRegionDataAmountFragmentGeneralUp;
import de.rwth.comsys.cloudanalyzer.gui.util.AppEntry;

import java.text.DateFormat;
import java.util.Calendar;

public class AppServiceRegionDetailActivity extends StatsActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle(((AppEntry) getIntent().getSerializableExtra("appEntry")).getAppName());
        getSupportActionBar().setIcon(((AppEntry) getIntent().getSerializableExtra("appEntry")).getAppIcon(this));

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

    public void updateFragment(int position)
    {
        Fragment frag = ((SectionsPagerAdapter) mSectionsPagerAdapter).getCurrentFragment(position);
        if (frag instanceof AppServiceRegionDataAmountFragmentGeneral)
        {
            ((AppServiceRegionDataAmountFragmentGeneral) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof AppServiceRegionDataAmountFragmentGeneralDown)
        {
            ((AppServiceRegionDataAmountFragmentGeneralDown) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
        else if (frag instanceof AppServiceRegionDataAmountFragmentGeneralUp)
        {
            ((AppServiceRegionDataAmountFragmentGeneralUp) frag).update(minCal.getTimeInMillis(), maxCal.getTimeInMillis());
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!CaptureCentral.isMainHandlerInitialized())
            finish();
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
                finish();

            Fragment fragment;
            Bundle bundle = new Bundle();
            bundle.putLong("start", minCal.getTimeInMillis());
            bundle.putLong("end", maxCal.getTimeInMillis());
            bundle.putBoolean("detailView", true);
            bundle.putBoolean("cloudData", false);
            bundle.putBoolean("regionData", true);

            switch (position)
            {
                case 1:
                    fragment = new AppServiceRegionDataAmountFragmentGeneralUp();
                    bundle.putString("name", ((AppEntry) getIntent().getSerializableExtra("appEntry")).getPackageName());
                    bundle.putString("service", Integer.toString(getIntent().getIntExtra("serviceID", -1)));
                    bundle.putString("region", Integer.toString(getIntent().getIntExtra("region", -1)));
                    break;

                case 2:
                    fragment = new AppServiceRegionDataAmountFragmentGeneralDown();
                    bundle.putString("name", ((AppEntry) getIntent().getSerializableExtra("appEntry")).getPackageName());
                    bundle.putString("service", Integer.toString(getIntent().getIntExtra("serviceID", -1)));
                    bundle.putString("region", Integer.toString(getIntent().getIntExtra("region", -1)));
                    break;


                default:
                    fragment = new AppServiceRegionDataAmountFragmentGeneral();
                    bundle.putString("name", ((AppEntry) getIntent().getSerializableExtra("appEntry")).getPackageName());
                    bundle.putString("service", Integer.toString(getIntent().getIntExtra("serviceID", -1)));
                    bundle.putString("region", Integer.toString(getIntent().getIntExtra("region", -1)));
                    break;
            }

            fragment.setArguments(bundle);

            return fragment;
        }


        @Override
        public int getCount()
        {
            return 3;
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
            }
            return createdFragment;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return getString(R.string.headerAppServiceDetail_DataAmount).replace("$app", ((AppEntry) getIntent().getSerializableExtra("appEntry")).getAppName()).replace("$service", getIntent().getStringExtra("serviceName"));
                case 1:
                    return getString(R.string.headerAppServiceDetail_DataAmountUp).replace("$app", ((AppEntry) getIntent().getSerializableExtra("appEntry")).getAppName()).replace("$service", getIntent().getStringExtra("serviceName"));
                case 2:
                    return getString(R.string.headerAppServiceDetail_DataAmountDown).replace("$app", ((AppEntry) getIntent().getSerializableExtra("appEntry")).getAppName()).replace("$service", getIntent().getStringExtra("serviceName"));
            }
            return null;
        }
    }
}
