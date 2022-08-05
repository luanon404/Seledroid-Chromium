package com.luanon.chromium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DebugActivity extends AppCompatActivity {

	String[] exceptionType = {
		"StringIndexOutOfBoundsException",
		"IndexOutOfBoundsException",
		"ArithmeticException",
		"NumberFormatException",
		"ActivityNotFoundException"
	};

	String[] errMessage = {
		"Invalid string operation\n",
		"Invalid list operation\n",
		"Invalid arithmetical operation\n",
		"Invalid toNumber block operation\n",
		"Invalid intent operation"
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String errMsg = "";
		String madeErrMsg = "";
		if (intent != null) {
			errMsg = intent.getStringExtra("error");
			String[] spilt = errMsg.split("\n");
			try {
				for (int i = 0; i < exceptionType.length; i++) {
					if (spilt[0].contains(exceptionType[i])) {
						madeErrMsg = errMessage[i];
						int addIndex = spilt[0].indexOf(exceptionType[i]) + exceptionType[i].length();
						madeErrMsg += spilt[0].substring(addIndex, spilt[0].length());
						break;
					}
				}
				if (madeErrMsg.isEmpty()) madeErrMsg = errMsg;
			} catch (Exception e) {}
		}
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Error list");
		alertDialogBuilder.setMessage(madeErrMsg);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.setCancelable(false);
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.show();
		((TextView) alertDialog.getWindow().getDecorView().findViewById(android.R.id.message)).setTextIsSelectable(true);
    }

}
