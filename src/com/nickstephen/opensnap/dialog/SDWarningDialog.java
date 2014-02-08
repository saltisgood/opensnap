package com.nickstephen.opensnap.dialog;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.preference.PreferenceManager;
import org.holoeverywhere.widget.CheckBox;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.settings.SettingsAccessor;

public final class SDWarningDialog extends Activity {
	private CheckBox mDoNotShowCheckBox;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.sd_warning_dialog);
		
		WindowManager.LayoutParams params = this.getWindow().getAttributes();
		params.gravity = Gravity.CENTER;
		this.getWindow().setAttributes(params);
		
		this.findViewById(R.id.content_container).setOnClickListener(checkContainerClicker);
		this.findViewById(R.id.ok_button).setOnClickListener(okButtonClicker);
		mDoNotShowCheckBox = (CheckBox)this.findViewById(R.id.checkBox1);
	}
	
	private final OnClickListener checkContainerClicker = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mDoNotShowCheckBox.toggle();
		}
	};
	
	private final OnClickListener okButtonClicker = new OnClickListener() {
        @SuppressLint("NewApi")
		@Override
		public void onClick(View v) {
			if (mDoNotShowCheckBox.isChecked()) {
				SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(SDWarningDialog.this).edit();
				prefsEditor.putBoolean(SettingsAccessor.SHOW_SD_WARNING_KEY, false);
                if (Build.VERSION.SDK_INT >= 9) {
				    prefsEditor.apply();
                } else {
                    prefsEditor.commit();
                }
			}
			SDWarningDialog.this.finish();
		}
	};
}
