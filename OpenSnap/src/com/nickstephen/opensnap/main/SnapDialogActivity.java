package com.nickstephen.opensnap.main;

import static com.nickstephen.opensnap.preview.PreviewConstants.CAPTION_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.CAPTION_LOC_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.PATH_KEY;
import static com.nickstephen.opensnap.preview.PreviewConstants.TIME_KEY;

import java.io.File;
import java.io.IOException;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListAdapter;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.preview.MediaPreview;
import com.nickstephen.opensnap.preview.VideoPreview;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.misc.DeleteSnap;

public class SnapDialogActivity extends Activity {
	public static final String SNAP_NO = "dicksdicksdicksandmoredicks";
	public static final String FILE_PATH_KEY = "file_path";
	public static final String SNAP_TIME_KEY = "snap_time";
	public static final String MEDIA_TYPE_KEY = "media_type";
	public static final String CAPTION_TEXT_KEY = "caption_text";
	public static final String CAPTION_ORI_KEY = "caption_ori";
	public static final String CAPTION_POS_KEY = "caption_pos";
	
	private LocalSnaps.LocalSnap mSnap;
	private String mFilePath;
	private int mSnapTime;
	private boolean mIsPhoto;
	private String mCaption;
	private int mCaptionOri;
	private float mCaptionPos;
	private boolean mAllowSaves;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.snap_dialog);
		
		WindowManager.LayoutParams params = this.getWindow().getAttributes();
		params.gravity = Gravity.CENTER;
		this.getWindow().setAttributes(params);
		
		ListView listView = (ListView)this.findViewById(R.id.listView1);
		listView.setAdapter(new DialogListAdapter());
		
		int snapno = this.getIntent().getIntExtra(SNAP_NO, -1);
		if (snapno == -1) {
			StatMethods.hotBread(this, "Error: missing mSnap number in intent", Toast.LENGTH_SHORT);
			this.finish();
		}
		
		mSnap = LocalSnaps.getSnapAt(snapno);
		
		mSnapTime = mSnap.getDisplayTime();
		mIsPhoto = mSnap.isPhoto();
		if (!mIsPhoto) {
			mCaption = mSnap.getCaption();
			mCaptionOri = mSnap.getCaptionOrientation();
			mCaptionPos = mSnap.getCaptionLocation();
		}
		
		mAllowSaves = SettingsAccessor.getAllowSaves(this);
		try {
			mFilePath = mSnap.getSnapPath(this);
		} catch (IOException e) {
			Twig.debug("SnapDialogActivity", "Error getting path");
		}
	}
	
	private class DialogListAdapter implements ListAdapter {
		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
		}

		@Override
		public int getCount() {
			return 5;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater)SnapDialogActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			TextView view = (TextView)inflater.inflate(R.layout.dialog_list_textview, null);
			
			switch (position) {
				case 0:
					view.setText("View Snap");
					if (mFilePath == null || !new File(mFilePath).exists()) {
						view.setEnabled(false);
					}
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (mAllowSaves) {
								return;
							}
							
							if (mIsPhoto) {
								Intent intent = new Intent(SnapDialogActivity.this, MediaPreview.class);
								if (mSnapTime != -1) {
									intent.putExtra(TIME_KEY, mSnapTime);
								}
								intent.putExtra(PATH_KEY, mFilePath);
								SnapDialogActivity.this.startActivity(intent);
							} else {
								Intent intent = new Intent(SnapDialogActivity.this, VideoPreview.class);
								intent.putExtra(PATH_KEY, mFilePath);
								if (mCaption != null) {
									intent.putExtra(CAPTION_KEY, mCaption);
									intent.putExtra(CAPTION_ORI_KEY, mCaptionOri);
									intent.putExtra(CAPTION_LOC_KEY, mCaptionPos);
								}
								SnapDialogActivity.this.startActivity(intent);
							}
						}
					});
					break;
				case 1:
					view.setText("Download Snap");
					
					if (mSnap.isDownloading() || !mSnap.getSnapAvailable() || mSnap.getSnapExists(SnapDialogActivity.this)) {
						view.setEnabled(false);
					}
					
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							StatMethods.hotBread(SnapDialogActivity.this, "Not implemented yet", Toast.LENGTH_SHORT);
						}
					});
					break;
				case 2:
					view.setText("Delete Snap");
					if (mFilePath == null || !new File(mFilePath).exists()) {
						view.setEnabled(false);
					}
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							StatMethods.QuestionBox(SnapDialogActivity.this, "Delete Snap?", "Are you sure? This can't be undone...", 
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											if (DeleteSnap.deleteSnapAndRescanMedia(SnapDialogActivity.this, mFilePath)) {
												StatMethods.hotBread(SnapDialogActivity.this, "Delete successful", Toast.LENGTH_SHORT);
												SnapDialogActivity.this.finish();
											} else {
												StatMethods.hotBread(SnapDialogActivity.this, "Delete unsuccessful. Try again later", Toast.LENGTH_SHORT);
											}
											/* File file = new File(mFilePath);
											if (file.delete()) {
												StatMethods.hotBread(SnapDialogActivity.this, "Delete successful", Toast.LENGTH_SHORT);
												SnapDialogActivity.this.finish();
											} else {
												StatMethods.hotBread(SnapDialogActivity.this, "Delete unsuccessful. Try again later", Toast.LENGTH_SHORT);
											} */
										}
									}, null);
						}
					});
					break;
				case 3:
					view.setText("View Details");
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							StatMethods.hotBread(SnapDialogActivity.this, "Not implemented yet", Toast.LENGTH_SHORT);
						}
					});
					break;
				case 4:
					view.setText("Close");
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							SnapDialogActivity.this.finish();
						}
					});
					break;
			}
			
			return view;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}
		
	}
}
