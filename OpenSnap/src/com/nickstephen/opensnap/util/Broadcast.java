package com.nickstephen.opensnap.util;

import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.Statistics;
import com.nickstephen.opensnap.main.ContactViewerListFrag;
import com.nickstephen.opensnap.main.LaunchActivity;
import com.nickstephen.opensnap.main.MainMenuFrag;
import com.nickstephen.opensnap.main.SnapViewerListFrag;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public final class Broadcast {
    private static ContactViewerListFrag mContactViewer;
    private static LaunchActivity mLaunchActivity;
    private static SnapViewerListFrag mSnapViewer;
    private static MainMenuFrag mMainMenu;
    private static List<IOnObjectReady<LocalSnaps>> mSnapWaiters = new ArrayList<IOnObjectReady<LocalSnaps>>();
    private static List<IOnObjectReady<Contacts>> mContactWaiters = new ArrayList<IOnObjectReady<Contacts>>();
    private static List<IOnObjectReady<Statistics>> mStatisticsWaiters = new ArrayList<IOnObjectReady<Statistics>>();

    private Broadcast() {
        // Never called
    }

    public static void finishUpdate() {
        if (mLaunchActivity != null) {
            mLaunchActivity.onUpdateFinish();
        }
        if (mSnapViewer != null) {
            mSnapViewer.onUpdateComplete();
        }
        if (mMainMenu != null) {
            mMainMenu.setUpdateText(LocalSnaps.getInstanceUnsafe().getUnseenSnaps());
        }
    }

    public static void loginComplete(boolean wasSuccessful) {
        if (mLaunchActivity != null) {
            mLaunchActivity.onLoginComplete(wasSuccessful);
        }
    }

    public static void logout() {
        //TODO: Implement some kind of system here
    }

    public static void onContactsReady() {
        for (IOnObjectReady<Contacts> waiter : mContactWaiters) {
            waiter.objectReady(Contacts.getInstanceUnsafe());
        }

        mContactWaiters.clear();
    }

    public static void onSnapsReady() {
        for (IOnObjectReady<LocalSnaps> waiter : mSnapWaiters) {
            waiter.objectReady(LocalSnaps.getInstanceUnsafe());
        }

        mSnapWaiters.clear();
    }

    public static void onStatisticsReady() {
        for (IOnObjectReady<Statistics> waiter : mStatisticsWaiters) {
            waiter.objectReady(Statistics.getInstanceUnsafe());
        }

        mStatisticsWaiters.clear();
    }

    public static void refreshContactViewer() {
        if (mContactViewer != null) {
            mContactViewer.refresh();
        }
    }

    public static void refreshSnapViewerListFrag() {
        if (mSnapViewer != null) {
            mSnapViewer.refresh();
        }
    }

    public static void refreshMainMenuFrag() {
        if (mMainMenu != null) {
            mMainMenu.setUpdateText(LocalSnaps.getInstanceUnsafe().getUnseenSnaps());
        }
    }

    public static void registerContactViewer(@NotNull ContactViewerListFrag frag) {
        mContactViewer = frag;
    }

    public static void registerLaunchActivity(@NotNull LaunchActivity activity) {
        mLaunchActivity = activity;
    }

    public static void registerMainMenuFrag(@NotNull MainMenuFrag frag) {
        mMainMenu = frag;
    }

    public static void registerSnapViewerListFrag(@NotNull SnapViewerListFrag frag) {
        mSnapViewer = frag;
    }

    public static void startUpdate() {
        if (mLaunchActivity != null) {
            mLaunchActivity.onUpdateStart();
        }
    }

    public static void unregisterContactViewer() {
        mContactViewer = null;
    }

    public static void unregisterLaunchActivity() {
        mLaunchActivity = null;
    }

    public static void unregisterMainMenuFrag() {
        mMainMenu = null;
    }

    public static void unregisterSnapViewerListFrag() {
        mSnapViewer = null;
    }

    public static void waitForContacts(IOnObjectReady<Contacts> waiter) {
        if (Contacts.checkInit()) {
            waiter.objectReady(Contacts.getInstanceUnsafe());
        } else {
            mContactWaiters.add(waiter);
        }
    }

    public static void waitForSnaps(IOnObjectReady<LocalSnaps> waiter) {
        if (LocalSnaps.checkInit()) {
            waiter.objectReady(LocalSnaps.getInstanceUnsafe());
        } else {
            mSnapWaiters.add(waiter);
        }
    }

    public static void waitForStatistics(IOnObjectReady<Statistics> waiter) {
        if (Statistics.checkInit()) {
            waiter.objectReady(Statistics.getInstanceUnsafe());
        } else {
            mStatisticsWaiters.add(waiter);
        }
    }
}
