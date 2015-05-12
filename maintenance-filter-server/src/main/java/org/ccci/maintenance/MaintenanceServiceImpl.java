package org.ccci.maintenance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.ccci.maintenance.util.Clock;
import org.ccci.maintenance.util.Exceptions;
import org.ccci.maintenance.util.JdbcUtils;
import org.ccci.maintenance.util.MaintenanceWindowDao;
import org.ccci.maintenance.util.Objects;
import org.ccci.maintenance.util.OwnedMaintenanceWindow;
import org.ccci.maintenance.util.Preconditions;
import org.ccci.maintenance.util.Strings;
import org.ccci.maintenance.util.TimeUtil;
import org.joda.time.DateTime;


//TODO: make this jdbc code nice, create or use a helper framework.
public class MaintenanceServiceImpl implements MaintenanceService
{
    
    private final Clock clock;
    private MaintenanceWindowDao dao;
    private String key;
    private Set<String> filterNames = new HashSet<String>(5);

    public MaintenanceServiceImpl(Clock clock, MaintenanceWindowDao dao, String key)
    {
        this.clock = clock;
        this.dao = dao;
        this.key = key;
    }

    public MaintenanceWindow getActiveMaintenanceWindow(String filterName)
    {
        if (!filterExists(filterName))
            throw new IllegalStateException("invalid filterName: " + filterName);

        return dao.getActiveMaintenanceWindow(filterName, clock.currentDateTime());
    }


    public void createOrUpdateMaintenanceWindow(String filterName, MaintenanceWindow window)
    {
        OwnedMaintenanceWindow existingWindow = dao.getMaintenanceWindowById(window.getId());
        if (existingWindow == null)
        {
            dao.createMaintenanceWindow(new OwnedMaintenanceWindow(window, filterName));
        }
        else
        {
            checkIdNotInUseByAnotherFilter(existingWindow, window.getId(), filterName);
            dao.updateMaintenanceWindow(new OwnedMaintenanceWindow(window, filterName));
        }
    }

    private void checkIdNotInUseByAnotherFilter(OwnedMaintenanceWindow existing, String id, String targetFilterName)
    {
        String otherFilterName = existing.owner;
        Preconditions.checkArgument(
            Objects.equal(otherFilterName, targetFilterName),
            "the '%s' maintenance window is owned by %s, not by %s",
            id,
            getFilterDescription(otherFilterName),
            getFilterDescription(targetFilterName));
    }


    private String getFilterDescription(String filterName)
    {
        return filterName == null ? "the default filter" : "the " + filterName + " filter";
    }

    public boolean isAuthenticated(String key) {
        return isEqualInConstantTime(key, this.key);
    }

    public boolean filterExists(String filterName) {
        return filterNames.contains(Strings.nullToEmpty(filterName));
    }

    // see http://codahale.com/a-lesson-in-timing-attacks/
    private boolean isEqualInConstantTime(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        boolean result = true;
        for (int i = 0; i < a.length(); i++) {
            result = result & (a.charAt(i) == b.charAt(i));
        }
        return result;
    }

    public void addFilterName(String filterName) {
        filterNames.add(Strings.nullToEmpty(filterName));
    }

    public void initializationComplete() {
        //If/when I depend on guava, use ImmutableSet instead
        filterNames = Collections.unmodifiableSet(filterNames);
    }

    @Override
    public String toString()
    {
        return "MaintenanceServiceImpl[dao=" + dao.getClass().getSimpleName() +"]";
    }
}
