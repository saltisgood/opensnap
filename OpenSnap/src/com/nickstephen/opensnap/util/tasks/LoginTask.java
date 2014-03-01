package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.Statistics;
import com.nickstephen.opensnap.global.TempSnaps;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerResponse;

import org.holoeverywhere.app.AlertDialog;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public class LoginTask extends BaseRequestTask {
    private static final String NAME = "LoginTask";

    protected final String mUsername;
    protected final String mPassword;
    protected int mNewSnaps;
    protected boolean mError = false;
    protected boolean mIsUserRun = true;

    public LoginTask(Context paramContext, String username, String password) {
        super(paramContext);

        mUsername = username;
        mPassword = password;
        mReuseAuthToken = false;
    }

    public LoginTask(Context paramContext, String username, String password, boolean IsUserRun) {
        this(paramContext, username, password);

        mIsUserRun = IsUserRun;
    }

    @Override
    protected Bundle getParams() {
        Bundle bundle = new Bundle();

        bundle.putString("username", mUsername);
        bundle.putString("password", mPassword);

        return bundle;
    }

    @Override
    protected String getPath() {
        return "/ph/login";
    }

    @Override
    protected String getTaskName() {
        return NAME;
    }

    @Override
    protected void onSuccessAsync(ServerResponse response) {
        if (!response.logged) {
            return;
        }

        GlobalVars.setAuthToken(mContext, response.auth_token);
        GlobalVars.setUsername(mContext, mUsername);
        if (mPassword != null) {
            GlobalVars.setPassword(mContext, mPassword);
        }

        Contacts.sync(response);
        try {
            Contacts.saveToFile(mContext);
        } catch (Exception e) {
            Twig.printStackTrace(e);
            mError = true;
        }

        mNewSnaps = LocalSnaps.getInstanceUnsafe().sync(new LocalSnaps(response.snaps));
        try {
            LocalSnaps.getInstanceUnsafe().serialiseToFile(mContext);
        } catch (Exception e) {
            Twig.printStackTrace(e);
            mError = true;
        }
        Statistics.Sync(response, mContext);

        TempSnaps.resetLite(mContext);

        GlobalVars.setLoggedIn(mContext, true);
    }

    @Override
    protected void onSuccess(ServerResponse response) {
        if (!response.logged) {
            if (mIsUserRun) {
                StatMethods.hotBread(mContext, "Login failed:\nIncorrect username or password", Toast.LENGTH_LONG);
                Broadcast.loginComplete(false);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Login Failed!")
                        .setMessage("Login of cached username and password has failed! This is normally " +
                                "because your password has changed since you last logged in with OpenSnap. " +
                                "You will have to sign out and sign in with your new password before you can " +
                                "send or receive snaps.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
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
            if (mIsUserRun) {
                Broadcast.loginComplete(true);
            } else {

            }
        }
    }

    @Override
    protected void onFinish() {
        Broadcast.finishUpdate();
    }
}
