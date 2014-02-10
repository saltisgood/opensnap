package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.main.MainFrag;

/**
 * Created by Nick Stephen on 1/02/14.
 */
public class TutorialIntroFrag extends TutorialRootFrag {
    public static final String FRAG_TAG = "com.nickstephen.opensnap.main.tuts.TutorialIntroFrag";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tutorial_main, null);

        View button = rootView.findViewById(R.id.no_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(true);
            }
        });

        button = rootView.findViewById(R.id.yes_button);
        button.setOnClickListener(beginTutorial);

        return rootView;
    }

    private final View.OnClickListener beginTutorial = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish(false);
            TutorialIntroFrag.this.getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.launch_container, new TutorialMainFrag(), TutorialMainFrag.FRAG_TAG)
                    .addToBackStack(TutorialRootFrag.FRAG_TAG).commit();
        }
    };
}
