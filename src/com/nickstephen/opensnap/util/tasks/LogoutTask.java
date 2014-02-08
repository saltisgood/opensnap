package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;

public class LogoutTask extends BaseRequestTask {
	private static final String USER_KEY = "username";
	private static final String JSON_KEY = "json";
	private static final String EVENT_KEY = "events";
	
	private final String mUsername;

	public LogoutTask(Context paramContext, String user) {
		super(paramContext);
		
		mUsername = user;
	}

	@Override
	protected Bundle getParams() {
		Bundle bundle = new Bundle();
		bundle.putString(USER_KEY, mUsername);
		// Both these keys seem to be necessary for a proper logout (otherwise throws a 500 (server side error))
		// Both are JSON strings. Just pass an empty JSON instead 
		// This seems to be for syncing any opened snaps that haven't yet been sent to the server
		bundle.putString(JSON_KEY, "{}");
		// This seems to be for syncing any screenshots that haven't yet been sent to the server
		bundle.putString(EVENT_KEY, "{}");
		return bundle;
	}

	@Override
	protected String getPath() {
		return "/ph/logout";
	}

	@Override
	protected String getTaskName() {
		return "LogoutTask";
	}

}
