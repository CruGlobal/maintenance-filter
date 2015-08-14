package org.ccci.maintenance.util;

import org.ccci.maintenance.DatabaseMigrator;
import org.ccci.maintenance.DatasourceManager;

import javax.sql.DataSource;

/**
 * @author Matt Drees
 */
public class MaintenanceWindowDaoFactory
{

    private static final String INFINISPAN_LOCATION_PARAM_NAME =
        "org.ccci.maintenance.window.infinispan.cache.location";

    private final ConfigReader configReader;
    private DatasourceManager datasourceManager;
    private DataSource dataSource;
    private Object cacheContainer;

    public MaintenanceWindowDaoFactory(
        ConfigReader configReader,
        DatasourceManager datasourceManager)
    {
        this.configReader = configReader;
        this.datasourceManager = datasourceManager;
    }

    public void initialize()
    {
        String location = configReader.getParameter(INFINISPAN_LOCATION_PARAM_NAME);
        if (location == null)
        {
            dataSource = datasourceManager.lookupOrCreateDataSource();
            initDatabaseIfNecessary(dataSource);
        }
        else
        {
            //avoid a hard dependency on infinispan, by not using Cache.class
            Class<?> cacheContainerClass = getInfinispanCacheContainerClass();
            cacheContainer = Lookups.doLookup(location, cacheContainerClass);
        }
    }

    private Class<?> getInfinispanCacheContainerClass()
    {
        try
        {
            return Class.forName("org.infinispan.manager.CacheContainer");
        }
        catch (ClassNotFoundException e)
        {
            throw Exceptions.wrap(e);
        }
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
            return new InfinispanMaintenanceWindowDao(cacheContainer);
        }
    }
}
