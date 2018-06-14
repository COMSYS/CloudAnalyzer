package de.rwth.comsys.cloudanalyzer.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;

public class DatabaseStatementImpl implements DatabaseStatement
{

    private SQLiteStatement stmt;
    private SQLiteDatabase db;
    private String sql;
    private DatabaseStatementType type;
    private List<Param[]> batch;
    private int paramCount;
    private Param[] currentParams;

    DatabaseStatementImpl(SQLiteDatabase db, String sql, DatabaseStatementType type) throws SQLException
    {
        this.type = type;
        paramCount = 0;
        this.db = db;
        this.sql = sql;
        for (int i = 0; i < sql.length(); ++i)
            if (sql.charAt(i) == '?')
                paramCount++;
        if (type != DatabaseStatementType.QUERY)
        {
            try
            {
                this.stmt = db.compileStatement(sql);
            }
            catch (android.database.SQLException e)
            {
                throw new SQLException(e.toString());
            }
        }
        currentParams = new Param[paramCount];
    }

    @Override
    public DatabaseStatementType getType()
    {
        return type;
    }

    @Override
    public void setInt(int index, Integer value)
    {
        currentParams[index - 1] = new Param(Types.INTEGER, value);
    }

    @Override
    public void setLong(int index, Long value)
    {
        currentParams[index - 1] = new Param(Types.BIGINT, value);
    }

    @Override
    public void setString(int index, String value)
    {
        currentParams[index - 1] = new Param(Types.VARCHAR, value);
    }

    @Override
    public void setFloat(int index, Float value)
    {
        currentParams[index - 1] = new Param(Types.FLOAT, value);
    }

    @Override
    public void setDouble(int index, Double value)
    {
        currentParams[index - 1] = new Param(Types.DOUBLE, value);
    }

    @Override
    public void setShort(int index, Short value)
    {
        currentParams[index - 1] = new Param(Types.SMALLINT, value);
    }

    @Override
    public void setNull(int index, int sqlType)
    {
        if (getType() != DatabaseStatementType.UPDATE)
            currentParams[index - 1] = new Param(Types.NULL, null);
    }

    @Override
    public void setBytes(int index, byte[] bytes)
    {
        currentParams[index - 1] = new Param(Types.BLOB, bytes);
    }

    @Override
    public void setBoolean(int index, boolean value)
    {
        setInt(index, value ? 1 : 0);
    }

    private DatabaseResultSet executeQuery(Param[] params) throws SQLException
    {
        if (getType() != DatabaseStatementType.QUERY)
            throw new UnsupportedOperationException();
        try
        {
            String[] args = new String[paramCount];
            for (int i = 0; i < args.length; ++i)
            {
                Param param = params[i];
                args[i] = param.value.toString();
            }
            Cursor cur = db.rawQuery(sql, args);
            return new DatabaseResultSetImpl(cur);
        }
        catch (android.database.SQLException e)
        {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public DatabaseResultSet executeQuery() throws SQLException
    {
        return executeQuery(currentParams);
    }

    @Override
    public void addToBatch()
    {
        if (batch == null)
        {
            batch = new LinkedList<>();
        }
        batch.add(currentParams);
        currentParams = new Param[paramCount];
    }

    @Override
    public void executeBatch() throws SQLException
    {
        if (batch == null)
            return;
        for (Param[] params : batch)
        {
            execute(params);
        }
        batch.clear();
    }

    @Override
    public void close()
    {
        if (stmt == null)
        {
            return;
        }
        stmt.close();
    }

    private void bindValues(Param[] params)
    {
        for (int i = 0; i < params.length; ++i)
        {
            Param param = params[i];
            switch (param.sqlType)
            {
                case Types.INTEGER:
                    stmt.bindLong(i + 1, (Integer) param.value);
                    break;
                case Types.BIGINT:
                    stmt.bindLong(i + 1, (Long) param.value);
                    break;
                case Types.SMALLINT:
                    stmt.bindLong(i + 1, (Short) param.value);
                    break;
                case Types.FLOAT:
                    stmt.bindDouble(i + 1, (Float) param.value);
                    break;
                case Types.DOUBLE:
                    stmt.bindDouble(i + 1, (Double) param.value);
                    break;
                case Types.VARCHAR:
                    stmt.bindString(i + 1, (String) param.value);
                    break;
                case Types.BLOB:
                    stmt.bindBlob(i + 1, (byte[]) param.value);
                    break;
                case Types.NULL:
                    stmt.bindNull(i + 1);
                    break;
            }
        }
    }

    private long executeUpdate(Param[] params) throws SQLException
    {
        try
        {
            bindValues(params);
            if (getType() == DatabaseStatementType.NO_RESULT || getType() == DatabaseStatementType.QUERY || stmt == null)
                throw new UnsupportedOperationException();
            if (getType() == DatabaseStatementType.INSERT)
                return stmt.executeInsert();
            else
                return stmt.executeUpdateDelete();
        }
        catch (android.database.SQLException e)
        {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public long executeUpdate() throws SQLException
    {
        return executeUpdate(currentParams);
    }

    private void executeNoResult(Param[] params) throws SQLException
    {
        try
        {
            if (getType() != DatabaseStatementType.NO_RESULT || stmt == null)
                throw new UnsupportedOperationException();
            bindValues(params);
            stmt.execute();
        }
        catch (android.database.SQLException e)
        {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public void executeNoResult() throws SQLException
    {
        executeNoResult(currentParams);
    }

    private void execute(Param[] params) throws SQLException
    {
        try
        {
            switch (getType())
            {
                case NO_RESULT:
                    executeNoResult(params);
                    break;
                case QUERY:
                    executeQuery(params);
                    break;
                default:
                    executeUpdate(params);
                    break;
            }
        }
        catch (android.database.SQLException e)
        {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public void execute() throws SQLException
    {
        execute(currentParams);
    }

    @Override
    public String toString()
    {
        return stmt.toString();
    }

    private static class Param
    {
        public int sqlType;
        public Object value;

        public Param(int sqlType, Object value)
        {
            this.sqlType = sqlType;
            this.value = value;
        }
    }
}
