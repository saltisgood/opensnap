package com.nickstephen.opensnap.preview;

import static com.nickstephen.opensnap.preview.PreviewConstants.CAPTION_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.CAPTION_LOC_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.CAPTION_ORI_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.PATH_KEY;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.TextView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.VideoView;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.LocalSnaps.CaptionOrientation;
import com.nickstephen.opensnap.settings.SettingsAccessor;

/**
 * The VideoPreview activity that is used for... wait for it:
 * previewing videos!
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class VideoPreview extends Activity {	
	public static final int AUTO_HIDE_DELAY_MS = 2000;
	
	private MediaController mMediaController;
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Build.VERSION.SDK_INT >= 11) {
			this.requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		}
		
		Intent result = new Intent();
		result.putExtra(PreviewConstants.PATH_KEY, getPath());
		this.setResult(RESULT_CANCELED, getIntent());
		
		setContentView(R.layout.activity_video_preview);
		setupActionBar();
		
		TextView txt = (TextView)this.findViewById(R.id.textView1);
		if (getCaption() != null) {
			txt.setText(getCaption());
			float location = getCaptionLocation();
			if (Build.VERSION.SDK_INT >= 11) {
				switch (getCaptionOrientation()) {
					case LANDSCAPE_LEFT:
						txt.setRotation(-90);
						break;
					case LANDSCAPE_RIGHT:
						txt.setRotation(90);
						break;
					default:
						break;
				}
			}
			RelativeLayout.LayoutParams params = (LayoutParams) txt.getLayoutParams();
			if (location <= 0) {
				params.topMargin = 10;
			} else if (location >= 0.9) {
				params.bottomMargin = 10;
			} else {
				Display display = this.getWindowManager().getDefaultDisplay();
				int height;
				if (Build.VERSION.SDK_INT < 13) {
					height = display.getHeight();
				} else {
					Point point = new Point();
					display.getSize(point);
					height = point.y;
				}
				height = Float.valueOf((float) height * location).intValue();
				params.topMargin = height;
			}
			txt.setLayoutParams(params);
			txt.setVisibility(View.VISIBLE);
		}
		
		VideoView video = (VideoView)findViewById(R.id.fullscreen_video);
		if (SettingsAccessor.getSnapTiming(this)) {
			video.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer player) {
					Intent result = new Intent();
					result.putExtra(PreviewConstants.PATH_KEY, getPath());
					VideoPreview.this.setResult(RESULT_OK, result);
					VideoPreview.this.finish();
				}
			});
		} else {
			video.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer player) {
					player.start();
				}
			});
			
			mMediaController = new MediaController(this);
			mMediaController.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					delayedHide(1500);
					return false;
				}
			});
			video.setMediaController(mMediaController);
		}
		
		video.setVideoPath(getPath());
		video.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				showComponents();
				return false;
			}
		});
		video.start();
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		delayedHide(1500);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.video_preview, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			Intent result = new Intent();
			result.putExtra(PreviewConstants.PATH_KEY, getPath());
			this.setResult(RESULT_OK, result);
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @SuppressLint("NewApi")
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} else {
			this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

    @SuppressLint("NewApi")
	private void hideComponents() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.getActionBar().hide();
		} else {
			this.getSupportActionBar().hide();
		}
		
		if (mMediaController != null) {
			mMediaController.hide();
		}
	}

    @SuppressLint("NewApi")
	private void showComponents() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.getActionBar().show();
		} else {
			this.getSupportActionBar().show();
		}
		
		delayedHide(2000);
	}
	
	private void delayedHide(int millis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, millis);
	}
	
	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hideComponents();
		}
	};
	
	private String getCaption() {
		return this.getIntent().getStringExtra(CAPTION_KEY);
	}
	
	private CaptionOrientation getCaptionOrientation() {
		return CaptionOrientation.getOrientation(this.getIntent().getIntExtra(CAPTION_ORI_KEY, 0));
	}
	
	private float getCaptionLocation() {
		return this.getIntent().getFloatExtra(CAPTION_LOC_KEY, 0);
	}
	
	private String getPath() {
		return this.getIntent().getStringExtra(PATH_KEY);
	}
}
