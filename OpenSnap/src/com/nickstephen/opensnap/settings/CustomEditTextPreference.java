package com.nickstephen.opensnap.settings;

import org.holoeverywhere.preference.EditTextPreference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class CustomEditTextPreference extends EditTextPreference {
	public CustomEditTextPreference(Context context) {
		super(context);
	}

	public CustomEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomEditTextPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected View onCreateDialogView(Context context) {
		return getEditText();
	}
}
