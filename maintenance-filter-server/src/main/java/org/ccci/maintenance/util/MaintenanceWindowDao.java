package org.ccci.maintenance.util;

import org.ccci.maintenance.MaintenanceWindow;
import org.joda.time.DateTime;

/**
 * @author Matt Drees
 */
public interface MaintenanceWindowDao
{
    MaintenanceWindow getActiveMaintenanceWindow(String filterName, DateTime currentDateTime);
    OwnedMaintenanceWindow getMaintenanceWindowById(String id);

    void createMaintenanceWindow(OwnedMaintenanceWindow window);
    void updateMaintenanceWindow(OwnedMaintenanceWindow window);
}
