package com.nickstephen.opensnap.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.content.Context;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.http.ServerFriend;
import com.nickstephen.opensnap.util.http.ServerResponse;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;

/**
 * A class used for keeping track of Contacts. It's designed to be accessed statically.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class Contacts {
	/**
	 * The filename of the file that holds the contact data
	 */
	public static final String CONTACTS_FILENAME = "contacts.bin";

    private static final long CONTACTS_HEADER_VER_1 = 0x4E533031434F3031L; //NS01CO01
	
	private static final long CONTACTS_CURRENT_HEADER_VER = 0x4E533031434F3032L; //NS01CO02
	
	/**
	 * The instance of the class that is used for all the static access. It's initialised
	 * with a call to {@link #init(Context)}. It has private visibility so that all
	 * modifications go through the static accessors.
	 */
	private static Contacts sInstance = null;

    public static Contacts getInstanceUnsafe() {
        return sInstance;
    }

    public static void getInstanceSafe(IOnObjectReady<Contacts> waiter) {
        Broadcast.waitForContacts(waiter);
    }
	
	/**
	 * Initialise the static instance for later use. If the config file
	 * exists then that is read in, otherwise it's just a default constructor.
	 * If the variable is already initialised then all this is skipped.
	 * @param ctxt The context to be used to find the config file
	 * @return True if the initialisation was successful, false on failure
	 */
	public static boolean init(Context ctxt) {
		if (checkInit())
			return true;
		try {
			sInstance = new Contacts(ctxt);
		} catch (Exception e) {
			sInstance = new Contacts();
			return false;
		}
		return true;
	}
	
	/**
	 * Check whether the static instance variable is initialised
	 * @return
	 */
	public static boolean checkInit() {
		return sInstance != null;
	}
	
	/**
	 * A list of "Contact"s that is stored within the instance of the Contacts class
	 * Nice terminology there nicko
	 */
	private List<Contact> mContacts;
	
	/**
	 * The private default constructor. This should be used when the config file doesn't exist
	 */
	private Contacts() {
		mContacts = new ArrayList<Contact>();
	}
	
	/**
	 * The private primary constructor. This is used when the config file exists.
	 * @param ctxt A context
	 * @throws FileNotFoundException Thrown if the file doesn't exist
	 * @throws Exception Primarily thrown from IOExceptions
	 */
	private Contacts(Context ctxt) throws FileNotFoundException, Exception {
		mContacts = new ArrayList<Contact>();
		
		readFromFile(ctxt); 
	}
	
	/**
	 * Read the config file and initialise the Contact list
	 * @param ctxt A context
	 * @throws FileNotFoundException If the config file doesn't exist
	 * @throws Exception Primarily for IOExceptions
	 */
	private void readFromFile(Context ctxt) throws FileNotFoundException, Exception {
		FileInputStream fs = ctxt.openFileInput(GlobalVars.getUsername(ctxt) + "-" + CONTACTS_FILENAME);
		
		byte[] buff = new byte[8];
        boolean isCurrentVersion = true;
		try {
			int len = fs.read(buff);
			if (len != 8) {
				throw new IOException("Incorrect number of bytes read");
			}
			long ver = BitConverter.toInt64(buff, 0);
			if (ver != CONTACTS_CURRENT_HEADER_VER && ver != CONTACTS_HEADER_VER_1) {
				throw new RuntimeException("Incorrect header version: " + ver);
			}
            isCurrentVersion = (ver == CONTACTS_CURRENT_HEADER_VER);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		buff = new byte[4];
		try {
			int len = fs.read(buff);
			if (len != 4)
				throw new IOException("Incorrect number of bytes read");
			len = BitConverter.toInt32(buff, 0);
			for (int i = 0; i < len; i++) {
				mContacts.add(new Contact(fs, isCurrentVersion));
			}
		} catch(IOException e) {
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

    public boolean addFriend(ServerFriend friend) {
        for (Contact c : mContacts) {
            if (friend.name.compareTo(c.mUsername) == 0) {
                return false;
            }
        }

        mContacts.add(new Contact(friend));
        return true;
    }
	
	/**
	 * Save the contacts to file
	 * @param context A context
	 */
	public boolean serialiseToFile(Context context) {
        FileOutputStream fs;
        try {
            fs = context.openFileOutput(GlobalVars.getUsername(context) + "-" + CONTACTS_FILENAME, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            Twig.printStackTrace(e);
            return false;
        }

        try {
            fs.write(BitConverter.getBytes(CONTACTS_CURRENT_HEADER_VER));
            fs.write(BitConverter.getBytes(mContacts.size()));

            for (Contact ct : mContacts) {
                ct.serialiseToFile(fs);
            }

            fs.close();
        } catch (IOException e) {
            Twig.printStackTrace(e);
            return false;
        } catch (Exception e) {
            Twig.printStackTrace(e);
            return false;
        }

        return true;
	}
	
	/**
	 * Get the username of the contact at a certain position
	 * @param position The position of the contact
	 * @return The username of the contact
	 */
	public String getUsernameAt(int position) {
		return mContacts.get(position).getUserName();
	}
	
	/**
	 * Get the display name of the contact at a certain position
	 * @param position The position of the Contact
	 * @return The display name
	 */
	private String getDisplayNameAt(int position) {
		return mContacts.get(position).getDisplayName();
	}
	
	/**
	 * Get a Contact object
	 * @param position The position of the contact
	 * @return The Contact object
	 */
	private Contact getContact(int position) {
		return mContacts.get(position);
	}
	
	/**
     * Check whether a contact exists with a particular username and/or get
     * its position in the list.
	 * @param username The username to search for
	 * @return -1 if it doesn't exist, otherwise the position in the list
	 */
	public int contactExists(String username) {
		for (int i = 0; i < mContacts.size(); i++) {
			if (mContacts.get(i).getUserName().compareTo(username) == 0) {
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Remove a contact from the list.
	 * @param username The username of the contact to be removed
	 */
	private void removeContact(String username) {
		for (int i = 0; i < mContacts.size(); i++) {
			if (mContacts.get(i).getUserName().compareTo(username) == 0) {
				mContacts.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Sort the contacts into username alphabetical order. Should only really be 
	 * called when the contact list changes otherwise it's a waste of time.
	 */
	public void sort() {
		Contact[] tmpCt = mContacts.toArray(new Contact[mContacts.size()]);
		Arrays.sort(tmpCt, new Comparator<Contact>() {
            @Override
            public int compare(Contact lhs, Contact rhs) {
                return lhs.getDisplayOrUserName().toUpperCase(Locale.ENGLISH).compareTo(rhs.getDisplayOrUserName().toUpperCase(Locale.ENGLISH));
            }
        });
		
		mContacts = new ArrayList<Contact>();
        Collections.addAll(mContacts, tmpCt);
	}

    /**
     * Check whether a contact has a display name
     * @param position The position of the contact in the contact list (sorted alphabetically by username)
     * @return True if the contact has a display name, false if it doesn't
     */
	public boolean hasDisplay(int position) {
		return mContacts.get(position).hasDisplay();
	}

    /**
     * Check whether a contact is a best friend
     * @param position The position of the contact in the list
     * @return True if the contact is a best friend, false otherwise
     */
    public boolean isBesty(int position) {
        return mContacts.get(position).mBesty;
    }

    /**
     * Get an instance of a contact by a username
     * @param username The username to search for
     * @return The Contact object
     */
    public Contacts.Contact getContactWithName(String username) {
        for (Contact ct : mContacts) {
            if (username.compareTo(ct.getUserName()) == 0) {
                return ct;
            }
        }
        return null;
    }

    /**
     * Get either the display or username of a contact at a position in the list.
     * The display name is taken preferentially over the username.
     * @param position The position of the contact in the list (sorted alphabetically by username)
     * @return The display or username
     */
	public String getDisplayOrUserName(int position) {
		return mContacts.get(position).getDisplayOrUserName();
	}

    /**
     * Sets the display name of a contact
     * @param user The username of the contact to change
     * @param newDisplay The new display name for that contact to have
     * @return False if the contact couldn't be found with that username, true on success
     */
    public boolean setDisplayName(String user, String newDisplay) {
        Contact contact = getContactWithName(user);
        if (contact == null) {
            return false;
        }

        contact.mDisplayName = newDisplay;
        return true;
    }

    public void sync(ServerResponse response) {
        mContacts = new ArrayList<Contact>();
        for (ServerFriend friend : response.friends) {
            mContacts.add(new Contact(friend));
        }
        sort();

        if (response.bests != null) {
            for (String best : response.bests) {
                for (Contact ct : mContacts) {
                    if (ct.mUsername.compareTo(best) == 0) {
                        ct.mBesty = true;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get the number of contacts
     * @return The number of contacts
     */
    public int getNumContacts() {
        return mContacts.size();
    }
	
	/**
	 * A Contact class. Used for storing all the relevant info for a single Contact.
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public static class Contact {
		/**
		 * The Contact's username. Every user has one and they're all unique.
		 * Probably not allowed to contain ,
		 */
		private String mUsername = null;
		/**
		 * The Contact's display name. These are optional and aren't necessarily unique.
		 */
		private String mDisplayName = null;
        private int mType;
		/**
		 * An enum version of {@link #mType}
		 */
		private FriendType mFriendType;
		/**
		 * Whether the contact is a best friend
		 */
		private boolean mBesty = false;

        public Contact(ServerFriend friend) {
            mUsername = friend.name;
            mDisplayName = friend.display;
            mType = friend.type;
        }

		/**
		 * The third constructor. This one is used when reading from the config file.
		 * @param fs The stream to read from
		 * @throws IOException 
		 * @throws Exception
		 */
		public Contact(FileInputStream fs, boolean currentVersion) throws IOException, Exception {
			byte[] buff = new byte[4];
			int len = fs.read(buff);
			if (len != 4)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 1)");
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 2)");
			mUsername = new String(buff);
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 3)");
			len = BitConverter.toInt32(buff, 0);
			if (len != 0) {
				buff = new byte[len];
				len = fs.read(buff);
				if (len != buff.length)
					throw new IOException("Incorrect number of bytes read (Contact constructor: 4)");
				mDisplayName = new String(buff);
			}
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 5");
            if (currentVersion) {
                mType = BitConverter.toInt32(buff, 0);
            } else {
                mType = (int) BitConverter.toSingle(buff, 0);
            }
			buff = new byte[1];
			len = fs.read(buff);
			if (len != 1)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 6");
			mBesty = BitConverter.toBoolean(buff, 0);
		}
		
		/**
		 * Return the username
		 * @return The username
		 */
		public String getUserName() {
			return mUsername;
		}
		
		/**
		 * Check whether the contact has a display name
		 * @return True if they do, false if they don't
		 */
		public Boolean hasDisplay() {
			return !StatMethods.IsStringNullOrEmpty(mDisplayName);
		}
		
		/**
		 * Return the display name. Does not check whether it actually exists.
		 * @return The display name
		 */
		public String getDisplayName() {
			return mDisplayName;
		}
		
		/**
		 * Get the raw number of the friend type
		 * @return The type as an int
		 */
		public int getType() {
			return mType;
		}
		
		/**
		 * Get either the display or username of the contact. Display names are returned
		 * preferentially.
		 * @return The display or username
		 */
		public String getDisplayOrUserName() {
			return (hasDisplay()) ? mDisplayName : mUsername;
		}
		
		/**
		 * Write the Contact object to file.
		 * @param fs The stream to output to
		 * @throws Exception Either a BitConverter error, or an IOException
		 */
		public void serialiseToFile(FileOutputStream fs) throws Exception {
			fs.write(BitConverter.getBytes(mUsername.length()));
			fs.write(BitConverter.getBytes(mUsername));
			if (!hasDisplay()) {
				fs.write(BitConverter.getBytes(0)); // int32
			} else {
				fs.write(BitConverter.getBytes(mDisplayName.length()));
				fs.write(BitConverter.getBytes(mDisplayName));
			}
			fs.write(BitConverter.getBytes(mType));
			fs.write(BitConverter.getBytes(mBesty));
		}
		
		/**
		 * Get the friend type as an enum
		 * @return The FriendType enum
		 */
		public FriendType getFriendType() {
			return mFriendType;
		}
	
		/**
		 * Check whether the contact is a best friend
		 * @return True if the contact is a best friend, false if not
		 */
		public boolean isBesty() {
			return mBesty;
		}
		
		@Override
		public String toString() {
			return getDisplayOrUserName();
		}
	}
	
	/**
	 * A simple enum that makes the friend type checking easier
	 * @author Nick Stephen (a.k.a. saltisgood)
	 *
	 */
	public enum FriendType {
		CONFIRMED, UNCONFIRMED
	}
}
