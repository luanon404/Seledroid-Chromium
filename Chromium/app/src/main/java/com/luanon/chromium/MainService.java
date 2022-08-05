package com.luanon.chromium;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.Proxy;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Spliterator;
import org.json.JSONException;
import org.json.JSONObject;

public class MainService extends Service {

	private String CHANNEL_ID = "Notification";
	private JSONObject data;
	private String command;
	private String importantScript;
	private String overrideScript;
	private boolean taskDone = false;
	private boolean skipFlag = false;
	private boolean close = false;
	private int webViewLoaded = 0; // call any function can check when webview loaded
	private JSONObject webViewLoadedObject = new JSONObject();
	private WebView mWebView;
	private Thread thread;
    private CookieManager mCookieManager;
	private Map <String, String> extraHeaders = new HashMap<String, String>();
	private Socket mSocket;

	private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
				CHANNEL_ID,
				"Notification Channel",
				NotificationManager.IMPORTANCE_DEFAULT
            );
			serviceChannel.setSound(null, null);
			serviceChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

	public void createNotification(String content) {
		Intent notificationIntent = new Intent(this, StopServiceBroadCast.class);
		notificationIntent.putExtra("action", "exit");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		NotificationCompat.Action action = new NotificationCompat.Action.Builder(0, "Exit", pendingIntent).build();
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle("Chromium")
			.setContentText(content)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setContentIntent(pendingIntent)
			.addAction(action)
			.setAutoCancel(false)
			.setOngoing(true)
			.build();
        startForeground(1, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, final int startId) {
		createNotificationChannel();
		createNotification("INIT SUCCESSFULLY");

		if (intent.getData() != null) {
			try {
				data = new JSONObject(intent.getData().toString());
				Locale locale = new Locale(data.get("lang").toString());
				setLocale(locale);
			} catch (JSONException ex) {}
		}

        mCookieManager = CookieManager.getInstance();
		mWebView = new WebView(this);
		mWebView.setWebViewClient(new WebViewClient(){

				@Override
				public void onPageStarted(final WebView view, String url, Bitmap icon) {
					if (overrideScript != null) {
						view.evaluateJavascript(overrideScript, null);
					}
				}

				@Override
				public void onPageFinished(WebView view, String url) {
					super.onPageFinished(view, url);
					if (!webViewLoadedObject.has("onPageFinished")) {
						webViewLoaded += 1;
						runScriptAfterLoaded(view);
						try {
							webViewLoadedObject.put("onPageFinished", true);
						} catch (JSONException ex) {}
					}
				}

				@Override
				public void onLoadResource(final WebView view, String url) {
					super.onLoadResource(view, url);
					if (overrideScript != null) {
						view.evaluateJavascript(overrideScript, null);
					}
				}

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					loadUrl(view, url);
					return true;
				}

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    for (String key : request.getRequestHeaders().keySet()) {
                        switch (key.toLowerCase()) {
                            case "x-requested-with":
                                continue;
                            case "user-agent":
                                continue;
                        }
                        extraHeaders.put(title(key), request.getRequestHeaders().get(key));
                    }
                    return super.shouldInterceptRequest(view, request);
                }

			});
		mWebView.setWebChromeClient(new WebChromeClient(){
				@Override
				public void onProgressChanged(final WebView view, int process) {
					if (process == 100) {
						if (!webViewLoadedObject.has("onProgressChanged")) {
							webViewLoaded += 1;
							runScriptAfterLoaded(view);
							try {
								webViewLoadedObject.put("onProgressChanged", true);
							} catch (JSONException ex) {}
						}
					}
				}
			});
		mWebView.setPictureListener(new WebView.PictureListener() {
				@Override
				public void onNewPicture(final WebView view, Picture picture) {
					if (!webViewLoadedObject.has("onNewPicture")) {
						webViewLoaded += 1;
						runScriptAfterLoaded(view);
						try {
							webViewLoadedObject.put("onNewPicture", true);
						} catch (JSONException ex) {}
					}
				}
			});
		mWebView.addJavascriptInterface(this, "android");
		mWebView.setFocusable(true);
		mWebView.setFocusableInTouchMode(true);
		mWebView.setScrollbarFadingEnabled(true);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.getSettings().setAllowContentAccess(true);
		mWebView.getSettings().setBuiltInZoomControls(false);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setUseWideViewPort(false);
		mWebView.getSettings().setDomStorageEnabled(true);
		mWebView.getSettings().setDatabasePath("/data/data/" + this.getPackageName() + "/database/");
		mWebView.getSettings().setGeolocationEnabled(true);
		mWebView.getSettings().setDatabaseEnabled(true);
		mWebView.getSettings().setSaveFormData(false);
		mWebView.getSettings().setSavePassword(false);
		mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		mWebView.getSettings().setLightTouchEnabled(true);
		mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
		mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);

		mCookieManager.setAcceptThirdPartyCookies(mWebView, true);

        extraHeaders.put("X-Requested-With", "XMLHttpRequest"); // cant remove only can override
		extraHeaders.put("User-Agent", mWebView.getSettings().getUserAgentString()); // fix cant change user agent

		if (intent.getData() == null) {
			loadUrl(mWebView, "https://google.com");
		} else {
			try {
				data = new JSONObject(intent.getData().toString());
				command = data.get("command").toString();
			} catch (JSONException ex) {
				Toast.makeText(getApplicationContext(), "Error parsing JSONObject", Toast.LENGTH_LONG).show();
			}

			if (command.equals("init")) {
				final Handler handler = new Handler();
			    thread = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								String host = data.get("host").toString();
								int port = data.getInt("port");
								mSocket = new Socket(host, port);
								OutputStream output = mSocket.getOutputStream();
								final PrintWriter writer = new PrintWriter(output, true);
								InputStream input = mSocket.getInputStream();
								final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
								final String response;
								while ((response = reader.readLine()) != null) {
									handler.post(new Runnable(){
											@Override
											public void run() {
												receiver(response);
												while (true) {
													if (data.length() != 0) {
														taskDone = true;
														break;
													}
												}
											}
										});
									while (true) {
										if (taskDone && (!skipFlag ? isWebLoaded() : true) && data.has("result")) { // this is too crazy
											writer.print(String.valueOf(data.toString().length()) + data.toString());
											writer.flush();
											data = new JSONObject();
											taskDone = false;
											skipFlag = false;
											importantScript = null;
											if (close) {
												if (mSocket != null) {
													mSocket.close();
												}
												deleteAllCookie();
												clearAppData();
												stopSelf(startId);
											}
											break;
										}
									}
								}
							} catch (JSONException ex) {
							} catch (UnknownHostException ex) {
							} catch (IOException ex) {
							}
						}
					});
				thread.start();
			}
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		deleteAllCookie();
		clearAppData();
	}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

	private String base64Encode(String data)  {
		try {
			return new String(Base64.encode(data.getBytes("UTF-8"), Base64.NO_WRAP));
		} catch (UnsupportedEncodingException e) {
			return data;
		}
	}

	private String base64Decode(String data) {
		try {
			return new String(Base64.decode(data.getBytes("UTF-8"), Base64.NO_WRAP));
		} catch (UnsupportedEncodingException e) {
			return data;
		}
	}

    public void clearCookies(String url) {
		String cookiestring = mCookieManager.getCookie(url);
		if (cookiestring != null) {
			for (String cookie : cookiestring.split(";")) {
				String script_clear_cookies = String.format("%1$s=; Max-Age=-999999", cookie.split("=")[0].trim());
				mCookieManager.setCookie(url, script_clear_cookies);
			}
		}
    }

	private float convertDpToPixel(float dp) {
		return dp * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
	}

	private void clearAppData() {
		try {
			if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
				((ActivityManager) getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
			} else {
				Runtime runtime = Runtime.getRuntime();
				runtime.gc();
				runtime.exec("pm clear " + getPackageName());
			}
		} catch (Exception ex) {}
	}

	private void click(float x, float y) {
		long downTime = SystemClock.uptimeMillis();
		long upTime = SystemClock.uptimeMillis();
		MotionEvent keyDown = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_DOWN, x, y, 0);
		MotionEvent keyUp = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0);
		mWebView.dispatchTouchEvent(keyDown);
		mWebView.dispatchTouchEvent(keyUp);
	}

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void deleteAllCookie() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mCookieManager.removeAllCookies(null);
            mCookieManager.flush();
        } else {
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
            cookieSyncManager.startSync();
            mCookieManager.removeAllCookie();
            mCookieManager.removeSessionCookie();
            cookieSyncManager.stopSync();
            cookieSyncManager.sync();
        }
        if (mWebView != null) {
			mWebView.post(new Runnable() {
					@Override
					public void run() {
						mWebView.clearFormData();
						mWebView.clearSslPreferences();
						mWebView.clearCache(true);
						mWebView.clearHistory();
					}
				}); 
        }
        WebStorage.getInstance().deleteAllData();
    }

	private void executeScript(String script) {
//		Toast.makeText(getApplication(), script, Toast.LENGTH_SHORT).show();
		while (true) {
			if (isWebLoaded()) {
				mWebView.evaluateJavascript(
					"(function() { return " + script + "; })();",
					new ValueCallback<String>() {
						@Override
						public void onReceiveValue(String result) {
							result = result.replaceAll("^\"|\"$", "");
//							Toast.makeText(getApplication(), result, Toast.LENGTH_SHORT).show();
							try {
								data.put("result", base64Encode(result));
							} catch (JSONException ex) {} 
						}
					}
				);
				break;
			}
		}
	}

	private void executeScript(String script, boolean promise) {
//		Toast.makeText(getApplication(), script, Toast.LENGTH_SHORT).show();
		while (true) {
			if (isWebLoaded()) {
				mWebView.evaluateJavascript(script, null);
				break;
			}
		}
	}

	private boolean isWebLoaded() {
		return webViewLoaded >= 3;
	}

	private <T> Iterable<T> iteratorToIterable(final Iterator<T> iterator) {
		return new Iterable<T>() {

			@Override
			public Spliterator<T> spliterator() {
				return null;
			}

			@Override
			public Iterator<T> iterator() {
				return iterator;
			}

		};	
	}

	private String getAttribute(String name, JSONObject recv) throws JSONException {
		if (recv.has(name)) {
			return base64Decode(recv.get(name).toString());
		}
		return "";
	}

	private String getAttribute(String name, JSONObject recv, String else_return) throws JSONException {
		if (recv.has(name)) {
			return base64Decode(recv.get(name).toString());
		}
		return else_return;
	}

	private void loadUrl(WebView view, String url) {
		createNotification(url);
		if (url.startsWith("javascript")) {
			executeScript(url.split(":", 2)[1], true);
		} else {
			webViewLoaded = 0;
			webViewLoadedObject = new JSONObject();
			if (overrideScript != null) {
				mWebView.setVisibility(View.INVISIBLE);
			}
			view.loadUrl(url, extraHeaders);
		}
	}

    private String title(String string) {
        String result = "";
        for (String str : string.split("-")) {
            if (!result.isEmpty()) {
                result += "-";
            }
            result += capitalize(str);
        }
        return result;
    }

	@JavascriptInterface
	public void returnAwait(String result) {
		result = result.replaceAll("^\"|\"$", "");
//		Toast.makeText(getApplication(), result, Toast.LENGTH_SHORT).show();
		try {
			data.put("result", base64Encode(result));
		} catch (JSONException ex) {} 
	}

	private void runScriptAfterLoaded(final WebView view) {
		if (isWebLoaded()) {
			if (importantScript != null) {
				executeScript(importantScript, true);
			}
			mCookieManager.setAcceptCookie(true);
			mCookieManager.acceptCookie();
			mCookieManager.flush();
			if (overrideScript != null) {
				view.evaluateJavascript(
					overrideScript,
					new ValueCallback<String>() {
						@Override
						public void onReceiveValue(String result) {
							view.setVisibility(View.VISIBLE);	
						}
					}
				);
			}
		}
	}

	private void setLocale(Locale newLocale) {
		Resources resources = getResources();
        final Configuration config = resources.getConfiguration();
        final Locale curLocale = getLocale(config);
        if (!curLocale.equals(newLocale)) {
            Locale.setDefault(newLocale);
            final Configuration conf = new Configuration(config);
            conf.setLocale(newLocale);
            resources.updateConfiguration(conf, resources.getDisplayMetrics());
        }
    }

    private static Locale getLocale(Configuration config) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return config.getLocales().get(0);
        } else {
            return config.locale;
        }
    }

	private void sendKeyEvent(int keyCode, int action) {
		KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
		mWebView.dispatchKeyEvent(event);
	}

	private void swipe(final float xStart, final float yStart, final float xEnd, final float yEnd, final int speed) {
		thread = new Thread(new Runnable() {
				@Override
				public void run() {
					int xFlag = 0;
					int yFlag = 0;
					float xMove = xStart;
					float yMove = yStart;
					long downTime = SystemClock.uptimeMillis();
					long upTime = SystemClock.uptimeMillis();
					if ((xStart - xEnd) < 0) {  // x increase
						xFlag = 1;
					}
					if ((xStart - xEnd) > 0) { // x decrease
						xFlag = 2;
					}
					if ((yStart - yEnd) < 0) { // y increase
						yFlag = 1;
					}
					if ((yStart - yEnd) > 0) { // y decrease
						yFlag = 2;
					}
					mWebView.dispatchTouchEvent(MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_DOWN, xStart, yStart, 0));
					int count = 0;
					while (true) {
						if (xMove != xEnd) {
							if (xFlag == 1) {
								if ((xMove + count) <= xEnd) {
									xMove += count;
								} else {
									xFlag = 0; 
								}
							}
							if (xFlag == 2) {
								if ((xMove - count) >= xEnd) {
									xMove -= count;
								} else {
									xFlag = 0; 
								}
							}
						}
						if (yMove != yEnd) {
							if (yFlag == 1) {
								if ((yMove + count) <= yEnd) {
									yMove += count;
								} else {
									yFlag = 0; 
								}
							}
							if (yFlag == 2) {
								if ((yMove - count) >= yEnd) {
									yMove -= count;
								} else {
									yFlag = 0; 
								}
							}
						}
						if (xFlag == 0 && yFlag == 0) {
							break;
						}
//						mUrlBox.setText(String.format("%d - %d", xMove, yMove));
						mWebView.dispatchTouchEvent(MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_MOVE, xMove, yMove, 0));
						count += (new Random().nextInt(speed + 1)) + (speed % 2 == 0 ? speed / 2 : (speed - 1) / 2);
					}
					mWebView.dispatchTouchEvent(MotionEvent.obtain(downTime, ++upTime, MotionEvent.ACTION_UP, xEnd, yEnd, 0));
				}
			});
		thread.start();
	}

	private void setProxy(WebView webView, String host, String port) {
		Context appContext = webView.getContext().getApplicationContext();
		System.setProperty("http.proxyHost", host);
		System.setProperty("http.proxyPort", port);
		System.setProperty("https.proxyHost", host);
		System.setProperty("https.proxyPort", port);
		try {
			Class applictionCls = Class.forName("android.app.Application");
			Field loadedApkField = applictionCls.getField("mLoadedApk");
			loadedApkField.setAccessible(true);
			Object loadedApk = loadedApkField.get(appContext);
			Class loadedApkCls = Class.forName("android.app.LoadedApk");
			Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
			receiversField.setAccessible(true);
			ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
			for (Object receiverMap : receivers.values()) {
				for (Object rec : ((ArrayMap) receiverMap).keySet()) {
					Class clazz = rec.getClass();
					if (clazz.getName().contains("ProxyChangeListener")) {
						Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
						Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
						onReceiveMethod.invoke(rec, appContext, intent);
					}
				}
			}
		} catch (ClassNotFoundException ex) {
		} catch (NoSuchFieldException ex) {
		} catch (IllegalAccessException ex) {
		} catch (IllegalArgumentException ex) {
		} catch (NoSuchMethodException ex) {
		} catch (InvocationTargetException ex) {
		}
	}

	private void receiver(String response) {
		try {
			data = new JSONObject(response);
			command = getAttribute("command", data);
			String None = base64Encode("None");

			switch (command) {
				case "close":
					{
						skipFlag = true;
						close = true;
						data.put("result", None);
						break;
					}
				case "clear cookie":
					{
						skipFlag = true;
						String url = getAttribute("url", data);
						String cookie_name = getAttribute("cookie_name", data);
						String script_clear_cookie = String.format("%1$s=; Max-Age=-999999", cookie_name);
						if (url.isEmpty()) {
							mCookieManager.setCookie(mWebView.getUrl(), script_clear_cookie);
						} else {
							mCookieManager.setCookie(url, script_clear_cookie);
						}
						data.put("result", None);
						break;
					}
				case "clear cookies":
					{
						skipFlag = true;
                        String url = getAttribute("url", data);
                        if (url.isEmpty()) {
                            clearCookies(mWebView.getUrl());
                        } else {
                            clearCookies(url);
                        }
						data.put("result", None);
						break;
					}
				case "current url":
					{
						skipFlag = true;
						String url = mWebView.getUrl();
						if (url == null) {
							data.put("result", "");
						} else {
							data.put("result", base64Encode(url));
						}
						break;
					}
				case "click java":
					{
						skipFlag = true;
						String[] position = getAttribute("position", data).split(" ");
						float x = convertDpToPixel(Float.valueOf(position[0]));
						float y = convertDpToPixel(Float.valueOf(position[1]));
						click(x, y);
						data.put("result", None);
						break;
					}
				case "clear local storage":
					{
						skipFlag = false;
						executeScript("window.localStorage.clear()");
						break;
					}
				case "clear session storage":
					{
						skipFlag = false;
						executeScript("window.sessionStorage.clear()");
						break;
					}
                case "delete all cookie":
                    {
                        skipFlag = true;
                        deleteAllCookie();
                        data.put("result", None);
                        break;
                    }
				case "execute script":
					{
						skipFlag = false;
						String script = getAttribute("script", data);
						executeScript(script);
						break;
					}
				case "find element":
					{
						skipFlag = false;
						String script = "";
						String request = getAttribute("request", data);
						String path = getAttribute("path", data, "document");
						String by = getAttribute("by", data);
						String value = getAttribute("value", data).replace("\"", "'").replace("\\\"", "\\'");
						List<String> except = new ArrayList<String>();
						except.add("wait until element");
						except.add("wait until not element");
						if (request.isEmpty() || except.contains(request)) {
							switch (by) {
								case "id":
									script = String.format("%s.querySelector(\"#%s\")", path, value);
									break;
								case "xpath":
									script = String.format("%s.evaluate(\"%s\", %s, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue", path, value, path);
									break;
								case "link text":
									script = String.format("%s.querySelector(\"[href=%s]\")", path, value);
									break;
								case "partial link text":
									script = String.format("%s.querySelector(\"[href*=%s]\")", path, value);
									break;
								case "name":
									script = String.format("%s.querySelector(\"[name=%s]\")", path, value);
									break;
								case "tag name":
									script = String.format("%s.querySelector(\"%s\")", path, value);
									break;
								case "class name":
									script = String.format("%s.querySelector(\".%s\")", path, value);
									break;
								case "css selector":
									script = String.format("%s.querySelector(\"%s\")", path, value);
									break;
							}
						} else {
							script = path;
						}
						data.put("path", base64Encode(script));
						switch (request) {
							case "get attribute":
								String script_getAttribute = String.format("(function() { var element = %1$s; return element.%2$s })()", script, getAttribute("attribute_name", data));
								executeScript(script_getAttribute);
								break;
							case "remove attribute":
								String script_remove_attribute = String.format("(function() { var element = %1$s; return element.removeAttribute(\"%2$s\") })()", script, getAttribute("attribute_name", data));
								executeScript(script_remove_attribute);
								break;
							case "send key":
								sendKeyEvent(Integer.valueOf(getAttribute("key", data)), KeyEvent.ACTION_DOWN);
								data.put("result", None);
								break;
							case "send text":
								char[] char_array = getAttribute("text", data).toCharArray();
								KeyCharacterMap char_map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
								KeyEvent[] events = char_map.getEvents(char_array);
								for (KeyEvent event : events) {
									mWebView.dispatchKeyEvent(event);
								}
								data.put("result", None);
								break;
							case "set attribute":
								String attribute_value = getAttribute("attribute_value", data);
								if (getAttribute("is_string", data).equals("true")) {
									attribute_value = String.format("\"%1$s\"", attribute_value);
								}
								String script_set_attribute = String.format("(function() { var element = %1$s; element.%2$s = %3$s })()", script, getAttribute("attribute_name", data), attribute_value);
								executeScript(script_set_attribute);
								break;
							case "wait until element":
								String func_wait_until = String.format("function wait() { return new Promise(resolve => { if (%1$s) { return resolve(%1$s); } const observer = new MutationObserver(mutations => { if (%1$s) { resolve(%1$s); observer.disconnect(); } }); observer.observe(document.body, { childList: true, subtree: true }); }); }", script);
								String script_wait_until_element = String.format("(function() { %1$s; return wait() })().then((element) => {android.returnAwait(element.outerHTML)})", func_wait_until);
								importantScript = script_wait_until_element;
								executeScript(script_wait_until_element, true); // promise
								break;
							case "wait until not element":
								String func_wait_until_not = String.format("function wait() { return new Promise(resolve => { if (%1$s) { return resolve(); } const observer = new MutationObserver(mutations => { if (%1$s) { resolve(); observer.disconnect(); } }); observer.observe(document.body, { childList: true, subtree: true }); }); }", "!" + script);
								String script_wait_until_not_element = String.format("(function() { %1$s; return wait() })().then(() => {android.returnAwait(\"%2$s\")})", func_wait_until_not, "True"); // encode base64 result so cant set result is base64
								importantScript = script_wait_until_not_element;
								executeScript(script_wait_until_not_element, true); // promise
								break;
							default:
								data.put("path", base64Encode(script));
								executeScript(String.format("%s%s", script, (!request.isEmpty() ? request : ".outerHTML")));
								break;
						}
						break;
					}
				case "find elements":
					{
						skipFlag = false;
						String script = "";
						String document = "";
						String path = getAttribute("path", data, "document");
						String by = getAttribute("by", data);
						String value = getAttribute("value", data).replace("\"", "'").replace("\\\"", "\\'");
						switch (by) {
							case "id":
								document = String.format("%1$s.querySelectorAll(\"#%2$s\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
							case "xpath":
								document = String.format("%1$s.evaluate(\"%2$s\", %1$s, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)", path, value);
								script = String.format("(function() { var elements = %1$s; return [...Array(elements.snapshotLength)].map((_, index) => [index, elements.snapshotItem(index).outerHTML])})()", document);
								break;
							case "link text":
								document = String.format("%1$s.querySelectorAll(\"[href=%2$s]\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
							case "partial link text":
								document = String.format("%1$s.querySelectorAll(\"[href*=%2$s]\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
							case "name":
								document = String.format("%1$s.querySelectorAll(\"[name=%2$s]\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
							case "tag name":
								document = String.format("%1$s.querySelectorAll(\"%2$s\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
							case "class name":
								document = String.format("%1$s.querySelectorAll(\".%2$s\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
							case "css selector":
								document = String.format("%1$s.querySelectorAll(\"%2$s\")", path, value);
								script = String.format("(function() { var index = 0, data = []; %1$s.forEach((element) => { data.push([index++, element.outerHTML.replace(/\"/g, \"'\")]); }); return data })()", document);
								break;
						}
						data.put("path", base64Encode(document));
						executeScript(script);
						break;
					}
				case "get":
					{
						skipFlag  = false;
						String url = getAttribute("url", data);
						loadUrl(mWebView, url);
						data.put("result", None);
						break;
					}
				case "get cookie":
					{
						skipFlag = true;
						String url = getAttribute("url", data);
						String cookie_name = getAttribute("cookie_name", data);
						String cookies = "";
						if (url.isEmpty()) {
							cookies = mCookieManager.getCookie(mWebView.getUrl());
						} else {
							cookies = mCookieManager.getCookie(url);
						}
						String script_get_cookie = String.format("%1$s=", cookie_name);
						if (cookies == null || !cookies.contains(script_get_cookie)) {
							data.put("result", "");
						} else {
							data.put("result", base64Encode(script_get_cookie + cookies.split(script_get_cookie)[1].split(";")[0]));
						}
						break;
					}
				case "get cookies":
					{
						skipFlag = true;
						String url = getAttribute("url", data);
						String cookies = "";
						if (url.isEmpty()) {
							cookies = mCookieManager.getCookie(mWebView.getUrl());
						} else {
							cookies = mCookieManager.getCookie(url);
						}
						if (cookies == null) {
							data.put("result", "");
						} else {
							data.put("result", base64Encode(cookies));
						}
						break;
					}
				case "get user agent":
					{
						skipFlag = true;
						String user_agent = extraHeaders.get("User-Agent");
						if (user_agent == null) {
							data.put("result", "");
						} else {
							data.put("result", base64Encode(user_agent));
						}
						break;
					}
				case "get headers":
					{
						skipFlag = true;
						data.put("result", new JSONObject(extraHeaders));
						break;
					}
				case "get local storage":
					{
						skipFlag = false;
						executeScript("JSON.stringify(window.localStorage)");
						break;
					}
				case "get session storage":
					{
						skipFlag = false;
						executeScript("JSON.stringify(window.sessionStorage)");
						break;
					}
				case "get recaptcha v3 token":
					{
						skipFlag = false;
						String site_key = getAttribute("site_key", data);
						String action = getAttribute("action", data);
						String script_get_recaptcha_v3_token = "";
						if (!action.isEmpty()) {
							script_get_recaptcha_v3_token = String.format("\"%1$s\", { action: \"%2$s\" }", site_key, action);
						} else {
							script_get_recaptcha_v3_token = String.format("\"%1$s\"", site_key);
						}
						executeScript(String.format("grecaptcha.execute(%1$s).then(function(token) { android.returnAwait(token) });", script_get_recaptcha_v3_token), true);
						break;
					}
				case "override js function":
					{
						skipFlag = true;
						String script = getAttribute("script", data);
						if (overrideScript != null) {
							if (overrideScript.endsWith(";")) {
								overrideScript += script;
							} else {
								overrideScript += ";" + script;
							}
						} else {
							overrideScript = script;
						}
						data.put("result", None);
						break;
					}
				case "page source":
					{
						skipFlag = true;
						if (isWebLoaded()) {
							executeScript("document.querySelector(\"html\").outerHTML");
						} else {
							data.put("result", "");
						}
						break;
					}
				case "swipe":
					{
						skipFlag = true;
						String[] position = getAttribute("position", data).split(" ");
						int speed = Integer.valueOf(getAttribute("speed", data));
						swipe(Float.valueOf(position[0]), Float.valueOf(position[1]), Float.valueOf(position[2]), Float.valueOf(position[3]), speed);
						data.put("result", None);
						break;
					}
				case "swipe down":
					{
						skipFlag = true;
						DisplayMetrics size = Resources.getSystem().getDisplayMetrics();
						swipe(size.widthPixels / 2, size.heightPixels / 4 + size.heightPixels / 8, size.widthPixels / 2, size.heightPixels / 4 + size.heightPixels / 2, 15);
						data.put("result", None);
						break;
					}
				case "swipe up":
					{
						skipFlag = true;
						DisplayMetrics size = Resources.getSystem().getDisplayMetrics();
						swipe(size.widthPixels / 2, size.heightPixels / 4 + size.heightPixels / 2, size.widthPixels / 2, size.heightPixels / 4 + size.heightPixels / 8, 15);
						data.put("result", None);
						break;
					}
				case "set cookie":
					{
						skipFlag = true;
						String url = getAttribute("url", data);
						String cookie_name = getAttribute("cookie_name", data);
						String value = getAttribute("value", data);
						String script_set_cookie = String.format("%1$s=%2$s", cookie_name, value);
						if (url.isEmpty()) {
							mCookieManager.setCookie(mWebView.getUrl(), script_set_cookie);
						} else {
							mCookieManager.setCookie(url, script_set_cookie);
						}
						data.put("result", None);
						break;
					}
				case "set user agent":
					{
						skipFlag = true;
						String user_agent = getAttribute("user_agent", data);
                        extraHeaders.put("X-Requested-With", "XMLHttpRequest");
						extraHeaders.put("User-Agent", user_agent);
						mWebView.getSettings().setUserAgentString(user_agent);
						data.put("result", None);
						break;
					}
				case "set proxy":
					{
						skipFlag = true;
						String[] proxy = getAttribute("proxy", data).split(" ");
						setProxy(mWebView, proxy[0], proxy[1]);
						data.put("result", None);
						break;
					}
				case "scroll to":
					{
						skipFlag = true;
						String[] position = getAttribute("position", data).split(" ");
						mWebView.scrollTo(Integer.valueOf(position[0]), Integer.valueOf(position[1]));
						data.put("result", None);
						break;
					}
				case "set headers":
					{
						skipFlag = true;
						JSONObject headers = new JSONObject(getAttribute("headers", data));
						for (String key : iteratorToIterable(headers.keys())) {
							String value = headers.get(key).toString();
							extraHeaders.put(key, value);
							switch (key.toLowerCase()) {
                                case "x-requested-with":
                                    extraHeaders.put("X-Requested-With", "XMLHttpRequest");
									break;
                                case "user-agent":
                                    mWebView.getSettings().setUserAgentString(value);
									break;
                            }
						}
						data.put("result", None);
						break;
					}
				case "set local storage":
					{
						skipFlag = false;
						String value = getAttribute("value", data);
						if (getAttribute("is_string", data).equals("true")) {
							value = String.format("\"%1$s\"", value);
						}
						executeScript(String.format("window.localStorage.setItem(\"%1$s\", %2$s)", getAttribute("key", data), value));
						break;
					}
				case "set session storage":
					{
						skipFlag = false;
						String value = getAttribute("value", data);
						if (getAttribute("is_string", data).equals("true")) {
							value = String.format("\"%1$s\"", value);
						}
						executeScript(String.format("window.sessionStorage.setItem(\"%1$s\", %2$s)", getAttribute("key", data), value));
						break;
					}
				case "title":
					{
						skipFlag = true;
                        if (isWebLoaded()) {
                            executeScript("document.title");
                        } else {
                            data.put("result", "");
                        }
						break;
					}
			}
		} catch (JSONException ex) {
			Toast.makeText(getApplicationContext(), "Error parsing JSONObject", Toast.LENGTH_LONG).show();
		}
	}

}
