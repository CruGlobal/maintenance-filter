package org.ccci.maintenance.util;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimeParserBucket;

/** 
 * non-instantiable class with static utility methods for date/time related stuffs
 * 
 * @author Matt Drees
 */
//TODO: copy/pasted from Util project
public class TimeUtil
{
    private TimeUtil() {}
    
    public static final DateTime epoch = new DateTime("1970-01-01T00:00:00Z");

    public static Period addHrMin(Period p1, Period p2)
    {
        Preconditions.checkNotNull(p1, "p1 is null");
        Preconditions.checkNotNull(p2, "p2 is null");
        return p1.plusHours(p2.getHours()).plusMinutes(p2.getMinutes());
    }

    public static Period add(Period p1, Period p2)
    {
        Preconditions.checkNotNull(p1, "p1 is null");
        Preconditions.checkNotNull(p2, "p2 is null");
        return p1.plusYears(p2.getYears()).plusMonths(p2.getMonths()).plusDays(p2.getDays()).plusHours(p2.getHours()).plusMinutes(
            p2.getMinutes()).plusSeconds(p2.getSeconds()).plusMillis(p2.getMillis());
    }

    public static double durationToHours(Duration duration)
    {
        Preconditions.checkNotNull(duration, "duration is null");
        return (double) duration.getMillis() / DateTimeConstants.MILLIS_PER_HOUR;
    }

    public static Duration hoursToDuration(double hours)
    {
        return new Duration((long) (hours * DateTimeConstants.MILLIS_PER_HOUR));
    }

    public static Time localTimeToSqlTime(LocalTime localTime)
    {
        return localTime == null ? null : localTimeToSqlTime(localTime, DateTimeZone.getDefault());
    }

    public static LocalTime sqlTimeToLocalTime(Time time)
    {
        return time == null ? null : sqlTimeToLocalTime(time, DateTimeZone.getDefault());
    }

    public static Date localDateToSqlDate(LocalDate localDate)
    {
        return localDate == null ? null : new Date(localDate.toDateTimeAtStartOfDay().getMillis());
    }

    public static LocalDate sqlDateToLocalDate(Date date)
    {
        return date == null ? null : new LocalDate(date);
    }
    
    public static LocalDate utilDateToLocalDate(java.util.Date date)
    {
        return date == null ? null : new LocalDate(date);
    }

    /**
     * Create a java.sql.Time that, when interpreted according to the given timezone, has the same time fields as the
     * fields of the given LocalTime
     * 
     * @param localTime
     * @param outputTimeZone
     * @return
     */
    public static Time localTimeToSqlTime(LocalTime localTime, DateTimeZone outputTimeZone)
    {
        return new Time(localTime.toDateTime(epoch).withZone(outputTimeZone).getMillis());
    }

    public static LocalTime sqlTimeToLocalTime(Time time, DateTimeZone inputTimeZone)
    {
        return new LocalTime(time, inputTimeZone);
    }
    
    
    /**
     * Create a java.sql.Timestamp that, when interpreted according to the given timezone, has the same fields as the
     * fields of the given DateTime. Note that implies the Timestamp's millisecond-epoch-offset will be different from
     * the given DateTime's, if the given DateTime's time zone is different from the given timezone.
     * 
     * If the two time zones have different DST behavior such that a Timestamp can't be constructed as required, throw
     * an Exception. See test for reason & example.
     * 
     * @param dateTime
     * @Param outputTimeZone
     * @return
     */
    /*
     * Maybe this could be shorter by using DateTime#withZone()... but not currently used, anyway
     */
    public static Timestamp dateTimeToSqlTimestamp(DateTime dateTime, DateTimeZone outputTimeZone)
    {
        DateTimeZone inputZone = dateTime.getZone();
        long inputUTCMillis = dateTime.getMillis();
        long inputLocalMillis = inputZone.convertUTCToLocal(inputUTCMillis);
        long outputUTCMillis = inputZone.getMillisKeepLocal(outputTimeZone, inputUTCMillis);
        if (outputTimeZone.convertUTCToLocal(outputUTCMillis) != inputLocalMillis) 
        {
            throw new RuntimeException("local timestamp can't be converted to an appropriate server timestamp"); 
        }
        return new Timestamp(outputUTCMillis);
    }

    /**
     * Exactly like {@link #dateTimeToSqlTimestamp(DateTime, DateTimeZone)}, except {@link DateTimeZone#getDefault()} is used
     * for the outputTimeZone.
     */
    public static Timestamp dateTimeToSqlTimestamp(DateTime dateTime)
    {
        return dateTime == null ? null : dateTimeToSqlTimestamp(dateTime, DateTimeZone.getDefault());
    }

    /**
     * Create a DateTime with the {@code outputTimeZone} that has the same fields as the given timestamp (when
     * interpreted according to {@code inputTimeZone}.
     * 
     * @param timestamp
     * @param inputTimeZone
     * @param outputTimeZone
     * @return
     */
    public static DateTime sqlTimestampToDateTime(Timestamp timestamp, DateTimeZone inputTimeZone,
                                                  DateTimeZone outputTimeZone)
    {
        return new DateTime(inputTimeZone.getMillisKeepLocal(outputTimeZone, timestamp.getTime()), outputTimeZone);
    }

    /**
     * Exactly like {@link #sqlTimestampToDateTime(Timestamp, DateTimeZone, DateTimeZone)}, except {@link DateTimeZone#getDefault()} 
     * is used for both inputTimeZone and outputTimeZone.
     * @param timestamp
     * @return
     */
    public static DateTime sqlTimestampToDateTime(Timestamp timestamp)
    {
        return timestamp == null ? null : sqlTimestampToDateTime(timestamp, DateTimeZone.getDefault(), DateTimeZone.getDefault());
    }
    
    public static String getZoneOffsetAsString(int offset)
    {
        return getZoneOffsetAsString(DateTimeZone.forOffsetMillis(offset), epoch);
    }

    public static String getZoneOffsetAsString(DateTimeZone zone, ReadableInstant dateTime)
    {
        return DateTimeFormat.forPattern("Z").print(new DateTime(dateTime, zone));
    }
    
    public static int getZoneOffset(String offset)
    {
        return DateTimeZone.forID(offset).getOffset(epoch);
    }

    /**
     * The number of milliseconds since the beginning of the day
     * @param localTime
     * @return
     */
    public static long getLocalMillis(LocalTime localTime)
    {
        return localTime.toDateTime(epoch).getMillis();
    }

    public static DateTime withSetOffsetZone(DateTime dateTime)
    {
        DateTimeZone setOffsetZone =
                DateTimeZone.forOffsetMillis(dateTime.getZone().getOffset(dateTime.getMillis()));
        return dateTime.withZone(setOffsetZone);
    }

    public static boolean isDaylightSavingsTimeBoundary(LocalDate localDate, DateTimeZone zone)
    {
        Preconditions.checkNotNull(localDate, "localDate is null");
        Preconditions.checkNotNull(zone, "zone is null");
        long instant = localDate.toDateTimeAtStartOfDay(zone).getMillis();
        long nextTransition = zone.nextTransition(instant);
        if (instant != nextTransition)
        {
            if (new LocalDate(nextTransition).equals(localDate))
            {
                return true;
            }
        }
        return false;
    }
    
    /*
     * Note: this is taken from a commit to joda-time: http://github.com/vvs/joda-time/commit/cd6e87b6510451b875a2886f50c16b068b13ee91
     * It will be available in a future joda time release.  It's just something I want now.  :-)
     * TODO: remove when upgrading joda-time
     */
    /**
     * Parses only the local date-time from the given text, returning a new LocalDate.
     * <p>
     * This will parse the text fully according to the formatter, using the UTC zone.
     * Once parsed, only the local date-time will be used.
     * This means that any parsed time-zone or offset field is completely ignored.
     * It also means that the zone and offset-parsed settings are ignored.
     *
     * @param text  the text to parse, not null
     * @return the parsed date-time, never null
     * @throws UnsupportedOperationException if parsing is not supported
     * @throws IllegalArgumentException if the text to parse is invalid
     */
    public static LocalDateTime parseLocalDateTime(DateTimeFormatter formatter, String text) {

        DateTimeParser parser = formatter.getParser();
        Chronology chronolgy = formatter.getChronolgy() == null ? ISOChronology.getInstance() : formatter.getChronolgy();
        Chronology chrono = chronolgy.withUTC();  // always use UTC, avoiding DST gaps
        DateTimeParserBucket bucket = new DateTimeParserBucket(0, chrono, formatter.getLocale(), formatter.getPivotYear());
        int newPos = parser.parseInto(bucket, text, 0);
        if (newPos >= 0) {
            if (newPos >= text.length()) {
                long millis = bucket.computeMillis(true, text);
                if (bucket.getZone() == null) {  // treat withOffsetParsed() as being true
                    int parsedOffset = bucket.getOffset();
                    DateTimeZone parsedZone = DateTimeZone.forOffsetMillis(parsedOffset);
                    chrono = chrono.withZone(parsedZone);
                }
                return new LocalDateTime(millis, chrono);
            }
        } else {
            newPos = ~newPos;
        }
        throw new IllegalArgumentException(makeErrorMessage(text, newPos));
    }

    private static String makeErrorMessage(String text, int errorPos)
    {
        int sampleLen = errorPos + 32;
        String sampleText;
        String message;
        if (text.length() <= sampleLen + 3) {
            sampleText = text;
        } else {
            sampleText = text.substring(0, sampleLen).concat("...");
        }
        
        if (errorPos <= 0) {
            message = "Invalid format: \"" + sampleText + '"';
        }
        
        if (errorPos >= text.length()) {
            message = "Invalid format: \"" + sampleText + "\" is too short";
        }
        
        message = "Invalid format: \"" + sampleText + "\" is malformed at \"" +
            sampleText.substring(errorPos) + '"';
        return message;
    }


}
