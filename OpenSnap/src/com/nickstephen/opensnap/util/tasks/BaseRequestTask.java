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
    protected boolean mReuseAuthToken = true;

	public BaseRequestTask(Context paramContext)
	{
		this.mContext = paramContext;
	}

	protected ServerResponse doInBackground(String... paramArrayOfString)
	{
        String authToken = null;
        if (mReuseAuthToken) {
            authToken = GlobalVars.getAuthToken(mContext);
        }

		GlobalVars.lockNetwork(-1);
		Bundle localBundle;
		try {
			localBundle = SnapAPI.postData(getPath(), getParams(), authToken);
		} catch (NetException e) {
			this.mStatusCode = e.getStatusCode();
			this.mFailureMessage = e.getMessage();
			return null;
		} finally {
            GlobalVars.releaseNetwork();
        }
		
		//Bundle localBundle = SnapchatServer.makeRequest(getPath(), getParams(), 2, this.mContext);
		this.mResultJson = localBundle.getString(com.nickstephen.opensnap.util.http.ServerResponse.RESULT_DATA_KEY);
		this.mStatusCode = localBundle.getInt(com.nickstephen.opensnap.util.http.ServerResponse.STATUS_CODE_KEY);
		if (mStatusCode == 200)	{
			if (StatMethods.IsStringNullOrEmpty(mResultJson)) {
				mEmptyResponse = true;
				return null;
			} else {
                ServerResponse response = ServerResponse.getResponseFromString((mResultJson));
                onSuccessAsync(response);
				return response;
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

	protected final void onPostExecute(ServerResponse response)
	{
		Twig.info("OpenSnap RequestTask", getTaskName() + " completed in " + (int) (System.currentTimeMillis() - this.mStartMillis) + " milliseconds");
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
			onSuccess(response);
		} else {
			onFail(this.mFailureMessage);
		}

        onFinish();
	}

	protected void onPreExecute()
	{
		Twig.info("OpenSnap Request", "Starting new " + getTaskName());
		this.mStartMillis = System.currentTimeMillis();
	}

	protected void onSuccess(ServerResponse response)
	{
	}

	protected void onSuccessAsync(ServerResponse response)
	{
	}

	protected void onSuccessBFF(String paramString)
	{
	}

    /**
     * This method is called after all other methods in onPostExecute and is just used to clean up
     * / finish things regardless of whether an error occurred or not
     */
    protected void onFinish() {    }
}
