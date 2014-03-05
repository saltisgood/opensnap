package com.nickstephen.opensnap.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ExpandableListView;
import org.holoeverywhere.widget.ExpandableListView.OnChildClickListener;
import org.holoeverywhere.widget.ExpandableListView.OnGroupClickListener;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.lib.play.IabHelper;
import com.nickstephen.lib.play.IabResult;
import com.nickstephen.lib.play.Inventory;
import com.nickstephen.lib.play.Purchase;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.composer.CaptureActivity;
import com.nickstephen.opensnap.composer.editor.EditorActivity;
import com.nickstephen.opensnap.dialog.SDWarningDialog;
import com.nickstephen.opensnap.drawer.DrawerAdapter;
import com.nickstephen.opensnap.global.GlobalVars;
import com.nickstephen.opensnap.global.LocalSnaps;
import com.nickstephen.opensnap.global.Statistics;
import com.nickstephen.opensnap.global.Stories;
import com.nickstephen.opensnap.global.TempSnaps;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.main.tuts.TutorialMainFrag;
import com.nickstephen.opensnap.main.tuts.TutorialRootFrag;
import com.nickstephen.opensnap.settings.Settings;
import com.nickstephen.opensnap.settings.SettingsAccessor;
import com.nickstephen.opensnap.util.Broadcast;
import com.nickstephen.opensnap.util.Constants;
import com.nickstephen.opensnap.util.gcm.GCMUtil;
import com.nickstephen.opensnap.util.gcm.NotificationReceiver;
import com.nickstephen.opensnap.util.gcm.SnapGCMRegistrar;
import com.nickstephen.opensnap.util.misc.CameraUtil;
import com.nickstephen.opensnap.util.misc.FileIO;
import com.nickstephen.opensnap.util.play.SKU;
import com.nickstephen.opensnap.util.tasks.ClearFeedTask;
import com.nickstephen.opensnap.util.tasks.FriendTask;
import com.nickstephen.opensnap.util.tasks.IOnObjectReady;
import com.nickstephen.opensnap.util.tasks.LoadFiles;
import com.nickstephen.opensnap.util.tasks.LoginTask;
import com.nickstephen.opensnap.util.tasks.LogoutTask;
import com.nickstephen.opensnap.util.tasks.UpdateTask;


/**
 * The entry point to the program and the main platform from which everything
 * starts/returns/etc. An extension of ActionBarActivity.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class LaunchActivity extends Activity implements IOnObjectReady<Statistics> {
    public static final String TAG = "OpenSnap LaunchActivity";

    public static final int REQUEST_PLAY_SERVICES_UPDATE = 9614;

	private boolean mIsRunning = true;
	private ActionBarDrawerToggle mDrawerToggle;
	private MenuItem mRefreshItem;
	private ExpandableListView mDrawerListView;
	private RelativeLayout mDrawerNoLoginLayout;
	private FrameLayout mDrawerFrame;
	private DrawerLayout mDrawerLayout;
	private CustomHandler mHandler = new CustomHandler();
    private IabHelper mPlayHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Broadcast.registerLaunchActivity(this);

        // Play Stuff
        mPlayHelper = new IabHelper(this, SKU.retrieveB64());
        mPlayHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Twig.debug("OpenSnap Vending", "Problem setting up IAB: " + result);
                } else {
                    List<String> additionalSkuList = new ArrayList<String>();
                    additionalSkuList.add(SKU.PREMIUM_FEATURES);
                    mPlayHelper.queryInventoryAsync(true, additionalSkuList, mGotInventoryListener);
                }
            }
        });
		
		switch (SettingsAccessor.getThemePref(this)) {
			case snapchat:
				this.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF4FA885));
				break;
			case def:
			default:
				break;
		}
		
		this.setContentView(R.layout.launch_drawer_layout);
		mDrawerLayout = (DrawerLayout)this.findViewById(R.id.drawer_layout);
		mDrawerFrame = (FrameLayout)this.findViewById(R.id.drawer_frame);
		mDrawerListView = (ExpandableListView)this.findViewById(R.id.left_drawer);
		mDrawerNoLoginLayout = (RelativeLayout)this.findViewById(R.id.drawer_no_login_display);
		if (!GlobalVars.isLoggedIn(this)) {
			mDrawerListView.setVisibility(View.GONE);
			mDrawerNoLoginLayout.setVisibility(View.VISIBLE);
		}
		mDrawerListView.setAdapter(new DrawerAdapter(this));
		mDrawerListView.setOnChildClickListener(drawerOnChildClickListener);
		mDrawerListView.setOnGroupClickListener(drawerOnGroupClickListener);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_navigation_drawer, R.string.drawer_open, R.string.drawer_close);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		this.getSupportActionBar().setHomeButtonEnabled(true);
		this.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
		this.getSupportActionBar().setTitle(R.string.app_name);
		
		/* if (!Contacts.init(this)) {
			StatMethods.hotBread(this, "Error initialising Contacts", Toast.LENGTH_SHORT);
		} */

        new LoadFiles(this).execute();

		/* if (!LocalSnaps.init(this)) {
			StatMethods.hotBread(this, "Error initialising snaps", Toast.LENGTH_SHORT);
		} */

        /* int err;
		if ((err = Statistics.init(this)) < 0) {
            Twig.warning(TAG, "Error initialising stats: " + err);
			StatMethods.hotBread(this, "Error initialising stats: Code " + err, Toast.LENGTH_SHORT);
		} */

        Statistics.getInstanceSafe(this);

		
		if (GlobalVars.isLoggedIn(this) && StatMethods.isNetworkAvailable(this, false)) {
			new SnapGCMRegistrar(this.getApplicationContext()).setupGoogleCloudManager(false);
			
			if (this.getIntent() != null && this.getIntent().getBooleanExtra(NotificationReceiver.LAUNCH_GOTO_FEED, false)) {
                new UpdateTask(this, GlobalVars.getUsername(this)).execute();
			}
		}
		
		if (FileIO.setupFolders() == -1 && SettingsAccessor.getShowSDWarning(this)) {
			Intent intent = new Intent(this, SDWarningDialog.class);
			this.startActivity(intent);
		}
		
		if (savedInstanceState != null) {
			return;
		}
		
		if (GlobalVars.isLoggedIn(this)) {
			LoadMenu(2, 1);
			return;
		}
		
		LaunchFrag fragger = new LaunchFrag();
		this.getSupportFragmentManager().beginTransaction().add(R.id.launch_container, fragger, LaunchFrag.FRAGTAG).commit();
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		mDrawerToggle.syncState();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.launch, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;
		
		switch (item.getItemId()){
			case R.id.action_settings:
				onSettingsClick();
				return true;
			case R.id.sign_out_menu:
				Logout();
				return true;
			case R.id.refresh_menu:
				mRefreshItem = item;
                if (StatMethods.isNetworkAvailable(this, true)) {
				    new UpdateTask(this, GlobalVars.getUsername(this)).execute();
                }
				return true;
			case R.id.clear_feed_menu:
				if (!StatMethods.isNetworkAvailable(this, true)) {
					return true;
				}
				final boolean saveLocal = SettingsAccessor.getSaveLocalFeed(this);
				String title = saveLocal ? "Clear cloud feed?" : "Clear feed?";
				String desc = saveLocal ? "This will clear the snaps from the cloud (not locally). Continue?" 
						: "This will permanently clear all records of snaps. Continue?";
				StatMethods.QuestionBox(this, title, desc,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								ClearFeed(null);
								
								if (!saveLocal) {
									LocalSnaps.reset(LaunchActivity.this);
                                    Broadcast.refreshSnapViewerListFrag();
                                    Broadcast.refreshMainMenuFrag();
								}
							}
						}, 
						null);
				return true;
            case R.id.purchase_premium:
                if (SettingsAccessor.getPremium(this)) {
                    StatMethods.hotBread(this, "Premium access already purchased!", Toast.LENGTH_SHORT);
                } else if (!checkPlayServices(true)) {
                    //StatMethods.hotBread(this, "Sorry! It seems Google Play services are not supported by your device!", Toast.LENGTH_SHORT);
                } else {
                    if (mPlayHelper != null) {
                        try {
                            mPlayHelper.launchPurchaseFlow(this, SKU.PREMIUM_FEATURES, SKU.REQUEST_PREMIUM, mPurchaseListener, null);
                        } catch (RuntimeException e) {
                            StatMethods.hotBread(this, "Sorry! Google Play services encountered an error with your request!", Toast.LENGTH_SHORT);
                        }
                    }
                }
                return true;
            case R.id.other_apps:
                Intent devTent = new Intent(Intent.ACTION_VIEW);
                devTent.setData(Uri.parse(Constants.DEV_URI));
                this.startActivity(devTent);
                return true;
            case R.id.view_tut:
                this.getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.launch_container, new TutorialMainFrag(), TutorialMainFrag.FRAG_TAG)
                        .addToBackStack(TutorialRootFrag.FRAG_TAG)
                        .commit();
                return true;
            case R.id.add_friend:
                if (StatMethods.isNetworkAvailable(this, true)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(R.array.add_friend_options_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // From Phone Contacts
                                LaunchActivity.this.getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out, R.anim.push_up_in, R.anim.push_up_out)
                                        .add(R.id.launch_container, new FindContactsListFrag(), FindContactsListFrag.FRAG_TAG)
                                        .addToBackStack(null).commit();
                                break;
                            case 1: // By Username
                                AlertDialog.Builder subbuild = new AlertDialog.Builder(LaunchActivity.this);
                                final EditText editor = new EditText(LaunchActivity.this);
                                subbuild.setView(editor).setTitle("Enter the username to add")
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        })
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String text = editor.getText().toString().trim();
                                                if (StatMethods.IsStringNullOrEmpty(text)) {
                                                    StatMethods.hotBread(LaunchActivity.this, "Enter a username", Toast.LENGTH_SHORT);
                                                } else {
                                                    new FriendTask(LaunchActivity.this,
                                                            GlobalVars.getUsername(LaunchActivity.this),
                                                            text, FriendTask.FriendAction.ADD).execute();
                                                }
                                            }
                                        }).create().show();
                                break;
                        }
                    }
                        })
                        .setTitle("Add a Friend")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create().show();
                }

                return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void setRefreshMenuItem(MenuItem item) {
		mRefreshItem = item;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mIsRunning = true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mIsRunning = false;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

        Broadcast.unregisterLaunchActivity();

		TempSnaps.getInstanceUnsafe().write(this);

        if (mPlayHelper != null) {
            mPlayHelper.dispose();
            mPlayHelper = null;
        }
	}
	
	private boolean checkPlayServices(boolean showDialogOnFailure) {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else {
            Twig.debug(TAG, "Google Play Services unavailable: " + GooglePlayServicesUtil.getErrorString(result));

            if (showDialogOnFailure && GooglePlayServicesUtil.isUserRecoverableError(result)) {
                GooglePlayServicesUtil.getErrorDialog(result, this, REQUEST_PLAY_SERVICES_UPDATE).show();
            }

            return false;
        }

	}
	
	/**
	 * Upon clicking the settings button we come here. Simply the launch 
	 * for the settings activity.
	 */
	private void onSettingsClick() {
		this.startActivityForResult(new Intent(this, Settings.class), Settings.REQUEST_RESULT_CODE);
	}
	
	/**
	 * A significant override so it deserves an explanation. This method intercepts 
	 * back button presses and performs custom actions. This is mainly for the main
	 * menu ViewPager and just returns to the main menu instead of quitting the 
	 * app.
	 */
	@Override
	public void onBackPressed() {
        if (this.getSupportFragmentManager().popBackStackImmediate(TutorialRootFrag.FRAG_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)) {
            return;
        }

		Fragment frag = (Fragment)this.getSupportFragmentManager().findFragmentByTag(ComposerMenuFrag.FRAG_TAG);
		if (frag != null && frag.isFocused()) {
			frag.popFragment();
			return;
		}
		
		ListFragment listfrag = (ListFragment)this.getSupportFragmentManager().findFragmentByTag(SnapThreadListFrag.FRAGTAG);
		if (listfrag != null && listfrag.isFocused()) {
			listfrag.popFragment();
			return;
		}

        listfrag = (ListFragment)this.getSupportFragmentManager().findFragmentByTag(FindContactsListFrag.FRAG_TAG);
        if (listfrag != null && listfrag.isFocused()) {
            listfrag.popFragment();
            return;
        }
		
		frag = (Fragment)this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG);
		if (frag != null && frag.isFocused()) {
			if (((MainFrag)frag).onBackPressed())
				super.onBackPressed();
			return;
		}
		
		super.onBackPressed();
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		super.onActivityResult(requestCode, resultCode, resultData);
		
		switch (requestCode) {
			case ComposerMenuFrag.PICTURE_PICK:
				if (resultCode == Activity.RESULT_OK) {
					Uri returnUri = resultData.getData();
					String[] filePathColumn = {MediaStore.Images.Media.DATA};
					Cursor cursor = this.getContentResolver().query(returnUri, filePathColumn, null, null, null);
					cursor.moveToFirst();
					
					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
					String filePath = cursor.getString(columnIndex);
					cursor.close();
					
					Intent intent = new Intent(this, EditorActivity.class);
					intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, filePath);
					intent.putExtra(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true);
					this.startActivity(intent);
				} else {
					// No image selected from the picker
				}
				break;
			case ComposerMenuFrag.CAMERA_PIC_PICK:
				if (resultCode == Activity.RESULT_OK) {
					File camFile = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, CameraUtil.CAMERA_FILE);
					if (!camFile.exists()) {
						StatMethods.hotBread(this, "Return file couldn't be read", Toast.LENGTH_SHORT);
						return;
					}
					Intent intent = new Intent(this, EditorActivity.class);
					intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, camFile.getAbsolutePath());
					intent.putExtra(SnapEditorBaseFrag.MEDIA_TYPE_KEY, true);
					this.startActivity(intent);
				} else {
					// No image captured from camera
				}
				break;
			case Settings.REQUEST_RESULT_CODE:
				if (resultCode == Settings.RESULT_RESTART_ACTIVITY) {
					if (Build.VERSION.SDK_INT < 11) {
						Intent intent = this.getIntent();
						this.finish();
						this.startActivity(intent);
					} else {
						this.recreate();
					}
				} else if (resultCode == Settings.RESULT_PURCHASE_PREMIUM) {
                    if (!checkPlayServices(true)) {
                        //StatMethods.hotBread(this, "Sorry! It seems Google Play services are not supported by your device!", Toast.LENGTH_SHORT);
                    } else if (mPlayHelper != null) {
                        try {
                            mPlayHelper.launchPurchaseFlow(this, SKU.PREMIUM_FEATURES, SKU.REQUEST_PREMIUM, mPurchaseListener, null);
                        } catch (RuntimeException e) {
                            StatMethods.hotBread(this, "Sorry! Google Play services encountered an error with your request!", Toast.LENGTH_SHORT);
                        }
                    }
                }
				break;
            case SKU.REQUEST_PREMIUM:
                if (mPlayHelper != null) {
                    mPlayHelper.handleActivityResult(requestCode, resultCode, resultData);
                }
                break;
			default:
				break;
		}
	}
	
	/**
	 * Called when clicking on the login button in the login screen.
	 * @param v The button that was pressed.
	 */
	public void onLoginClick(View v)
	{
		EditText vEdit = (EditText)this.findViewById(R.id.loginEdit);
		EditText vPword = (EditText)this.findViewById(R.id.pwordEdit);
		if (vEdit == null || vPword == null)
			return;
		
		String login = vEdit.getText().toString().trim().toLowerCase();
		String pWord = vPword.getText().toString().trim();
		if (StatMethods.IsStringNullOrEmpty(login)) {
			StatMethods.hotBread(this, "Login field is empty", Toast.LENGTH_SHORT);
			return;
		}
		else if (StatMethods.IsStringNullOrEmpty(pWord)) {
			StatMethods.hotBread(this, "Password field is empty", Toast.LENGTH_SHORT);
			return;
		}
		
		if (!StatMethods.isNetworkAvailable(this, true))
			return;
		
		Button loginButton = (Button)this.findViewById(R.id.button1);
		loginButton.setVisibility(View.INVISIBLE);
		ProgressBar pb1 = (ProgressBar)this.findViewById(R.id.progressBar1);
		pb1.setVisibility(View.VISIBLE);
		//new BGLogin(this.getApplicationContext(), null).execute("login", login, pWord);
        new LoginTask(this.getApplicationContext(), login, pWord).execute();
	}
	
	/**
	 * Called when clicking on the new snap button in the main menu screen
	 * @param v The button that called this method
	 */
	public void onNewSnapClick(View v) {
		((MainFrag)this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG)).onNewSnapClick(v);
	}
	
	/**
	 * Called when clicking on the snaps view button in the main menu screen
	 * @param v The button that called this method
	 */
	public void onSnapsClick(View v) {
		((MainFrag)this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG)).onSnapsClick(v);
	}
	
	/**
	 * Called when clicking on the contacts view button in the main menu screen
	 * @param v The button that called this method
	 */
	public void onContactsClick(View v) {
		((MainFrag)this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG)).onContactsClick(v);
	}
	
	/**
	 * Menu loading helper method. Creates a new {@link MainFrag} fragment and 
	 * animates it popping it. The direction of the slide and the page that the
	 * menu gets set to can be set with the chosen with the params.
	 * @param style 0 for a left slide, 1 for a right slide, 2 for no animation
	 * @param page The number of the page that the menu will be set to 
	 */
	private void LoadMenu(int style, int page) {
		if (mDrawerNoLoginLayout.getVisibility() == View.VISIBLE) {
			mDrawerNoLoginLayout.setVisibility(View.GONE);
		}
		if (mDrawerListView.getVisibility() != View.VISIBLE) {
			mDrawerListView.setVisibility(View.VISIBLE);
		}
		
		MainFrag mainFrag = (MainFrag) this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG);
		if (mainFrag == null) {
			mainFrag = new MainFrag();
			Bundle args = new Bundle();
			args.putInt(MainFrag.PAGE_KEY, page);
			mainFrag.setArguments(args);
		}
		
		switch (style) {
			case 3: // Slide down
				this.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out)
				.replace(R.id.launch_container, mainFrag, MainFrag.FRAGTAG).commit();
				break;
			case 2: // No animations
				this.getSupportFragmentManager().beginTransaction().replace(R.id.launch_container, mainFrag, MainFrag.FRAGTAG).commit();
				break;
			case 1: // Slide in to the right
				this.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.push_right_in, R.anim.push_right_out)
				.replace(R.id.launch_container, mainFrag, MainFrag.FRAGTAG).commit();
				break;
			default: // Slide in to the left
				this.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out)
				.replace(R.id.launch_container, mainFrag, MainFrag.FRAGTAG).commit();
				break;
		}
	}
	
	public void ClearFeed(BGFinishedCallback callback) {
		//new BGClear(this.getApplicationContext(), callback).execute(new Void[] {});
		new ClearFeedTask(this.getApplicationContext(), GlobalVars.getUsername(this)).execute();
	}
	
	/**
	 * Logout helper method. Sets the login state to false and swaps in the
	 * {@link LaunchFrag} fragment with an animation.
	 */
	public void Logout() {
		new LogoutTask(this, GlobalVars.getUsername(this)).execute(new String[0]);
		
		GlobalVars.setLoggedIn(this, false);
		
		mDrawerNoLoginLayout.setVisibility(View.VISIBLE);
		mDrawerListView.setVisibility(View.GONE);
		
		LaunchFrag fragger = new LaunchFrag();
		getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.push_up_in, R.anim.push_up_out)
		.replace(R.id.launch_container, fragger, LaunchFrag.FRAGTAG).commit();
		
		List<android.support.v4.app.Fragment> frags = this.getSupportFragmentManager().getFragments();
		for (android.support.v4.app.Fragment frag : frags) {
			if (frag instanceof MainFrag) {
				((MainFrag)frag).popFragment();
			}
		}
	}

    public void onUpdateStart() {
        mHandler.sendEmptyMessage(CustomHandler.ANIMATE_REFRESH_ICON);
    }

    public void onUpdateFinish() {
        mHandler.sendEmptyMessage(CustomHandler.STOP_ANIMATE_REFRESH_ICON);
    }

    public void onLoginComplete(boolean wasSuccessful) {
        if (wasSuccessful) {
            if (GCMUtil.checkPlayServices(this)) {
                new SnapGCMRegistrar(this).setupGoogleCloudManager(true);
            }

            LoadMenu(3, 1);
        } else {
            ProgressBar pb = (ProgressBar)this.findViewById(R.id.progressBar1);
            pb.setVisibility(View.INVISIBLE);
            Button butt = (Button)this.findViewById(R.id.button1);
            butt.setVisibility(View.VISIBLE);
        }
    }

    public void performPurchase(IabHelper.OnIabPurchaseFinishedListener purchaseListener) {
        if (mPlayHelper != null) {
            if (purchaseListener == null) {
                purchaseListener = mPurchaseListener;
            }
            if (checkPlayServices(true)) {
                mPlayHelper.launchPurchaseFlow(this, SKU.PREMIUM_FEATURES, SKU.REQUEST_PREMIUM, purchaseListener, null);
            }
        }
    }

    @Override
    public void objectReady(Statistics obj) {
        if ((this.getIntent() == null || !this.getIntent().getBooleanExtra(NotificationReceiver.LAUNCH_GOTO_FEED, false)) &&
                (SettingsAccessor.getAutoRefreshPref(this) &&
                (System.currentTimeMillis() - obj.getLastUpdate() >= SettingsAccessor.getRefreshTimeLong(this))) &&
                StatMethods.isNetworkAvailable(this, false)) {
            new UpdateTask(this, GlobalVars.getUsername(this)).execute();
        }
    }

    private OnChildClickListener drawerOnChildClickListener = new OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
			if (groupPosition == 1) {
				switch (childPosition) {
					case 0:
						mDrawerLayout.closeDrawer(mDrawerFrame);
						if (!StatMethods.IsCameraAvailable(LaunchActivity.this)){
							StatMethods.hotBread(LaunchActivity.this, "Camera not available", Toast.LENGTH_SHORT);
							return true;
						}
						if (!StatMethods.getExternalWriteable()) {
							StatMethods.hotBread(LaunchActivity.this, "External storage not available", Toast.LENGTH_SHORT);
							return true;
						}
						Intent picIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						picIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment.getExternalStorageDirectory() 
								+ CameraUtil.ROOT_PATH, CameraUtil.CAMERA_FILE)));
						startActivityForResult(picIntent, ComposerMenuFrag.CAMERA_PIC_PICK);
						break;
					case 1:
						mDrawerLayout.closeDrawer(mDrawerFrame);
						Intent photoPick = new Intent(Intent.ACTION_PICK);
						photoPick.setType("image/*");
						startActivityForResult(photoPick, ComposerMenuFrag.PICTURE_PICK);
						break;
					case 2:
						mDrawerLayout.closeDrawer(mDrawerFrame);
						StatMethods.hotBread(LaunchActivity.this, "Not implemented yet", Toast.LENGTH_SHORT);
						break;
					case 3:
						mDrawerLayout.closeDrawer(mDrawerFrame);
						SharedPreferences resendPref = LaunchActivity.this.getSharedPreferences(SnapEditorBaseFrag.RESEND_INFO_KEY, Context.MODE_PRIVATE);
						String filePath = resendPref.getString(SnapEditorBaseFrag.FILE_PATH_KEY, null);
						if (filePath == null) {
							StatMethods.hotBread(LaunchActivity.this, "Missing filePath attribute", Toast.LENGTH_SHORT);
							v.setEnabled(false);
							return true;
						}
						File file = new File(filePath);
						if (!file.exists()) {
							StatMethods.hotBread(LaunchActivity.this, "Previously used picture not found", Toast.LENGTH_SHORT);
							v.setEnabled(false);
							return true;
						}
						
						if (Build.VERSION.SDK_INT < 11) {
							Intent intent = new Intent(LaunchActivity.this, EditorActivity.class);
							intent.putExtra(SnapEditorBaseFrag.RESEND_BOOL_KEY, true);
							startActivity(intent);
						} else {
							Intent intent = new Intent(LaunchActivity.this, CaptureActivity.class);
							intent.putExtra(SnapEditorBaseFrag.RESEND_BOOL_KEY, true);
							startActivity(intent);
						}
						break;
				}
			}
			return true;
		}
	};
	
	private OnGroupClickListener drawerOnGroupClickListener = new OnGroupClickListener() {
		@Override
		public boolean onGroupClick(ExpandableListView parent, View view, int groupPosition, long id) {
			switch (groupPosition) {
			case 0: // Home
				mDrawerLayout.closeDrawer(mDrawerFrame);
				SnapThreadListFrag snapThread = (SnapThreadListFrag)LaunchActivity.this.getSupportFragmentManager().findFragmentByTag(SnapThreadListFrag.FRAGTAG);
				MainFrag menuFrag = (MainFrag)LaunchActivity.this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG);
				if (snapThread != null && !snapThread.isDetached()) {
					LaunchActivity.this.getSupportFragmentManager().beginTransaction().detach(snapThread)
					.setCustomAnimations(R.anim.push_right_in, R.anim.push_right_out).commit();
					if (menuFrag != null) {
						LaunchActivity.this.getSupportFragmentManager().beginTransaction().add(menuFrag, MainFrag.FRAGTAG).commit();
						menuFrag.goToMenu();
					} else {
						LoadMenu(1,1);
					}
				} else if (menuFrag != null && !menuFrag.isDetached()) {
					menuFrag.goToMenu();
				} else {
					StatMethods.hotBread(LaunchActivity.this, "Not implemented yet", Toast.LENGTH_SHORT);
				}
				break;
			case 1: // New Snap Group
				break;
			case 2: // Snaps
				mDrawerLayout.closeDrawer(mDrawerFrame);
				snapThread = (SnapThreadListFrag)LaunchActivity.this.getSupportFragmentManager().findFragmentByTag(SnapThreadListFrag.FRAGTAG);
				menuFrag = (MainFrag)LaunchActivity.this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG);
				if (snapThread != null && !snapThread.isDetached()) {
					 LaunchActivity.this.getSupportFragmentManager().beginTransaction().detach(snapThread)
					.setCustomAnimations(R.anim.push_right_in, R.anim.push_right_out).commit();
					if (menuFrag != null) {
						LaunchActivity.this.getSupportFragmentManager().beginTransaction().add(menuFrag, MainFrag.FRAGTAG).commit();
						menuFrag.goToSnaps();
					}
					else {
						LoadMenu(1,0);
					} 
				} else if (menuFrag != null && !menuFrag.isDetached()) {
					menuFrag.goToSnaps();
				} else {
					StatMethods.hotBread(LaunchActivity.this, "Not implemented yet", Toast.LENGTH_SHORT);
				}
				break;
			case 3: // Contacts
				mDrawerLayout.closeDrawer(mDrawerFrame);
				snapThread = (SnapThreadListFrag)LaunchActivity.this.getSupportFragmentManager().findFragmentByTag(SnapThreadListFrag.FRAGTAG);
				menuFrag = (MainFrag)LaunchActivity.this.getSupportFragmentManager().findFragmentByTag(MainFrag.FRAGTAG);
				if (snapThread != null && !snapThread.isDetached()) {
					LaunchActivity.this.getSupportFragmentManager().beginTransaction().detach(snapThread)
					.setCustomAnimations(R.anim.push_right_in, R.anim.push_right_out).commit();
					if (menuFrag != null) {
						LaunchActivity.this.getSupportFragmentManager().beginTransaction().add(menuFrag, MainFrag.FRAGTAG).commit();
						menuFrag.goToContacts();
					} else 
						LoadMenu(1,2);
				} else if (menuFrag != null && !menuFrag.isDetached()) {
					menuFrag.goToContacts();
				} else {
					StatMethods.hotBread(LaunchActivity.this, "Not implemented yet", Toast.LENGTH_SHORT);
				}
				break;
			case 4: // Settings
				mDrawerLayout.closeDrawer(mDrawerFrame);
				onSettingsClick();
				break;
			case 5: // Refresh
				mDrawerLayout.closeDrawer(mDrawerFrame);
                if (StatMethods.isNetworkAvailable(LaunchActivity.this, true)) {
				    new UpdateTask(LaunchActivity.this, GlobalVars.getUsername(LaunchActivity.this)).execute();
                }
				break;
			case 6: // Sign Out
				mDrawerLayout.closeDrawer(mDrawerFrame);
				Logout();
				break;
			}
			return false;
		}
	};

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            Twig.debug(TAG, "Query inventory finished");

            if (mPlayHelper == null) {
                return;
            }

            if (result.isFailure()) {
                Twig.warning(TAG, "Inventory query failure: " + result);
                //StatMethods.hotBread(LaunchActivity.this, "Inventory query failure: " + result, Toast.LENGTH_SHORT);
                return;
            }

            Twig.debug(TAG, "Query inventory successful");

            Purchase premiumPurchase = inv.getPurchase(SKU.PREMIUM_FEATURES);
            SettingsAccessor.setPremium(LaunchActivity.this, premiumPurchase != null);
        }
    };

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isFailure()) {
                Twig.debug(TAG, "Error purchasing: " + result);
            } else if (info.getSku().equals(SKU.PREMIUM_FEATURES)) {
                SettingsAccessor.setPremium(LaunchActivity.this, true);
                StatMethods.hotBread(LaunchActivity.this, "Purchase successful. Premium features enabled! Thank-you!", Toast.LENGTH_LONG);
            }
            LaunchActivity.this.startActivityForResult(new Intent(LaunchActivity.this, Settings.class), Settings.REQUEST_RESULT_CODE);

        }
    };

	private class CustomHandler extends Handler {
		public static final int ANIMATE_REFRESH_ICON = 0x10;
		public static final int STOP_ANIMATE_REFRESH_ICON = 0x100;
		
		@Override
        @SuppressLint("NewApi")
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case ANIMATE_REFRESH_ICON:
					if (mRefreshItem != null) {
                        if (Build.VERSION.SDK_INT < 11) {
                            mRefreshItem.setIcon(R.drawable.custom_progress);
                            AnimationDrawable drawable = (AnimationDrawable)mRefreshItem.getIcon();
                            drawable.start();
                        } else {
                            ImageView imgView = (ImageView) LaunchActivity.this.getLayoutInflater().inflate(R.layout.refresh_menu_item);
                            Animation rotation = AnimationUtils.loadAnimation(LaunchActivity.this, R.anim.refresh_icon);
                            rotation.setRepeatCount(Animation.INFINITE);
                            imgView.startAnimation(rotation);
                            mRefreshItem.setActionView(imgView);
                        }
					} else {
						this.sendEmptyMessageDelayed(ANIMATE_REFRESH_ICON, 50);
					}
					break;
				case STOP_ANIMATE_REFRESH_ICON:
					this.removeMessages(ANIMATE_REFRESH_ICON);
					if (mRefreshItem != null) {
                        if (Build.VERSION.SDK_INT < 11) {
						    mRefreshItem.setIcon(R.drawable.ic_menu_refresh);
                        } else {
                            View view = mRefreshItem.getActionView();
                            if (view != null) {
                                view.clearAnimation();
                            }
                        }
					} else {
						this.sendEmptyMessageDelayed(STOP_ANIMATE_REFRESH_ICON, 50);
					}
					break;
			}
		}
	}
}


