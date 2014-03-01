package com.nickstephen.opensnap.global;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateUtils;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerSnap;
import com.nickstephen.opensnap.util.misc.CameraUtil;
import com.nickstephen.opensnap.util.misc.CustomJSON;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;

/**
 * The class that holds the data on all the mSnaps. Everything is controlled statically
 * so there should be no issues with variables going missing from memory and whatnot.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class LocalSnaps {
	/**
	 * The key for the snap data in the SnapChat JSON file
	 */
	private static final String SNAP_KEY = "snaps";
	/**
	 * The type that snaps are in the CustomJSON.JSONNode
	 */
	private static final String SNAP_TYPE = "List<JSONNode>";
	private static final String EMPTY_SNAP_TYPE = "List<String>";
	/**
	 * The filename of the config file that holds all the snaps data
	 */
	private static final String SNAPS_FILENAME = "snaps.bin";
	
	private static final long SNAPS_CURRENT_HEADER_VER = 0x4E533031534E3031L; //NS01SN01
	
	/**
	 * The private static instance of this class
	 * (Yeah I know it's a conflict of terms but THAT'S WHAT IT IS)
	 */
	private static LocalSnaps sInstance;

    public static LocalSnaps getInstanceUnsafe() {
        return sInstance;
    }

    public static void getInstanceSafe(IOnObjectReady<LocalSnaps> waiter) {
        Broadcast.waitForSnaps(waiter);
    }
	
	/**
	 * Check whether the static instance {@link #sInstance} has been initialised
	 * @return True if it is instantiated, false otherwise
	 */
	public static boolean checkInit() {
		return sInstance != null;
	}
	
	/**
	 * Initialise the SnapData. Should always be called before using the LocalSnap
	 * data.
	 * @param ctxt A context
	 * @return True on a successful initialisation, false if an error occurred.
	 */
	public static boolean init(Context ctxt) {
		if (sInstance != null)
			return true;
		
		try {
			sInstance = new LocalSnaps(ctxt);
		} catch (FileNotFoundException e) {
			sInstance = new LocalSnaps();
		} catch (Exception e) {
			e.printStackTrace();
			sInstance = new LocalSnaps();
			return false;
		}
		return true;
	}
	
	public static void reset(Context ctxt) {
		sInstance = new LocalSnaps();
		try {
			sInstance.serialiseToFile(ctxt);
		} catch (IOException e) {
            Twig.printStackTrace(e);
        }
	}
	
	/**
	 * The local list of mSnaps. Not accessible from outside the class.
	 */
	private List<LocalSnap> mSnaps;
	
	/**
	 * The simplest constructor. Just instantiates the mSnaps list. I think this only
	 * occurs when you generate the LocalSnaps for the first time.
	 */
	private LocalSnaps() {
		mSnaps = new ArrayList<LocalSnap>();
	}
	
	/**
	 * The constructor used when reading from the config file. This is the primary
	 * constructor for when starting the program. 
	 * @param ctxt A context
	 * @throws FileNotFoundException Rethrown
	 * @throws Exception Rethrown
	 * @see {@link #readFromFile(Context)}
	 */
	private LocalSnaps(Context ctxt) throws Exception {
		readFromFile(ctxt);
	}
	
	/**
	 * Constructor when syncing from the SnapChat server's JSON
	 * @param json The JSON to sync with
	 * @throws JSONException Missing snap keys, etc
	 */
	public LocalSnaps(CustomJSON json) throws JSONException {
		if (!json.CheckKeyExists(SNAP_KEY)) {
			throw new JSONException("Snaps data missing in JSON");
		} else if (json.GetType(SNAP_KEY).compareTo(EMPTY_SNAP_TYPE) == 0) {
			mSnaps = new ArrayList<LocalSnap>();
			return;
		} else if (json.GetType(SNAP_KEY).compareTo(SNAP_TYPE) != 0) {
			throw new JSONException("Invalid snap data type in JSON");
		}
		
		@SuppressWarnings("unchecked")
		List<CustomJSON.JSONNode> nodes = (List<CustomJSON.JSONNode>)json.GetValue(SNAP_KEY);
		mSnaps = new ArrayList<LocalSnap>();
		for (CustomJSON.JSONNode node : nodes) {
			try {
				mSnaps.add(new LocalSnap(node));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    public LocalSnaps(List<ServerSnap> snaps) {
        mSnaps = new ArrayList<LocalSnap>();
        for (ServerSnap snap : snaps) {
            mSnaps.add(new LocalSnap(snap));
        }
    }

    /**
     * Get a list of snaps from a certain contact
     * @param username The username to search for
     * @return A list of snaps
     */
    public List<LocalSnaps.LocalSnap> getContactSnaps(String username) {
        List<LocalSnaps.LocalSnap> snaps = new ArrayList<LocalSnaps.LocalSnap>();
        for (LocalSnaps.LocalSnap snap : mSnaps) {
            if ((snap.IO == IOType.SENT && snap.Recipient.compareTo(username) == 0) ||
                    (snap.IO == IOType.RECEIVED && snap.Sender.compareTo(username) == 0)) {
                snaps.add(snap);
            }
        }
        return snaps;
    }
	
	/**
	* Get the time for the snap to be displayed at a certain position in the list.
	* @param position The position of the snap in the list
	* @return The time for the snap to be displayed in seconds
	*/
	public int getDisplayTime(int position) {
		return mSnaps.get(position).getDisplayTime();
	}

    public int getNumberOfSnaps() {
        return mSnaps.size();
    }

    public LocalSnap getSnapAt(int position) {
        return mSnaps.get(position);
    }
	
	/**
	 * Get the snap id
	 * @param position The position of the snap in the list
	 * @return The snap id
	 */
	public String getSnapID(int position) {
		return mSnaps.get(position).getSnapId();
	}

    public int getUnseenSnaps() {
        int count = 0;

        for (LocalSnap snap : mSnaps) {
            if (snap.inCloud && snap.IO == IOType.RECEIVED && (snap.State == SnapStatus.DELIVERED || snap.State == SnapStatus.SENT)) {
                count++;
            }
        }

        return count;
    }
	
	/**
	 * Get whether the snap has a display time
	 * @param position The position of the snap in the list
	 * @return True if the snap has a display time
	 */
	public boolean hasDisplayTime(int position) {
		return mSnaps.get(position).hasDisplayTime();
	}
	
	/**
	 * Called from the file constructor ({@link #LocalSnaps(Context)}). Reads from the
	 * default config file.
	 * @param ctxt A context
	 * @throws FileNotFoundException Thrown if the config file doesn't exist
	 * @throws Exception IOExceptions or BitConverter exceptions
	 */
	private void readFromFile(Context ctxt) throws Exception {
		FileInputStream fs = ctxt.openFileInput(GlobalVars.getUsername(ctxt) + "-" + SNAPS_FILENAME);
		
		mSnaps = new ArrayList<LocalSnap>();
		
		byte[] buff = new byte[8];
		int len = fs.read(buff);
		if (len != 8) {
			throw new IOException("Incorrect read number");
		}
		long ver = BitConverter.toInt64(buff, 0);
		if (ver != SNAPS_CURRENT_HEADER_VER) {
			throw new RuntimeException("Incorrect header version: " + ver);
		}
		
		buff = new byte[4];
		len = fs.read(buff);
		if (len != 4)
			throw new IOException("Incorrect read number");
		len = BitConverter.toInt32(buff, 0);
		for (int i = 0; i < len; i++) {
			mSnaps.add(new LocalSnap(fs));
		}
		fs.close();
	}

	/**
	 * Write the list of snaps to file
	 * @param ctxt A context
	 * @throws IOException A rethrown exception
	 */
	public void serialiseToFile(Context ctxt) throws IOException {
		FileOutputStream fs = ctxt.openFileOutput(GlobalVars.getUsername(ctxt) + "-" + SNAPS_FILENAME, Context.MODE_PRIVATE);
		
		if (!checkInit()) {
			fs.write(BitConverter.getBytes(SNAPS_CURRENT_HEADER_VER));
			fs.write(BitConverter.getBytes(0));
		} else {
			fs.write(BitConverter.getBytes(SNAPS_CURRENT_HEADER_VER));
			fs.write(BitConverter.getBytes(mSnaps.size()));
			for (LocalSnap snap : mSnaps) {
				try {
					snap.serialise(fs);
				} catch (Exception e) {
					fs.close();
					e.printStackTrace();
					throw new IOException("Rethrown exception");
				}
			}
		}
		fs.close();
	}

    /**
     * Check whether the cloud snap feed should be cleared. This will return true if the first n
     * snaps have been opened and won't be affected by updates anymore.
     * @return True if the cloud should be cleared, false otherwise
     */
    public boolean shouldClear() {
        if (mSnaps.size() == 0) {
            return false;
        }

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < mSnaps.size(); i++) {
            LocalSnap snap;
            if (!(snap = mSnaps.get(i)).inCloud || snap.State != SnapStatus.OPENED) {
                return false;
            }
        }

        return true;
    }
	
	/**
	 * Check whether a given snap ID exists in the list of snaps
	 * @param snapID The ID of the requested snap
	 * @return -1 if the ID isn't found, otherwise the position in the list
	 */
	public int snapIDExists(String snapID) {
		for (int i = 0; i < mSnaps.size(); i++) {
			if (snapID.compareTo(mSnaps.get(i).SnapID) == 0) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Sort the {@link #mSnaps} list by their sent time stamps
	 */
	private void sort() {
		LocalSnap[] tmpSp = mSnaps.toArray(new LocalSnap[mSnaps.size()]);
		Arrays.sort(tmpSp, new Comparator<LocalSnap>() {
            @Override
            public int compare(LocalSnap lhs, LocalSnap rhs) {
                if (rhs.getSentTimeStamp() == lhs.getSentTimeStamp())
                    return 0;
                else if (rhs.getSentTimeStamp() > lhs.getSentTimeStamp())
                    return 1;
                else
                    return -1;
            }
        });
		
		mSnaps = new ArrayList<LocalSnap>();
        Collections.addAll(mSnaps, tmpSp);
	}

    /**
     * Synchronise the local copy of snaps information with the version from the SnapChat servers.
     * Note that this doesn't delete items that are present locally but not remotely.
     * @param snaps The set of mSnaps from the remote server
     * @return The number of new snaps
     */
    public int sync(LocalSnaps snaps) {
        for (LocalSnap snap : mSnaps) {
            snap.inCloud = false;
        }

        int newsnaps = 0;
        for (LocalSnaps.LocalSnap snap : snaps.mSnaps) {
            int pos;
            if ((pos = snapIDExists(snap.SnapID)) != -1) {
                update(pos, snap);
            } else {
                snap.inCloud = true;
                mSnaps.add(snap);
                if (!snap.getSent() && snap.getSnapAvailable())
                    newsnaps++;
            }
        }

        sort();
        return newsnaps;
    }
	
	/**
	 * Update a particular snap with another {@link LocalSnap} at a certain position
	 * @param position The position of the snap in the list
	 * @param snap The snap to update with
	 */
	private void update(int position, LocalSnap snap) {
		mSnaps.get(position).update(snap);
	}
	
	/**
	 * A simple enum abstraction of whether a snap was sent by the user or received.
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public enum IOType {
		RECEIVED, SENT;
		
		/**
		 * Get the enum value from an integer representation
		 * @param val The integer value
		 * @return The corresponding enum
		 */
		public static IOType getType(int val) {
			switch (val) {
				case 0:
					return SENT;
				case 1:
					return RECEIVED;
				default:
					return null;
			}
		}
		
		/**
		 * Get the integer value of the enum
		 * @param val The enum to be converted
		 * @return The integer representation
		 */
		public static int getValue(IOType val) {
			switch (val) {
				case SENT:
					return 0;
				case RECEIVED:
					return 1;
				default:
					return -1;
			}
		}
	}
	
	/**
	 * A class representing one Snap object. Doesn't require a reference to the 
	 * outer class ({@link LocalSnaps})
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public static class LocalSnap {
		/**
		 * A key that is used to identify the caption mCameraOrientation from a SnapChat JSON
		 */
		private static final String CAPTION_ORI_KEY = "cap_ori";
		/**
		 * A key that is used to identify the caption position from a SnapChat JSON
		 */
		private static final String CAPTION_POS_KEY = "cap_pos";
		/**
		 * A key that is used to identify the caption text from a SnapChat JSON
		 */
		private static final String CAPTION_TEXT_KEY = "cap_text";
		/**
		 * A key that is used to identify the convo id from a SnapChat JSON
		 */
		private static final String CONVID_KEY= "c_id";
		/**
		 * A key that is used to identify the display time from a SnapChat JSON
		 */
		private static final String DISPLAY_TIME_KEY = "t";
		/**
		 * A value used to check for the float type when querying a CustomJSON value type
		 */
		private static final String FLOAT_TYPE = "Float";
		/**
		 * A key that is used to identify the snap id from a SnapChat JSON
		 */
		private static final String ID_KEY = "id";
		/**
		 * A key that is used to identify the media type from a SnapChat JSON
		 */
		private static final String MEDIA_KEY = "m";
		/**
		 * A key that is used to identify the recipient from a SnapChat JSON
		 */
		private static final String RECIP_KEY = "rp";
		/**
		 * A key that is used to identify the sending timestamp from a SnapChat JSON
		 */
		private static final String SEND_TIMESTAMP_KEY = "sts";
		/**
		 * A key that is used to identify the sender from a SnapChat JSON
		 */
		private static final String SENDER_KEY = "sn";
		/**
		 * A key that is used to identify the media state from a SnapChat JSON
		 */
		private static final String STATE_KEY = "st";
		/**
		 * A value used to check for the string type when querying a CustomJSON value type
		 */
		private static final String STR_TYPE = "String";
		/**
		 * A key that is used to identify the timestamp from a SnapChat JSON
		 */
		private static final String TIMESTAMP_KEY = "ts";
		
		/**
		 * The location of the caption to be displayed over the snap
		 */
		private float captionLocation; 
		/**
		 * The mCameraOrientation of the caption to be displayed over the snap
		 */
		private int captionOrientation;
		/**
		 * The caption to be displayed over the snap (normally a video)
		 */
		private String captionText;
		/**
		 * The string convo id. Mainly used for keeping track of the local filenames of snaps when
		 * they were sent.
		 */
		private String ConvID;
		/**
		 * The time for the snap to be displayed. If in Snap compat mode.
		 */
		private int DisplayTime;
		private boolean inCloud;
		/**
		 * The enum value of whether this snap was sent or received.
		 * @see {@link IOType}
		 */
		private IOType IO;
		private boolean isDownloading = false;
		private boolean isError = false;
		/**
		 * The enum value of the type of media this snap is. Is used for choosing file types and such.
		 * @see {@link MediaType}
		 */
		private MediaType Media;
		/**
		 * The percentage that a snap is at if it's currently downloading
		 */
		private int progressPercentage = 0;
		/**
		 * The reference to the receiver's contact details. If the snap was received then this is null.
		 */
		private Contacts.Contact RecipCon;
		/**
		 * The snap receiver's username. If the snap was received then this is null.
		 */
		private String Recipient;
		/**
		 * The snap sender's username. If the snap was sent then this is null.
		 */
		private String Sender;
		/**
		 * The reference to the sender's contact details. If the snap was sent then this is null.
		 */
		private Contacts.Contact SenderCon;
		
		/**
		 * The sent timestamp. This is when the SnapChat server received the send message. It represents
		 * the number of milliseconds since the epoch.
		 */
		private long SentTimeStamp;
		
		/**
		 * The ID of the snap. This is mainly used for correctly downloading a media file from
		 * the SnapChat servers.
		 */
		private String SnapID;
		
		/**
		 * The enum value of the status of the snap.
		 * @see {@link SnapStatus}
		 */
		private SnapStatus State;
		/**
		 * The primary timestamp. This is normally the open time of a snap. It represents the number
		 * of milliseconds since the epoch.
		 */
		private long TimeStamp;

        public LocalSnap(ServerSnap snap) {
            SnapID = snap.id;
            Sender = snap.sn;
            TimeStamp = snap.ts;
            SentTimeStamp = snap.sts;
            ConvID = snap.c_id;
            Recipient = snap.rp;
            DisplayTime = snap.t;
            captionText = snap.cap_text;
            captionOrientation = snap.cap_ori;
            captionLocation = snap.cap_pos;
            Media = MediaType.getType(snap.m);
            State = SnapStatus.getType(snap.st);

            if (Sender != null) {
                IO = IOType.RECEIVED;
                SenderCon = Contacts.getContactWithName(Sender);
            } else {
                IO = IOType.SENT;
                RecipCon = Contacts.getContactWithName(Recipient);
            }
        }
		
		/**
		 * Construct a Snap with a JSONNode. Normally used when syncing from the server.
		 * @param jnode Da node wid da info
		 * @throws JSONException On an invalid JSON
		 * @throws Exception Invalid enums, etc
		 */
		public LocalSnap(CustomJSON.JSONNode jnode) throws Exception {
			List<String> keys = jnode.GetKeys();
			for (String key : keys) {
				if (key.compareTo(ID_KEY) == 0) {
					if (jnode.GetType(key).compareTo(STR_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					SnapID = (String)jnode.GetValue(key); 
				} else if (key.compareTo(SENDER_KEY) == 0) {
					if (jnode.GetType(key).compareTo(STR_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					Sender = (String)jnode.GetValue(key);
					IO = IOType.RECEIVED;
				} else if (key.compareTo(TIMESTAMP_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					TimeStamp = ((Float)jnode.GetValue(key)).longValue();
				} else if (key.compareTo(SEND_TIMESTAMP_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					SentTimeStamp = ((Float)jnode.GetValue(key)).longValue();
				} else if (key.compareTo(CONVID_KEY) == 0) {
					if (jnode.GetType(key).compareTo(STR_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					ConvID = (String)jnode.GetValue(key);
				} else if (key.compareTo(MEDIA_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					int val = ((Float)jnode.GetValue(key)).intValue();
					switch (val) {
						case 0:
							Media = MediaType.PHOTO;
							break;
						case 1:
							Media = MediaType.VIDEO;
							break;
						case 2:
							Media = MediaType.VIDEO_NOAUDIO;
							break;
						case 3:
							Media = MediaType.FRIEND_REQ;
							break;
						case 4:
							Media = MediaType.FRIEND_REQ_IMAGE;
							break;
						case 5:
							Media = MediaType.FRIEND_REQ_VIDEO;
							break;
						case 6:
							Media = MediaType.FRIEND_REQ_VIDEO_NOAUDIO;
							break;
						default:
							throw new Exception("Invalid MEDIA enum: " + val);
					}
				} else if (key.compareTo(STATE_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					int val = ((Float)jnode.GetValue(key)).intValue();
					switch (val) {
						case 0:
							State = SnapStatus.SENT;
							break;
						case 1:
							State = SnapStatus.DELIVERED;
							break;
						case 2:
							State = SnapStatus.OPENED;
							break;
						case 3:
							State = SnapStatus.SCREENSHOT;
							break;
						default:
							throw new Exception("Invalid State enum: " + val);
					}
				} else if (key.compareTo(RECIP_KEY) == 0) {
					if (jnode.GetType(key).compareTo(STR_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					Recipient = (String)jnode.GetValue(key);
					IO = IOType.SENT;
				} else if (key.compareTo(DISPLAY_TIME_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0)
						throw new JSONException("Invalid JSON value type");
					DisplayTime = ((Float)jnode.GetValue(key)).intValue();
				} else if (key.compareTo(CAPTION_TEXT_KEY) == 0) {
					if (jnode.GetType(key).compareTo(STR_TYPE) != 0) {
						throw new JSONException("Invalid JSON value type");
					}
					captionText = (String)jnode.GetValue(key);
				} else if (key.compareTo(CAPTION_ORI_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0) {
						throw new JSONException("Invalid JSON value type");
					}
					captionOrientation = ((Float)jnode.GetValue(key)).intValue();
				} else if (key.compareTo(CAPTION_POS_KEY) == 0) {
					if (jnode.GetType(key).compareTo(FLOAT_TYPE) != 0) {
						throw new JSONException("Invalid JSON value type");
					}
					captionLocation = (Float)jnode.GetValue(key);
				}
			}
			
			if (getSent()) {
				RecipCon = Contacts.getContactWithName(Recipient);
			} else {
				SenderCon = Contacts.getContactWithName(Sender);
			}
			//SenderCon = Contacts.getContactWithName(Sender);
			//RecipCon = Contacts.getContactWithName(Recipient);
		}
	
		/**
		 * Construct a Snap from a local file. Normally used when initialising the LocalSnaps
		 * guff
		 * @param fs The stream to read from
		 * @throws Exception IOExceptions/Invalid enums/etc
		 */
		private LocalSnap(FileInputStream fs) throws Exception {
			byte[] buff = new byte[4];
			int len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length)
				throw new IOException("Invalid read number");
			SnapID = new String(buff);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			IO = IOType.getType(BitConverter.toInt32(buff, 0));
			if (IO == null)
				throw new Exception("Invalid IOType enum");
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length)
				throw new IOException("Invalid read number");
			if (getSent()) {
				Recipient = new String(buff);
				RecipCon = Contacts.getContactWithName(Recipient);
			} else {
				Sender = new String(buff);
				SenderCon = Contacts.getContactWithName(Sender);
			}
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			len = BitConverter.toInt32(buff, 0);
			if (len != 0) {
				buff = new byte[len];
				len = fs.read(buff);
				if (len != buff.length)
					throw new IOException("Invalid read number");
				ConvID = new String(buff);
			}
			
			buff = new byte[8];
			len = fs.read(buff);
			if (len != 8)
				throw new IOException("Invalid read number");
			TimeStamp = BitConverter.toInt64(buff, 0);
			
			buff = new byte[8];
			len = fs.read(buff);
			if (len != 8)
				throw new IOException("Invalid read number");
			SentTimeStamp = BitConverter.toInt64(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			DisplayTime = BitConverter.toInt32(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			Media = MediaType.getType(BitConverter.toInt32(buff, 0));
			if (Media == null)
				throw new Exception("Invalid MediaType enum");
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			State = SnapStatus.getType(BitConverter.toInt32(buff, 0));
			if (State == null)
				throw new Exception("Invalid SnapStatus enum");
			
			buff = new byte[1];
			len = fs.read(buff);
			if (len != 1) {
				throw new IOException("Invalid read number");
			}
			inCloud = BitConverter.toBoolean(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			len = BitConverter.toInt32(buff, 0);
			if (len != 0) {
				buff = new byte[len];
				len = fs.read(buff);
				if (len != buff.length)
					throw new IOException("Invalid read number");
				captionText = new String(buff);
			}
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Invalid read number");
			}
			captionOrientation = BitConverter.toInt32(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4) {
				throw new IOException("Invalid read number");
			}
			captionLocation = BitConverter.toSingle(buff, 0);
		}
		
		public String getCaption() {
			return captionText;
		}
		
		public float getCaptionLocation() {
			return captionLocation;
		}
		
		public int getCaptionOrientation() {
			return captionOrientation;
		}
		
		/**
		 * Get the time for the snap to be displayed. 
		 * @return The time for the snap to be displayed in seconds.
		 */
		public int getDisplayTime() {
			return DisplayTime;
		}
		
		public int getDownloadProgress() {
			return progressPercentage;
		}
		
		/**
		 * Get the display name of the snap recipient, or the username if that fails
		 * @return The display or username of the snap recipient
		 * @see {@link #getFriendlySenderName()}
		 */
		public String getFriendlyRecipName() {
			if (RecipCon != null && RecipCon.hasDisplay()) {
				return RecipCon.getDisplayName();
			} else {
				return Recipient;
			}
		}
		
		/**
		 * Get the display name of the snap sender, or the username if that fails
		 * @return The display or username of the sender
		 * @see {@link #getFriendlyRecipName()}
		 */
		public String getFriendlySenderName() {
			if (SenderCon != null && SenderCon.hasDisplay()) {
				return SenderCon.getDisplayName();
			} else {
				return Sender;
			}
		}
		
		/**
		 * Get a human readable version of the sent timestamp of this Snap
		 * @return A string version of the sent timestamp
		 */
		public String getReadableSentTimeStamp() {
			String ts = (String) DateUtils.getRelativeTimeSpanString(getSentTimeStamp(), new Date().getTime(), DateUtils.MINUTE_IN_MILLIS);
			if (ts.compareTo("0 minutes ago") == 0 || ts.startsWith("in")) {
				ts = "moments ago";
			}
			return ts;
		}
		
		/**
		 * Get the readable (i.e. display if possible, username if not) name of the sender
		 * or recipient of the snap depending on whether it was sent or received."
		 * @return The display or username of the other contact
		 */
		public String getSenderOrRecipient() {
			return (getSent() ? getFriendlyRecipName() : getFriendlySenderName());
		}
		
		/**
		 * Get whether the snap was sent or received. NOT the current state of the
		 * snap.
		 * @return True if it was sent, false if it was received
		 */
		public boolean getSent() {
			return IO == IOType.SENT;
		}
		
		/**
		 * Get the sent timestamp in milliseconds since the epoch
		 * @return The sent timestamp
		 */
		private long getSentTimeStamp() {
			return SentTimeStamp;
		}
		
		/**
		 * Gets whether the snap is likely to be able to be downloaded. This means it
		 * was not sent by the user and is either in the sent or delivered state.
		 * @return True if the snap is probably able to be downloaded and false otherwise
		 */
		public boolean getSnapAvailable() {
			return (!getSent() && (State == SnapStatus.SENT || State == SnapStatus.DELIVERED));
		}
		
		/**
		 * Get whether the snap is available locally
		 * @param ctxt A context
		 * @return True if the snap does exist on file, false otherwise
		 */
		public boolean getSnapExists(Context ctxt) {
			try {
				File imgFile = new File(getSnapPath(ctxt));
				return imgFile.exists();
			} catch (IOException e) {
				return false;
			}
			
			/* if (getSent() || SettingsAccessor.getAllowSaves(ctxt)) {
				try {
					File imgFile = new File(getSnapPath());
					if (imgFile.exists())
						return true;
					return false;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			} else {
				String ext = isPhoto() ? ".jpg" : ".mp4";
				File imgFile = new File(ctxt.getCacheDir(), SnapID + ext);
				return imgFile.exists();
			} */
		}
		
		/**
		 * Get the snap id
		 * @return The snap id
		 */
		public String getSnapId() {
			return SnapID;
		}
		
		/**
		 * Get the local path to a snap. This is always on an external storage device and
		 * so the method first checks whether that is readable. Works with both sent and
		 * received photos and videos. Will also create the required folders if they don't
		 * exist.
		 * @param context A context
		 * @return The path to where the snap should be stored 
		 * @throws IOException If the SD-Card is unavailable this error will be thrown
		 */
		public String getSnapPath(Context context) throws IOException {
			if (isPhoto() && !getSent() && !SettingsAccessor.getAllowSaves(context)) {
				return new File(context.getCacheDir(), SnapID + ".jpg").getAbsolutePath();
			}
			
			if (!StatMethods.getExternalReadable())
				throw new IOException("SD-Card is not readable");
			else if (!StatMethods.getExternalWriteable())
				throw new IOException("SD-Card is not writeable");
			
			String ext = (isPhoto()) ? ".jpg" : ".mp4";
			
			String mediaFolder = (isPhoto()) ? CameraUtil.PICTURE_PATH : CameraUtil.VIDEO_PATH;
			File snapdir = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH + mediaFolder);
			if (!snapdir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                snapdir.mkdirs();
			}
			return (getSent()) ? new File(snapdir, ConvID + ext).getAbsolutePath() : new File(snapdir, SnapID + ext).getAbsolutePath();
		}
		
		/**
		 * Get whether a snap's thumbnail image exists locally. If the external storage 
		 * device is unavailable when calling {@link #getSnapThumbPath()}  } this
		 * method will catch the exception and return false.
		 * @return True if the thumbnail exists and is accessible.
		 */
		public boolean getSnapThumbExists() {
			File imgFile; 
			try {
				imgFile = new File(getSnapThumbPath());
			} catch (IOException e) {
				return false;
			}
            return imgFile.exists();
        }
		
		/**
		 * Get the local path to a snap thumbnail. This is always on an external storage and
		 * so the method first checks whether that is readable. Works with both sent and 
		 * received photos and videos. Will also create the required folders if they don't
		 * exist.
		 * @return A path to the snap's thumbnail image
		 */
		public String getSnapThumbPath() throws IOException {
			if (!StatMethods.getExternalReadable())
				throw new IOException("SD-Card is not readable");
			else if (!StatMethods.getExternalWriteable())
				throw new IOException("SD-Card is not writeable");
			
			String mediaFolder = (isPhoto()) ? CameraUtil.PICTURE_PATH : CameraUtil.VIDEO_PATH;
			File snapdir = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH + mediaFolder);
			if (!snapdir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                snapdir.mkdirs();
			}
			return (getSent()) ? new File(snapdir, ConvID + ".thumb.jpg.nomedia").getAbsolutePath() : 
				new File(snapdir, SnapID + ".thumb.jpg.nomedia").getAbsolutePath();
		}
		
		/**
		 * Get the primary timestamp in milliseconds since the epoch
		 * @return The timestamp in milliseconds
		 */
		private long getTimeStamp() {
			return TimeStamp;
		}
		
		/**
		 * Get whether the snap has a display time. A video will also return false
		 * @return True if the snap has a display time
		 */
		public boolean hasDisplayTime() {
			return Media == MediaType.PHOTO && DisplayTime != 0;
		}
	
		/**
		 * Get whether the snap is in the delivered state. Note that this is ONLY the 
		 * delivered state. An opened state will return false from this. Could be modified
		 * later to rectify this if necessary.
		 * @return True if the snap is currently in the delivered state, false otherwise
		 */
		private boolean isDelivered() {
			return State == SnapStatus.DELIVERED;
		}
		
		public boolean isDownloading() {
			return isDownloading;
		}
		
		public boolean isError() {
			return isError;
		}
		
		/**
		 * Get whether the snap is in the opened state. 
		 * @return True if the snap is currently in the opened or screenshotted state, false otherwise
		 */
		public boolean isOpened() {
			return State == SnapStatus.OPENED || State == SnapStatus.SCREENSHOT;
		}
		
		/**
		 * Get whether snap is a photo. Only true for the exact photo type for now.
		 * @return True if the snap is a photo, false otherwise
		 */
		public boolean isPhoto() {
			return Media == MediaType.PHOTO;
		}
		
		/**
		 * Get whether the snap is currently in the sent state. Note that this is
		 * mutually exclusive to all the other states and this is NOT whether the
		 * user actually sent this snap. Should probably not occur in OpenSnap currently.
		 * @return True if the snap is currently in the sent state, false otherwise
		 */
		private boolean isSent() {
			return State == SnapStatus.SENT;
		}
		
		/**
		 * Get whether the snap is a video. This includes both the standard video type
		 * and also the VIDEO_NOAUDIO type.
		 * @return True if the snap is a video, false otherwise
		 */
		public boolean isVideo() {
			return (Media == MediaType.VIDEO || Media == MediaType.VIDEO_NOAUDIO);
		}

        /**
         * Check whether a snap is actually a friend request
         * @return True for a friend request, false otherwise
         */
        public boolean isFriendRequest() {
            return Media == MediaType.FRIEND_REQ || Media == MediaType.FRIEND_REQ_IMAGE || Media == MediaType.FRIEND_REQ_VIDEO || Media == MediaType.FRIEND_REQ_VIDEO_NOAUDIO;
        }
		
		/**
		 * Write the Snap to file. Normally used when writing all the LocalSnaps to a file.
		 * @param fs The stream to output to.
		 * @throws Exception IOExceptions mainly
		 */
		private void serialise(FileOutputStream fs) throws Exception {
			fs.write(BitConverter.getBytes(SnapID.length()));
			fs.write(BitConverter.getBytes(SnapID));
			fs.write(BitConverter.getBytes(IOType.getValue(IO)));
			if (getSent()) {
				fs.write(BitConverter.getBytes(Recipient.length()));
				fs.write(BitConverter.getBytes(Recipient));
			} else {
				fs.write(BitConverter.getBytes(Sender.length()));
				fs.write(BitConverter.getBytes(Sender));
			}
			
			if (ConvID == null) {
				fs.write(BitConverter.getBytes(0));
			} else {
				fs.write(BitConverter.getBytes(ConvID.length()));
				fs.write(BitConverter.getBytes(ConvID));
			}
			fs.write(BitConverter.getBytes(TimeStamp));
			fs.write(BitConverter.getBytes(SentTimeStamp));
			fs.write(BitConverter.getBytes(DisplayTime));
			fs.write(BitConverter.getBytes(MediaType.getValue(Media)));
			fs.write(BitConverter.getBytes(SnapStatus.getValue(State)));
			fs.write(BitConverter.getBytes(inCloud));
			if (captionText == null) {
				fs.write(BitConverter.getBytes(0));
			} else {
				fs.write(BitConverter.getBytes(captionText.length()));
				fs.write(BitConverter.getBytes(captionText));
			}
			fs.write(BitConverter.getBytes(captionOrientation));
			fs.write(BitConverter.getBytes(captionLocation));
		}
		
		/**
		 * Set the snap to be currently downloading
		 * @param b The downloading state
		 * @return A reference to this
		 */
		public LocalSnap setDownloading(boolean b) {
			isDownloading = b;
			if (!isDownloading) {
				progressPercentage = -1;
			}
			return this;
		}
		
		/**
		 * Set the snap's current download progress
		 * @param percent The download percentage
		 * @return A reference to this
		 */
		public LocalSnap setDownloadProgress(int percent) {
			progressPercentage = percent;
			return this;
		}
		
		/**
		 * Set the snap's current error status
		 * @param b The error state
		 * @return A reference to this
		 */
		public LocalSnap setError(boolean b) {
			isError = b;
			return this;
		}
		
		/**
		 * Set this snap to be in the opened state. As in it's just been opened by the user.
		 */
		public void setOpened() {
			State = SnapStatus.OPENED;
		}
		
		@Override
		public String toString() {
			return ((getSent() ? "Sent to " : "Received from ") + getSenderOrRecipient() + ": " + getReadableSentTimeStamp());
		}
		
		/**
		 * Update this snap with info from one downloaded from the server
		 * @param snap The new snap info
		 */
		private void update(LocalSnap snap) {
			SentTimeStamp = snap.SentTimeStamp;
			TimeStamp = snap.TimeStamp;
			State = snap.State;
			this.inCloud = true;
		}
		

		public boolean wasOpened() {
			return !(State == SnapStatus.SENT || State == SnapStatus.DELIVERED);
		}
	}
	
	/**
	 * An enum abstraction of the SnapChat media type value as returned from the server
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public static enum MediaType {
		FRIEND_REQ, FRIEND_REQ_IMAGE, FRIEND_REQ_VIDEO, FRIEND_REQ_VIDEO_NOAUDIO, PHOTO, VIDEO, VIDEO_NOAUDIO;
		
		/**
		 * Get the enum representation of an integer value
		 * @param val The integer value to be converted
		 * @return The enum equivalent
		 */
		public static MediaType getType(int val) {
			switch (val) {
				case 0:
					return PHOTO;
				case 1:
					return VIDEO;
				case 2:
					return VIDEO_NOAUDIO;
				case 3:
					return FRIEND_REQ;
				case 4:
					return FRIEND_REQ_IMAGE;
				case 5:
					return FRIEND_REQ_VIDEO;
				case 6:
					return FRIEND_REQ_VIDEO_NOAUDIO;
				default:
					return null;
			}
		}
		
		/**
		 * Get the integer value of a enum representation
		 * @param val The enum to convert
		 * @return The equivalent integer value. -1 is for an error 
		 */
		public static int getValue(MediaType val) {
			switch (val) {
				case PHOTO:
					return 0;
				case VIDEO:
					return 1;
				case VIDEO_NOAUDIO:
					return 2;
				case FRIEND_REQ:
					return 3;
				case FRIEND_REQ_IMAGE:
					return 4;
				case FRIEND_REQ_VIDEO:
					return 5;
				case FRIEND_REQ_VIDEO_NOAUDIO:
					return 6;
				default:
					return -1;
			}
		}
	}
	
	/**
	 * An enum of the status of a Snap.
	 * Options are: SENT, DELIVERED, OPENED, SCREENSHOT.
	 * Note that all statuses are mutually exclusive and have to be inferred from others,
	 * i.e. an opened snap has already been sent and delivered.
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public static enum SnapStatus {
		/**
		 * The snap has been delivered to the recipient. I'm not sure this is completely accurate
		 * since it seems to occur instantaneously. If not, it probably just means it's been
		 * uploaded to the servers successfully.
		 */
		DELIVERED, 
		/**
		 * The snap has been opened by the recipient. This also means the snap media is unavailable
		 * for further download.
		 */
		OPENED,
		/**
		 * The snap has been screenshotted by the recipient. OpenSnap doesn't ever send this message.
		 */
		SCREENSHOT,
		/**
		 * The snap has been sent to the recipient. Should only be available until the return
		 * from the SnapChat server's confirmation.
		 */
		SENT;
		
		/**
		 * Get whether the given status is available for download
		 * @param status The SnapStatus enum
		 * @return True if the snap should be available for download, false otherwise
		 */
		public static Boolean getAvailable(SnapStatus status) {
            return status == SENT || status == DELIVERED;
		}
		
		/**
		 * Get the enum of a given integer. This would normally be used to get an enum value from the
		 * SnapChat JSON
		 * @param val The integer value to be converted
		 * @return The equivalent {@link SnapStatus} enum or null for an invalid/unrecognised value
		 */
		public static SnapStatus getType(int val) {
			switch (val) {
				case 0:
					return SENT;
				case 1:
					return DELIVERED;
				case 2:
					return OPENED;
				case 3:
					return SCREENSHOT;
				default:
					return null;
			}
		}
		
		/**
		 * Get the integer value of an enum. This is the value that is actually sent in the SnapChat
		 * JSON
		 * @param status The enum to be evaluated
		 * @return The integer equivalent
		 */
		public static int getValue(SnapStatus status) {
			switch (status) {
				case SENT:
					return 0;
				case DELIVERED:
					return 1;
				case OPENED:
					return 2;
				case SCREENSHOT:
					return 3;
				default:
					return -1;
			}
		}
	}
	
	/**
	 * 
	 * @author Nick's Laptop
	 *
	 */
	public static enum CaptionOrientation {
		PORTRAIT,
		LANDSCAPE_LEFT,
		LANDSCAPE_RIGHT;
		
		public static CaptionOrientation getOrientation(int val) {
			switch (val) {
				case 1:
					return LANDSCAPE_LEFT;
				case 2:
					return LANDSCAPE_RIGHT;
				default:
					return PORTRAIT;
			}
		}
	}
}
