package de.rwth.comsys.cloudanalyzer.util;

import de.rwth.comsys.cloudanalyzer.database.DatabaseConnection;
import de.rwth.comsys.cloudanalyzer.database.DatabaseStatement;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SqlBatchExecuteThread extends Thread
{

    private static Logger logger = Logger.getLogger(SqlBatchExecuteThread.class.getName());
    private volatile boolean exit;
    private DatabaseStatement[] statements;
    private LinkedBlockingQueue<QueueElement> stmtQueue;
    private ReentrantLock lock;
    private Condition exceededBufferThreshold;
    private int bufferThreshold;
    private volatile boolean flush;
    private int maxWait;
    private Runnable onExecute;

    public SqlBatchExecuteThread(int bufferThreshold, int bufferSize, int maxWait, DatabaseConnection dbConn, String... sql)
    {
        exit = false;
        flush = false;
        stmtQueue = new LinkedBlockingQueue<>(bufferSize);
        lock = new ReentrantLock();
        exceededBufferThreshold = lock.newCondition();
        this.statements = new DatabaseStatement[sql.length];
        try
        {
            for (int s = 0; s < sql.length; s++)
            {
                statements[s] = dbConn.createStatement(sql[s]);
            }
        }
        catch (SQLException e)
        {
            logger.w(e.toString());
        }
        this.bufferThreshold = bufferThreshold;
        this.maxWait = maxWait;
        setDaemon(true);
    }

    @Override
    public void run()
    {
        while (!exit)
        {
            lock.lock();
            try
            {
                if (!flush && !exceededBufferThreshold())
                {
                    exceededBufferThreshold.await(maxWait, TimeUnit.SECONDS);
                }
            }
            catch (InterruptedException e)
            {
                logger.w("Thread " + getName() + " interrupted");
                interrupt();
            }
            finally
            {
                lock.unlock();
            }

            executeBatches();

        }
        synchronized (this)
        {
            executeBatches();
        }
    }

    private void executeBatches()
    {
        flush = false;
        if (onExecute != null)
            onExecute.run();
        QueueElement qe;
        while ((qe = stmtQueue.poll()) != null)
        {
            qe.setter.accept(statements[qe.index]);
        }
    }

    public void setStatement(int s, Consumer<DatabaseStatement> setter) throws InterruptedException
    {
        stmtQueue.put(new QueueElement(s, setter));
        lock.lock();
        if (exceededBufferThreshold())
        {
            exceededBufferThreshold.signal();
        }
        lock.unlock();
    }

    public void setOnExecute(Runnable onExecute)
    {
        this.onExecute = onExecute;
    }

    private boolean exceededBufferThreshold()
    {
        return stmtQueue.size() > bufferThreshold;
    }

    public void flush()
    {
        flush = true;
    }

    public void exit()
    {
        exit = true;
        lock.lock();
        exceededBufferThreshold.signalAll();
        lock.unlock();
    }

    private static class QueueElement
    {
        public final int index;
        public final Consumer<DatabaseStatement> setter;

        public QueueElement(int index, Consumer<DatabaseStatement> setter)
        {
            this.index = index;
            this.setter = setter;
        }
    }
}
