package com.nickstephen.opensnap.util;

import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.main.ContactViewerListFrag;
import com.nickstephen.opensnap.main.LaunchActivity;
import com.nickstephen.opensnap.main.MainMenuFrag;
import com.nickstephen.opensnap.main.SnapViewerListFrag;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public final class Broadcast {
    private static ContactViewerListFrag mContactViewer;
    private static LaunchActivity mLaunchActivity;
    private static SnapViewerListFrag mSnapViewer;
    private static MainMenuFrag mMainMenu;

    private Broadcast() {
    }

    public static void finishUpdate() {
        if (mLaunchActivity != null) {
            mLaunchActivity.onUpdateFinish();
        }
        if (mSnapViewer != null) {
            mSnapViewer.onUpdateComplete();
        }
        if (mMainMenu != null) {
            mMainMenu.setUpdateText(LocalSnaps.getUnseenSnaps());
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
            mMainMenu.setUpdateText(LocalSnaps.getUnseenSnaps());
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
}
