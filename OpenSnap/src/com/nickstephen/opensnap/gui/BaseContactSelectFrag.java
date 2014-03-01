package com.nickstephen.opensnap.gui;

import static com.nickstephen.opensnap.gui.SnapEditorBaseFrag.RESEND_INFO_KEY;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.preference.SharedPreferences.Editor;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.CheckBox;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nickstephen.lib.misc.BitmapUtil;
import com.nickstephen.lib.misc.FileIO;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps.MediaType;
import com.nickstephen.opensnap.global.TempSnaps;
import com.nickstephen.opensnap.global.TempSnaps.TempSnap;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.misc.CameraUtil;
import com.nickstephen.opensnap.util.tasks.SnapUpload;

/**
 * The ListFragment that is used for selecting the recipients of a Snap
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class BaseContactSelectFrag extends ListFragment {
	public static final String FILE_PATH_KEY = "wubwubwubwubwub";
	public static final String TIME_KEY = "powpowpowpow";
	public static final String ORIENTATION_KEY = "orica";
	public static final String MEDIA_TYPE_KEY = "insertfunninesshere";
	public static final String CAPTION_KEY = "cappy";
	public static final String CAP_LOCATION_KEY = "cappysquared";
	public static final String FRAG_TAG = "ContactSelectEditor";
	public static final String SELECTED_CONTACT_KEY = "SelectedContact";
	
	private static final String RESEND_FILE_PATH = "resend";
	private static final int SWITCH_BUTTON_TEXT = 0x20000;
	
	/**
	 * The path of the file that contains the Snap
	 */
	protected String filePath;
	/**
	 * The length of time to display the Snap
	 */
	protected int snapTime; 
	/**
	 * If the user selects a single recipient, the contact position is stored here
	 */
	protected int selectedPosition;
	/**
	 * Stores the number of selected recipients
	 */
	protected int numSelected = 0;
	/**
	 * Stores the list of usernames of the recipients
	 */
	protected List<String> selectedUsers = new ArrayList<String>();
	protected Theme theme;
	protected SoftReference<BitmapDrawable> drawable;
	protected Button mSendButton;
	protected GuiHandler mGuiHandler = new GuiHandler();
	
	@SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		View v = inflater.inflate(R.layout.contact_select_preview_list, null);
		
		switch (theme = SettingsAccessor.getThemePref(this.getActivity())) {
			case ori:
			case snapchat:
				break;
			case black:
			case def:
			default:
				BitmapDrawable draw;
				if (drawable == null || (draw = drawable.get()) == null) {
					draw = (BitmapDrawable) this.getActivity().getResources().getDrawable(R.drawable.main_menu_default_background);
					draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
					drawable = new SoftReference<BitmapDrawable>(draw);
				}
				TextView title = (TextView)v.findViewById(R.id.textView1);
				title.setTextColor(0xFFFFFFFF);
				ListView list = (ListView)v.findViewById(android.R.id.list);
				LinearLayout lay = (LinearLayout)v.findViewById(R.id.button_container);
				RelativeLayout laylay = (RelativeLayout)v.findViewById(R.id.content_container);
				if (Build.VERSION.SDK_INT < 16) {
					list.setBackgroundDrawable(draw);
					v.setBackgroundDrawable(draw);
					lay.setBackgroundDrawable(draw);
					laylay.setBackgroundDrawable(draw);
				} else {
					list.setBackground(draw);
					v.setBackground(draw);
					lay.setBackground(draw);
					laylay.setBackground(draw);
				}
				list.setCacheColorHint(0x0);
				break;
		}
		
		mSendButton = (Button)v.findViewById(R.id.send_button);
		mSendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMultiple();
			}
		});
		mSendButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				sendMultipleDelay();
				return true;
			}
		});
		
		this.setListAdapter(new ContactViewListAdapter(this.getActivity()));
		
		filePath = this.getArguments().getString(FILE_PATH_KEY);
		snapTime = this.getArguments().getInt(TIME_KEY, 5);
		this.setRetainInstance(true);
		
		new LazyImageLoader(filePath, (ImageView)v.findViewById(R.id.contact_drawer_img), 
				(ProgressBar)v.findViewById(R.id.contact_drawer_pb)).execute(new Void[] {});
		
		return v;
	}
	
	/**
	 * This method is called when the user clicks on an item in the list.
	 * It sends the snap to a single person after a confirmation box.
	 * @param position The position of the contact in the list
	 */
	protected void contactSelect(int position) {
		selectedPosition = position;
		StatMethods.QuestionBox(this.getActivity(), "Please confirm", "Send snap to " + Contacts.getInstanceUnsafe().getDisplayOrUserName(selectedPosition) + "?",
				mesgBoxListener, mesgBoxListener);
	}
	
	/**
	 * Updates the send button at the bottom of the screen.
	 * Called whenever the user toggles a check box in the contact list
	 */
	private void updateButton() {
		if (numSelected > 0) {
			mSendButton.setEnabled(true);
			String buttText = "Send to: ";
			for (int i = 0; i < selectedUsers.size() - 1; i++) {
				buttText += selectedUsers.get(i) + ", ";
			}
			buttText += selectedUsers.get(selectedUsers.size() - 1);
			mSendButton.setText(buttText);
			mGuiHandler.removeMessages(SWITCH_BUTTON_TEXT);
			mGuiHandler.sendEmptyMessageDelayed(SWITCH_BUTTON_TEXT, 3000);
		} else {
			mSendButton.setText("No recipients selected");
			mSendButton.setEnabled(false);
			mGuiHandler.removeMessages(SWITCH_BUTTON_TEXT);
		}
	}
	
	/**
	 * Send the Snap to multiple recipients. Called from the Send button.
	 */
	private void sendMultiple() {
		String users = "";
		for (int i = 0; i < selectedUsers.size() - 1; i++) {
			users += selectedUsers.get(i) + ",";
		}
		users += selectedUsers.get(selectedUsers.size() - 1);
		if (isPhoto()) {
			try {
				String finalPath = new File(this.getActivity().getCacheDir(), "temp-" + new Random().nextInt(99999) + ".jpg").getAbsolutePath();
				new SnapUpload(this.getActivity().getApplicationContext(), filePath, finalPath)
						.execute(finalPath, GlobalVars.getUsername(this.getActivity()),
						GlobalVars.getAuthToken(this.getActivity()), Integer.valueOf(getPhotoType()).toString(), users, String.valueOf(snapTime));
				onSuccess();
			} catch (IOException e) {
				StatMethods.hotBread(this.getActivity(), "Error copying file. Please try again", Toast.LENGTH_SHORT);
			}
			
			/* new BGUpload(this.getActivity().getApplicationContext()).execute(filePath, GlobalVars.getUsername(this.getActivity()), GlobalVars.getAuthToken(this.getActivity()), 
				"0", users, String.valueOf(snapTime)); */
		} else {
			try {
				String finalPath = new File(this.getActivity().getCacheDir(), "temp-" + new Random().nextInt(99999) + ".mp4").getAbsolutePath();
				new SnapUpload(this.getActivity().getApplicationContext(), filePath, finalPath)
						.execute(finalPath, GlobalVars.getUsername(this.getActivity()), GlobalVars.getAuthToken(this.getActivity()),
						Integer.valueOf(getPhotoType()).toString(), users, getCaption(), Integer.valueOf(getCaptionOrientation()).toString(),
						Float.valueOf(getCaptionLocation()).toString());
				onSuccess();
			} catch (IOException e) {
				StatMethods.hotBread(this.getActivity(), "Error copying file. Please try again", Toast.LENGTH_SHORT);
			}
			
			/* new BGUpload(this.getActivity().getApplicationContext()).execute(filePath, GlobalVars.getUsername(this.getActivity()),
					GlobalVars.getAuthToken(this.getActivity()), Integer.valueOf(getPhotoType()).toString(), users, getCaption(), 
					Integer.valueOf(getCaptionOrientation()).toString(), Float.valueOf(getCaptionLocation()).toString()); */
		}
	}
	
	private void sendMultipleDelay() {
		String users = "";
		for (int i = 0; i < selectedUsers.size() - 1; i++) {
			users += selectedUsers.get(i) + ",";
		}
		users += selectedUsers.get(selectedUsers.size() - 1);
		TempSnap newSnap = TempSnaps.getInstanceUnsafe().add();
		
		newSnap.setUsers(users);
		
		File finalPath = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH + CameraUtil.PICTURE_PATH,
				newSnap.getId() + selectedUsers.get(0).toUpperCase(Locale.ENGLISH) + ".jpg");
		
		if (FileIO.bufferedCopy(filePath, finalPath.getAbsolutePath()) < 0) {
			StatMethods.hotBread(getActivity(), "File copy failed! Try again later", Toast.LENGTH_SHORT);
			TempSnaps.getInstanceUnsafe().remove(getActivity(), newSnap);
			return;
		}
		
		newSnap.setFilePath(finalPath.getAbsolutePath());
		if (isPhoto()) {
			newSnap.setMediaType(MediaType.PHOTO).setCaptionTime(snapTime);
		} else {
			newSnap.setMediaType(MediaType.VIDEO).setVideoCaption(getCaption()).setCaptionPosition(getCaptionLocation()).setCaptionOrientation(getCaptionOrientation());
		}
		
		TempSnaps.getInstanceUnsafe().write(getActivity());
		
		StatMethods.hotBread(this.getActivity(), "Saved for later...", Toast.LENGTH_SHORT);
		onSuccess();
		
		//this.getActivity().setResult(Activity.RESULT_OK);
		//this.getActivity().finish();
	}
	
	protected boolean isPhoto() {
		return this.getArguments().getBoolean(MEDIA_TYPE_KEY, true);
	}
	
	protected int getPhotoType() {
		return isPhoto() ? 0 : 1;
	}
	
	protected String getUserArg() {
		Bundle args = this.getArguments();
		if (args != null) {
			return args.getString(SELECTED_CONTACT_KEY);
		}
		return null;
	}
	
	protected String getCaption() {
		return this.getArguments().getString(CAPTION_KEY);
	}
	
	protected int getCaptionOrientation() {
		return this.getArguments().getInt(ORIENTATION_KEY, 0);
	}
	
	protected float getCaptionLocation() {
		return this.getArguments().getFloat(CAP_LOCATION_KEY, 0.5f);
	}
	
	/**
	 * A callback used when the user has successfully selected a user to send to
	 */
	protected void onSuccess() {
		SharedPreferences.Editor editor = (Editor) this.getActivity().getSharedPreferences(RESEND_INFO_KEY, Context.MODE_PRIVATE).edit();
		String text = getCaption();
		if (text == null)
			text = "";
		
		String resendFile = new File(this.getActivity().getCacheDir(), RESEND_FILE_PATH + (isPhoto() ? ".jpg" : ".mp4")).getAbsolutePath();
		if (FileIO.bufferedCopy(filePath, resendFile) < 0) {
			StatMethods.hotBread(this.getActivity(), "Error saving file for resend", Toast.LENGTH_SHORT);
		}
		
		editor.putString(SnapEditorBaseFrag.CAPTION_KEY, text).putString(SnapEditorBaseFrag.FILE_PATH_KEY, resendFile)
				.putBoolean(SnapEditorBaseFrag.MEDIA_TYPE_KEY, isPhoto()).putInt(SnapEditorBaseFrag.SNAP_TIME_KEY, snapTime).commit();
		
		onFragmentPopped();
		this.getActivity().setResult(Activity.RESULT_OK);
		this.getActivity().finish();
	}

    @SuppressLint("NewApi")
	@Override
	public void popFragment() {
		if (Build.VERSION.SDK_INT < 11) {
			((Activity)this.getActivity()).getSupportActionBar().hide();
		} else {
			((Activity)this.getActivity()).getActionBar().hide();
		}
		this.getFragmentManager().popBackStack();
		
		onFragmentPopped();
	}
	
	@Override
	public void onFragmentPopped() {
		super.onFragmentPopped();
		
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
	}
	
	/**
	 * The OnClickListener that's called when the user clicks on a button in the confirmation
	 * alert box
	 */
	protected DialogInterface.OnClickListener mesgBoxListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				if (isPhoto()) {
					try {
						String finalPath = new File(BaseContactSelectFrag.this.getActivity().getCacheDir(),
								"temp-" + new Random().nextInt(99999) + ".jpg").getAbsolutePath();
						new SnapUpload(BaseContactSelectFrag.this.getActivity().getApplicationContext(), filePath, finalPath)
								.execute(finalPath, GlobalVars.getUsername(BaseContactSelectFrag.this.getActivity()),
								GlobalVars.getAuthToken(BaseContactSelectFrag.this.getActivity()), Integer.valueOf(getPhotoType()).toString(),
								Contacts.getInstanceUnsafe().getUsernameAt(selectedPosition), String.valueOf(snapTime));
						onSuccess();
					} catch (IOException e) {
						StatMethods.hotBread(BaseContactSelectFrag.this.getActivity(), "Error copying file. Please try again", Toast.LENGTH_SHORT);
					}
					
					/* new BGUpload(BaseContactSelectFrag.this.getActivity().getApplicationContext()).execute(filePath, GlobalVars.getUsername(BaseContactSelectFrag.this.getActivity()), 
						GlobalVars.getAuthToken(BaseContactSelectFrag.this.getActivity()), "0", Contacts.getUsernameAt(selectedPosition), String.valueOf(snapTime)); */
				} else {
					try {
						String finalPath = new File(BaseContactSelectFrag.this.getActivity().getCacheDir(), 
								"temp-" + new Random().nextInt(99999) + ".mp4").getAbsolutePath();
						new SnapUpload(BaseContactSelectFrag.this.getActivity().getApplicationContext(), filePath, finalPath)
								.execute(finalPath, GlobalVars.getUsername(BaseContactSelectFrag.this.getActivity()), 
								GlobalVars.getAuthToken(BaseContactSelectFrag.this.getActivity()), Integer.valueOf(getPhotoType()).toString(),
								Contacts.getInstanceUnsafe().getUsernameAt(selectedPosition), getCaption(), Integer.valueOf(getCaptionOrientation()).toString(),
								Float.valueOf(getCaptionLocation()).toString());
						onSuccess();
					} catch (IOException e) {
						StatMethods.hotBread(BaseContactSelectFrag.this.getActivity(), "Error copying file. Please try again", Toast.LENGTH_SHORT);
					}
					
					/* new BGUpload(BaseContactSelectFrag.this.getActivity().getApplicationContext()).execute(filePath, 
							GlobalVars.getUsername(BaseContactSelectFrag.this.getActivity()), GlobalVars.getAuthToken(BaseContactSelectFrag.this.getActivity()), 
							Integer.valueOf(getPhotoType()).toString(), Contacts.getUsernameAt(selectedPosition), getCaption(), 
							Integer.valueOf(getCaptionOrientation()).toString(), Float.valueOf(getCaptionLocation()).toString()); */
				}
			}
			else if (which == DialogInterface.BUTTON_NEGATIVE) {
				// Do nothing
			}
		}
	};
	
	/**
	 * The ArrayAdapter that is used for the ListView in this ListFragment.
	 * It works with an array of Contacts passed in the constructor.
	 * @author Nick's Laptop
	 */
	protected class ContactViewListAdapter extends ArrayAdapter<Contacts.Contact> {
		private boolean userSet = false;
		
		public ContactViewListAdapter (Context ctxt) {
			super(ctxt, R.layout.contact_text_check_v2);
		}

        @SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater)BaseContactSelectFrag.this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.contact_text_check_v2, parent, false);
			TextView contactText = (TextView)v.findViewById(R.id.contact_textview);
			contactText.setText(Contacts.getInstanceUnsafe().getDisplayOrUserName(position));
			TextView displayText = (TextView)v.findViewById(R.id.contact_displaytext);
			if (Contacts.getInstanceUnsafe().hasDisplay(position)) {
				displayText.setText(Contacts.getInstanceUnsafe().getUsernameAt(position));
			}
			
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v2) {
					BaseContactSelectFrag.this.contactSelect(position);
				}
			});
			
			CheckBox chkBox = (CheckBox)v.findViewById(R.id.checkBox1);
			chkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						numSelected++;
						selectedUsers.add(Contacts.getInstanceUnsafe().getUsernameAt(position));
					} else {
						numSelected--;
						for (int i = 0; i < selectedUsers.size(); i++) {
							if (Contacts.getInstanceUnsafe().getUsernameAt(position).compareTo(selectedUsers.get(i)) == 0) {
								selectedUsers.remove(i);
								break;
							}
						}
					}
					updateButton();
				}
			});
			
			if (!userSet && getUserArg() != null) {
				String user = Contacts.getInstanceUnsafe().getUsernameAt(position);
				if (user.compareTo(getUserArg()) == 0) {
					chkBox.setChecked(true);
					userSet = true;
				}
			}
			
			switch (theme) {
			case ori:
			case snapchat:
				break;
			case def:
			default:
				BitmapDrawable draw;
				if (drawable == null || (draw = drawable.get()) == null) {
					draw = (BitmapDrawable) BaseContactSelectFrag.this.getResources().getDrawable(R.drawable.main_menu_default_background);
					draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
					drawable = new SoftReference<BitmapDrawable>(draw);
				}
				if (Build.VERSION.SDK_INT < 16) {
					v.setBackgroundDrawable(draw);
				} else {
					v.setBackground(draw);
				}
				contactText.setTextColor(0xFFFFFFFF);
				displayText.setTextColor(0xFFA9A9A9);
				break;
		    }
			
			return v;
		}
		
		@Override
		public int getCount() {
			return Contacts.getInstanceUnsafe().getNumContacts();
		}
	}

	protected class LazyImageLoader extends AsyncTask<Void, Void, Bitmap> {
		protected final String mFilePath;
		protected final ImageView mImgView;
		protected final ProgressBar mProgressBar;
		protected final int mWidth;
		protected final int mHeight;
		
		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		private LazyImageLoader(String file, ImageView img, ProgressBar pb) {
			mFilePath = file;
			mImgView = img;
			mProgressBar = pb;
			float scale = BaseContactSelectFrag.this.getActivity().getResources().getDisplayMetrics().density;
			mWidth = (int) (250 * scale + 0.5f);
			Display display = BaseContactSelectFrag.this.getActivity().getWindowManager().getDefaultDisplay();
			if (Build.VERSION.SDK_INT < 13) {
				mHeight = display.getHeight() - (int) (40 * scale + 0.5f);
			} else {
				Point size = new Point();
				display.getSize(size);
				mHeight = size.y - (int) (40 * scale + 0.5f);
			}
		}
		
		@Override
		protected Bitmap doInBackground(Void... params) {
			if (mFilePath == null || mImgView == null || mProgressBar == null) {
				return null;
			}
			if (isPhoto()) {
				return BitmapUtil.decodeSampledBitmapFromFile(mFilePath, mWidth, mHeight);
			} else {
				return ThumbnailUtils.createVideoThumbnail(mFilePath, MediaStore.Video.Thumbnails.MINI_KIND);
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				mImgView.setImageBitmap(bitmap);
				mProgressBar.setVisibility(View.GONE);
				mImgView.setVisibility(View.VISIBLE);
			}
		}
	}
	
	/**
	 * Little handler extension that switches the text on the sending button to show you can
	 * hold it to send later.
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	protected class GuiHandler extends Handler {
		private boolean mButtonTextToggleState = true;
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SWITCH_BUTTON_TEXT:
					if (mSendButton != null && mSendButton.isEnabled()) {
						this.sendEmptyMessageDelayed(SWITCH_BUTTON_TEXT, 3000);
						if (mButtonTextToggleState) {
							mSendButton.setText("Hold to send later");
						} else {
							updateButton();
						}
						mButtonTextToggleState = !mButtonTextToggleState;
					}
					break;
			}
			
			super.handleMessage(msg);
		}
		
		public void resetButtonToggle() {
			mButtonTextToggleState = false;
		}
	}
}