package org.ccci.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
        Connection connection;
        try
        {
            connection = dataSource.getConnection();
            try
            {
                new Migration(connection).migrate();
            }
            finally
            {
                JdbcUtils.close(connection);
            }
        }
        catch (SQLException e)
        {
            throw Exceptions.wrap(e);
        }
    }

    public long getVersion()
    {
        Connection connection;
        try
        {
            connection = dataSource.getConnection();
            try
            {
                return new Migration(connection).getCurrentVersion();
            }
            finally
            {
                JdbcUtils.close(connection);
            }
        }
        catch (SQLException e)
        {
            throw Exceptions.wrap(e);
        }
    }

    public long getTargetVersion()
    {
        return 2;
    }
    
    class Migration
    {
        private final Connection connection;

        public Migration(Connection connection)
        {
            this.connection = connection;
        }

        void migrate() throws SQLException
        {
            createVersionTableIfNecessary();
            long version = getCurrentVersion();
            if (version < 1)
            {
                createWindowTable();
                setVersion(1);
            }
            if (version < 2)
            {
                addNameColumn();
                setVersion(2);
            }
        }

        private long getCurrentVersion() throws SQLException
        {
            Statement statement = connection.createStatement();
            try
            {
                String sql = "select version from DatabaseVersion where id = 1";
                ResultSet resultSet = statement.executeQuery(sql);
                if (!resultSet.next())
                {
                    throw new IllegalStateException("No DatabaseVersion row with id = 1!");
                }
                return resultSet.getLong("version");
            }
            finally
            {
                JdbcUtils.close(statement);
            }
        }

        private void setVersion(int i) throws SQLException
        {
            int updateCount = executeUpdate("update DatabaseVersion set version = " + i + " where id = 1");
            if (updateCount != 1) 
                throw new IllegalStateException("version did not update exactly one row; updated " + updateCount);
        }

        private void createVersionTableIfNecessary() throws SQLException
        {
            String sql = "create table if not exists DatabaseVersion (" +
                "id varchar(50) primary key, " +
                "version bigint not null " +
                ")";
            executeUpdate(sql);
            
            insertRowIntoVersionTableIfNecessary();
        }

        private void insertRowIntoVersionTableIfNecessary() throws SQLException
        {
            String sql = "select id from DatabaseVersion where id = 1";
            Statement statement = connection.createStatement();
            try
            {
                ResultSet resultSet = statement.executeQuery(sql);
                if (!resultSet.next())
                {
                    insertRowIntoVersionTable();
                }
                resultSet.close();
            }
            finally
            {
                JdbcUtils.close(statement);
            }
        }

        private void insertRowIntoVersionTable() throws SQLException
        {
            executeUpdate("insert into DatabaseVersion(id, version) values (1, 0)");
        }

        /*
         * Note: this migration needs to work even if the table exists aleady, because the version
         * table was not introduced into the codebase until later, and some window databases out there
         * have this table but will be considered version 0.
         */
        void createWindowTable() throws SQLException
        {
            String sql = "create table if not exists MaintenanceWindow (" +
                "id varchar(50) primary key, " +
                "shortMessage varchar(200) not null, " +
                "longMessage varchar(2000) not null, " +
                "beginAt timestamp, " +
                "endAt timestamp " +
                ")";
            executeUpdate(sql);
        }
        
        private void addNameColumn() throws SQLException
        {
            executeUpdate("alter table MaintenanceWindow add column filterName varchar(200) ");
        }

        private int executeUpdate(String sql) throws SQLException
        {
            PreparedStatement statement = connection.prepareStatement(sql);
            try
            {
                return statement.executeUpdate();
            }
            finally
            {
                JdbcUtils.close(statement);
            }
        }
    }


}
