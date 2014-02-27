package com.nickstephen.opensnap.util;

import com.nickstephen.opensnap.main.ContactViewerListFrag;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public final class Broadcast {
    private Broadcast() {}

    private static ContactViewerListFrag mContactViewer;

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
}
