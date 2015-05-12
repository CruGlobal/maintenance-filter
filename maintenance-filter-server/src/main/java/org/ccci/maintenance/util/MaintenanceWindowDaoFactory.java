package org.ccci.maintenance.util;

import org.ccci.maintenance.DatabaseMigrator;
import org.ccci.maintenance.DatasourceManager;

import javax.sql.DataSource;

/**
 * @author Matt Drees
 */
public class MaintenanceWindowDaoFactory
{

    private final ConfigReader configReader;
    private DatasourceManager datasourceManager;
    private DataSource dataSource;

    public MaintenanceWindowDaoFactory(
        ConfigReader configReader,
        DatasourceManager datasourceManager)
    {
        this.configReader = configReader;
        this.datasourceManager = datasourceManager;
    }

    public void initialize()
    {
        dataSource = datasourceManager.lookupOrCreateDataSource();
        initDatabaseIfNecessary(dataSource);
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
        return new JdbcMaintenanceWindowDao(dataSource);
    }
}
