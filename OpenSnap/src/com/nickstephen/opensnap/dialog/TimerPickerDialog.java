package com.nickstephen.opensnap.dialog;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.NumberPicker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;

import com.nickstephen.opensnap.R;

public class TimerPickerDialog extends Activity {
	public static final String RESULT_TIME_VAL_KEY = "time_val";
	public static final String CURRENT_TIME_VAL_KEY = "time_current_val";
	
	private NumberPicker mPicker;

	@SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.time_picker_dialog);
		
		Display display = this.getWindowManager().getDefaultDisplay();
		int width;
		if (Build.VERSION.SDK_INT < 13) {
			width = display.getWidth();
		} else {
			Point outSize = new Point();
			display.getSize(outSize);
			width = outSize.x;
		}
		
		LayoutParams params = this.getWindow().getAttributes();
		params.width = width - 30;
		this.getWindow().setAttributes(params);
		
		mPicker = (NumberPicker)this.findViewById(R.id.no_picker_time);
		mPicker.setEditable(false);
		mPicker.setMinValue(0);
		mPicker.setMaxValue(9);
		mPicker.setDisplayedValues(new String[] { "1 second", "2 seconds", "3 seconds", "4 seconds", "5 seconds", "6 seconds",
				"7 seconds", "8 seconds", "9 seconds", "10 seconds" });
		
		int val;
		if (this.getIntent() != null && (val = this.getIntent().getIntExtra(CURRENT_TIME_VAL_KEY, -1)) != -1) {
			mPicker.setValue(val - 1);
		}
		mPicker.setWrapSelectorWheel(false);
		
		View button = this.findViewById(R.id.cancel_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TimerPickerDialog.this.setResult(RESULT_CANCELED);
				TimerPickerDialog.this.finish();
			}
		});
		
		button = this.findViewById(R.id.ok_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(RESULT_TIME_VAL_KEY, mPicker.getValue() + 1);
				TimerPickerDialog.this.setResult(RESULT_OK, intent);
				TimerPickerDialog.this.finish();
			}
		});
	}
}
