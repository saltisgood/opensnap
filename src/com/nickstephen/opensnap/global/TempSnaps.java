package com.nickstephen.opensnap.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.text.format.DateUtils;

import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.opensnap.global.LocalSnaps.MediaType;
import com.nickstephen.opensnap.util.tasks.SnapUpload;

public final class TempSnaps {
	private static final String TEMPSNAPS_FILENAME = "tempsnaps.bin";
	
	private static final long TSNAPS_CURRENT_HEADER_VER = 0x4E53303154533031L; //NS01TS01
	
	private static TempSnaps sThis;
	
	public static void init(Context ctxt) {
		if (sThis != null) {
			return;
		}
		
		try {
			sThis = new TempSnaps(ctxt);
		} catch (FileNotFoundException e) {
			sThis = new TempSnaps();
		} catch (Exception e) {
			e.printStackTrace();
			sThis = new TempSnaps();
		}
	}
	
	public static void write(Context ctxt) {
		if (sThis == null) {
			return;
		}
		
		FileOutputStream fs = null;
		try {
			fs = ctxt.openFileOutput(GlobalVars.getUsername(ctxt) + "-" + TEMPSNAPS_FILENAME, Context.MODE_PRIVATE);
			
			fs.write(BitConverter.getBytes(TSNAPS_CURRENT_HEADER_VER));
			fs.write(BitConverter.getBytes(sThis.Snaps.size()));
			
			for (TempSnap snap : sThis.Snaps) {
				if (snap.users == null) {
					fs.write(BitConverter.getBytes(0));
				} else {
					fs.write(BitConverter.getBytes(snap.users.length()));
					fs.write(BitConverter.getBytes(snap.users));
				}
				
				fs.write(BitConverter.getBytes(snap.sentTimeStamp));
				fs.write(BitConverter.getBytes(LocalSnaps.MediaType.getValue(snap.mediaType)));
				fs.write(BitConverter.getBytes(snap.sent));
				
				if (snap.filePath == null) {
					fs.write(BitConverter.getBytes(0));
				} else {
					fs.write(BitConverter.getBytes(snap.filePath.length()));
					fs.write(BitConverter.getBytes(snap.filePath));
				}
				
				if (snap.vidCaption == null) {
					fs.write(BitConverter.getBytes(0));
				} else {
					fs.write(BitConverter.getBytes(snap.vidCaption.length()));
					fs.write(BitConverter.getBytes(snap.vidCaption));
				}
				
				fs.write(BitConverter.getBytes(snap.captionOrientation));
				fs.write(BitConverter.getBytes(snap.captionPosition));
				fs.write(BitConverter.getBytes(snap.captionTime));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static TempSnap add() {
		TempSnap snap = new TempSnap();
		//sThis.Snaps.add(snap);
		sThis.Snaps.add(0, snap);
		return snap;
	}
	
	public static int getCount() {
		return sThis.Snaps.size();
	}
	
	public static void setUploadPercent(int position, int percent) {
		sThis.Snaps.get(position).uploadPercent = percent;
	}
	
	public static boolean isPhoto(int position) {
		return sThis.Snaps.get(position).mediaType == LocalSnaps.MediaType.PHOTO;
	}
	
	public static String getUsers(int position) {
		return sThis.Snaps.get(position).users;
	}
	
	public static String getReadableSentTimeStamp(int position) {
		String ts = (String) DateUtils.getRelativeTimeSpanString(sThis.Snaps.get(position).sentTimeStamp, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS);
		if (ts.compareTo("0 minutes ago") == 0) {
			ts = "moments ago";
		} else if (ts.startsWith("in"))
			ts = "moments ago";
		return ts;
	}
	
	public static boolean isSent(int position) {
		return sThis.Snaps.get(position).sent;
	}
	
	public static boolean isError(int position) {
		return sThis.Snaps.get(position).error;
	}
	
	public static boolean shouldUpdateGui(int position) {
		return sThis.Snaps.get(position).update;
	}
	
	public static void setShouldUpdateGui(int position, boolean update) {
		sThis.Snaps.get(position).update = update;
	}
	
	public static int getUploadPercent(int position) {
		return sThis.Snaps.get(position).uploadPercent;
	}
	
	public static boolean isSending(int position) {
		return sThis.Snaps.get(position).isSending;
	}
	
	public static void setIsSending(int position, boolean isSending) {
		sThis.Snaps.get(position).isSending = isSending;
	}
	
	public static void setHide(int position, boolean hide) {
		sThis.Snaps.get(position).hide = hide;
	}
	
	public static void remove(Context ctxt, int position) {
		sThis.Snaps.remove(position);
		
		write(ctxt);
	}
	
	public static void remove(Context ctxt, TempSnap snap) {
		sThis.Snaps.remove(snap);
		
		write(ctxt);
	}
	
	public static boolean getHide(int position) {
		return sThis.Snaps.get(position).hide;
	}
	
	public static String getId(int position) {
		TempSnap ts = sThis.Snaps.get(position);
		Long sts = ts.sentTimeStamp;
		String usrs = ts.users;
		return usrs + "-" + sts.toString();
	}
	
	public static TempSnap get(int location) {
		if (location > sThis.Snaps.size() || location < 0) {
			return null;
		}
		return sThis.Snaps.get(location);
	}
	
	/**
	 * Remove any temporary snaps that have been sent with no errors
	 * @param ctxt A context to use to update the file
	 */
	public static void resetLite(Context ctxt) {
		for (int i = 0; i < sThis.Snaps.size(); i++) {
			if (sThis.Snaps.get(i).sent) {
				sThis.Snaps.remove(i);
			}
		}
		
		write(ctxt);
	}
	
	/**
	 * Remove all temporary snaps
	 * @param ctxt A context to use to update the file
	 */
	public static void resetHard(Context ctxt) {
		sThis = new TempSnaps();
		
		write(ctxt);
	}
	
	/**
	 * Convenience method for starting a snap upload 
	 * @param context
	 * @param position
	 * @param params
	 */
	public static void sendSnap(Context context, int position, String... params) {
		sThis.Snaps.get(position).send(context, params);
	}
	
	public static String getFilePath(int position) {
		return sThis.Snaps.get(position).filePath;
	}
	
	public static void setIsError(int position, boolean err) {
		sThis.Snaps.get(position).error = err;
	}
	
	private List<TempSnap> Snaps;
	
	private TempSnaps() {
		Snaps = new ArrayList<TempSnap>();
	}

	private TempSnaps(Context ctxt) throws FileNotFoundException, Exception {
		FileInputStream fs = ctxt.openFileInput(GlobalVars.getUsername(ctxt) + "-" + TEMPSNAPS_FILENAME);
		
		Snaps = new ArrayList<TempSnap>();
		
		
		try {
			byte[] buff = new byte[8];
			int len = fs.read(buff);
			if (len != 8) {
				throw new IOException("Incorrect number of bytes read");
			}
			long ver = BitConverter.toInt64(buff, 0);
			if (ver != TSNAPS_CURRENT_HEADER_VER) {
				throw new RuntimeException("Incorrect header version: " + ver);
			}
		} catch (IOException e) {
			throw e;
		}
		
		byte[] buff = new byte[4];
		
		int len;
		try {
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect number of bytes read");
			}
			len = BitConverter.toInt32(buff, 0);
		} catch (IOException e) {
			throw e;
		}
		
		for (int i = 0; i < len; i++) {
			Snaps.add(new TempSnap(fs));
		}
		
		try {
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static final class TempSnap {
		private String users;
		private long sentTimeStamp;
		private LocalSnaps.MediaType mediaType;
		private boolean sent = false;
		private boolean isSending = false;
		private boolean error = false;
		private String filePath;
		private int captionTime;
		
		private String vidCaption;
		private int captionOrientation;
		private float captionPosition;
		
		private int uploadPercent = 0;
		private boolean update = true;
		private boolean hide = false;
		
		public TempSnap(FileInputStream fs) throws IOException, Exception {
			byte[] buff = new byte[4];
			int len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length) {
				throw new IOException("Incorrect read number");
			}
			users = new String(buff);
			
			buff = new byte[8];
			len = fs.read(buff);
			if (len != 8) {
				throw new IOException("Incorrect read number");
			}
			sentTimeStamp = BitConverter.toInt64(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			mediaType = LocalSnaps.MediaType.getType(BitConverter.toInt32(buff, 0));
			
			buff = new byte[1];
			len = fs.read(buff);
			if (len != 1) {
				throw new IOException("Incorrect read number");
			}
			sent = BitConverter.toBoolean(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length) {
				throw new IOException("Incorrect read number");
			}
			filePath = new String(buff);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length) {
				throw new IOException("Incorrect read number");
			}
			vidCaption = new String(buff);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			captionOrientation = BitConverter.toInt32(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			captionPosition = BitConverter.toSingle(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Incorrect read number");
			}
			captionTime = BitConverter.toInt32(buff, 0);
		}
	
		public TempSnap() {
			sentTimeStamp = System.currentTimeMillis();
		}
		
		public TempSnap setUsers(String usernames) {
			users = usernames;
			return this;
		}
		
		public TempSnap setSent(boolean s) {
			sent = s;
			return this;
		}
		
		public TempSnap setFilePath(String fp) {
			filePath = fp;
			return this;
		}
		
		public TempSnap setMediaType(LocalSnaps.MediaType mt) {
			mediaType = mt;
			return this;
		}
		
		public TempSnap setVideoCaption(String caption) {
			vidCaption = caption;
			return this;
		}
		
		public TempSnap setUploadPercent(int percentage) {
			uploadPercent = percentage;
			return this;
		}
		
		public TempSnap setError(boolean e) {
			error = e;
            isSending = false;
			return this;
		}
		
		public String getUsers() {
			return users;
		}
		
		public long getSentTimeStamp() {
			return sentTimeStamp;
		}
		
		public LocalSnaps.MediaType getMediaType() {
			return mediaType;
		}
		
		public boolean isPhoto() {
			return mediaType == MediaType.PHOTO;
		}

        public boolean isVideo() {
            return mediaType == MediaType.VIDEO || mediaType == MediaType.VIDEO_NOAUDIO;
        }
		
		public boolean isSent() {
			return sent;
		}
		
		public boolean isSending() {
			return isSending;
		}
		
		public String getFilePath() {
			return filePath;
		}
		
		public String getVideoCaption() {
			return vidCaption;
		}
		
		public int getUploadPercent() {
			return uploadPercent;
		}
	
		public boolean isError() {
			return error;
		}
		
		public TempSnap setIsSending(boolean sending) {
			isSending = sending;
			return this;
		}
		
		public TempSnap setCaptionPosition(float position) {
			captionPosition = position;
			return this;
		}
		
		public float getCaptionPosition() {
			return captionPosition;
		}
		
		public TempSnap setCaptionOrientation(int ori) {
			captionOrientation = ori;
			return this;
		}
		
		public int getCaptionOrientation() {
			return captionOrientation;
		}
		
		public void send(Context context, String... params) {
			isSending = true;
			uploadPercent = 0;
			update = true;
			error = false;
			
			new SnapUpload(context, this).execute(params);
		}
		
		public String getId() {
			return users + "-" + sentTimeStamp;
		}
		
		public String getReadableSentTimeStamp() {
			String ts = (String) DateUtils.getRelativeTimeSpanString(sentTimeStamp, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS);
			if (ts.compareTo("0 minutes ago") == 0 || ts.startsWith("in")) {
				ts = "moments ago";
			}
			return ts;
		}
		
		public TempSnap setCaptionTime(int time) {
			captionTime = time;
			return this;
		}
		
		public int getCaptionTime() {
			return captionTime;
		}
		
		public TempSnap setTimeStamp(long ts) {
			sentTimeStamp = ts;
			return this;
		}
	}
}
