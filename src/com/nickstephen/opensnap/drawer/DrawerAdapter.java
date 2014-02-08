package com.nickstephen.opensnap.drawer;


import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.BaseExpandableListAdapter;
import org.holoeverywhere.widget.TextView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;

public class DrawerAdapter extends BaseExpandableListAdapter {
	final private Context context;
	
	public DrawerAdapter(Context ctxt) {
		context = ctxt;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		if (groupPosition != 1)
			return null;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.drawer_subitem, null);
		}
		TextView txt = (TextView) convertView.findViewById(R.id.drawer_text);
		ImageView img = (ImageView)convertView.findViewById(R.id.drawer_icon);
		switch (childPosition) {
			case 0:
				txt.setText("Take a Picture");
				img.setImageResource(R.drawable.ic_action_camera);
				break;
			case 1:
				txt.setText("Pick a Picture");
				img.setImageResource(R.drawable.ic_action_picture);
				break;
			case 2:
				txt.setText("Take a Video");
				img.setImageResource(R.drawable.ic_action_video);
				break;
			case 3:
				txt.setText("Resend");
				img.setImageResource(R.drawable.ic_action_forward);
				break;
			default:
				txt.setText("blererelghgh");
				break;
		}
		
		convertView.setEnabled(GlobalVars.isLoggedIn(context));
		
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		switch (groupPosition) {
			case 1:
				return 4;
			default:
				return 0;
		}
	}

	@Override
	public Object getGroup(int groupPosition) {
		return null;
	}

	@Override
	public int getGroupCount() {
		return 7;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,	View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.drawer_item, null);
		}
		TextView txt = (TextView) convertView.findViewById(R.id.drawer_text);
		ImageView img = (ImageView)convertView.findViewById(R.id.drawer_icon);
		
		switch (groupPosition) {
		case 0:
			txt.setText("Home");
			img.setImageResource(R.drawable.ic_menu_home);
			break;
		case 1:
			txt.setText("New Snap");
			if (!isExpanded) {
				img.setImageResource(R.drawable.ic_action_expand);
			} else {
				img.setImageResource(R.drawable.ic_action_collapse);
			}
			break;
		case 2:
			txt.setText("Snaps");
			img.setImageResource(R.drawable.ic_action_previous_item);
			break;
		case 3:
			txt.setText("Contacts");
			img.setImageResource(R.drawable.ic_action_next_item);
			break;
		case 4:
			txt.setText("Settings");
			img.setImageResource(R.drawable.cog_settings);
			break;
		case 5:
			txt.setText("Refresh");
			img.setImageResource(R.drawable.ic_menu_refresh);
			break;
		case 6:
			txt.setText("Sign Out");
			img.setImageResource(R.drawable.ic_action_back);
			break;
		default:
			txt.setText("dfkjdkjd");
			break;
		}
		
		convertView.setEnabled(GlobalVars.isLoggedIn(context));
		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
