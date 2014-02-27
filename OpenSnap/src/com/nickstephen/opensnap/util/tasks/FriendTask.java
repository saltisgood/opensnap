package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerResponse;
import com.nickstephen.opensnap.util.misc.CustomJSON;

import org.json.JSONException;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public class FriendTask extends BaseRequestTask {
    private static final String NAME = "FriendTask";

    private final String mUsername;
    private final String mFriend;
    private final FriendAction mAction;
    private String mFriendDisplay;

    public FriendTask(Context paramContext, String user, String friend, FriendAction action) {
        super(paramContext);

        mUsername = user;
        mFriend = friend;
        mAction = action;
        mFriendDisplay = null;
    }

    public FriendTask(Context paramContext, String user, String friend, String display) {
        this(paramContext, user, friend, FriendAction.DISPLAY);

        mFriendDisplay = display;
    }

    @Override
    protected Bundle getParams() {
        Bundle params = new Bundle();
        params.putString("username", mUsername);
        params.putString("action", getActionName(mAction));
        params.putString("friend", mFriend);

        if (mAction == FriendAction.DISPLAY) {
            params.putString("display", mFriendDisplay);
        }

        return params;
    }

    @Override
    protected String getPath() {
        return "/ph/friend";
    }

    @Override
    protected String getTaskName() {
        return NAME;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        StatMethods.hotBread(mContext, "Working...", Toast.LENGTH_SHORT);
    }

    @Override
    protected void onSuccess(ServerResponse paramServerResponse) {
        if (mAction == FriendAction.DISPLAY) {
            Contacts.setDisplayName(mFriend, mFriendDisplay);
            Contacts.sort();
            StatMethods.hotBread(mContext, "Name changed successfully", Toast.LENGTH_SHORT);
        }
        Broadcast.refreshContactViewer();
    }

    public enum FriendAction {
        ADD, DELETE, BLOCK, UNBLOCK, DISPLAY
    }

    public static String getActionName(FriendAction action) {
        switch (action) {
            case ADD:
                return "add";
            case DELETE:
                return "delete";
            case BLOCK:
                return "block";
            case UNBLOCK:
                return "unblock";
            case DISPLAY:
                return "display";
        }
        return null;
    }
}
