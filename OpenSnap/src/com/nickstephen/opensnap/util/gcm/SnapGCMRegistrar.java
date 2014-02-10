package com.nickstephen.opensnap.util.gcm;

import java.io.IOException;
import java.sql.Timestamp;

import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.util.tasks.DeviceSyncTask;

public class SnapGCMRegistrar {
	public static final String PROPERTY_REG_ID = "registrationId";
	public static final long REGISTRATION_EXPIRY_TIME_MS = 604800000L;
	public static final String SENDER_ID = "191410808405";
	
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTimeMs";
	private static final String TAG = "OpenSnap GCM";

	private Context mApplicationContext;
	private String mRegistrationId;
	private GoogleCloudMessaging mGCM;

	public SnapGCMRegistrar(Context paramContext)
	{
		this.mApplicationContext = paramContext.getApplicationContext();
	}

	/**
	 * Get the Application's version code from the PackageManager
	 * @param paramContext
	 * @return Application's version code from the {@code PackageManager}
	 */
	private static int getAppVersion(Context paramContext)
	{
		try {
			PackageInfo packageInfo = paramContext.getPackageManager().getPackageInfo(paramContext.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * Get Application's SharedPrefs
	 * @param paramContext
	 * @return Application's {@code SharedPreferences}
	 */
	private static SharedPreferences getGCMPreferences(Context paramContext)
	{
		return paramContext.getSharedPreferences(SnapGCMRegistrar.class.getSimpleName(), 0);
	}

	/**
	 * Gets the current registration ID for application on GCM service
	 * <p>
	 * If result is empty, the app needs to register
	 * @param paramContext
	 * @return registration ID, or empty string if there is not existing registration ID
	 */
    @SuppressLint("NewApi")
	private static String getRegistrationId(Context paramContext)
	{
		final SharedPreferences prefs = getGCMPreferences(paramContext);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (Build.VERSION.SDK_INT >= 9) {
            if (registrationId.isEmpty()) {
                Twig.info(TAG, "Registration not found.");
                return "";
            }
        } else if (registrationId.length() == 0) {
            Twig.info(TAG, "Registration not found.");
            return "";
        }
		
		// Check if app was updated. If so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(paramContext);
		if (registeredVersion != currentVersion || isRegistrationExpired(paramContext)) {
			Twig.info(TAG, "App version changed or registration expired.");
			return "";
		}
		return registrationId;
	}

	private static boolean isRegistrationExpired(Context context)
	{
		long l = getGCMPreferences(context).getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1L);
		return System.currentTimeMillis() > l;
	}

	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param paramContext application's context
	 * @param regId registration ID
	 */
	public static void setRegistrationId(Context paramContext, String regId)
	{
		final SharedPreferences localSharedPreferences = getGCMPreferences(paramContext);
		int appVersion = getAppVersion(paramContext);
		Twig.info(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = localSharedPreferences.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		long l = REGISTRATION_EXPIRY_TIME_MS + System.currentTimeMillis();
		Twig.verbose(TAG, "Setting registration expiry time to " + new Timestamp(l));
		editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, l);
		editor.commit();
	}

	/**
	 * Setup the GCM connection. The call is async and will return immediately.
	 * <p>
	 * <b>Note: this must be called from the UI thread</b>
	 */
	public void setupGoogleCloudManager(boolean forceSync) {
		Twig.debug(TAG, "Setting up GCM");
		String reg = getRegistrationId(mApplicationContext);
		if (StatMethods.IsStringNullOrEmpty(reg)) {
			new GcmRegistrationTask().execute(new Void[0]);
		} else if (forceSync) {
			new DeviceSyncTask(mApplicationContext, reg).execute(new String[0]);
		} else {
			Twig.info(TAG, "Registration still valid");
		}
	}

	private class GcmRegistrationTask extends AsyncTask<Void, Void, Boolean>
	{
		private GcmRegistrationTask()
		{
		}

		@Override
		protected Boolean doInBackground(Void... paramArrayOfVoid)
		{
			try
			{
				if (mGCM == null) {
					mGCM = GoogleCloudMessaging.getInstance(mApplicationContext);
				}
				mRegistrationId = mGCM.register(SENDER_ID);
				
				Twig.debug(TAG, "Device registered, registration id = " + mRegistrationId);
				setRegistrationId(SnapGCMRegistrar.this.mApplicationContext, mRegistrationId);
				return true;
			}
			catch (IOException e)
			{
				Twig.error(TAG, "Error: " + e.getMessage());
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if (result) {
				// Syncs the device ID to the snapchat server
				new DeviceSyncTask(mApplicationContext, mRegistrationId).execute(new String[0]);
			} else {
				//StatMethods.hotBread(mApplicationContext, "OpenSnap notification registration failed", Toast.LENGTH_SHORT);
                Twig.debug(TAG, "OpenSnap notification registration failed");
			}
			
			//new DeviceSyncTask(NotificationRegistrar.this.mApplicationContext, NotificationRegistrar.this.mRegistrationId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[0]);
			//User localUser = User.getInstance(NotificationRegistrar.this.mApplicationContext);
			//localUser.setNotificationId(NotificationRegistrar.this.mRegistrationId);
			//localUser.save();
		}
	}
}
