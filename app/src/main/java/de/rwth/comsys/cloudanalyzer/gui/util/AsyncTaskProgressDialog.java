package de.rwth.comsys.cloudanalyzer.gui.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import de.rwth.comsys.cloudanalyzer.R;

public abstract class AsyncTaskProgressDialog<Params, Progress, Results> extends AsyncTask<Params, Progress, Results>
{
    private static final String TAG = "AsyncTaskProgressDialog";

    Activity activity;
    ProgressDialog progressDialog;

    protected AsyncTaskProgressDialog(Activity activity)
    {
        this.activity = activity;
    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(activity.getString(R.string.loading));
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        if (!isCancelled() && getActivity() != null)
            progressDialog.show();
    }

    @Override
    protected void onCancelled(Results results)
    {
        if (progressDialog != null)
        {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            progressDialog = null;
        }
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Results results)
    {
        try
        {
            if (progressDialog != null)
            {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
                progressDialog = null;
            }
            super.onPostExecute(results);
        }
        catch (IllegalArgumentException e)
        {
            Log.e(TAG, "IllegalArgumentException: " + e.getMessage());
        }

    }

    protected Activity getActivity()
    {
        return activity;
    }
}
