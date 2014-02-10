package com.nickstephen.opensnap.dialog;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.nickstephen.opensnap.R;

public class DialogActivity extends Activity {
	private int prevX = -1;
	private int prevY = -1;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT <= 13) {
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		}
		this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_dialog);
		WindowManager.LayoutParams params = this.getWindow().getAttributes();
		
		params.gravity = Gravity.TOP | Gravity.LEFT;
		this.getWindow().setAttributes(params);
		
		Button button = (Button)this.findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DialogActivity.this.finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.dialog, menu);
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_MOVE:
				WindowManager.LayoutParams params = this.getWindow().getAttributes();
				int x = (int)event.getRawX();
				int y = (int)event.getRawY();
				
				if (prevX != -1 && prevY != -1) {
					int dx = x - prevX;
					int dy = y - prevY;
					
					params.x += dx;
					params.y += dy;
					this.getWindow().setAttributes(params);
				}
				
				prevX = x;
				prevY = y;
				break;
			case MotionEvent.ACTION_UP:
				prevX = -1;
				prevY = -1;
				break;
		}
		return false;
	}
}
