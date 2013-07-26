package org.ccci.maintenance;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceWindowUpdate
{
    
    private List<String> serverControlUrls = new ArrayList<String>();
    private MaintenanceWindow window;
    private String key;


    public List<String> getServerControlUrls()
    {
        return serverControlUrls;
    }

    public void setServerControlUrls(List<String> serverControlUrls)
    {
        this.serverControlUrls = serverControlUrls;
    }

    public MaintenanceWindow getWindow()
    {
        return window;
    }

    public void setWindow(MaintenanceWindow window)
    {
        this.window = window;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
