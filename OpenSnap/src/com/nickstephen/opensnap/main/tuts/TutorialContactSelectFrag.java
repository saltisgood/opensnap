package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;

import org.holoeverywhere.LayoutInflater;

/**
 * Created by Nick Stephen on 9/02/14.
 */
public class TutorialContactSelectFrag extends TutorialRootFrag {
    public static final String FRAG_TAG = "com.nickstephen.opensnap.main.tuts.TutorialContactSelectFrag";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tut_contactselect);

        View button = rootView.findViewById(R.id.no_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(false);
            }
        });

        return rootView;
    }
}
