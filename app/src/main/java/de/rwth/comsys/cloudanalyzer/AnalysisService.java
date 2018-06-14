package de.rwth.comsys.cloudanalyzer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import de.rwth.comsys.cloudanalyzer.sources.AndroidLiveSource;
import de.rwth.comsys.cloudanalyzer.sources.NetworkPacketSource;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

public class AnalysisService extends Service implements Runnable
{
    private static Logger logger = Logger.getLogger(AnalysisService.class.getName());
    private Thread analysisThread;
    private boolean analysisActive;

    public AnalysisService()
    {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!analysisActive)
        {
            super.onStartCommand(intent, flags, startId);

            analysisActive = false;

            if (analysisThread != null)
            {
                analysisThread.interrupt();
            }

            analysisThread = new Thread(this, "MainHandlerThread");
            analysisThread.setPriority(Thread.MIN_PRIORITY);
            analysisThread.start();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        MainHandler.stopAnalysis();
        try
        {
            analysisThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            analysisActive = false;
        }

        logger.i("MainHandler Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void run()
    {
        if (!analysisActive)
        {
            analysisActive = true;

            final NetworkPacketSource networkPacketSource = new AndroidLiveSource();

            MainHandler.startAnalysis(networkPacketSource);

            analysisActive = false;
        }
    }
}
