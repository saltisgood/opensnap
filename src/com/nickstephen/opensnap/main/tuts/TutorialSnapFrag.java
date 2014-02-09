package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.lib.play.IabHelper;
import com.nickstephen.lib.play.IabResult;
import com.nickstephen.lib.play.Purchase;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.main.LaunchActivity;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.play.SKU;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.Toast;

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

        button = rootView.findViewById(R.id.buy_button);
        button.setOnClickListener(mBuyClickL);

        return rootView;

    }

    private final View.OnClickListener mNextClickL = new View.OnClickListener() {
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

    private final View.OnClickListener mBuyClickL = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ((LaunchActivity) TutorialSnapFrag.this.getActivity()).performPurchase(mPurchaseL);
        }
    };

    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseL = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isFailure()) {
                Twig.debug(FRAG_TAG, "Error purchasing: " + result);
                StatMethods.hotBread(TutorialSnapFrag.this.getActivity(), "Press the options button to purchase at any time", Toast.LENGTH_LONG);
            } else if (info.getSku().equals(SKU.PREMIUM_FEATURES)) {
                SettingsAccessor.setPremium(TutorialSnapFrag.this.getActivity(), true);
                StatMethods.hotBread(TutorialSnapFrag.this.getActivity(), "Purchase successful. Premium features enabled! Thank-you!", Toast.LENGTH_LONG);
            }
        }
    };
}
