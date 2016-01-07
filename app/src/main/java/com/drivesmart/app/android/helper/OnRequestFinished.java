package com.drivesmart.app.android.helper;

import com.drivesmart.app.android.model.Report;

import java.util.List;

/**
 * Created by omiwrench on 2016-01-07.
 */
public interface OnRequestFinished<T> {
    void onSuccess(List<T> results);
    void onError(String error);
}
