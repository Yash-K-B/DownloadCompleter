package com.yash.download.completer.webview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.yash.download.completer.R;
import com.yash.download.completer.WebViewActivity;

public class WebChromeClientImp extends WebChromeClient {
    private static final String TAG = "WebChromeClientImp";
    WebViewActivity activity;
    RelativeLayout childLayout;
    TextView titleText;
    WebView browserLayout;

    public WebChromeClientImp(WebViewActivity activity) {
        this.activity = activity;
        browserLayout = activity.webViewBinding.mainBrowserLayout;
        childLayout = activity.webViewBinding.mainAdChildLayout;
        titleText = activity.webViewBinding.toolbarText;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        // remove any current child views
        browserLayout.removeAllViews();
        // make the child web view's layout visible
        childLayout.setVisibility(View.VISIBLE);
        activity.webViewBinding.auxToolbar.setVisibility(View.VISIBLE);
        activity.webViewBinding.secondTitle.setText(view.getUrl());

        // now create a new web view
        WebView newView = new WebView(activity);
        WebSettings settings = newView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setUseWideViewPort(false);
        newView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //prevText = titleText.getText().toString();
                activity.webViewBinding.secondTitle.setText(view.getUrl());
                activity.webViewBinding.progressBarWrapperAux.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                activity.webViewBinding.progressBarWrapperAux.setVisibility(View.INVISIBLE);
            }

        });
        newView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                activity.webViewBinding.progressBarAux.setProgress(newProgress);
            }

        });
        newView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Log.d(TAG, "Aux download");
                activity.setResultAndFinish(url);
            }
        });
        activity.webViewBinding.auxRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                newView.loadUrl(newView.getUrl());
                activity.webViewBinding.auxRefresh.setRefreshing(false);
            }
        });

        //newView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        for (int i = 0; i < newView.getChildCount(); i++) {
//            browserLayout.addView(newView.getChildAt(i));
//        }
        browserLayout.addView(newView.getRootView());

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newView);
        resultMsg.sendToTarget();

        Animation slideUp = AnimationUtils.loadAnimation(activity, R.anim.slide_up);
        childLayout.startAnimation(slideUp);
        return true;
    }

    /**
     * Lower the child web view down
     */
    public void closeChild() {
        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.slide_down);
        childLayout.startAnimation(slideDown);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // titleText.setText(prevText);
                childLayout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public boolean isChildOpen() {
        return childLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        activity.webViewBinding.progressBar.setProgress(newProgress);
    }
}
