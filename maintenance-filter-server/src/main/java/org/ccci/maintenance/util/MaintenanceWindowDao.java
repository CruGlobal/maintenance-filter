package org.ccci.maintenance.util;

import org.ccci.maintenance.MaintenanceWindow;
import org.joda.time.DateTime;

/**
 * @author Matt Drees
 */
public interface MaintenanceWindowDao
{
    MaintenanceWindow getActiveMaintenanceWindow(String filterName, DateTime currentDateTime);

    /** retrieves the window identified by the given id, or null if it does not exist */
    OwnedMaintenanceWindow getMaintenanceWindowById(String id);

    void createMaintenanceWindow(OwnedMaintenanceWindow window);
    void updateMaintenanceWindow(OwnedMaintenanceWindow window);
}
