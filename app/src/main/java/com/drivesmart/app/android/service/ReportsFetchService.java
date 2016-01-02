package com.drivesmart.app.android.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.drivesmart.app.android.R;
import com.drivesmart.app.android.model.Report;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by omiwrench on 2016-01-02.
 */
public class ReportsFetchService {
    private static final String TAG = ReportsFetchService.class.getName();

    public interface OnFetchFinished{
        void onSuccess(List<Report> reports);
        void onError(String error);
    }

    private Context context;

    public ReportsFetchService(Context context){
        this.context = context;
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
        queue.add(request);
    }

    private List<Report> parseReportsJson(String json) throws IllegalArgumentException{
        try{
            List<Report> reports = new ArrayList<>();
            JSONArray jsonReports = new JSONArray(json);
            for(int i = 0; i < jsonReports.length(); i++){
                JSONObject report = (JSONObject) jsonReports.get(i);
                String title = report.getString("title");
                String description = report.getString("description");
                String location = report.getString("location");
                String timeString = report.getString("createdAt");
                reports.add(new Report(title, description, location, timeString));
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
        url = url.replaceAll("\\s+","%");
        Log.d(TAG, url);
        return url;
    }
}
