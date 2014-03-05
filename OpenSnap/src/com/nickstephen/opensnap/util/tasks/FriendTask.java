package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerResponse;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public class FriendTask extends BaseRequestTask {
    private static final String NAME = "FriendTask";

    private final String mUsername;
    private final String mFriend;
    private final FriendAction mAction;
    private final String mFriendDisplay;

    public FriendTask(Context paramContext, String user, String friend, FriendAction action) {
        super(paramContext);

        mUsername = user;
        mFriend = friend;
        mAction = action;
        mFriendDisplay = null;
    }

    public FriendTask(Context paramContext, String user, String friend, String display, FriendAction action) {
        super(paramContext);

        mUsername = user;
        mFriend = friend;
        mFriendDisplay = display;
        mAction = action;
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
    protected void onSuccess(ServerResponse response) {
        if (mAction == FriendAction.DISPLAY) {
            Contacts.getInstanceUnsafe().setDisplayName(mFriend, mFriendDisplay);
            Contacts.getInstanceUnsafe().sort();
            Contacts.getInstanceUnsafe().serialiseToFile(mContext);
        } else if (mAction == FriendAction.ADD) {
            if (response.object == null) { // User couldn't be found
                StatMethods.hotBread(mContext, response.message, Toast.LENGTH_SHORT);
                return;
            } else {
                Contacts.getInstanceUnsafe().addFriend(response.object);
                Contacts.getInstanceUnsafe().serialiseToFile(mContext);

                if (mFriendDisplay != null) {
                    new FriendTask(mContext, mUsername, mFriend, mFriendDisplay, FriendAction.DISPLAY)
                            .execute();
                    StatMethods.hotBread(mContext, mFriendDisplay + " is now your friend!", Toast.LENGTH_SHORT);
                } else {
                    StatMethods.hotBread(mContext, mFriend + " is now your friend!", Toast.LENGTH_SHORT);
                }
            }

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
