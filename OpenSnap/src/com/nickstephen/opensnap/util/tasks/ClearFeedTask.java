package com.nickstephen.opensnap.util.tasks;

import org.holoeverywhere.widget.Toast;

import android.content.Context;
import android.os.Bundle;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.util.http.ServerResponse;

public class ClearFeedTask extends BaseRequestTask {
	private static final String NAME = "ClearFeedTask";
	
	private final String mUsername;

	public ClearFeedTask(Context paramContext, String user) {
		super(paramContext);
		
		mUsername = user;
	}

	@Override
	protected Bundle getParams() {
		Bundle bundle = new Bundle();
	    bundle.putString("username", mUsername);
	    return bundle;
	}

	@Override
	protected String getPath() {
		return "/ph/clear";
	}

	@Override
	protected String getTaskName() {
		return NAME;
	}

	@Override
	protected void onSuccess(ServerResponse response) {
		StatMethods.hotBread(mContext, "Feed cleared", Toast.LENGTH_SHORT);
	}
}
