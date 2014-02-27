package com.nickstephen.opensnap.settings;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.gui.Theme;
import com.nickstephen.opensnap.util.http.SnapAPI;
import com.nickstephen.opensnap.util.play.SKU;

import org.holoeverywhere.preference.Preference;

/**
 * A static abstraction of the Settings for easy access 
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class SettingsAccessor {
	public static final String SHOW_SD_WARNING_KEY = "show_sd_warning";
	
	private static final String AUTH_KEY = "gimme784WEthose1837snaps93028baby90987364_dunno3940w0telsetoput";
	private static final String GENERAL_SETTINGS_KEY = "misc_settings";
	private static final String FIRST_TIME_KEY = "first_timer";
    private static final String FIRST_TIME_EDIT_KEY = "first_edit_time";
	
	/**
	 * Private hidden constructor. Shouldn't ever be called.
	 */
	private SettingsAccessor() {}

    public static boolean getPremium(Context ctxt) {
        return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(SKU.PREMIUM_FEATURES, false) || GlobalVars.getUsername(ctxt).compareTo("saltisgood") == 0;
    }

    @SuppressLint("NewApi")
    public static void setPremium(Context ctxt, boolean val) {
        if (Build.VERSION.SDK_INT >= 9) {
            PreferenceManager.getDefaultSharedPreferences(ctxt).edit().putBoolean(SKU.PREMIUM_FEATURES, val).apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(ctxt).edit().putBoolean(SKU.PREMIUM_FEATURES, val).commit();
        }
    }
	
	/**
	 * Get the user's preference about whether to preview media in an external activity or
	 * inside OpenSnap
	 * @param ctxt A context
	 * @return True for external previews, false for OpenSnap previews
	 */
	public static Boolean getExternalPreviewPref(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_preview_key), false);
	}

	public static Boolean getAllowSaves(Context ctxt, String s) {
		return s.compareTo(SnapAPI.createToken(GlobalVars.getUsername(ctxt), AUTH_KEY).substring(0, 32)) == 0;
	}
	
	/**
	 * Get the user's 'preference' about whether they can save files or whether to emulate
	 * SnapChat.
	 * @param ctxt A context
	 * @return True to allow saving, false to not allow it
	 */
	public static Boolean getAllowSaves(Context ctxt) {
        return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_allow_save_key), false);
		/* String key = PreferenceManager.getDefaultSharedPreferences(ctxt).getString(ctxt.getString(R.string.pref_allow_save_key), null);
		
		if (key == null)
			return false;
		return getAllowSaves(ctxt, key); */
	}
	
	/**
	 * Get the user's preference about whether to display snaps with a timer if possible. Will return true if 
	 * saves aren't allowed.
	 * @param ctxt A context
	 * @return True if to display times, false if not
	 */
	public static Boolean getSnapTiming(Context ctxt) {
		if (!getAllowSaves(ctxt)) {
			return true;
		}
		return !PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_time_key), false);
	}
	
	/**
	 * Get the user's preference about how long between Snap list refreshes (in minutes)
	 * @param ctxt A context
	 * @return The number of minutes between automatic refreshes
	 */
	public static int getRefreshTime(Context ctxt) {
		String str = PreferenceManager.getDefaultSharedPreferences(ctxt).getString(ctxt.getString(R.string.pref_refresh_key), "30");
		return Integer.valueOf(str);
	}
	
	/**
	 * Get the user's preference about how long between Snap list refreshes (int ticks)
	 * @param ctxt A context
	 * @return The number of ticks between automatic refreshes
	 */
	public static long getRefreshTimeLong(Context ctxt) {
		return getRefreshTime(ctxt) * 60000L;
	}
	
	/**
	 * Get the user's preference about whether to send Snap opened messages to the server
	 * @param ctxt A context
	 * @return True to not send messages, false to do so
	 */
	public static boolean getPrivateMode(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_markopen_key), false);
	}
	
	/**
	 * Get the user's preference about whether to always download mSnaps ASAP
	 * @param ctxt A context
	 * @return True to download straight away, false otherwise
	 */
	public static boolean getAlwaysDownload(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_dl_key), false);
	}

	/**
	 * Get the user's preference about whether to display a notification when downloading snaps
	 * @param ctxt A context
	 * @return True to display a notification, false otherwise
	 */
	public static boolean getDownloadNotificationPref(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_download_not_key), true);
	}
	
	/**
	 * Get the user's preference about whether to auto refresh teh snap list
	 * @param ctxt A context 
	 * @return True to auto refresh, false otherwise
	 */
	public static boolean getAutoRefreshPref(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_auto_refresh_key), true);
	}

	/**
	 * Get the user's preference about whether to display a notification when uploading snaps
	 * @param ctxt A context
	 * @return True to display a notification, false otherwise
	 */
	public static boolean getUploadNotificationPref(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_upload_not_key), true);
	}

	/**
	 * Get the user's preference about how many snaps to keep in the cloud
	 * @param ctxt A context
	 * @return The number of snaps to keep before attempting to clear the feed
	 */
	public static int getCloudSnapListSize(Context ctxt) {
		return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctxt).getString(ctxt.getString(R.string.pref_snapstokeep_key), "50"));
	}
	
	/**
	 * Get the user's preference about whether to display a toast on an update
	 * @param ctxt A context
	 * @return True to display a toast, false otherwise
	 */
	public static boolean getUpdateToastPref(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_update_toast_key), true);
	}
	
	/**
	 * Get the user's preference about whether to add snaps to the android library
	 * @param ctxt A context
	 * @return True to add to library, false otherwise 
	 */
	public static boolean getAddToLibraryPref(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_addtolibrary_key), true);
	}
	
	/**
	 * Get the user's preference about which theme to use
	 * @param ctxt A context
	 * @return A theme enum
	 */
	public static Theme getThemePref(Context ctxt) {
		String theme = PreferenceManager.getDefaultSharedPreferences(ctxt).getString(ctxt.getString(R.string.pref_theme_key), null);
		
		if (theme == null) {
			return Theme.def;
		}
		
		String[] vals = ctxt.getResources().getStringArray(R.array.themes_values);
		if (theme.compareTo(vals[1]) == 0) {
			return Theme.ori;
		} else if (theme.compareTo(vals[2]) == 0) {
			return Theme.black;
		} else if (theme.compareTo(vals[3]) == 0) {
			return Theme.snapchat;
		} else {
			return Theme.def;
		}
	}
	
	public static boolean getFirstTimeStart(Context ctxt) {
		SharedPreferences generalPrefs = ctxt.getSharedPreferences(GENERAL_SETTINGS_KEY, Context.MODE_PRIVATE);
		return generalPrefs.getBoolean(FIRST_TIME_KEY, true);
	}
	
	public static void setFirstTimeStart(Context ctxt, boolean val) {
		ctxt.getSharedPreferences(GENERAL_SETTINGS_KEY, Context.MODE_PRIVATE).edit().putBoolean(FIRST_TIME_KEY, val).commit();
	}

    /**
     * Get whether this is the first time running the snap editor
     * @param context
     * @return
     */
    public static boolean getFirstTimeEdit(Context context) {
        SharedPreferences generalPrefs = context.getSharedPreferences(GENERAL_SETTINGS_KEY, Context.MODE_PRIVATE);
        return generalPrefs.getBoolean(FIRST_TIME_EDIT_KEY, true);
    }

    public static void setFirstTimeEdit(Context context, boolean value) {
        context.getSharedPreferences(GENERAL_SETTINGS_KEY, Context.MODE_PRIVATE).edit()
                .putBoolean(FIRST_TIME_EDIT_KEY, value).commit();
    }
	
	public static void cleanupCache(Context ctxt) {
		File cacheDir = ctxt.getCacheDir();
		File[] files = cacheDir.listFiles();
		String resendPath = ctxt.getSharedPreferences(SnapEditorBaseFrag.RESEND_INFO_KEY, Context.MODE_PRIVATE).getString(SnapEditorBaseFrag.FILE_PATH_KEY, null);
		for (File file : files) {
			if (resendPath.compareTo(file.getAbsolutePath()) != 0) {
				file.delete();
			}
		}
	}
	
	/**
	 * Get whether to show a warning about the SD-Card not being mounted. (Used at startup)
	 * @param ctxt A context
	 * @return True to show the warning (default), false otherwise
	 */
	public static boolean getShowSDWarning(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(SHOW_SD_WARNING_KEY, true);
	}
	
	public static boolean getSaveLocalFeed(Context ctxt) {
		return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.pref_local_feed_save_key), true);
	}

    /**
     * Get whether Play services are supported
     * @param ctxt
     * @return
     */
    public static boolean getPlaySupported(Context ctxt) {
        return PreferenceManager.getDefaultSharedPreferences(ctxt).getBoolean(ctxt.getString(R.string.play_supported_key), true);
    }

    public static void setPlaySupported(Context ctxt, boolean val) {
        PreferenceManager.getDefaultSharedPreferences(ctxt).edit()
                .putBoolean(ctxt.getString(R.string.play_supported_key), val).commit();
    }
}
