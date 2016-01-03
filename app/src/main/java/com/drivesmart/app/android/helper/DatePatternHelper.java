package com.drivesmart.app.android.helper;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by omiwrench on 2016-01-02.
 */
public class DatePatternHelper {
    public static String JSON_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String printForJSONFormat(DateTime time){
        return DateTimeFormat.forPattern(JSON_DATE_TIME_FORMAT).print(time);
    }
    public static DateTime parseForJSONFormat(String timeString){
        return DateTimeFormat.forPattern(JSON_DATE_TIME_FORMAT).parseDateTime(timeString);
    }
}
