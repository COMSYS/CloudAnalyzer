package de.rwth.comsys.cloudanalyzer.gui.util;

import android.app.Activity;
import android.widget.Toast;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.cloudanalyzer.R;

public abstract class AsyncTaskProgressDialogWithLock<Params, Progress, Results> extends AsyncTaskProgressDialog<Params, Progress, Results>
{
    protected Boolean noaction = false;

    protected AsyncTaskProgressDialogWithLock(Activity activity)
    {
        super(activity);
    }


    @Override
    protected void onPreExecute()
    {
        if (CaptureCentral.getInstance().isActive())
        {
            Toast.makeText(activity, R.string.active, Toast.LENGTH_SHORT).show();
            noaction = true;
        }
        if (CaptureCentral.getInstance().isLocked())
        {
            Toast.makeText(activity, R.string.locked, Toast.LENGTH_SHORT).show();
            noaction = true;
        }
        if (!noaction)
        {
            // disallow service startup
            CaptureCentral.getInstance().lock();
        }
    }

    @Override
    protected void onCancelled(Results results)
    {
        if (!noaction)
        {
            // allow service startup
            CaptureCentral.getInstance().unlock();
        }
        super.onCancelled(results);
    }


    @Override
    protected void onPostExecute(Results results)
    {
        if (!noaction)
        {
            // allow service startup
            CaptureCentral.getInstance().unlock();
        }
        super.onPostExecute(results);
    }
}
