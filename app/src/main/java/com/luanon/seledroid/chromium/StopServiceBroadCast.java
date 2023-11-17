package com.luanon.seledroid.chromium;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class StopServiceBroadCast extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getExtras().getString("action", "exit");
		if (action.equals("exit")) {
			Intent service = new Intent(context, MainService.class);
			context.stopService(service);
		}
	}

}
