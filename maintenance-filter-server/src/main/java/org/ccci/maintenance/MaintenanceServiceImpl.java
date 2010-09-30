package org.ccci.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.Exceptions;
import org.ccci.maintenance.util.JdbcUtils;
import org.ccci.maintenance.util.TimeUtil;
import org.joda.time.DateTime;

//TODO: make this jdbc code nice, create or use a helper framework.
public class MaintenanceServiceImpl implements MaintenanceService
{
    
    private final Clock clock;
    private final DataSource dataSource;
    
    public MaintenanceServiceImpl(Clock clock, DataSource dataSource)
    {
        this.clock = clock;
        this.dataSource = dataSource;
    }

    public MaintenanceWindow getActiveMaintenanceWindow()
    {
        Connection connnection;
        try
        {
            connnection = dataSource.getConnection();
            try
            {
                return getCurrentWindowWithConnection(connnection);
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

    private MaintenanceWindow getCurrentWindowWithConnection(Connection connnection) throws SQLException
    {
        String sql = "select * from MaintenanceWindow where " +
        		"(beginAt < ? or beginAt is null) and " +
        		"(endAt > ? or endAt is null) ";
        PreparedStatement statement = connnection.prepareStatement(sql);
        try
        {
            return getCurrentWindowWithStatement(statement);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private MaintenanceWindow getCurrentWindowWithStatement(PreparedStatement statement) throws SQLException
    {
        DateTime currentDateTime = clock.currentDateTime();
        Timestamp timestamp = TimeUtil.dateTimeToSqlTimestamp(currentDateTime);
        ResultSet resultSet;
        statement.setTimestamp(1, timestamp);
        statement.setTimestamp(2, timestamp);
        resultSet = statement.executeQuery();
        try
        {
            return getCurrentWindowWithResultSet(resultSet);
        }
        finally
        {
            JdbcUtils.close(resultSet);
        }
    }
    
    private MaintenanceWindow getCurrentWindowWithResultSet(ResultSet resultSet) throws SQLException
    {
        if (!resultSet.next())
        {
            return null;
        }
        MaintenanceWindow window = buildWindow(resultSet);
        if (resultSet.next())
        {
            throw new IllegalStateException("More than one window are currently active!");
        }
        return window;
    }

    private MaintenanceWindow buildWindow(ResultSet resultSet) throws SQLException
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId(resultSet.getString("id"));
        window.setShortMessage(resultSet.getString("shortMessage"));
        window.setLongMessage(resultSet.getString("longMessage"));
        window.setBeginAt(TimeUtil.sqlTimestampToDateTime(resultSet.getTimestamp("beginAt")));
        window.setEndAt(TimeUtil.sqlTimestampToDateTime(resultSet.getTimestamp("endAt")));
        return window;
    }
    
    public void createOrUpdateMaintenanceWindow(MaintenanceWindow window)
    {
        Connection connnection;
        try
        {
            connnection = dataSource.getConnection();
            try
            {
                createOrUpdateMaintenanceWindowWithConnection(connnection, window);
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

    private void createOrUpdateMaintenanceWindowWithConnection(Connection connnection, MaintenanceWindow window) throws SQLException
    {
        /* don't really need to deal with transactions here; we're simple enough to get by with autocommit */
        connnection.setAutoCommit(true);
        
        if (! updateMaintenanceWindowWithConnection(connnection, window))
        {
            createMaintenanceWindowWithConnection(connnection, window);
        }
    }

    /** returns true if the row existed and was updated, false otherwise */
    private boolean updateMaintenanceWindowWithConnection(Connection connnection, MaintenanceWindow window) throws SQLException
    {
        String sql = "update MaintenanceWindow " +
        		"set " +
        		"shortMessage = ?, " +
        		"longMessage = ?, " +
        		"beginAt = ?, " +
        		"endAt = ? " +
                "where id = ?";
        PreparedStatement statement = connnection.prepareStatement(sql);
        try
        {
            return updateMaintenanceWindowWithStatement(statement, window);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private boolean updateMaintenanceWindowWithStatement(PreparedStatement statement, MaintenanceWindow window) throws SQLException
    {
        statement.setString(1, window.getShortMessage());
        statement.setString(2, window.getLongMessage());
        statement.setTimestamp(3, TimeUtil.dateTimeToSqlTimestamp(window.getBeginAt()));
        statement.setTimestamp(4, TimeUtil.dateTimeToSqlTimestamp(window.getEndAt()));
        statement.setString(5, window.getId());
        int rowsUpdated = statement.executeUpdate();
        if (rowsUpdated > 1)
            throw new IllegalStateException("More than one row updated by update call for id: " + window.getId());
        assert rowsUpdated >= 0;
        return rowsUpdated == 1;
    }
    
    private void createMaintenanceWindowWithConnection(Connection connnection, MaintenanceWindow window) throws SQLException
    {
        String sql = "insert into MaintenanceWindow (" +
                "shortMessage, " +
                "longMessage, " +
                "beginAt, " +
                "endAt, " +
                "id" +
                ") values (" +
                "?, ?, ?, ?, ?" +
                ")";
        PreparedStatement statement = connnection.prepareStatement(sql);
        try
        {
            createMaintenanceWindowWithStatement(statement, window);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private void createMaintenanceWindowWithStatement(PreparedStatement statement, MaintenanceWindow window) throws SQLException
    {
        statement.setString(1, window.getShortMessage());
        statement.setString(2, window.getLongMessage());
        statement.setTimestamp(3, TimeUtil.dateTimeToSqlTimestamp(window.getBeginAt()));
        statement.setTimestamp(4, TimeUtil.dateTimeToSqlTimestamp(window.getEndAt()));
        statement.setString(5, window.getId());
        int rowsUpdated = statement.executeUpdate();
        if (rowsUpdated != 1)
            throw new IllegalStateException("Exactly one row was not inserted, as expected; row count: " + rowsUpdated + ", id: " + window.getId());
    }
    
}
