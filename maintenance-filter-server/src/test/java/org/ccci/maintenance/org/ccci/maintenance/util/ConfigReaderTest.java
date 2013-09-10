package org.ccci.maintenance.org.ccci.maintenance.util;

import org.ccci.maintenance.util.ConfigReader;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletContext;
import java.sql.SQLException;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

/**
 * @author Matt Drees
 */
public class ConfigReaderTest {


    @Mock
    ServletContext context;

    Properties properties;
    ConfigReader reader;

    @BeforeMethod
    public void setupDb() throws SQLException
    {
        MockitoAnnotations.initMocks(this);
        properties = new Properties();
        reader = new ConfigReader(context, properties);
    }

    @Test
    public void testParameterWithNoSystemProperties()
    {
        when(context.getInitParameter("color")).thenReturn("red");
        assertThat(reader.getParameter("color"), is(equalTo("red")));
    }

    @Test
    public void testParameterWithNoSystemPropertiesButContainsSomeSpecialCharacters()
    {
        when(context.getInitParameter("color")).thenReturn("${red");
        assertThat(reader.getParameter("color"), is(equalTo("${red")));
    }

    @Test
    public void testParameterWithOneSystemPropertyThatExists()
    {
        when(context.getInitParameter("color")).thenReturn("${josh.favorite.color}");
        properties.put("josh.favorite.color", "black");
        assertThat(reader.getParameter("color"), is(equalTo("black")));
    }

    @Test(expectedExceptions =  IllegalArgumentException.class)
    public void testParameterWithOneSystemPropertyThatDoesNotExist()
    {
        when(context.getInitParameter("color")).thenReturn("${josh.favorite.color}");
        reader.getParameter("color");
    }

    @Test
    public void testParameterWithTwoSystemPropertiesThatExist()
    {
        when(context.getInitParameter("favorites")).thenReturn("${josh.favorite.color},${josh.favorite.transportation},hamburgers");
        properties.put("josh.favorite.color", "black");
        properties.put("josh.favorite.transportation", "motorcycle");
        assertThat(reader.getParameter("favorites"), is(equalTo("black,motorcycle,hamburgers")));
    }

    @Test
    public void testParameterWithRecursiveSystemPropertiesThatExist()
    {
        when(context.getInitParameter("color")).thenReturn("${josh.favorite.color}");
        properties.put("josh.favorite.color", "${matt.favorite.color}");
        properties.put("matt.favorite.color", "black");
        assertThat(reader.getParameter("color"), is(equalTo("black")));
    }

    @Test
    public void testWhenNoParameterConfiguredInWebXml()
    {
        when(context.getInitParameter("color")).thenReturn(null);
        assertThat(reader.getParameter("color"), is(nullValue()));
    }


}
