package com.yash.download.completer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobScheduler;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.yash.download.completer.databinding.ActivityMainBinding;
import com.yash.download.completer.keystore.Keys;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yash.download.completer.keystore.Keys.CHANNEL_ID;
import static com.yash.download.completer.keystore.Keys.CHANNEL_NAME;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int PERMISSION_REQUEST_CODE = 1001;

    ActivityMainBinding mainBinding;
    Uri downloadLocationUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        setSupportActionBar(mainBinding.toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManagerCompat manager = NotificationManagerCompat.from(MainActivity.this);
            manager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
        initUI();


    }

    void initUI() {
        mainBinding.fileLocation.setOnClickListener(v -> {
            showFileChooser();
        });
        mainBinding.webUrl.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                EditText et = mainBinding.webUrl.getEditText();
                if(et!=null && !et.getText().toString().equals("")){
                    Intent webIntent = new Intent(MainActivity.this,WebViewActivity.class);
                    webIntent.putExtra("webUrl",et.getText().toString());
                    startActivityForResult(webIntent,DOWNLOAD_LINK_CODE);
                }
                else Toast.makeText(MainActivity.this, "Please enter url", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        mainBinding.generateLink.setOnClickListener(v -> {
            EditText et = mainBinding.webUrl.getEditText();
            if(et!=null && !et.getText().toString().equals("")){
                Intent webIntent = new Intent(MainActivity.this,WebViewActivity.class);
                webIntent.putExtra("webUrl",et.getText().toString());
                startActivityForResult(webIntent,DOWNLOAD_LINK_CODE);
            }
            else Toast.makeText(MainActivity.this, "Please enter url", Toast.LENGTH_SHORT).show();
        });
        mainBinding.download.setOnClickListener(v -> {
            Log.d(TAG, "On Download");
            String refreshLink = mainBinding.refreshedLink.getEditText().getText().toString();
            Intent intent = new Intent(MainActivity.this, DownloadService.class);
            intent.setData(downloadLocationUri);
            intent.putExtra(Keys.EXTRA_REFRESH_LINK, refreshLink);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            DownloadService.enqueueWork(MainActivity.this,intent);
//            new Thread() {
//                @Override
//                public void run() {
//                    super.run();
//                    if (!refreshLink.isEmpty()) {
//                        URL url;
//                        String fileName = "";
//                        try {
//                            url = new URL(refreshLink);
//                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                            Pattern fileNamePattern = Pattern.compile("/([^/]+)\\?");
//                            Matcher fileNameMatcher = fileNamePattern.matcher(refreshLink);
//                            if (fileNameMatcher.find())
//                                fileName = fileNameMatcher.group(1);
//                            else
//                                fileName = connection.getHeaderField("Content-Disposition");
//                            Log.d(TAG, "File Name : " + fileName);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }.start();

        });

        String hintUrl = "https://files1.mp3slash.xyz/stream/2718c3f6c865b22a71a80450eb0a97c5";
        String hintUrl2 = "http://dns2.vippendu.com/320k/159822/20323/Dil-Ko-Karaar-Aaya-(From-Sukoon)-Yasser-Desai,Neha-Kakkar.mp3";
        mainBinding.refreshedLink.getEditText().setText(hintUrl2);
    }

    private static final int FILE_SELECT_CODE = 0;
    private static final int DOWNLOAD_LINK_CODE = 1;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    downloadLocationUri = data.getData();
                    data.getData();
                    Log.d(TAG, "File Uri: " + downloadLocationUri);
                    String fileName = null;
                    if (downloadLocationUri != null && downloadLocationUri.getScheme().equals("content")) {
                        try (Cursor cursor = getContentResolver().query(downloadLocationUri, null, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (fileName == null) {
                        fileName = downloadLocationUri.getPath();
                        int cut = fileName.lastIndexOf('/');
                        if (cut != -1) {
                            fileName = fileName.substring(cut + 1);
                        }
                    }

                    //"https://download.wetransfer.com//eu2/e11046d4bfa79bcaec58050a980ae4fa20201102193803/3d3577ca266060a2bcdec59afa3991e2722122cf/JGNSKMHD%20%282020%29%20www.SkymoviesHD.nl%20720p%20HEVC%20HDRip%20Hindi%20DUbbed%20x265%20AAC.mkv?cf=y&token=eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MDQ0MTI1MTIsInVuaXF1ZSI6ImUxMTA0NmQ0YmZhNzliY2FlYzU4MDUwYTk4MGFlNGZhMjAyMDExMDIxOTM4MDMiLCJmaWxlbmFtZSI6IkpHTlNLTUhEICgyMDIwKSB3d3cuU2t5bW92aWVzSEQubmwgNzIwcCBIRVZDIEhEUmlwIEhpbmRpIERVYmJlZCB4MjY1IEFBQy5ta3YiLCJ3YXliaWxsX3VybCI6Imh0dHA6Ly9wcm9kdWN0aW9uLmJhY2tlbmQuc2VydmljZS5ldS13ZXN0LTEud3Q6OTI5Mi93YXliaWxsL3YxL3Nhcmthci8yYTgxODM0ZmI1Nzc1MDkzNTRhMDJhYTlmZDZmZTJkMGJiY2IwMWVkOTRjZjA2YTkzYzg1MDcyZWJjNjlhMDMyNjI4ZDdhOTIzOTZkZWZkZWRiNTVhZSIsImZpbmdlcnByaW50IjoiM2QzNTc3Y2EyNjYwNjBhMmJjZGVjNTlhZmEzOTkxZTI3MjIxMjJjZiIsImNhbGxiYWNrIjoie1wiZm9ybWRhdGFcIjp7XCJhY3Rpb25cIjpcImh0dHA6Ly9wcm9kdWN0aW9uLmZyb250ZW5kLnNlcnZpY2UuZXUtd2VzdC0xLnd0OjMwMDAvd2ViaG9va3MvYmFja2VuZFwifSxcImZvcm1cIjp7XCJ0cmFuc2Zlcl9pZFwiOlwiZTExMDQ2ZDRiZmE3OWJjYWVjNTgwNTBhOTgwYWU0ZmEyMDIwMTEwMjE5MzgwM1wiLFwiZG93bmxvYWRfaWRcIjoxMDYzOTUwNDk4M319In0.n9ct1O11k8KSwPoYNPG0_AeEhVcnv_Y_t8FU4Xzi7V8"

                    mainBinding.fileLocation.setText(fileName);
                }
                break;
            case DOWNLOAD_LINK_CODE:
                if(data.getData() == null) break;
                String refreshedLink = data.getData().toString();
                mainBinding.refreshedLink.getEditText().setText(refreshedLink);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        finish();
                }
                initUI();
                break;
        }
    }
}