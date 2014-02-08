package com.nickstephen.opensnap.main;

import java.lang.ref.SoftReference;

import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.nickstephen.lib.gui.ListFragment;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.gui.Theme;
import com.nickstephen.opensnap.settings.SettingsAccessor;

/**
 * A ListFragment extension that displays all the user's current Friends (contacts)
 * @author Nick's Laptop
 */
public class ContactViewerListFrag extends ListFragment {
	private Theme theme;
	private SoftReference<BitmapDrawable> drawable;

	public ContactViewerListFrag() {}

	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		ListView v = (ListView)inflater.inflate(R.layout.contact_list, container, false);
		
		switch (theme = SettingsAccessor.getThemePref(this.getActivity())) {
			case ori:
			case snapchat:
				break;
			case black:
			case def:
			default:
				BitmapDrawable draw;
				if (drawable == null || (draw = drawable.get()) == null) {
					draw = (BitmapDrawable) this.getActivity().getResources().getDrawable(R.drawable.main_menu_default_background);
					draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
					drawable = new SoftReference<BitmapDrawable>(draw);
				}
				if (Build.VERSION.SDK_INT < 16) {
					v.setBackgroundDrawable(draw);
				} else {
					v.setBackground(draw);
				}
				v.setCacheColorHint(0x0);
				break;
		}
		
		if (Contacts.init(this.getActivity())) {
			ContactViewListAdapter adapter = new ContactViewListAdapter(this.getActivity());
			setListAdapter(adapter);
		} else {
			StatMethods.hotBread(this.getActivity(), "Error initialising Contacts class", Toast.LENGTH_LONG);
		}
		
		return v;
	}
	
	/**
	 * The ArrayAdapter extension that converts Contacts to the ListView items
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public class ContactViewListAdapter extends ArrayAdapter<Contacts.Contact> {
		private LayoutInflater inflater;
		
		public ContactViewListAdapter(Context ctxt) {
			super(ctxt, R.layout.contact_text);
		}
		
		@Override
		public int getCount() {
			return Contacts.getNumContacts();
		}
		
		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (inflater == null) {
				inflater = (LayoutInflater)ContactViewerListFrag.this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			}
			View v = inflater.inflate(R.layout.contact_text, parent, false);
			TextView contactText = (TextView)v.findViewById(R.id.contact_textview);
			contactText.setText(Contacts.getDisplayOrUserName(position));
			if (Contacts.isBesty(position)) {
				contactText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.star, 0);
			}
			
			TextView displayText = (TextView)v.findViewById(R.id.contact_displaytext);
			if (Contacts.hasDisplay(position)) {
				displayText.setText(Contacts.getUsernameAt(position));
			}
			
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					SnapThreadListFrag thread = new SnapThreadListFrag();
					Bundle args = new Bundle();
					args.putString("username", Contacts.getUsernameAt(position));
					thread.setArguments(args);
					
					ContactViewerListFrag.this.getFragmentManager().beginTransaction()
						.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out, R.anim.push_right_in, R.anim.push_right_out)
						.add(R.id.launch_container, thread, SnapThreadListFrag.FRAGTAG).addToBackStack(null).commit();
				}
			});
			
			switch (theme) {
				case ori:
				case snapchat:
					break;
				case def:
				default:
					BitmapDrawable draw;
					if (drawable == null || (draw = drawable.get()) == null) {
						draw = (BitmapDrawable) ContactViewerListFrag.this.getActivity().getResources().getDrawable(R.drawable.main_menu_default_background);
						draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
						drawable = new SoftReference<BitmapDrawable>(draw);
					}
					if (Build.VERSION.SDK_INT < 16) {
						v.setBackgroundDrawable(draw);
					} else {
						v.setBackground(draw);
					}
					contactText.setTextColor(0xFFFFFFFF);
					displayText.setTextColor(0xFFA9A9A9);
					break;
			}
			
			return v;
		}
	}
}
