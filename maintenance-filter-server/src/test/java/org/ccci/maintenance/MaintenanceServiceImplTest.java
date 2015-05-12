package org.ccci.maintenance;

import static com.atlassian.hamcrest.DeepIsEqual.deeplyEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.InfinispanMaintenanceWindowDao;
import org.ccci.maintenance.util.JdbcMaintenanceWindowDao;
import org.ccci.maintenance.util.MaintenanceWindowDao;
import org.ccci.maintenance.util.OwnedMaintenanceWindow;
import org.h2.jdbcx.JdbcDataSource;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.joda.time.DateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class MaintenanceServiceImplTest
{

    DataSource dataSource;

    Cache<String, OwnedMaintenanceWindow> cache;

    @Mock
    Clock clock;

    @BeforeClass
    public void setupDb()
    {
        MockitoAnnotations.initMocks(this);

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        dataSource = ds;

        new DatabaseMigrator(dataSource).migrate();

        cache = new DefaultCacheManager().getCache();
    }

    @BeforeMethod
    public void setupService() throws SQLException
    {
        clearTable();

        cache.clear();
    }

    private void clearTable() throws SQLException
    {
        //test code is allowed to sloppily deal with exceptions & closing
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeUpdate("delete from MaintenanceWindow");
        connection.close();
    }

    private MaintenanceServiceImpl createService(MaintenanceWindowDao dao)
    {
        MaintenanceServiceImpl service = new MaintenanceServiceImpl(clock, dao, "secrets");
        service.addFilterName(null);
        service.addFilterName("special");
        service.initializationComplete();
        return service;
    }


    @DataProvider
    public Object[][] services()
    {
        return new Object[][]{
            { createService(new JdbcMaintenanceWindowDao(dataSource)) },
            { createService(new InfinispanMaintenanceWindowDao(cache)) }
        };
    }

    @Test(dataProvider = "services")
    public void testCreate(MaintenanceServiceImpl service)
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  " +
        		"Please visit <a href='http://www.youtube.com'>somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));
        
        service.createOrUpdateMaintenanceWindow(null, window);
        
        when(clock.currentDateTime()).thenReturn(new DateTime(2010, 9, 23, 2, 56, 24, 985));
        
        MaintenanceWindow retrievedWindow = service.getActiveMaintenanceWindow(null);
        
        assertThat(retrievedWindow, is(deeplyEqualTo(window)));
        assertThat(service.getActiveMaintenanceWindow("special"), is(nullValue()));
    }

    @Test(dataProvider = "services")
    public void testCreateForNonDefaultFilter(MaintenanceServiceImpl service)
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  " +
        "Please visit <a href='http://www.youtube.com'>somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));

        service.createOrUpdateMaintenanceWindow("special", window);

        when(clock.currentDateTime()).thenReturn(new DateTime(2010, 9, 23, 2, 56, 24, 985));

        MaintenanceWindow retrievedWindow = service.getActiveMaintenanceWindow("special");
        
        assertThat(retrievedWindow, is(deeplyEqualTo(window)));
        assertThat(service.getActiveMaintenanceWindow(null), is(nullValue()));
    }

    @Test(dataProvider = "services")
    public void testUpdate(MaintenanceServiceImpl service)
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  " +
                "Please visit <a href='http://www.youtube.com'>somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));
        
        service.createOrUpdateMaintenanceWindow(null, window);
        
        MaintenanceWindow updatedWindow = new MaintenanceWindow();
        updatedWindow.setId(window.getId());
        updatedWindow.setShortMessage("This site will be unavailable for a bit longer than we thought");
        updatedWindow.setLongMessage(window.getLongMessage() + "  Or go grocery shopping.  It'll be a while.");
        updatedWindow.setBeginAt(window.getBeginAt());
        updatedWindow.setEndAt(window.getEndAt().plusHours(3));
        
        service.createOrUpdateMaintenanceWindow(null, updatedWindow);
        
        when(clock.currentDateTime()).thenReturn(new DateTime(2010, 9, 23, 2, 56, 24, 985));
        
        MaintenanceWindow retrievedWindow = service.getActiveMaintenanceWindow(null);
        
        assertThat(retrievedWindow, is(deeplyEqualTo(updatedWindow)));
    }

    @Test(dataProvider = "services")
    public void testInvalidAuthentication(MaintenanceServiceImpl service)
    {
        assertThat(service.isAuthenticated("pleaseletmein"), is(false));
    }

    @Test(dataProvider = "services")
    public void testInvalidAuthenticationWithClosePassword(MaintenanceServiceImpl service)
    {
        assertThat(service.isAuthenticated("secretz"), is(false));
    }

    @Test(dataProvider = "services")
    public void testValidAuthentication(MaintenanceServiceImpl service)
    {
        assertThat(service.isAuthenticated("secrets"), is(true));
    }
}
