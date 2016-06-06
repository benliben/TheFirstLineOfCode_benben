package com.example.benben.thefirstlineofcode_benben.util;

/**
 * Created by benebn on 2016/6/1.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
