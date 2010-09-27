package org.ccci.maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static com.atlassian.hamcrest.DeepIsEqual.deeplyEqualTo;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.h2.jdbcx.JdbcDataSource;
import org.joda.time.DateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class MaintenanceServiceImplTest
{

    MaintenanceServiceImpl service;
    
    DataSource dataSource;

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
    }
    
    @BeforeMethod
    public void setupService() throws SQLException
    {
        service = new MaintenanceServiceImpl(clock, dataSource);
        clearTable();
    }
    
    private void clearTable() throws SQLException
    {
        //test code is allowed to sloppily deal with exceptions & closing 
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeUpdate("delete from MaintenanceWindow");
        connection.close();
    }

    @Test
    public void testCreate()
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  " +
        		"Please visit <a href='http://www.youtube.com'>somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));
        
        service.createOrUpdateMaintenanceWindow(window);
        
        when(clock.currentDateTime()).thenReturn(new DateTime(2010, 9, 23, 2, 56, 24, 985));
        
        MaintenanceWindow retrievedWindow = service.getCurrentWindow();
        
        assertThat(retrievedWindow, is(deeplyEqualTo(window)));
    }

    @Test
    public void testUpdate()
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  " +
                "Please visit <a href='http://www.youtube.com'>somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));
        
        service.createOrUpdateMaintenanceWindow(window);
        
        MaintenanceWindow updatedWindow = new MaintenanceWindow();
        updatedWindow.setId(window.getId());
        updatedWindow.setShortMessage("This site will be unavailable for a bit longer than we thought");
        updatedWindow.setLongMessage(window.getLongMessage() + "  Or go grocery shopping.  It'll be a while.");
        updatedWindow.setBeginAt(window.getBeginAt());
        updatedWindow.setEndAt(window.getEndAt().plusHours(3));
        
        service.createOrUpdateMaintenanceWindow(updatedWindow);
        
        when(clock.currentDateTime()).thenReturn(new DateTime(2010, 9, 23, 2, 56, 24, 985));
        
        MaintenanceWindow retrievedWindow = service.getCurrentWindow();
        
        assertThat(retrievedWindow, is(deeplyEqualTo(updatedWindow)));
    }
    
}
