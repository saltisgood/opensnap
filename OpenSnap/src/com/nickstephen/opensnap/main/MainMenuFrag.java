package com.nickstephen.opensnap.main;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nickstephen.lib.gui.Fragment;
import com.nickstephen.lib.gui.widget.AnimTextView;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.gui.DragTouchListener;
import com.nickstephen.opensnap.settings.SettingsAccessor;

/**
 * An extension to {@link Fragment} that just has the main menu text and buttons and whatnot
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class MainMenuFrag extends Fragment {
	public static final String FRAG_TAG = "MainMenuFrag";
	
	private int easterEggCount = 0;
	private TextView unseenText;
	private AnimTextView mGreetingText;
	
	public MainMenuFrag() {}
	
	@SuppressWarnings("deprecation")
	@Override
    @SuppressLint("NewApi")
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		RelativeLayout v;
		switch (SettingsAccessor.getThemePref(this.getActivity())) {
			case ori:
			case snapchat:
				v = (RelativeLayout)inflater.inflate(R.layout.main_menu_selector_v2, null);
				break;
			case black:
				v = (RelativeLayout)inflater.inflate(R.layout.main_menu_selector_default_v2, null);
				ImageView img = new ImageView(this.getActivity());
				img.setImageResource(R.drawable.kanye);
				v.addView(img);
				img.setOnTouchListener(new DragTouchListener());
				BitmapDrawable background = (BitmapDrawable)this.getResources().getDrawable(R.drawable.main_menu_default_background);
				background.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
				if (Build.VERSION.SDK_INT < 16) {
					v.setBackgroundDrawable(background);
				} else {
					v.setBackground(background);
				}
				break;
			case def:
			default:
				v = (RelativeLayout)inflater.inflate(R.layout.main_menu_selector_default_v2, null);
				background = (BitmapDrawable)this.getResources().getDrawable(R.drawable.main_menu_default_background);
				background.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
				if (Build.VERSION.SDK_INT < 16) {
					v.setBackgroundDrawable(background);
				} else {
					v.setBackground(background);
				}
				break;
		}
		
		Integer noUnseen;
		if ((noUnseen = LocalSnaps.getUnseenSnaps()) > 0) { 
			unseenText = (TextView)v.findViewById(R.id.new_snaps_notice);
			unseenText.setText(noUnseen.toString());
			unseenText.setVisibility(View.VISIBLE);
		}
		
		mGreetingText = (AnimTextView) v.findViewById(R.id.creator_text);
		mGreetingText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {	
				if (++easterEggCount == 3) {
					StatMethods.hotBread(MainMenuFrag.this.getActivity(), "keep going...", Toast.LENGTH_SHORT);
				} else if (easterEggCount == 7) {
					StatMethods.hotBread(MainMenuFrag.this.getActivity(), "almost there...", Toast.LENGTH_SHORT);
				} else if (easterEggCount == 10) {
					StatMethods.hotBread(MainMenuFrag.this.getActivity(), ":P", Toast.LENGTH_SHORT);
					Intent webTent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.nickstephen.com/"));
					MainMenuFrag.this.getActivity().startActivity(webTent);
				}
			}
		});
		
		return v;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mGreetingText != null) {
			mGreetingText.pauseAnimation();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (mGreetingText != null && !mGreetingText.isAnimationRunning()) {
			mGreetingText.startAnimation(true);
		}
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		view.invalidate();
	}
	
	public void setUpdateText(Integer updates) {
        if (unseenText == null) {
            View rootView = this.getView();
            if (rootView != null) {
                unseenText = (TextView) rootView.findViewById(R.id.new_snaps_notice);
            }
        }

		if (unseenText != null) {
			if (updates > 0) {
				unseenText.setText(updates.toString());
				if (unseenText.getVisibility() != View.VISIBLE) {
					unseenText.setVisibility(View.VISIBLE);
				}
			} else {
				if (unseenText.getVisibility() == View.VISIBLE) {
					unseenText.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
}
