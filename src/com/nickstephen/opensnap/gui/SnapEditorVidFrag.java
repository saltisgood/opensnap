package com.nickstephen.opensnap.gui;

import java.io.File;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.EditText;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.nickstephen.lib.misc.FileIO;
import com.nickstephen.opensnap.R;

public abstract class SnapEditorVidFrag extends SnapEditorBaseFrag {
	protected static final String EDIT_FILENAME = "edit.mp4";
	
	private MediaPlayer mMediaPlayer;

	public SnapEditorVidFrag() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
		setFocused(true);
		
		final View v = inflater.inflate(R.layout.snap_edit_vid, null);
		
		VideoView vv = (VideoView)v.findViewById(R.id.snap_vid);
		vv.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer player) {
				mMediaPlayer = player;
				player.setLooping(true);
				player.start();
			}
		});
		vv.setVideoPath(getFilePath());
		vv.setOnTouchListener(mediaOnTouchListener);
		
		mCaption = (EditText)v.findViewById(R.id.snap_user_text);
		mCaption.addTextChangedListener(captionTextWatcher);
		mCaption.setOnTouchListener(captionOnTouchListener);
		
		View cancel = (View)v.findViewById(R.id.back_button);
		cancel.setOnClickListener(cancelOnClickListener);
		
		View send = (View)v.findViewById(R.id.send_button);
		send.setOnClickListener(sendOnClickListener);
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		try {
			if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
				if (!mMediaPlayer.isLooping()) {
					mMediaPlayer.setLooping(true);
				}
				mMediaPlayer.start();
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
			try {
				mMediaPlayer.pause();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void setResendInfo(String fileName, int time) {
		SharedPreferences resendPref = this.getActivity().getSharedPreferences(RESEND_INFO_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = resendPref.edit();
		EditText txt = (EditText)this.getView().findViewById(R.id.snap_user_text);
		String text = txt.getText().toString();
		if (text == null)
			text = "";
		editor.putString(CAPTION_KEY, text);
		if (fileName != null) editor.putString(FILE_PATH_KEY, fileName);
		editor.putBoolean(MEDIA_TYPE_KEY, false);
		editor.commit();
	}

	@Override
	protected void handleTextInput(String input) {
		// do nothing
	}
	
	private int getOrientation() {
		int rotation = this.getActivity().getWindowManager().getDefaultDisplay().getRotation();
		
		switch (rotation) {
			case Surface.ROTATION_90:
				return 1;
			case Surface.ROTATION_270:
				return 2;
			case Surface.ROTATION_0:
			case Surface.ROTATION_180:
			default:
				return 0;
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private float getCaptionLocation() {
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		int height;
		if (Build.VERSION.SDK_INT < 13) {
			height = display.getHeight();
		} else {
			Point point = new Point();
			display.getSize(point);
			height = point.y;
		}
		
		return (float) mCaption.getTop() / (float) height;
	}
	
	protected abstract void onSendCallback(Bundle args);
	
	private final OnClickListener sendOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			File outFile = new File(SnapEditorVidFrag.this.getActivity().getCacheDir(), EDIT_FILENAME);
			FileIO.bufferedCopy(SnapEditorVidFrag.this.getFilePath(), outFile.getAbsolutePath());
			
			Bundle args = new Bundle();
			args.putString(BaseContactSelectFrag.FILE_PATH_KEY, outFile.getAbsolutePath());
			args.putBoolean(BaseContactSelectFrag.MEDIA_TYPE_KEY, false);
			args.putInt(BaseContactSelectFrag.ORIENTATION_KEY, getOrientation());
			args.putString(BaseContactSelectFrag.CAPTION_KEY, mCaption.getText().toString());
			args.putFloat(BaseContactSelectFrag.CAP_LOCATION_KEY, getCaptionLocation());
			
			onSendCallback(args);
			
			if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
				try {
					mMediaPlayer.pause();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
		}
	};
}
