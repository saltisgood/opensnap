package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;

import org.holoeverywhere.LayoutInflater;

/**
 * Created by Nick Stephen on 1/02/14.
 */
public class TutorialSnap2Frag extends TutorialRootFrag {
    public final static String FRAG_TAG = "com.nickstephen.opensnap.main.tuts.TutorialSnap2Frag";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tutorial_snap2);

        View button = rootView.findViewById(R.id.no_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        button = rootView.findViewById(R.id.yes_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        return rootView;
    }
}
