package de.rwth.comsys.cloudanalyzer.gui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.DatePickerFragment;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.OnDateSetListenerCustom;
import de.rwth.comsys.cloudanalyzer.gui.util.CloudUsageAccessor;

import java.text.DateFormat;
import java.util.Calendar;

public abstract class StatsActivity extends AppCompatActivity
{
    protected final static Calendar maxCal = Calendar.getInstance();
    protected final static Calendar minCal = Calendar.getInstance();
    protected boolean calRunning = false;
    protected long startDate = System.currentTimeMillis();
    FragmentPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    OnDateSetListenerCustom listener = new OnDateSetListenerCustom()
    {
        @Override
        public void onDateSet(long minDate, long maxDate)
        {
            // set global calendars to new times
            minCal.setTimeInMillis(minDate);
            maxCal.setTimeInMillis(maxDate);
            // refresh title
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
    };
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        minCal.setTimeInMillis(getIntent().getLongExtra("min", System.currentTimeMillis()));
        maxCal.setTimeInMillis(getIntent().getLongExtra("max", System.currentTimeMillis()));
        new GetDateTask().execute();

        final TextView title = findViewById(R.id.resultsHeader);
        if (minCal.get(Calendar.DATE) == maxCal.get(Calendar.DATE))
        {
            title.setText(String.format("%s:", DateFormat.getDateInstance().format(minCal.getTime())));
        }
        else
        {
            title.setText(String.format("%s - %s:", DateFormat.getDateInstance().format(minCal.getTime()), DateFormat.getDateInstance().format(maxCal.getTime())));
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();
        intent.putExtra("start", minCal.getTimeInMillis());
        intent.putExtra("end", maxCal.getTimeInMillis());
        setResult(2, intent);
        super.onBackPressed();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!CaptureCentral.isMainHandlerInitialized())
            this.finish();
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
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        // if calendar choosen, start GetDateTask
        if (id == R.id.action_calendar_stats && !calRunning)
        {
            calRunning = true;
            showDialogFragment(startDate);
            calRunning = false;
            return true;
        }
        else if (item.getItemId() == R.id.action_toggle_vpn)
        {
            ResultsActivity.toggleCapturing(this, menu);
        }
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

    public abstract void updateFragment(int position);

    private class GetDateTask extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... params)
        {
            // get first data to show in calendar
            startDate = CloudUsageAccessor.getFirstDate();
            return true;
        }
    }

}
