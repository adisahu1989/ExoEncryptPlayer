package com.example.exoencryptplayer.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.exoencryptplayer.BackgroundNotificationService;
import com.example.exoencryptplayer.EncryptedFileDataSourceFactory;
import com.example.exoencryptplayer.ExoApplication;
import com.example.exoencryptplayer.NetworkChangeReceiver;
import com.example.exoencryptplayer.R;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.UUID;

public class VideoDownloadActivity extends AppCompatActivity implements NetworkChangeReceiver.ConnectivityReceiverListener {
    public static final String PROGRESS_UPDATE = "progress_update";
    private static final int PERMISSION_REQUEST_CODE = 1;
    ImageView fab;
    private SimpleExoPlayerView mSimpleExoPlayerView;
    private ProgressBar mProgressBar;
    private SimpleExoPlayer player;
    private boolean isButtonClick;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_download);
        mSimpleExoPlayerView = findViewById(R.id.simpleexoplayerview);
        mProgressBar = findViewById(R.id.progress_bar);
        // internet connection status listener
        ExoApplication.getInstance().setConnectivityListener(this);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    isButtonClick = true;
                    startImageDownload();
                } else {
                    requestPermission();
                }
            }
        });
        registerReceiver();
    }

    private void registerReceiver() {
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(PROGRESS_UPDATE)) {

                boolean downloadComplete = intent.getBooleanExtra("downloadComplete", false);
                //Log.d("API123", download.getProgress() + " current progress");
                if (downloadComplete) {

                    Toast.makeText(getApplicationContext(), "File download completed", Toast.LENGTH_SHORT).show();
                   String fileN = "testing_video"+ ".mp4";
                    File filename = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/UlurnVideo/", fileN);
                    initializePlayer(filename.toString());
                }
            }
        }
    };
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void startImageDownload() {
        if(!isMyServiceRunning(this,BackgroundNotificationService.class)) {
            Intent intent = new Intent(this, BackgroundNotificationService.class);
            BackgroundNotificationService.enqueueWork(getApplicationContext(), intent);
        }
        //startService(intent);
    }

    private void initializePlayer(String filePath) {
        player = ExoPlayerFactory.newSimpleInstance(this,
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());

        Log.e("filepath", filePath);
        Uri uri = Uri.parse(filePath);

        ExtractorMediaSource audioSource = new ExtractorMediaSource(
                uri,
                new DefaultDataSourceFactory(this, "MyExoplayer"),
                new DefaultExtractorsFactory(),
                null,
                null
        );

        player.prepare(audioSource);
        mSimpleExoPlayerView.setPlayer(player);
        player.setPlayWhenReady(true);
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImageDownload();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    public static boolean isMyServiceRunning(Activity activity, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if(isConnected)
        {
            startImageDownload();
        }
        else {
           cancelNotification();
        }
    }

    private void cancelNotification()
    {
        String fileN = "testing_video"+ ".mp4";
        File filename = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/UlurnVideo/", fileN);
        if(isMyServiceRunning(this,BackgroundNotificationService.class))
        {
            filename.delete();
        }
        NotificationManagerCompat notificationCompat = NotificationManagerCompat.from(getApplicationContext());

        notificationCompat.cancel(0);
    }

    @Override
    protected void onDestroy() {
        Log.d("VideoDownloadActivity","On Destroy call");
        cancelNotification();
        super.onDestroy();
    }
}