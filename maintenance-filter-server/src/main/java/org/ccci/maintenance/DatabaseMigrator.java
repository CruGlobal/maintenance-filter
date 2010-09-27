package org.ccci.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.ccci.maintenance.util.Exceptions;
import org.ccci.maintenance.util.JdbcUtils;

public class DatabaseMigrator
{
    private final DataSource dataSource;

    public DatabaseMigrator(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void migrate()
    {
        Connection connnection;
        try
        {
            connnection = dataSource.getConnection();
            try
            {
                migrateConnection(connnection);
            }
            finally
            {
                JdbcUtils.close(connnection);
            }
        }
        catch (SQLException e)
        {
            throw Exceptions.wrap(e);
        }
    }

    private void migrateConnection(Connection connnection) throws SQLException
    {
        String sql = "create table if not exists MaintenanceWindow (" +
                "id varchar(50) primary key, " +
                "shortMessage varchar(200) not null, " +
                "longMessage varchar(2000) not null, " +
                "beginAt timestamp, " +
                "endAt timestamp " +
                ")";
        PreparedStatement statement = connnection.prepareStatement(sql);
        try
        {
            statement.executeUpdate();
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }
    
}
