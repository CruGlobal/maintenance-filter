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
import org.ccci.maintenance.util.Objects;
import org.ccci.maintenance.util.Preconditions;
import org.ccci.maintenance.util.TimeUtil;
import org.joda.time.DateTime;


//TODO: make this jdbc code nice, create or use a helper framework.
public class MaintenanceServiceImpl implements MaintenanceService
{
    
    private final Clock clock;
    private final DataSource dataSource;
    private final String filterName;
    private String key;

    public MaintenanceServiceImpl(Clock clock, DataSource dataSource, String filterName, String key)
    {
        this.clock = clock;
        this.dataSource = dataSource;
        this.filterName = filterName;
        this.key = key;
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
        		"(endAt > ? or endAt is null) " + buildFilterNameClause();
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

    private String buildFilterNameClause()
    {
        if (filterName == null)
            return " and (filterName is null)";
        else
            return " and (filterName = ?)"; 
    }

    private MaintenanceWindow getCurrentWindowWithStatement(PreparedStatement statement) throws SQLException
    {
        DateTime currentDateTime = clock.currentDateTime();
        Timestamp timestamp = TimeUtil.dateTimeToSqlTimestamp(currentDateTime);
        ResultSet resultSet;
        statement.setTimestamp(1, timestamp);
        statement.setTimestamp(2, timestamp);
        if (filterName != null)
        {
            statement.setString(3, filterName);
        }
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
    private boolean updateMaintenanceWindowWithConnection(Connection connection, MaintenanceWindow window) throws SQLException
    {
        checkIdNotInUseByAnotherFilter(connection, window.getId());
        
        String sql = "update MaintenanceWindow " +
        		"set " +
        		"shortMessage = ?, " +
        		"longMessage = ?, " +
        		"beginAt = ?, " +
        		"endAt = ? " +
                "where id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        try
        {
            return updateMaintenanceWindowWithStatement(statement, window);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private void checkIdNotInUseByAnotherFilter(Connection connection, String id) throws SQLException
    {
        String sql = "select filterName from MaintenanceWindow where id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        try
        {
            checkIdNotInUseByAnotherFilterWithStatement(statement, id);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private void checkIdNotInUseByAnotherFilterWithStatement(PreparedStatement statement, String id) throws SQLException
    {
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        try
        {
            checkIdNotInUseByAnotherFilterWithResultSet(resultSet, id);
        }
        finally
        {
            JdbcUtils.close(resultSet);
        }
    }

    private void checkIdNotInUseByAnotherFilterWithResultSet(ResultSet resultSet, String id) throws SQLException
    {
        if (resultSet.next())
        {
            String filterName = resultSet.getString("filterName");
            Preconditions.checkArgument(Objects.equal(filterName, this.filterName),
                "the '%s' maintenance window is owned by %s, not by %s",
                id,
                getFilterDescription(filterName),
                getFilterDescription(this.filterName));
            Preconditions.checkState(!resultSet.next(), "More than one window exists for id " + id);
        }
    }

    private String getFilterDescription(String filterName)
    {
        return filterName == null ? "the default filter" : "the " + filterName + " filter";
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
                "id, " +
                "filterName" +
                ") values (" +
                "?, ?, ?, ?, ?, ?" +
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
        statement.setString(6, filterName);
        int rowsUpdated = statement.executeUpdate();
        if (rowsUpdated != 1)
            throw new IllegalStateException("Exactly one row was not inserted, as expected; row count: " + rowsUpdated + ", id: " + window.getId());
    }

    public boolean isAuthenticated(String key) {
        return isEqualInConstantTime(key, this.key);
    }

    // see http://codahale.com/a-lesson-in-timing-attacks/
    private boolean isEqualInConstantTime(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        boolean result = true;
        for (int i = 0; i < a.length(); i++) {
            result = result && (a.charAt(i) == b.charAt(i));
        }
        return result;
    }


}
