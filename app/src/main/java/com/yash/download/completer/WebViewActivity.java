package com.yash.download.completer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.yash.download.completer.databinding.ActivityWebViewBinding;
import com.yash.download.completer.webview.WebChromeClientImp;
import com.yash.download.completer.webview.WebViewClientImp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";
    public ActivityWebViewBinding webViewBinding;
    WebChromeClientImp webChromeClient;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webViewBinding = ActivityWebViewBinding.inflate(getLayoutInflater());
        setContentView(webViewBinding.getRoot());
        setSupportActionBar(webViewBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        webViewBinding.progressBar.setMax(100);



        Bundle extras = getIntent().getExtras();
        action = getIntent().getAction();

        if (extras != null) {
            String url = extras.getString("webUrl");
            WebSettings webSettings = webViewBinding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportMultipleWindows(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setAppCacheEnabled(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setSupportZoom(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setUseWideViewPort(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
            }

            Pattern urlPattern = Pattern.compile("(https?://)?(www\\.)?([A-za-z0-9_]+)(\\.[A-za-z0-9]+)");
            Matcher matcher = urlPattern.matcher(url);
            if (!matcher.find()) {
                url = url.replaceAll(" ", "+");
                url = "https://www.google.com/search?q=" + url;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
            webViewBinding.toolbarText.setText(url);
            webViewBinding.webView.setDownloadListener((url1, userAgent, contentDisposition, mimetype, contentLength) -> setResultAndFinish(url1));
            webViewBinding.webView.setWebViewClient(new WebViewClientImp(this));
            webChromeClient = new WebChromeClientImp(this);
            webViewBinding.webView.setWebChromeClient(webChromeClient);

            webViewBinding.close.setOnClickListener(v -> webChromeClient.closeChild());
            webViewBinding.refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    webViewBinding.webView.loadUrl(webViewBinding.webView.getUrl());
                    webViewBinding.refresh.setRefreshing(false);
                }
            });

            //load webPage
            webViewBinding.webView.loadUrl(url);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView webView = this.webViewBinding.webView;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webChromeClient.isChildOpen()) {
                    webChromeClient.closeChild();
                    return true;
                } else if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                setResult(Activity.RESULT_OK, new Intent(action));
                return super.onKeyDown(keyCode, event);
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    public void setResultAndFinish(String url){
        Intent intent = new Intent(action);
        intent.setData(Uri.parse(url));
        setResult(Activity.RESULT_OK,intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                WebView webView = this.webViewBinding.webView;
                if (webChromeClient.isChildOpen()) {
                    webChromeClient.closeChild();
                    return true;
                } else if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                setResult(Activity.RESULT_OK, new Intent(action));
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}