package com.nickstephen.opensnap.main;

import java.io.File;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nickstephen.lib.misc.FileIO;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.composer.CaptureActivity;
import com.nickstephen.opensnap.composer.editor.EditorActivity;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;
import com.nickstephen.opensnap.gui.DragTouchListener;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.misc.CameraUtil;

/**
 * The main menu displayed when the user goes into compose snap
 * @author Nick Stephen (a.k.a saltisgood)
 */
public class ComposerMenuFrag extends Fragment {
	public static final String FRAG_TAG = "ComposerMenuFrag";
	/**
	 * Random value used for the return from a picture library pick activity
	 */
	public static final int PICTURE_PICK = 117;
	/**
	 * Random value used for the return from taking a picture from the camera
	 */
	public static final int CAMERA_PIC_PICK = 343;
	
	private static final int EDITOR_REQUEST_CODE = 6354;
	private static final int EDITOR_REQUEST_CODE_DELETE = 6598;
	private static final int CAPTURE_REQUEST_CODE = 3452;
	
	private String tempFilePath;
	
	public ComposerMenuFrag() {}
	
	@SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		RelativeLayout v = (RelativeLayout)inflater.inflate(R.layout.compose_snap_menu_v2, null);
		
		switch (SettingsAccessor.getThemePref(this.getActivity())) {
			case snapchat:
			case ori:
				break;
			case black:
				ImageView img = new ImageView(this.getActivity());
				img.setImageResource(R.drawable.kanye);
				v.addView(img);
				img.setOnTouchListener(new DragTouchListener());
			case def:
			default:
				TextView txt = (TextView)v.findViewById(R.id.textView1);
				txt.setTextColor(0xFFFFFFFF);
				BitmapDrawable background = (BitmapDrawable)this.getResources().getDrawable(R.drawable.main_menu_default_background);
				background.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
				if (Build.VERSION.SDK_INT < 16) {
					v.setBackgroundDrawable(background);
				} else {
					v.setBackground(background);
				}
				break;
		}
		
		Button button = (Button)v.findViewById(R.id.compose_snap_camera_button);
		button.setOnClickListener(new OnClickListener() {
			/**
			 * Launch the android camera app to take a picture
			 * @param v The button that called this method
			 */
			@Override
			public void onClick(View v) {
				if (!StatMethods.IsCameraAvailable(ComposerMenuFrag.this.getActivity())){
					StatMethods.hotBread(ComposerMenuFrag.this.getActivity(), "Camera not available", Toast.LENGTH_SHORT);
					return;
				}
				if (!StatMethods.getExternalWriteable()) {
					StatMethods.hotBread(ComposerMenuFrag.this.getActivity(), "External storage not available", Toast.LENGTH_SHORT);
					return;
				}
				Intent picIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				//picIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, "camera.jpg")));
				startActivityForResult(picIntent, CAMERA_PIC_PICK);
			}
		});
		
		button = (Button)v.findViewById(R.id.compose_snap_select);
		button.setOnClickListener(new OnClickListener() {
			/**
			 * LaunchActivity the picture picker
			 * @param v The button that called this method
			 */
			@Override
			public void onClick(View v) {
				Intent photoPick = new Intent(Intent.ACTION_PICK);
				photoPick.setType("image/*");
				startActivityForResult(photoPick, PICTURE_PICK);
			}
		});
		
		button = (Button)v.findViewById(R.id.compose_snap_resend);
		button.setOnClickListener(new OnClickListener() {
			/**
			 * Resend button click
			 */
			@Override
			public void onClick(View v) {
				SharedPreferences resendPref = ComposerMenuFrag.this.getSharedPreferences(SnapEditorBaseFrag.RESEND_INFO_KEY, Context.MODE_PRIVATE);
				String filePath = resendPref.getString(SnapEditorBaseFrag.FILE_PATH_KEY, null);
				if (filePath == null) {
					StatMethods.hotBread(ComposerMenuFrag.this.getActivity(), "Missing filePath attribute", Toast.LENGTH_SHORT);
					v.setEnabled(false);
					return;
				}
				File file = new File(filePath);
				if (!file.exists()) {
					StatMethods.hotBread(ComposerMenuFrag.this.getActivity(), "Previously used file not found!", Toast.LENGTH_SHORT);
					v.setEnabled(false);
					return;
				}
				
				if (!SnapEditorBaseFrag.isResendPicture(ComposerMenuFrag.this.getActivity())) {
					if (!StatMethods.getExternalWriteable()) {
						StatMethods.hotBread(ComposerMenuFrag.this.getActivity(), "Need SD-Card access to edit videos!", Toast.LENGTH_SHORT);
						return;
					}
					
					File tempFile = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, CameraUtil.VID_FILE);
					if (FileIO.bufferedCopy(filePath, tempFile.getAbsolutePath()) < 0) {
						StatMethods.hotBread(ComposerMenuFrag.this.getActivity(), "Error copying video to SD", Toast.LENGTH_SHORT);
						return;
					}
					
					tempFilePath = filePath = tempFile.getAbsolutePath();
					Intent intent = new Intent(ComposerMenuFrag.this.getActivity(), EditorActivity.class);
					intent.putExtra(SnapEditorBaseFrag.RESEND_BOOL_KEY, true);
					intent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUsername());
					ComposerMenuFrag.this.startActivityForResult(intent, EDITOR_REQUEST_CODE_DELETE);
					return;
				}
				
				Intent intent = new Intent(ComposerMenuFrag.this.getActivity(), EditorActivity.class);
				intent.putExtra(SnapEditorBaseFrag.RESEND_BOOL_KEY, true);
				intent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUsername());
				ComposerMenuFrag.this.startActivityForResult(intent, EDITOR_REQUEST_CODE);
			}
		});
		
		android.content.SharedPreferences resendPref = this.getActivity().getSharedPreferences(SnapEditorBaseFrag.RESEND_INFO_KEY, Context.MODE_PRIVATE);
		if (!resendPref.contains(SnapEditorBaseFrag.FILE_PATH_KEY)) {
			button.setEnabled(false);
		}
		
		button = (Button)v.findViewById(R.id.snap_cam_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent camIntent = new Intent(ComposerMenuFrag.this.getActivity(), CaptureActivity.class);
				camIntent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUsername());
				ComposerMenuFrag.this.startActivityForResult(camIntent, CAPTURE_REQUEST_CODE);
			}
		});
		
		return v;
	}
	
	private String getUsername() {
		Bundle args = this.getArguments();
		if (args != null) {
			return args.getString(BaseContactSelectFrag.SELECTED_CONTACT_KEY);
		}
		return null;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
		super.onActivityResult(requestCode, resultCode, returnIntent);
		
		switch (requestCode) {
			case PICTURE_PICK:
				if (resultCode == Activity.RESULT_OK) {
					Intent intent = new Intent(this.getActivity(), EditorActivity.class);
					intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, FileIO.getRealPathFromUri(this.getActivity(), returnIntent.getData()));
					intent.putExtra(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true);
					intent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUsername());
					this.startActivityForResult(intent, EDITOR_REQUEST_CODE);
				}
				break;
			case CAMERA_PIC_PICK:
				if (resultCode == Activity.RESULT_OK) {
					Intent intent = new Intent(this.getActivity(), EditorActivity.class);

                    Uri data = returnIntent.getData();
                    if (data != null) {
                        tempFilePath = FileIO.getRealPathFromUri(this.getActivity(), returnIntent.getData());
                    } else { // Samsung S3 Fix
                        tempFilePath = FileIO.getRealPathFromLastPic(this.getActivity());
                    }

					intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, tempFilePath);
					intent.putExtra(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true);
					intent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUsername());
					this.startActivityForResult(intent, EDITOR_REQUEST_CODE_DELETE);
				}
				break;
			case EDITOR_REQUEST_CODE_DELETE:
				File file = new File(tempFilePath);
				if (file.exists()) {
					file.delete();
				}
			case EDITOR_REQUEST_CODE:
				if (resultCode == Activity.RESULT_OK) {
					//this.getActivity().onBackPressed();
					this.getFragmentManager().popBackStack();
				}
				break;
			case CAPTURE_REQUEST_CODE:
				if (resultCode == Activity.RESULT_OK) {
					//this.getActivity().onBackPressed();
					this.getFragmentManager().popBackStack();
				}
				break;
			default:
				break;
		}
	}
}
