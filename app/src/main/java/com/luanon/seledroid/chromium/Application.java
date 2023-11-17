package com.luanon.seledroid.chromium;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = this;
        Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Intent intent = new Intent(context, ShowErrorActivity.class);
            intent.putExtra("errorMessage", getErrorMessage(throwable));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                if (defaultUEH != null) {
                    defaultUEH.uncaughtException(thread, throwable);
                }
            } else {
                System.exit(0);
            }
        });

    }

    private String getErrorMessage(Throwable throwable) {
        StringBuilder errorMessage = new StringBuilder();
        String packageName = getPackageName();
        errorMessage.append("<html><body>");
        errorMessage.append(String.format("<font color='#FFB6C1'><pre><strong> %s</strong></pre></font>", throwable));
        for (StackTraceElement element : throwable.getStackTrace()) {
            String elementString = String.format("<pre>    at %s</pre>", element);
            errorMessage.append(elementString.replace(packageName, String.format("<font color='#FFB6C1'><strong>%s</strong></font>", packageName)));
        }
        errorMessage.append(String.format("<font color='#FFB6C1'><strong><pre> Caused by: %s</pre></strong></font>", throwable.getCause()));
        for (StackTraceElement element : throwable.getCause().getStackTrace()) {
            String elementString = String.format("<pre>    at %s</pre>", element);
            errorMessage.append(elementString.replace(packageName, String.format("<font color='#FFB6C1'><strong>%s</strong></font>", packageName)));
        }
        errorMessage.append("</body></html>");
        return errorMessage.toString();
    }

}
