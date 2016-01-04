package com.drivesmart.app.android.dao;

import java.util.List;

/**
 * Created by omiwrench on 2016-01-04.
 */
public interface OnQueryFinished<T>{
    void onSuccess(List<T> results);
    void onError(Exception e);
}
