package de.rwth.comsys.cloudanalyzer.database;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public interface DatabaseConnection extends Closeable
{

    DatabaseStatement createStatement(String sql) throws SQLException;

    void executeFile(String file) throws IOException, SQLException;

    void executeFile(File file) throws IOException, SQLException;

    void executeFile(InputStream is) throws IOException, SQLException;

    boolean createdDatabase();

    void close();

    void beginTransaction();

    void endTransaction();

    void commitTransaction();
}
