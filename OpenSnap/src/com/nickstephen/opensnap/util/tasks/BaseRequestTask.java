package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.http.NetException;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.util.http.ServerResponse;
import com.nickstephen.opensnap.util.http.SnapAPI;

public abstract class BaseRequestTask extends AsyncTask<String, Void, ServerResponse> {
	protected boolean m401Error = false;
	protected boolean mEmptyResponse = false;
	protected final Context mContext;
	protected String mFailureMessage;
	protected String mResultJson;
	private long mStartMillis;
	private int mStatusCode;

	public BaseRequestTask(Context paramContext)
	{
		this.mContext = paramContext;
	}

	protected ServerResponse doInBackground(String... paramArrayOfString)
	{
		GlobalVars.lockNetwork(-1);
		Bundle localBundle;
		try {
			localBundle = SnapAPI.postData(getPath(), getParams(), GlobalVars.getAuthToken(mContext));
		} catch (NetException e) {
			this.mStatusCode = e.getStatusCode();
			this.mFailureMessage = e.getMessage();
			return null;
		}
		GlobalVars.releaseNetwork();
		
		//Bundle localBundle = SnapchatServer.makeRequest(getPath(), getParams(), 2, this.mContext);
		this.mResultJson = localBundle.getString(com.nickstephen.opensnap.util.http.ServerResponse.RESULT_DATA_KEY);
		this.mStatusCode = localBundle.getInt(com.nickstephen.opensnap.util.http.ServerResponse.STATUS_CODE_KEY);
		if (mStatusCode == 200)	{
			if (StatMethods.IsStringNullOrEmpty(mResultJson)) {
				mEmptyResponse = true;
				return null;
			} else {
				return ServerResponse.getResponseFromString(mResultJson);
			}
		} else if (mStatusCode == 401) {
			this.m401Error = true;
			this.mFailureMessage = "Login expired/ un-authorised access";
			onFailAsync(this.mFailureMessage);
			return null;
		} else {
			// Should not occur (gets caught by NetException^^)
			return null;
		}
	}

	protected abstract Bundle getParams();

	protected abstract String getPath();

	protected abstract String getTaskName();

	protected void on401Code()
	{
		//TODO: Logout
		//BusProvider.getInstance().post(new LogoutEvent());
		//new LogoutTask(this.mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[0]);
	}

	protected void onFail(String paramString)
	{
	}

	protected void onFailAsync(String paramString)
	{
	}

	protected void onPostExecute(ServerResponse paramServerResponse)
	{
		Twig.info("OpenSnap RequestTask", getTaskName() + " completed in " + Integer.valueOf((int)(System.currentTimeMillis() - this.mStartMillis)) + " milliseconds");
		if (getTaskName().equalsIgnoreCase("GetProfileInfoTask")) {
			if (this.mStatusCode == 200) {
				onSuccessBFF(this.mResultJson);
				return;
			}
			onFail(this.mFailureMessage);
			return;
		}
		if (this.m401Error)	{
			on401Code();
			return;
		}
		if (StatMethods.IsStringNullOrEmpty(mFailureMessage)) {
			onSuccess(paramServerResponse);
		} else {
			onFail(this.mFailureMessage);
		}
	}

	protected void onPreExecute()
	{
		Twig.info("OpenSnap Request", "Starting new " + getTaskName());
		this.mStartMillis = System.currentTimeMillis();
	}

	protected void onSuccess(ServerResponse paramServerResponse)
	{
	}

	protected void onSuccessAsync(ServerResponse paramServerResponse)
	{
	}

	protected void onSuccessBFF(String paramString)
	{
	}
}
