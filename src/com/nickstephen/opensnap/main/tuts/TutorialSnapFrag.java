package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.main.MainFrag;

import org.holoeverywhere.LayoutInflater;

/**
 * Created by Nick Stephen on 1/02/14.
 */
public class TutorialSnapFrag extends TutorialRootFrag {
    public final static String FRAG_TAG = "com.nickstephen.opensnap.main.tuts.TutorialSnapFrag";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tutorial_snaps);

        View button = rootView.findViewById(R.id.no_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        button = rootView.findViewById(R.id.yes_button);
        button.setOnClickListener(mNextClickL);

        return rootView;

    }

    private View.OnClickListener mNextClickL = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();

            TutorialSnapFrag.this.getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.launch_container, new TutorialSnap2Frag(), TutorialSnap2Frag.FRAG_TAG)
                    .addToBackStack(TutorialSnap2Frag.FRAG_TAG)
                    .commit();
        }
    };
}
