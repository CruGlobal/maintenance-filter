package org.ccci.maintenance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Matt Drees
 */
public class PathParser {

    private static final String ACTION_OR_FILTER_NAME = "[\\w\\-+]+";
    private static Pattern pattern = Pattern.compile("/(" + ACTION_OR_FILTER_NAME + ")(/(" + ACTION_OR_FILTER_NAME + "))?");

    Matcher matcher;


    /**
     * Intended to parse a path that is either just an action,
     * or a named filter and an action.
     *
     * Examples:
     * <ul>
     *   <li>
     *      /updateWindow
     *   </li>
     *   <li>
     *      /hrDatabase/updateWindow
     *   </li>
     * </ul>
     */
    public void parse(String pathInfo)
    {
        if (pathInfo == null)
            return;
        matcher = pattern.matcher(pathInfo);
        matcher.matches();
    }

    public boolean isValid() {
        return matcher != null && matcher.matches();
    }

    public String getAction() {
        if (matcher.group(2) == null)
            return matcher.group(1);
        else
            return matcher.group(3);
    }

    public String getFilterName() {
        if (matcher.group(2) == null)
            return null;
        else
            return matcher.group(1);
    }

    public boolean isValidFilterName(String name)
    {
        return name.matches(ACTION_OR_FILTER_NAME);
    }
}
