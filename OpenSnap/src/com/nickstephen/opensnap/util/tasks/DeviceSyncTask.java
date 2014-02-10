package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;

import com.nickstephen.lib.Twig;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.util.http.ServerResponse;

public class DeviceSyncTask extends BaseRequestTask {
	private static final String TASK_NAME = "DeviceSyncTask";
	private final String DEVICE_TYPE = "android";
	private final String mGcmRegistrationId;
	//private User mUser = User.getInstanceUnsafe();

	public DeviceSyncTask(Context paramContext, String regId) {
		super(paramContext);
		
		mGcmRegistrationId = regId;
	}

	@Override
	protected Bundle getParams() {
		Bundle bundle = new Bundle();
		bundle.putString("username", GlobalVars.getUsername(mContext));
		bundle.putString("type", DEVICE_TYPE);
		bundle.putString("device_token", mGcmRegistrationId);
		return bundle;
	}

	@Override
	protected String getPath() {
		return "/ph/device";
	}

	@Override
	protected String getTaskName() {
		return TASK_NAME;
	}

	@Override
	protected void onFail(String paramString) {
		Twig.debug("OpenSnap - DeviceSyncTask", "Device reg failure: " + paramString);
	}
	
	@Override
	protected void onSuccess(ServerResponse response) {
		if (response != null) {
			Twig.debug("OpenSnap - DeviceSyncTask", "Device reg success: " + response);
		}
	}
}
