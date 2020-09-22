package com.example.exoencryptplayer;

import android.app.Application;
import android.content.Context;

public class ExoApplication extends Application {
    public static ExoApplication exoApplication;
    private static volatile Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        exoApplication = this;
    }

    public static ExoApplication getInstance()
    {
        return exoApplication;
    }

    public void setConnectivityListener(NetworkChangeReceiver.ConnectivityReceiverListener listener) {
        NetworkChangeReceiver.connectivityReceiverListener = listener;
    }
}
