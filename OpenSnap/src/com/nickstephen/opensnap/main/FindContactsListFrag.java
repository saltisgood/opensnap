package com.nickstephen.opensnap.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.util.tasks.ContactLoader;
import com.nickstephen.opensnap.util.tasks.FindFriendsTask;

import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.TextView;

import java.util.List;

/**
 * Created by Nick Stephen on 28/02/14.
 */
public class FindContactsListFrag extends ListFragment {
    public static final String FRAG_TAG = "com.nickstephen.opensnap.main.FindContactsListFrag";

    private boolean mListReady = false;
    private List<ContactLoader.PhoneContact> mContacts;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListReady = true;
        new ContactLoader(this).execute();
    }

    public void onContactsReady(List<ContactLoader.PhoneContact> contacts) {
        mContacts = contacts;
        new FindFriendsTask(this.getActivity(), GlobalVars.getUsername(this.getActivity()), mContacts)
                .execute();

        if (mListReady) {
            this.setListAdapter(new ContactAdapter(this.getActivity()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListReady = false;
    }

    private class ContactAdapter extends ArrayAdapter<ContactLoader.PhoneContact> {
        private final LayoutInflater mInflater;

        public ContactAdapter(Context context) {
            super(context, 0);

            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mContacts.size();
        }

        @Override
        public ContactLoader.PhoneContact getItem(int position) {
            return mContacts.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //TODO: Implement the proper version of this
            View view = mInflater.inflate(R.layout.add_contact_list_item, null);

            TextView txt = (TextView) view.findViewById(R.id.contact_name);
            txt.setText(getItem(position).name);

            txt = (TextView) view.findViewById(R.id.contact_number);
            txt.setText(getItem(position).numbers.get(0));

            return view;
        }
    }
}
