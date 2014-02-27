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

import org.json.JSONException;

import android.content.Context;

import com.nickstephen.lib.misc.BitConverter;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.util.misc.CustomJSON;

/**
 * A class used for keeping track of Contacts. It's designed to be accessed statically.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class Contacts {
	/**
	 * The filename of the file that holds the contact data
	 */
	public static final String CONTACTS_FILENAME = "contacts.bin";
	/**
	 * The key used to find the contact info in the JSON
	 */
	private static final String FRIEND_KEY = "friends";
	/**
	 * The type of the friends node
	 */
	private static final String LIST_TYPE = "List<JSONNode>";
	/**
	 * The key used to find the best friends info in the JSON
	 */
	private static final String BESTS_KEY = "bests";
	/**
	 * The type of the best friends node
	 */
	private static final String LIST_STRING_TYPE = "List<String>";
	
	private static final long CONTACTS_CURRENT_HEADER_VER = 0x4E533031434F3031L; //NS01CO01
	
	/**
	 * The instance of the class that is used for all the static access. It's initialised
	 * with a call to {@link #init(Context)}. It has private visibility so that all
	 * modifications go through the static accessors.
	 */
	private static Contacts sThis = null;
	
	/**
	 * Initialise the static instance for later use. If the config file
	 * exists then that is read in, otherwise it's just a default constructor.
	 * If the variable is already initialised then all this is skipped.
	 * @param ctxt The context to be used to find the config file
	 * @return True if the initialisation was successful, false on failure
	 */
	public static Boolean init(Context ctxt) {
		if (checkInit())
			return true;
		try {
			sThis = new Contacts(ctxt);
		} catch (FileNotFoundException e) { 
			sThis = new Contacts();
		} catch (Exception e) {
			sThis = new Contacts();
			return false;
		}
		return true;
	}
	
	/**
	 * Check whether the static instance variable is initialised
	 * @return
	 */
	public static Boolean checkInit() {
		return sThis != null;
	}
	
	/**
	 * Get the username of the contact at a certain position
	 * @param position The position in the contact list (sorted alphabetically by username)
	 * @return The username
	 */
	public static String getUsernameAt(int position) {
		return sThis.getUsernameAtThis(position);
	}
	
	/**
	 * Get the number of contacts
	 * @return The number of contacts
	 */
	public static int getNumContacts() {
		return sThis.Contacts.size();
	}
	
	/**
	 * Get the display name of the contact at a certain position
	 * @param position The position in the contact list (sorted alphabetically by username)
	 * @return The display name
	 */
	public static String getDisplayNameAt(int position) {
		return sThis.getDisplayNameAtThis(position);
	}
	
	/**
	 * Get an instance of a contact by a username
	 * @param username The username to search for
	 * @return The Contact object
	 */
	public static Contacts.Contact getContactWithName(String username) {
		for (Contact ct : sThis.Contacts) {
			if (username.compareTo(ct.getUserName()) == 0) {
				return ct;
			}
		}
		return null;
	}
	
	/**
	 * Sync the Contact list. Normally called after logging in or updating from the server
	 * @param json The json file downloaded from the server
	 * @throws JSONException 
	 */
	public static void sync(CustomJSON json) throws JSONException {
		if (!json.CheckKeyExists(FRIEND_KEY) || json.GetType(FRIEND_KEY).compareTo(LIST_TYPE) != 0) {
			throw new JSONException("\"friends\" key not found in JSON");
		}
		
		@SuppressWarnings("unchecked")
		List<CustomJSON.JSONNode> jnodes = (List<CustomJSON.JSONNode>)json.GetValue(FRIEND_KEY);
		sThis.Contacts = new ArrayList<Contact>();
		for (CustomJSON.JSONNode node : jnodes) {
			sThis.Contacts.add(new Contact(node));
		}
		sThis.sortThis();
		
		if (json.CheckKeyExists(BESTS_KEY) && json.GetType(BESTS_KEY).compareTo(LIST_STRING_TYPE) == 0) {
			@SuppressWarnings("unchecked")
			List<String> bests = (List<String>)json.GetValue(BESTS_KEY);
			for (String best : bests) {
				for (Contact ct : sThis.Contacts) {
					if (ct._userName.compareTo(best) == 0) {
						ct._besty = true;
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Save the Contacts to file
	 * @param ctxt The context to use
	 * @throws Exception Can rethrow an exception
	 */
	public static void saveToFile(Context ctxt) throws Exception {
		sThis.writeToFile(ctxt);
	}
	
	/**
	 * Check whether a contact has a display name
	 * @param position The position of the contact in the contact list (sorted alphabetically by username) 
	 * @return True if the contact has a display name, false if it doesn't
	 */
	public static boolean hasDisplay(int position) {
		return sThis.hasDisplayThis(position);
	}
	
	/**
	 * Get either the display or username of a contact at a position in the list.
	 * The display name is taken preferentially over the username.
	 * @param position The position of the contact in the list (sorted alphabetically by username)
	 * @return The display or username
	 */
	public static String getDisplayOrUserName(int position) {
		return sThis.getDisplayOrUserNameThis(position);
	}
	
	/**
	 * Check whether a contact is a best friend
	 * @param position The position of the contact in the list
	 * @return True if the contact is a best friend, false otherwise
	 */
	public static boolean isBesty(int position) {
		return sThis.Contacts.get(position)._besty;
	}

    /**
     * Sets the display name of a contact
     * @param user The username of the contact to change
     * @param newDisplay The new display name for that contact to have
     * @return False if the contact couldn't be found with that username, true on success
     */
    public static boolean setDisplayName(String user, String newDisplay) {
        Contact contact = getContactWithName(user);
        if (contact == null) {
            return false;
        }

        contact._displayName = newDisplay;
        return true;
    }

    /**
     * Sets the display name of a contact given a position in the list of contacts
     * @param position The position of the contact in the list
     * @param newDisplay The new display name for that contact to have
     */
    public static void setDisplayName(int position, String newDisplay) {
        sThis.Contacts.get(position)._displayName = newDisplay;
    }

    /**
     * Sort the contacts into alphabetical order of display name first, then username
     */
    public static void sort() {
        sThis.sortThis();
    }
	
	/**
	 * Get a Contact object
	 * @param position The position of the contact in the list (sorted alphabetically by username)
	 * @return The contact object
	 */
	public Contact getContact(int position) {
		return sThis.getContactThis(position);
	}
	
	/**
	 * Check whether a contact exists with a particular username and/or get 
	 * its position in the list. 
	 * @param username The username to look for
	 * @return -1 if the username doesn't exist, otherwise the position of the contact in the list
	 */
	public Integer contactExists(String username) {
		return sThis.contactExistsThis(username);
	}

	/**
	 * Remove a contact from the contact list
	 * @param username The username of the contact to be removed
	 */
	public void removeContact(String username) {
		sThis.removeContactThis(username);
	}
	
	/**
	 * A list of "Contact"s that is stored within the instance of the Contacts class
	 * Nice terminology there nicko
	 */
	private List<Contact> Contacts;
	
	/**
	 * The private default constructor. sThis should be used when the config file doesn't exist
	 */
	private Contacts() {
		Contacts = new ArrayList<Contact>();
	}
	
	/**
	 * The private primary constructor. sThis is used when the config file exists.
	 * @param ctxt A context
	 * @throws FileNotFoundException Thrown if the file doesn't exist
	 * @throws Exception Primarily thrown from IOExceptions
	 */
	private Contacts(Context ctxt) throws FileNotFoundException, Exception {
		Contacts = new ArrayList<Contact>();
		
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
		try {
			int len = fs.read(buff);
			if (len != 8) {
				throw new IOException("Incorrect number of bytes read");
			}
			long ver = BitConverter.toInt64(buff, 0);
			if (ver != CONTACTS_CURRENT_HEADER_VER) {
				throw new RuntimeException("Incorrect header version: " + ver);
			}
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
				Contacts.add(new Contact(fs));
			}
		} catch(IOException e) {
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * The instance version of the writeToFile
	 * @param ctxt A context
	 * @throws Exception General exception (NullPointerException/IOException/etc)
	 */
	private void writeToFile(Context ctxt) throws Exception {
		serialiseToFile(ctxt.openFileOutput(GlobalVars.getUsername(ctxt) + "-" + CONTACTS_FILENAME, Context.MODE_PRIVATE));
	}
	
	/**
	 * The raw file writing method. sThis one does the nitty gritty of actually converting
	 * the Contacts to data.
	 * @param fs The stream to output to
	 * @throws Exception General exceptions (NullPointerException/IOException/etc)
	 */
	private void serialiseToFile(FileOutputStream fs) throws Exception {
		if (fs == null)
			throw new NullPointerException("NULL FileOutputStream to serialise");
		else if (Contacts == null)
			throw new NullPointerException("Contact list hasn't been initialised");
		
		fs.write(BitConverter.getBytes(CONTACTS_CURRENT_HEADER_VER));
		fs.write(BitConverter.getBytes(Contacts.size()));
		
		for (Contact ct : Contacts) {
			try {
				ct.serialiseToFile(fs);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException();
			}
		}
	}
	
	/**
	 * The instance accessor of username
	 * @param position The position of the contact
	 * @return The username of the contact
	 */
	private String getUsernameAtThis(int position) {
		return Contacts.get(position).getUserName();
	}
	
	/**
	 * The instance accessor of display name
	 * @param position The position of the Contact
	 * @return The display name
	 */
	private String getDisplayNameAtThis(int position) {
		return Contacts.get(position).getDisplayName();
	}
	
	/**
	 * The instance accessor of Contact
	 * @param position The position of the contact
	 * @return The Contact object
	 */
	private Contact getContactThis(int position) {
		return Contacts.get(position);
	}
	
	/**
	 * The instance version of {@link #contactExists(String)}
	 * @param username The username to search for
	 * @return -1 if it doesn't exist, otherwise the position in the list
	 */
	private Integer contactExistsThis(String username) {
		for (int i = 0; i < Contacts.size(); i++) {
			if (Contacts.get(i).getUserName().compareTo(username) == 0) {
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * The instance version of {@link #removeContact(String)}. Remove a contact
	 * from the list.
	 * @param username The username of the contact to be removed
	 */
	private void removeContactThis(String username) {
		for (int i = 0; i < Contacts.size(); i++) {
			if (Contacts.get(i).getUserName().compareTo(username) == 0) {
				Contacts.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Sort the contacts into username alphabetical order. Should only really be 
	 * called when the contact list changes otherwise it's a waste of time.
	 */
	private void sortThis() {
		Contact[] tmpCt = Contacts.toArray(new Contact[Contacts.size()]);
		Arrays.sort(tmpCt, new Comparator<Contact>() {
			@Override
			public int compare(Contact lhs, Contact rhs) {
				return lhs.getDisplayOrUserName().toUpperCase(Locale.ENGLISH).compareTo(rhs.getDisplayOrUserName().toUpperCase(Locale.ENGLISH));
			}
		});
		
		Contacts = new ArrayList<Contact>();
        Collections.addAll(Contacts, tmpCt);
	}
	
	/**
	 * The instance version of {@link #hasDisplay(int)}
	 * @param position The position of the contact
	 * @return True if the contact has a display name, false if not
	 */
	private boolean hasDisplayThis(int position) {
		return Contacts.get(position).hasDisplay();
	}
	
	/**
	 * The instance version of {@link #getDisplayOrUserName(int)}
	 * @param position The position of the contact
	 * @return The display or username
	 */
	private String getDisplayOrUserNameThis(int position) {
		return Contacts.get(position).getDisplayOrUserName();
	}
	
	/**
	 * A Contact class. Used for storing all the relevant info for a single Contact.
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public static class Contact {
		/**
		 * The key for the username that's used in the JSONNode
		 */
		private static final String NAMEKEY = "name";
		/**
		 * The key for the display name that's used in the JSONNode
		 */
		private static final String DISPLAYKEY = "display";
		/**
		 * The key for the friend type that's used in the JSONNode
		 */
		private static final String TYPEKEY = "type";
		
		/**
		 * The Contact's username. Every user has one and they're all unique.
		 * Probably not allowed to contain ,
		 */
		private String _userName = null;
		/**
		 * The Contact's display name. These are optional and aren't necessarily unique.
		 */
		private String _displayName = null;
		/**
		 * The friend type as read from the JSONNode.
		 */
		private Float _type = null;
		/**
		 * An enum version of {@link #_type}
		 */
		private FriendType _friendType;
		/**
		 * Whether the contact is a best friend
		 */
		private boolean _besty = false;
		
		/**
		 * The second constructor version. sThis one skips Friends and uses a JSONNode directly
		 * @param jnode The JSONNode to read from
		 * @throws JSONException On missing attributes throw an exception
		 */
		public Contact(CustomJSON.JSONNode jnode) throws JSONException {
			if (!jnode.KeyExists(NAMEKEY) || jnode.GetType(NAMEKEY).compareTo("String") != 0)
				throw new JSONException("\"" + NAMEKEY + "\" key not found in JSONNode");
			if (!jnode.KeyExists(DISPLAYKEY) || jnode.GetType(DISPLAYKEY).compareTo("String") != 0)
				throw new JSONException("\"" + DISPLAYKEY + "\" key not found in JSONNode");
			if (!jnode.KeyExists(TYPEKEY) || jnode.GetType(TYPEKEY).compareTo("Float") != 0)
				throw new JSONException("\"" + TYPEKEY + "\" key not found in JSONNode");
			
			_userName = (String)jnode.GetValue(NAMEKEY);
			_displayName = (String)jnode.GetValue(DISPLAYKEY);
			_type = (Float)jnode.GetValue(TYPEKEY);
		}

		/**
		 * The third constructor. sThis one is used when reading from the config file.
		 * @param fs The stream to read from
		 * @throws IOException 
		 * @throws Exception
		 */
		public Contact(FileInputStream fs) throws IOException, Exception {
			byte[] buff = new byte[4];
			int len = fs.read(buff);
			if (len != 4)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 1)");
			len = BitConverter.toInt32(buff, 0);
			buff = new byte[len];
			len = fs.read(buff);
			if (len != buff.length)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 2)");
			_userName = new String(buff);
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
				_displayName = new String(buff);
			}
			buff = new byte[4];
			len = fs.read(buff);
			if (len != 4)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 5");
			_type = BitConverter.toSingle(buff, 0);
			buff = new byte[1];
			len = fs.read(buff);
			if (len != 1)
				throw new IOException("Incorrect number of bytes read (Contact constructor: 6");
			_besty = BitConverter.toBoolean(buff, 0);
		}
		
		/**
		 * Return the username
		 * @return The username
		 */
		public String getUserName() {
			return _userName;
		}
		
		/**
		 * Check whether the contact has a display name
		 * @return True if they do, false if they don't
		 */
		public Boolean hasDisplay() {
			return !StatMethods.IsStringNullOrEmpty(_displayName);
		}
		
		/**
		 * Return the display name. Does not check whether it actually exists.
		 * @return The display name
		 */
		public String getDisplayName() {
			return _displayName;
		}
		
		/**
		 * Get the raw number of the friend type
		 * @return The type as a float
		 */
		public Float getType() {
			return _type;
		}
		
		/**
		 * Get either the display or username of the contact. Display names are returned
		 * preferentially.
		 * @return The display or username
		 */
		public String getDisplayOrUserName() {
			return (hasDisplay()) ? _displayName : _userName;
		}
		
		/**
		 * Write the Contact object to file.
		 * @param fs The stream to output to
		 * @throws Exception Either a BitConverter error, or an IOException
		 */
		public void serialiseToFile(FileOutputStream fs) throws Exception {
			fs.write(BitConverter.getBytes(_userName.length()));
			fs.write(BitConverter.getBytes(_userName));
			if (!hasDisplay()) {
				fs.write(BitConverter.getBytes(0)); // int32
			} else {
				fs.write(BitConverter.getBytes(_displayName.length()));
				fs.write(BitConverter.getBytes(_displayName));
			}
			fs.write(BitConverter.getBytes(_type));
			fs.write(BitConverter.getBytes(_besty));
		}
		
		/**
		 * Get the friend type as an enum
		 * @return The FriendType enum
		 */
		public FriendType getFriendType() {
			return _friendType;
		}
	
		/**
		 * Check whether the contact is a best friend
		 * @return True if the contact is a best friend, false if not
		 */
		public boolean isBesty() {
			return _besty;
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
