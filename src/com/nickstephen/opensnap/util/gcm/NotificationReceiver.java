package com.nickstephen.opensnap.util.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.main.LaunchActivity;

public class NotificationReceiver extends BroadcastReceiver {
	public static final int FRIEND_REQUEST_NOTIFICATION = "2baaawnburb1f3dndddhh83243".hashCode();
	public static final int RECEIVED_SNAP_NOTIFICATION = "2peacheszxcsnapchat88whdsb3243".hashCode();
	public static final int SCREENSHOT_NOTIFICATION = "2ballfacechillahzx8vhf8hh83243".hashCode();
	public static final String LAUNCH_GOTO_FEED = "goToFeedFrag";

	private static final String WAKE_LOCK_TAG = "FuckSnapchatLock";
	private static final String PREFS_KEY_SNAPS_SINCE_LAST_TIME = "snapsSinceLastOpened";
	private static final String PREFS_KEY_SENDERS_SINCE_LAST_TIME = "sendersSinceLastOpened";
	
	public NotificationReceiver() {
		
	}

	@Override
	public void onReceive(Context paramContext, Intent paramIntent) {
		String user = paramIntent.getExtras().getString("username");
	    String screenshotter = paramIntent.getExtras().getString("screenshot_taker");
	    String sender = paramIntent.getExtras().getString("sender");
	    String text = paramIntent.getExtras().getString("text");
	    String friender = paramIntent.getExtras().getString("friend_adder");
	    
	    Twig.debug("OpenSnap - NotificationReceiver", "Received message: user - " + user + ", screen - " + screenshotter + ", sender - " + sender + 
	    		", text - " + text + ", friender - " + friender);
	    
	    if (GlobalVars.getUsername(paramContext).compareTo(user) != 0) {
	    	Twig.warning("OpenSnap - NotificationReceiver", "Mismatch of usernames!");
	    	return;
	    }
	    
	    if (screenshotter != null) {
	    	generateNotification(paramContext, "OpenSnap", screenshotter + " took a screenshot!", screenshotter + " took a screenshot!", SCREENSHOT_NOTIFICATION);
	    	return;
	    }
	    
	    if (!StatMethods.IsStringNullOrEmpty(friender)) {
	    	generateNotification(paramContext, "OpenSnap", friender + " added you!", friender + " added you!", FRIEND_REQUEST_NOTIFICATION);
	    	return;
	    }
	    
	    if (sender != null) {
	    	generateReceivedSnapNotification(paramContext, sender, PreferenceManager.getDefaultSharedPreferences(paramContext));
	    }
	}

	public static void generateNotification(Context paramContext, String title, String text, String ticker, int notificationCode) {
		Intent intent = new Intent(paramContext, LaunchActivity.class);
		intent.putExtra(LAUNCH_GOTO_FEED, true);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(paramContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationManager notManager = (NotificationManager) paramContext.getSystemService(Context.NOTIFICATION_SERVICE);
		notManager.cancel(notificationCode);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(paramContext).setContentTitle(title).setContentText(text)
				.setTicker(ticker).setAutoCancel(true).setWhen(System.currentTimeMillis()).setSmallIcon(R.drawable.ghost)
				.setVibrate(new long[] { 0L, 50L, 100L, 180L }).setDefaults(Notification.DEFAULT_SOUND).setLights(0xFFFFFF00, 1000, 2000)
				.setContentIntent(pendingIntent);
		wakeScreen(paramContext);
		notManager.notify(notificationCode, builder.build());
	}
	
	public static void generateReceivedSnapNotification(Context paramContext, String sender, SharedPreferences sharedPrefs) {
		SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
		int unseenSnaps = sharedPrefs.getInt(PREFS_KEY_SNAPS_SINCE_LAST_TIME, 0);
		unseenSnaps++;
		String unseenSenders = sharedPrefs.getString(PREFS_KEY_SENDERS_SINCE_LAST_TIME, "");
		String title = "OpenSnap";
		String text = "New Snap from " + sender + "!";
		String ticker = "New snap from " + sender + "!";
		if (unseenSenders.compareTo("") != 0) {
			if (!senderIsInListOfSendersSinceLastOpened(sender, unseenSenders)) {
				unseenSenders = sender + ", " + unseenSenders;
			}
			title = unseenSnaps + " new mSnaps!";
			text = unseenSenders;
		}
		
		generateNotification(paramContext, title, text, ticker, RECEIVED_SNAP_NOTIFICATION);
		prefsEditor.putInt(PREFS_KEY_SNAPS_SINCE_LAST_TIME, unseenSnaps);
		prefsEditor.putString(PREFS_KEY_SENDERS_SINCE_LAST_TIME, unseenSenders);
		prefsEditor.commit();
	}
	
	private static boolean senderIsInListOfSendersSinceLastOpened(String sender, String senderList)
	{
		if (senderList.contains(sender)) {
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	private static void wakeScreen(Context paramContext) {
		PowerManager powerManager = (PowerManager)paramContext.getSystemService(Context.POWER_SERVICE);
		if (!powerManager.isScreenOn()) {
			if (Build.VERSION.SDK_INT < 17) {
				powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, WAKE_LOCK_TAG).acquire(3000L);
			} else {
				powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKE_LOCK_TAG).acquire(3000L);
			}
		}
	}
}
