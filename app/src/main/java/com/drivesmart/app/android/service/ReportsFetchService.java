package com.drivesmart.app.android.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.drivesmart.app.android.R;
import com.drivesmart.app.android.helper.DatePatternHelper;
import com.drivesmart.app.android.model.Report;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by omiwrench on 2016-01-02.
 */
public class ReportsFetchService {
    private static final String TAG = ReportsFetchService.class.getName();

    public interface OnFetchFinished{
        void onSuccess(List<Report> reports);
        void onError(String error);
    }

    private static final int UPDATE_INTERVAL = 10; //seconds

    private Context context;
    private SharedPreferences sharedPreferences;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture updaterHandle;

    public ReportsFetchService(Context context){
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    public void startAutoUpdating(final OnFetchFinished callback){
        final Runnable updater = new Runnable() {
            @Override
            public void run() {
                getNewReports(callback);
            }
        };
        updaterHandle = scheduler.scheduleAtFixedRate(updater, 0, UPDATE_INTERVAL, TimeUnit.SECONDS);
    }
    public void cancelAutoUpdating(){
        if(updaterHandle != null){
            updaterHandle.cancel(true);
            updaterHandle = null;
        }
        else{
            Log.i(TAG, "Auto update already cancelled.");
        }
    }

    public void getNewReports(final OnFetchFinished callback){
        DateTime lastUpdate = getLastUpdate();
        getAllReportsSince(lastUpdate, callback);
        DateTime now = new DateTime();
        setLastUpdate(now);
    }
    public void getAllReportsSince(DateTime lastUpdate, final OnFetchFinished callback){
        String fetchUrl = context.getResources().getString(R.string.url_get_reports);
        fetchUrl = appendTimestampToUrl(fetchUrl, lastUpdate);
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest request = new StringRequest(Request.Method.GET, fetchUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    List<Report> reports = parseReportsJson(response);
                    callback.onSuccess(reports);
                } catch (IllegalArgumentException e) {
                    callback.onError("Request was successful but returned bad JSON.");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Reports fetch unsuccessful.");
            }
        });
        request.setShouldCache(false);
        queue.add(request);
    }

    private DateTime getLastUpdate(){
        DateTime defaultTime = new DateTime(0);
        String defaultString = DatePatternHelper.printForJSONFormat(defaultTime);
        String lastUpdateString = sharedPreferences.getString(context.getString(R.string.pref_last_update_key), defaultString);
        Log.d(TAG, "Last update: " + lastUpdateString);
        return DatePatternHelper.parseForJSONFormat(lastUpdateString);
    }
    private void setLastUpdate(DateTime lastUpdate){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String lastUpdateString = DateTimeFormat.forPattern(DatePatternHelper.JSON_DATE_TIME_FORMAT).print(lastUpdate);
        editor.putString(context.getString(R.string.pref_last_update_key), lastUpdateString);
        editor.commit();
    }

    private List<Report> parseReportsJson(String json) throws IllegalArgumentException{
        try{
            List<Report> reports = new ArrayList<>();
            JSONArray jsonReports = new JSONArray(json);
            for(int i = 0; i < jsonReports.length(); i++){
                JSONObject report = (JSONObject) jsonReports.get(i);
                int id = report.getInt("id");
                String title = report.getString("title");
                String description = report.getString("description");
                String location = report.getString("location");
                String timeString = report.getString("createdAt");
                reports.add(new Report(id, title, description, location, timeString));
            }
            return reports;
        }
        catch(JSONException e){
            Log.e(TAG, "Bad JSON passed to parseReportsJson. JSON: " + json);
            Log.e(TAG, e.getMessage());
            throw new IllegalArgumentException();
        }
    }
    private String appendTimestampToUrl(String url, DateTime timestamp){
        String timeString = DateTimeFormat.forPattern(Report.CREATED_AT_FORMAT).print(timestamp);
        url += "?after=" + timeString;
        url = url.replaceAll("\\s+","T");
        Log.d(TAG, url);
        return url;
    }
}
