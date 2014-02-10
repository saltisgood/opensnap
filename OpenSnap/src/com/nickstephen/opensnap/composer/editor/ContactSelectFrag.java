package com.nickstephen.opensnap.composer.editor;

import java.io.File;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.main.tuts.TutorialContactSelectFrag;
import com.nickstephen.opensnap.main.tuts.TutorialRootFrag;

public class ContactSelectFrag extends BaseContactSelectFrag {
	public ContactSelectFrag() {
		this.setRetainInstance(true);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        Bundle args = this.getArguments();
        if (args != null){
            if (args.getBoolean(SnapEditorBaseFrag.FIRST_TIME_KEY, false)) {
                this.getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.fragment_container, new TutorialContactSelectFrag(), TutorialContactSelectFrag.FRAG_TAG)
                        .addToBackStack(TutorialRootFrag.FRAG_TAG)
                        .commit();
            }
        }
    }

    @Override
    @SuppressLint("NewApi")
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setFocused(true);
		
		if (Build.VERSION.SDK_INT < 11) {
			((EditorActivity)this.getActivity()).getSupportActionBar().show();
		} else {
			((EditorActivity)this.getActivity()).getActionBar().show();
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.view_tut, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_tut:
                this.getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.fragment_container, new TutorialContactSelectFrag(), TutorialContactSelectFrag.FRAG_TAG)
                        .addToBackStack(TutorialRootFrag.FRAG_TAG)
                        .commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    @SuppressLint("NewApi")
	public final void popFragment() {
		if (Build.VERSION.SDK_INT < 11) {
			((Activity)this.getActivity()).getSupportActionBar().hide();
		} else {
			((Activity)this.getActivity()).getActionBar().hide();
		}
		
		super.popFragment();
	}
	
	@Override
	public final void onFragmentPopped() {
		super.onFragmentPopped();
		
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
	}
}
