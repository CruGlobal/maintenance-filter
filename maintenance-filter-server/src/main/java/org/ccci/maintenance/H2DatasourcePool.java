package org.ccci.maintenance;

import org.ccci.maintenance.util.Exceptions;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * H2 is an optional dependency; this class should only be constructed if the app is going to take responsibility
 * to create a maintenance database (using h2).  If the appserver manages the database, then this class isn't needed
 * and should not be constructed.
 *
 * @author Matt Drees
 */
public class H2DatasourcePool {

    private JdbcConnectionPool pool;

    public DataSource getDataSource() {
        return pool;
    }

    public void initH2DatabaseLocatedAt(String dbPath)
    {
        /*
         * use a file-based database, since this must persist across jvm restarts.
         */
        String url = "jdbc:h2:file:" + dbPath;
        pool = JdbcConnectionPool.create(url, "sa", "");
    }

    public void dispose() {
        try
        {
            pool.dispose();
            pool = null;
        }
        catch (SQLException e)
        {
            throw Exceptions.wrap(e);
        }
    }
}
