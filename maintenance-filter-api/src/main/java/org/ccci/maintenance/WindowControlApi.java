package org.ccci.maintenance;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class WindowControlApi
{

    /** 
     * Character encoding to encode/decode http requests from the control client to the control servlet. UTF-8 is 
     * always present on any compliant JVM.
     */
    public static final String REQUEST_CHARACTER_ENCODING = "UTF-8";
    
    public static final String BYPASS_REQUEST_PARAMETER = "bypassMaintenanceFilter";
    
    public static final String CREATE_OR_UPDATE_PATH = "updateWindow";
    
    public static DateTimeFormatter dateTimeFormatter()
    {
        return ISODateTimeFormat.dateTime();
    }
    public static final String WINDOW_SUCCESSFULLY_UPDATED_RESPONSE = "window successfully updated";

    public static final String ID_PARAMETER = "id";

    public static final String END_AT_PARAMETER = "endAt";

    public static final String BEGIN_AT_PARAMETER = "beginAt";

    public static final String LONG_MESSAGE_PARAMETER = "longMessage";

    public static final String SHORT_MESSAGE_PARAMETER = "shortMessage";
    

}
