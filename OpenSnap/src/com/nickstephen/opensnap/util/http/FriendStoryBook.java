package com.nickstephen.opensnap.util.http;

import java.util.List;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public class FriendStoryBook {
    private String username;
    private List<FriendStory> stories;

    public static class FriendStory {
        private boolean viewed;
        private ServerStory story;
    }
}
