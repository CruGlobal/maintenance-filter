package org.ccci.maintenance.util;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * @author Matt Drees
 */
public class Lookups
{
    public static <T> T doLookup(String location, Class<T> expectedType)
    {
        try
        {
            Object found = new InitialContext().lookup(location);
            if (found == null)
            {
                throw notFound(location, expectedType);
            }
            if (!expectedType.isInstance(found))
            {
                throw new IllegalStateException(String.format(
                    "Found %s bound at %s instead of a %s",
                    found,
                    location,
                    expectedType.getSimpleName()));
            }
            return expectedType.cast(found);
        }
        catch (NameNotFoundException e)
        {
            throw notFound(location, expectedType);
        }
        catch (NamingException e)
        {
            throw Exceptions.wrap(e);
        }
    }

    private static <T> IllegalStateException notFound(String location, Class<T> expectedType)
    {
        return new IllegalStateException(String.format(
            "No %s bound at %s",
            expectedType.getSimpleName(),
            location));
    }

}
