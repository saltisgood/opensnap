package com.nickstephen.opensnap.util.http;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public class ServerStoryNotes {
    private String viewer;
    private boolean screenshotted;
    private int timestamp;
    private StoryPointer storypointer;

    public static class StoryPointer {
        private String mKey;
        private String mField;
    }
}
