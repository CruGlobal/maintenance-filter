package org.ccci.maintenance.util;

/**
 * Indicates that this http request is badly formed, most likely due to missing or invalid parameters.
 * 
 * @author Matt Drees
 */
public class BadRequestException extends RuntimeException
{
    /**
     * @param message to be sent to the client, as part of the 400 error response 
     */
    public BadRequestException(String message)
    {
        super(message);
    }

    private static final long serialVersionUID = 1L;
    
}
