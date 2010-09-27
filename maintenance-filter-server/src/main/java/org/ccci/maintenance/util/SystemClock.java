package org.ccci.maintenance.util;

import org.joda.time.DateTime;

public class SystemClock extends Clock
{

	@Override
	public DateTime currentDateTime() {
		return new DateTime();
	}

}
