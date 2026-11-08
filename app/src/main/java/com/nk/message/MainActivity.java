package com.nk.message;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private WebView webView;
    private PermissionRequest pendingRequest;
    private ValueCallback<Uri[]> fileCallback;

    private static final int WEB_PERMISSION_CODE = 101;
    private static final int NOTIFICATION_CODE = 102;
    private static final int FILE_CHOOSER_CODE = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_CODE
            );
        }

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                try {
                    startActivityForResult(
                        Intent.createChooser(intent, "Select Profile Photo"),
                        FILE_CHOOSER_CODE
                    );
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }

                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {

                if (!"https://nk-message.onrender.com".equals(
                    request.getOrigin().toString().replaceAll("/$", "")
                )) {
                    request.deny();
                    return;
                }

                if (pendingRequest != null) {
                    request.deny();
                    return;
                }

                List<String> permissions = new ArrayList<>();

                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                        && checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.CAMERA);
                    }

                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.RECORD_AUDIO);
                    }
                }

                if (permissions.isEmpty()) {
                    request.grant(request.getResources());
                } else {
                    pendingRequest = request;
                    requestPermissions(
                        permissions.toArray(new String[0]),
                        WEB_PERMISSION_CODE
                    );
                }
            }
        });

        webView.loadUrl("https://nk-message.onrender.com/");
        setContentView(webView);
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_CODE && fileCallback != null) {
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == WEB_PERMISSION_CODE && pendingRequest != null) {
            List<String> allowed = new ArrayList<>();

            for (String resource : pendingRequest.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                    && checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    allowed.add(resource);
                }

                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                    allowed.add(resource);
                }
            }

            if (!allowed.isEmpty()) {
                pendingRequest.grant(allowed.toArray(new String[0]));
            } else {
                pendingRequest.deny();
            }

            pendingRequest = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
