package com.nickstephen.opensnap.gui;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;

public class DragTouchListener implements OnTouchListener {
	private int prevX = -1;
	private int prevY = -1;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_MOVE:
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)v.getLayoutParams();
				int x = (int)event.getRawX();
				int y = (int)event.getRawY();
				
				if (prevX != -1 && prevY != -1) {
					int dx = x - prevX;
					int dy = y - prevY;
					
					params.setMargins(params.leftMargin + dx, params.topMargin + dy, params.rightMargin - dx, params.bottomMargin - dy);
					
					v.setLayoutParams(params);
				}
				
				prevX = x;
				prevY = y;
				break;
			case MotionEvent.ACTION_UP:
				prevX = -1;
				prevY = -1;
				break;
		}
		return true;
	}
}
