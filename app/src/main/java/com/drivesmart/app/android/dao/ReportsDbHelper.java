package com.drivesmart.app.android.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import com.drivesmart.app.android.model.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.drivesmart.app.android.dao.DriveSmartContract.*;

/**
 * Created by omiwrench on 2016-01-04.
 */
class ReportsDbHelper extends SQLiteOpenHelper{
    private static final int DELETE_ALL_REPORTS_FLAG = -1;

    public ReportsDbHelper(Context context, int databaseVersion, String databaseName){
        super(context, databaseName, null, databaseVersion);
    }

    public void onCreate(SQLiteDatabase database){
        String createSQL = DriveSmartContract.ReportsEntry.SQL_CREATE_ENTRIES;
        database.execSQL(createSQL);
    }
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        String deleteSQL = DriveSmartContract.ReportsEntry.SQL_DELETE_ENTRIES;
        database.execSQL(deleteSQL);
        onCreate(database);
    }

    public void insertReport(Report report, OnQueryFinished<Void> onFinished){
        List<Report> reports = new ArrayList<>();
        reports.add(report);
        insertReports(reports, onFinished);
    }
    public void insertReports(List<Report> reports, OnQueryFinished<Void> onFinished){
        InsertReportsTask insertTask = new InsertReportsTask(onFinished);
        insertTask.execute(reports);
    }
    public void getAllReports(OnQueryFinished<Report> onFinished){
        QueryReportsTask queryTask = new QueryReportsTask(onFinished);
        queryTask.execute();
    }
    public void deleteReportById(int id, OnQueryFinished<Void> onFinished){
        List<Integer> ids = new ArrayList<>();
        ids.add(id);
        deleteReportsById(ids, onFinished);
    }
    public void deleteReportsById(List<Integer> ids, OnQueryFinished<Void> onQueryFinished){
        DeleteReportsTask deleteTask = new DeleteReportsTask(onQueryFinished);
        deleteTask.execute(ids);
    }
    public void deleteAllReports(OnQueryFinished<Void> onFinished){
        DeleteReportsTask deleteTask = new DeleteReportsTask(onFinished);
        List<Integer> ids = Arrays.asList(DELETE_ALL_REPORTS_FLAG);
        deleteTask.execute(ids);
    }

    /**
     * @return returns the row ID of the newly created row
     */
    private long dbInsertReport(Report report){
        SQLiteDatabase database = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ReportsEntry.COLUMN_NAME_REPORT_ID, report.getId());
        values.put(ReportsEntry.COLUMN_NAME_TITLE, report.getTitle());
        values.put(ReportsEntry.COLUMN_NAME_DESCRIPTION, report.getDescription());
        values.put(ReportsEntry.COLUMN_NAME_LOCATION, report.getLocation());
        values.put(ReportsEntry.COLUMN_NAME_CREATED_AT, report.getCreatedAtAsString());

        long newRowId = database.insert(ReportsEntry.TABLE_NAME, "null", values);
        return newRowId;
    }
    private List<Long> dbInsertReports(List<Report> reports){
        List<Long> ids = new ArrayList<>();
        for(Report report : reports){
            ids.add(dbInsertReport(report));
        }
        return ids;
    }
    private List<Report> dbQueryAllReports(){
        SQLiteDatabase database = getReadableDatabase();
        String[] projection = {
                ReportsEntry.COLUMN_NAME_REPORT_ID,
                ReportsEntry.COLUMN_NAME_TITLE,
                ReportsEntry.COLUMN_NAME_DESCRIPTION,
                ReportsEntry.COLUMN_NAME_LOCATION,
                ReportsEntry.COLUMN_NAME_CREATED_AT
        };
        String sortOrder = ReportsEntry.COLUMN_NAME_REPORT_ID + " DESC";

        Cursor cursor = database.query(ReportsEntry.TABLE_NAME, projection, null, null, null, null, sortOrder);
        List<Report> reports = extractReportsFromCursor(cursor);
        return reports;
    }
    private void dbDeleteReportsById(List<Integer> ids){
        for(int i : ids){
            if(i == DELETE_ALL_REPORTS_FLAG){
                dbDeleteAllReports();
                return;
            }
        }
        SQLiteDatabase database = getWritableDatabase();
        String selection = ReportsEntry.COLUMN_NAME_REPORT_ID + " IN(" + new String(new char[ids.size()-1]).replace("\0", "?,") + "?)";
        String[] selectionArgs = new String[ids.size()];
        for(int i = 0; i < ids.size(); i++){
            selectionArgs[i] = String.valueOf(ids.get(i));
        }
        database.delete(ReportsEntry.TABLE_NAME, selection, selectionArgs);
    }
    private void dbDeleteAllReports(){
        SQLiteDatabase database = getWritableDatabase();
        database.execSQL("DELETE FROM " + ReportsEntry.TABLE_NAME);
    }

    private List<Report> extractReportsFromCursor(Cursor cursor){
        List<Report> reports = new ArrayList<>();
        while(cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(ReportsEntry.COLUMN_NAME_REPORT_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(ReportsEntry.COLUMN_NAME_TITLE));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(ReportsEntry.COLUMN_NAME_DESCRIPTION));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(ReportsEntry.COLUMN_NAME_LOCATION));
            String createdAtString = cursor.getString(cursor.getColumnIndexOrThrow(ReportsEntry.COLUMN_NAME_CREATED_AT));

            Report report = new Report(id, title, description, location, createdAtString);
            reports.add(report);
        }
        return reports;
    }

    private class InsertReportsTask extends AsyncTask<List<Report>, Integer, List<Long>>{
        private OnQueryFinished callback;

        InsertReportsTask(OnQueryFinished<Void> callback){
            this.callback = callback;
        }
        @Override
        protected List<Long> doInBackground(List<Report>... reports) {
            return dbInsertReports(reports[0]);
        }
        @Override
        protected void onPostExecute(List<Long> ids) {
            callback.onSuccess(ids);
        }
    }
    private class QueryReportsTask extends AsyncTask<Void, Integer, List<Report>>{
        private OnQueryFinished<Report> callback;

        QueryReportsTask(OnQueryFinished<Report> callback){
            this.callback = callback;
        }
        @Override
        protected List<Report> doInBackground(Void... params) {
            return dbQueryAllReports();
        }
        @Override
        protected void onPostExecute(List<Report> reports) {
            callback.onSuccess(reports);
        }
    }
    private class DeleteReportsTask extends AsyncTask<List<Integer>, Integer, Void>{
        private OnQueryFinished<Void> callback;

        DeleteReportsTask(){}
        DeleteReportsTask(OnQueryFinished<Void> callback){
            this.callback = callback;
        }
        @Override
        protected Void doInBackground(List<Integer>... ids) {
            dbDeleteReportsById(ids[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(callback != null) {
                callback.onSuccess(null);
            }
        }
    }
}
