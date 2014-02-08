package com.nickstephen.opensnap.util.tests;

import com.nickstephen.opensnap.global.LocalSnaps.LocalSnap;

public final class DownloadTestTask extends TestTask {
	private final LocalSnap mSnap;
	
	public DownloadTestTask(LocalSnap snap) {
		mSnap = snap;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		mSnap.setDownloadProgress(progress[0]);
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		if (!result) {
			mSnap.setError(true);
			mSnap.setDownloading(false);
			return;
		}
		mSnap.setDownloading(false);
	}
}
