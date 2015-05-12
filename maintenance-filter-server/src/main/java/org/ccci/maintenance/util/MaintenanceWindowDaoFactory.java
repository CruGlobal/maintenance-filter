package org.ccci.maintenance.util;

import org.ccci.maintenance.DatabaseMigrator;
import org.ccci.maintenance.DatasourceManager;

import javax.sql.DataSource;

/**
 * @author Matt Drees
 */
public class MaintenanceWindowDaoFactory
{

    String infinispanLocationParamName = "org.ccci.maintenance.window.infinispan.cache.location";

    private final ConfigReader configReader;
    private DatasourceManager datasourceManager;
    private DataSource dataSource;
    private Object cache;

    public MaintenanceWindowDaoFactory(
        ConfigReader configReader,
        DatasourceManager datasourceManager)
    {
        this.configReader = configReader;
        this.datasourceManager = datasourceManager;
    }

    public void initialize()
    {
        String location = configReader.getParameter(infinispanLocationParamName);
        if (location == null)
        {
            dataSource = datasourceManager.lookupOrCreateDataSource();
            initDatabaseIfNecessary(dataSource);
        }
        else
        {
            //avoid a hard dependency on infinispan, by not using Cache.class
            Class<?> cacheClass = getInfinispanCacheClass();
            cache = Lookups.doLookup(location, cacheClass);
        }
    }

    private Class<?> getInfinispanCacheClass()
    {
        Class<?> cacheClass;
        try
        {
            cacheClass = Class.forName("org.infinispan.Cache");
        }
        catch (ClassNotFoundException e)
        {
            throw Exceptions.wrap(e);
        }
        return cacheClass;
    }

    private void initDatabaseIfNecessary(DataSource dataSource)
    {
        new DatabaseMigrator(dataSource).migrate();
    }


    public void shutdown()
    {
        datasourceManager.shutdownPoolIfNecessary();
    }

    public MaintenanceWindowDao createDao()
    {
        if (dataSource != null)
        {
            return new JdbcMaintenanceWindowDao(dataSource);
        }
        else
        {
            return new InfinispanMaintenanceWindowDao(cache);
        }
    }
}
