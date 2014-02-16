package com.nickstephen.opensnap.main;

import static com.nickstephen.opensnap.preview.PreviewConstants.PATH_KEY;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.bgtasks.LazyThumbLoader;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.LocalSnaps.LocalSnap;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;
import com.nickstephen.opensnap.gui.Theme;
import com.nickstephen.opensnap.preview.MediaPreview;
import com.nickstephen.opensnap.preview.VideoPreview;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.tasks.LazyImageLoader;
import com.nickstephen.opensnap.util.tasks.OpenSnapTask;

/**
 * An extension to {@link ListFragment} that displays a list of snaps between 
 * the user and another contact in a thread.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class SnapThreadListFrag extends ListFragment {
	/**
	 * The TAG of this {@link Fragment} that is used for querying {@link FragmentManager} and stuff 
	 */
	public static final String FRAGTAG = "SnapThreadListFrag";
	
	/**
	 * The username of the other contact
	 */
	private String mUsername = null;
	private Theme mTheme;
	private SoftReference<BitmapDrawable> mBackgroundDrawable;
	
	public SnapThreadListFrag() {}
	
	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
		setFocused(true);
		
		ListView v = (ListView)inflater.inflate(R.layout.contact_list, root, false);
		if (LocalSnaps.init(this.getActivity())) {
			this.setListAdapter(new SnapThreadAdapter(this.getActivity(), R.layout.contact_thread_sent, LocalSnaps.getContactSnaps(getUser())));
		} else {
			StatMethods.hotBread(this.getActivity(), "Error initialising snap list", Toast.LENGTH_LONG);
		}
		
		switch (mTheme = SettingsAccessor.getThemePref(this.getActivity())) {
		case ori:
		case snapchat:
			break;
		case black:
		case def:
		default:
			BitmapDrawable draw;
			if (mBackgroundDrawable == null || (draw = mBackgroundDrawable.get()) == null) {
				draw = (BitmapDrawable) this.getActivity().getResources().getDrawable(R.drawable.main_menu_default_background);
				draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
				mBackgroundDrawable = new SoftReference<BitmapDrawable>(draw);
			}
			if (Build.VERSION.SDK_INT < 16) {
				v.setBackgroundDrawable(draw);
			} else {
				v.setBackground(draw);
			}
			v.setCacheColorHint(0x0);
			break;
	}
		
		return v;
	}
	
	/**
	 * A simple abstraction to {@link #mUsername} that can get the mUsername from the arguments passed
	 * to the fragment 
	 * @return The mUsername
	 */
	private String getUser() {
		if (mUsername == null) {
			mUsername = this.getArguments().getString("username");
		}
		return mUsername;
	}
	
	/**
	 * An extension to {@link ArrayAdapter} that interfaces with a list of {@link LocalSnaps.LocalSnap}
	 * to provide a list of items to the {@link SnapThreadListFrag}
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public class SnapThreadAdapter extends ArrayAdapter<LocalSnaps.LocalSnap> {
		List<LocalSnaps.LocalSnap> mSnaps = null;
		private LayoutInflater mInflater;
		
		public SnapThreadAdapter(Context context, int resource, List<LocalSnaps.LocalSnap> snaps) {
			super(context, resource);
			mSnaps = snaps;
		}
		
		@Override
		public int getCount() {
			return mSnaps.size() + 1;
		}
		
		@SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
		@Override
		public View getView(int viewPosition, View convertView, ViewGroup parent) {
			if (mSnaps == null) {
				mSnaps = LocalSnaps.getContactSnaps(SnapThreadListFrag.this.getUser());
			}
			if (mInflater == null) {
				mInflater = (LayoutInflater)SnapThreadListFrag.this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			}
			
			if (viewPosition == 0) {
				View v = mInflater.inflate(R.layout.list_header_button);
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						ComposerMenuFrag menu = new ComposerMenuFrag();
						Bundle args = new Bundle();
						args.putString(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUser());
						menu.setArguments(args);
						SnapThreadListFrag.this.getFragmentManager().beginTransaction()
							.setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out, R.anim.push_up_in, R.anim.push_up_out)
							.add(R.id.launch_container, menu, ComposerMenuFrag.FRAG_TAG).addToBackStack(null).commit();
					}
				});
				return v;
			}
			
			final int position = viewPosition - 1;
			
			LocalSnap snap = mSnaps.get(position);
			
			View v;
			if (snap.getSent()) { 
				v = mInflater.inflate(R.layout.contact_thread_sent, parent, false);
				ImageView img = (ImageView)v.findViewById(R.id.imageView1);
				
				String imgPath = null, thumbPath = null;
				try {
					imgPath = snap.getSnapPath(this.getContext());
					thumbPath = snap.getSnapThumbPath();
				} catch (IOException e) {
				}
				
				if (snap.isDownloading()) {
					LayoutParams params = img.getLayoutParams();
					ProgressBar pb = new ProgressBar(this.getContext());
					pb.setVisibility(View.VISIBLE);
					((RelativeLayout)v).addView(pb, params);
					img.setVisibility(View.GONE);
					Twig.debug("SnapThreadListFrag", "IsDownloading catch");
				} else if (snap.getSnapThumbExists()) {
					if (thumbPath != null) {
						new LazyThumbLoader(img).execute(thumbPath);
					} else {
						img.setImageResource(R.drawable.cancel);
					}
				} else if (snap.getSnapExists(this.getContext())) {
					if (imgPath != null) {
						new LazyImageLoader(img, snap.isPhoto()).execute(imgPath, thumbPath);
					} else {
						img.setImageResource(R.drawable.cancel);
					}
				} else {
					img.setImageResource(R.drawable.cancel);
				}
				/*
				if (snap.getSnapExists(this.getContext())) {
					if (snap.isPhoto()) {
						String imgPath = null;
						try {
							imgPath = snap.getSnapPath();
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (imgPath != null) {
							try {
								new LazyImageLoader(img, true).execute(new String[] {imgPath, 
										snap.getSnapThumbPath(SnapThreadListFrag.this.getActivity())});
							} catch (IOException e) {
								e.printStackTrace();
								img.setImageResource(R.drawable.cancel);
							}
						}
					} else {
						//img.setImageResource(R.drawable.clapper);
						try {
							new LazyImageLoader(img, false).execute(new String[] { snap.getSnapPath(), 
									snap.getSnapThumbPath(getActivity()) });
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					img.setImageResource(R.drawable.cancel);
				} */
				TextView txt = (TextView)v.findViewById(R.id.sent_text);
				txt.setText("Sent: " + snap.getReadableSentTimeStamp());
			} else {
				v = mInflater.inflate(R.layout.contact_thread_receive, parent, false);
				ImageView img = (ImageView)v.findViewById(R.id.receive_image);
				
				String imgPath = null, thumbPath = null;
				try {
					imgPath = snap.getSnapPath(this.getContext());
					thumbPath = snap.getSnapThumbPath();
				} catch (IOException e) {
				}
				
				if (snap.getSnapThumbExists()) {
					if (thumbPath != null) {
						new LazyThumbLoader(img).execute(thumbPath);
					} else {
						img.setImageResource(R.drawable.cancel);
					}
				} else if (snap.getSnapExists(this.getContext())) {
					if (imgPath != null) {
						new LazyImageLoader(img, snap.isPhoto()).execute(imgPath, thumbPath);
					} else {
						img.setImageResource(R.drawable.cancel);
					}
				} else if (snap.getSnapAvailable()) {
					// TODO: implement downloading
					Twig.debug("SnapThreadListFrag", "Snap available but not implemented");
				} else {
					img.setImageResource(R.drawable.cancel);
				}
				
				/*
				if (snap.getSnapExists(this.getContext())) {
					if (snap.isPhoto()) {
						String imgPath = null;
						try {
							imgPath = snap.getSnapPath();
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (imgPath != null) {
							try {
								new LazyImageLoader(img, true).execute(new String[] {imgPath, snap.getSnapThumbPath(SnapThreadListFrag.this.getActivity())});
							} catch (IOException e) {
								e.printStackTrace();
								img.setImageResource(R.drawable.cancel);
							}
						}
					} else {
						//img.setImageResource(R.drawable.clapper);
						try {
							new LazyImageLoader(img, false).execute(new String[] { snap.getSnapPath(),
									snap.getSnapThumbPath(getActivity()) });
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					img.setImageResource(R.drawable.cancel);
				} */
				TextView txt = (TextView)v.findViewById(R.id.receive_text);
				txt.setText("Received: " + snap.getReadableSentTimeStamp());
			}
			
			switch (mTheme) {
				case ori:
				case snapchat:
					break;
				case black:
				case def:
				default:
					BitmapDrawable draw;
					if (mBackgroundDrawable == null || (draw = mBackgroundDrawable.get()) == null) {
						draw = (BitmapDrawable) SnapThreadListFrag.this.getActivity().getResources().getDrawable(R.drawable.main_menu_default_background);
						draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
						mBackgroundDrawable = new SoftReference<BitmapDrawable>(draw);
					}
					if (Build.VERSION.SDK_INT < 16) {
						v.setBackgroundDrawable(draw);
					} else {
						v.setBackground(draw);
					}
					break;
			}
			
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (mSnaps == null) {
						mSnaps = LocalSnaps.getContactSnaps(SnapThreadListFrag.this.getUser());
					}
					
					LocalSnap snap = mSnaps.get(position);
					
					boolean allowSaves;
					if (!(allowSaves = SettingsAccessor.getAllowSaves(getActivity())) && snap.isOpened()) {
						StatMethods.hotBread(getActivity(), "Sorry! You need save privileges to view saved images", Toast.LENGTH_SHORT);
						return;
					}
					if (!snap.isOpened()) {
						new OpenSnapTask(getActivity(), snap, GlobalVars.getUsername(getActivity())).execute(new String[0]);
					}
					if (snap.isPhoto()) {
						if (allowSaves && SettingsAccessor.getExternalPreviewPref(getActivity())) {
							Intent photoViewIntent = new Intent(Intent.ACTION_VIEW);
							try {
								Uri tempuri = Uri.fromFile(new File(snap.getSnapPath(SnapThreadListFrag.this.getActivity())));
								photoViewIntent.setDataAndType(tempuri, "image/*");
							} catch (IOException e) {
								e.printStackTrace();
								StatMethods.hotBread(SnapThreadListFrag.this.getActivity(), "Error doing something.... blergh!", Toast.LENGTH_LONG);
								return;
							}
							SnapThreadListFrag.this.getActivity().startActivityFromFragment(SnapThreadListFrag.this, photoViewIntent, -1);
						} else {
							Intent intent = new Intent(SnapThreadListFrag.this.getActivity(), MediaPreview.class);
							try {
								intent.putExtra(PATH_KEY, snap.getSnapPath(SnapThreadListFrag.this.getActivity()));
							} catch (IOException e) {
								e.printStackTrace();
								StatMethods.hotBread(SnapThreadListFrag.this.getActivity(), "Error doing something else... beep bep", Toast.LENGTH_LONG);
								return;
							}
							SnapThreadListFrag.this.getActivity().startActivity(intent);
						}
					} else {
						if (allowSaves && SettingsAccessor.getExternalPreviewPref(getActivity())) {
							Intent videoViewIntent = new Intent(Intent.ACTION_VIEW);
							try {
								Uri tempuri = Uri.fromFile(new File(snap.getSnapPath(SnapThreadListFrag.this.getActivity())));
								videoViewIntent.setDataAndType(tempuri, "video/*");
							} catch (IOException e) {
								e.printStackTrace();
								StatMethods.hotBread(getActivity(), "Error doing something... blergh!", Toast.LENGTH_LONG);
								return;
							}
							SnapThreadListFrag.this.getActivity().startActivityFromFragment(SnapThreadListFrag.this, videoViewIntent, -1);
						} else {
							Intent vidIntent = new Intent(SnapThreadListFrag.this.getActivity(), VideoPreview.class);
							try {
								vidIntent.putExtra(PATH_KEY, snap.getSnapPath(SnapThreadListFrag.this.getActivity()));
							} catch (IOException e) {
								e.printStackTrace();
								StatMethods.hotBread(getActivity(), "Error doing something else... beep bep", Toast.LENGTH_LONG);
								return;
							}
							SnapThreadListFrag.this.getActivity().startActivity(vidIntent);
						}
					}
				}
			});
			
			return v;
		}
	}
}
