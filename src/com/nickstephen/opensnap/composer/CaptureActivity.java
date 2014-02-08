package com.nickstephen.opensnap.composer;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.FrameLayout;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.nickstephen.lib.SystemUiHider;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class CaptureActivity extends Activity{
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
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES;
	
	public static final int REQUEST_CAMERA_CAPTURE = 717;
	public static final int RESULT_SNAP_SENT = 897;
	
	public static final String REQUEST_CAMERA = "cam_whore";

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

    private int mVidQualityFragResult;
    private boolean mVidQualityChanged = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_camera_capture_v2);
		setupActionBar();

		FrameLayout fragFrame = (FrameLayout)this.findViewById(R.id.fragment_container);
		fragFrame.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
				return true;
			}
		});
		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, fragFrame, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							if (visible) {
								getActionBar().show();
							} else {
								getActionBar().hide();
							}
						} else {
							if (visible) {
								getSupportActionBar().show();
							} else {
								getSupportActionBar().hide();
							}
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		if (savedInstanceState == null) {
			Fragment frag = new CaptureFrag();
			Bundle args = new Bundle();
			args.putString(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUser());
			frag.setArguments(args);
			this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, frag, CaptureFrag.FRAG_TAG).commit();
		}
	}
	
	private String getUser() {
		return this.getIntent().getStringExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY);
	}

    @Override
    protected void onStart() {
        super.onStart();

        mSystemUiHider.hide();
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		CaptureFrag cappy = (CaptureFrag) this.getSupportFragmentManager().findFragmentByTag(CaptureFrag.FRAG_TAG);
		if (cappy != null && cappy.isResumed()) {
			if (cappy.onKeyDown(keyCode))
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		CaptureFrag cappy = (CaptureFrag) this.getSupportFragmentManager().findFragmentByTag(CaptureFrag.FRAG_TAG);
		if (cappy != null && cappy.isResumed()) {
			if (cappy.onKeyUp(keyCode))
				return true; 
		}
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
			this.setResult(RESULT_CANCELED);
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    public boolean hasVidQualityChanged() {
        return mVidQualityChanged;
    }

    public int getVidQualityResult() {
        mVidQualityChanged = false;
        return mVidQualityFragResult;
    }

    public void setVidQualityResult(int result) {
        mVidQualityChanged = true;
        mVidQualityFragResult = result;
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
}
