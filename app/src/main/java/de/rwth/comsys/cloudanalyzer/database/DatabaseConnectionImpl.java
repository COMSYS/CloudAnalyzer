package de.rwth.comsys.cloudanalyzer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.util.logging.Logger;

import java.io.*;
import java.sql.SQLException;

public class DatabaseConnectionImpl extends SQLiteOpenHelper implements DatabaseConnection
{

    private static Logger logger = Logger.getLogger(DatabaseConnectionImpl.class.getName());
    private boolean createdDatabase;

    public DatabaseConnectionImpl(Context context, String dbName, int dbVersion)
    {
        super(context, dbName, null, dbVersion);
        //prompt onCreate etc.
        getReadableDatabase();
    }

    @Override
    public DatabaseStatement createStatement(String sql) throws SQLException
    {
        return createStatement(sql, getWritableDatabase());
    }

    public DatabaseStatement createStatement(String sql, SQLiteDatabase db) throws SQLException
    {
        sql = sql.trim();
        DatabaseStatementType type = getType(sql);
        if (type == null)
            return null;

        return new DatabaseStatementImpl(db, sql, type);
    }

    @Override
    public void executeFile(String file) throws IOException, SQLException
    {
        executeFile(new File(file));
    }

    public void executeFile(String file, SQLiteDatabase db) throws IOException, SQLException
    {
        executeFile(new File(file), db);
    }

    @Override
    public void executeFile(File file) throws IOException, SQLException
    {
        executeFile(file, getWritableDatabase());
    }

    public void executeFile(File file, SQLiteDatabase db) throws IOException, SQLException
    {
        executeFile(new FileInputStream(file), db);
    }

    @Override
    public void executeFile(InputStream is) throws IOException, SQLException
    {
        executeFile(is, getWritableDatabase());
    }

    public void executeFile(InputStream is, SQLiteDatabase db) throws IOException, SQLException
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = r.readLine()) != null)
        {
            if (line.startsWith("--"))
                continue;
            if (line.endsWith(";"))
                line = line.substring(0, line.length() - 1);
            createStatement(line, db).execute();
        }
    }

    @Override
    public boolean createdDatabase()
    {
        return createdDatabase;
    }

    @Override
    public void beginTransaction()
    {
        getWritableDatabase().beginTransaction();
    }

    @Override
    public void endTransaction()
    {
        getWritableDatabase().endTransaction();
    }

    @Override
    public void commitTransaction()
    {
        getWritableDatabase().setTransactionSuccessful();
    }

    private DatabaseStatementType getType(String sql)
    {
        int i = sql.indexOf(' ');
        if (i < 0)
            return null;
        String cmd = sql.substring(0, i).toUpperCase();
        switch (cmd)
        {
            case "SELECT":
                return DatabaseStatementType.QUERY;
            case "INSERT":
                return DatabaseStatementType.INSERT;
            case "UPDATE":
                return DatabaseStatementType.UPDATE;
            case "DELETE":
                return DatabaseStatementType.DELETE;
            default:
                return DatabaseStatementType.NO_RESULT;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        createdDatabase = true;
        try
        {
            executeFile(MainHandler.getAssets().open("storage/dbSQL/ca.sql"), db);
            logger.d("Database created.");
        }
        catch (IOException | SQLException e)
        {
            logger.e(e.toString());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        switch (oldVersion)
        {
            case 1:
                //upgrade logic from version 1 to 2
            case 2:
                //upgrade logic from version 2 to 3
            case 3:
                //upgrade logic from version 3 to 4
                break;
            default:
                logger.e("onUpgrade() with unknown old version " + oldVersion);
        }
        logger.i("Database up/downgraded: " + oldVersion + " -> " + newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        logger.e("onDowngrade() is not supported");
    }
}
