package org.ccci.maintenance.util;

import org.ccci.maintenance.MaintenanceWindow;
import org.joda.time.DateTime;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author Matt Drees
 */
public class JdbcMaintenanceWindowDao implements MaintenanceWindowDao
{
    private final DataSource dataSource;

    public JdbcMaintenanceWindowDao(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public MaintenanceWindow getActiveMaintenanceWindow(
        String filterName,
        DateTime currentDateTime)
    {
        Connection connection;
        try
        {
            connection = dataSource.getConnection();
            try
            {
                return getCurrentWindowWithConnection(connection, filterName, currentDateTime);
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

    private MaintenanceWindow getCurrentWindowWithConnection(
        Connection connection,
        String filterName,
        DateTime currentDateTime) throws SQLException
    {
        String sql = "select * from MaintenanceWindow where " +
                     "(beginAt < ? or beginAt is null) and " +
                     "(endAt > ? or endAt is null) " + buildFilterNameClause(filterName);
        PreparedStatement statement = connection.prepareStatement(sql);
        try
        {
            return getCurrentWindowWithStatement(statement, filterName, currentDateTime);
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

    private MaintenanceWindow getCurrentWindowWithStatement(
        PreparedStatement statement,
        String filterName, DateTime currentDateTime) throws SQLException
    {
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

    public OwnedMaintenanceWindow getMaintenanceWindowById(String id)
    {

        Connection connection;
        try
        {
            connection = dataSource.getConnection();
            try
            {
                return getMaintenanceWindowByIdWithConnection(id, connection);
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

    private OwnedMaintenanceWindow getMaintenanceWindowByIdWithConnection(
        String id,
        Connection connection)
        throws SQLException
    {
        String sql = "select * from MaintenanceWindow where id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        try
        {
            return getMaintenanceWindowByIdWithStatement(statement, id);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private OwnedMaintenanceWindow getMaintenanceWindowByIdWithStatement(
        PreparedStatement statement,
        String id) throws SQLException
    {
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        try
        {
            if (!resultSet.next())
            {
                return null;
            }
            else
            {
                MaintenanceWindow window = buildWindow(resultSet);
                String owner = resultSet.getString("filterName");
                OwnedMaintenanceWindow ownedMaintenanceWindow =
                    new OwnedMaintenanceWindow(window, owner);

                Preconditions.checkState(
                    !resultSet.next(),
                    "More than one window exists for id " + id);
                return ownedMaintenanceWindow;
            }
        }
        finally
        {
            JdbcUtils.close(resultSet);
        }
    }


    public void createMaintenanceWindow(OwnedMaintenanceWindow window)
    {
        Connection connection;
        try
        {
            connection = connectAndSetAutoCommit();

            try
            {
                createMaintenanceWindowWithConnection(connection, window);
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

    private void createMaintenanceWindowWithConnection(
        Connection connection,
        OwnedMaintenanceWindow window) throws SQLException
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
        PreparedStatement statement = connection.prepareStatement(sql);
        try
        {
            createMaintenanceWindowWithStatement(statement, window);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private void createMaintenanceWindowWithStatement(
        PreparedStatement statement,
        OwnedMaintenanceWindow ownedMaintenanceWindow) throws SQLException
    {
        String filterName = ownedMaintenanceWindow.owner;
        MaintenanceWindow window = ownedMaintenanceWindow.window;
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

    private Connection connectAndSetAutoCommit() throws SQLException
    {
        Connection connection = dataSource.getConnection();

        /* don't really need to deal with transactions here;
         * we're simple enough to get by with autocommit
         */
        connection.setAutoCommit(true);
        return connection;
    }

    public void updateMaintenanceWindow(OwnedMaintenanceWindow window)
    {
        Connection connection;
        try
        {
            connection = connectAndSetAutoCommit();
            try
            {
                updateMaintenanceWindowWithConnection(connection, window);
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

    private void updateMaintenanceWindowWithConnection(
        Connection connection,
        OwnedMaintenanceWindow window) throws SQLException
    {

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
            updateMaintenanceWindowWithStatement(statement, window);
        }
        finally
        {
            JdbcUtils.close(statement);
        }
    }

    private boolean updateMaintenanceWindowWithStatement(
        PreparedStatement statement,
        OwnedMaintenanceWindow ownedMaintenanceWindow) throws SQLException
    {
        MaintenanceWindow window = ownedMaintenanceWindow.window;
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

}
