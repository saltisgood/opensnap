package com.nickstephen.opensnap.main;

import java.lang.ref.SoftReference;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.Contacts;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.gui.Theme;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.IRefresh;
import com.nickstephen.opensnap.util.tasks.FriendTask;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;

/**
 * A ListFragment extension that displays all the user's current Friends (contacts)
 * @author Nick's Laptop
 */
public class ContactViewerListFrag extends ListFragment implements IRefresh, IOnObjectReady<Contacts> {
	private Theme theme;
	private SoftReference<BitmapDrawable> drawable;

	public ContactViewerListFrag() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Broadcast.registerContactViewer(this);
    }

    @SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		ListView v = (ListView)inflater.inflate(R.layout.contact_list);
		
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
		
		Contacts.getInstanceSafe(this);
		
		return v;
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.getListView().setOnItemClickListener(mListClickL);
        this.getListView().setOnItemLongClickListener(mListLongClickL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Broadcast.unregisterContactViewer();
    }

    @Override
    public void refresh() {
        ArrayAdapter adapter;
        if ((adapter = (ArrayAdapter)this.getListAdapter()) != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void objectReady(Contacts obj) {
        this.setListAdapter(new ContactViewListAdapter(this.getActivity()));
    }

    private final AdapterView.OnItemLongClickListener mListLongClickL = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int contactPosition, long id) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ContactViewerListFrag.this.getActivity());
            builder.setTitle(Contacts.getInstanceUnsafe().getDisplayOrUserName(contactPosition));
            builder.setItems(R.array.contact_options_dialog, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    switch (item) {
                        case 0: // Change Display Name
                            if (!StatMethods.isNetworkAvailable(ContactViewerListFrag.this.getActivity(), false)) {
                                StatMethods.hotBread(ContactViewerListFrag.this.getActivity(),
                                        "Must be connected to a network to do this", Toast.LENGTH_SHORT);
                                return;
                            }

                            dialog.dismiss();

                            AlertDialog.Builder build = new
                                    AlertDialog.Builder(ContactViewerListFrag.this.getActivity());
                            build.setTitle("Enter the new display name:");
                            build.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                            View contentView = ContactViewerListFrag.this.getLayoutInflater()
                                    .inflate(R.layout.input_dialog_layout);
                            final EditText displayName = (EditText)contentView.findViewById(R.id.input_text);

                            build.setView(contentView);
                            build.setPositiveButton("Change name", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!StatMethods.isNetworkAvailable(ContactViewerListFrag.this.getActivity(), false)) {
                                        StatMethods.hotBread(ContactViewerListFrag.this.getActivity(),
                                                "Must be connected to a network to do this", Toast.LENGTH_SHORT);
                                        return;
                                    }
                                    String newName = displayName.getText().toString();
                                    if (StatMethods.IsStringNullOrEmpty(newName)) {
                                        StatMethods.hotBread(ContactViewerListFrag.this.getActivity(),
                                                "New display name is empty!", Toast.LENGTH_SHORT);
                                        return;
                                    }

                                    dialog.dismiss();
                                    new FriendTask(ContactViewerListFrag.this.getActivity(),
                                            GlobalVars.getUsername(ContactViewerListFrag.this.getActivity()),
                                            Contacts.getInstanceUnsafe().getUsernameAt(contactPosition), newName,
                                            FriendTask.FriendAction.DISPLAY)
                                            .execute();
                                }
                            });
                            build.create().show();

                            break;
                        case 1: // Delete
                            build = new AlertDialog.Builder(ContactViewerListFrag.this.getActivity());
                            String friend = Contacts.getInstanceUnsafe().getDisplayOrUserName(contactPosition);
                            build.setTitle("Really???")
                                    .setMessage("Are you sure you want to delete " + friend + " from your friends list?")
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            new FriendTask(ContactViewerListFrag.this.getActivity(),
                                                    GlobalVars.getUsername(ContactViewerListFrag.this.getActivity()),
                                                    Contacts.getInstanceUnsafe().getUsernameAt(contactPosition),
                                                    FriendTask.FriendAction.DELETE).execute();
                                        }
                                    })
                                    .create().show();
                        case 2: // Block
                            build = new AlertDialog.Builder(ContactViewerListFrag.this.getActivity());
                            friend = Contacts.getInstanceUnsafe().getDisplayOrUserName(contactPosition);
                            build.setTitle("Really???")
                                    .setMessage("Are you sure you want to block " + friend + "?")
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            new FriendTask(ContactViewerListFrag.this.getActivity(),
                                                    GlobalVars.getUsername(ContactViewerListFrag.this.getActivity()),
                                                    Contacts.getInstanceUnsafe().getUsernameAt(contactPosition),
                                                    FriendTask.FriendAction.BLOCK).execute();
                                        }
                                    })
                                    .create().show();
                            break;
                        // Ignore cancel
                    }
                }
            });
            builder.create().show();

            return true;
        }
    };

    private final AdapterView.OnItemClickListener mListClickL = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SnapThreadListFrag thread = new SnapThreadListFrag();
            Bundle args = new Bundle();
            args.putString("username", Contacts.getInstanceUnsafe().getUsernameAt(position));
            thread.setArguments(args);

            ContactViewerListFrag.this.getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out, R.anim.push_right_in, R.anim.push_right_out)
                    .add(R.id.launch_container, thread, SnapThreadListFrag.FRAGTAG).addToBackStack(null).commit();
        }
    };

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
			return Contacts.getInstanceUnsafe().getNumContacts();
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
			contactText.setText(Contacts.getInstanceUnsafe().getDisplayOrUserName(position));
			if (Contacts.getInstanceUnsafe().isBesty(position)) {
				contactText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.star, 0);
			}
			
			TextView displayText = (TextView)v.findViewById(R.id.contact_displaytext);
			if (Contacts.getInstanceUnsafe().hasDisplay(position)) {
				displayText.setText(Contacts.getInstanceUnsafe().getUsernameAt(position));
			}
			
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
