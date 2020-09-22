package com.example.exoencryptplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Objects;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class NetworkChangeReceiver extends BroadcastReceiver {
  public static ConnectivityReceiverListener connectivityReceiverListener;

  @Override
  public void onReceive(Context context, Intent intent) {
    boolean result = isConnected();
    if (!result) {
      if (connectivityReceiverListener != null) {
        connectivityReceiverListener.onNetworkConnectionChanged(result);
      }
    }
  }
  public static boolean isConnected() {
    ConnectivityManager
            cm = (ConnectivityManager) ExoApplication.getInstance().getApplicationContext()
            .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null
            && activeNetwork.isConnectedOrConnecting();
  }
  public interface ConnectivityReceiverListener {
    void onNetworkConnectionChanged(boolean isConnected);
  }
}
