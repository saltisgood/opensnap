package com.nickstephen.opensnap.util.http;

import java.util.List;

import android.os.Bundle;

import com.google.gson.Gson;

public class ServerResponse {
	public static final String KEY_LOGGED_USER = "logged";
	public static final String RESULT_DATA_KEY = "resultData";
	public static final String STATUS_CODE_KEY = "statusCode";

	private ServerResponse() {	}

	public static Bundle getLoggedOut() {
		Bundle bundle = new Bundle();
		bundle.putBoolean(KEY_LOGGED_USER, false);
		bundle.putString(RESULT_DATA_KEY, "logged out");
		return bundle;
	}

	public static Bundle get401Code() {
		Bundle bundle = new Bundle();
		bundle.putString(RESULT_DATA_KEY, "un-authorised access");
		bundle.putInt(STATUS_CODE_KEY, 401);
		return bundle;
	}
	
	public static ServerResponse getResponseFromString(String result) {
		Gson gson = new Gson();
        return gson.fromJson(result, ServerResponse.class);
	}

	public List<ServerFriend> added_friends;
	public long added_friends_timestamp;
	public String auth_token;
	public List<String> bests;
	public List<String> broken_cameras;
	public boolean can_view_mature_content;
    public String country_code;
    public long current_timestamp;
    public String device_token;
	public String email;
	public List<FriendStoryBook> friend_stories;
	public List<ServerFriend> friends;
    public String image_caption;
    public boolean is_beta;
	//public PostStoryTask.PostStoryJsonResponse json;
	public long last_updated;
	public boolean logged;
	//public MatureContentDictionary mature_content_dictionary;
	public String message;
	public String mobile;
	public String mobile_verification_key;
	public List<StoryLogbook> my_stories;
    public String notification_sound_setting;
    public int number_of_best_stories;
	public ServerFriend object;
    public String param;
	public int received;
	public List<String> recents;
    public List<ServerFriend> requests;
	public List<ServerFriend> results;
    public int score;
	public int sent;
    public boolean should_send_text_to_verify_number;
	public int snap_p;
	public SnapOrStoryDoublePostResponse snap_response;
	public String snapchat_phone_number;
	public List<ServerSnap> snaps;
    public int status;
	public String story_privacy;
	public SnapOrStoryDoublePostResponse story_response;
    public String token;
	public String username;

	public String toString()
	{
		return "ServerResponse [logged=" + this.logged + ", username=" + this.username + ", auth_token=" + this.auth_token + 
				//", snaps=" + this.snaps + ", friends=" + this.friends + ", added_friends=" + this.added_friends + 
				", bests=" + this.bests + ", recents=" + this.recents + ", sent=" + this.sent + ", received=" + this.received + 
				", email=" + this.email + ", mobile=" + this.mobile + ", mobile_verification_key=" + this.mobile_verification_key + 
				", snap_p=" + this.snap_p + ", added_friends_timestamp=" + this.added_friends_timestamp + ", last_updated=" 
				+ this.last_updated + ", message=" + this.message + 
				//", results=" + this.results + ", object=" + this.object + ", my_stories=" + this.my_stories + ", friend_stories=" + this.friend_stories + 
				", snapchat_phone_number=" + this.snapchat_phone_number + "]";
	}
}
