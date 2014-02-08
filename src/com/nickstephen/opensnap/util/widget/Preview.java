package com.nickstephen.opensnap.util.widget;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.nickstephen.lib.Twig;

public class Preview extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
    private Camera.Parameters mCameraParams;

	private int mPreviewWidth;
	private int mPreviewHeight;
	
	@SuppressWarnings("deprecation")
	public Preview(Context context, Camera pCamera) {
		super(context);
		
		mCamera = pCamera;
        mCameraParams = mCamera.getParameters();
		
		// Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed 
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		// deprecated setting but required on android prior to 3.0
		if (Build.VERSION.SDK_INT < 11) {
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
	}
	
	public Preview(Context context) {
		// DON'T CALL
		super(context);
        throw new RuntimeException("Not allowed to create without a camera! Use Preview(Context, Camera) instead.");
	}
	
	public Preview(Context context, AttributeSet attrs) {
		// DON'T CALL
		super(context, attrs);
        throw new RuntimeException("Not allowed to create without a camera! Use Preview(Context, Camera) instead.");
	}
	
	public Preview(Context context, AttributeSet attrs, int arg2) {
		// DON'T CALL
		super(context, attrs, arg2);
        throw new RuntimeException("Not allowed to create without a camera! Use Preview(Context, Camera) instead.");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
        //Twig.debug("Preview", "SurfaceChanged called");
		// If the preview can change or rotate, take care of that here. Make sure to stop the preview
		// before resizing or reformatting it. Probably not that necessary for my purposes.
		if (this.mHolder.getSurface() == null)
			// Preview surface doesn't exist
			return;
		
		// stop preview before making changes. Probably not necessary but done for safety
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore. tried to stop a non-existent preview
		}
		
		// set preview size and make any resize, rotate or reformatting changes here
        mCameraParams.setPreviewSize(mPreviewHeight, mPreviewWidth);
        mCamera.setParameters(mCameraParams);
		
		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(this.mHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			Twig.debug("OpenSnap Preview", "Error starting mCamera preview: " + e.getMessage());
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        // empty. The preview stuff is handled in surface changed, which is called directly after this one
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the mCamera preview in the activity
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	}
	
	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        //Twig.debug("Preview", "OnMeasure called");
        if (mCamera == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            int measureHeight = View.MeasureSpec.getSize(heightMeasureSpec), measureWidth = View.MeasureSpec.getSize(widthMeasureSpec);

            // Fix for only one layout pass. Gets the preview sizes that are going to be used for
            // the camera and sets the correct dimensions for use later.
            if (mPreviewHeight == 0 || mPreviewWidth == 0) {
                List<Size> sizes = mCameraParams.getSupportedPreviewSizes();
                Size previewSize = getOptimalPreviewSize(sizes, measureWidth, measureHeight);
                mPreviewHeight = previewSize.width;
                mPreviewWidth = previewSize.height;
                /* mCameraParams.setPreviewSize(previewSize.width, previewSize.height);
                mPreviewHeight = previewSize.width;
                mPreviewWidth = previewSize.height; */

                //mCamera.setParameters(mCameraParams);
            }

            if (mPreviewHeight > measureHeight) {
                float ratio = (float)measureHeight / mPreviewHeight;
                mPreviewHeight = measureHeight;
                mPreviewWidth = (int)(ratio * mPreviewWidth);
            } else if (mPreviewWidth > measureWidth) {
                float ratio = (float)measureWidth / mPreviewWidth;
                mPreviewWidth = measureWidth;
                mPreviewHeight = (int)(ratio * mPreviewHeight);
            }
            this.setMeasuredDimension(mPreviewWidth, mPreviewHeight);
        }
	}
	
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.01;
        //double targetRatio = (double) w / h;
        double targetRatio = (double) h / w;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        //int targetHeight = h;
        int targetHeight = w;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	
	private Size getOptimalSaveSize(List<Size> sizes) {
		final double ASPECT_TOLERANCE = 0.05;
		final int HEIGHT = 720;
		final int WIDTH = 1280;
		
		double targetRatio = (double) WIDTH / HEIGHT;
		if (sizes == null) return null;
		
		Size optimalSize = null;
		int minDiff = Integer.MAX_VALUE;
		
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - HEIGHT) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - HEIGHT);
			}
		}
		
		if (optimalSize == null) {
			minDiff = Integer.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - HEIGHT) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - HEIGHT);
				}
			}
		}
		
		return optimalSize;
	}

	public Surface getSurface() {
		if (mHolder != null)
			return this.mHolder.getSurface();
		return null;
	}
	
	public int getPreviewWidth() {
		return mPreviewWidth;
	}
	
	public int getPreviewHeight() {
		return mPreviewHeight;
	}
}
