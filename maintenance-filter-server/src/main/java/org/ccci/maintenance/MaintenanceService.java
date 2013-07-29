package org.ccci.maintenance;

public interface MaintenanceService
{

    /**
     * Returns the active maintenance window for the given filter, if it exists.  An active window is one whose start time is before now,
     * and whose end time is after now. If filterName is null, then the default maintenance window is checked.
     * If there is no active window, this returns null.
     */
    public MaintenanceWindow getActiveMaintenanceWindow(String filterName);
    
    /**
     * Creates a new maintenance window for the given filter, or, if a window exists with the same id as the given window, updates its data
     * with the given window's data.  If filterName is null, then the update is performed on the default maintenance window.
     */
    public void createOrUpdateMaintenanceWindow(String filterName, MaintenanceWindow window);

    /**
     * Returns true if the given key matches the configured key
     */
    public boolean isAuthenticated(String key);

    /**
     * Returns true if the given filter name is valid
     */
    boolean filterExists(String filterName);
}
