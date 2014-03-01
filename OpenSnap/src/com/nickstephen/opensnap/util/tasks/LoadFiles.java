package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.nickstephen.lib.Twig;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.Statistics;
import com.nickstephen.opensnap.global.Stories;
import com.nickstephen.opensnap.global.TempSnaps;
import com.nickstephen.opensnap.util.Broadcast;

/**
 * Loads the files like contact information, snap information and statistics in the background
 * Created by Nick Stephen on 1/03/14.
 */
public class LoadFiles extends AsyncTask<Void, Void, Void> {
    public static final String NAME = "LoadFilesAsync";

    private final Context mContext;

    public LoadFiles(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (!Contacts.init(mContext)) {
            Twig.debug(NAME, "Contacts init failed");
        }

        if (!LocalSnaps.init(mContext)) {
            Twig.debug(NAME, "LocalSnaps init failed");
        }

        int errCode;
        if ((errCode = Statistics.init(mContext)) < 0) {
            Twig.debug(NAME, "Statistics init failed: " + errCode);
        }

        TempSnaps.init(mContext);

        Stories.init(mContext);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Broadcast.onContactsReady();
        Broadcast.onSnapsReady();
        Broadcast.onStatisticsReady();
    }
}
