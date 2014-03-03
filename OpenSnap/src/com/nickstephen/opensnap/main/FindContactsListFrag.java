package com.nickstephen.opensnap.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.tasks.ContactLoader;
import com.nickstephen.opensnap.util.tasks.FindFriendsTask;
import com.nickstephen.opensnap.util.tasks.FriendTask;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;

import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.TextView;

import java.util.List;

/**
 * Created by Nick Stephen on 28/02/14.
 */
public class FindContactsListFrag extends ListFragment implements IOnObjectReady<List<ContactLoader.PhoneContact>> {
    public static final String FRAG_TAG = "com.nickstephen.opensnap.main.FindContactsListFrag";

    private List<ContactLoader.PhoneContact> mContacts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Broadcast.registerFindContactsFrag(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new ContactLoader(this.getActivity()).execute();
    }

    @Override
    public void objectReady(List<ContactLoader.PhoneContact> obj) {
        new FindFriendsTask(this.getActivity(), GlobalVars.getUsername(this.getActivity()), obj)
                .execute();
    }

    public void onContactsFinished(List<ContactLoader.PhoneContact> contacts) {
        mContacts = contacts;
        if (contacts.size() == 0) {
            StatMethods.hotBread(this.getActivity(), "No new friends found in your contacts", Toast.LENGTH_SHORT);
            this.getActivity().onBackPressed();
        } else {
            this.setListAdapter(new ContactAdapter(this.getActivity()));
        }
    }

    public void onContactFailure(int errCode) {
        if (errCode == 503) {
            StatMethods.hotBread(this.getActivity(), "Service temporarily unavailable. Try again later.", Toast.LENGTH_LONG);
            this.getActivity().onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Broadcast.unregisterFindContactsFrag();
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
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.add_contact_list_item, null);

            TextView txt = (TextView) view.findViewById(R.id.contact_name);
            txt.setText(getItem(position).displayName);

            txt = (TextView) view.findViewById(R.id.contact_number);
            txt.setText(getItem(position).userName);

            View butt = view.findViewById(R.id.add_friend_button);
            butt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new FriendTask(FindContactsListFrag.this.getActivity(),
                            GlobalVars.getUsername(FindContactsListFrag.this.getActivity()),
                            getItem(position).userName, FriendTask.FriendAction.ADD).execute();
                    StatMethods.hotBread(FindContactsListFrag.this.getActivity(),
                            "Adding friend...", Toast.LENGTH_SHORT);
                }
            });

            return view;
        }
    }
}
