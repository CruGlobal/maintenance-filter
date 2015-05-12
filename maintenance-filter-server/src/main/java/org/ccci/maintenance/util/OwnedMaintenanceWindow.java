package org.ccci.maintenance.util;

import org.ccci.maintenance.MaintenanceWindow;

import java.io.Serializable;

/**
 * @author Matt Drees
 */
public class OwnedMaintenanceWindow implements Serializable
{
    private static final long serialVersionUID = 1L;

    public final MaintenanceWindow window;
    public final String owner;

    public OwnedMaintenanceWindow(MaintenanceWindow window, String owner)
    {
        this.window = window;
        this.owner = owner;
    }
}
