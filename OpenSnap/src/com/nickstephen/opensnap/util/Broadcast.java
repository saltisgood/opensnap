package com.nickstephen.opensnap.util;

import com.nickstephen.opensnap.main.ContactViewerListFrag;
import com.nickstephen.opensnap.main.LaunchActivity;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public final class Broadcast {
    private Broadcast() {}

    private static ContactViewerListFrag mContactViewer;
    private static LaunchActivity mLaunchActivity;

    public static void registerContactViewer(ContactViewerListFrag frag) {
        mContactViewer = frag;
    }

    public static void unregisterContactViewer() {
        mContactViewer = null;
    }

    public static void refreshContactViewer() {
        if (mContactViewer != null) {
            mContactViewer.Refresh();
        }
    }

    public static void registerLaunchActivity(LaunchActivity activity) {
        mLaunchActivity = activity;
    }

    public static void unregisterLaunchActivity() {
        mLaunchActivity = null;
    }

    public static void loginComplete(boolean wasSuccessful) {
        if (mLaunchActivity != null) {
            mLaunchActivity.onLoginComplete(wasSuccessful);
        }
    }
}
