package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerResponse;

/**
 * Created by Nick Stephen on 28/02/14.
 */
public class UpdateTask extends LoginTask {
    private static final String NAME = "UpdateTask";

    public UpdateTask(Context context, String username) {
        super(context, username, null);

        this.mReuseAuthToken = true;
    }

    @Override
    protected Bundle getParams() {
        Bundle bundle = new Bundle();
        bundle.putString("username", mUsername);
        return bundle;
    }

    @Override
    protected String getPath() {
        return "/bq/updates";
    }

    @Override
    protected String getTaskName() {
        return NAME;
    }

    @Override
    protected void onPreExecute() {
        Broadcast.startUpdate();
    }

    @Override
    protected void onSuccess(ServerResponse response) {
        if (!response.logged) { // Shouldn't reach here. Should 401 before it gets to here
            StatMethods.hotBread(mContext, "Login failed:\nIncorrect username or password", Toast.LENGTH_LONG);
        } else if (mError) {

        } else {
            if (SettingsAccessor.getUpdateToastPref(mContext)) {
                if (mNewSnaps == 0) {
                    StatMethods.hotBread(mContext, "No new snaps :(", org.holoeverywhere.widget.Toast.LENGTH_SHORT);
                } else if (mNewSnaps == 1) {
                    StatMethods.hotBread(mContext, "1 new snap!", org.holoeverywhere.widget.Toast.LENGTH_SHORT);
                } else if (mNewSnaps > 1) {
                    StatMethods.hotBread(mContext, mNewSnaps + " new snaps!", org.holoeverywhere.widget.Toast.LENGTH_SHORT);
                }
            }

            if (LocalSnaps.getInstanceUnsafe().getNumberOfSnaps() >= SettingsAccessor.getCloudSnapListSize(mContext) && LocalSnaps.getInstanceUnsafe().shouldClear()) {
                new ClearFeedTask(mContext, GlobalVars.getUsername(mContext)).execute();
            }
        }

        Broadcast.finishUpdate();
    }

    @Override
    protected void on401Code() {
        GlobalVars.setLoggedIn(mContext, false);
        StatMethods.hotBread(mContext, "Login expired. Attempting to re-login", Toast.LENGTH_SHORT);
        new LoginTask(mContext, mUsername, GlobalVars.getPassword(mContext), false).execute();
    }
}
