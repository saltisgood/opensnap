package com.nickstephen.opensnap.util.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerFriend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Stephen on 28/02/14.
 */
public class ContactLoader extends AsyncTask<Void, Void, List<ContactLoader.PhoneContact>> {
    private final Context mContext;

    public ContactLoader(Context context) {
        mContext = context;
    }

    @Override
    protected List<PhoneContact> doInBackground(Void... params) {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        List<PhoneContact> contacts = new ArrayList<PhoneContact>();

        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                PhoneContact contact = new PhoneContact(name);

                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id},
                            null);
                    if (pCur != null) {
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            contact.numbers.add(phoneNo);
                        }
                        pCur.close();
                    }
                }

                if (contact.numbers.size() > 0) {
                    boolean found = false;
                    for (PhoneContact c : contacts) {
                        if (contact.numbers.get(0).compareTo(c.numbers.get(0)) == 0) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        contacts.add(contact);
                    }
                }
            }
            cur.close();
        }

        return contacts;
    }

    @Override
    protected void onPostExecute(List<PhoneContact> phoneContacts) {
        Broadcast.onNewContactsReady(phoneContacts);
    }

    public static class PhoneContact {
        public String displayName;
        public List<String> numbers;
        public String userName;

        public PhoneContact(String nam) {
            displayName = nam;
            numbers = new ArrayList<String>();
        }

        public PhoneContact(ServerFriend friend) {
            displayName = friend.display;
            userName = friend.name;
        }
    }
}
