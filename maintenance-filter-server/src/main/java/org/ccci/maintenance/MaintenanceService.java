package org.ccci.maintenance;

public interface MaintenanceService
{

    /**
     * Returns the active maintenance window, if it exists.  An active window is one whose start time is before now,
     * and whose end time is after now.
     * If there is no active window, this returns null.
     */
    public MaintenanceWindow getCurrentWindow();
    
    /**
     * Creates a new maintenance window, or, if a window exists with the same id as the given window, updates its data
     * with the given window's data.
     */
    public void createOrUpdateMaintenanceWindow(MaintenanceWindow window);
    
}
