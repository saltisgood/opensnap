package com.nickstephen.opensnap.util.tests;

import android.os.AsyncTask;

public abstract class TestTask extends AsyncTask<Integer, Integer, Boolean> {
	@Override
	protected Boolean doInBackground(Integer... params) {
		for (int i = 0; i < 100; i++) {
			this.publishProgress(i);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (params.length > 0 && params[0] == i) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	abstract protected void onProgressUpdate(Integer... progress);
	
	@Override
	abstract protected void onPostExecute(Boolean result);
}
