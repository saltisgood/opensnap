package com.nickstephen.opensnap.util.misc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;

import com.nickstephen.lib.Twig;

public class CameraUtil {
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	public static final String ROOT_PATH = "/OpenSnap/";
	public static final String PICTURE_PATH = "/Snap Pictures/";
	public static final String VIDEO_PATH = "/Snap Videos/";
	public static final String CAMERA_FILE = "camera.jpg";
	public static final String VID_FILE = "temp.mp4";

	private CameraUtil() {
	}

	public static Camera getDefaultCameraInstance() {
        return Camera.open();
	}
	
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static Camera getCameraInstance(int camID) {
		Camera c = null;
		
		try {
			c = Camera.open(camID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return c;
	}
	
	@SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(Context ctxt, int type) {
		File outputFolder = new File(Environment.getExternalStorageDirectory() + ROOT_PATH);
		
		if (!outputFolder.exists()) {
			if (!outputFolder.mkdirs()) {
				Twig.debug("OpenSnap CameraUtil", "Failed to create directories!");
			}
		}
		
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		
		switch (type) {
			case MEDIA_TYPE_IMAGE:
				outputFolder = new File(outputFolder, PICTURE_PATH);
				if (!outputFolder.exists() && !outputFolder.mkdirs()) {
					Twig.debug("OpenSnap CameraUtil", "Failed to create directories!");
				}
				mediaFile = new File(outputFolder, "IMG_" + timestamp + ".jpg");
				break;
			case MEDIA_TYPE_VIDEO:
				outputFolder = new File(outputFolder, VIDEO_PATH);
				if (!outputFolder.exists() && !outputFolder.mkdirs()) {
					Twig.debug("OpenSnap CameraUtil", "Failed to create directories!");
				}
				mediaFile = new File(outputFolder, "VID_" + timestamp + ".mp4");
				break;
			default:
				return null;
		}
		
		return mediaFile;
	}
}
