package com.example.qrcodescanner;

import android.app.Application;

public class MyApp extends Application {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
