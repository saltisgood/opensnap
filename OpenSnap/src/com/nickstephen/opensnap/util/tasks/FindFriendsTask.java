package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.nickstephen.opensnap.global.Statistics;

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

        mContacts = mContacts.subList(0, 20);

        String contacts = "{";
        for (int i = 0; i < mContacts.size() - 1; i++) {
            contacts += "\"" + mContacts.get(i).numbers.get(0) + "\":\"" + mContacts.get(i).name + "\",";
        }
        contacts += "\"" + mContacts.get(mContacts.size() - 1).numbers.get(0) + "\":\"" + mContacts.get(mContacts.size() - 1).name + "\"}";

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
}
