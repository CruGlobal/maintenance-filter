package org.ccci.maintenance;

import org.ho.yaml.exception.YamlException;
import org.ho.yaml.wrapper.AbstractWrapper;
import org.ho.yaml.wrapper.SimpleObjectWrapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeObjectWrapper extends AbstractWrapper implements SimpleObjectWrapper
{

    private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
    
    public DateTimeObjectWrapper(Class<?> type)
    {
        super(type);
    }
    
    public Class<?> expectedArgType()
    {
        return String.class;
    }

    public Object getOutputValue()
    {
        DateTime dateTime = (DateTime) getObject();
        return formatter.print(dateTime);
    }
    
    @Override
    public void setObject(Object obj)
    {
        if (obj instanceof String)
        { 
            String objAsString = (String) obj;
            DateTime dateTime;
            try
            {
                dateTime = formatter.parseDateTime(objAsString);
            }
            catch (IllegalArgumentException e)
            {
                throw new YamlException("Problem parsing " + obj + ": " + e);
            }
            super.setObject(dateTime);
        }
        else
        {
            super.setObject(obj);
        }
    }

}
