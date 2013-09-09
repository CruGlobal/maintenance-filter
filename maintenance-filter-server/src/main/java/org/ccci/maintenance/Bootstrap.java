package org.ccci.maintenance;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

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
        MaintenanceServiceImpl maintenanceService;
        if (bootstrap == null)
        {
            DataSource dataSource = lookupOrCreateDataSource();
            initDatabaseIfNecessary(dataSource);
            servletContext.setAttribute(bootstrapLocation, this);

            String key = getKey();
            maintenanceService = new MaintenanceServiceImpl(Clock.system(), dataSource, key);
            servletContext.setAttribute(getMaintenanceServiceLocation(), maintenanceService);
        }
        else
        {
            maintenanceService = getMaintenanceServiceImpl();
        }
        maintenanceService.addFilterName(filterName);
    }

    private String getKey() {
        String keyParamName = "org.ccci.maintenance.window.key";
        String key = servletContext.getInitParameter(keyParamName);

        if (key == null)
        {
            throw new IllegalArgumentException(String.format(
                "you must provide provide an authentication key via servlet init parameter %s",
                keyParamName
            ));
        }
        return key;
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

    private DataSource lookupOrCreateDataSource()
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
            {
                dbPath = getDbPathFromConfigFileOrFail(datasourceParamName, dbPathParamName);
            }
            initH2DatasourcePoolLocatedAt(dbPath);
            return pool;
        }
    }

    private String getDbPathFromConfigFileOrFail(String datasourceParamName, String dbPathParamName) {
        String configFileNameParamName = "org.ccci.maintenance.window.db.path.configfile.name";
        String configFilePropertyKeyParamName = "org.ccci.maintenance.window.db.path.configfile.propertykey";

        String configFile = servletContext.getInitParameter(configFileNameParamName);
        if (configFile == null)
        {
            throw new IllegalArgumentException(String.format(
                "you must provide either %s, %s, or %s  as an init-param",
                datasourceParamName,
                dbPathParamName,
                configFileNameParamName
            ));
        }
        else
        {
            String dbPathPropertyKey = servletContext.getInitParameter(configFilePropertyKeyParamName);
            if (dbPathPropertyKey == null)
                throw new IllegalArgumentException(String.format(
                    "you must provide %s when you use %s",
                    configFilePropertyKeyParamName,
                    configFileNameParamName
                ));
            return getDatabasePathFromConfigFile(configFile, dbPathPropertyKey);
        }
    }

    private String getDatabasePathFromConfigFile(String configFile, String dbPathPropertyKey)
    {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream configFileStream = classLoader.getResourceAsStream(configFile);
        if (configFileStream == null)
            throw new IllegalStateException(String.format(
                "Cannot find config file resource '%s' from classloader %s", 
                configFile,
                classLoader));
        Properties configProperties = loadConfigProperties(configFileStream);
        
        String dbPath = configProperties.getProperty(dbPathPropertyKey);
        if (dbPath == null)
            throw new IllegalStateException(String.format(
                "config file '%s' doesn't define a property with key %s", 
                configFile,
                dbPathPropertyKey));
            
        return dbPath;
    }

    private Properties loadConfigProperties(InputStream configFileStream)
    {
        try
        {
            Properties config = new Properties();
            try
            {
                config.load(configFileStream);
            }
            catch (IOException e)
            {
                throw Exceptions.wrap(e);
            }
            return config;
        }
        finally
        {
            closeStreamCarefully(configFileStream);
        }
    }

    private void closeStreamCarefully(InputStream configFileStream)
    {
        try
        {
            configFileStream.close();
        }
        catch (IOException e)
        {
            Exceptions.swallow(e, "exception closing config stream");
        }
    }

    private void initH2DatasourcePoolLocatedAt(String dbPath)
    {
        /*
         * use a file-based database, since this must persist across jvm restarts.
         */
        String url = "jdbc:h2:file:" + dbPath;
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

    public MaintenanceService getMaintenanceService()
    {
        return getMaintenanceServiceImpl();
    }

    private MaintenanceServiceImpl getMaintenanceServiceImpl() {
        return (MaintenanceServiceImpl) servletContext.getAttribute(getMaintenanceServiceLocation());
    }


    private String getMaintenanceServiceLocation()
    {
        return MaintenanceService.class.getName();
    }

    public MaintenanceService getInitializedMaintenanceService() {
        MaintenanceServiceImpl maintenanceService = getMaintenanceServiceImpl();
        maintenanceService.initializationComplete();
        return maintenanceService;
    }
}
