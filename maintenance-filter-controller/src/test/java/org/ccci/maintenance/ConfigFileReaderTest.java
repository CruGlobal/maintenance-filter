package org.ccci.maintenance;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import com.atlassian.hamcrest.DeepIsEqual;

public class ConfigFileReaderTest
{
    
    @Test
    public void testDumpAndReadConfigFile() throws IOException
    {
        MaintenanceWindowUpdate update = new MaintenanceWindowUpdate();
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId("test-outage");
        window.setShortMessage("This site will be unavailable for a short while");
        window.setLongMessage("This site will be unavailable for the next hour or so.  \n" +
                "Please visit <a href=\"http://www.youtube.com\">somewhere else</a> while you wait.");
        window.setBeginAt(new DateTime(2010, 9, 23, 2, 29, 50, 234));
        window.setEndAt(new DateTime(2010, 9, 23, 3, 35, 50, 0));
        
        update.setWindow(window);
        
        update.getServerControlUrls().add("http://www.example.com/control/path");
        update.getServerControlUrls().add("http://www.example2.com/control2/path");
        
        ConfigFileReader reader = new ConfigFileReader();
        
        String configFilePath = "target/testConfig.yml";
        OutputStream stream = new FileOutputStream(configFilePath);
        reader.dumpConfigFile(stream, configFilePath, update);
        stream.close();
        
        InputStream inputStream = new FileInputStream(configFilePath);
        MaintenanceWindowUpdate reloadedConfigUpdate = reader.parseConfigFile(inputStream, configFilePath);
        inputStream.close();
        
        assertThat(reloadedConfigUpdate, DeepIsEqual.deeplyEqualTo(update));
    }

}
