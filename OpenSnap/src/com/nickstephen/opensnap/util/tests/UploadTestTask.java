package com.nickstephen.opensnap.util.tests;

import com.nickstephen.opensnap.global.TempSnaps.TempSnap;

public final class UploadTestTask extends TestTask {
	private final TempSnap mSnap;
	
	public UploadTestTask(TempSnap snap) {
		mSnap = snap;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		mSnap.setUploadPercent(progress[0]);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (!result) {
			mSnap.setError(true).setIsSending(false);
			return;
		}
		mSnap.setIsSending(false).setSent(true).setTimeStamp(System.currentTimeMillis());
	}

}
