package com.nickstephen.opensnap.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.opensnap.util.misc.CustomJSON;

/**
 * A class for saving the random statistics that come with the SnapChat JSON
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class Statistics {
	// JSON Keys
	/**
	 * A key for getting the snapchat phone number from the snapchat JSON
	 */
	private static final String SNAP_PHONE_NO_KEY = "snapchat_phone_number";
	/**
	 * A key for getting the number of received snaps from the snapchat JSON
	 */
	private static final String RECEIVED_SNAPS_KEY = "received";
	/**
	 * A key for getting the country code from the snapchat JSON
	 */
	private static final String COUNTRY_KEY = "country_code";
	/**
	 * A key for getting the number of sent snaps from the snapchat JSON
	 */
	private static final String SENT_SNAPS_KEY = "sent";
	/**
	 * A key for getting the device token from the snapchat JSON
	 */
	private static final String DEVICE_KEY = "device_token";
	/**
	 * A key for getting the email address from the snapchat JSON
	 */
	private static final String EMAIL_KEY = "email";
	/**
	 * A key for getting the last update time from the snapchat JSON
	 * DEPRECATED
	 */
	//private static final String LAST_UPDATE_KEY = "last_updated";
	/**
	 * A key for getting the mobile verification key from the snapchat JSON
	 */
	private static final String MOBILE_VERI_KEY = "mobile_verification_key";
	/**
	 * A key for getting the mobile number of the user from the snapchat JSON
	 */
	private static final String MOBILE_NUM_KEY = "mobile";
	
	private static final long STATS_CURRENT_HEADER_VER = 0x4E53303153543031L; //NS01ST01
	
	// JSON Types
	/**
	 * The string type string
	 */
	private static final String STRING_TYPE = "String";
	/**
	 * The float type string
	 */
	private static final String FLOAT_TYPE = "Float";
	
	/**
	 * The name of the file that's used to store the statistics
	 */
	private static final String STATS_FILE = "stats.bin";
	
	/**
	 * The static instance of this class
	 */
	private static Statistics sThis = null;

	/**
	 * Initialise the statistics information from file
	 * @param ctxt A context
	 * @return 0 on success, < 0 for error
	 */
	public static int Init(Context ctxt) {
		if (sThis != null)
			return 0;
		sThis = new Statistics();
		return sThis.read(ctxt);
	}
	
	/**
	 * Sync the statistics from a SnapChat JSON
	 * @param json The JSON object returned from the server
	 */
	public static int Sync(CustomJSON json, Context ctxt) {
		sThis = new Statistics();
		
		if (json.CheckKeyExists(SNAP_PHONE_NO_KEY) && json.GetType(SNAP_PHONE_NO_KEY).compareTo(STRING_TYPE) == 0) {
			sThis.mSnapPhoneNumber = (String)json.GetValue(SNAP_PHONE_NO_KEY);
		}
		if (json.CheckKeyExists(RECEIVED_SNAPS_KEY) && json.GetType(RECEIVED_SNAPS_KEY).compareTo(FLOAT_TYPE) == 0) {
			sThis.mReceivedSnaps = ((Float)json.GetValue(RECEIVED_SNAPS_KEY)).intValue();
		}
		if (json.CheckKeyExists(COUNTRY_KEY) && json.GetType(COUNTRY_KEY).compareTo(STRING_TYPE) == 0) {
			sThis.mCountryCode = (String)json.GetValue(COUNTRY_KEY);
		}
		if (json.CheckKeyExists(SENT_SNAPS_KEY) && json.GetType(SENT_SNAPS_KEY).compareTo(FLOAT_TYPE) == 0) {
			sThis.mSentSnaps = ((Float)json.GetValue(SENT_SNAPS_KEY)).intValue();
		}
		if (json.CheckKeyExists(DEVICE_KEY) && json.GetType(DEVICE_KEY).compareTo(STRING_TYPE) == 0) {
			sThis.mDeviceToken = (String)json.GetValue(DEVICE_KEY);
		}
		if (json.CheckKeyExists(EMAIL_KEY) && json.GetType(EMAIL_KEY).compareTo(STRING_TYPE) == 0) {
			sThis.mEmail = (String)json.GetValue(EMAIL_KEY);
		}
		sThis.mLastUpdate = System.currentTimeMillis();
		/*
		if (json.CheckKeyExists(LAST_UPDATE_KEY) && json.GetType(LAST_UPDATE_KEY).compareTo(FLOAT_TYPE) == 0) {
			sThis._lastUpdate = ((Float)json.GetValue(LAST_UPDATE_KEY)).longValue();
		}
		*/
		if (json.CheckKeyExists(MOBILE_VERI_KEY) && json.GetType(MOBILE_VERI_KEY).compareTo(STRING_TYPE) == 0) {
			sThis.mMobileVerification = (String)json.GetValue(MOBILE_VERI_KEY);
		}
		if (json.CheckKeyExists(MOBILE_NUM_KEY) && json.GetType(MOBILE_NUM_KEY).compareTo(STRING_TYPE) == 0) {
			sThis.mMobileNumber = (String)json.GetValue(MOBILE_NUM_KEY);
		}
		
		return sThis.write(ctxt);
	}
	
	/**
	 * Get the SnapChat phone number
	 * @return The string of the phone number
	 */
	public static String getSnapPhoneNo() {
		return sThis.mSnapPhoneNumber;
	}
	
	/**
	 * Get the number of received snaps
	 * @return The number of received snaps
	 */
	public static int getReceivedSnaps() {
		return sThis.mReceivedSnaps;
	}
	
	/**
	 * Get the country code of the user
	 * @return A short string version of the user's country code, e.g. 'AU'
	 */
	public static String getCountryCode() {
		return sThis.mCountryCode;
	}
	
	/**
	 * Get the number of sent snaps by the user
	 * @return The number of sent snaps
	 */
	public static int getSentSnaps() {
		return sThis.mSentSnaps;
	}
	
	/**
	 * Get the special device token of the user's phone
	 * @return The string token
	 */
	public static String getDeviceToken() {
		return sThis.mDeviceToken;
	}
	
	/**
	 * Get the user's email address
	 * @return An email address
	 */
	public static String getEmail() {
		return sThis.mEmail;
	}
	
	/**
	 * Get the timestamp of the last update
	 * @return The time of the last update in milliseconds since the epoch
	 */
	public static long getLastUpdate() {
		return sThis.mLastUpdate;
	}
	
	/**
	 * Get the mobile verification key
	 * @return A mobile verification key string
	 */
	public static String getMobileVerification() {
		return sThis.mMobileVerification;
	}
	
	/**
	 * Get the user's mobile phone number
	 * @return A string of the phone number
	 */
	public static String getMobileNo() {
		return sThis.mMobileNumber;
	}
	
	private Statistics() {}
	
	private String mSnapPhoneNumber;
	private int mReceivedSnaps;
	private String mCountryCode;
	private int mSentSnaps;
	private String mDeviceToken;
	private String mEmail;
	private long mLastUpdate;
	private String mMobileVerification;
	private String mMobileNumber;
	
	/**
	 * Write the stats from file
	 * @param ctxt A context
	 * @return A status code. < 0 for error
	 */
	private int write(Context ctxt) {
		FileOutputStream fs;
		try {
			fs = ctxt.openFileOutput(GlobalVars.getUsername(ctxt) + "-" + STATS_FILE, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return -1;
		}
		
		try {
			fs.write(BitConverter.getBytes(STATS_CURRENT_HEADER_VER));
            if (mSnapPhoneNumber == null) {
                fs.write(BitConverter.getBytes(0));
            } else {
                fs.write(BitConverter.getBytes(mSnapPhoneNumber.length()));
                fs.write(BitConverter.getBytes(mSnapPhoneNumber));
            }
			fs.write(BitConverter.getBytes(mReceivedSnaps));
            if (mCountryCode == null) {
                fs.write(BitConverter.getBytes(0));
            } else {
                fs.write(BitConverter.getBytes(mCountryCode.length()));
                fs.write(BitConverter.getBytes(mCountryCode));
            }
			fs.write(BitConverter.getBytes(mSentSnaps));
            if (mDeviceToken == null) {
                fs.write(BitConverter.getBytes(0));
            } else {
                fs.write(BitConverter.getBytes(mDeviceToken.length()));
                fs.write(BitConverter.getBytes(mDeviceToken));
            }
            if (mEmail == null) {
                fs.write(BitConverter.getBytes(0));
            } else {
                fs.write(BitConverter.getBytes(mEmail.length()));
                fs.write(BitConverter.getBytes(mEmail));
            }
			fs.write(BitConverter.getBytes(mLastUpdate));
            if (mMobileVerification == null) {
                fs.write(BitConverter.getBytes(0));
            } else {
                fs.write(BitConverter.getBytes(mMobileVerification.length()));
                fs.write(BitConverter.getBytes(mMobileVerification));
            }
            if (mMobileNumber == null) {
                fs.write(BitConverter.getBytes(0));
            } else {
                fs.write(BitConverter.getBytes(mMobileNumber.length()));
                fs.write(BitConverter.getBytes(mMobileNumber));
            }
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
			return -2;
		} catch (Exception e) {
			e.printStackTrace();
			return -3;
		}
		
		return 0;
	}
	
	/**
	 * Read the stats from file
	 * @param ctxt A context
	 * @return A status code. < 0 for errors
	 */
	private int read(Context ctxt) {
		FileInputStream fs;
		try {
			fs = ctxt.openFileInput(GlobalVars.getUsername(ctxt) + "-" + STATS_FILE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
		
		try {
			byte[] buff = new byte[8];
			int len = fs.read(buff);
			if (len != 8) {
				throw new IOException("Invalid read number");
			}
			long ver = BitConverter.toInt64(buff, 0);
			if (ver != STATS_CURRENT_HEADER_VER) {
				throw new RuntimeException("Incorrect header version: " + ver);
			}
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			int num = BitConverter.toInt32(buff, 0);
            if (num == 0) {
                mSnapPhoneNumber = "";
            } else {
                buff = new byte[num];
                len = fs.read(buff);
                if (len != num)
                    throw new IOException("Invalid read number");
                mSnapPhoneNumber = BitConverter.toString(buff);
            }
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			mReceivedSnaps = BitConverter.toInt32(buff, 0);
			
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			num = BitConverter.toInt32(buff, 0);
            if (num == 0) {
                mCountryCode = "";
            } else {
                buff = new byte[num];
                len = fs.read(buff);
                if (len != num)
                    throw new IOException("Invalid read number");
                mCountryCode = BitConverter.toString(buff);
            }
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			mSentSnaps = BitConverter.toInt32(buff, 0);
			
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			num = BitConverter.toInt32(buff, 0);
            if (num == 0) {
                mDeviceToken = "";
            } else {
                buff = new byte[num];
                len = fs.read(buff);
                if (len != num)
                    throw new IOException("Invalid read number");
                mDeviceToken = BitConverter.toString(buff);
            }
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			num = BitConverter.toInt32(buff, 0);
            if (num == 0) {
                mEmail = "";
            } else {
                buff = new byte[num];
                len = fs.read(buff);
                if (len != num)
                    throw new IOException("Invalid read number");
                mEmail = BitConverter.toString(buff);
            }
			
			buff = new byte[8];
			len = fs.read(buff);
			if (len != 8)
				throw new IOException("Invalid read number");
			mLastUpdate = BitConverter.toInt64(buff, 0);
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			num = BitConverter.toInt32(buff, 0);
            if (num == 0) {
                mMobileVerification = "";
            } else {
                buff = new byte[num];
                len = fs.read(buff);
                if (len != num)
                    throw new IOException("Invalid read number");
                mMobileVerification = BitConverter.toString(buff);
            }
			
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Invalid read number");
			num = BitConverter.toInt32(buff, 0);
            if (num == 0) {
                mMobileNumber = "";
            } else {
                buff = new byte[num];
                len = fs.read(buff);
                if (len != num)
                    throw new IOException("Invalid read number");
                mMobileNumber = BitConverter.toString(buff);
            }
			
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
			return -2;
		} catch (Exception e) {
			e.printStackTrace();
			return -3;
		}
		
		return 0;
	}
}
