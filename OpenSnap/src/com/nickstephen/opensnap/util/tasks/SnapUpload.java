package com.nickstephen.opensnap.util.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.holoeverywhere.widget.Toast;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;

import com.nickstephen.lib.http.IWriteListener;
import com.nickstephen.lib.misc.FileIO;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps.MediaType;
import com.nickstephen.opensnap.global.TempSnaps;
import com.nickstephen.opensnap.global.TempSnaps.TempSnap;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.http.SnapAPI;
import com.nickstephen.opensnap.util.misc.CameraUtil;
import com.nickstephen.opensnap.util.notify.Notifications;

public class SnapUpload extends AsyncTask<String, Long, Integer> implements IWriteListener {

	protected final Context mLocalContext;
	protected long mUploadSize;
	protected final TempSnap mSnap;
	protected final boolean mDoNotifications;
	/**
	 * Generic name, so I'll explain it. This is for keeping track of whether the TempSnap is being 
	 * populated with information via the params to the background method or should already have
	 * it's information.
	 */
	protected final boolean mAlreadyBeenCreated;
	
	/**
	 * Constructor 1. Used if sending a snap that's already been instantiated. Normally if sending after
	 * the user has saved it for some time.
	 * @param ctxt
	 * @param snapToSend
	 */
	public SnapUpload(Context ctxt, TempSnap snapToSend) {
		mLocalContext = ctxt;
		mSnap = snapToSend;
		mAlreadyBeenCreated = true;
		
		if (mDoNotifications = SettingsAccessor.getUploadNotificationPref(mLocalContext)) {
			Notifications.init(mLocalContext);
			Notifications.setupUploadNotification(mLocalContext);
		}
	}
	
	/**
	 * Constructor 2. Used if immediately sending a snap. This constructor copies the snap which is why
	 * it can throw an IOException.
	 * @param ctxt
	 * @param oriPath
	 * @param finalPath
	 */
	public SnapUpload(Context ctxt, String oriPath, String finalPath) throws IOException {
		mLocalContext = ctxt;
		mSnap = TempSnaps.getInstanceUnsafe().add();
		mAlreadyBeenCreated = false;
		
		if (FileIO.bufferedCopy(oriPath, finalPath) < 0) {
			TempSnaps.getInstanceUnsafe().remove(mLocalContext, mSnap);
			throw new IOException("Error copying file");
		}
		
		if (mDoNotifications = SettingsAccessor.getUploadNotificationPref(mLocalContext)) {
			Notifications.init(mLocalContext);
			Notifications.setupUploadNotification(mLocalContext);
		}
	}
	
	@Override
	protected void onPreExecute() {
		StatMethods.hotBread(mLocalContext, "Sending Snap...", Toast.LENGTH_SHORT);
	}
	
	/**
	 * Params guide:
	 * 0: Path to snap file
	 * 1: Username of sender
	 * 2: Auth token
	 * 3: Media type enum
	 * 4: Users to send to
	 * 5: Snap time or caption
	 * 6: (optional) caption mCameraOrientation
	 * 7: (optional) caption position
	 */
	@Override
	protected Integer doInBackground(String... params) {
		boolean isPhoto;
		String path, username = params[1], authToken = params[2], target, captionText = null;
		int mediaType, timeToDisplay = 10, captionOri = 0;
		float captionPos = 0f;
		
		if (!mAlreadyBeenCreated) {
			if (params.length < 6 || params[0] == null || params[1] == null || params[2] == null || params[3] == null || params[4] == null || params[5] == null) {
				return -1;
			}
			
			try {
				mediaType = Integer.valueOf(params[3]);
				isPhoto = mediaType == 0;
				if (isPhoto) {
					timeToDisplay = Integer.valueOf(params[5]);
				} else {
					captionText = params[5];
					captionOri = Integer.valueOf(params[6]);
					captionPos = Float.valueOf(params[7]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
			
			path = params[0];
			target = params[4];
			
			mSnap.setFilePath(path).setUsers(target).setIsSending(true);
			if (isPhoto) {
				mSnap.setMediaType(MediaType.PHOTO);
			} else {
				mSnap.setMediaType(MediaType.VIDEO).setVideoCaption(captionText).setCaptionOrientation(captionOri).setCaptionPosition(captionPos);
				
			}
			TempSnaps.getInstanceUnsafe().write(mLocalContext);
		} else {
			isPhoto = mSnap.isPhoto();
			mediaType = isPhoto ? 0 : 1;
			path = mSnap.getFilePath();
			target = mSnap.getUsers();
			captionText = mSnap.getVideoCaption();
			captionOri = mSnap.getCaptionOrientation();
			captionPos = mSnap.getCaptionPosition();
		}
		
		String mediaID;
		try {
			mediaID = SnapAPI.uploadFile(mLocalContext, path, username, authToken, mediaType, this, 0);
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
		
		if (isPhoto) {
			if (SnapAPI.sendFile(mediaID, username, authToken, target, timeToDisplay, new String[] { }) == -1) {
				return -3;
			}
		} else {
			if (SnapAPI.sendFile(mediaID, username, authToken, target, timeToDisplay, captionText, String.valueOf(captionOri), String.valueOf(captionPos)) == -1) {
				return -3;
			}
		}
		
		String[] users = target.split(",");
		String fullpath = null;
		for (String user : users) {
			File finalPath;
			if (isPhoto) {
				finalPath = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH + CameraUtil.PICTURE_PATH,
						mediaID + user.toUpperCase(Locale.ENGLISH) + ".jpg");
			} else {
				finalPath = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH + CameraUtil.VIDEO_PATH,
						mediaID + user.toUpperCase(Locale.ENGLISH) + ".mp4");
			}
			fullpath = finalPath.getAbsolutePath();
			
			if (FileIO.bufferedCopy(path, finalPath.getAbsolutePath()) < 0) {
				return -4;
			}
		}
		
		if (isPhoto) {
			MediaScannerConnection.scanFile(mLocalContext, new String[] { fullpath }, new String[] { "image/jpeg" }, null);
		} else {
			MediaScannerConnection.scanFile(mLocalContext, new String[] { fullpath }, new String[] { "video/mp4" }, null);
		}
		
		//SettingsAccessor.cleanupCache(mLocalContext);
		if (!mAlreadyBeenCreated) {
			File file = new File(path);
			file.delete();
		}
		
		return 0;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if (result < 0) {
			mSnap.setError(true);
			TempSnaps.getInstanceUnsafe().write(mLocalContext);
			if (mDoNotifications) {
				Notifications.updateUploadNotificationWithError();
			}
		} 
		switch (result) {
			case -1:
				StatMethods.hotBread(mLocalContext, "Error during param parsing", Toast.LENGTH_LONG);
				return;
			case -2:
				StatMethods.hotBread(mLocalContext, "Error during file upload", Toast.LENGTH_LONG);
				return;
			case -3:
				StatMethods.hotBread(mLocalContext, "Error during file send", Toast.LENGTH_LONG);
				return;
			case -4:
				StatMethods.hotBread(mLocalContext, "Note: file copy did not succeed", Toast.LENGTH_LONG);
				return;
		}
		if (mDoNotifications) {
			Notifications.updateUploadNotificationWithFinish();
		}
		mSnap.setSent(true).setIsSending(false).setTimeStamp(System.currentTimeMillis());
		TempSnaps.getInstanceUnsafe().write(mLocalContext);
	}
	
	@Override
	protected void onCancelled(Integer result) {
		GlobalVars.releaseNetwork();
		
		if (mDoNotifications) {
			Notifications.updateUploadNotificationWithError();
		}
		
		mSnap.setError(true).setIsSending(false);
	}
	
	@Override
	public void registerWrite(long amountOfBytesWritten, int flags) {
		this.publishProgress(amountOfBytesWritten);
	}
	
	@Override
	public void setLength(long totalBytes) {
		mUploadSize = totalBytes;
	}
	
	@Override
	protected void onProgressUpdate(Long... bytes) {
		float percent = (float) bytes[0] / (float) mUploadSize;
		percent *= 100;
		int perInt = (int) percent;
		
		mSnap.setUploadPercent(perInt);
		
		if (mDoNotifications) {
			if (mUploadSize > 0) {
				Notifications.updateUploadNotification(perInt);
			} else {
				Notifications.updateUploadNotification(-1);
			}
		}
	}

}
