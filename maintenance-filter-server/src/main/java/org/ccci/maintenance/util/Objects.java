package org.ccci.maintenance.util;

/**
 * @author Matt Drees
 */
public class Objects {

    public static boolean equal(Object first, Object second) {
        return first == second ||
            (first != null && second != null && first.equals(second));
    }
}
