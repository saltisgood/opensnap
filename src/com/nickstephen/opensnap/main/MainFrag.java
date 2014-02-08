package com.nickstephen.opensnap.main;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.ViewPager;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.lib.gui.Fragment;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.main.tuts.TutorialIntroFrag;

/**
 * An extension of {@link Fragment} that is the main menu fragment container. This is an important
 * distinction. It is not the fragment that appears as the main menu, it's the fragment that 
 * CONTAINS the main menu, as well as the snaps and contacts stuff.
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class MainFrag extends Fragment {
	/**
	 * The {@link Fragment} TAG that's used to uniquely describe this fragment.
	 * Normally used when querying a {@link FragmentManager}.
	 */
	public static final String FRAGTAG = "MenuFrag";
	/**
	 * The key for a page argument to be passed to the fragment 
	 */
	public static final String PAGE_KEY = "PAGEKEY";
	
	/**
	 * The {@link ViewPager} that controls the animation/scrolling of the main pages
	 */
	private ViewPager menuPager;
	/**
	 * The {@link MainPageAdapter} that interfaces between the main frags and the {@link #menuPager}
	 */
	private MainPageAdapter menuPagerAdapter;
	/**
	 * The current page being displayed to screen, as a {@link PageStyle} enum
	 */
	private PageStyle CurrentView;
	/**
	 * A variable that can keep track of the {@link SnapViewerListFrag} fragment currently being displayed. This
	 * allows the {@link ViewPager} to return to the fragment without having to recreate it.
	 */
	private SnapViewerListFrag activeSnaps;
	
	public MainFrag() {}
	
	@Override
	public void onCreate(Bundle bnd) {
		setHasOptionsMenu(true);
		super.onCreate(bnd);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.frag_menu, menu);
		MenuItem refreshItem = menu.findItem(R.id.refresh_menu);
		((LaunchActivity)this.getActivity()).setRefreshMenuItem(refreshItem);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		View v = inflater.inflate(R.layout.main_menu_frag, null);
		menuPager = (ViewPager)v.findViewById(R.id.menu_pager);
		menuPagerAdapter = new MainPageAdapter(this.getFragmentManager());
		menuPager.setAdapter(menuPagerAdapter);
		menuPager.setCurrentItem(this.getArguments().getInt(MainFrag.PAGE_KEY, 1));
		switch (this.getArguments().getInt(MainFrag.PAGE_KEY, 1)) {
			case 0:
				CurrentView = PageStyle.SNAPS;
				break;
			case 1:
				CurrentView = PageStyle.MAIN;
				break;
			case 2:
				CurrentView = PageStyle.FRIENDS;
				break;
		}
		menuPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}

			@Override
			public void onPageSelected(int pos) {
				switch (pos) {
				case 0:
					CurrentView = PageStyle.SNAPS;
					break;
				case 1:
					CurrentView = PageStyle.MAIN;
					break;
				case 2:
					CurrentView = PageStyle.FRIENDS;
					break;
				}
			}
			
		});

		return v;
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (true) { // TODO: Fix up condition later
            this.getFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.launch_container, new TutorialIntroFrag(), TutorialIntroFrag.FRAG_TAG)
                    .addToBackStack(TutorialIntroFrag.FRAG_TAG)
                    .commit();
        }
    }

    /**
	 * Called from the {@link LaunchActivity} activity as an intercept to pressing back 
	 * @return True if the super method should be called, false if the call was intercepted and shouldn't be passed on
	 */
	public Boolean onBackPressed() {
		/* if (CurrentView == PageStyle.SNAPS && ((SnapViewerListFrag)menuPagerAdapter.getItem(0)).snapMode){
			((SnapViewerListFrag)menuPagerAdapter.getItem(0)).onBackPressed();
			return false; */
		//} else if (CurrentView == PageStyle.SNAPS) {
			if (CurrentView == PageStyle.SNAPS) {
			menuPager.setCurrentItem(1);
			return false;
		} else if (CurrentView == PageStyle.FRIENDS) {
			menuPager.setCurrentItem(1);
			return false;
		}
		return true;
	}
	
	@Override
	public void popFragment() {
		this.setFocused(false);
	}
	
	/**
	 * Called from the {@link LaunchActivity} activity to compose a new snap
	 * @param v The button that was pressed
	 */
	public void onNewSnapClick(View v){
		//this.getActivity().startActivityFromFragment(this, new Intent(this.getActivity(), ComposeSnap.class), -1);
		ComposerMenuFrag menu = new ComposerMenuFrag();
		this.getFragmentManager().beginTransaction()
			.setCustomAnimations(R.anim.push_down_in, R.anim.push_down_out, R.anim.push_up_in, R.anim.push_up_out)
			.add(R.id.launch_container, menu, ComposerMenuFrag.FRAG_TAG).addToBackStack(null).commit();
	}
	
	/**
	 * Called from the {@link LaunchActivity} activity to move to the {@link SnapViewerListFrag} fragment
	 * @param v The button that was pressed
	 */
	public void onSnapsClick(View v) {
		menuPager.setCurrentItem(0, true);
	}
	
	/**
	 * Called from the {@link LaunchActivity} activity to move to the {@link ContactViewerListFrag} fragment
	 * @param v The button that was pressed
	 */
	public void onContactsClick(View v) {
		menuPager.setCurrentItem(2, true);
	}

	public void goToMenu() {
		menuPager.setCurrentItem(1, true);
		CurrentView = PageStyle.MAIN;
	}
	
	public void goToSnaps() {
		menuPager.setCurrentItem(0, true);
		CurrentView = PageStyle.SNAPS;
	}
	
	public void goToContacts() {
		menuPager.setCurrentItem(2, true);
		CurrentView = PageStyle.FRIENDS;
	}
	
	public MainPageAdapter getViewPagerAdapter() {
		return menuPagerAdapter;
	}
	
	/**
	 * An extension to the {@link FragmentStatePagerAdapter} that feeds main menu fragment
	 * pages to the main menu {@link ViewPager}.
	 * @author Nick's Laptop
	 */
	private class MainPageAdapter extends FragmentStatePagerAdapter {
		private MainMenuFrag mainMenuFrag; 
		
		public MainPageAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public android.support.v4.app.Fragment getItem(int page) {
			switch (page) {
				case 0:
					if (activeSnaps == null)
						activeSnaps = new SnapViewerListFrag();
					return activeSnaps;
				case 1: 
					if (mainMenuFrag == null) {
						mainMenuFrag = new MainMenuFrag();
					}
					return mainMenuFrag;
				case 2:
					return new ContactViewerListFrag();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}
		
	}
	
	/**
	 * A simple enum that is used for describing the current page active in the this fragment
	 * view pager
	 * @author Nick's Laptop
	 */
	public enum PageStyle {
		SNAPS, FRIENDS, MAIN
	}
}