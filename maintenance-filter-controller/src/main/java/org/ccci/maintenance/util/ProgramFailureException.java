package org.ccci.maintenance.util;

/**
 * Indicates that the program should halt, and the given message should be displayed
 * to the user.  A custom exit code may be given, otherwise the exit code will be 1. 
 * 
 * @author Matt Drees
 */
public class ProgramFailureException extends RuntimeException
{
    private final int exitCode;
    
    public ProgramFailureException(String message, Throwable cause)
    {
        super(message, cause);
        exitCode = 1;
    }

    public ProgramFailureException(String message)
    {
        super(message);
        exitCode = 1;
    }
    
    public ProgramFailureException(String message, int exitCode)
    {
        super(message);
        this.exitCode = exitCode;
    }
    
    public ProgramFailureException(String message, Throwable cause, int exitCode)
    {
        super(message, cause);
        this.exitCode = exitCode;
    }
    
    public int getExitCode()
    {
        return exitCode;
    }

    private static final long serialVersionUID = 1L;
}
