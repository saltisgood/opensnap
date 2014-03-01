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

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.opensnap.global.LocalSnaps.MediaType;
import com.nickstephen.opensnap.util.tasks.SnapUpload;

public final class TempSnaps {
	private static final String TEMPSNAPS_FILENAME = "tempsnaps.bin";
	
	private static final long TSNAPS_CURRENT_HEADER_VER = 0x4E53303154533031L; //NS01TS01
	
	private static TempSnaps sInstance;

    public static TempSnaps getInstanceUnsafe() {
        return sInstance;
    }

    public static boolean checkInit() {
        return sInstance != null;
    }
	
	public static void init(Context ctxt) {
		if (checkInit()) {
			return;
		}
		
		try {
			sInstance = new TempSnaps(ctxt);
		} catch (FileNotFoundException e) {
			sInstance = new TempSnaps();
		} catch (Exception e) {
            Twig.printStackTrace(e);
            sInstance = new TempSnaps();
		}
	}

	/**
	 * Remove all temporary snaps
	 * @param ctxt A context to use to update the file
	 */
	public static void resetHard(Context ctxt) {
		sInstance = new TempSnaps();

		sInstance.write(ctxt);
	}
	
	private List<TempSnap> mSnaps;
	
	private TempSnaps() {
		mSnaps = new ArrayList<TempSnap>();
	}

	private TempSnaps(Context ctxt) throws FileNotFoundException, Exception {
		FileInputStream fs = ctxt.openFileInput(GlobalVars.getUsername(ctxt) + "-" + TEMPSNAPS_FILENAME);
		
		mSnaps = new ArrayList<TempSnap>();
		
		
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
			mSnaps.add(new TempSnap(fs));
		}
		
		try {
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public TempSnap add() {
        TempSnap snap = new TempSnap();
        mSnaps.add(0, snap);
        return snap;
    }

    public TempSnap get(int location) {
        if (location > mSnaps.size() || location < 0) {
            return null;
        }
        return mSnaps.get(location);
    }

    public String getId(int position) {
        TempSnap ts = mSnaps.get(position);
        Long sts = ts.sentTimeStamp;
        String usrs = ts.users;
        return usrs + "-" + sts.toString();
    }

    public int getCount() {
        return mSnaps.size();
    }

    public String getFilePath(int position) {
        return mSnaps.get(position).filePath;
    }

    public boolean getHide(int position) {
        return mSnaps.get(position).hide;
    }

    public String getReadableSentTimeStamp(int position) {
        String ts = (String) DateUtils.getRelativeTimeSpanString(mSnaps.get(position).sentTimeStamp, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS);
        if (ts.compareTo("0 minutes ago") == 0) {
            ts = "moments ago";
        } else if (ts.startsWith("in"))
            ts = "moments ago";
        return ts;
    }

    public int getUploadPercent(int position) {
        return mSnaps.get(position).uploadPercent;
    }

    public String getUsers(int position) {
        return mSnaps.get(position).users;
    }

    public boolean isError(int position) {
        return mSnaps.get(position).error;
    }

    public boolean isPhoto(int position) {
        return mSnaps.get(position).mediaType == LocalSnaps.MediaType.PHOTO;
    }

    public boolean isSending(int position) {
        return mSnaps.get(position).isSending;
    }

    public boolean isSent(int position) {
        return mSnaps.get(position).sent;
    }

    public void remove(Context context, TempSnap snap) {
        mSnaps.remove(snap);

        write(context);
    }

    /**
     * Remove any temporary snaps that have been sent with no errors
     * @param ctxt A context to use to update the file
     */
    public void resetLite(Context ctxt) {
        for (int i = 0; i < mSnaps.size(); i++) {
            if (mSnaps.get(i).sent) {
                mSnaps.remove(i);
            }
        }

        write(ctxt);
    }

    /**
     * Convenience method for starting a snap upload
     * @param context
     * @param position
     * @param params
     */
    public void sendSnap(Context context, int position, String... params) {
        mSnaps.get(position).send(context, params);
    }

    public void setIsError(int position, boolean err) {
        mSnaps.get(position).error = err;
    }

    public void setIsSending(int position, boolean isSending) {
        mSnaps.get(position).isSending = isSending;
    }

    public void setHide(int position, boolean hide) {
        mSnaps.get(position).hide = hide;
    }

    public void setShouldUpdateGui(int position, boolean update) {
        mSnaps.get(position).update = update;
    }

    public void setUploadPercent(int position, int percent) {
        mSnaps.get(position).uploadPercent = percent;
    }

    public boolean shouldUpdateGui(int position) {
        return mSnaps.get(position).update;
    }

    public void write(Context ctxt) {
        FileOutputStream fs;
        try {
            fs = ctxt.openFileOutput(GlobalVars.getUsername(ctxt) + "-" + TEMPSNAPS_FILENAME, Context.MODE_PRIVATE);

            fs.write(BitConverter.getBytes(TSNAPS_CURRENT_HEADER_VER));
            fs.write(BitConverter.getBytes(mSnaps.size()));

            for (TempSnap snap : mSnaps) {
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

            fs.close();
        } catch (FileNotFoundException e) {
            Twig.printStackTrace(e);
        } catch (IOException e) {
            Twig.printStackTrace(e);
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
