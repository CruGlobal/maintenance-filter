package org.ccci.maintenance;

import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.Exceptions;
import org.h2.jdbcx.JdbcConnectionPool;

public class Bootstrap
{

    private final ServletContext servletContext;
    private JdbcConnectionPool pool;

    public Bootstrap(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }

    public static Bootstrap getInstance(ServletContext servletContext)
    {
        String location = Bootstrap.class.getName();
        Bootstrap bootstrap = (Bootstrap) servletContext.getAttribute(location);
        if (bootstrap != null)
        {
            return bootstrap;
        }
        else
        {
            throw new IllegalStateException("Boostrap has not yet been created");
        }
    }

    public void init(String filterName)
    {
        String bootstrapLocation = Bootstrap.class.getName();
        Bootstrap bootstrap = (Bootstrap) servletContext.getAttribute(bootstrapLocation);
        if (bootstrap == null)
        {
            DataSource dataSource = createDataSource();
            initDatabaseIfNecessary(dataSource);
            servletContext.setAttribute(bootstrapLocation, this);
            bootstrap = this;
        }
        MaintenanceServiceImpl maintenanceService = new MaintenanceServiceImpl(Clock.system(), bootstrap.pool, filterName);
        servletContext.setAttribute(getMaintenanceServiceLocation(filterName), maintenanceService);
        
    }
    
    /** may be called multiple times, if multiple filters are configured */
    public void shutdown()
    {
        shutdownPoolIfNecessary();
    }

    private void shutdownPoolIfNecessary()
    {
        if (pool != null)
        {
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

    private void initDatabaseIfNecessary(DataSource dataSource)
    {
        new DatabaseMigrator(dataSource).migrate();
    }

    private DataSource createDataSource()
    {
        String datasourceParamName = "org.ccci.maintenance.window.datasource";
        String dbPathParamName = "org.ccci.maintenance.window.db.path";
        String datasourceLocation = servletContext.getInitParameter(datasourceParamName);
        if (datasourceLocation != null)
        {
            return lookupDataSource(datasourceLocation);
        }
        else
        {
            String dbPath = servletContext.getInitParameter(dbPathParamName);
            if (dbPath == null)
                throw new IllegalArgumentException(String.format(
                    "you must provide either %s or %s",
                    datasourceParamName,
                    dbPathParamName
                ));
            initH2DatasourcePoolLocatedAt(dbPath);
            return pool;
        }
    }

    private void initH2DatasourcePoolLocatedAt(String dbPath)
    {
        /*
         * use a file-based database, since this must persist across jvm restarts.
         * avoid file-based locking, because ungraceful process terminations cause
         * the lock file to hang around, which prevents the app from starting.
         */
        String url = "jdbc:h2:file:" + dbPath + ";FILE_LOCK=SOCKET";
        pool = JdbcConnectionPool.create(url, "sa", "");
    }

    private DataSource lookupDataSource(String datasourceLocation)
    {
        try
        {
            Object found = new InitialContext().lookup(datasourceLocation);
            if (found == null)
            {
                throw new IllegalStateException("No datasource bound at " + datasourceLocation);
            }
            if (!(found instanceof DataSource))
            {
                throw new IllegalStateException(String.format(
                    "Found %s bound at %s instead of a DataSource",
                    found,
                    datasourceLocation));
            }
            return (DataSource) found;
        }
        catch (NameNotFoundException e)
        {
            throw new IllegalStateException("No datasource bound at " + datasourceLocation, e);
        }
        catch (NamingException e)
        {
            throw Exceptions.wrap(e);
        }
    }

    /** gets the default maintenance service */
    public MaintenanceService getMaintenanceService()
    {
        return getMaintenanceService(null);
    }
    
    /** 
     * gets the maintenance service with the given name.
     * If {@code name} is null, the default maintenance service is returned. 
     */
    public MaintenanceService getMaintenanceService(String name)
    {
        String location = getMaintenanceServiceLocation(name);
        MaintenanceService maintenanceService = (MaintenanceService) servletContext.getAttribute(location);
        if (maintenanceService == null) throw new IllegalStateException("can't find maintenance service in servlet context at " + location);
        return maintenanceService;
    }

    private String getMaintenanceServiceLocation(String filterName)
    {
        String suffix = filterName == null ? "" : ("-" + filterName);
        return MaintenanceService.class.getName() + suffix;
    }
    
}
