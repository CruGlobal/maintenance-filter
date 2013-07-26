package org.ccci.maintenance;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.ccci.maintenance.util.BadRequestException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

/**
 * Handles new maintenance window requests and requests to update existing maintenance windows.  These requests will come
 * from the maintenance-filter-controller app. 
 * 
 * My hope is that this can one day replaced by a jax-rs handler, once all of our apps are on a Java EE 6 platform
 * 
 * @author Matt Drees
 */
public class MaintenanceControlServlet extends HttpServlet
{
    private final Logger log = Logger.getLogger(getClass());
    
    private MaintenanceService maintenanceService;
    private DateTimeFormatter formatter = WindowControlApi.dateTimeFormatter();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        request.setCharacterEncoding(WindowControlApi.REQUEST_CHARACTER_ENCODING);
        response.setCharacterEncoding(WindowControlApi.REQUEST_CHARACTER_ENCODING);

        try
        {
            authenticate(request);
            handle(request, response);
        }
        catch (BadRequestException e)
        {
            response.sendError(e.getResponseCode(), e.getMessage());
        }
    }

    private void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/" + WindowControlApi.CREATE_OR_UPDATE_PATH))
        {
                handleUpdate(request, response);
        }
        else
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "no such command");
        }
    }

    private void authenticate(HttpServletRequest request) {
        if (!maintenanceService.isAuthenticated(getKey(request)))
        {
            throw new BadRequestException(HttpServletResponse.SC_UNAUTHORIZED, "invalid authentication key");
        }
    }


    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        MaintenanceWindow window = constructWindowFromRequest(request);
        log.info("updating window " + window);
        try
        {
            maintenanceService.createOrUpdateMaintenanceWindow(window);
        }
        catch (IllegalArgumentException e)
        {
            log.info("failed request: " + e);
            throw new BadRequestException(e.getMessage());
        }
        catch (RuntimeException e)
        {
            log.error("internal server error for request", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }
        response
            .getWriter()
            .append(WindowControlApi.WINDOW_SUCCESSFULLY_UPDATED_RESPONSE)
            .flush();
    }

    private String getKey(HttpServletRequest request) {
        String key = request.getHeader("Authorization");
        if (key == null)
            throw new BadRequestException(HttpServletResponse.SC_UNAUTHORIZED, "authentication required");
        if (!key.startsWith("Key "))
            throw new BadRequestException(HttpServletResponse.SC_UNAUTHORIZED, "unusable authentication scheme");
        return key.substring("Key ".length());
    }


    private MaintenanceWindow constructWindowFromRequest(HttpServletRequest request)
    {
        String windowId = getRequiredParameter(request, WindowControlApi.ID_PARAMETER);
        String shortMessage = getRequiredParameter(request, WindowControlApi.SHORT_MESSAGE_PARAMETER);
        String longMessage = getRequiredParameter(request, WindowControlApi.LONG_MESSAGE_PARAMETER);
        DateTime beginAt = parseIfPresent(request.getParameter("beginAt"), WindowControlApi.BEGIN_AT_PARAMETER);
        DateTime endAt = parseIfPresent(request.getParameter("endAt"), WindowControlApi.END_AT_PARAMETER);
        
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId(windowId);
        window.setShortMessage(shortMessage);
        window.setLongMessage(longMessage);
        window.setBeginAt(beginAt);
        window.setEndAt(endAt);
        return window;
    }


    private String getRequiredParameter(HttpServletRequest request, String parameter)
    {
        String value = request.getParameter(parameter);
        if (value == null)
            throw new BadRequestException("required parameter is missing: " + parameter);
        return value;
    }


    private DateTime parseIfPresent(String dateAsString, String parameterName)
    {
        if (dateAsString == null)
            return null;
        try
        {
            return formatter.parseDateTime(dateAsString);
        }
        catch (IllegalArgumentException e)
        {
            throw new BadRequestException("invalid date format for " + parameterName + "; " + e.getMessage());
        }
    }


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String name = config.getInitParameter("name");
        Bootstrap bootstrap = Bootstrap.getInstance(config.getServletContext());
        maintenanceService = bootstrap.getMaintenanceService(name);
    }
    
    
    private static final long serialVersionUID = 1L;

}
