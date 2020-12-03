package com.yash.download.completer;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.yash.download.completer.broadcast_receiver.DownloadServiceActionReceiver;
import com.yash.download.completer.converter.ConverterUtil;
import com.yash.download.completer.keystore.Keys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yash.download.completer.keystore.Keys.CHANNEL_ID;
import static com.yash.download.completer.keystore.Keys.EXTRA_ACTION;

public class DownloadService extends JobIntentService {
    private static final String TAG = "DownloadService";
    public static final int NOTIFICATION_ID = 2;

    ExecutorService executor;
    NotificationManager manager;
    boolean stopDownloading;

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, DownloadService.class, JOB_ID, work);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newFixedThreadPool(5);
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        stopDownloading = false;
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleIntent: ");
        work(intent);

    }

    private void work(Intent intent) {

        Uri downloadLocation = intent.getData();
        String refreshLink = intent.getStringExtra(Keys.EXTRA_REFRESH_LINK);

        Log.d(TAG, "Location : " + downloadLocation + "\nRefresh Link : " + refreshLink);
        if (downloadLocation == null || refreshLink == null) return;

        DownloadServiceActionReceiver.pauseDownload = false;

        Intent pauseIntent = new Intent(this, DownloadServiceActionReceiver.class);
        pauseIntent.putExtra(Keys.EXTRA_ACTION, "pause");
        pauseIntent.putExtra(Keys.EXTRA_JOB_ID, JOB_ID);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(DownloadService.this, 10, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent cancelIntent = new Intent(this, DownloadServiceActionReceiver.class);
        cancelIntent.putExtra(Keys.EXTRA_ACTION, "cancel");
        cancelIntent.putExtra(Keys.EXTRA_JOB_ID, JOB_ID);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(DownloadService.this, 11, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(DownloadService.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Just Relax, I am working")
                .setContentText("Extracting file details")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
                .addAction(R.drawable.ic_close, "Cancel", cancelPendingIntent);

        manager.notify(NOTIFICATION_ID, builder.build());
        long contentLength = 0;
        OutputStream outputStream = null;
//                for (int i = 0; i < 100; i++) {
//                    try {
//                        Thread.sleep(1000);
//                        Log.d(TAG," i : "+i);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
        try {
            if (downloadLocation.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(downloadLocation, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        getContentResolver().takePersistableUriPermission(downloadLocation, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        contentLength = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                        outputStream = getContentResolver().openOutputStream(downloadLocation, "wa");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                File downloadFile = new File(downloadLocation.toString());
                contentLength = downloadFile.length();
                outputStream = new FileOutputStream(downloadFile, true);
            }


            Log.d(TAG, "downloaded length: " + contentLength);
            URL url = new URL(refreshLink);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range", "bytes=" + contentLength + "-");
            connection.connect();

            String contentDisposition = connection.getHeaderField("Content-Disposition");
            String fileName = URLUtil.guessFileName(refreshLink, contentDisposition, null);
            builder.setContentTitle(fileName);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200 && contentLength != 0) {
                Toast.makeText(this, "Resume Not Supported", Toast.LENGTH_SHORT).show();
                return;
            } else if (responseCode == 416) {
                builder.setOngoing(false);
                builder.setProgress(0, 0, false);
                builder.setSmallIcon(R.drawable.ic_done_24);
                builder.setContentText("Download Completed");
                manager.notify(NOTIFICATION_ID, builder.build());
                return;
            }
            //int fileBytes = connection.getContentLength();

//            Pattern fileNamePattern = Pattern.compile("/([^/]+)\\?");
//            Matcher fileNameMatcher = fileNamePattern.matcher(refreshLink);
//            if (fileNameMatcher.find())
//                builder.setContentTitle(fileNameMatcher.group(1));
//            else {
//
//                if (contentDisposition != null) {
//                    String title = contentDisposition.split("=")[1];
//                    builder.setContentTitle(title != null ? title.replace("\"", "") : null);
//                }
//
//            }
            long fileLength = (connection.getContentLength() + contentLength);
            Log.d(TAG, "from server length:" + connection.getContentLength());
            InputStream stream = connection.getInputStream();
            byte[] bytes = new byte[1024];
            int count;
            long total = contentLength;
            long start = System.currentTimeMillis(), end;
            String fullSize = ConverterUtil.bytesToHighest(fileLength);
            while ((count = stream.read(bytes)) != -1) {
                total += count;
                outputStream.write(bytes, 0, count);
                end = System.currentTimeMillis();
                if (end >= start + 1000) {
                    start = end;
                    if (DownloadServiceActionReceiver.pauseDownload) break;
                    int percent = (int) ((total * 100) / fileLength);
                    Log.d(TAG, "download progress: " + percent + "%");
                    builder.setProgress(100, percent, false);
                    builder.setContentText(ConverterUtil.bytesToHighest(total) + " / " + fullSize);
                    manager.notify(NOTIFICATION_ID, builder.build());
                }
            }

            outputStream.flush();
            outputStream.close();
            builder.setOngoing(false);

            if (total >= fileLength) {
                Log.d(TAG, "File Downloaded");
                builder.setSmallIcon(R.drawable.ic_done_24);
                builder.setProgress(0, 0, false);
                builder.setContentText("Download Completed");
                builder.mActions.clear();
            } else {
                Log.d(TAG, "File Paused");
                builder.setSmallIcon(R.drawable.ic_download);
                builder.setProgress(0, 0, false);
                builder.setContentText("Download Paused");
                Intent playIntent = new Intent(this, DownloadServiceActionReceiver.class);
                playIntent.putExtra(Keys.EXTRA_REFRESH_LINK, refreshLink);
                playIntent.setData(downloadLocation);
                playIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                playIntent.putExtra(EXTRA_ACTION, "play");
                PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 12, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.mActions.set(0, new NotificationCompat.Action(R.drawable.ic_play, "Continue", playPendingIntent));
            }
            manager.notify(NOTIFICATION_ID, builder.build());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
