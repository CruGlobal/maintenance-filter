package org.ccci.maintenance.util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * Acts a little like JSR 310 Clock: https://jsr-310.dev.java.net/nonav/doc-2008-08-04/javax/time/Clock.html
 * (except simpler)
 * 
 * May be subclassed for tests which want to manipulate time. see http://tech.puredanger.com/2008/09/24/controlling-time/
 * 
 * Should be replaced by JSR 310's Clock whenever JSR 310 is released
 * 
 * @author Matt Drees
 *
 */
public abstract class Clock 
{

	public abstract DateTime currentDateTime();
	
	public static Clock system()
	{
		return new SystemClock();
	}

    public LocalDate today()
    {
        return new LocalDate(currentDateTime());
    }
	
}
