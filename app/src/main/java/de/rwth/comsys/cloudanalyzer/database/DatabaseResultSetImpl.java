package de.rwth.comsys.cloudanalyzer.database;

import android.database.Cursor;

public class DatabaseResultSetImpl implements DatabaseResultSet
{

    private Cursor cur;

    DatabaseResultSetImpl(Cursor cur)
    {
        this.cur = cur;
    }

    @Override
    public Integer getInt(int index)
    {
        return cur.getInt(index - 1);
    }

    @Override
    public Long getLong(int index)
    {
        return cur.getLong(index - 1);
    }

    @Override
    public Short getShort(int index)
    {
        return cur.getShort(index - 1);
    }

    @Override
    public Float getFloat(int index)
    {
        return cur.getFloat(index - 1);
    }

    @Override
    public Double getDouble(int index)
    {
        return cur.getDouble(index - 1);
    }

    @Override
    public String getString(int index)
    {
        return cur.getString(index - 1);
    }

    @Override
    public Boolean getBoolean(int index)
    {
        return getInt(index) != 0;
    }

    @Override
    public boolean moveTo(int row)
    {
        return cur.moveToPosition(row - 1);
    }

    @Override
    public boolean move(int offset)
    {
        return cur.move(offset);
    }

    @Override
    public boolean moveToFirst()
    {
        return cur.moveToFirst();
    }

    @Override
    public boolean moveToLast()
    {
        return cur.moveToLast();
    }

    @Override
    public boolean next()
    {
        return cur.moveToNext();
    }

    @Override
    public boolean previous()
    {
        return cur.moveToPrevious();
    }

    @Override
    public int getColumnCount()
    {
        return cur.getColumnCount();
    }

    @Override
    public void close()
    {
        cur.close();
    }
}
