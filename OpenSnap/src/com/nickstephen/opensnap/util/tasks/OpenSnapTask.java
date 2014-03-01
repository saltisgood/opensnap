package com.nickstephen.opensnap.util.tasks;

import java.util.Date;

import android.content.Context;
import android.os.Bundle;

import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.LocalSnaps.LocalSnap;
import com.nickstephen.opensnap.util.http.ServerResponse;

public class OpenSnapTask extends BaseRequestTask {
	private static final String TASKNAME = "UpdateSnapsTask";
	private static final String USER_KEY = "username";
	private static final String JSON_KEY = "json";
	
	private final LocalSnap mUpdateSnap;
	private final String mUsername;
	
	public OpenSnapTask(Context paramContext, LocalSnap snap, String username) {
		super(paramContext);
		
		mUpdateSnap = snap;
		mUsername = username;
	}

	@Override
	protected Bundle getParams() {
		Bundle bundle = new Bundle();
		bundle.putString(USER_KEY, mUsername);
		bundle.putString(JSON_KEY, "{\"" + mUpdateSnap.getSnapId() + "\":{\"t\":" + Long.valueOf(new Date().getTime() / 1000L).toString() + 
				",\"c\":0}}"); // t = 0: opened, t = 1: screenshotted
		
		return bundle;
	}

	@Override
	protected String getPath() {
		return "/bq/update_snaps";
	}

	@Override
	protected String getTaskName() {
		return TASKNAME;
	}

	@Override
	protected void onSuccess(ServerResponse response) {
		mUpdateSnap.setOpened();
		try {
			LocalSnaps.getInstanceUnsafe().serialiseToFile(mContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
