package com.nickstephen.opensnap.util.misc;

import java.io.File;

import android.os.Environment;

import com.nickstephen.lib.misc.StatMethods;

public final class FileIO extends com.nickstephen.lib.misc.FileIO {
	/**
	 * Setup the folders to save media to. Will also check for external storage.
	 * @return 0 on success.
	 */
	public static int setupFolders() {
		if (!StatMethods.getExternalReadable() || !StatMethods.getExternalWriteable()) {
			return -1;
		}
		
		File rootDir = new File(Environment.getExternalStorageDirectory(), CameraUtil.ROOT_PATH);
		if (!rootDir.exists()) {
			rootDir.mkdirs();
		}
		File subDir = new File(rootDir, CameraUtil.PICTURE_PATH);
		if (!subDir.exists()) {
			subDir.mkdirs();
		}
		subDir = new File(rootDir, CameraUtil.VIDEO_PATH);
		if (!subDir.exists()) {
			subDir.mkdirs();
		}
		
		return 0;
	}
	
	protected FileIO() {}
}
