package com.nickstephen.opensnap.util.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.util.tasks.DeviceSyncTask;

public class SnapGCMMessageReceiver extends BroadcastReceiver {
	public static final String DISPLAY_MESSAGE_ACTION = "com.nickstephen.opensnap.NOTIFICATION";
	
	private static final String REG_ID_KEY = "registration_id";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Twig.verbose("OpenSnap Receiver", "GCM Message Received!");
		String str = intent.getStringExtra(REG_ID_KEY);
		if (!StatMethods.IsStringNullOrEmpty(str)) {
			SnapGCMRegistrar.setRegistrationId(context, str);
			new DeviceSyncTask(context, str).execute(new String[0]);
		} else {
			broadcastFromGCMToApp(context, intent);
		}
	}
	
	public static void broadcastFromGCMToApp(Context paramContext, Intent paramIntent) {
		Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
		if (paramIntent != null) {
			intent.replaceExtras(paramIntent);
		}
		paramContext.sendOrderedBroadcast(intent, null);
	}
}
