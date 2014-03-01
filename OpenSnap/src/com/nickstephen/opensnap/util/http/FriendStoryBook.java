package com.nickstephen.opensnap.util.http;

import java.util.List;

/**
 * Created by Nick Stephen on 27/02/14.
 */
public class FriendStoryBook {
    public String username;
    public List<FriendStory> stories;

    public static class FriendStory {
        public boolean viewed;
        public ServerStory story;
    }
}
