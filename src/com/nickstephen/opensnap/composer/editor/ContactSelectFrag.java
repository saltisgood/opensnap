package com.nickstephen.opensnap.composer.editor;

import java.io.File;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.nickstephen.opensnap.gui.BaseContactSelectFrag;

public class ContactSelectFrag extends BaseContactSelectFrag {
	public ContactSelectFrag() {
		this.setRetainInstance(true);
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
