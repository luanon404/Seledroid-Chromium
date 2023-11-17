package com.luanon.seledroid.chromium;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (!isTaskRoot() && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && Intent.ACTION_MAIN.equals(intent.getAction())) {
            finish();
            return;
        }

        Uri uriData = intent.getData();
        if (uriData != null) {
            String stringData = uriData.toString();
            if (stringData.startsWith("webdriver")) {
                String dataParameter = extractDataParameter(stringData);
                if (dataParameter != null) {
                    stringData = base64Decode(dataParameter);
                }
            }
            try {
                JSONObject jsonData = new JSONObject(stringData);
                Intent targetIntent = jsonData.optBoolean("state") ?
                        new Intent(this, MainActivity.class) : new Intent(this, MainService.class);
                targetIntent.setData(uriData);
                if (jsonData.optBoolean("state")) {
                    targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    targetIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    startActivity(targetIntent);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(targetIntent);
                    } else {
                        startService(targetIntent);
                    }
                }
            } catch (JSONException ignored) {
            }
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }

    private String extractDataParameter(String url) {
        int dataParamStartIndex = url.indexOf("data=");
        if (dataParamStartIndex != -1) {
            String dataParameterValue = url.substring(dataParamStartIndex + 5);
            try {
                return URLDecoder.decode(dataParameterValue, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        return null;
    }

    private String base64Decode(String data) {
        return new String(Base64.decode(data.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
    }

}