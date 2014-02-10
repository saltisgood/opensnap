package com.nickstephen.opensnap.main;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.util.SparseArray;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.gui.ListFragment;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.LocalSnaps.LocalSnap;
import com.nickstephen.opensnap.global.TempSnaps;
import com.nickstephen.opensnap.global.TempSnaps.TempSnap;
import com.nickstephen.opensnap.gui.Theme;
import com.nickstephen.opensnap.preview.MediaPreview;
import com.nickstephen.opensnap.preview.PreviewConstants;
import com.nickstephen.opensnap.preview.VideoPreview;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.notify.Notifications;
import com.nickstephen.opensnap.util.tasks.OpenSnapTask;
import com.nickstephen.opensnap.util.tasks.SnapDownload;
import com.nickstephen.opensnap.util.tasks.SnapUpload;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.AbcDefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * An extension to {@link ListFragment} that displays a list of Snap info guff that
 * can then be clicked on to view more detailed stuff
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class SnapViewerListFrag extends ListFragment {
	public static final String FRAGTAG = "SnapViewerListFrag";
	
	private static final int RETURN_AND_DELETE = 0x100;
	
	private SparseArray<ValueAnimator> mActiveAnims;
	private Theme mTheme;
	private SoftReference<BitmapDrawable> mDrawable;
	private final GuiHandler mHandler = new GuiHandler();
    private PullToRefreshLayout mPTRLayout;
	
	public SnapViewerListFrag() {
		mActiveAnims = new SparseArray<ValueAnimator>();
	}

    @SuppressLint("NewApi")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTheme = SettingsAccessor.getThemePref(this.getActivity());

        // View created by list frag
        ViewGroup viewGroup = (ViewGroup) view;

        // Create PTR layout manually
        mPTRLayout = new PullToRefreshLayout(viewGroup.getContext());

        // Setup PTR
        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .theseChildrenArePullable(getListView(), getListView().getEmptyView())
                .options(new Options.Builder().headerTransformer(new AbcDefaultHeaderTransformer())
                        .refreshOnUp(true).scrollDistance(0.5f).build())
                .listener(new OnRefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        ((LaunchActivity) SnapViewerListFrag.this.getActivity()).update(new BGFinishedCallback() {
                            @Override
                            public void onCompleted() {
                                mPTRLayout.setRefreshComplete();
                            }
                        });
                    }
                })
                .setup(mPTRLayout);

        if (LocalSnaps.init(this.getActivity())) {
            this.setListAdapter(new SnapViewListAdapter(this.getActivity()));
        } else {
            StatMethods.hotBread(this.getActivity(), "Error initialising the snap list", Toast.LENGTH_LONG);
        }

        this.setRetainInstance(true);

        if (Build.VERSION.SDK_INT > 11) {
            this.getListView().setLayoutTransition(new LayoutTransition());
        }

        this.getListView().setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, final View view, int position, long id) {
                Object snap = view.getTag(R.id.snap_item);
                if (snap instanceof TempSnap) {
                    tempSnapClick((TempSnap) snap, position);
                } else {
                    localSnapClick((LocalSnap) snap, position);
                }
            }
        });
        this.getListView().setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, final int position, long id) {
                Object obj = view.getTag(R.id.snap_item);
                if (obj instanceof TempSnap) {
                    final TempSnap snap = (TempSnap) obj;
                    if (snap.isSending()) {
                        StatMethods.hotBread(SnapViewerListFrag.this.getActivity(), "Snap still sending...", Toast.LENGTH_SHORT);
                    } else if (snap.isSent()) {
                        StatMethods.QuestionBox(SnapViewerListFrag.this.getActivity(), "Remove?",
                                "This snap has been sent. Do you wish to remove it from the list?",
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        TempSnaps.remove(SnapViewerListFrag.this.getActivity(), snap);
                                        refreshList();
                                    }
                                }, null);
                    } else {
                        StatMethods.QuestionBox(SnapViewerListFrag.this.getActivity(), "Remove?",
                                "This snap hasn't been sent yet. Are you sure you want to remove it?",
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                       TempSnaps.remove(SnapViewerListFrag.this.getActivity(), snap);
                                        refreshList();
                                    }
                                }, null);
                    }
                } else {
                    final LocalSnap snap = (LocalSnap) obj;
                    int pos = (Integer) view.getTag(R.id.snap_item_position);
                    if (LocalSnaps.getSnapId(pos).compareTo(snap.getSnapId()) != 0) {
                        refreshList();
                    } else {
                        Intent intent = new Intent(SnapViewerListFrag.this.getActivity(), SnapDialogActivity.class);
                        intent.putExtra(SnapDialogActivity.SNAP_NO, pos);
                        SnapViewerListFrag.this.startActivity(intent);
                    }
                }
                return true;
            }
        });
    }

	private void localSnapListClick(int snapId, View view) {
		LocalSnap snap = LocalSnaps.getSnapAt(snapId);
		boolean allowSaves = SettingsAccessor.getAllowSaves(this.getActivity()), isPhoto = snap.isPhoto(),
			isOpened = snap.isOpened();
		String path;
		try {
			path = snap.getSnapPath(this.getActivity());
		} catch (IOException e) {
			Twig.warning("SnapViewerListFrag", "External storage not available");
			StatMethods.hotBread(this.getActivity(), "External storage not available! Connect storage and retry!", Toast.LENGTH_LONG);
			return;
		}
		if (snap.isDownloading()) {
			// Is currently downloading, ignore click
		} else if (snap.getSnapExists(this.getActivity())) {
			// Not downloading and exists. Good.
			if (allowSaves || !isOpened) {
				// If saves are allowed you can re-open it. Otherwise make sure it's not already opened
				Intent viewIntent;
				if (SettingsAccessor.getExternalPreviewPref(this.getActivity())) {
					// Want external opening
					Uri uri = Uri.fromFile(new File(path));
					viewIntent = isPhoto ? new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*") : 
						new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*");
				} else {
					// Want internal opening
					viewIntent = isPhoto ? new Intent(this.getActivity(), MediaPreview.class) : new Intent(this.getActivity(), VideoPreview.class);
					if (SettingsAccessor.getSnapTiming(this.getActivity()) && LocalSnaps.hasDisplayTime(snapId)) {
						viewIntent.putExtra(PreviewConstants.TIME_KEY, LocalSnaps.getDisplayTime(snapId));
					}
					String caption;
					if (!StatMethods.IsStringNullOrEmpty((caption = snap.getCaption()))) {
						viewIntent.putExtra(PreviewConstants.CAPTION_KEY, caption).putExtra(PreviewConstants.CAPTION_LOC_KEY, snap.getCaptionLocation())
							.putExtra(PreviewConstants.CAPTION_ORI_KEY, snap.getCaptionOrientation());
					}
					viewIntent.putExtra(PreviewConstants.PATH_KEY, path);
				}
				
				if (allowSaves) {
					// Saves allowed so we don't need a callback on return
					this.startActivity(viewIntent);
				} else {
					// Saves not allowed so delete on return
					this.startActivityForResult(viewIntent, RETURN_AND_DELETE);
				}
				
				if (!isOpened && !snap.getSent() && (!allowSaves || !SettingsAccessor.getPrivateMode(this.getActivity()))) {
					// To return true the snap MUST NOT BE opened, MUST BE RECEIVED, and if saves are allowed MUST NOT be in private mode
					new OpenSnapTask(this.getActivity(), snap, GlobalVars.getUsername(getActivity())).execute(new String[] {});
					mHandler.sendMessageDelayed(mHandler.obtainMessage(GuiHandler.OPEN_SNAP, GuiHandler.LOCALSNAP, 
							TempSnaps.getCount() + snapId, snap.getSnapId()), 200);
				}
			} else {
				// Saves aren't allowed and it's already been opened. Ignore click.
			}
		} else if (snap.getSnapAvailable()) {
			if (StatMethods.isNetworkAvailable(this.getActivity(), true)) {
				new SnapDownload(this.getSupportApplication(), snap)
					.execute(GlobalVars.getUsername(this.getActivity()), GlobalVars.getAuthToken(this.getActivity()));
				mHandler.sendMessage(mHandler.obtainMessage(GuiHandler.REPEAT_REFRESH_VIEW, GuiHandler.LOCALSNAP, 
						TempSnaps.getCount() + snapId, snap.getSnapId()));
			}
		} else {
			StatMethods.hotBread(this.getActivity(), "Snap not available", Toast.LENGTH_SHORT);
		}
	}

	private void tempSnapClick(int snapID, View view) {
		// TODO: tempsnap click mHandler
		if (TempSnaps.isSending(snapID)) {
			// ignore for the momento
		} else if (TempSnaps.isSent(snapID)) {
			// also ignore for the momento
		} else {
			String filePath = TempSnaps.getFilePath(snapID);
			Message toPost;
			if (!new File(filePath).exists()) {
				StatMethods.hotBread(this.getActivity(), "Error! File missing!", Toast.LENGTH_SHORT);
				TempSnaps.setIsError(snapID, true);
				toPost = mHandler.obtainMessage(GuiHandler.REFRESH_VIEW, GuiHandler.TEMPSNAP, snapID, TempSnaps.getId(snapID));
			} else {
				TempSnap snap = TempSnaps.get(snapID);
				snap.setIsSending(true).setError(false).setUploadPercent(-1);
				new SnapUpload(this.getSupportApplication(), snap).execute(
						new String[] { null, GlobalVars.getUsername(getActivity()), GlobalVars.getAuthToken(getActivity()) });
				toPost = mHandler.obtainMessage(GuiHandler.REPEAT_REFRESH_VIEW, GuiHandler.TEMPSNAP, snapID, TempSnaps.getId(snapID));
			}
			mHandler.sendMessage(toPost);
		}
	}

    private void tempSnapClick(TempSnap snap, int position) {
        if (snap.isSending()) {
            // ignore for the moment
        } else if (snap.isSent()) {
            // also ignore
        } else { // attempt to send snap
            String filePath = snap.getFilePath();
            Message toPost;
            if (!new File(filePath).exists()) {
                StatMethods.hotBread(this.getActivity(), "Error! File missing!", Toast.LENGTH_SHORT);
                snap.setError(true);
                toPost = mHandler.obtainMessage(GuiHandler.REFRESH_VIEW, GuiHandler.TEMPSNAP, position, snap.getId());
            } else {
                if (StatMethods.isNetworkAvailable(this.getActivity(), true)) {
                    snap.setIsSending(true).setError(false).setUploadPercent(-1);
                    new SnapUpload(this.getSupportApplication(), snap).execute(
                            null, GlobalVars.getUsername(this.getActivity()),
                            GlobalVars.getAuthToken(this.getActivity())
                    );
                    toPost = mHandler.obtainMessage(GuiHandler.REPEAT_REFRESH_VIEW, GuiHandler.TEMPSNAP, position, snap.getId());
                } else {
                    toPost = null;
                }
            }

            if (toPost != null) {
                mHandler.sendMessage(toPost);
            }
        }
    }

    private void localSnapClick(LocalSnap snap, int position) {
        boolean allowSaves = SettingsAccessor.getAllowSaves(this.getActivity()), isPhoto = snap.isPhoto(),
                isOpened = snap.isOpened();

        String path;
        try {
            path = snap.getSnapPath(this.getActivity());
        } catch (IOException e) {
            Twig.warning(FRAGTAG, "External storage not available");
            StatMethods.hotBread(this.getActivity(), "External storage not available! Connect storage and retry!",
                    Toast.LENGTH_LONG);
            return;
        }

        if (snap.isDownloading()) {
            // Is currently downloading, ignore click
        } else if (snap.getSnapExists(this.getActivity())) {
            // Not downloading and exists. Good.
            if (allowSaves || !isOpened || snap.getSent()) {
                // If saves are allowed you can re-open it. Otherwise make sure it's not already opened
                Intent viewIntent;
                if (SettingsAccessor.getExternalPreviewPref(this.getActivity())) {
                    // Want external opening
                    Uri uri = Uri.fromFile(new File(path));
                    viewIntent = isPhoto ? new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*") :
                            new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*");
                } else {
                    // Want internal opening
                    viewIntent = isPhoto ? new Intent(this.getActivity(), MediaPreview.class) :
                            new Intent(this.getActivity(), VideoPreview.class);
                    if (SettingsAccessor.getSnapTiming(this.getActivity()) && snap.hasDisplayTime()) {
                        viewIntent.putExtra(PreviewConstants.TIME_KEY, snap.getDisplayTime());
                    }
                    String caption;
                    if (!StatMethods.IsStringNullOrEmpty((caption = snap.getCaption()))) {
                        viewIntent.putExtra(PreviewConstants.CAPTION_KEY, caption)
                                .putExtra(PreviewConstants.CAPTION_LOC_KEY, snap.getCaptionLocation())
                                .putExtra(PreviewConstants.CAPTION_ORI_KEY, snap.getCaptionOrientation());
                    }
                    viewIntent.putExtra(PreviewConstants.PATH_KEY, path);
                }

                if (allowSaves || snap.getSent()) {
                    // Saves allowed so we don't need a callback on return
                    this.startActivity(viewIntent);
                } else {
                    // Saves not allowed so delete on return
                    this.startActivityForResult(viewIntent, RETURN_AND_DELETE);
                }

                if (!isOpened && !snap.getSent() && (!allowSaves || !SettingsAccessor.getPrivateMode(this.getActivity()))) {
                    // To return true the snap MUST NOT BE opened, MUST BE RECEIVED, and if saves are allowed MUST NOT be in private mode
                    new OpenSnapTask(this.getActivity(), snap, GlobalVars.getUsername(this.getActivity())).execute();
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(GuiHandler.OPEN_SNAP,
                            GuiHandler.LOCALSNAP, position, snap.getSnapId()), 200);
                }
            } else {
                // Saves aren't allowed and it's already been opened yet somehow the file exists. Delete it.
                File file = new File(path);
                file.delete();
                StatMethods.hotBread(this.getActivity(), "Snap not available", Toast.LENGTH_SHORT);
            }
        } else if (snap.getSnapAvailable()) {
            if (StatMethods.isNetworkAvailable(this.getActivity(), true)) {
                new SnapDownload(this.getSupportApplication(), snap).execute(
                        GlobalVars.getUsername(this.getActivity()), GlobalVars.getAuthToken(this.getActivity())
                );
                mHandler.sendMessage(mHandler.obtainMessage(
                        GuiHandler.REPEAT_REFRESH_VIEW, GuiHandler.LOCALSNAP, position, snap.getSnapId()));
            }
        } else {
            StatMethods.hotBread(this.getActivity(), "Snap not available", Toast.LENGTH_SHORT);
        }
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RETURN_AND_DELETE) {
			String filePath = data.getStringExtra(PreviewConstants.PATH_KEY);
			if (filePath != null) {
				File file = new File(filePath);
				if (file.exists()) {
					file.delete();
				}
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (mActiveAnims != null) {
				for (int i = 0; i < mActiveAnims.size(); i++) {
					ValueAnimator va = mActiveAnims.get(mActiveAnims.keyAt(i));
					if (va != null && va.isRunning())
						va.end();
				}
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
		mHandler.sendEmptyMessageDelayed(GuiHandler.REFRESH_WHOLE_LIST, 200);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		mHandler.removeMessages(GuiHandler.REFRESH_VIEW);
		mHandler.removeMessages(GuiHandler.REPEAT_REFRESH_VIEW);
		mHandler.removeMessages(GuiHandler.REFRESH_WHOLE_LIST);
	}

	protected View getListChildView(int wantedPosition) {
        // TODO: Fix if list destroyed
		if (this.getListView() == null) {
			return null; 
		}
		int wantedChild = wantedPosition - getFirstPosition();

		if (wantedChild < 0 || wantedChild >= this.getListView().getChildCount()) {
			return null;
		}
		
		return this.getListView().getChildAt(wantedChild);
	}

	protected int getFirstPosition() {
		return this.getListView().getFirstVisiblePosition() - this.getListView().getHeaderViewsCount();
	}

	protected int getNumVisible() {
		return this.getListView().getLastVisiblePosition() - this.getListView().getFirstVisiblePosition();
	}

	@SuppressWarnings("unchecked")
	public void refreshList() {
		ArrayAdapter<LocalSnaps.LocalSnap> adapter;
		if ((adapter = (ArrayAdapter<LocalSnaps.LocalSnap>) this.getListAdapter()) != null) {
			adapter.notifyDataSetChanged();
		}
		
		/* if ((TempSnaps.getCount() + LocalSnaps.getNumberOfSnaps()) == 0) {
			if (emptyListView.getVisibility() != View.VISIBLE) {
				emptyListView.setVisibility(View.VISIBLE);
			}
            // TODO: refresh list stuff
			if (list.getVisibility() == View.VISIBLE) {
				list.setVisibility(View.INVISIBLE);
			}
		} else {
			if (emptyListView.getVisibility() == View.VISIBLE) {
				emptyListView.setVisibility(View.INVISIBLE);
			}
            // TODO: refresh list stuff
			if (list.getVisibility() != View.VISIBLE) {
				list.setVisibility(View.VISIBLE);
			}
		} */
	}
	
	/**
	 * An extension to {@link ArrayAdapter} that feeds mSnaps to a list view
	 * @author Nick Stephen (a.k.a. saltisgood)
	 */
	public class SnapViewListAdapter extends ArrayAdapter<LocalSnaps.LocalSnap> {
		private int mMaxListSize = 20;
		private LayoutInflater mInflater;
		
		public SnapViewListAdapter (Context context) {
			super(context, R.layout.snap_text_v3);
		}

		@Override
		public int getCount() {
			int maxCount = TempSnaps.getCount() + LocalSnaps.getNumberOfSnaps();
			return (mMaxListSize > maxCount) ? maxCount : mMaxListSize;
		}

		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View v;
			if (mInflater == null) {
				mInflater = SnapViewerListFrag.this.getLayoutInflater();
			}
			
			TextView userText, descText;
			
			if (position == (getCount() - 1) && (position < TempSnaps.getCount() + LocalSnaps.getNumberOfSnaps() - 1)) {
				v = mInflater.inflate(R.layout.list_footer_button);
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mMaxListSize += 20;
						refreshList();
					}
				});
				
				return v;
			}
			
			if (position >= (TempSnaps.getCount())) {
				position -= TempSnaps.getCount();
				LocalSnap snap = LocalSnaps.getSnapAt(position);

                if (!snap.getSent() && !snap.wasOpened()) {
                    v = mInflater.inflate(R.layout.snap_text_unseen);

                    if (snap.isDownloading()) {
                        int progress = snap.getDownloadProgress();
                        View pb1 = v.findViewById(R.id.progressBar1);
                        pb1.setVisibility(View.VISIBLE);
                        if (progress > 0 && progress < 100) {
                            ProgressBar pb = (ProgressBar)v.findViewById(R.id.progressBar2);
                            pb.setProgress(progress);
                            pb.setVisibility(View.VISIBLE);
                        }

                        mHandler.removeMessages(GuiHandler.REPEAT_REFRESH_VIEW, snap.getSnapId());
                        mHandler.sendMessage(mHandler.obtainMessage(GuiHandler.REPEAT_REFRESH_VIEW,
                                GuiHandler.LOCALSNAP, TempSnaps.getCount() + position, snap.getSnapId()));
                    } else if (!snap.getSnapExists(this.getContext()) && snap.getSnapAvailable() &&
                            StatMethods.isNetworkAvailable(SnapViewerListFrag.this.getActivity(), false) &&
                            SettingsAccessor.getAlwaysDownload(SnapViewerListFrag.this.getActivity())) {
                        new SnapDownload(SnapViewerListFrag.this.getSupportApplication(), snap)
                                .execute(GlobalVars.getUsername(this.getContext()), GlobalVars.getAuthToken(this.getContext()));

                        mHandler.removeMessages(GuiHandler.REPEAT_REFRESH_VIEW, snap.getSnapId());
                        mHandler.sendMessage(mHandler.obtainMessage(GuiHandler.REPEAT_REFRESH_VIEW,
                                GuiHandler.LOCALSNAP, TempSnaps.getCount() + position, snap.getSnapId()));
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        ValueAnimator va = ObjectAnimator.ofInt(v, "backgroundColor", Color.BLACK,
                                SnapViewerListFrag.this.getResources().getColor(R.color.deep_red));
                        va.setDuration(1250).setEvaluator(new ArgbEvaluator());
                        va.setRepeatCount(ValueAnimator.INFINITE);
                        va.setRepeatMode(ValueAnimator.REVERSE);
                        va.start();
                        mActiveAnims.append(position, va);
                    } else {
                        //TODO: Back port across the animation stuff
                        v.setBackgroundColor(SnapViewerListFrag.this.getResources().getColor(R.color.deep_red));
                    }

                    ImageView img = (ImageView) v.findViewById(R.id.snap_status_icon);
                    if (snap.getSnapExists(this.getContext())) {
                        img.setImageResource(R.drawable.check);
                        img.setTag(R.drawable.check);
                        img.setVisibility(View.VISIBLE);
                    } else if (snap.isError()) {
                        img.setImageResource(R.drawable.cancel);
                        img.setTag(R.drawable.cancel);
                        img.setVisibility(View.VISIBLE);
                    }
                } else {
                    v = mInflater.inflate(R.layout.snap_text_v3);
                }

                userText = (TextView) v.findViewById(R.id.snap_user_text);
                if (snap.getSent()) {
                    userText.setText(snap.getFriendlyRecipName());
                } else {
                    userText.setText(snap.getFriendlySenderName());
                }

                descText = (TextView) v.findViewById(R.id.snap_desc_text);
                descText.setText(snap.getReadableSentTimeStamp());

                ImageView img = (ImageView) v.findViewById(R.id.image);
                //TODO: Check for friend request
                switch (mTheme) {
                    case snapchat:
                        userText.setTextColor(0xFF000000);
                        if (snap.getSent()) {
                            if (snap.wasOpened()) {
                                if (snap.isPhoto()) {
                                    img.setImageResource(R.drawable.aa_feed_icon_sent_photo_opened);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_sent_photo_opened);
                                } else {
                                    img.setImageResource(R.drawable.aa_feed_icon_sent_video_opened);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_sent_video_opened);
                                }
                            } else {
                                if (snap.isPhoto()) {
                                    img.setImageResource(R.drawable.aa_feed_icon_sent_photo_unopened);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_sent_photo_unopened);
                                } else {
                                    img.setImageResource(R.drawable.aa_feed_icon_sent_video_unopened);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_sent_video_unopened);
                                }
                            }
                        } else {
                            if (snap.wasOpened()) {
                                if (snap.isPhoto()) {
                                    img.setImageResource(R.drawable.aa_feed_icon_opened_photo);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_opened_photo);
                                } else {
                                    img.setImageResource(R.drawable.aa_feed_icon_opened_video);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_opened_video);
                                }
                            } else {
                                if (snap.isPhoto()) {
                                    img.setImageResource(R.drawable.aa_feed_icon_unopened_photo);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_unopened_photo);
                                } else {
                                    img.setImageResource(R.drawable.aa_feed_icon_unopened_video);
                                    img.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_unopened_video);
                                }
                            }
                        }
                        img.setTag(R.id.img_anim, false);
                        break;
                    case ori:
                        userText.setTextColor(0xFF000000);
                        if (snap.getSent()) {
                            if (snap.wasOpened()) {
                                img.setImageResource(R.drawable.eye_send_open);
                                img.setTag(R.id.img_res_id, R.drawable.eye_send_open);
                            } else {
                                img.setImageResource(R.drawable.eye_send_unopen);
                                img.setTag(R.id.img_res_id, R.drawable.eye_send_unopen);
                            }
                        } else {
                            if (snap.wasOpened()) {
                                img.setImageResource(R.drawable.eye_receive_open);
                                img.setTag(R.id.img_res_id, R.drawable.eye_receive_open);
                            } else if (!snap.getSnapExists(this.getContext())) {
                                img.setImageResource(R.drawable.stat_sys_download_anim0);
                                img.setTag(R.id.img_res_id, R.drawable.stat_sys_download_anim0);
                            } else {
                                img.setImageResource(R.drawable.eye_receive_unopen);
                                img.setTag(R.id.img_res_id, R.drawable.eye_receive_unopen);
                            }
                        }
                        img.setTag(R.id.img_anim, false);
                        break;
                    case def:
                    default:
                        if (snap.getSent()) {
                            if (snap.wasOpened()) {
                                img.setImageResource(R.drawable.eye_send_open_white);
                                img.setTag(R.id.img_res_id, R.drawable.eye_send_open_white);
                            } else {
                                img.setImageResource(R.drawable.eye_send_unopen_white);
                                img.setTag(R.id.img_res_id, R.drawable.eye_send_unopen_white);
                            }
                        } else {
                            if (snap.wasOpened()) {
                                img.setImageResource(R.drawable.eye_receive_open_white);
                                img.setTag(R.id.img_res_id, R.drawable.eye_receive_open_white);
                            } else if (!snap.getSnapExists(this.getContext())) {
                                img.setImageResource(R.drawable.stat_sys_download_anim0_w);
                                img.setTag(R.id.img_res_id, R.drawable.stat_sys_download_anim0_w);
                            } else {
                                img.setImageResource(R.drawable.eye_receive_unopen_white);
                                img.setTag(R.id.img_res_id, R.drawable.eye_receive_unopen_white);
                            }
                        }
                        img.setTag(R.id.img_anim, false);
                        break;
                }

                if (mTheme != Theme.snapchat && snap.isVideo()) {
                    img = (ImageView) v.findViewById(R.id.right_icon);
                    img.setImageResource(R.drawable.clapper);
                }

                v.setTag(R.id.snap_item_position, position);
                v.setTag(R.id.snap_item, snap);
			} else { // TempSnap
				v = mInflater.inflate(R.layout.snap_text_unseen);

                TempSnap tempSnap = TempSnaps.get(position);
                ImageView imgback = (ImageView) v.findViewById(R.id.image);
                switch (mTheme) {
                    case snapchat:
                    case ori:
                        if (!tempSnap.isSending()) {
                            imgback.setImageResource(R.drawable.stat_sys_upload_anim0);
                            imgback.setTag(R.id.img_res_id, R.drawable.stat_sys_upload_anim0);
                            imgback.setTag(R.id.img_anim, false);
                        } else {
                            imgback.setImageResource(R.drawable.upload_anim);
                            AnimationDrawable anim = (AnimationDrawable) imgback.getDrawable();
                            anim.setOneShot(false);
                            anim.start();
                            imgback.setTag(R.id.img_anim, true);
                            imgback.setTag(R.id.img_res_id, R.drawable.upload_anim);
                        }
                        break;
                    case def:
                    default:
                        if (!tempSnap.isSending()) {
                            imgback.setImageResource(R.drawable.stat_sys_upload_anim0_w);
                            imgback.setTag(R.id.img_anim, false);
                            imgback.setTag(R.id.img_res_id, R.drawable.stat_sys_upload_anim0_w);
                        } else {
                            imgback.setImageResource(R.drawable.upload_anim_w);
                            AnimationDrawable anim = (AnimationDrawable) imgback.getDrawable();
                            anim.setOneShot(false);
                            anim.start();
                            imgback.setTag(R.id.img_anim, true);
                            imgback.setTag(R.id.img_res_id, R.drawable.upload_anim_w);
                        }
                }

                if (tempSnap.isVideo()) {
                    ImageView img = (ImageView) v.findViewById(R.id.right_icon);
                    img.setImageResource(R.drawable.clapper);
                }

                userText = (TextView) v.findViewById(R.id.snap_user_text);
                userText.setText(tempSnap.getUsers());

                descText = (TextView) v.findViewById(R.id.snap_desc_text);
                if (tempSnap.isSent()) {
                    descText.setText("Sent " + tempSnap.getReadableSentTimeStamp());
                } else if (tempSnap.isSending()) {
                    descText.setText("Sending...");
                } else if (tempSnap.isError()) {
                    descText.setText("Error - tap to retry");
                } else {
                    descText.setText("Tap to send");
                }

                if (tempSnap.isSending()) {
                    View pb1 = v.findViewById(R.id.progressBar1);
                    pb1.setVisibility(View.VISIBLE);
                    int progress = tempSnap.getUploadPercent();
                    if (progress > 0 && progress < 100) {
                        ProgressBar pb = (ProgressBar) v.findViewById(R.id.progressBar2);
                        pb.setProgress(progress);
                        pb.setVisibility(View.VISIBLE);
                    }
                    mHandler.removeMessages(GuiHandler.REPEAT_REFRESH_VIEW, tempSnap.getId());
                    mHandler.sendMessage(mHandler.obtainMessage(GuiHandler.REPEAT_REFRESH_VIEW,
                            GuiHandler.TEMPSNAP, position, tempSnap.getId()));
                }

                v.setTag(R.id.snap_item_position, position);
                v.setTag(R.id.snap_item, tempSnap);
			}
			
			/* switch (mTheme) {
				case snapchat:
				case ori:
					break;
				case def:
				default:
					if (!backgroundSet) {
						BitmapDrawable draw;
						if (mDrawable == null || (draw = mDrawable.get()) == null) {
							draw = (BitmapDrawable)SnapViewerListFrag.this.getActivity().getResources().getDrawable(R.drawable.main_menu_default_background);
							draw.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
							mDrawable = new SoftReference<BitmapDrawable>(draw);
						}
						if (Build.VERSION.SDK_INT < 16) {
							v.setBackgroundDrawable(draw);
						} else {
							v.setBackground(draw);
						}
					}
					userText.setTextColor(0xFFFFFFFF);
					descText.setTextColor(0xFFA9A9A9);
					break;
			} */
			
			return v;
		}
	}

	/**
	 * A handler extension that interprets messages used for refreshing certain parts of the GUI
	 * without having to resort to another thread or blocking.
	 * @author Nicholas Stephen (a.k.a. saltisgood)
	 */
	private final class GuiHandler extends Handler {
		public static final int REFRESH_VIEW = 0x10;
		public static final int REPEAT_REFRESH_VIEW = 0x100;
		public static final int REFRESH_WHOLE_LIST = 0x1000;
		public static final int CLEAR_NOTIFICATION = 0x10000;
		public static final int OPEN_SNAP = 0x100000;
		public static final int TEMPSNAP = 0x0;
		public static final int LOCALSNAP = 0x1;
		
		@SuppressLint({"CutPasteId", "NewApi"})
		@SuppressWarnings("deprecation")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case OPEN_SNAP:
                    View view = getListChildView(msg.arg2);
                    if (view != null) {
                        Object obj = view.getTag(R.id.snap_item);
                        LocalSnap snap;
                        if (obj != null && obj instanceof LocalSnap &&
                                (snap = (LocalSnap)obj).getSnapId().compareTo((String)msg.obj) == 0) {
                            if (snap.isOpened()) {
                                ImageView statusIcon = (ImageView) view.findViewById(R.id.snap_status_icon);
                                if (statusIcon == null) {
                                    refreshList();
                                    return;
                                }
                                if (statusIcon.getVisibility() == View.VISIBLE) {
                                    statusIcon.setVisibility(View.GONE);
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                    ValueAnimator anim = mActiveAnims.get(msg.arg2);
                                    if (anim != null && anim.isRunning()) {
                                        anim.end();
                                    }
                                    mActiveAnims.remove(msg.arg2);
                                } else {
                                    // TODO: set default colour for list background
                                }

                                statusIcon = (ImageView) view.findViewById(R.id.image);
                                if (statusIcon == null) {
                                    refreshList();
                                    return;
                                }
                                statusIcon.setTag(R.id.img_anim, false);
                                switch (mTheme) {
                                    case ori:
                                        statusIcon.setImageResource(R.drawable.eye_receive_open);
                                        statusIcon.setTag(R.id.img_res_id, R.drawable.eye_receive_open);
                                        break;
                                    case snapchat:
                                        if (snap.isPhoto()) {
                                            statusIcon.setImageResource(R.drawable.aa_feed_icon_opened_photo);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_opened_photo);
                                        } else {
                                            statusIcon.setImageResource(R.drawable.aa_feed_icon_opened_video);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.aa_feed_icon_opened_video);
                                        }
                                        break;
                                    default:
                                        statusIcon.setImageResource(R.drawable.eye_receive_open_white);
                                        statusIcon.setTag(R.id.img_res_id, R.drawable.eye_receive_open_white);
                                        break;
                                }

                                if (SnapViewerListFrag.this.getFragmentManager() != null) {
                                    List<android.support.v4.app.Fragment> frags = SnapViewerListFrag.this.getFragmentManager().getFragments();
                                    if (frags != null) {
                                        for (android.support.v4.app.Fragment frag : frags) {
                                            if (frag instanceof MainMenuFrag) {
                                                ((MainMenuFrag) frag).setUpdateText(LocalSnaps.getUnseenSnaps());
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Not yet opened, repeat the message later
                                this.sendMessageDelayed(this.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj), 400);
                            }
                        } else {
                            this.removeMessages(REPEAT_REFRESH_VIEW, msg.obj);
                            refreshList();
                            return;
                        }
                    } else { // view == null
                        //TODO: Ensure this doesn't cause issues with a disposed list
                        this.removeMessages(REPEAT_REFRESH_VIEW, msg.obj);
                        refreshList();
                        return;
                    }
					break;
				case CLEAR_NOTIFICATION:
					if (msg.arg1 == LOCALSNAP) {
						Notifications.clearDownloadNotifications(SnapViewerListFrag.this.getActivity());
					} else {
						Notifications.clearUploadNotifications(SnapViewerListFrag.this.getActivity());
					}
					break;
				case REFRESH_WHOLE_LIST:
					refreshList();
					this.sendEmptyMessageDelayed(REFRESH_WHOLE_LIST, 30000);
					break;
				case REPEAT_REFRESH_VIEW:
					this.sendMessageDelayed(this.obtainMessage(REPEAT_REFRESH_VIEW, msg.arg1, msg.arg2, msg.obj), 400);
				case REFRESH_VIEW:
                    view = getListChildView(msg.arg2);
                    if (view == null) {
                        refreshList();
                        return;
                    }

                    if (msg.arg1 == TEMPSNAP) {
                        Object obj = view.getTag(R.id.snap_item);
                        TempSnap tempSnap;
                        if (obj != null && obj instanceof TempSnap &&
                                (tempSnap = (TempSnap) obj).getId().compareTo((String) msg.obj) == 0) {
                            TextView descText = (TextView) view.findViewById(R.id.snap_desc_text);
                            if (descText == null) {
                                refreshList();
                            }
                            String text = descText.getText().toString();
                            if (tempSnap.isSent()) {
                                if (!text.startsWith("Sent")) {
                                    descText.setText("Sent " + tempSnap.getReadableSentTimeStamp());
                                }
                            } else if (tempSnap.isSending()) {
                                if (!text.startsWith("Send")) {
                                    descText.setText("Sending...");
                                }
                            } else if (tempSnap.isError()) {
                                if (!text.startsWith("Err")) {
                                    descText.setText("Error - tap to retry");
                                }
                            } else {
                                if (!text.startsWith("Tap")) {
                                    descText.setText("Tap to send");
                                }
                            }

                            ImageView statusIcon = (ImageView) view.findViewById(R.id.snap_status_icon);
                            if (statusIcon == null) {
                                refreshList();
                                return;
                            }
                            if (tempSnap.isSent()) {
                                if (statusIcon.getTag() == null || (Integer)statusIcon.getTag() != R.drawable.check) {
                                    statusIcon.setImageResource(R.drawable.check);
                                    statusIcon.setTag(R.drawable.check);
                                }
                                if (statusIcon.getVisibility() != View.VISIBLE) {
                                    statusIcon.setVisibility(View.VISIBLE);
                                }
                            } else if (tempSnap.isError()) {
                                if (statusIcon.getTag() == null || (Integer) statusIcon.getTag() != R.drawable.cancel) {
                                    statusIcon.setImageResource(R.drawable.cancel);
                                    statusIcon.setTag(R.drawable.cancel);
                                }
                                if (statusIcon.getVisibility() != View.VISIBLE) {
                                    statusIcon.setVisibility(View.VISIBLE);
                                }
                            } else if (statusIcon.getVisibility() == View.VISIBLE) {
                                statusIcon.setVisibility(View.GONE);
                            }

                            statusIcon = (ImageView) view.findViewById(R.id.image);
                            if (statusIcon == null) {
                                refreshList();
                            }
                            if (!tempSnap.isSending()) {
                                if ((Boolean)statusIcon.getTag(R.id.img_anim)) {
                                    statusIcon.setTag(R.id.img_anim, false);
                                    switch (mTheme) {
                                        case snapchat:
                                        case ori:
                                            statusIcon.setImageResource(R.drawable.stat_sys_upload_anim0);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.stat_sys_upload_anim0);
                                            break;
                                        default:
                                            statusIcon.setImageResource(R.drawable.stat_sys_upload_anim0_w);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.stat_sys_upload_anim0_w);
                                            break;
                                    }
                                }
                            } else {
                                if (!(Boolean) statusIcon.getTag(R.id.img_anim)) {
                                    statusIcon.setTag(R.id.img_anim, true);
                                    switch (mTheme) {
                                        case snapchat:
                                        case ori:
                                            statusIcon.setImageResource(R.drawable.upload_anim);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.upload_anim);
                                            break;
                                        default:
                                            statusIcon.setImageResource(R.drawable.upload_anim_w);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.upload_anim_w);
                                            break;
                                    }
                                    AnimationDrawable anim = (AnimationDrawable) statusIcon.getDrawable();
                                    if (anim != null) {
                                        anim.start();
                                    }
                                }
                            }

                            ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
                            ProgressBar pb2 = (ProgressBar) view.findViewById(R.id.progressBar2);
                            if (pb == null || pb2 == null) {
                                refreshList();
                                return;
                            }

                            if (tempSnap.isSending()) {
                                if (pb.getVisibility() != View.VISIBLE) {
                                    pb.setVisibility(View.VISIBLE);
                                }

                                int progress = tempSnap.getUploadPercent();
                                if (progress > 0 && progress < 100) {
                                    if (pb2.getVisibility() != View.VISIBLE) {
                                        pb2.setVisibility(View.VISIBLE);
                                    }
                                    pb2.setProgress(progress);
                                } else {
                                    if (pb2.getVisibility() == View.VISIBLE) {
                                        pb2.setVisibility(View.INVISIBLE);
                                    }
                                }
                            } else {
                                if (pb.getVisibility() == View.VISIBLE) {
                                    pb.setVisibility(View.GONE);
                                }
                                if (pb2.getVisibility() == View.VISIBLE) {
                                    pb2.setVisibility(View.INVISIBLE);
                                }
                                this.removeMessages(msg.what, msg.obj);
                                if (SettingsAccessor.getUploadNotificationPref(SnapViewerListFrag.this.getActivity())) {
                                    this.sendMessageDelayed(this.obtainMessage(CLEAR_NOTIFICATION, TEMPSNAP, 0), 2000);
                                }
                            }
                        } else {
                            this.removeMessages(REPEAT_REFRESH_VIEW, msg.obj);
                            refreshList();
                            return;
                        }
                    } else { // LOCALSNAP
                        Object obj = view.getTag(R.id.snap_item);
                        LocalSnap snap;
                        if (obj != null && obj instanceof LocalSnap && (snap = (LocalSnap) obj).getSnapId().compareTo((String) msg.obj) == 0) {
                            ImageView statusIcon = (ImageView) view.findViewById(R.id.snap_status_icon);
                            if (statusIcon == null) {
                                refreshList();
                                return;
                            }
                            if (snap.isError()) {
                                if (statusIcon.getTag() == null || (Integer) statusIcon.getTag() != R.drawable.cancel) {
                                    statusIcon.setImageResource(R.drawable.cancel);
                                    statusIcon.setTag(R.drawable.cancel);
                                }
                                if (statusIcon.getVisibility() != View.VISIBLE) {
                                    statusIcon.setVisibility(View.VISIBLE);
                                }
                            } else if (!snap.isDownloading() &&
                                    snap.getSnapExists(SnapViewerListFrag.this.getActivity())) {
                                if (statusIcon.getTag() == null || (Integer) statusIcon.getTag() != R.drawable.check) {
                                    statusIcon.setImageResource(R.drawable.check);
                                    statusIcon.setTag(R.drawable.check);
                                }
                                if (statusIcon.getVisibility() != View.VISIBLE) {
                                    statusIcon.setVisibility(View.VISIBLE);
                                }
                            } else if (statusIcon.getVisibility() == View.VISIBLE) {
                                statusIcon.setVisibility(View.INVISIBLE);
                            }

                            statusIcon = (ImageView) view.findViewById(R.id.image);
                            if (statusIcon == null) {
                                refreshList();
                                return;
                            }
                            if (!snap.isDownloading()) {
                                if ((Boolean) statusIcon.getTag(R.id.img_anim)) {
                                    statusIcon.setTag(R.id.img_anim, false);
                                    switch (mTheme) {
                                        case snapchat:
                                        case ori:
                                            statusIcon.setImageResource(R.drawable.stat_sys_download_anim0);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.stat_sys_download_anim0);
                                            break;
                                        default:
                                            statusIcon.setImageResource(R.drawable.stat_sys_download_anim0_w);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.stat_sys_download_anim0_w);
                                            break;
                                    }
                                }
                            } else {
                                if (!(Boolean) statusIcon.getTag(R.id.img_anim)) {
                                    statusIcon.setTag(R.id.img_anim, true);
                                    switch (mTheme) {
                                        case snapchat:
                                        case ori:
                                            statusIcon.setImageResource(R.drawable.download_anim);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.download_anim);
                                            break;
                                        default:
                                            statusIcon.setImageResource(R.drawable.download_anim_w);
                                            statusIcon.setTag(R.id.img_res_id, R.drawable.download_anim_w);
                                            break;
                                    }
                                    AnimationDrawable anim = (AnimationDrawable) statusIcon.getDrawable();
                                    if (anim != null) {
                                        anim.start();
                                    }
                                }
                            }

                            ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
                            ProgressBar pb2 = (ProgressBar) view.findViewById(R.id.progressBar2);
                            if (pb == null || pb2 == null) {
                                refreshList();
                                return;
                            }

                            if (snap.isDownloading()) {
                                if (pb.getVisibility() != View.VISIBLE) {
                                    pb.setVisibility(View.VISIBLE);
                                }
                                int progress = snap.getDownloadProgress();
                                if (progress > 0 && snap.getDownloadProgress() < 100) {
                                    if (pb2.getVisibility() != View.VISIBLE) {
                                        pb2.setVisibility(View.VISIBLE);
                                    }
                                    pb2.setProgress(progress);
                                } else {
                                    if (pb2.getVisibility() == View.VISIBLE) {
                                        pb2.setVisibility(View.INVISIBLE);
                                    }
                                }
                            } else {
                                if (pb.getVisibility() == View.VISIBLE) {
                                    pb.setVisibility(View.GONE);
                                }
                                if (pb2.getVisibility() == View.VISIBLE) {
                                    pb2.setVisibility(View.INVISIBLE);
                                }
                                this.removeMessages(msg.what, msg.obj);
                                if (SettingsAccessor.getDownloadNotificationPref(SnapViewerListFrag.this.getActivity())) {
                                    this.sendMessageDelayed(this.obtainMessage(CLEAR_NOTIFICATION, LOCALSNAP, 0), 2000);
                                }
                            }
                        } else {
                            this.removeMessages(REPEAT_REFRESH_VIEW, msg.obj);
                            refreshList();
                            return;
                        }
                    }
					break;
			}
			
			super.handleMessage(msg);
		}
	}
}