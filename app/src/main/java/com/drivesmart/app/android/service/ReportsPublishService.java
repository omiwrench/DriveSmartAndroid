package com.drivesmart.app.android.service;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.drivesmart.app.android.R;
import com.drivesmart.app.android.helper.OnRequestFinished;
import com.drivesmart.app.android.model.Report;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by omiwrench on 2016-01-07.
 */
public class ReportsPublishService {
    private static final String TAG = ReportsPublishService.class.getName();

    private Context context;

    public ReportsPublishService(Context context){
        this.context = context;
    }

    public void publishReport(final Report report, final OnRequestFinished<Void> onFinished){
        RequestQueue queue = Volley.newRequestQueue(context);

        String url = context.getResources().getString(R.string.url_publish_report);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "---------------Response");
                        onFinished.onSuccess(null);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onFinished.onError(error.getMessage());
                    }
                }){
            protected Map<String, String> getParams() throws AuthFailureError{
                Map<String, String> params = new HashMap<>();
                params.put("title", report.getTitle());
                params.put("location", report.getLocation());
                params.put("description", report.getDescription());
                return params;
            }
        };

        Log.d(TAG, "----------------Queueing");
        queue.add(request);
    }
}
