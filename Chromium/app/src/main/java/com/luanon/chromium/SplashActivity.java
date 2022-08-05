package com.luanon.chromium;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.Display;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {

	private Intent mIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIntent = getIntent();
		if (!isTaskRoot()
			&& mIntent.hasCategory(Intent.CATEGORY_LAUNCHER)
			&& mIntent.getAction() != null
			&& mIntent.getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }
		Uri data = mIntent.getData();
		if (data != null) {
			try {
				JSONObject dataObj = new JSONObject(data.toString());
				if (dataObj.get("pip_mode").toString().equals("true")) {
					enterPipMode();
				}
			} catch (JSONException ex) {}
		}
		mIntent = new Intent(this, MainActivity.class);
		mIntent.setData(data);
		startActivity(mIntent);
		finish();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void enterPipMode() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Rational rational = new Rational(4, 5);
			PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
			builder.setAspectRatio(rational);
			this.enterPictureInPictureMode(builder.build());
		} else {
			Toast.makeText(getApplicationContext(), "Your device not support", Toast.LENGTH_LONG).show();
		}
	}

}
