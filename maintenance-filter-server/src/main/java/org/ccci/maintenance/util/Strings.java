package org.ccci.maintenance.util;

/**
 * @author Matt Drees
 */
public class Strings {

    public static String nullToEmpty(String in)
    {
        if (in == null)
            return "";
        else
            return in;
    }

}
