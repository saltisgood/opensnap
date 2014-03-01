package com.nickstephen.opensnap.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerResponse;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;

/**
 * A class for saving the random statistics that come with the SnapChat JSON
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class Statistics {
	private static final long STATS_CURRENT_HEADER_VER = 0x4E53303153543031L; //NS01ST01
	
	/**
	 * The name of the file that's used to store the statistics
	 */
	private static final String STATS_FILE = "stats.bin";
	
	/**
	 * The static instance of this class
	 */
	private static Statistics sInstance = null;

    public static Statistics getInstanceUnsafe() {
        return sInstance;
    }

    public static void getInstanceSafe(IOnObjectReady<Statistics> waiter) {
        Broadcast.waitForStatistics(waiter);
    }

    public static boolean checkInit() {
        return sInstance != null;
    }

	/**
	 * Initialise the statistics information from file
	 * @param ctxt A context
	 * @return 0 on success, < 0 for error
	 */
	public static int init(Context ctxt) {
		if (sInstance != null)
			return 0;
		sInstance = new Statistics();
		return sInstance.read(ctxt);
	}

    public static int sync(ServerResponse response, Context context) {
        sInstance = new Statistics();

        sInstance.mSnapPhoneNumber = response.snapchat_phone_number;
        sInstance.mReceivedSnaps = response.received;
        sInstance.mSentSnaps = response.sent;
        sInstance.mDeviceToken = response.device_token;
        sInstance.mEmail = response.email;
        sInstance.mLastUpdate = System.currentTimeMillis();
        sInstance.mLastUpdate = response.last_updated;
        sInstance.mMobileVerification = response.mobile_verification_key;
        sInstance.mMobileNumber = response.mobile;
        sInstance.mCountryCode = response.country_code;
        
        return sInstance.write(context);
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
     * Get the SnapChat phone number
     * @return The string of the phone number
     */
    public String getSnapPhoneNo() {
        return mSnapPhoneNumber;
    }

    /**
     * Get the number of received snaps
     * @return The number of received snaps
     */
    public int getReceivedSnaps() {
        return mReceivedSnaps;
    }

    /**
     * Get the country code of the user
     * @return A short string version of the user's country code, e.g. 'AU'
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * Get the number of sent snaps by the user
     * @return The number of sent snaps
     */
    public int getSentSnaps() {
        return mSentSnaps;
    }

    /**
     * Get the special device token of the user's phone
     * @return The string token
     */
    public String getDeviceToken() {
        return mDeviceToken;
    }

    /**
     * Get the user's email address
     * @return An email address
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * Get the timestamp of the last update
     * @return The time of the last update in milliseconds since the epoch
     */
    public long getLastUpdate() {
        return mLastUpdate;
    }

    /**
     * Get the mobile verification key
     * @return A mobile verification key string
     */
    public String getMobileVerification() {
        return mMobileVerification;
    }

    /**
     * Get the user's mobile phone number
     * @return A string of the phone number
     */
    public String getMobileNo() {
        return mMobileNumber;
    }
	
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
