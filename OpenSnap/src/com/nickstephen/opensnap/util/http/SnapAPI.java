package com.nickstephen.opensnap.util.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.http.IWriteListener;
import com.nickstephen.lib.http.MultipartEntityWithPb;
import com.nickstephen.lib.http.NetException;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.util.misc.CustomJSON;

public class SnapAPI {
	/**
	 * The Secret String token that's mixed with other strings to generate an encrypted 
	 * authorisation token.
	 * @see {@link #createToken(String, String)}
	 */
	private static final String SECRET_STR = "iEk21fuwZApXlz93750dmW22pw389dPwOk";
	/**
	 * The pattern to mix the 2 SHA-256 encrypted strings to generate the authorisation token
	 * @see {@link #createToken(String, String)}
	 */
	private static final String PATTERN = "0001110111101110001111010101111011010001001110011000110001000110";
	/**
	 * 
	 */
	private static final String STATIC_TOKEN = "m198sOkJEn37DjqZ32lpRu76xmw288xSQ9";
	/**
	 * The base url used for SnapChat internet guff
	 */
	private static final String API_URL = "https://feelinsonice.appspot.com";
	
	private static final String CAPTION_TEXT_KEY = "caption_text";
	private static final String CAPTION_ORI_KEY = "caption_orientation";
	private static final String CAPTION_POS_KEY = "caption_position";
	
	public static Bundle postData(String slug, Bundle params, String authToken) throws NetException {
		List<NameValuePair> data = paramsToList(params);
		String tStamp = makeTimeStamp();
		data.add(new BasicNameValuePair("timestamp", tStamp));
		if (authToken != null) {
			data.add(new BasicNameValuePair("req_token", createToken(authToken, tStamp)));
		} else {
			data.add(new BasicNameValuePair("req_token", createStaticToken(tStamp)));
		}
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(API_URL + slug);
		HttpResponse response;
		try {
			post.setEntity(new UrlEncodedFormEntity(data));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new NetException("URL Encoded Error!", NetException.NON_HTTP_RESPONSE_CODE);
		}
		try {
			response = client.execute(post);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			throw new NetException("Internet problemo", NetException.NON_HTTP_RESPONSE_CODE);
		} catch (IOException e) {
			e.printStackTrace();
			throw new NetException("Internet problemo", NetException.NON_HTTP_RESPONSE_CODE);
		} 
		
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			switch (statusCode) {
				case 401:
					//return ServerResponse.getLoggedOut();
					return ServerResponse.get401Code();
				default:
					throw new NetException("Internet problemo", statusCode);
			}
		}
		
		try {
			String result = StatMethods.inputStreamToString(response.getEntity().getContent());
			Bundle vals = new Bundle();
			vals.putString(ServerResponse.RESULT_DATA_KEY, result);
			vals.putInt(ServerResponse.STATUS_CODE_KEY, 200);
			return vals;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new NetException("Response problemo", NetException.NON_HTTP_RESPONSE_CODE);
		} catch (IOException e) {
			e.printStackTrace();
			throw new NetException("Response problemo", NetException.NON_HTTP_RESPONSE_CODE);
		}
	}
	
	private static List<NameValuePair> paramsToList(Bundle params) {
		List<NameValuePair> nvpList = new ArrayList<NameValuePair>(params.size());
		Iterator<String> bundleIterator = params.keySet().iterator();
		while (bundleIterator.hasNext()) {
			String key = bundleIterator.next();
			Object obj = params.get(key);
			if (obj != null) {
				nvpList.add(new BasicNameValuePair(key, obj.toString()));
			}
		}
		return nvpList;
	}
	
	/**
	 * Perform a HTTP POST to one of the SnapChat pages. Returns a CustomJSON object
	 * with the response from the server. Note that some of the responses are for
	 * error reporting.
	 * A "logged":false response means authorisation failed and the user was logged out of the service.
	 * @param data A list of NVPs to use as data to send to the server
	 * @param slug The url extension to add on to the {@link #API_URL}
	 * @param auth_tok An authorisation token to use when sending data. Should not be used for login (optional)
	 * @return A CustomJSON object either as the resopnse from the server or an error state (null is most common error)
	 */
	public static CustomJSON postData(List<NameValuePair> data, String slug, String auth_tok) {
		String tStamp = makeTimeStamp();
		String tok;
		if (auth_tok != null)
			tok = createToken(auth_tok, tStamp);
		else
			tok = createStaticToken(tStamp);
		
		data.add(new BasicNameValuePair("timestamp", tStamp));
		data.add(new BasicNameValuePair("req_token", tok));
		
		HttpClient client = new DefaultHttpClient();
		HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 15000);
        HttpConnectionParams.setSoTimeout(params, 15000);

		HttpPost post = new HttpPost(API_URL + slug);
		HttpResponse response;
		try {
			post.setEntity(new UrlEncodedFormEntity(data));
			response = client.execute(post);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		if (response.getStatusLine().getStatusCode() == 401) { // unauthorised
			try {
			return new CustomJSON("{\"logged\":false}");
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		} else if (response.getStatusLine().getStatusCode() != 200) {
			Twig.error("SnapLib", "Error getting http junk. Code: " + response.getStatusLine().getStatusCode());
			return null;
		}
		
		try {
			String result = StatMethods.inputStreamToString(response.getEntity().getContent());
			if (!StatMethods.IsStringNullOrEmpty(result))
				return new CustomJSON(result);
			else
				return new CustomJSON("{\"empty\":true}");
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			e.printStackTrace();
			Twig.error("JSONException!", e.getMessage());
			return null;
		}
	}

	/**
	 * Get a string representing how many milliseconds since the epoch
	 * @return The string value
	 */
	public static String makeTimeStamp() {
		return Long.valueOf(System.currentTimeMillis()).toString();
	}
	
	/**
	 * Create an authorisation token for calls to the SnapChat server.
	 * It's basically a combination of {@link #SECRET_STR} and the left
	 * and right strings encrypted with the SHA-256 algorithm and then mixed together with {@link #PATTERN}.
	 * @param left The left string
	 * @param right The right string
	 * @return The combined authorisation token
	 */
	public static String createToken(String left, String right) {
		String hex1, hex2;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			hex1 = StatMethods.bytesToHex(digest.digest((SECRET_STR + left).getBytes("UTF-8")));
			hex2 = StatMethods.bytesToHex(digest.digest((right + SECRET_STR).getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		String mesg = "";
		for (int i = 0; i < PATTERN.length(); i++) {
			char c = PATTERN.charAt(i);
			char d;
			if (c != '0')
				d = hex2.charAt(i);
			else
				d = hex1.charAt(i);
			mesg += d;
		}
		return mesg;
	}
	
	/**
	 * Create an authorisation token for calls to the SnapChat server
	 * when there is only one secret token. The function mixes this secret token
	 * with {@link #STATIC_TOKEN} and then calls {@link #createToken(String, String)}.
	 * @param right The one token to use for encryption
	 * @return The encrypted authorisation token
	 */
	public static String createStaticToken(String right) {
		return createToken(STATIC_TOKEN, right);
	}
	
	/**
	 * Helper login method that will attempt to login given a username and password
	 * @param username The username to use for login
	 * @param password The password to use for login
	 * @return The CustomJSON object returned from the server. Should be checked for null
	 * and "logged":false
	 * @see {@link #postData(List, String, String)}
	 */
	public static CustomJSON login(String username, String password) {
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		nvp.add(new BasicNameValuePair("username", username));
		nvp.add(new BasicNameValuePair("password", password));
		return postData(nvp, "/ph/login", null);
	}
	
	/**
	 * Perform an update to the SnapChat server. This is actually equivalent to the
	 * login except that you don't have to change the authorisation tokens afterwards.
	 * @param username The username to use
	 * @param authtoken The authorisation token to use instead of a password
	 * @return The CustomJSON object returned from the server. Like {@link #login(String, String)}
	 * the object should be tested for null and "logged":false.
	 * @see {@link #postData(List, String, String)}
	 */
	public static CustomJSON update(String username, String authtoken) {
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		nvp.add(new BasicNameValuePair("username", username));
		nvp.add(new BasicNameValuePair("json", "{}"));
		return postData(nvp, "/ph/sync", authtoken);
	}

	/**
	 * Get an encrypted data blob from the SnapChat server. Normally used for downloading
	 * pictures and videos.
	 * @param id The ID of the media file for download
	 * @param username The username to use for authorisation
	 * @param authtoken The authorisation token to use
	 * @return The raw byte array of the data file
	 */
	public static byte[] getBlob(String id, String username, String authtoken, IWriteListener wListener, int flags)
		throws SnapGoneException {
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		String tstamp = makeTimeStamp();
		nvp.add(new BasicNameValuePair("id", id));
		nvp.add(new BasicNameValuePair("username", username));
		nvp.add(new BasicNameValuePair("timestamp", tstamp));
		nvp.add(new BasicNameValuePair("req_token", createToken(authtoken, tstamp)));
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(API_URL + "/ph/blob");
		
		HttpResponse response;
		
		try {
			post.setEntity(new UrlEncodedFormEntity(nvp));
			response = client.execute(post);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} 
		
		if (response.getStatusLine().getStatusCode() == 410) {
			throw new SnapGoneException("Resource missing code from server!");
		} else if (response.getStatusLine().getStatusCode() != 200) {
			return null;
		}
		
		wListener.setLength(response.getEntity().getContentLength());
		
		try {
			return StatMethods.inputStreamToBytes(response.getEntity().getContent(), wListener, flags);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * The key to use for decryption/encryption of data
	 * @see {@link #DecryptBlob(String, String, String)} 
	 */
	private static final String AESKEY1 = "M02cnQ51Ji97vwT4";
	
	/**
	 * Download and decrypt a data file from the SnapChat server
	 * @param id The ID of the media file to download
	 * @param username The username to use for authorisation
	 * @param authtoken The authorisation token for authorisation
	 * @return The decrypted data array
	 */
	public static byte[] decryptBlob(String id, String username, String authtoken, IWriteListener wListener, int flags)
		throws SnapGoneException {
		byte[] buff = getBlob(id, username, authtoken, wListener, flags);
		
		if (buff == null) {
			return null;
		}
		
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		}
		SecretKeySpec key = new SecretKeySpec(AESKEY1.getBytes(), "AES");
		try {
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getEncoded(), "AES"));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		}
		byte[] result;
		try {
			result = cipher.doFinal(buff);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		}
		
		buff = null;
		
		return result;
	}

	/**
	 * Encrypt a file, write it to another file and return a return a reference to the
	 * created file. 
	 * @param input The input file to be encrypted
	 * @param ctxt The context to use to locate the program file directory
	 * @return The encrypted file reference
	 */
	public static File encryptFile(File input, Context ctxt) {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		SecretKeySpec key = new SecretKeySpec(AESKEY1.getBytes(), "AES");
		try {
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getEncoded(), "AES"));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		}
		File output = new File(ctxt.getFilesDir(), "tmp.bin");
		try {
			FileOutputStream fs = new FileOutputStream(output);
			FileInputStream fs2 = new FileInputStream(input);
			byte[] buff = new byte[1000];
			int status;
			while ((status = fs2.read(buff)) != -1){
				byte[] buff2 = cipher.update(buff, 0, status);
				if (buff2 != null)
					fs.write(buff2);
				//fs.write(cipher.update(buff, 0, status));
			}
			fs.write(cipher.doFinal());
			fs.close();
			fs2.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return output;
	}
	
	/**
	 * Upload a file to the SnapChat server. Used normally for uploading photos and videos.
	 * The network status should be checked before calling.
	 * @param ctxt The context to use
	 * @param path The path to the file to upload
	 * @param username The username to use for authorisation
	 * @param authtoken The authorisation token to use for authorisation
	 * @param mediaType The media type of the file (enum)
	 * @return The media ID string
	 * @throws Exception Exceptions can be if the input file is not found, or encryption fails, or the upload fails
	 */
	@SuppressLint("DefaultLocale")
	public static String uploadFile(Context ctxt, String path, String username, String authtoken, Integer mediaType) throws Exception {
		String tstamp = makeTimeStamp();
		String tok = createToken(authtoken, tstamp);
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(API_URL + "/ph/upload");
		
		File input = new File(path);
		if (!input.exists()) {
			throw new FileNotFoundException("File missing or not readable");
		}
		File output = encryptFile(input, ctxt);
		if (output == null) {
			throw new Exception("Encryption failed");
		}
		
		HttpResponse response;
		try {
			MultipartEntity multipart = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			multipart.addPart("username", new StringBody(username));
			multipart.addPart("media_id", new StringBody(username.toUpperCase() + tstamp));
			multipart.addPart("timestamp", new StringBody(tstamp));
			multipart.addPart("req_token", new StringBody(tok));
			multipart.addPart("type", new StringBody(mediaType.toString()));
			multipart.addPart("data", new FileBody(output));
			post.setEntity(multipart);
			
			response = client.execute(post);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("POST failed");
		}
		
		if (response.getStatusLine().getStatusCode() != 200)
			throw new Exception("Return status not OK: " + response.getStatusLine().getStatusCode());
		
		return username.toUpperCase() + tstamp;
	}
	
	@SuppressLint("DefaultLocale")
	public static String uploadFile(Context ctxt, String path, String username, String authtoken, 
			Integer mediaType, IWriteListener writeListener, int flags) throws Exception {
		String tstamp = makeTimeStamp();
		String tok = createToken(authtoken, tstamp);
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(API_URL + "/ph/upload");
		
		File input = new File(path);
		if (!input.exists()) {
			throw new FileNotFoundException("File missing or not readable");
		}
		File output = encryptFile(input, ctxt);
		if (output == null) {
			throw new Exception("Encryption failed");
		}
		
		HttpResponse response;
		try {
			MultipartEntityWithPb multipart = new MultipartEntityWithPb(HttpMultipartMode.BROWSER_COMPATIBLE, writeListener, flags);
			multipart.addPart("username", new StringBody(username));
			multipart.addPart("media_id", new StringBody(username.toUpperCase() + tstamp));
			multipart.addPart("timestamp", new StringBody(tstamp));
			multipart.addPart("req_token", new StringBody(tok));
			multipart.addPart("type", new StringBody(mediaType.toString()));
			multipart.addPart("data", new FileBody(output));
			post.setEntity(multipart);
			writeListener.setLength(multipart.getContentLength());
			
			response = client.execute(post);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("POST failed");
		}
		
		if (response.getStatusLine().getStatusCode() != 200)
			throw new Exception("Return status not OK: " + response.getStatusLine().getStatusCode());
		
		return username.toUpperCase() + tstamp;
	}
	
	/**
	 * Send a message to the SnapChat server to send the (already uploaded) file to a
	 * recipient/s. The mediaID parameter should be the result from calling 
	 * {@link #uploadFile(Context, String, String, String, Integer)}
	 * @param mediaID The ID of the media file that's already been uploaded
	 * @param username The username to use for authorisation
	 * @param authToken The authorisation token to use for authorisation
	 * @param target The recipient/s of the Snap
	 * @param timeToDisplay The time to display the Snap
	 * @param extras For use with video snaps. extras[0] = caption text. extras[1] = caption mCameraOrientation. extras[2] = caption position.
	 * @return A status value. -1 indicates an error
	 */
	public static int sendFile(String mediaID, String username, String authToken, String target, Integer timeToDisplay, String... extras) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", username));
		nvps.add(new BasicNameValuePair("media_id", mediaID));
		nvps.add(new BasicNameValuePair("recipient", target));
		nvps.add(new BasicNameValuePair("time", timeToDisplay.toString()));
		if (extras != null && extras.length >= 3) {
			if (extras.length >= 1 && extras[0] != null) {
				nvps.add(new BasicNameValuePair(CAPTION_TEXT_KEY, extras[0]));
			}
			if (extras.length >= 2 && extras[1] != null) {
				nvps.add(new BasicNameValuePair(CAPTION_ORI_KEY, extras[1]));
			}
			if (extras.length >= 3 && extras[2] != null) {
				nvps.add(new BasicNameValuePair(CAPTION_POS_KEY, extras[2]));
			}
		}
		
		CustomJSON rtVal = postData(nvps, "/ph/send", authToken);
		if (rtVal == null)
			return -1;
		return 0;
	}
	
	public static int markAsOpened(String mediaID, String username, String authToken) {
		String tStamp = makeTimeStamp();
		String tok = createToken(authToken, tStamp);
		
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new BasicNameValuePair("timestamp", tStamp));
		data.add(new BasicNameValuePair("username", username));
		data.add(new BasicNameValuePair("req_token", tok));
		data.add(new BasicNameValuePair("json", "{\"" + mediaID + "\":{\"c\":0,\"t\":0}}"));
		
		HttpClient client = new DefaultHttpClient();
		
		HttpPost post = new HttpPost(API_URL + "/ph/sync");
		HttpResponse response;
		try {
			post.setEntity(new UrlEncodedFormEntity(data));
			response = client.execute(post);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return -3;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return -3;
		} catch (IOException e) {
			e.printStackTrace();
			return -3;
		}
		
		if (response.getStatusLine().getStatusCode() == 401) {
			return -4;
		} else if (response.getStatusLine().getStatusCode() != 200) {
			return -5;
		} else {
			return 0;
		}
	}

	/**
	 * Send a message to the SnapChat server to mark a Snap as opened
	 * @param mediaID The ID of the media file
	 * @param pCtxt The context to use for whatever
	 * @return Status value. -1 indicates error. 0 on success.
	 */
	public static int markAsOpened(String mediaID, Context pCtxt) {
		return markAsOpened(mediaID, GlobalVars.getUsername(pCtxt), GlobalVars.getAuthToken(pCtxt));
	}

	public static int clearFeed(String username, String authtoken) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", username));
		CustomJSON result = postData(nvps, "/ph/clear", authtoken);
		
		if (result == null) {
			return -1;
		}
		return 0;
	}
}
