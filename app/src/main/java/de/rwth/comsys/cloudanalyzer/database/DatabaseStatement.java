package de.rwth.comsys.cloudanalyzer.database;

import java.io.Closeable;
import java.sql.SQLException;

public interface DatabaseStatement extends Closeable
{

    DatabaseStatementType getType();

    void setInt(int index, Integer value);

    void setLong(int index, Long value);

    void setString(int index, String value);

    void setFloat(int index, Float value);

    void setDouble(int index, Double value);

    void setShort(int index, Short value);

    void setNull(int index, int sqlType);

    void setBytes(int index, byte[] bytes);

    void setBoolean(int index, boolean value);

    void execute() throws SQLException;

    void executeNoResult() throws SQLException;

    long executeUpdate() throws SQLException;

    DatabaseResultSet executeQuery() throws SQLException;

    void addToBatch();

    void executeBatch() throws SQLException;

    void close();
}
