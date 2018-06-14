package de.rwth.comsys.cloudanalyzer.database;

import java.io.Closeable;

public interface DatabaseResultSet extends Closeable
{

    Integer getInt(int index);

    Long getLong(int index);

    Short getShort(int index);

    Float getFloat(int index);

    Double getDouble(int index);

    String getString(int index);

    Boolean getBoolean(int index);

    boolean moveTo(int row);

    boolean move(int offset);

    boolean moveToFirst();

    boolean moveToLast();

    boolean next();

    boolean previous();

    int getColumnCount();

    void close();
}
