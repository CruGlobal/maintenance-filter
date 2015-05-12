package org.ccci.maintenance;

import org.ccci.maintenance.util.ConfigReader;
import org.ccci.maintenance.util.Exceptions;
import org.ccci.maintenance.util.Lookups;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Matt Drees
 */
public class DatasourceManager {

    private ConfigReader configReader;
    private H2DatasourcePool pool;

    String datasourceParamName = "org.ccci.maintenance.window.datasource";
    String dbPathParamName = "org.ccci.maintenance.window.db.path";
    String configFileNameParamName = "org.ccci.maintenance.window.db.path.configfile.name";
    String configFilePropertyKeyParamName = "org.ccci.maintenance.window.db.path.configfile.propertykey";

    public DatasourceManager(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public DataSource lookupOrCreateDataSource()
    {

        String datasourceLocation = configReader.getParameter(datasourceParamName);
        if (datasourceLocation != null)
        {
            return Lookups.doLookup(datasourceLocation, DataSource.class);
        }
        else
        {
            String dbPath = configReader.getParameter(dbPathParamName);
            if (dbPath == null)
            {
                dbPath = getDbPathFromConfigFileOrFail();
            }
            pool = new H2DatasourcePool();
            pool.initH2DatabaseLocatedAt(dbPath);
            return pool.getDataSource();
        }
    }

    private String getDbPathFromConfigFileOrFail() {

        String configFile = configReader.getParameter(configFileNameParamName);
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
            String dbPathPropertyKey = configReader.getParameter(configFilePropertyKeyParamName);
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

    public void shutdownPoolIfNecessary()
    {
        if (pool != null)
        {
            pool.dispose();
            pool = null;
        }
    }


}
