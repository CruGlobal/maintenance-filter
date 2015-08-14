package org.ccci.maintenance;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.ConfigReader;
import org.ccci.maintenance.util.JdbcMaintenanceWindowDao;
import org.ccci.maintenance.util.MaintenanceWindowDaoFactory;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

public class Bootstrap
{

    private final ServletContext servletContext;
    private final ConfigReader configReader;
    private MaintenanceWindowDaoFactory daoFactory;

    public Bootstrap(ServletContext servletContext)
    {
        this.servletContext = servletContext;
        this.configReader = new ConfigReader(servletContext, System.getProperties());
        this.daoFactory = new MaintenanceWindowDaoFactory(
            configReader,
            new DatasourceManager(configReader));
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
            throw new IllegalStateException("Bootstrap has not yet been created");
        }
    }

    public void init(String filterName)
    {
        MaintenanceServiceImpl maintenanceService = getOrCreateMaintenanceService();
        maintenanceService.addFilterName(filterName);
    }

    private synchronized MaintenanceServiceImpl getOrCreateMaintenanceService()
    {
        String bootstrapLocation = Bootstrap.class.getName();
        Bootstrap bootstrap = (Bootstrap) servletContext.getAttribute(bootstrapLocation);
        MaintenanceServiceImpl maintenanceService;
        if (bootstrap == null)
        {
            maintenanceService = storeThisBootstrapAndCreateMaintenanceService(bootstrapLocation);
        }
        else
        {
            maintenanceService = getMaintenanceServiceImpl();
        }
        return maintenanceService;
    }

    private MaintenanceServiceImpl storeThisBootstrapAndCreateMaintenanceService(String
        bootstrapLocation)
    {
        daoFactory.initialize();
        servletContext.setAttribute(bootstrapLocation, this);

        String key = getKey();
        MaintenanceServiceImpl maintenanceService = new MaintenanceServiceImpl(
            Clock.system(),
            daoFactory.createDao(),
            key);
        servletContext.setAttribute(getMaintenanceServiceLocation(), maintenanceService);
        return maintenanceService;
    }

    private String getKey() {
        String keyParamName = "org.ccci.maintenance.window.key";
        String key = configReader.getParameter(keyParamName);

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
        daoFactory.shutdown();
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
