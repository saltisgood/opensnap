package com.nickstephen.opensnap.gui;

import java.io.File;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.preference.PreferenceManager;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.widget.EditText;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.nickstephen.lib.gui.Fragment;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.util.misc.CameraUtil;

public abstract class SnapEditorBaseFrag extends Fragment implements OnGlobalLayoutListener {
	public static final String FRAG_TAG = "SnapEditor";
	public static final String RESEND_INFO_KEY = "resend_info";
	public static final String CAPTION_KEY = "caption";
	public static final String FILE_PATH_KEY = "file_path";
	public static final String SNAP_TIME_KEY = "snap_time";
	public static final String RESEND_BOOL_KEY = "resend_bool";
	public static final String KEYBOARD_PORTRAIT_HEIGHT_KEY = "keyboard_portrait_height_key";
	public static final String KEYBOARD_LAND_HEIGHT_KEY = "keyboard_land_height_key";
	
	/**
	* A key used for deciding which editor to use. True for pictures, false for videos
	*/
	public static final String MEDIA_TYPE_KEY = "media_type";
	
	protected EditText mCaption;
	private boolean moved = false;
	private InputMethodManager mKeyboardManager;
	private int prevHeight = -1;
	private int mOriginalViewSizeY = -1;
	private int mCaptionHeight;
	private int mCaptionPrevTop;
	private int mKeyboardPortraitHeight;
	private int mKeyboardLandHeight;
	private boolean mIsPortraitMode;
	private boolean mIsKeyboardBeingToggled = false;
	
	public static boolean isResendPicture(Context ctxt) {
		SharedPreferences resendPref = (SharedPreferences) ctxt.getSharedPreferences(RESEND_INFO_KEY, Context.MODE_PRIVATE);
		return resendPref.getBoolean(MEDIA_TYPE_KEY, true);
	}

	@Override
	public abstract View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState);
	
	protected abstract void setResendInfo(String fileName, int time);
	
	@Override
	public void onGlobalLayout() {
		Rect visibleWindow = new Rect();
        View rootView = this.getView();
        if (rootView == null) {
            return;
        }
		rootView.getWindowVisibleDisplayFrame(visibleWindow);
		
		if (prevHeight == -1 || mOriginalViewSizeY == -1) {
			prevHeight = visibleWindow.height();
			mOriginalViewSizeY = visibleWindow.height();
			mCaptionHeight = mCaption.getHeight();
			return;
		}
		
		if (visibleWindow.height() < prevHeight && mOriginalViewSizeY - visibleWindow.height() > 100) {
			prevHeight = visibleWindow.height();
			
			RelativeLayout.LayoutParams params = (LayoutParams) mCaption.getLayoutParams();
			if (!mIsKeyboardBeingToggled) {
				mCaptionPrevTop = params.topMargin;
			}
			
			params.topMargin = visibleWindow.height() - mCaptionHeight;
			if (params.topMargin < 0) {
				params.topMargin = 0;
			}
			mCaption.setLayoutParams(params);
			
			if (mIsPortraitMode) {
				if (mKeyboardPortraitHeight > params.topMargin) {
					mKeyboardPortraitHeight = params.topMargin;
					PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit()
							.putInt(KEYBOARD_PORTRAIT_HEIGHT_KEY, mKeyboardPortraitHeight).apply();
				}
			} else {
				if (mKeyboardLandHeight > params.topMargin) {
					mKeyboardLandHeight = params.topMargin;
					PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit()
							.putInt(KEYBOARD_LAND_HEIGHT_KEY, mKeyboardLandHeight).apply();
				}
			}
		} else if (prevHeight < visibleWindow.height()) {
			prevHeight = visibleWindow.height();
			RelativeLayout.LayoutParams params = (LayoutParams) mCaption.getLayoutParams();
			params.topMargin = mCaptionPrevTop;
			mCaption.setLayoutParams(params);
			if (mCaption.getText().length() <= 0) {
				mCaption.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		int rot = this.getActivity().getWindowManager().getDefaultDisplay().getRotation();
		this.mIsPortraitMode = (rot == Surface.ROTATION_0 || rot == Surface.ROTATION_180);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		mKeyboardPortraitHeight = prefs.getInt(KEYBOARD_PORTRAIT_HEIGHT_KEY, -1);
		mKeyboardLandHeight = prefs.getInt(KEYBOARD_LAND_HEIGHT_KEY, -1);
		
		view.getViewTreeObserver().addOnGlobalLayoutListener(this);
	}
	
	protected String getFilePath() {
		if (isResend()) {
			SharedPreferences resendPref = (SharedPreferences) this.getActivity().getSharedPreferences(RESEND_INFO_KEY, Context.MODE_PRIVATE);
			if (!isResendPicture(this.getActivity())) {
				return new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, CameraUtil.VID_FILE).getAbsolutePath();
			}
			return resendPref.getString(FILE_PATH_KEY, null);
		} else {
			return this.getArguments().getString(FILE_PATH_KEY);
		}
	}
	
	protected String getCaption() {
		if (isResend()) {
			SharedPreferences resendPref = (SharedPreferences) this.getActivity().getSharedPreferences(RESEND_INFO_KEY, Context.MODE_PRIVATE);
			return resendPref.getString(CAPTION_KEY, null);
		} else {
			return null;
		}
	}
	
	protected boolean isResend() {
		return this.getArguments().containsKey(RESEND_BOOL_KEY);
	}
	
	protected final String getUser() {
		Bundle args = this.getArguments();
		if (args != null) {
			return args.getString(BaseContactSelectFrag.SELECTED_CONTACT_KEY);
		}
		return null;
	}
	
	protected abstract void handleTextInput(String input);
	
	protected TextWatcher captionTextWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable arg0) {
			Editable str = mCaption.getText();

			if (str.toString().compareTo("flipside") == 0) {
				handleTextInput("flipside");
			} else if (str.toString().compareTo("50shades") == 0) {
				handleTextInput("50shades");
			} else if (str.toString().compareTo("hispter") == 0) {
				handleTextInput("hispter");
			} else if (str.toString().compareTo("mchammer") == 0) {
				handleTextInput("mchammer");
			} else if (str.toString().compareTo("reset") == 0) {
				handleTextInput("reset");
			} else if (str.toString().compareTo("itsyabirfday") == 0) {
				handleTextInput("itsyabirfday");
			}
			
			mCaption.invalidate();
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			RelativeLayout.LayoutParams params = (LayoutParams) mCaption.getLayoutParams();
			
			if (mIsPortraitMode) {
				if (mKeyboardPortraitHeight != -1 && mKeyboardPortraitHeight < params.topMargin) {
					params.topMargin = mKeyboardPortraitHeight;
					mCaption.setLayoutParams(params);
				}
			} else {
				if (mKeyboardLandHeight != -1 && mKeyboardLandHeight < params.topMargin) {
					params.topMargin = mKeyboardLandHeight;
					mCaption.setLayoutParams(params);
				}
			}
			
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	};
	
	protected OnTouchListener captionOnTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams)mCaption.getLayoutParams();
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					moved = false;
					break;
				case MotionEvent.ACTION_MOVE:
					int y_coord = (int)event.getRawY();
					layout.topMargin = y_coord;
					mCaption.setLayoutParams(layout);
					moved = true;
					break;
                case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					if (!moved) {
						int keyboardTop;
						if (mIsPortraitMode) {
							if (mKeyboardPortraitHeight == -1) {
								keyboardTop = 200;
							} else {
								keyboardTop = mKeyboardPortraitHeight;
							}
						} else {
							if (mKeyboardLandHeight == -1) {
								keyboardTop = 0;
							} else {
								keyboardTop = mKeyboardLandHeight;
							}
						}
						
						mIsKeyboardBeingToggled = true;
						
						RelativeLayout.LayoutParams params = (LayoutParams) mCaption.getLayoutParams();
						mCaptionPrevTop = params.topMargin;
						params.topMargin = keyboardTop;
						mCaption.setLayoutParams(params);
						
						if (mKeyboardManager == null) {
							mKeyboardManager = (InputMethodManager) SnapEditorBaseFrag.this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
						}
                        v.requestFocus();
						mKeyboardManager.showSoftInput(mCaption, 0);
						
						return false;
					}
					else
						break;
				default:
					return false;
			}
			return true;
		}
	};
	
	protected OnTouchListener mediaOnTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				if (mCaption.getVisibility() != View.VISIBLE) {
					RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams)mCaption.getLayoutParams();
					int y_coord = (int)event.getRawY();
					layout.topMargin = y_coord;
					mCaption.setLayoutParams(layout);
					mCaption.setVisibility(View.VISIBLE);
					mCaption.requestFocus();
					return true;
				}
			}
			
			return false;
		}
	};

	protected OnClickListener cancelOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SnapEditorBaseFrag.this.getActivity());
            builder.setTitle("Delete Snap?").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SnapEditorBaseFrag.this.getActivity().setResult(Activity.RESULT_CANCELED);
                    SnapEditorBaseFrag.this.getActivity().finish();
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            }).show();
		}
	};

	protected OnEditorActionListener captionOnEditorActionListener = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			switch (actionId) {
				case EditorInfo.IME_ACTION_DONE:
					if (StatMethods.IsStringNullOrEmpty(v.getText().toString())) {
						v.setVisibility(View.INVISIBLE);
					}
					break;
			}
			return false;
		}
	};
}
