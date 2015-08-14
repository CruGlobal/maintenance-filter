package org.ccci.maintenance.util;

import org.ccci.maintenance.MaintenanceWindow;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Set;

/**
 * @author Matt Drees
 */
public class InfinispanMaintenanceWindowDao implements MaintenanceWindowDao
{
    private final Cache<String, OwnedMaintenanceWindow> cache;

    //using Object instead of Cache here to allow caller to avoid a hard dependency upon infinispan
    @SuppressWarnings("unchecked")
    public InfinispanMaintenanceWindowDao(Object containerAsObject)
    {
        CacheContainer cacheContainer = (CacheContainer) containerAsObject;
        cache = cacheContainer.getCache();
    }

    public MaintenanceWindow getActiveMaintenanceWindow(
        String filterName, DateTime currentDateTime)
    {
        for (Map.Entry<String, OwnedMaintenanceWindow> entry : cache.entrySet())
        {
            if (windowIsActiveAndOwnedByFilter(filterName, currentDateTime, entry))
            {
                return entry.getValue().window;
            }
        }

        return null;
    }

    private boolean windowIsActiveAndOwnedByFilter(
        String filterName,
        DateTime currentDateTime,
        Map.Entry<String, OwnedMaintenanceWindow> entry)
    {
        MaintenanceWindow window = entry.getValue().window;
        String owner = entry.getValue().owner;

        return windowHasBegun(currentDateTime, window.getBeginAt()) &&
            windowHasNotYetEnded(currentDateTime, window.getEndAt()) &&
            Objects.equal(owner, filterName);
    }

    private boolean windowHasBegun(
        DateTime currentDateTime, DateTime beginAt)
    {
        return beginAt == null || beginAt.isBefore(currentDateTime);
    }

    private boolean windowHasNotYetEnded(
        DateTime currentDateTime, DateTime endAt)
    {
        return endAt == null || endAt.isAfter(currentDateTime);
    }

    public OwnedMaintenanceWindow getMaintenanceWindowById(String id)
    {
        return cache.get(id);
    }

    public void createMaintenanceWindow(OwnedMaintenanceWindow ownedMaintenanceWindow)
    {
        putOrReplace(ownedMaintenanceWindow);
    }

    private void putOrReplace(OwnedMaintenanceWindow ownedMaintenanceWindow)
    {
        cache.put(ownedMaintenanceWindow.window.getId(), ownedMaintenanceWindow);
    }

    public void updateMaintenanceWindow(OwnedMaintenanceWindow ownedMaintenanceWindow)
    {
        putOrReplace(ownedMaintenanceWindow);
    }
}
