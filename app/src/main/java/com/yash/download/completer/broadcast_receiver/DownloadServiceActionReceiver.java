package com.yash.download.completer.broadcast_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yash.download.completer.DownloadService;
import com.yash.download.completer.keystore.Keys;


public class DownloadServiceActionReceiver extends BroadcastReceiver {
    public static boolean pauseDownload = false;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra(Keys.EXTRA_ACTION) != null && intent.getStringExtra(Keys.EXTRA_ACTION).equals("pause"))
            pauseDownload = true;
        else if(intent.getStringExtra(Keys.EXTRA_ACTION) != null && intent.getStringExtra(Keys.EXTRA_ACTION).equals("play"))
            DownloadService.enqueueWork(context,intent);
    }
}
