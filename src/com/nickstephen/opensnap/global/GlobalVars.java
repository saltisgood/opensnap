package com.nickstephen.opensnap.global;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;

import com.nickstephen.opensnap.R;

/**
 * A set of static accessors and some general un-modifiable Strings that are
 * used in various places throughout the program.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public final class GlobalVars {
	/**
	 * Private constructor. Should never be called, because YOLO right?
	 */
	private GlobalVars() {}
	
	/**
	 * A key that's used for checking SnapChat JSONs for the authorisation token
	 */
	public static final String AUTH_TOKEN_KEY = "auth_token";
	/**
	 * A key that's used for checking SnapChat JSONs for the username token
	 */
	public static final String USER_KEY = "username";
	
	/**
	 * Private static variable that holds the value of the user's username. Only ever
	 * accessed through static methods that can re-initialise it from file if necessary.
	 * @see {@link #getUsername(Context)}
	 * @see {@link #setUsername(Context, String)}
	 */
	private static String sUsername = null;
	/**
	 * Private static variable that holds the value of the user's password. Only ever
	 * accessed through static methods that can re-initialise it from file if necessary.
	 * @see {@link #getPassword(Context)}
	 * @see {@link #setPassword(Context, String)}
	 */
	private static String sPassword = null;
	/**
	 * Private static variable that holds the current value of the authorisation token.
	 * Only ever accessed through static methods that can re-initialise it from file if necessary.
	 * @see {@link #getAuthToken(Context)}
	 * @see {@link #setAuthToken(Context, String)}
	 */
	private static String sAuthToken = null;
	/**
	 * Private static variable that holds the value of whether the user is logged in.
	 * Only ever accessed through static methods that can re-initialise it from file if necessary.
	 * @see {@link #isLoggedIn(Context)}
	 * @see {@link #setLoggedIn(Context, Boolean)}
	 */
	private static Boolean sIsLoggedIn = null;
	
	private static final AtomicBoolean sNetworkBusy = new AtomicBoolean();
	
	/**
	 * Gain a lock on the network. DO NOT CALL FROM THE UI THREAD OR IT COULD SERIOUSLY BLOCK IT.
	 * @param timeout Specify the maximum time (seconds) to attempt to gain a lock. 0 or negative values are ignored
	 * @return True if a lock is gained, false if it timed out
	 */
	public static boolean lockNetwork(int timeout) {
		timeout *= 5;
		int count = 0;
		while (true) {
			synchronized (sNetworkBusy) {
				if (!sNetworkBusy.get()) {
					sNetworkBusy.set(true);
					break;
				} else {
					try {
						Thread.sleep(200L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (count++ >= timeout && timeout > 0) {
				return false;
			}
		}
		return true;
	}
	
	public static void releaseNetwork() {
		sNetworkBusy.set(false);
	}
	
	public static boolean isNetworkBusy() {
		return sNetworkBusy.get();
	}
	
	/**
	 * Get either the current user's {@link #sUsername} or the previous user's one. Can re-read
	 * from file if necessary or will just grab the value in memory.
	 * @param ctxt A context
	 * @return The username
	 */
	public static String getUsername(Context ctxt) {
		if (sUsername == null) {
			SharedPreferences loginpref = ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE);
			sUsername = loginpref.getString(ctxt.getString(R.string.pref_login_user_key), "");
		}
		
		return sUsername;
	}
	
	/**
	 * Set the current user's {@link #sUsername}. Will immediately write it to file as well.
	 * @param ctxt A context
	 * @param newuser The new username
	 * @return True on successfully writing to permanent storage
	 */
	public static Boolean setUsername(Context ctxt, String newuser) {
		sUsername = newuser;
		
		SharedPreferences loginpref = ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = loginpref.edit();
		editor.putString(ctxt.getString(R.string.pref_login_user_key), sUsername);
		return editor.commit();
	}
	
	/**
	 * Get either the current user's password ({@link #sPassword}) or the previous user's one. Can re-read from
	 * file if necessary or will just grab the value in memory.
	 * @param ctxt A context
	 * @return The password
	 */
	public static String getPassword(Context ctxt) {
		if (sPassword == null) {
			sPassword = ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE)
					.getString(ctxt.getString(R.string.pref_login_pword_key), "");
		}
		return sPassword;
	}
	
	/**
	 * Set the current user's password ({@link #sPassword}). Will immediately write it to file as well.
	 * @param ctxt A context
	 * @param newpword The new password
	 * @return True on successfully writing to permanent storage
	 */
	public static Boolean setPassword(Context ctxt, String newpword) {
		sPassword = newpword;
		
		return ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE).edit()
			.putString(ctxt.getString(R.string.pref_login_pword_key), sPassword).commit();
	}
	
	/**
	 * Get the value of the current login session's authorisation token ({@link #sAuthToken}). Can re-read
	 * from file if necessary or will just grab the value in memory.
	 * @param ctxt A context
	 * @return The authorisation token
	 */
	public static String getAuthToken(Context ctxt) {
		if (sAuthToken == null) {
			sAuthToken = ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE)
					.getString(ctxt.getString(R.string.pref_login_auth_key), "");
		}
		return sAuthToken;
	}
	
	/**
	 * Set the value of the current login session's authorisation token ({@link #sAuthToken}).
	 * Will immediately write it to file as well.
	 * @param ctxt A context
	 * @param newtoken The new authorisation token
	 * @return True on successfully writing to permanent storage
	 */
	public static Boolean setAuthToken(Context ctxt, String newtoken) {
		sAuthToken = newtoken;
		
		return ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE).edit()
			.putString(ctxt.getString(R.string.pref_login_auth_key), sAuthToken).commit();
	}

	/**
	 * Get whether the user is currently logged in ({@link #sIsLoggedIn}). Can re-read from file 
	 * if necessary or will just grab the value in memory.
	 * @param ctxt A context
	 * @return The login state
	 */
	public static Boolean isLoggedIn(Context ctxt) {
		if (sIsLoggedIn == null) {
			sIsLoggedIn = ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE)
					.getBoolean(ctxt.getString(R.string.pref_login_logged_key), false);
		}
		return sIsLoggedIn;
	}
	
	/**
	 * Set the value of the current login state ({@link #sIsLoggedIn}). Will immediately
	 * write it to file as well. 
	 * @param ctxt A context
	 * @param newval The new login state value
	 * @return True on successfully writing to permanent storage
	 */
	public static Boolean setLoggedIn(Context ctxt, Boolean newval) {
		sIsLoggedIn = newval;
		
		return ctxt.getSharedPreferences(ctxt.getString(R.string.pref_login_file_key), Context.MODE_PRIVATE).edit()
			.putBoolean(ctxt.getString(R.string.pref_login_logged_key), sIsLoggedIn).commit();
	}
}
