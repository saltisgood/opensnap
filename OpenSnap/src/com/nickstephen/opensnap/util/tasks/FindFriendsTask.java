package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;

import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.global.Statistics;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerFriend;
import com.nickstephen.opensnap.util.http.ServerResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Stephen on 28/02/14.
 */
public class FindFriendsTask extends BaseRequestTask {
    private static final String NAME = "FindFriendsTask";

    private final String mUsername;
    private List<ContactLoader.PhoneContact> mContacts;

    public FindFriendsTask(Context context, String username, List<ContactLoader.PhoneContact> contacts) {
        super(context);

        mUsername = username;
        mContacts = contacts;
    }

    @Override
    protected Bundle getParams() {
        Bundle bundle = new Bundle();

        bundle.putString("username", mUsername);
        bundle.putString("countryCode", Statistics.getInstanceUnsafe().getCountryCode());

        String contacts = "{";
        for (int i = 0; i < mContacts.size() - 1; i++) {
            contacts += "\"" + mContacts.get(i).numbers.get(0) + "\":\"" + mContacts.get(i).displayName + "\",";
        }
        contacts += "\"" + mContacts.get(mContacts.size() - 1).numbers.get(0) + "\":\"" + mContacts.get(mContacts.size() - 1).displayName + "\"}";

        bundle.putString("numbers", contacts);

        //bundle.putString("numbers", "{\"0415191804\": \"Ben madden\", \"0420576970\": \"Nicholas Stephen\"}");

        return bundle;
    }

    @Override
    protected String getPath() {
        return "/ph/find_friends";
    }

    @Override
    protected String getTaskName() {
        return NAME;
    }

    @Override
    protected void onFail(String paramString) {
        Broadcast.onFindFriendsFailure(this.mStatusCode);
    }

    @Override
    protected void onSuccessAsync(ServerResponse response) {
        mContacts = new ArrayList<ContactLoader.PhoneContact>();
        for (ServerFriend friend : response.results) {
            if (Contacts.getInstanceUnsafe().contactExists(friend.name) == -1) {
                mContacts.add(new ContactLoader.PhoneContact(friend));
            }
        }
    }

    @Override
    protected void onSuccess(ServerResponse response) {
        Broadcast.onFindFriendsFinished(mContacts);
    }
}
