package com.drivesmart.app.android.model;

import android.util.Log;

import com.drivesmart.app.android.helper.DatePatternHelper;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Created by omiwrench on 2016-01-02.
 */
public class Report {
    private static final String TAG = Report.class.getName();
    public static final String CREATED_AT_FORMAT = "yyyy/MM/dd HH:mm:ss";

    private String title;
    private String description;
    private String location;
    private DateTime createdAt;

    public Report(String title, String description, String location, String createdAtString){
        try{
            DateTime createdAt = DateTimeFormat.forPattern(DatePatternHelper.JSON_DATE_TIME_FORMAT).parseDateTime(createdAtString);
            init(title, description, location, createdAt);
        }
        catch(IllegalArgumentException e) {
            String msg = "Report createdAt date string was invalid. createdAt: " + createdAtString;
            Log.e(TAG, msg);
            throw new IllegalArgumentException(msg);
        }
    }
    public Report(String title, String description, String location, DateTime createdAt) {
        init(title, description, location, createdAt);
    }
    private void init(String title, String description, String location, DateTime createdAt){
        this.title = title;
        this.description = description;
        this.location = location;
        this.createdAt = createdAt;
    }
}
