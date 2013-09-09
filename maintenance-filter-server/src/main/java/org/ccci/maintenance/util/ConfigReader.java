package org.ccci.maintenance.util;

import javax.servlet.ServletContext;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Matt Drees
 */
public class ConfigReader {
    private ServletContext servletContext;
    private Properties properties;

    static final Pattern pattern = Pattern.compile("\\{([^${}]*)\\}");

    public ConfigReader(ServletContext servletContext, Properties properties) {
        this.servletContext = servletContext;
        this.properties = properties;
    }

    public String getParameter(String parameterName)
    {
        String literalParameter = servletContext.getInitParameter(parameterName);
        if (literalParameter == null)
            return null;
        else
            return interpolate(literalParameter);
    }

    private String interpolate(String value) {
        String propertyReference = findPropertyReference(value);
        if (propertyReference != null)
            return interpolate(replacePropertyReference(value, propertyReference));
        else
            return value;
    }

    private String replacePropertyReference(String value, String propertyReference) {
        String propertyValue = properties.getProperty(propertyReference);
        if (propertyValue == null)
            throw new IllegalArgumentException("there is no such system property: " + propertyReference);
        return value.replace("${" + propertyReference + "}", propertyValue);
    }

    private String findPropertyReference(String value) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.find())
            return matcher.group(1);
        else
            return null;
    }
}
