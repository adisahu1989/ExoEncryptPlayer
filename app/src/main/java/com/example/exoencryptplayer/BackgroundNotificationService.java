package com.example.exoencryptplayer;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.exoencryptplayer.activity.VideoDownloadActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;

public class BackgroundNotificationService extends JobIntentService {
    private static final String TAG = "BackgroundNotificationService";
    private ResponseBody responseBody;
    /**
     * Unique job ID for this service.
     */
    private static final int JOB_ID = 102;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, BackgroundNotificationService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("id", "an", NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription("no sound");
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        notificationBuilder = new NotificationCompat.Builder(this, "id")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Download")
                .setContentText("Downloading Image")
                .setDefaults(0)
                .setAutoCancel(true);
        notificationManager.notify(0, notificationBuilder.build());
        initRetrofit();

    }

    private void initRetrofit() {
        // String urlString = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.learningcontainer.com/")
                .build();
        RetrofitInterface retrofitInterface = retrofit.create(RetrofitInterface.class);
        Call<ResponseBody> request = retrofitInterface.downloadImage("wp-content/uploads/2020/05/sample-mp4-file.mp4");
        try {
            responseBody = request.execute().body();
            downloadVideo(responseBody);
        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadVideo(ResponseBody body) throws IOException {
        int count;
        byte data[] = new byte[1024 * 4];
        long fileSize = body.contentLength();
        InputStream inputStream = new BufferedInputStream(body.byteStream(), 1024 * 8);
        String fileN = "testing_video" + ".mp4";
        File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/UlurnVideo/", fileN);
        //File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "journaldev-image-downloaded.jpg");
        OutputStream outputStream = new FileOutputStream(outputFile);
        long total = 0;
        boolean downloadComplete = false;
        //int totalFileSize = (int) (fileSize / (Math.pow(1024, 2)));
        while ((count = inputStream.read(data)) != -1) {
            if(NetworkChangeReceiver.isConnected()) {
                total += count;
                int progress = (int) ((double) (total * 100) / (double) fileSize);
                updateNotification(progress);
                outputStream.write(data, 0, count);
                downloadComplete = true;
            }else {
                notificationManager.cancel(0);
                if(outputFile.exists())
                {
                    outputFile.delete();
                }
                return;
            }
        }
        onDownloadComplete(downloadComplete);
        outputStream.flush();
        outputStream.close();
        inputStream.close();

    }

    private void updateNotification(int currentProgress) {
        notificationBuilder.setProgress(100, currentProgress, false);
        notificationBuilder.setContentText("Downloaded: " + currentProgress + "%");
        notificationManager.notify(0, notificationBuilder.build());
    }

    private void sendProgressUpdate(boolean downloadComplete) {
        Intent intent = new Intent(VideoDownloadActivity.PROGRESS_UPDATE);
        intent.putExtra("downloadComplete", downloadComplete);
        LocalBroadcastManager.getInstance(BackgroundNotificationService.this).sendBroadcast(intent);
    }

    private void onDownloadComplete(boolean downloadComplete) {
        sendProgressUpdate(downloadComplete);
        notificationManager.cancel(0);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("Image Download Complete");
        notificationManager.notify(0, notificationBuilder.build());

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        notificationManager.cancel(0);
    }
    
}
