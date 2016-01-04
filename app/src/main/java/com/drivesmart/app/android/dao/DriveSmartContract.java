package com.drivesmart.app.android.dao;

import android.provider.BaseColumns;

/**
 * Created by omiwrench on 2016-01-04.
 */
public final class DriveSmartContract {
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String SEPARATOR = ", ";

    public DriveSmartContract() throws InstantiationException{
        throw new InstantiationException("DrivesmartContract cannot be instantiated");
    }

    public static abstract class ReportsEntry implements BaseColumns{
        public static final String TABLE_NAME = "reports";
        public static final String COLUMN_NAME_REPORT_ID = "reportId";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_LOCATION = "location";
        public static final String COLUMN_NAME_CREATED_AT = "createdAt";

        public static final String SQL_CREATE_ENTRIES  =
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY" + SEPARATOR +
                COLUMN_NAME_REPORT_ID + INT_TYPE + SEPARATOR +
                COLUMN_NAME_TITLE + TEXT_TYPE + SEPARATOR +
                COLUMN_NAME_DESCRIPTION + TEXT_TYPE + SEPARATOR +
                COLUMN_NAME_LOCATION + TEXT_TYPE + SEPARATOR +
                COLUMN_NAME_CREATED_AT + TEXT_TYPE +
                " )";
        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
