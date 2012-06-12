package org.ccci.maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.sql.SQLException;

import org.ccci.maintenance.util.JdbcUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DatabaseMigratorTest
{


    private JdbcDataSource dataSource;

    @BeforeClass
    public void setupDb() throws SQLException
    {
        MockitoAnnotations.initMocks(this);
        
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        dataSource = ds;
        JdbcUtils.executeUpdate(dataSource, "drop all objects");
    }
    
    @Test
    public void testMigrationFromDatabasePriorToMigrationTable() throws SQLException
    {
        String sql = "create table if not exists MaintenanceWindow (" +
        "id varchar(50) primary key, " +
        "shortMessage varchar(200) not null, " +
        "longMessage varchar(2000) not null, " +
        "beginAt timestamp, " +
        "endAt timestamp " +
        ")";
        JdbcUtils.executeUpdate(dataSource, sql);
        
        DatabaseMigrator migrator = new DatabaseMigrator(dataSource);
        migrator.migrate();
        
        assertThat(migrator.getVersion(), is(migrator.getTargetVersion()));
    }
    
    @Test
    public void testMigrationWithEmptyDatabase() throws SQLException
    {
        DatabaseMigrator migrator = new DatabaseMigrator(dataSource);
        migrator.migrate();
        
        assertThat(migrator.getVersion(), is(migrator.getTargetVersion()));
    }
    
    @Test
    public void testMigrationWithUpToDateDatabase() throws SQLException
    {
        DatabaseMigrator migrator = new DatabaseMigrator(dataSource);
        migrator.migrate();
        
        migrator.migrate();
        assertThat(migrator.getVersion(), is(migrator.getTargetVersion()));
    }
}
