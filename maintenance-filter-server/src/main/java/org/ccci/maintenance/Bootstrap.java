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

    public void init()
    {
        DataSource dataSource = createDataSource();
        initDatabaseIfNecessary(dataSource);
        MaintenanceServiceImpl maintenanceService = new MaintenanceServiceImpl(Clock.system(), dataSource);
        String maintenanceServiceLocation = MaintenanceService.class.getName();
        servletContext.setAttribute(maintenanceServiceLocation, maintenanceService);
        
        String bootstrapLocation = Bootstrap.class.getName();
        servletContext.setAttribute(bootstrapLocation, this);
    }
    
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
        pool = JdbcConnectionPool.create("jdbc:h2:file:" + dbPath, "sa", "");
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

    public MaintenanceService getMaintenanceService()
    {
        String location = MaintenanceService.class.getName();
        return (MaintenanceService) servletContext.getAttribute(location);
    }
    
}
