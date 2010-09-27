package org.ccci.maintenance;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceWindowUpdate
{
    
    private List<String> serverControlUrls = new ArrayList<String>();
    
    private MaintenanceWindow window;

    
    
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
}
