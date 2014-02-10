package com.nickstephen.opensnap.dialog;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.SeekBar;
import org.holoeverywhere.widget.SeekBar.OnSeekBarChangeListener;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.nickstephen.opensnap.R;

public class ColorPickerDialog extends Activity {
	public static final int REQUEST_COLOR = 756;
	public static final String RED_KEY = "rosered";
	public static final String GREEN_KEY  = "verde";
	public static final String BLUE_KEY = "I'm blue dabadee";
	
	private SeekBar redBar;
	private SeekBar greenBar;
	private SeekBar blueBar;
	private Canvas colorDisplay;
	private ImageView colorImg;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT <= 13) {
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		}
		this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.color_picker_dialog);
		WindowManager.LayoutParams params = this.getWindow().getAttributes();
		
		params.gravity = Gravity.CENTER;
		this.getWindow().setAttributes(params);
		
		Button button = (Button)this.findViewById(R.id.ok_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				intent.putExtra(RED_KEY, getRedValue());
				intent.putExtra(GREEN_KEY, getGreenValue());
				intent.putExtra(BLUE_KEY, getBlueValue());
				ColorPickerDialog.this.setResult(RESULT_OK, intent);
				ColorPickerDialog.this.finish();
			}
		});
		
		button = (Button)this.findViewById(R.id.cancel_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				ColorPickerDialog.this.setResult(RESULT_CANCELED);
				ColorPickerDialog.this.finish();
			}
		});
		
		redBar = (SeekBar)this.findViewById(R.id.red_bar);
		greenBar = (SeekBar)this.findViewById(R.id.green_bar);
		blueBar = (SeekBar)this.findViewById(R.id.blue_bar);
		redBar.setOnSeekBarChangeListener(seekBarListener);
		greenBar.setOnSeekBarChangeListener(seekBarListener);
		blueBar.setOnSeekBarChangeListener(seekBarListener);
		
		colorImg = (ImageView)this.findViewById(R.id.color_display);
		float scale = this.getResources().getDisplayMetrics().density;
		Bitmap colorMap = Bitmap.createBitmap((int) (230 * scale + 0.5f), (int) (40 * scale + 0.5f), Config.ARGB_8888);
		colorDisplay = new Canvas(colorMap);
		colorImg.setImageBitmap(colorMap);
		
		int val = this.getIntent().getIntExtra(RED_KEY, -1);
		if (val != -1) {
			redBar.setProgress(val);
		}
		if ((val = this.getIntent().getIntExtra(GREEN_KEY, -1)) != -1) {
			greenBar.setProgress(val);
		}
		if ((val = this.getIntent().getIntExtra(BLUE_KEY, -1)) != -1) {
			blueBar.setProgress(val);
		}
		colorDisplay.drawRGB(getRedValue(), getGreenValue(), getBlueValue());
		colorImg.invalidate();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.dialog, menu);
		return true;
	}
	
	private int getRedValue() {
		if (redBar != null) {
			return redBar.getProgress();
		}
		return 255;
	}
	
	private int getGreenValue() {
		if (greenBar != null) {
			return greenBar.getProgress();
		}
		return 255;
	}
	
	private int getBlueValue() {
		if (blueBar != null) {
			return blueBar.getProgress();
		}
		return 255;
	}
	
	private final OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser) {
				if (colorDisplay != null) {
					colorDisplay.drawRGB(getRedValue(), getGreenValue(), getBlueValue());
					if (colorImg != null) {
						colorImg.invalidate();
					}
				}
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// ignore
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// ignore
		}
	};
}
