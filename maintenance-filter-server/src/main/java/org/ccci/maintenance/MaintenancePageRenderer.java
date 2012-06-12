package org.ccci.maintenance;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.ccci.maintenance.util.Exceptions;


public class MaintenancePageRenderer
{
    /** 
     * The character set with which we render the maintenance page.  We currently always use UTF-8, instead of using the character set
     * requested by the client.
     */
    static final String CHARSET = "UTF-8";

    private final Logger log = Logger.getLogger(MaintenancePageRenderer.class);
    
    /**
     * The contents of this file form a large 'format string'.  There are 2 indexed arguments
     * to this format string.
     * 1. the message body to be displayed to users (which may contain html markup)
     * 2. the character set with which this html page will be encoded.
     * 
     * See http://download.oracle.com/javase/6/docs/api/java/util/Formatter.html for more details
     * on the syntax of format strings.
     * Note in particular '%' symbols must be escaped as '%%'
     */
    String pageName = "MaintenancePage.html";

    public void renderMaintenancePage(HttpServletResponse response, MaintenanceWindow window)
    {
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(pageName);
        if (templateStream == null)
        {
            throw new IllegalStateException(pageName + " does not appear to be on the classpath.  Is the build correct?");
        }
        try
        {
            renderWithInputStream(response, window, templateStream);
        }
        finally
        {
            try
            {
                templateStream.close();
            }
            catch (IOException e)
            {
                Exceptions.swallow(e, "exception closing template input stream");
            }
        }
    }

    private void renderWithInputStream(HttpServletResponse response, MaintenanceWindow window, InputStream templateStream)
    {
        String template;
        try
        {
            template = IOUtils.toString(templateStream, CHARSET);
        }
        catch (IOException e)
        {
            throw Exceptions.wrap(e);
        }
        String html = formatHtml(window, template);
        deliverHtml(response, html);
    }


    /** see {@link #pageName} for parameter meanings */
    private String formatHtml(MaintenanceWindow window, String template)
    {
        return template
            .replace("${charset}", CHARSET)
            .replace("${outageMessage}", window.getLongMessage());
    }
    
    private void deliverHtml(HttpServletResponse response, String html)
    {
        response.setContentType("text/html");
        response.setCharacterEncoding(CHARSET);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

        PrintWriter writer;
        try
        {
            writer = response.getWriter();
        }
        catch (IOException e)
        {
            try
            {
                log.error("Unable to get PrintWriter from response; sending http 500", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Sorry, an odd internal error occurred.  Please try again.");
                return;
            }
            catch (IOException sendErrorException)
            //Hmmm.  What now?
            {
                throw Exceptions.wrap(sendErrorException);
            }
        }
        sendMessage(writer, html);
        return;
    }

    private void sendMessage(PrintWriter writer, String message)
    {
        writer.append(message);
        writer.flush();
        if (writer.checkError())
        {
            log.error("Unknown error occurred while sending message to client via PrintWriter.");
        }
        writer.close();
    }

    public void sendSimpleTextMaintenanceMessage(ServletResponse response, MaintenanceWindow window)
    {
        response.setCharacterEncoding(CHARSET);
        PrintWriter writer;
        try
        {
            writer = response.getWriter();
        }
        catch (IOException e)
        {
            log.error("Unable to get PrintWriter from non-http response; ignoring this request, as not much else we can do", e);
            return;
        }
        sendMessage(writer, window.getShortMessage());
        return;
    }

}
