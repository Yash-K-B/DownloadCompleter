package com.yash.download.completer.webview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.yash.download.completer.WebViewActivity;

import java.util.Objects;

public class WebViewClientImp extends WebViewClient {
    private static final String TAG = "WebViewClientImp";
    WebViewActivity activity;
    public WebViewClientImp(WebViewActivity activity){
        this.activity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d(TAG,"new url : "+url);
//        if(Uri.parse(url).getHost().contains(Objects.requireNonNull(Uri.parse(view.getUrl()).getHost()))) return false;
//        Intent intent = new Intent(activity,WebViewActivity.class);
//        intent.putExtra("webUrl",url);
//        activity.startActivity(intent);
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Log.d(TAG,"new url : "+request.getUrl().toString());
        Intent intent = new Intent(activity,WebViewActivity.class);
        intent.putExtra("webUrl",request.getUrl().toString());
        activity.startActivity(intent);
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        Log.d(TAG,"Page Loading Started");
        activity.webViewBinding.toolbarText.setText(url);
        activity.webViewBinding.progressBarWrapper.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.d(TAG,"Page Loading Finished");
        activity.webViewBinding.progressBarWrapper.setVisibility(View.INVISIBLE);
    }


}
