package org.ccci.maintenance.util;

import javax.servlet.http.HttpServletResponse;

/**
 * Indicates that this http request can't be processed.
 * It may badly formed, most likely due to missing or invalid parameters.
 * Or it may not have proper authentication.
 * 
 * @author Matt Drees
 */
public class BadRequestException extends RuntimeException
{
    private int responseCode;

    /**
     * Creates an BadRequestException with a 400 status code.
     * @param message to be sent to the client, as part of the 400 error response 
     */
    public BadRequestException(String message)
    {
        super(message);
        responseCode = HttpServletResponse.SC_BAD_REQUEST;
    }

    /**
     * Creates an BadRequestException with the given status code.
     * @param responseCode the status code to be sent to the client
     * @param message to be sent to the client, as part of the error response
     */
    public BadRequestException(int responseCode, String message)
    {
        super(message);
        this.responseCode = responseCode;
    }

    private static final long serialVersionUID = 1L;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
}
