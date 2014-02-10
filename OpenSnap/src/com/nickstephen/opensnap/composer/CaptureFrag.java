package com.nickstephen.opensnap.composer;

import java.io.File;

import org.holoeverywhere.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import com.nickstephen.opensnap.composer.editor.EditorActivity;
import com.nickstephen.opensnap.gui.BaseCaptureFrag;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.util.misc.CameraUtil;

public class CaptureFrag extends BaseCaptureFrag {
	public static final int CAPTURE_EDIT_REQUEST_CODE = 9876;
	
	public CaptureFrag() {
	}
	
	@Override
	protected void postPictureCallback(String filePath) {
		Intent intent = new Intent(this.getActivity(), EditorActivity.class);
		intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, filePath);
		intent.putExtra(EditorActivity.RETURN_TO_CAMERA_KEY, true);
		intent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUser());
		this.startActivityForResult(intent, CAPTURE_EDIT_REQUEST_CODE);
	}
	
	@Override
	protected void postVideoCallback(String filePath) {
		Intent intent = new Intent(this.getActivity(), EditorActivity.class);
		intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, filePath);
		intent.putExtra(SnapEditorBaseFrag.MEDIA_TYPE_KEY, false);
		intent.putExtra(EditorActivity.RETURN_TO_CAMERA_KEY, true);
		intent.putExtra(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUser());
		this.startActivityForResult(intent, CAPTURE_EDIT_REQUEST_CODE);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
			case CAPTURE_EDIT_REQUEST_CODE:
				File file = new File(this.getActivity().getCacheDir(), CAM_FILENAME);
				if (file.exists()) {
					file.delete();
				}
				file = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, VID_FILENAME);
				//file = new File(this.getActivity().getCacheDir(), VID_FILENAME);
				if (file.exists()) {
					file.delete();
				}
				if (resultCode == Activity.RESULT_OK) {
					this.getActivity().setResult(Activity.RESULT_OK);
					this.getActivity().finish();
				}
				break;
		}
	}
	
	private String getUser() {
		Bundle args = this.getArguments();
		if (args != null) {
			return args.getString(BaseContactSelectFrag.SELECTED_CONTACT_KEY);
		}
		return null;
	}
}
