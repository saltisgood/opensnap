package com.nickstephen.opensnap.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.gui.Fonts.Roboto;
import com.nickstephen.opensnap.gui.Fonts.RobotoBoldCondensed;

public class RobotoTextView extends TextView {

	public RobotoTextView(Context context) {
		super(context);
	}

	public RobotoTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttributes(context, attrs);
	}

	public RobotoTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		parseAttributes(context, attrs);
	}

	private void parseAttributes(Context context, AttributeSet attrs) {
		TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.RobotoTextView);
		
		int typeface = values.getInt(R.styleable.RobotoTextView_typeface, 0);
		switch (typeface) {
			case Fonts.ROBOTO:
				this.setTypeface(new Roboto(context).getTypeface());
				break;
			case Fonts.ROBOTO_BOLD_CONDENSED:
				//this.setTypeface(new Roboto(context).getTypeface());
				this.setTypeface(new RobotoBoldCondensed(context).getTypeface());
				break;
		}
		values.recycle();
	}
}
