package com.nickstephen.opensnap.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.preference.SharedPreferences;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.FaceDetector;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.misc.BitmapUtil;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.dialog.ColorPickerDialog;
import com.nickstephen.opensnap.dialog.TimerPickerDialog;
import com.nickstephen.opensnap.util.misc.CameraUtil;

public abstract class SnapEditorPicFrag extends SnapEditorBaseFrag {
	public static final int TIMER_PICK_REQUEST_CODE = 9877;
	
	protected static final String EDIT_FILENAME = "edit.jpg";
	
	private static final int NON_COLOUR = 0x22000000;
	
	private final AtomicBoolean working = new AtomicBoolean();
	private int modFlags = 0;
	private boolean modded = false;
	private Bitmap drawBitmap;
	private Canvas drawCanvas;
	private float prevX = -1.0f;
	private float prevY = -1.0f;
	private int drawingColor = NON_COLOUR;
	private ImageView colourPickImg;
	private int orientation = -1;
	private Integer displayTime = 5;
	private TextView displayText;
    private View[] mButtonViews = new View[5];
    private AlphaAnimation[] mButtonAnims = new AlphaAnimation[5];
	
	/**
	 * 0 = disabled
	 * 1 = draw enabled
	 * 2 = erase enabled
	 */
	private int drawMode = 0;
	@SuppressWarnings("rawtypes")
	private List<AsyncTask> workers = new ArrayList<AsyncTask>();
	
	public SnapEditorPicFrag() {
		this.setRetainInstance(true);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
		setFocused(true);
		
		View v = inflater.inflate(R.layout.snap_edit, null);
		
		if (!isResend()) {
			Intent intent = new Intent();
			intent.putExtra(SnapEditorBaseFrag.FILE_PATH_KEY, getFilePath());
			this.getActivity().setResult(Activity.RESULT_CANCELED, intent);
		}
		
		int prevOrientation = orientation;
		int rot = this.getActivity().getWindowManager().getDefaultDisplay().getRotation();
		switch (rot) {
			case Surface.ROTATION_90:
				orientation = 1;
				break;
			case Surface.ROTATION_270:
				orientation = 2;
				break;
			case Surface.ROTATION_180:
				orientation = 3;
				break;
			case Surface.ROTATION_0:
			default:
				orientation = 0;
				break;
		}
		
		final ImageView img = (ImageView)v.findViewById(R.id.snap_img);
		workers.add(new ImageLoad(img).execute(getFilePath()));
		img.setOnTouchListener(mediaOnTouchListener);
		
		mCaption = (EditText)v.findViewById(R.id.snap_caption);
		mCaption.setOnTouchListener(captionOnTouchListener);
		mCaption.addTextChangedListener(captionTextWatcher);
        mCaption.setOnEditorActionListener(captionOnEditorActionListener);
		
		View cancel = v.findViewById(R.id.back_button);
		cancel.setOnClickListener(cancelOnClickListener);
        mButtonViews[0] = cancel;
		
		View send = v.findViewById(R.id.send_button);
		send.setOnClickListener(sendOnClickListener);
        mButtonViews[1] = send;
		
		View save = v.findViewById(R.id.save_button);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				writeFile(CameraUtil.getOutputMediaFile(getActivity(), CameraUtil.MEDIA_TYPE_IMAGE), true);
			}
		});
        mButtonViews[2] = save;
		
		displayText = (TextView)v.findViewById(R.id.pick_time_choice);
		displayText.setText(displayTime.toString());
		displayText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), TimerPickerDialog.class);
				intent.putExtra(TimerPickerDialog.CURRENT_TIME_VAL_KEY, displayTime);
				startActivityForResult(intent, TIMER_PICK_REQUEST_CODE);
			}
		});
        mButtonViews[3] = displayText;
		
		if (modFlags == 0 || modded) {
			
		}
		
		img.setFocusable(true);
		img.requestFocus();
		
		ImageView drawImage = (ImageView)v.findViewById(R.id.snap_edit_draw_surface);
		drawImage.setOnTouchListener(drawListener);
		Display display = this.getActivity().getWindowManager().getDefaultDisplay();
		int width, height;
		if (Build.VERSION.SDK_INT >= 13) {
			Point point = new Point();
			display.getSize(point);
			width = point.x;
			height = point.y;
		} else {
			width = display.getWidth();
			height = display.getHeight();
		}
		if (drawBitmap == null) {
			drawBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			drawCanvas = new Canvas(drawBitmap);
		} else if (prevOrientation != orientation) {
			Bitmap tempBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			drawCanvas = new Canvas(tempBitmap);
			Matrix matrix = new Matrix();
			int rotationAngle = 180;
			switch (prevOrientation) {
				case 0:
					if (orientation == 1) {
						rotationAngle = 270;
					} else if (orientation == 2) {
						rotationAngle = 90;
					}
					break;
				case 1:
					if (orientation == 0) {
						rotationAngle = 90;
					} else if (orientation == 3) {
						rotationAngle = 270;
					}
					break;
				case 2:
					if (orientation == 0) {
						rotationAngle = 270;
					} else if (orientation == 3) {
						rotationAngle = 90;
					}
					break;
				case 3:
					if (orientation == 1) {
						rotationAngle = 90;
					} else if (orientation == 2) {
						rotationAngle = 270;
					}
					break;
			}
			matrix.setRotate(rotationAngle, (float) drawBitmap.getWidth() / 2, (float) drawBitmap.getHeight() / 2);
			Bitmap rotateBitmap = Bitmap.createBitmap(drawBitmap, 0, 0, height, width, matrix, true);
			if (rotateBitmap != drawBitmap) {
				drawBitmap.recycle();
				drawBitmap = null;
			}
			
			drawCanvas.drawBitmap(rotateBitmap, new Matrix(), null);
			rotateBitmap.recycle();
			rotateBitmap = null;
			drawBitmap = tempBitmap;
		}
		drawImage.setImageBitmap(drawBitmap);
		
		colourPickImg = (ImageView)v.findViewById(R.id.color_pick_button);
		switch (drawMode) {
			case 0:
				colourPickImg.setImageResource(R.drawable.pencil_disabled);
				break;
			case 1:
				colourPickImg.setImageResource(R.drawable.pencil_enabled);
				break;
			case 2:
				colourPickImg.setImageResource(R.drawable.eraser);
				break;
		}
		colourPickImg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (drawMode) {
					case 0:
						drawMode++;
						((ImageView)v).setImageResource(R.drawable.pencil_enabled);
						break;
					case 1:
						drawMode++;
						((ImageView)v).setImageResource(R.drawable.eraser);
						break;
					case 2:
						drawMode = 0;
						((ImageView)v).setImageResource(R.drawable.pencil_disabled);
						break;
				}
			}
		});
		colourPickImg.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				
				Intent intent = new Intent(SnapEditorPicFrag.this.getActivity(), ColorPickerDialog.class);
				if (drawingColor != NON_COLOUR) {
					intent.putExtra(ColorPickerDialog.RED_KEY, Color.red(drawingColor));
					intent.putExtra(ColorPickerDialog.GREEN_KEY, Color.green(drawingColor));
					intent.putExtra(ColorPickerDialog.BLUE_KEY, Color.blue(drawingColor));
				}
				SnapEditorPicFrag.this.startActivityForResult(intent, ColorPickerDialog.REQUEST_COLOR);
				return true;
			}
		});
        mButtonViews[4] = colourPickImg;
		
		return v;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		//setResendInfo(getFilePath(), timeSelector.getSelectedItemPosition() + 1);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		/* if (getShouldDelete()) {
			File file = new File(getFilePath());
			file.delete();
		} */
		
		for (int i = 0; i < workers.size(); i++) {
			workers.get(i).cancel(true);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ColorPickerDialog.REQUEST_COLOR:
				if (resultCode == Activity.RESULT_OK && data != null) {
					int red = data.getIntExtra(ColorPickerDialog.RED_KEY, 255);
					int green = data.getIntExtra(ColorPickerDialog.GREEN_KEY, 255);
					int blue = data.getIntExtra(ColorPickerDialog.BLUE_KEY, 255);
					drawingColor = Color.rgb(red, green, blue);
				}
				break;
			case TIMER_PICK_REQUEST_CODE:
				if (resultCode == Activity.RESULT_OK && data != null) {
					displayTime = data.getIntExtra(TimerPickerDialog.RESULT_TIME_VAL_KEY, 5);
					displayText.setText(displayTime.toString());
				}
				break;
		}
	}

	@Override
	protected void setResendInfo(String fileName, int time) {
		SharedPreferences resendPref = (SharedPreferences) this.getActivity().getSharedPreferences(RESEND_INFO_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = resendPref.edit();
		String text = mCaption.getText().toString();
		if (text == null)
			text = "";
		editor.putString(CAPTION_KEY, text);
		editor.putString(FILE_PATH_KEY, fileName);
		editor.putInt(SNAP_TIME_KEY, time);
		editor.putBoolean(MEDIA_TYPE_KEY, true);
		editor.commit();
	}

	@Override
	protected void handleTextInput(String input) {
		ImageView img = (ImageView) this.getView().findViewById(R.id.snap_img);
		if (input.compareTo("flipside") == 0) {
			workers.add(new ImageMod(img).execute(ImageMod.FLIP_INT));
		} else if (input.compareTo("50shades") == 0) {
			workers.add(new ImageMod(img).execute(ImageMod.GREY_INT));
		} else if (input.compareTo("hispter") == 0) {
			workers.add(new ImageMod(img).execute(ImageMod.SEPIA_INT));
		} else if (input.compareTo("mchammer") == 0) {
			workers.add(new ImageMod(img).execute(ImageMod.BLOCK_INT));
		} else if (input.compareTo("reset") == 0) {
			workers.add(new ImageMod(img).execute(ImageMod.RESET_INT));
		} else if (input.compareTo("itsyabirfday") == 0) {
			workers.add(new ImageMod(img).execute(ImageMod.PARTY_INT));
		}
	}
	
	protected abstract void onSendCallback(Bundle args);
	
	private boolean writeFile(File outFile, boolean permaSave) {
        RelativeLayout newRel = new RelativeLayout(this.getActivity());

		RelativeLayout rel = (RelativeLayout)SnapEditorPicFrag.this.getView().findViewById(R.id.editor_layout);
        newRel.setLayoutParams(rel.getLayoutParams());

		Bitmap bitmap = Bitmap.createBitmap(rel.getWidth(), rel.getHeight(), Bitmap.Config.ARGB_8888);

        View[] views = new View[3];
        LayoutParams[] viewParams = new LayoutParams[3];
        for (int i = 0; i < 3; i++) {
            views[i] = rel.getChildAt(i);
            viewParams[i] = views[i].getLayoutParams();
        }
        rel.removeViews(0, 3);
        for (int i = 0; i < 3; i++) {
            newRel.addView(views[i], i, viewParams[i]);
        }

		Canvas canvas = new Canvas(bitmap);
		mCaption.setCursorVisible(false);
        newRel.invalidate();
        newRel.draw(canvas);
		mCaption.setCursorVisible(true);
        newRel.removeViews(0, 3);

        for (int i = 0; i < 3; i++) {
            rel.addView(views[i], i, viewParams[i]);
        }
		
		FileOutputStream fs;
		try {
			fs = new FileOutputStream(outFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		if (bitmap.getWidth() > bitmap.getHeight()) {
			Twig.debug("OpenSnap", "Rotating bitmap...");
			Matrix rotMatrix = new Matrix();
			rotMatrix.postRotate(90f);
			Bitmap tempBit = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotMatrix, true);
			if (tempBit != bitmap) {
				bitmap.recycle();
				bitmap = null;
				bitmap = tempBit;
			}
		}
		
		if (!bitmap.compress(CompressFormat.JPEG, 90, fs)) {
			StatMethods.hotBread(getActivity(), "Error compressing jpeg. Try again", Toast.LENGTH_SHORT);
			return false;
		}
		
		try {
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		if (permaSave) {
			MediaScannerConnection.scanFile(this.getActivity(), new String[] { outFile.getAbsolutePath() }, new String[] { "image/jpeg" }, null);
			StatMethods.hotBread(getActivity(), "Saved to " + outFile.getAbsolutePath(), Toast.LENGTH_SHORT);
		}
		
		return true;
	}
	
	private OnClickListener sendOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			/* File outFile = new File(SnapEditorPicFrag.this.getActivity().getCacheDir(), "resend.jpg");
			FileIO.bufferedCopy(getFilePath(), outFile.getAbsolutePath());
			setResendInfo(outFile.getAbsolutePath(), displayTime); */
			//File outFile = new File(SnapEditorPicFrag.this.getActivity().getCacheDir(), EDIT_FILENAME);
			
			File outFile = new File(SnapEditorPicFrag.this.getActivity().getCacheDir(), EDIT_FILENAME);
			Bundle args = new Bundle();
			args.putString(BaseContactSelectFrag.FILE_PATH_KEY, outFile.getAbsolutePath());
			if (!writeFile(outFile, false)) {
				return;
			}
			
			args.putInt(BaseContactSelectFrag.TIME_KEY, displayTime);
			
			onSendCallback(args);
		}
	};
	
	private OnTouchListener drawListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (drawMode > 0) {
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						prevX = event.getRawX();
						prevY = event.getRawY();

                        for (int i = 0; i < 5; i++) {
                            if (mButtonAnims[i] != null && !mButtonAnims[i].hasEnded()) {
                                mButtonAnims[i].cancel();
                            }

                            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.2f);
                            anim.setDuration(250);
                            anim.setFillAfter(true);
                            mButtonViews[i].clearAnimation();
                            mButtonViews[i].startAnimation(anim);
                            mButtonAnims[i] = anim;
                        }
						break;
					case MotionEvent.ACTION_MOVE:
						Paint paint = new Paint();
						if (drawMode == 1) {
							if (drawingColor != NON_COLOUR) {
								paint.setColor(drawingColor);
							} else {
								paint.setColor(0xFFFFFF00);
							}
							paint.setStrokeWidth(10.0f);
						} else {
							paint.setColor(Color.TRANSPARENT);
							paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
							paint.setStrokeWidth(15.0f);
						}
						drawCanvas.drawLine(prevX, prevY, event.getRawX(), event.getRawY(), paint);
						drawCanvas.drawCircle(event.getRawX(), event.getRawY(), 5.0f, paint);
						prevX = event.getRawX();
						prevY = event.getRawY();
						v.invalidate();
						break;
					case MotionEvent.ACTION_UP:
						prevX = -1.0f;
						prevY = -1.0f;

                        for (int i = 0; i < 5; i++) {
                            if (!mButtonAnims[i].hasEnded()) {
                                mButtonAnims[i].cancel();
                            }

                            AlphaAnimation anim = new AlphaAnimation(0.2f, 1.0f);
                            anim.setDuration(250);
                            anim.setFillAfter(true);
                            mButtonViews[i].clearAnimation();
                            mButtonViews[i].startAnimation(anim);
                            mButtonAnims[i] = anim;
                        }
						break;
				}
				return true;
			} else {
				return false;
			}
		}
	};
	
	private class ImageMod extends AsyncTask<Integer, Void, Bitmap> {
		private static final int FLIP_INT = 0x1;
		private static final int GREY_INT = 0x10;
		private static final int SEPIA_INT = 0x100;
		private static final int BLOCK_INT = 0x1000;
		private static final int RESET_INT = 0x0;
		private static final int PARTY_INT = 0x10000;
		private static final int PIXELATE_CONST = 50;
		
		private final ImageView imgView;
		private final int width;
		private final int height;
		private final int rotator;
		
		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		private ImageMod(ImageView img) {
			imgView = img;
			
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			if (Build.VERSION.SDK_INT < 13) {
				width = display.getWidth();
				height = display.getHeight();
			} else {
				Point point = new Point();
				display.getSize(point);
				width = point.x;
				height = point.y;
			}
			
			rotator = orientation;
		}
		
		@Override
		protected Bitmap doInBackground(Integer... params) {
			while (working.get()) {
				try {
					Thread.sleep(500L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (this.isCancelled()) {
					return null;
				}
			}
			
			working.set(true);
			
			Bitmap bitmap = BitmapUtil.createScaledRotatedBitmapFromFile(getFilePath(), width, height, rotator);
			if (!bitmap.isMutable() && params[0] != PARTY_INT) {
				Bitmap swap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
				if (swap == null)
					return null;
				bitmap.recycle();
				bitmap = swap;
			} else if (params[0] == PARTY_INT) {
				Bitmap swap = bitmap.copy(Bitmap.Config.RGB_565, true);
				if (swap == null)
					return null;
				bitmap.recycle();
				bitmap = swap;
			}
			modded = true;
			
			switch (params[0]) {
				case GREY_INT:
					for (int y = 0; y < bitmap.getHeight(); y++) {
						for (int x = 0; x < bitmap.getWidth(); x++) {
							int color1 = bitmap.getPixel(x, y);
							int ave = (((color1 >> 16) & 0xFF) + ((color1 >> 8) & 0xFF) + (color1 & 0xFF)) / 3;
							bitmap.setPixel(x, y, ((color1 >>> 24) << 24) | (ave << 16) | (ave << 8) | ave);
						}
					}
					modFlags = GREY_INT;
					break;
				case FLIP_INT:
					for (int y = 0; y < bitmap.getHeight(); y++) {
						for (int x = 0; x < bitmap.getWidth(); x++) {
							int c = bitmap.getPixel(x, y);
							int c2 = ((c >>> 24) << 24) | ((255 - ((c >> 16) & 0xFF)) << 16) | ((255 - ((c >> 8) & 0xFF)) << 8) | (255 - (c & 0xFF));
							bitmap.setPixel(x, y, c2);
						}
					}
					modFlags = FLIP_INT;
					break;
				case SEPIA_INT:
					for (int y = 0; y < bitmap.getHeight(); y++) {
						for (int x = 0; x < bitmap.getWidth(); x++) {
							int c = bitmap.getPixel(x, y);
							int r = (int)((0.393 * ((c >> 16) & 0xFF)) + (0.769 * ((c >> 8) & 0xFF)) + (0.189 * (c & 0xFF)));
							int g = (int)((0.349 * ((c >> 16) & 0xFF)) + (0.686 * ((c >> 8) & 0xFF)) + (0.168 * (c & 0xFF)));
							int b = (int)((0.272 * ((c >> 16) & 0xFF)) + (0.543 * ((c >> 8) & 0xFF)) + (0.131 * (c & 0xFF)));
							
							if (r > 255)
								r = 255;
							else if (r < 0)
								r = 0;
							if (g > 255)
								g = 255;
							else if (g < 0)
								g = 0;
							if (b > 255)
								b = 255;
							else if (b < 0)
								b = 0;
							bitmap.setPixel(x, y, ((c >>> 24 ) << 24) | (r << 16) | (g << 8) | b);
						}
					}
					modFlags = SEPIA_INT;
					break;
				case BLOCK_INT:
					int pixelateFactor = bitmap.getHeight() / PIXELATE_CONST;
					for (int y = 0; y < bitmap.getHeight(); y += pixelateFactor) {
						for (int x = 0; x < bitmap.getWidth(); x += pixelateFactor) {
							// 1. Get ave
							int aveR = 0, aveG = 0, aveB = 0;
							for (int y1 = y; (y1 - y < pixelateFactor) && y1 < bitmap.getHeight(); y1++) {
								for (int x1 = x; (x1 - x < pixelateFactor) && x1 < bitmap.getWidth(); x1++) {
									int px = bitmap.getPixel(x1, y1);
									aveR += (px >> 16) & 0xFF;
									aveG += (px >> 8) & 0xFF;
									aveB += px & 0xFF;
								}
							}
							
							aveR /= pixelateFactor * pixelateFactor;
							aveG /= pixelateFactor * pixelateFactor;
							aveB /= pixelateFactor * pixelateFactor;
							
							for (int y1 = y; (y1 - y < pixelateFactor) && y1 < bitmap.getHeight(); y1++) {
								for (int x1 = x; (x1 - x < pixelateFactor) && x1 < bitmap.getWidth(); x1++) {
									bitmap.setPixel(x1, y1, Color.argb(255, aveR, aveG, aveB));
								}
							}
						}
					}
					modFlags = BLOCK_INT;
					break;
				case RESET_INT:
					modded = false;
					modFlags = RESET_INT;
					break;
				case PARTY_INT:
					int width = bitmap.getWidth(), height = bitmap.getHeight();
					FaceDetector fd;
					FaceDetector.Face[] faces = new FaceDetector.Face[3];
					fd = new FaceDetector(width, height, 3);
					int count = fd.findFaces(bitmap, faces);
					if (count > 0) {
						for (int i = 0; i < count; i++) {
							// TODO: party eegg
						}
					}
					break;
			}
			
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null && imgView != null) {
				//ImageView img = (ImageView)SnapEditorPicFrag.this.getView().findViewById(R.id.snap_img);
				//if (img != null) {
				//	img.setImageBitmap(bitmap);
				//}
				imgView.setImageBitmap(bitmap);
			}
			working.set(false);
			if (workers != null) {
				workers.remove(this);
			}
		}
		
		@Override
		protected void onCancelled() {
			if (workers != null) {
				workers.remove(this);
			}
		}
	}
	
	private class ImageLoad extends AsyncTask<String, Void, Bitmap> {
		private final ImageView imgView;
		private final int width;
		private final int height;
		private final int rotator;
		
		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		private ImageLoad(ImageView img) { 
			imgView = img;
			
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			if (Build.VERSION.SDK_INT < 13) {
				width = display.getWidth();
				height = display.getHeight();
			} else {
				Point point = new Point();
				display.getSize(point);
				width = point.x;
				height = point.y;
			}
			
			rotator = orientation;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			//TODO: Change from the size of the device to a more scaled size to stop possible memory issues. And implement for mod as well.
			try {
				return BitmapUtil.createScaledRotatedBitmapFromFile(params[0], width, height, rotator);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap == null) {
				StatMethods.hotBread(getActivity(), "Error loading pic", Toast.LENGTH_SHORT);
			} else if (imgView != null) {
				imgView.setImageBitmap(bitmap);
			}
			if (workers != null) {
				workers.remove(this);
			}
		}
		
		@Override
		protected void onCancelled() {
			if (workers != null) {
				workers.remove(this);
			}
		}
	}
}
