package com.nickstephen.opensnap.util.tasks;

import android.content.Context;
import android.os.Bundle;

import com.nickstephen.opensnap.global.Stories;
import com.nickstephen.opensnap.util.http.ServerResponse;

/**
 * Created by Nick Stephen on 1/03/14.
 */
public class GetStoriesTask extends BaseRequestTask {
    private static final String NAME = "GetStoriesTask";

    private final String mUsername;

    public GetStoriesTask(Context context, String username) {
        super(context);

        mUsername = username;
    }

    @Override
    protected Bundle getParams() {
        Bundle bundle = new Bundle();

        bundle.putString("username", mUsername);

        return bundle;
    }

    @Override
    protected String getPath() {
        return "/bq/stories";
    }

    @Override
    protected String getTaskName() {
        return NAME;
    }

    @Override
    protected void onSuccessAsync(ServerResponse response) {
        Stories.getInstance().sync(response).write(mContext);
    }
}
