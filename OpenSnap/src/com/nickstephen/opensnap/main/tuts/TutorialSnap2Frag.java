package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.main.LaunchActivity;
import com.nickstephen.opensnap.main.MainFrag;

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
                finish(true);
            }
        });

        button = rootView.findViewById(R.id.yes_button);
        button.setOnClickListener(mNextClickL);

        return rootView;
    }

    private final View.OnClickListener mNextClickL = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish(false);
            MainFrag frag = (MainFrag) TutorialSnap2Frag.this.getFragmentManager()
                    .findFragmentByTag(MainFrag.FRAGTAG);
            if (frag != null) {
                frag.goToContacts();
            }

            TutorialSnap2Frag.this.getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.launch_container, new TutorialContactFrag(), TutorialContactFrag.FRAG_TAG)
                    .addToBackStack(TutorialRootFrag.FRAG_TAG)
                    .commit();
        }
    };
}
