package com.luanon.chromium;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class MainApplication extends Application {

	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	@Override
	public void onCreate() {
		uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable throwable) {
					Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					intent.putExtra("error", getStackTrace(throwable));
					PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 11111, intent, PendingIntent.FLAG_ONE_SHOT);
					AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingIntent);
					android.os.Process.killProcess(android.os.Process.myPid());
					System.exit(2);
					uncaughtExceptionHandler.uncaughtException(thread, throwable);
				}
			});
		super.onCreate();
	}

	private String getStackTrace(Throwable throwable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		Throwable cause = throwable;
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		final String stacktraceAsString = result.toString();
		printWriter.close();
		return stacktraceAsString;
	}

}
