package com.nickstephen.opensnap.composer.editor;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.FrameLayout;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.nickstephen.lib.SystemUiHider;
import com.nickstephen.lib.gui.Fragment;
import com.nickstephen.lib.gui.ListFragment;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.main.tuts.TutorialRootFrag;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class EditorActivity extends Activity {
	public static final String RETURN_TO_CAMERA_KEY = "return_to_cam";
	
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

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

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
			Fragment frag;
			Bundle args = new Bundle();
			args.putString(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUser());
			
			if (this.getIntent().getBooleanExtra(SnapEditorBaseFrag.RESEND_BOOL_KEY, false)) {
				args.putBoolean(SnapEditorBaseFrag.RESEND_BOOL_KEY, true);
				if (this.getSharedPreferences(SnapEditorBaseFrag.RESEND_INFO_KEY, MODE_PRIVATE).getBoolean(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true)) {
					frag = new EditorPicFrag();
				} else {
					frag = new EditorVidFrag();
				}
			} else {
				args.putString(SnapEditorBaseFrag.FILE_PATH_KEY, this.getIntent().getStringExtra(SnapEditorBaseFrag.FILE_PATH_KEY));
				
				if (this.getIntent().getBooleanExtra(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true)) {
					frag = new EditorPicFrag();
					args.putBoolean(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true);
				} else {
					frag = new EditorVidFrag();
					args.putBoolean(SnapEditorBaseFrag.MEDIA_TYPE_KEY, false);
				}
			}
			
			frag.setArguments(args);
			this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, frag, SnapEditorBaseFrag.FRAG_TAG).commit();
		}
	}
	
	private String getUser() {
		return this.getIntent().getStringExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
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
				ListFragment contacts = (ListFragment)this.getSupportFragmentManager().findFragmentByTag(ContactSelectFrag.FRAG_TAG);
				if (contacts != null && contacts.isFocused()) {
					contacts.popFragment();
					return true;
				}
				
				if (this.getIntent() != null && this.getIntent().getBooleanExtra(RETURN_TO_CAMERA_KEY, false)) {
					this.setResult(Activity.RESULT_CANCELED);
					this.finish();
				} else {
					this.setResult(Activity.RESULT_CANCELED);
					this.finish();
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
        if (this.getSupportFragmentManager().popBackStackImmediate(TutorialRootFrag.FRAG_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)) {
            return;
        }

		ContactSelectFrag contacts = (ContactSelectFrag) this.getSupportFragmentManager().findFragmentByTag(ContactSelectFrag.FRAG_TAG);
		if (contacts != null) {
			if (contacts.isFocused()) {
				contacts.popFragment();
			} else {
				super.onBackPressed();
			}
		} else {
			super.onBackPressed();
		}
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
