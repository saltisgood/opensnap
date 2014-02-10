package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
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
 * Created by Nick Stephen on 9/02/14.
 */
public class TutorialContactFrag extends TutorialRootFrag {
    public static final String FRAG_TAG = "com.nickstephen.opensnap.main.tuts.TutorialContactFrag";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tut_contacts);

        View button = rootView.findViewById(R.id.no_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(true);
            }
        });

        button = rootView.findViewById(R.id.buy_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((LaunchActivity) TutorialContactFrag.this.getActivity()).performPurchase(mPurchaseL);
            }
        });

        return rootView;
    }

    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseL = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isFailure()) {
                Twig.debug(FRAG_TAG, "Error purchasing: " + result);
                StatMethods.hotBread(TutorialContactFrag.this.getActivity(), "Press the options button to purchase at any time", Toast.LENGTH_LONG);
            } else if (info.getSku().equals(SKU.PREMIUM_FEATURES)) {
                SettingsAccessor.setPremium(TutorialContactFrag.this.getActivity(), true);
                StatMethods.hotBread(TutorialContactFrag.this.getActivity(), "Purchase successful. Premium features enabled! Thank-you!", Toast.LENGTH_LONG);
            }
        }
    };
}
