package com.luanon.seledroid.chromium;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ShowErrorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error_layout);
        Intent intent = getIntent();
        String errorMessage = intent.getStringExtra("errorMessage");
        WebView webview = findViewById(R.id.webview);
        webview.loadDataWithBaseURL(null, errorMessage, "text/html", "UTF-8", null);
    }

}
