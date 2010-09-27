package org.ccci.maintenance.util;

/**
 * A set of precondition methods.  These are all are intended to be used to check parameters.  For internal-state-checking methods, you could
 * use Google Collection's Preconditions class.
 * 
 * See {@link #checkArgument(boolean, String, Object...)} for typical usage example.
 * 
 * All parameters to each method are required.
 * 
 * @author Matt Drees
 *
 */
//TODO: copy/pasted from Util project
public class Preconditions
{

    /**
     * Verifies that the given reference is not null. If the given string is null, an {@link NullPointerException}
     * is thrown with an exception message generated from calling {@link String#format(String, Object...)} on the
     * given {@code message} and {@code parameters}.
     * 
     * @param string the string to check
     * @param message a format string
     * @param parameters parameter values for the format string
     * @return the given {@code reference} to make inline checks possible.
     */
    public static <T> T checkNotNull(T reference, String message, Object... parameters)
    {
        if (reference == null) throw new NullPointerException(String.format(message, parameters));
        return reference;
    }


    /**
     * Verifies that the given string is not null and is not an empty string.
     * If the given string is null, a {@link NullPointerException} is thrown.
     * If the given string is empty, an {@link IllegalArgumentException} is thrown.  
     * In either case, an appropriate exception message based on {@code referenceName} is generated.
     * 
     * @param string the string to check
     * @param referenceName should refer to the name of the parameter being checked
     * @return the given string to make inline checks possible.
     */
    public static String checkNotNullOrEmpty(String string, String referenceName)
    {
        checkReferenceName(referenceName);
        checkNotNull(string, "%s is null", referenceName);
        if (string.length() == 0) illegalArg("%s is empty", referenceName);
        return string;
    }

    private static void illegalArg(String message, Object... parameters)
    {
        throw new IllegalArgumentException(String.format(message, parameters));
    }


    /**
     * A shortcut for performing both {@link #checkArgumentMaxLength(String, int, String)} and {@link #checkArgumentMaxLength(String, int, String)}. 
     * See those docs for details.
     * 
     * @param string the string to check
     * @param minLength
     * @param maxLength
     * @param referenceName should refer to the name of the parameter being checked
     * @return the given {@code string} to make inline checks possible
     */
    public static String checkArgumentSize(String string, int minLength, int maxLength, String referenceName)
    {
        checkArgumentMinLength(string, minLength, referenceName);
        checkArgumentMaxLength(string, maxLength, referenceName);
        return string;
    }

    /**
     * Verifies that the given string is at least {@code minLength} characters long.
     * If the given string is too short, an {@link IllegalArgumentException} is thrown whose message is based on {@code referenceName}.
     * 
     * @param string the string to check
     * @param minLength 
     * @param referenceName should refer to the name of the parameter being checked
     * @return the given {@code string} to make inline checks possible
     */
    public static String checkArgumentMinLength(String string, int minLength, String referenceName)
    {
        checkReferenceName(referenceName);
        if (string.length() < minLength) illegalArg("%s (%s) has less than %s character%s (it has %s characters)", 
            referenceName, string, minLength, minLength == 1 ? "" : "s", string.length());
        return string;
    }


    private static void checkReferenceName(String referenceName)
    {
        checkNotNull(referenceName, "Invalid use of Preconditions method: referenceName is null");
    }
    
    /**
     * Verifies that the given string is at most {@code maxLength} characters long.
     * If the given string is too long, an {@link IllegalArgumentException} is thrown whose message is based on {@code referenceName}.
     * 
     * @param string the string to check
     * @param maxLength 
     * @param referenceName should refer to the name of the parameter being checked
     * @return the given {@code string} to make inline checks possible
     */
    public static String checkArgumentMaxLength(String string, int maxSize, String referenceName)
    {
        checkReferenceName(referenceName);
        if (string.length() > maxSize) illegalArg("%s (%s) has more than %s character%s (it has %s characters)", 
            referenceName, string, maxSize, maxSize == 1 ? "" : "s", string.length());
        return string;
    }

    /**
     * Verifies that the given condition is true. If the given condition is not true, an
     * {@link IllegalArgumentException} is thrown whose message is the result of calling
     * {@link String#format(String, Object...)} on the given {@code message} and {@code parameters}. Calling code
     * should be used like this:
     * 
     * <pre>
     * public void doSomething(Foo foo)
     * {
     *    Preconditions.checkArgument(foo.isInitialized, &quot;foo %s is not initialized!&quot;, foo);
     *    ...
     * }
     * </pre>
     * 
     * 
     * @param string the string to check
     * @param maxLength
     * @param referenceName should refer to the name of the parameter being checked
     */
    public static void checkArgument(boolean condition, String message, Object... parameters)
    {
        if (!condition) illegalArg(message, parameters);
    }

    /**
     * Verifies the given string can be used as a valid <a href="contract of http://www.w3.org/TR/xmlschema-2/#token">token</a>
     * 
     * @param string the string to check
     * @param referenceName should refer to the name of the parameter being checked
     * @return the given {@code string}, to make inline checks possible
     */
    public static String checkNonEmptyToken(String string, String referenceName)
    {
        checkReferenceName(referenceName);
        checkNotNullOrEmpty(string, referenceName);
        if (string.startsWith(" ")) illegalArg("%s starts with a space", string);
        if (string.endsWith(" ")) illegalArg("%s ends with a space", string);
        if (string.contains("  ")) illegalArg("%s contains a sequence of two or more spaces", string);
        if (string.contains("\t")) illegalArg("%s contains a tab character", string);
        if (string.contains("\n")) illegalArg("%s contains a newline character", string);
        if (string.contains("\r")) illegalArg("%s contains a carriage return character", string);
        return string;
    }


    public static void checkState(boolean condition, String message, Object... parameters)
    {
        checkNotNull(message, "Invalid use of Preconditions.checkState(): message is null");
        if (!condition) throw new IllegalStateException(String.format(message, parameters));
    }


}
