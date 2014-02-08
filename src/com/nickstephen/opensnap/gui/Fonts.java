package com.nickstephen.opensnap.gui;

import android.content.Context;
import android.graphics.Typeface;

/**
 * DEPRECATED - Only here so I don't have to think about this later
 * @author Nick's Laptop
 *
 */
public abstract class Fonts {
	public static final int ROBOTO = 0;
	public static final int ROBOTO_BOLD_CONDENSED = 1;
	
	private Fonts(Context ctxt){
		init(ctxt);
	}
	
	public abstract Typeface getTypeface();
	protected abstract void init(Context ctxt);

	public static class Roboto extends Fonts {
		private static Typeface _typeface;
		
		public Roboto(Context ctxt) {
			super(ctxt);
		}
		
		@Override
		public Typeface getTypeface() {
			return _typeface;
		}

		@Override
		protected void init(Context ctxt) {
			if (_typeface == null) {
				_typeface = Typeface.createFromAsset(ctxt.getAssets(), "fonts/Roboto-Regular.ttf");
			}
		}
	}
	
	public static class RobotoBoldCondensed extends Fonts {
		private static Typeface _typeface;
		
		public RobotoBoldCondensed(Context ctxt) {
			super(ctxt);
		}
		
		@Override
		public Typeface getTypeface() {
			return _typeface;
		}
		
		@Override
		protected void init(Context ctxt) {
			if (_typeface == null) {
				_typeface = Typeface.createFromAsset(ctxt.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
			}
		}
	}
}
