package com.nickstephen.opensnap.util.misc;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

/**
 * Static class for deleting mSnaps from the phone with the ability to remove from the media store as well
 * @author Nicholas Stephen (a.k.a. saltisgood)
 */
public final class DeleteSnap {

	private DeleteSnap() {	}

	/**
	 * Deletes a snap from file. Does not through any exceptions on failures. Check the return value to see
	 * whether the file didn't exist or wasn't able to be deleted. 
	 * @param filePath The path to the snap to be deleted
	 * @return True on success, false if the file didn't exist or wasn't able to be deleted
	 */
	public static boolean deleteSnap(String filePath) {
		File file = new File(filePath);
		if (!file.exists() || !file.delete()) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Deletes a snap from file and also sends a broadcast to rescan the media store. Returns a value taken
	 * from a call to {@link #deleteSnap}.
	 * @param context A context to use for sending the broadcast
	 * @param filePath The path of the file to delete
	 * @return True on success, false if the file didn't exist or wasn't able to be deleted
	 */
	public static boolean deleteSnapAndRescanMedia(Context context, String filePath) {
		boolean result = deleteSnap(filePath);
		
		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		
		return result;
	}
}
