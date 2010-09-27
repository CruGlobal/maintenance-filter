package org.ccci.maintenance;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;

public class MaintenanceWindow
{
    @Size(max = 50)
    @NotNull
    private String id;
    
    @Size(max = 200)
    @NotNull
    private String shortMessage;
    
    /**
     * May contain html markup.  Will be rendered inside of an html <div>.
     */
    @Size(max = 2000)
    @NotNull
    private String longMessage;
    
    private DateTime beginAt;
    
    private DateTime endAt;

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append("MaintenanceWindow[")
            .append("id: ").append(id)
            .append(", ")
            .append("beginAt: ").append(beginAt)
            .append(", ")
            .append("endAt: ").append(endAt)
            .append("]")
            .toString();
    }
    
    
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public DateTime getBeginAt()
    {
        return beginAt;
    }

    public void setBeginAt(DateTime beginAt)
    {
        this.beginAt = beginAt;
    }

    public DateTime getEndAt()
    {
        return endAt;
    }

    public void setEndAt(DateTime endAt)
    {
        this.endAt = endAt;
    }

    public String getShortMessage()
    {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage)
    {
        this.shortMessage = shortMessage;
    }

    public String getLongMessage()
    {
        return longMessage;
    }

    public void setLongMessage(String longMessage)
    {
        this.longMessage = longMessage;
    }

}
