package de.rwth.comsys.cloudanalyzer.gui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RelativeLayout;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;
import de.rwth.comsys.cloudanalyzer.gui.fragments.util.TermsOfUseDialog;

public class SplashActivity extends Activity
{
    private static Boolean mToSAccepted = false;

    public static void setToSAccepted(boolean toSAccepted)
    {
        SplashActivity.mToSAccepted = toSAccepted;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mToSAccepted = !mSharedPref.getBoolean("pref_notification_splash", false) && mSharedPref.getBoolean("pref_tos_accepted", false);

        RelativeLayout relativeLayout = findViewById(R.id.splashRelativeLayout);
        relativeLayout.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                if (!mToSAccepted)
                {
                    DialogFragment dialog = new TermsOfUseDialog();
                    dialog.show(getFragmentManager(), "TermsOfUseFragmentTag");
                }
            }

        });

        new Thread()
        {
            @Override
            public void run()
            {
                if (!CaptureCentral.isMainHandlerInitialized())
                {
                    CaptureCentral.getInstance();
                    CaptureCentral.initializeMainHandler(SplashActivity.this.getApplicationContext());
                }
                while (!mToSAccepted)
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e)
                    {
                        // do nothing
                    }
                Intent mainIntent = new Intent(SplashActivity.this, ResultsActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish();
            }
        }.start();
    }

    protected void onResume()
    {
        super.onResume();
        if (!mToSAccepted)
        {
            DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag("TermsOfUseFragmentTag");
            if (dialog == null)
            {
                dialog = new TermsOfUseDialog();
                dialog.show(getFragmentManager(), "TermsOfUseFragmentTag");
            }
        }
    }

}
