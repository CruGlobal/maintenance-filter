package org.ccci.maintenance.util;

import org.ccci.maintenance.MaintenanceWindow;

/**
 * @author Matt Drees
 */
public class OwnedMaintenanceWindow
{
    public final MaintenanceWindow window;
    public final String owner;

    public OwnedMaintenanceWindow(MaintenanceWindow window, String owner)
    {
        this.window = window;
        this.owner = owner;
    }
}
