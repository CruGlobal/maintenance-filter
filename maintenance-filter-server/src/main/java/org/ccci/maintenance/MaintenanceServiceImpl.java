package org.ccci.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.Exceptions;
import org.ccci.maintenance.util.JdbcUtils;
import org.ccci.maintenance.util.Objects;
import org.ccci.maintenance.util.Preconditions;
import org.ccci.maintenance.util.Strings;
import org.ccci.maintenance.util.TimeUtil;
import org.joda.time.DateTime;


//TODO: make this jdbc code nice, create or use a helper framework.
public class MaintenanceServiceImpl implements MaintenanceService
{
    
    private final Clock clock;
    private final DataSource dataSource;
    private String key;
    private Set<String> filterNames = new HashSet<String>(5);

    public MaintenanceServiceImpl(Clock clock, DataSource dataSource, String key)
    {
        this.clock = clock;
        this.dataSource = dataSource;
        this.key = key;
    }

    public MaintenanceWindow getActiveMaintenanceWindow(String filterName)
    {
        if (!filterExists(filterName))
            throw new IllegalStateException("invalid filterName: " + filterName);

        Connection connection;
        try
        {
            connection = dataSource.getConnection();
            try
            {
                return getCurrentWindowWithConnection(connection, filterName);
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

    private MaintenanceWindow getCurrentWindowWithConnection(Connection connnection, String filterName) throws SQLException
    {
        String sql = "select * from MaintenanceWindow where " +
        		"(beginAt < ? or beginAt is null) and " +
        		"(endAt > ? or endAt is null) " + buildFilterNameClause(filterName);
        PreparedStatement statement = connnection.prepareStatement(sql);
        try
        {
            return getCurrentWindowWithStatement(statement, filterName);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private String buildFilterNameClause(String filterName)
    {
        if (filterName == null)
            return " and (filterName is null)";
        else
            return " and (filterName = ?)"; 
    }

    private MaintenanceWindow getCurrentWindowWithStatement(PreparedStatement statement, String filterName) throws SQLException
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
    
    public void createOrUpdateMaintenanceWindow(String filterName, MaintenanceWindow window)
    {
        Connection connection;
        try
        {
            connection = dataSource.getConnection();
            try
            {
                createOrUpdateMaintenanceWindowWithConnection(connection, window, filterName);
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

    private void createOrUpdateMaintenanceWindowWithConnection(Connection connnection, MaintenanceWindow window, String filterName) throws SQLException
    {
        /* don't really need to deal with transactions here; we're simple enough to get by with autocommit */
        connnection.setAutoCommit(true);
        
        if (! updateMaintenanceWindowWithConnection(connnection, window, filterName))
        {
            createMaintenanceWindowWithConnection(connnection, window, filterName);
        }
    }

    /** returns true if the row existed and was updated, false otherwise */
    private boolean updateMaintenanceWindowWithConnection(Connection connection, MaintenanceWindow window, String filterName) throws SQLException
    {
        checkIdNotInUseByAnotherFilter(connection, window.getId(), filterName);
        
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

    private void checkIdNotInUseByAnotherFilter(Connection connection, String id, String filterName) throws SQLException
    {
        String sql = "select filterName from MaintenanceWindow where id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        try
        {
            checkIdNotInUseByAnotherFilterWithStatement(statement, id, filterName);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private void checkIdNotInUseByAnotherFilterWithStatement(PreparedStatement statement, String id, String filterName) throws SQLException
    {
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        try
        {
            checkIdNotInUseByAnotherFilterWithResultSet(resultSet, id, filterName);
        }
        finally
        {
            JdbcUtils.close(resultSet);
        }
    }

    private void checkIdNotInUseByAnotherFilterWithResultSet(ResultSet resultSet, String id, String targetFilterName) throws SQLException
    {
        if (resultSet.next())
        {
            String otherfilterName = resultSet.getString("filterName");
            Preconditions.checkArgument(Objects.equal(otherfilterName, targetFilterName),
                "the '%s' maintenance window is owned by %s, not by %s",
                id,
                getFilterDescription(otherfilterName),
                getFilterDescription(targetFilterName));
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
    
    private void createMaintenanceWindowWithConnection(Connection connnection, MaintenanceWindow window, String filterName) throws SQLException
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
            createMaintenanceWindowWithStatement(statement, window, filterName);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private void createMaintenanceWindowWithStatement(PreparedStatement statement, MaintenanceWindow window, String filterName) throws SQLException
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

    public boolean filterExists(String filterName) {
        return filterNames.contains(Strings.nullToEmpty(filterName));
    }

    // see http://codahale.com/a-lesson-in-timing-attacks/
    private boolean isEqualInConstantTime(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        boolean result = true;
        for (int i = 0; i < a.length(); i++) {
            result = result & (a.charAt(i) == b.charAt(i));
        }
        return result;
    }

    public void addFilterName(String filterName) {
        filterNames.add(Strings.nullToEmpty(filterName));
    }

    public void initializationComplete() {
        //If/when I depend on guava, use ImmutableSet instead
        filterNames = Collections.unmodifiableSet(filterNames);
    }
}
