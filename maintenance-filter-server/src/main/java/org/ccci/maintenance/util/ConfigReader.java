package org.ccci.maintenance.util;

import javax.servlet.ServletContext;
import java.util.Properties;

/**
 * @author Matt Drees
 */
public class ConfigReader {
    private ServletContext servletContext;
    private Properties properties;

    public ConfigReader(ServletContext servletContext, Properties properties) {
        this.servletContext = servletContext;
        this.properties = properties;
    }

    public String getParameter(String parameterName)
    {
        return servletContext.getInitParameter(parameterName);
    }
}
