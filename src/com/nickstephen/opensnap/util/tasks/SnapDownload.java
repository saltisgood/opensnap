package com.nickstephen.opensnap.util.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.holoeverywhere.widget.Toast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.http.IWriteListener;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps.LocalSnap;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.http.SnapAPI;
import com.nickstephen.opensnap.util.http.SnapGoneException;
import com.nickstephen.opensnap.util.notify.Notifications;

/**
 * An {@link AsyncTask} extension for background downloading a mSnap. Requires a context and a LocalSnap object for instantiation.
 * Requires 2 strings to be passed to the execute function. The username and authtokens to use.
 * @author Nicholas Stephen (a.k.a. saltisgood)
 */
public class SnapDownload extends AsyncTask<String, Long, Integer> implements IWriteListener {
	private final Context mContext;
	private long mDownloadLength;
	private final LocalSnap mSnap;
	private final boolean mDoNotifications; 

	public SnapDownload(Context c, LocalSnap s) {
		mSnap = s;
		mContext = c;
		if (mDoNotifications = SettingsAccessor.getDownloadNotificationPref(c)) {
			Notifications.init(mContext);
			Notifications.setupDownloadNotification(mContext);
		}
		mSnap.setDownloading(true).setError(false);
	}

	@Override
	protected Integer doInBackground(String... args) {
		if (!mSnap.getSnapAvailable()) {
			return -1;
		}
		
		GlobalVars.lockNetwork(-1);

		File imgFile;
		boolean allowsaves = SettingsAccessor.getAllowSaves(mContext);
		try {
			imgFile = new File(mSnap.getSnapPath(mContext));
			/* if (SettingsAccessor.getAllowSaves(mContext)) {
				allowsaves = true;
				imgFile = new File(mSnap.getSnapPath());
			} else {
				allowsaves = false;
				String ext = mSnap.isPhoto() ? ".jpg" : ".mp4";
				imgFile = new File(mContext.getCacheDir(), mSnap.getSnapId() + ext);
			} */
		} catch (IOException e) {
			e.printStackTrace();
			return -4;
		}

		byte[] buff;
		try {
			if ((buff = SnapAPI.decryptBlob(mSnap.getSnapId(), args[0], args[1], this, 0)) == null) {
				return -2;
			}
		} catch (SnapGoneException e) {
			e.printStackTrace();
			return -6;
		}
		
		GlobalVars.releaseNetwork();

		try {
			FileOutputStream fs = new FileOutputStream(imgFile);
			fs.write(buff);
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
			return -3;
		}
		
		buff = null;

		if (!imgFile.exists()) {
			return -5;
		}
		
		// Generate the thumbnail
		if (mSnap.isPhoto()) {
			try {
				Bitmap fullsize = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

				Bitmap thumb = Bitmap.createScaledBitmap(fullsize, (int)(128 * ((float)fullsize.getWidth()/(float)fullsize.getHeight())), 128, false);

				if (thumb != fullsize) {
					fullsize.recycle();
					fullsize = null;
				}

				FileOutputStream fs = new FileOutputStream(mSnap.getSnapThumbPath());
				thumb.compress(CompressFormat.JPEG, 90, fs);
				fs.close();
			} catch (IOException e) {
				Twig.warning("SnapDownload", "Error creating thumb: " + e.getMessage());
			}
		} else {
			Bitmap thumb = ThumbnailUtils.createVideoThumbnail(imgFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
			if (thumb == null) {
				Twig.warning("SnapDownload", "Error creating video thumb");
			} else {
				try {
					FileOutputStream fs = new FileOutputStream(mSnap.getSnapThumbPath());
					thumb.compress(CompressFormat.JPEG, 90, fs);
					fs.close();
				} catch (IOException e) {
					Twig.warning("SnapDownload", "Error creating video thumb: " + e.getMessage());
				}
			}
		}

		if (allowsaves) {
			if (mSnap.isPhoto()) {
				if (mContext != null) MediaScannerConnection.scanFile(mContext, new String[] { imgFile.getAbsolutePath() }, new String[] { "image/jpeg" }, null);
			} else {
				if (mContext != null) MediaScannerConnection.scanFile(mContext, new String[] { imgFile.getAbsolutePath() }, new String[] { "video/avc" }, null);
			}
		}

		return 0;
	}

	@Override
	protected void onCancelled(Integer res) {
		GlobalVars.releaseNetwork();
		
		if (mDoNotifications) {
			Notifications.updateDownloadNotificationWithError();
		}
		
		mSnap.setError(true);
		mSnap.setDownloading(false);
	}

	@Override
	protected void onPostExecute(Integer result) {
		GlobalVars.releaseNetwork();
		mSnap.setDownloading(false);
		if (result < 0) {
			if (mDoNotifications) {
				Notifications.updateDownloadNotificationWithError();
			}
			
			mSnap.setError(true);
		}

		switch (result) {
			case 0:
				break;
			case -1:
				StatMethods.hotBread(mContext, "Snap not available\nHow did you get here???", Toast.LENGTH_LONG);
				return;
			case -2:
				StatMethods.hotBread(mContext, "Error during download", Toast.LENGTH_LONG);
				return;
			case -3:
				StatMethods.hotBread(mContext, "Error writing to file", Toast.LENGTH_LONG);
				return;
			case -4:
				StatMethods.hotBread(mContext, "Error getting file info", Toast.LENGTH_LONG);
				return;
			case -5:
				StatMethods.hotBread(mContext, "Error writing to file\nFile missing in system", Toast.LENGTH_LONG);
				return;
			case -6:
				StatMethods.hotBread(mContext, "Snap not available on server :(", Toast.LENGTH_LONG);
				return;
			default:
				StatMethods.hotBread(mContext, "General error", Toast.LENGTH_LONG);
				return;
		}
		if (mDoNotifications) {
			Notifications.updateDownloadNotificationWithFinish();
		}
	}

	@Override
	public void setLength(long totalBytes) {
		mDownloadLength = totalBytes;
	}

	@Override
	public void registerWrite(long amountOfBytesWritten, int flags) {
		this.publishProgress(amountOfBytesWritten);
	}

	@Override
	protected void onProgressUpdate(Long... bytes) {
		float percent = (float) bytes[0] / (float) mDownloadLength;
		percent *= 100;
		int perInt = (int) percent;
		mSnap.setDownloadProgress(perInt);
		
		if (mDoNotifications) {
			if (mDownloadLength > 0) {
				Notifications.updateDownloadNotification(perInt);
			} else {
				Notifications.updateDownloadNotification(-1);
			}
		}
	}
}
