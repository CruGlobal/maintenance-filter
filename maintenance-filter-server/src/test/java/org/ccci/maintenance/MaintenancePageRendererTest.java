package org.ccci.maintenance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MaintenancePageRendererTest
{
    MaintenancePageRenderer renderer = new MaintenancePageRenderer();
    
    
    @Mock
    HttpServletResponse response;
    
    @BeforeClass
    public void init()
    {
        MockitoAnnotations.initMocks(this);
    }
    
    
    @Test
    public void testRenderHtml() throws IOException
    {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  " +
                "Please visit <a href='http://www.youtube.com'>somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        
        when(response.getWriter()).thenReturn(printWriter);
        
        renderer.renderMaintenancePage(response, window);
        
        assertThat(stringWriter.toString(), containsString("<html>"));
        assertThat(stringWriter.toString(), containsString(window.getLongMessage()));
    }
    
}
