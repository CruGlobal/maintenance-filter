package org.ccci.maintenance.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.jdbcx.JdbcDataSource;

public class JdbcUtils
{

    public static void close(Connection con)
    {
        try
        {
            con.close();
        }
        catch (SQLException e)
        {
            Exceptions.swallow(e, "exception while closing connection %s", con);
        }
    }

    public static void close(Statement statement)
    {
        try
        {
            statement.close();
        }
        catch (SQLException e)
        {
            Exceptions.swallow(e, "exception while closing statement %s", statement);
        }
    }

    public static void close(ResultSet resultSet)
    {

        try
        {
            resultSet.close();
        }
        catch (SQLException e)
        {
            Exceptions.swallow(e, "exception while closing result set %s", resultSet);
        }
    }

    public static void executeUpdate(JdbcDataSource dataSource, String sql) throws SQLException
    {
        Connection connection = dataSource.getConnection();
        try
        {
            Statement statement = connection.createStatement();
            try
            {
                statement.executeUpdate(sql);
            }
            finally
            {
                close(statement);
            }
        }
        finally
        {
            close(connection);
        }
    }

}
