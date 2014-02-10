package com.nickstephen.opensnap.util.gcm;

import org.holoeverywhere.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.nickstephen.lib.Twig;

public final class GCMUtil {
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	private GCMUtil() {	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 * @param activity 
	 * @return True if available, false otherwise
	 */
	public static boolean checkPlayServices(Activity activity) {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Twig.info("OpenSnap - GCMUtil", "This device is not supported for Google Play Services");
			}
			return false;
		}
		return true;
	}
}
