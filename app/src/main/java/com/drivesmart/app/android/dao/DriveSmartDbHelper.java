package com.drivesmart.app.android.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.drivesmart.app.android.model.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by omiwrench on 2016-01-04.
 */
public class DriveSmartDbHelper extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "DriveSmart.db";

    private List<SQLiteOpenHelper> delegateHelpers = new ArrayList<>();
    private ReportsDbHelper reportsDbHelper;

    public DriveSmartDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        reportsDbHelper = new ReportsDbHelper(context, DATABASE_VERSION, DATABASE_NAME);
        delegateHelpers.add(reportsDbHelper);
    }

    public void onCreate(SQLiteDatabase database){
        for(SQLiteOpenHelper delegate : delegateHelpers){
            delegate.onCreate(database);
        }
    }
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        for(SQLiteOpenHelper delegate : delegateHelpers){
            delegate.onUpgrade(database, oldVersion, newVersion);
        }
    }

    public void insertReport(Report report, OnQueryFinished<Void> onFinished){
        reportsDbHelper.insertReport(report, onFinished);
    }
    public void insertReports(List<Report> reports, OnQueryFinished<Void> onFinished){
        reportsDbHelper.insertReports(reports, onFinished);
    }
    public void getAllReports(OnQueryFinished<Report> callback){
        reportsDbHelper.getAllReports(callback);
    }
    public void deleteReportById(int id){
        deleteReportById(id, null);
    }
    public void deleteReportById(int id, OnQueryFinished<Void> onFinished){
        reportsDbHelper.deleteReportById(id, onFinished);
    }
    public void deleteReportsById(List<Integer> ids){
        deleteReportsById(ids, null);
    }
    public void deleteReportsById(List<Integer> ids, OnQueryFinished<Void> onFinished){
        reportsDbHelper.deleteReportsById(ids, onFinished);
    }
    public void deleteAllReports(OnQueryFinished<Void> onFinished){
        reportsDbHelper.deleteAllReports(onFinished);
    }
}
