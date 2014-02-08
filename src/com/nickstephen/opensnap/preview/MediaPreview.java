package com.nickstephen.opensnap.preview;

import static com.nickstephen.opensnap.preview.PreviewConstants.PATH_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.TIME_KEY;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.nickstephen.lib.SystemUiHider;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.util.misc.DeleteSnap;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MediaPreview extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	//private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= 11) {
			this.requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		}

		setContentView(R.layout.activity_media_preview);
		setupActionBar();

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
		.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(
								android.R.integer.config_shortAnimTime);
					}
					controlsView
					.animate()
					.translationY(visible ? 0 : mControlsHeight)
					.setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE
							: View.GONE);
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					if (visible) {
						MediaPreview.this.getActionBar().show();
					} else {
						MediaPreview.this.getActionBar().hide();
					}
				} else {
					if (visible) {
						MediaPreview.this.getSupportActionBar().show();
					} else {
						MediaPreview.this.getSupportActionBar().hide();
					}
				}

				if (visible && AUTO_HIDE) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.delete_button).setOnTouchListener(
				mDelayHideTouchListener);
		findViewById(R.id.delete_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				StatMethods.QuestionBox(MediaPreview.this, "Confirm", "Are you sure you wish to delete this snap?\nThis action can't be undone", 
						snapDeleteListener, null);
			}
		});

		ImageView img = (ImageView)contentView;
		//Bitmap bit = BitmapFactory.decodeFile(imgPath);
		Bitmap bit = BitmapFactory.decodeFile(getImgPath());
		if (bit.getWidth() > bit.getHeight()) {
			Matrix rotMatrix = new Matrix();
			rotMatrix.postRotate(90);
			Bitmap tempBit = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(), bit.getHeight(), rotMatrix, true);
			if (tempBit != bit) {
				bit.recycle();
				bit = null;
				bit = tempBit;
			}
		}
		img.setImageBitmap(bit);

		final TextView time = (TextView)this.findViewById(R.id.textView1);
		int distime = getDisplayTime();
		if (distime != -1) {
			time.setVisibility(View.VISIBLE);
			time.setText(Integer.valueOf(distime).toString());
			new CountDownTimer(1000 * distime, 1000) {
				public void onTick(long millisUntilFinished) {
					time.setText(Integer.valueOf((int)millisUntilFinished / 1000).toString());
				} 

				public void onFinish() {
					Intent result = new Intent();
					result.putExtra(PATH_KEY, getImgPath());
					MediaPreview.this.setResult(RESULT_OK, result);
					MediaPreview.this.finish();
				}
			}.start();
		}
		
		Intent resultIntent = new Intent();
		resultIntent.putExtra(PATH_KEY, getImgPath());
		this.setResult(RESULT_OK, resultIntent);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		mSystemUiHider.hide();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
    @SuppressLint("NewApi")
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} else {
			this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	DialogInterface.OnClickListener snapDeleteListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (DeleteSnap.deleteSnapAndRescanMedia(MediaPreview.this, getImgPath())) {
				StatMethods.hotBread(MediaPreview.this, "Delete successful", Toast.LENGTH_SHORT);
				MediaPreview.this.finish();
			} else {
				StatMethods.hotBread(MediaPreview.this, "Error during delete!", Toast.LENGTH_SHORT);
			}
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	private int getDisplayTime() {
		return this.getIntent().getExtras().getInt(TIME_KEY, -1);
	}

	private String getImgPath() {
		return this.getIntent().getExtras().getString(PATH_KEY);
	}

}
