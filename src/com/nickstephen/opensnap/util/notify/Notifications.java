package com.nickstephen.opensnap.util.notify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.nickstephen.opensnap.R;

public final class Notifications {
	public static final int DOWNLOAD_NOTIFICATION_ID = 0x100;
	public static final int UPLOAD_NOTIFICATION_ID = 0x1000;
	
	private static NotificationManager sNotifyManager;
	
	private static NotificationCompat.Builder sDownloadBuilder;
	private static int sActiveDownloads = 0;
	private static int sCurrentDownload = 0;
	private static RemoteViews sDownloadView;
	private static long sLastDownloadUpdateTime;
	
	private static NotificationCompat.Builder sUploadBuilder;
	private static RemoteViews sUploadView;
	private static int sActiveUploads = 0;
	private static int sCurrentUpload = 0;
	private static long sLastUploadUpdateTime;
	
	private Notifications() {}
	
	/**
	 * Setup the Notifications abstraction
	 * @param ctxt A context to use
	 */
	public static void init(Context ctxt) {
		if (sNotifyManager == null) {
			sNotifyManager = (NotificationManager)ctxt.getSystemService(Context.NOTIFICATION_SERVICE);
		}
		if (sDownloadBuilder == null) {
			sDownloadBuilder = new NotificationCompat.Builder(ctxt);
		}
		if (sUploadBuilder == null) {
			sUploadBuilder = new NotificationCompat.Builder(ctxt);
		}
	}

	/**
	 * Clear all download notifications
	 * @param ctxt A context
	 */
	public static void clearDownloadNotifications(Context ctxt) {
		if (sNotifyManager == null) {
			init(ctxt);
		}
		
		sNotifyManager.cancel(DOWNLOAD_NOTIFICATION_ID);
	}
	
	/**
	 * Clear all upload notifications
	 * @param ctxt A context
	 */
	public static void clearUploadNotifications(Context ctxt) {
		if (sNotifyManager == null) {
			init(ctxt);
		}
		
		sNotifyManager.cancel(UPLOAD_NOTIFICATION_ID);
	}
	
	/**
	 * Clear all notifications
	 * @param ctxt A context
	 */
	public static void clearAllNotifications(Context ctxt) {
		if (sNotifyManager == null) {
			init(ctxt);
		}
		
		sNotifyManager.cancelAll();
	}
	
	/**
	 * Setup a new download notification. This should be called whenever starting a new download. (Even before it physically starts
	 * downloading)
	 * @param ctxt A context
	 */
	public static void setupDownloadNotification(Context ctxt) {
		if (sActiveDownloads++ == 0) {
			sDownloadView = new RemoteViews(ctxt.getPackageName(), R.layout.legacy_progress_notification);
			sDownloadView.setImageViewResource(R.id.notification_left_image, R.drawable.ghost);
			sDownloadView.setTextViewText(R.id.notification_title_text, "OpenSnap");
			sDownloadView.setTextViewText(R.id.notification_desc_text, "Downloading snap...");
			sDownloadView.setProgressBar(R.id.notification_progress_bar, 100, 0, false);
			sDownloadBuilder.setOngoing(true).setTicker("Downloading snap...").setSmallIcon(R.drawable.ghost).setProgress(0, 100, false)
				.setContentIntent(PendingIntent.getActivity(ctxt, 0, new Intent(), Intent.FLAG_ACTIVITY_NEW_TASK)).setVibrate(null);
			Notification not = sDownloadBuilder.build();
			if (Build.VERSION.SDK_INT < 14) {
				not.contentView = sDownloadView;
			}
			sNotifyManager.notify(DOWNLOAD_NOTIFICATION_ID, not);
		} else {
			sDownloadView.setTextViewText(R.id.notification_desc_text, "Downloading snaps...");
		}
		sLastDownloadUpdateTime = System.currentTimeMillis();
	}
	
	/**
	 * Update the download notification with a progress amount. Use negative values for indeterminate.
	 * @param progress The percentage progress (out of a 100). Use < 0 for an indeterminate value.
	 */
	public static void updateDownloadNotification(int progress) {
		sDownloadBuilder.setTicker(null).setVibrate(null);
		if (Build.VERSION.SDK_INT >= 14) {
			if (progress > 0) {
				sDownloadBuilder.setProgress(100, progress / (sCurrentDownload + 1), false);
			} else {
				sDownloadBuilder.setProgress(100, 0, true);
			}
		}
		Notification not = sDownloadBuilder.build();
		if (Build.VERSION.SDK_INT < 14) {
			if (progress > 0) {
				sDownloadView.setProgressBar(R.id.notification_progress_bar, 100, progress / (sCurrentDownload + 1), false);
			} else {
				sDownloadView.setProgressBar(R.id.notification_progress_bar, 100, 0, true);
			}
			not.contentView = sDownloadView;
		}
		if (System.currentTimeMillis() - sLastDownloadUpdateTime > 500) {
			sNotifyManager.notify(DOWNLOAD_NOTIFICATION_ID, not);
			sLastDownloadUpdateTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * Update the download notification to show an error. 
	 */
	public static void updateDownloadNotificationWithError() {
		sDownloadBuilder.setOngoing(false).setContent(null).setContentTitle("OpenSnap").setContentText("Download error!")
		.setTicker("Download error!").setAutoCancel(true).setVibrate(new long[] { 0, 250, 200, 250 });
		sNotifyManager.notify(DOWNLOAD_NOTIFICATION_ID, sDownloadBuilder.build());
		//TODO: Perhaps change this later if 1 thing has error and one doesn't
	}
	
	/**
	 * Update the download notification to show that a download has finished. Should be called on every download, not just the final 
	 * one. If all downloads are finished the notification will change from an ongoing notification and can be removed.
	 */
	public static void updateDownloadNotificationWithFinish() {
		if (++sCurrentDownload == sActiveDownloads) {
			sDownloadBuilder.setOngoing(false).setContent(null).setContentTitle("OpenSnap").setAutoCancel(true).setVibrate(new long[] { 0, 400 });
			if (sActiveDownloads > 1) {
				sDownloadBuilder.setTicker("All downloads complete!").setContentText("All downloads complete!");
			} else {
				sDownloadBuilder.setTicker("Download complete!").setContentText("Download complete!");
			}
			sNotifyManager.notify(DOWNLOAD_NOTIFICATION_ID, sDownloadBuilder.build());
			sActiveDownloads = 0;
			sCurrentDownload = 0;
		} else {
			// TODO: Fix later and upload version
			updateDownloadNotification(100);
		}
	}

	/**
	 * Setup a new upload notification. This should be called whenever starting a new upload. (Even before it physically starts
	 * downloading)
	 * @param ctxt A context
	 */
	public static void setupUploadNotification(Context ctxt) {
		if (sUploadView == null) {
			sUploadView = new RemoteViews(ctxt.getPackageName(), R.layout.legacy_progress_notification);
			sUploadView.setImageViewResource(R.id.notification_left_image, R.drawable.ghost);
			sUploadView.setTextViewText(R.id.notification_title_text, "OpenSnap");
		}
        if (sActiveUploads++ == 0) {
            sUploadView.setTextViewText(R.id.notification_desc_text, "Uploading snap...");
            sUploadView.setProgressBar(R.id.notification_progress_bar, 100, 0, false);
        } else {
            sUploadView.setTextViewText(R.id.notification_desc_text, "Uploading snaps...");
        }

        sUploadBuilder.setOngoing(true).setTicker("Uploading snap...").setSmallIcon(R.drawable.ghost).setProgress(0, 100, false)
                .setContentIntent(PendingIntent.getActivity(ctxt, 0, new Intent(), Intent.FLAG_ACTIVITY_NEW_TASK)).setVibrate(null);
        Notification not = sUploadBuilder.build();
        if (Build.VERSION.SDK_INT < 14) {
            not.contentView = sUploadView;
        }
        sNotifyManager.notify(UPLOAD_NOTIFICATION_ID, not);
		sLastUploadUpdateTime = System.currentTimeMillis();
	}
	
	/**
	 * Update the upload notification with a progress amount. Use negative values for indeterminate.
	 * @param progress The percentage progress (out of a 100). Use < 0 for an indeterminate value.
	 */
	public static void updateUploadNotification(int progress) {	
		sUploadBuilder.setTicker(null).setVibrate(null);
		if (Build.VERSION.SDK_INT >= 14) {
			if (progress > 0) {
				sUploadBuilder.setProgress(100, progress / (sCurrentUpload + 1), false).setVibrate(null);
			} else {
				sUploadBuilder.setProgress(100, 0, true).setVibrate(null);
			}
		}
		Notification not = sUploadBuilder.build();
		if (Build.VERSION.SDK_INT < 14) {
			if (progress > 0) {
				sUploadView.setProgressBar(R.id.notification_progress_bar, 100, progress / (sCurrentUpload + 1), false);
			} else {
				sUploadView.setProgressBar(R.id.notification_progress_bar, 100, 0, true);
			}
			not.contentView = sUploadView;
		}
		if (System.currentTimeMillis() - sLastUploadUpdateTime > 200) {
			sNotifyManager.notify(UPLOAD_NOTIFICATION_ID, not);
			sLastUploadUpdateTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * Update the upload notification to show an error. 
	 */
	public static void updateUploadNotificationWithError() {
		sUploadBuilder.setOngoing(false).setContent(null).setContentTitle("OpenSnap").setContentText("Upload error!")
		.setTicker("Upload error!").setAutoCancel(true).setVibrate(new long[] { 0, 250, 200, 250 });
		sNotifyManager.notify(UPLOAD_NOTIFICATION_ID, sUploadBuilder.build());
	}
	
	/**
	 * Update the upload notification to show that an upload has finished. Should be called on every upload, not just the final 
	 * one. If all uploads are finished the notification will change from an ongoing notification and can be removed.
	 */
	public static void updateUploadNotificationWithFinish() {
		if (++sCurrentUpload == sActiveUploads) {
			sUploadBuilder.setOngoing(false).setContent(null).setContentTitle("OpenSnap").setAutoCancel(true).setVibrate(new long[] { 0, 400 });
			if (sActiveUploads > 1) {
				sUploadBuilder.setTicker("All uploads complete!").setContentText("All uploads complete!");
			} else {
				sUploadBuilder.setTicker("Upload complete!").setContentText("Upload complete!");
			}
			sNotifyManager.notify(UPLOAD_NOTIFICATION_ID, sUploadBuilder.build());
			sActiveUploads = 0;
			sCurrentUpload = 0;
		} else {
			// TODO: Fix later
			updateUploadNotification(100);
		}
	}
}
