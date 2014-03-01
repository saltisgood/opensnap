package com.nickstephen.opensnap.global;

import android.content.Context;

import com.google.gson.Gson;
import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.opensnap.util.http.FriendStoryBook;
import com.nickstephen.opensnap.util.http.ServerResponse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Stephen on 1/03/14.
 */
public class Stories {
    private static final String FILENAME = "stories.bin";

    private static Stories sInstance;

    public static Stories init(Context context) {
        if (sInstance == null) {
            FileInputStream fs;

            try {
                fs = context.openFileInput(GlobalVars.getUsername(context) + "-" + FILENAME);
            } catch (FileNotFoundException e) {
                Twig.printStackTrace(e);
                sInstance = new Stories();
                return sInstance;
            }

            String json;
            try {
                byte[] buff = new byte[4];
                fs.read(buff);

                int len = BitConverter.toInt32(buff, 0);
                buff = new byte[len];

                fs.read(buff);
                json = BitConverter.toString(buff);
                fs.close();
            } catch (Exception e) {
                Twig.printStackTrace(e);
                sInstance = new Stories();
                return sInstance;
            }

            Gson gson = new Gson();
            sInstance = gson.fromJson(json, Stories.class);
        }

        return sInstance;
    }

    public static Stories getInstanceUnsafe() {
        return sInstance;
    }

    private List<FriendStory> mFriendStories;
    private List<MyStory> mMyStories;

    public Stories() {
        mFriendStories = new ArrayList<FriendStory>();
        mMyStories = new ArrayList<MyStory>();
    }

    public Stories sync(ServerResponse response) {
        if (response.my_stories != null) {
            //TODO: Add in when necessary
        }

        if (response.friend_stories != null) {
            for (FriendStoryBook storyBook : response.friend_stories) {
                for (FriendStoryBook.FriendStory friendStory : storyBook.stories) {
                    boolean found = false;

                    for (FriendStory story : mFriendStories) {
                        if (friendStory.story.id.compareTo(story.mID) == 0) {
                            //TODO: Sync any necessary information in the future
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        mFriendStories.add(new FriendStory(friendStory));
                    }
                }
            }
        }

        return this;
    }

    public void write(Context context) {
        FileOutputStream fs;
        try {
            fs = context.openFileOutput(GlobalVars.getUsername(context) + "-" + FILENAME, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            Twig.printStackTrace(e);
            return;
        }

        Gson gson = new Gson();
        String json = gson.toJson(this, Stories.class);

        try {
            fs.write(BitConverter.getBytes(json.length()));
            fs.write(BitConverter.getBytes(json));
            fs.close();
        } catch (IOException e) {
            Twig.printStackTrace(e);
        }
    }

    public static class Story {
        protected String mID;
        protected String mUsername;
        protected String mClientID;
        protected long mTimestamp;
        protected String mMediaID;
        protected String mMediaKey;
        protected String mMediaIV;
        protected String mThumbnailIV;
        protected int mMediaType;
        protected float mTime;
        protected int mTimeLeft;
        protected boolean mZipped;
        protected String mMediaURL;
        protected String mThumbURL;
    }

    public static class FriendStory extends Story {
        protected boolean mViewed;

        public FriendStory(FriendStoryBook.FriendStory story) {
            mViewed = story.viewed;
            mID = story.story.id;
            mUsername = story.story.username;
            mClientID = story.story.client_id;
            mTimestamp = story.story.timestamp;
            mMediaID = story.story.media_id;
            mMediaKey = story.story.media_key;
            mMediaIV = story.story.media_iv;
            mThumbnailIV = story.story.thumbnail_iv;
            mMediaType = story.story.media_type;
            mTime = story.story.time;
            mTimeLeft = story.story.time_left;
            mZipped = story.story.zipped;
            mMediaURL = story.story.media_url;
            mThumbURL = story.story.thumbnail_url;
        }
    }

    public static class MyStory extends Story {
        protected int mViewCount;
        protected int mScreenshotCount;
        protected StoryNotes mNotes;
    }

    public static class StoryNotes {
        protected String mViewer;
        protected boolean mScreenshotted;
        protected long mTimestamp;
        protected String mKey;
        protected String mField;
    }
}
