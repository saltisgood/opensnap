package com.nickstephen.opensnap.util.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

/**
 * An asynchronous image loader that will create thumbnails and save them if they don't
 * already exist. Will also load the image into an image view if wanted
 * @author Nick Stephen
 *
 */
public class LazyImageLoader extends AsyncTask<String, Void, Bitmap> {
	/**
	 * A {@link WeakReference} to the ImageView that was passed to this object. 
	 * It's a weak reference so that it can GC'ed if memory is low.
	 */
	private final WeakReference<ImageView> mImgViewRef;
	private final boolean mHasViewRef;
	private final boolean mIsPhoto;
	
	/**
	 * Construct this object with a reference to an ImageView that will be used to load the image into
	 * @param imgView
	 */
	public LazyImageLoader(ImageView imgView, boolean isPhoto) {
		mIsPhoto = isPhoto;
		if (imgView == null) {
			mImgViewRef = null;
			mHasViewRef = false;
		} else {
			mImgViewRef = new WeakReference<ImageView>(imgView);
			mHasViewRef = true;
		}
	}
	
	@Override
	protected Bitmap doInBackground(String... path) {
		File thumbFile = new File(path[1]);
		if (thumbFile.exists()) {
			return BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
		}
		if (mIsPhoto) {
			Bitmap thumb = null;
			try {
				Bitmap fullsize = BitmapFactory.decodeFile(path[0]);

				thumb = Bitmap.createScaledBitmap(fullsize, (int)(128 * ((float)fullsize.getWidth()/(float)fullsize.getHeight())), 128, false);

				if (thumb != fullsize) {
					fullsize.recycle();
					fullsize = null;
				}

				FileOutputStream fs = new FileOutputStream(thumbFile);
				thumb.compress(CompressFormat.JPEG, 90, fs);
				fs.close();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			return thumb;
		} else {
			Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path[0], MediaStore.Video.Thumbnails.MINI_KIND);
			if (thumb == null) {
				return null;
			}
			
			try {
				FileOutputStream fs = new FileOutputStream(new File(path[1]));
				thumb.compress(CompressFormat.JPEG, 90, fs);
				fs.close();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return thumb;
		}
	}
	
	@Override
	protected void onPostExecute(Bitmap img) {
		if ((this.isCancelled() || !mHasViewRef) && img != null) {
			img.recycle();
			img = null;
		}
		
		ImageView imgView;
		if ((mImgViewRef != null) && ((imgView = mImgViewRef.get()) != null)) {
			if (img != null) {
				imgView.setImageBitmap(img);
			}
		}
	}
}
