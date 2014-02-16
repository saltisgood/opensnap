package com.nickstephen.opensnap.main;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.TextView;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView.OnEditorActionListener;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;

/**
 * An extension of Fragment that is the very first visible fragment in the program.
 * Simply contains the login and password edittext fields as well as a progress bar
 * and a login button.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class LaunchFrag extends Fragment {
	/**
	 * The fragment tag used to uniquely describe this Fragment.
	 * Normally called upon when querying a FragmentManager
	 */
	public static final String FRAGTAG = "LaunchFragger";
	
	/**
	 * The login button. Held here so it can be called when the user presses enter on the
	 * soft keyboard.
	 */
	private Button mLoginButton;
	
	public LaunchFrag() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		View v = inflater.inflate(R.layout.launch_frag, container, false);
		
		EditText txt = (EditText)v.findViewById(R.id.loginEdit);
		txt.setText(GlobalVars.getUsername(getActivity()), TextView.BufferType.EDITABLE);
		txt = (EditText)v.findViewById(R.id.pwordEdit);
		txt.setText(GlobalVars.getPassword(getActivity()), TextView.BufferType.EDITABLE);
		txt.setOnEditorActionListener(EditorListener);
		mLoginButton = (Button)v.findViewById(R.id.button1);
		
		return v;
	}
	
	/**
	 * A EditorListener that is called whenever the user interacts with the soft keyboard 
	 * in the password edittext field. This is only really for pressing the login button
	 * from within the soft keyboard.
	 */
	OnEditorActionListener EditorListener = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				if (mLoginButton == null) {
					mLoginButton = (Button)LaunchFrag.this.getActivity().findViewById(R.id.button1);
				}
				
				mLoginButton.performClick();
				InputMethodManager inputManager = (InputMethodManager)LaunchFrag.this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(LaunchFrag.this.getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				return true;
			}
			if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
				mLoginButton.performClick();
				return true;
			}
			return false;
		}
	};
}