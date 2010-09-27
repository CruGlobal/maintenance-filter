package org.ccci.maintenance;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ccci.maintenance.util.Exceptions;
import org.ccci.maintenance.util.ProgramFailureException;
import org.ho.yaml.YamlConfig;
import org.ho.yaml.exception.YamlException;
import org.joda.time.DateTime;

public class ConfigFileReader
{

    public MaintenanceWindowUpdate readConfigFile(String configFilePath)
    {
        FileInputStream stream;
        try
        {
            stream = new FileInputStream(configFilePath);
        }
        catch (FileNotFoundException e)
        {
            throw new ProgramFailureException("Can't open file " + e.getLocalizedMessage(), e);
        }
        try
        {
            return parseConfigFile(stream, configFilePath);
        }
        finally
        {
            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                Exceptions.swallow(e, "exception closing " + configFilePath);
            }
        }
    }

    MaintenanceWindowUpdate parseConfigFile(InputStream stream, String configFilePath)
    {
        MaintenanceWindowUpdate windowUpdate;
        try
        {
            YamlConfig yamlConfig = buildYamlConfig();
            windowUpdate = yamlConfig.loadType(stream, MaintenanceWindowUpdate.class);
        }
        catch (YamlException e)
        {
            throw new ProgramFailureException("Can't parse config file " + configFilePath + ": " + e.getLocalizedMessage(), e);
        }
        return windowUpdate;
    }

    private YamlConfig buildYamlConfig()
    {
        YamlConfig yamlConfig = YamlConfig.getDefaultConfig();
        yamlConfig.getHandlers().put(DateTime.class.getName(), DateTimeObjectWrapper.class.getName());
        return yamlConfig;
    }
    
    void dumpConfigFile(OutputStream stream, String configFilePath, MaintenanceWindowUpdate update)
    {
        try
        {
            YamlConfig yamlConfig = buildYamlConfig();
            yamlConfig.dump(update, stream, true);
        }
        catch (YamlException e)
        {
            throw new ProgramFailureException("Can't dump config file " + configFilePath + ": " + e.getLocalizedMessage(), e);
        }
    }
}
