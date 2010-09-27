package org.ccci.maintenance;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.Exceptions;
import org.h2.jdbcx.JdbcDataSource;

public class Bootstrap
{

    private final ServletContext servletContext;

    public Bootstrap(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }

    public static synchronized Bootstrap getInstance(ServletContext servletContext)
    {
        String location = Bootstrap.class.getName();
        Bootstrap bootstrap = (Bootstrap) servletContext.getAttribute(location);
        if (bootstrap != null)
        {
            return bootstrap;
        }
        bootstrap = new Bootstrap(servletContext);
        bootstrap.init();
        servletContext.setAttribute(location, bootstrap);
        return bootstrap;
    }

    private void init()
    {
        DataSource dataSource = createDataSource();
        initDatabaseIfNecessary(dataSource);
        MaintenanceServiceImpl maintenanceService = new MaintenanceServiceImpl(Clock.system(), dataSource);
        String location = MaintenanceService.class.getName();
        servletContext.setAttribute(location, maintenanceService);
    }

    private void initDatabaseIfNecessary(DataSource dataSource)
    {
        new DatabaseMigrator(dataSource).migrate();
    }

    private DataSource createDataSource()
    {
        String datasourceLocation = servletContext.getInitParameter("org.ccci.maintenance.window.datasource");
        if (datasourceLocation != null)
        {
            return lookupDataSource(datasourceLocation);
        }
        else
        {
            String dbPath = servletContext.getInitParameter("org.ccci.maintenance.window.db.path");
            return createH2DatasourceLocatedAt(dbPath);
        }
    }

    private DataSource createH2DatasourceLocatedAt(String dbPath)
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:file:" + dbPath);
        ds.setUser("sa");
        return ds;
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
