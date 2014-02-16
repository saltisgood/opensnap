package com.nickstephen.opensnap.main.tuts;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.settings.SettingsAccessor;

import org.holoeverywhere.app.Fragment;

/**
 * Created by Nick Stephen on 1/02/14.
 */
public abstract class TutorialRootFrag extends Fragment {
    public static final String FRAG_TAG = "com.nickstephen.opensnap.main.tuts.TutorialRootFrag";

    private AlphaAnimation mAnim;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setOnTouchListener(mTouchListener);
    }

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (mAnim != null && !mAnim.hasEnded()) {
                        mAnim.cancel();
                    }

                    AlphaAnimation anim = new AlphaAnimation(1.0f, 0.2f);
                    anim.setDuration(250);
                    anim.setFillAfter(true);
                    view.clearAnimation();
                    view.startAnimation(anim);
                    mAnim = anim;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (mAnim != null && !mAnim.hasEnded()) {
                        mAnim.cancel();
                    }

                    anim = new AlphaAnimation(0.2f, 1.0f);
                    anim.setDuration(250);
                    anim.setFillAfter(true);
                    view.clearAnimation();
                    view.startAnimation(anim);
                    mAnim = anim;
                    break;
            }

            return true;
        }
    };

    protected void finish(boolean isFinish) {
        this.getFragmentManager().popBackStack();
        if (isFinish) {
            SettingsAccessor.setFirstTimeStart(this.getActivity(), false);
            StatMethods.hotBread(TutorialRootFrag.this.getActivity(), "Press the options button and \"View Tutorial\" to view again at any time!", Toast.LENGTH_SHORT);
        }
    }
}
