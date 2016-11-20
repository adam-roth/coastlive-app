package au.com.suncoastpc.coastlive.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.minidev.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;
import java.util.UUID;

import au.com.suncoastpc.coastlive.event.EventHandler;
import au.com.suncoastpc.coastlive.event.EventManager;
import au.com.suncoastpc.coastlive.event.EventType;
import au.com.suncoastpc.coastlive.net.ServerApi;

public class Environment {
	private static final int SOCKET_CONNECT_TIMEOUT = 5000;
	
	private static Context appContext;														//the Android application context
	private static long authenticatedUserId;												//the id of the currently authenticated user
	
	//XXX:  should we use a 'Service' for these (instead or additionally)?
	//private static final BackgroundSyncManager syncManager = new BackgroundSyncManager();				//handles background content sync
	//private static final BackgroundFileDownloader fileDownloader = new BackgroundFileDownloader();		//handles background download of images, resources, etc.
	//private static final BackgroundSubmissionManager uploader = new BackgroundSubmissionManager();      //handles submission of content up to the server
	
	private static final Object syncLock = new Object();
	private static boolean syncing = false;
	private static int syncProcessedCount = 0;
	private static int syncTotalCount = 0;
	private static boolean downloadsEnabled = true;

	private static double lastKnownLatitude = Double.MAX_VALUE;
	private static double lastKnownLongitude = Double.MAX_VALUE;
	private static long lastGeolocationTimestamp = -1;
	private static double lastKnownTemparature = Double.MAX_VALUE;
	private static long lastTemperatureTimestamp = -1;
	
	static {
		//syncManager.startBackgroundSync();
		//fileDownloader.start();
		//uploader.start();
		
		//listen for sync events so that we can provide a single interface for anyone who needs to track sync status
		EventManager.register(EnumSet.of(EventType.SYNC_STARTED, EventType.SYNC_UPDATE, EventType.SYNC_COMPLETE), new EventHandler() {
			@Override
			public void handleEvent(EventType eventType, Object eventData) {
				JSONObject info = (JSONObject)eventData;

				synchronized(syncLock) {
					switch (eventType) {
					case SYNC_STARTED:
						syncing = true;
						syncProcessedCount = 0;
						syncTotalCount = 0;
						break;
					
					case SYNC_UPDATE:
						//these things are only available on the 'update' event
						//syncTotalCount = info.getAsNumber(BackgroundSyncManager.UPDATE_TOTAL_ITEMS).intValue();
						//syncProcessedCount = info.getAsNumber(BackgroundSyncManager.UPDATE_ITEMS_PROCESSED).intValue();

						break;
					case SYNC_COMPLETE:
						syncing = false;
						syncProcessedCount = 0;
						syncTotalCount = 0;
						
						break;
					default:
						break;
					}
				}
				
			}
		});
	}

	public static synchronized void updateGeolocation(double lat, double lng) {
		updateGeolocation(lat, lng, new Date().getTime());
	}

	public static synchronized void updateGeolocation(double lat, double lng, long timestamp) {
		lastKnownLatitude = lat;
		lastKnownLongitude = lng;
		lastGeolocationTimestamp = timestamp;
	}

	public static synchronized void updateTemperature(double temp) {
		updateTemperature(temp, new Date().getTime());
	}

	public static synchronized void updateTemperature(double temp, long timestamp) {
		lastKnownTemparature = temp;
		lastTemperatureTimestamp = timestamp;
	}
	
	/*public static void disableSync() {
		syncManager.disableSync();
	}
	
	public static void enableSync() {
		syncManager.enableSync();
	}
	
	public static boolean isSyncEnabled() {
		return syncManager.isSyncEnabled();
	}*/
	
	public static void disableFileDownload() {
		downloadsEnabled = false;
	}
	
	public static void enableFileDownload() {
		downloadsEnabled = true;
	}
	
	public static boolean isFileDownloadEnabled() {
		return downloadsEnabled;
	}

	public static File getPhotosDirectory() {
		File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
		if (path.exists() || path.mkdirs()) {
			return path;
		}

		return null;
	}

	public static File getPrivateFilesDirectory() {
		if (getApplicationContext() == null) {
			return null;
		}

		return getApplicationContext().getFilesDir();
	}

	public static String randomId() {
		return UUID.randomUUID().toString();
	}
	
	public static synchronized JSONObject getDeviceInfo() {
		JSONObject result = new JSONObject();

		//OS details
		result.put("os.version", Build.VERSION.SDK_INT);
		result.put("os.release", Build.VERSION.RELEASE);
		result.put("os.codename", Build.VERSION.CODENAME);

		//device/hardware details
		if (! StringUtilities.isEmpty(Build.BRAND) && ! Build.UNKNOWN.equals(Build.BRAND)) {
			result.put("device.brand", Build.BRAND);
		}
		result.put("device.manufacturer", Build.MANUFACTURER);
		result.put("device.model", Build.MODEL);
		result.put("device.proc", Build.HARDWARE);
		result.put("device.product", Build.PRODUCT);
		if (! StringUtilities.isEmpty(Build.getRadioVersion()) && ! Build.UNKNOWN.equals(Build.getRadioVersion())) {
			result.put("device.radioVersion", Build.getRadioVersion());
		}
		if (! StringUtilities.isEmpty(Build.SERIAL) && ! Build.UNKNOWN.equals(Build.SERIAL)) {
			result.put("device.serial", Build.SERIAL);
		}

		//location
		if (lastKnownLatitude != Double.MAX_VALUE && lastKnownLongitude != Double.MAX_VALUE && lastGeolocationTimestamp > 0) {
			result.put("geo.latitude", lastKnownLatitude);
			result.put("geo.longitude", lastKnownLongitude);
			result.put("geo.timestamp", lastGeolocationTimestamp);
		}

		//temperature
		if (lastKnownTemparature != Double.MAX_VALUE && lastTemperatureTimestamp > 0) {
			result.put("env.temperature", lastKnownTemparature);
			result.put("env.temperatureTimestamp", lastTemperatureTimestamp);
		}

		//things that require a valid application content
		if (getApplicationContext() != null) {
			//device token
			//String fcmToken = UserPreferences.getValue(UserPreferences.REGISTERED_FCM_DEVICE_TOKEN);
			//if (! StringUtilities.isEmpty(fcmToken)) {
			//	result.put("device.token", fcmToken);
			//}

			//display
			DisplayMetrics metrics = new DisplayMetrics();
			WindowManager manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
			manager.getDefaultDisplay().getMetrics(metrics);
			result.put("screen.resolution", metrics.widthPixels + "x" + metrics.heightPixels);
			result.put("screen.dpi", metrics.densityDpi);

			//battery details
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

			if (batteryStatus != null) {
				//charging status
				int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
				result.put("battery.chargeState", isCharging ? "charging" : "discharging");

				//charge level
				int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				float batteryPct = level / (float) scale * 100.0f;
				result.put("battery.charge", batteryPct);

				if (isCharging) {
					//charging method
					int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
					boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
					boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

					result.put("battery.chargeMethod", usbCharge ? "usb" : (acCharge ? "ac" : "unknown"));
				}
			}

			//network state
			NetworkInfo wifi = null;
			NetworkInfo mobile = null;
			ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			for (Network network : connectivityManager.getAllNetworks()) {
				NetworkInfo info = connectivityManager.getNetworkInfo(network);
				if (wifi == null && info.getType() == ConnectivityManager.TYPE_WIFI) {
					wifi = info;
				}
				else if (mobile == null && info.getType() == ConnectivityManager.TYPE_MOBILE) {
					mobile = info;
				}
			}
			result.put("network.wifi", wifi != null && wifi.isAvailable() && wifi.isConnected());
			result.put("network.mobile", mobile != null && mobile.isAvailable() && mobile.isConnected());

			//wifi signal strength
			if (wifi != null && wifi.isAvailable() && wifi.isConnected()) {
				WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
				int numberOfLevels = 5;
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				int signalLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);

				result.put("network.wifiLevel", signalLevel);
			}

			//mobile signal strength
			if (mobile != null && mobile.isAvailable() && mobile.isConnectedOrConnecting()) {
				TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
				CellInfo info = telephonyManager.getAllCellInfo().get(0);
				if (info instanceof CellInfoCdma) {
					CellInfoCdma cellInfo = (CellInfoCdma) info;
					result.put("network.mobileType", "cdma");
					result.put("network.mobileLevel", cellInfo.getCellSignalStrength().getLevel());
					result.put("network.mobileDbm", cellInfo.getCellSignalStrength().getDbm());
				} else if (info instanceof CellInfoGsm) {
					CellInfoGsm cellInfo = (CellInfoGsm) info;
					result.put("network.mobileType", "gsm");
					result.put("network.mobileLevel", cellInfo.getCellSignalStrength().getLevel());
					result.put("network.mobileDbm", cellInfo.getCellSignalStrength().getDbm());
				} else if (info instanceof CellInfoLte) {
					CellInfoLte cellInfo = (CellInfoLte) info;
					result.put("network.mobileType", "lte");
					result.put("network.mobileLevel", cellInfo.getCellSignalStrength().getLevel());
					result.put("network.mobileDbm", cellInfo.getCellSignalStrength().getDbm());
				} else if (info instanceof CellInfoWcdma) {
					CellInfoWcdma cellInfo = (CellInfoWcdma) info;
					result.put("network.mobileType", "lte");
					result.put("network.mobileLevel", cellInfo.getCellSignalStrength().getLevel());
					result.put("network.mobileDbm", cellInfo.getCellSignalStrength().getDbm());
				} else {
					result.put("network.mobileType", "unknown");
					result.put("network.mobileLevel", -1);
					result.put("network.mobileDbm", -1);
				}
			}
		}

		return result;
	}

	public static JSONObject getSyncInfo() {
		synchronized(syncLock) {
			JSONObject result = new JSONObject();
			//result.put(BackgroundSyncManager.UPDATE_ITEMS_PROCESSED, syncProcessedCount);
			//result.put(BackgroundSyncManager.UPDATE_TOTAL_ITEMS, syncTotalCount);
			//result.put(BackgroundSyncManager.UPDATE_PERCENT_PROGRESS, getSyncProgress());
			
			return syncing ? result : null;
		}
	}
	
	public static double getSyncProgress() {
		synchronized(syncLock) {
			if (syncTotalCount < 1) {
				return 0.0;
			}
			
			return (double)syncProcessedCount / (double)syncTotalCount;
		}
	}
	
	public static Context getApplicationContext() {
		return appContext;
	}
	
	public static void setApplicationContext(Context context) {
		if (appContext == null) {
			appContext = context;
		}
	}
	
	public static boolean isDebugMode() {
		return Constants.USE_DEV;
	}
	
	public static boolean shouldForceResync() {
		//return true;
		return isDebugMode() && Constants.FORCE_RESYNC;
	}
	
	public static boolean isMainThread() {
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}
	
	public static boolean isNetworkAvailable() {
		if (appContext == null) {
			//can't actually check until we have a valid application context
			return false;
		}
		
		ConnectivityManager connectivityManager = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
  
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	public static boolean isServerAvailable() {
		try (Socket socket = new Socket()) {
	        socket.connect(new InetSocketAddress(ServerApi.getServerHostName(), ServerApi.getServerPort()), SOCKET_CONNECT_TIMEOUT);
	        return true;
	    } catch (IOException e) {
	        return false; // Either timeout or unreachable or failed DNS lookup.
	    }
	}
	
	public static boolean runOnMainThread(Runnable runnable) {
		if (runnable == null) {
			return false;
		}
		
		Handler mainHandler = new Handler(Looper.getMainLooper());
		return mainHandler.post(runnable);
	}

	public static String formatDate(long timestamp) {
		return formatDate(new Date(timestamp));
	}

	public static String formatDateUtc(long timestamp) {
		return formatDate(utcDateToLocalDate(new Date(timestamp)));
	}

	public static String formatDate(Date date) {
		Context context = getApplicationContext();
		if (context == null) {
			//no system format available, use the hardcoded default
			return Constants.DEFAULT_DATE_FORMAT.format(date);
		}

		//use the system format
		return android.text.format.DateFormat.getDateFormat(context).format(date);
	}

	public static String formatDateWithTime(long timestamp) {
		return formatDateWithTime(new Date(timestamp));
	}

	public static String formatDateWithTimeUtc(long timestamp) {
		return formatDateWithTime(utcDateToLocalDate(new Date(timestamp)));
	}

	public static String formatDateWithTime(Date date) {
		Context context = getApplicationContext();
		if (context == null) {
			//no system format available, use the hardcoded default
			return Constants.DEFAULT_DATE_FORMAT_WITH_TIME.format(date);
		}

		//use the system format
		return android.text.format.DateFormat.getDateFormat(context).format(date) + " " + android.text.format.DateFormat.getTimeFormat(context).format(date);
	}

	public static boolean isAndroidEmulator() {
		return Build.FINGERPRINT.startsWith("generic")
				|| Build.FINGERPRINT.startsWith("unknown")
				|| Build.MODEL.contains("google_sdk")
				|| Build.MODEL.contains("Emulator")
				|| Build.MODEL.contains("Android SDK built for x86")
				|| Build.MANUFACTURER.contains("Genymotion")
				|| (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
				|| "google_sdk".equals(Build.PRODUCT);
	}

	private static Date utcDateToLocalDate(Date utcDate) {
		TimeZone localTimezone = TimeZone.getDefault();
		int offsetMillis = localTimezone.getOffset(utcDate.getTime());

		return new Date(utcDate.getTime() - offsetMillis);
	}
}
