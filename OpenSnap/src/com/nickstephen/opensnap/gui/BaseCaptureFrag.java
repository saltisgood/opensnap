package com.nickstephen.opensnap.gui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.FrameLayout;
import org.holoeverywhere.widget.Toast;
import org.holoeverywhere.widget.ToggleButton;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

import com.nickstephen.lib.Twig;
import com.nickstephen.lib.anim.IOnAnimationCompletion;
import com.nickstephen.lib.anim.OrientationListener;
import com.nickstephen.lib.gui.Fragment;
import com.nickstephen.lib.gui.widget.PieProgressView;
import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.composer.CaptureActivity;
import com.nickstephen.opensnap.composer.VidQualitySelectFrag;
import com.nickstephen.opensnap.util.misc.CameraUtil;
import com.nickstephen.opensnap.util.widget.Preview;

public abstract class BaseCaptureFrag extends Fragment {
	public static final String FRAG_TAG = "CAPTURE_FRAG";
	public static final String CAPTURE_KEY = "CAPTURE";
	
	protected static final int UNFOCUSED = 0;
	protected static final int FOCUS_FINISHED = 1;
	protected static final int FOCUSING = 2;
	protected static final String CAM_FILENAME = "cam.jpg";
	protected static final String VID_FILENAME = "cam.mp4";

    // Camera fields. Should be reset each time a camera is loaded or disposed
	protected Camera mCamera;
	protected Camera.Parameters mCameraParams;
    protected int mCameraMaxZoom = -1;
    protected boolean mCameraAutoFocusEnabled = false;
    protected int mCameraDirection = CameraInfo.CAMERA_FACING_BACK;
    protected int mCameraOrientation = 0;
    protected int mVidQuality = 0;

    // Status fields
	protected int focusState = UNFOCUSED;
	protected boolean picTaken = false;
    protected boolean mIsRecording = false;
    protected boolean camLoaded = false;
    protected int mCurrentCamera = 0;

	protected MediaRecorder mRecorder;

	protected Button mCaptureButton;
    protected OrientationListener mOrientationListener;
    protected PieProgressView mPieView;
    protected Preview mPreviewWidget;
	protected ToggleButton mVideoToggleButton;

    protected String filePath;

    private FrameLayout mCameraContainer;

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
		setFocused(true);
		
		View v = inflater.inflate(R.layout.camera_frag, null);
		
		mCameraContainer = (FrameLayout) v.findViewById(R.id.camera_container);
		
		mPieView = (PieProgressView)v.findViewById(R.id.video_pie_pb);
		
		Button button = (Button)v.findViewById(R.id.camera_button);
        if (Build.VERSION.SDK_INT < 9 || Camera.getNumberOfCameras() <= 1) {
            button.setVisibility(View.GONE);
        } else {
            button.setOnClickListener(mSwitchCamClick);
        }
		
		mCaptureButton = (Button)v.findViewById(R.id.capture_button);
		mCaptureButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				picTaken = true;
				onKeyDown(KeyEvent.KEYCODE_CAMERA);
			}
		});
		mCaptureButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				onKeyDown(KeyEvent.KEYCODE_FOCUS);
				return true;
			}
		});
		mCaptureButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_UP && !picTaken) {
					onKeyUp(KeyEvent.KEYCODE_FOCUS);
				}
				return false;
			}
		});
		
		mVideoToggleButton = (ToggleButton)v.findViewById(R.id.movie_switch_button);
		
		mOrientationListener = new OrientationListener(this.getActivity(), SensorManager.SENSOR_DELAY_UI);
		mOrientationListener.addViewToAnimate(button);
		mOrientationListener.addViewToAnimate(mVideoToggleButton);
		
		return v;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.video_cap, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.video_quality:
                VidQualitySelectFrag frag = new VidQualitySelectFrag();
                Bundle args = new Bundle();
                if (((CaptureActivity)this.getActivity()).hasVidQualityChanged()) {
                    mVidQuality = ((CaptureActivity) this.getActivity()).getVidQualityResult();
                }
                args.putInt(VidQualitySelectFrag.KEY_CURRENT_QUAL, mVidQuality);
                args.putInt(VidQualitySelectFrag.KEY_CURRENT_CAM, mCurrentCamera);
                frag.setArguments(args);

                this.getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.push_down_in, 0)
                        .add(R.id.subfrag_container, frag)
                        .addToBackStack(null).commit();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NewApi")
	public boolean onKeyDown(int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			// zoom in
			if (mCameraMaxZoom != -1 && mCameraParams.getZoom() != mCameraMaxZoom) {
				mCamera.startSmoothZoom(mCameraParams.getZoom() + 1);
			}
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			// zoom out
			if (mCameraMaxZoom != -1 && mCameraParams.getZoom() != 0) {
				mCamera.startSmoothZoom(mCameraParams.getZoom() - 1);
			}
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
			if (mCameraAutoFocusEnabled && focusState == UNFOCUSED && !mIsRecording) {
				focusState = FOCUSING;
				mCamera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						if (success) {
							focusState = FOCUS_FINISHED;
						} else {
							focusState = UNFOCUSED;
						}
					}
				});
			}
		}
		else if (keyCode == KeyEvent.KEYCODE_CAMERA && camLoaded) {
			if (!isVideo()) {
				if (mCameraAutoFocusEnabled && focusState == FOCUSING) {
					mCamera.cancelAutoFocus();
					focusState = UNFOCUSED;
				}
				
				if (!takePicture()) {
					StatMethods.hotBread(this.getActivity(), "Camera not loaded! Try again later", Toast.LENGTH_SHORT);
				}
			} else {
				if (mIsRecording) {
					mPieView.stopAnimation(false);
				} else {
					if (prepareVideoRecorder()) {
						mRecorder.start();
						mIsRecording = true;
						
						mPieView.setVisibility(View.VISIBLE);
						mPieView.setAnimationOnCompletionListener(new IOnAnimationCompletion() {
							@Override
							public void onAnimationCompletion(View animatedView) {
								animatedView.setVisibility(View.INVISIBLE);
								mRecorder.stop();
								releaseMediaRecorder();
								mIsRecording = false;
								
								postVideoCallback(filePath);
								
								mVideoToggleButton.setEnabled(true);
							}
						});
                        mPieView.startAnimation();
						
						mVideoToggleButton.setEnabled(false);
					} else {
						releaseMediaRecorder();
					}
					
					
				}
			}
			return true;
		}
		return false;
	}
	
	public boolean onKeyUp(int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
			if (focusState == UNFOCUSED) {
				mCamera.cancelAutoFocus();
			}
			focusState = UNFOCUSED;
		}
		return false;
	}
	
	protected boolean takePicture() {
		if (mCamera == null) {
			return false;
		}
		
		mCamera.setOneShotPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				if (camera.getParameters().getPreviewFormat() != ImageFormat.NV21) {
					StatMethods.hotBread(BaseCaptureFrag.this.getActivity(), "Invalid Camera Preview Format!", Toast.LENGTH_SHORT);
					return;
				}
				
				Size previewSize = camera.getParameters().getPreviewSize();
				Rect rect = new Rect(0, 0, previewSize.width, previewSize.height);
				YuvImage img = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);

                File pictureFile = new File(BaseCaptureFrag.this.getActivity().getCacheDir(), CAM_FILENAME);

                try {
                    ByteArrayOutputStream oStream = new ByteArrayOutputStream();
                    img.compressToJpeg(rect, 90, oStream);

                    Bitmap writeMap;
                    byte[] bytes = oStream.toByteArray();
                    if (mCameraDirection == CameraInfo.CAMERA_FACING_FRONT) {
                        Matrix matrix = new Matrix();
                        matrix.postScale(-1.0f, 1.0f);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap == null) {
                            // Handle error
                        }
                        writeMap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                    } else {
                        writeMap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }

                    oStream.close();

                    FileOutputStream fs = new FileOutputStream(pictureFile);
                    writeMap.compress(Bitmap.CompressFormat.JPEG, 90, fs);
                    fs.close();
                } catch (FileNotFoundException e) {
                    StatMethods.hotBread(BaseCaptureFrag.this.getActivity(), "Error saving to file (Err 1)", Toast.LENGTH_SHORT);
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    StatMethods.hotBread(BaseCaptureFrag.this.getActivity(), "Error saving to file (Err 2)", Toast.LENGTH_SHORT);
                    e.printStackTrace();
                    return;
                }
				
				postPictureCallback(pictureFile.getAbsolutePath());
			}
		});
		
		return true;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mOrientationListener.disable();
		
		if (isVideo() && mIsRecording) {
			mRecorder.stop();
			releaseMediaRecorder();
			mIsRecording = false;
		}
		
		releaseCameraAndSurface();
		camLoaded = false;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mOrientationListener.enable();
		picTaken = false;
		new LazyCameraLoader(-1).execute(mCurrentCamera);
	}
	
	private void releaseCameraAndSurface() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;

            mCameraMaxZoom = -1;
            mCameraOrientation = 0;
            mCameraDirection = CameraInfo.CAMERA_FACING_BACK;
            mCameraAutoFocusEnabled = false;
            mVidQuality = 0;
            ((CaptureActivity) this.getActivity()).setVidQualityResult(0);
		}
		if (mPreviewWidget != null && mCameraContainer != null) {
            mCameraContainer.removeView(mPreviewWidget);
			/* FrameLayout camContainer = (FrameLayout) BaseCaptureFrag.this.getView().findViewById(R.id.camera_container);
			camContainer.removeView(mPreviewWidget); */
			mPreviewWidget = null;
		}
	}
	
	private boolean isVideo() {
		if (mVideoToggleButton == null) {
			mVideoToggleButton = (ToggleButton)this.getView().findViewById(R.id.movie_switch_button);
		}
		
		return mVideoToggleButton.isChecked();
	}

	@SuppressLint("NewApi")
    private boolean prepareVideoRecorder() {
		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.stopPreview();
		mCamera.unlock();
		mRecorder = new MediaRecorder();
		mRecorder.setCamera(mCamera);
		
		// Step 2: Set sources
		mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		
		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        if (Build.VERSION.SDK_INT >= 9) {
            /* CamcorderProfile profile = CamcorderProfile.get(mCurrentCamera, CamcorderProfile.QUALITY_HIGH);
            CamcorderProfile profileLow = CamcorderProfile.get(mCurrentCamera, CamcorderProfile.QUALITY_LOW);

            if (Build.VERSION.SDK_INT >= 11) {
                if (CamcorderProfile.hasProfile(mCurrentCamera, CamcorderProfile.QUALITY_720P)) {
                    profile = CamcorderProfile.get(mCurrentCamera, CamcorderProfile.QUALITY_720P);
                }
            } */

            //mRecorder.setProfile(profile);
            if (((CaptureActivity) this.getActivity()).hasVidQualityChanged()) {
                mVidQuality = ((CaptureActivity)this.getActivity()).getVidQualityResult();
            }
            mRecorder.setProfile(CamcorderProfile.get(mCurrentCamera, mVidQuality));
        } else {
            mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        }

        mRecorder.setMaxFileSize(1400000);
        mRecorder.setOnInfoListener(mRecorderListener);

		if (Build.VERSION.SDK_INT > 9) {
            if (mCameraDirection == CameraInfo.CAMERA_FACING_BACK) {
			    mRecorder.setOrientationHint(mCameraOrientation);
            } else {
                mRecorder.setOrientationHint((mCameraOrientation + 180) % 360);
            }
		}
		
		// Step 4: Set output file
		//filePath = CameraUtil.getOutputMediaFile(getActivity(), CameraUtil.MEDIA_TYPE_VIDEO).getAbsolutePath();
		//filePath = new File(BaseCaptureFrag.this.getActivity().getCacheDir(), VID_FILENAME).getAbsolutePath();
        File file = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, VID_FILENAME);
        if (file.exists()) {
            file.delete();
        }
		//filePath = new File(Environment.getExternalStorageDirectory() + CameraUtil.ROOT_PATH, VID_FILENAME).getAbsolutePath();
        filePath = file.getAbsolutePath();

		mRecorder.setOutputFile(filePath);
		
		// Step 5: Set the preview output
		mRecorder.setPreviewDisplay(mPreviewWidget.getSurface());
		
		// Step 6: Prepare configured MediaRecorder
		try {
			mRecorder.prepare();
		} catch (IllegalStateException e) {
			Twig.debug("OpenSnap", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Twig.debug("OpenSnap", "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}
	
	private void releaseMediaRecorder() {
		mRecorder.release();
		mCamera.lock();
	}

    private final OnClickListener mSwitchCamClick = new OnClickListener() {
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void onClick(View view) {
            Camera.CameraInfo info = new CameraInfo();
            int n = Camera.getNumberOfCameras();
            for (int i = 0; i < n; i++) {
                Camera.getCameraInfo(i, info);
                if (mCameraDirection == CameraInfo.CAMERA_FACING_BACK
                        && info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    n = i;
                    break;
                } else if (mCameraDirection == CameraInfo.CAMERA_FACING_FRONT
                        && info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    n = i;
                    break;
                }

                info = new CameraInfo();
            }

            if (n == Camera.getNumberOfCameras()) {
                return;
            }

            releaseCameraAndSurface();
            new LazyCameraLoader(mCurrentCamera).execute(n);
        }
    };

    private final MediaRecorder.OnInfoListener mRecorderListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                mPieView.stopAnimation(false);
            }
        }
    };

	protected abstract void postPictureCallback(String filePath); 
	
	protected abstract void postVideoCallback(String filePath);
	
	protected class LazyCameraLoader extends AsyncTask<Integer, Void, Camera> {
        private final int mOnFailureRestart;

        public LazyCameraLoader(int onFail) {
            mOnFailureRestart = onFail;
        }

		@SuppressLint("NewApi")
		@Override
		protected Camera doInBackground(Integer... parameters) {
			if (Build.VERSION.SDK_INT < 9) {
				mCamera = CameraUtil.getDefaultCameraInstance();
                if (mCamera == null) {
                    return null;
                }

                mCameraDirection = CameraInfo.CAMERA_FACING_BACK;
			} else {
				mCamera = CameraUtil.getCameraInstance(parameters[0]);
				if (mCamera == null) {
					return null;
				}
				Camera.CameraInfo info = new Camera.CameraInfo();
				Camera.getCameraInfo(parameters[0], info);

                if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    mCameraOrientation = (360 - info.orientation) % 360; // compensate the mirror
                } else {
                    mCameraOrientation = (info.orientation + 360) % 360;
                }
                mCamera.setDisplayOrientation(mCameraOrientation);

                mCameraDirection = info.facing;
                mCurrentCamera = parameters[0];
			}
			
			mCameraParams = mCamera.getParameters();
			if (mCameraParams.isZoomSupported()) {
				mCameraMaxZoom = mCameraParams.getMaxZoom();
			}
			List<String> modes = mCameraParams.getSupportedFocusModes();
            if (modes != null) {
                for (String mode : modes) {
                    if (mode.compareTo(Camera.Parameters.FOCUS_MODE_AUTO) == 0) {
                        mCameraAutoFocusEnabled = true;
                        break;
                    }
                }
            }
			
			return mCamera;
		}
		
		@Override
		protected void onCancelled() {
			//StatMethods.hotBread(getActivity(), "Cancelled", Toast.LENGTH_SHORT);
		}
		
		@Override
		protected void onPostExecute(Camera camera) {
			if (camera != null && mCameraContainer != null) {
                mPreviewWidget = new Preview(BaseCaptureFrag.this.getActivity(), camera);
                mPreviewWidget.setKeepScreenOn(true);
                mCameraContainer.addView(mPreviewWidget);
                FrameLayout.LayoutParams previewParams = (android.widget.FrameLayout.LayoutParams) mPreviewWidget.getLayoutParams();
                previewParams.gravity = Gravity.CENTER;
                mPreviewWidget.setLayoutParams(previewParams);
                camLoaded = true;
			} else {
				StatMethods.hotBread(getActivity(), "Error getting camera", Toast.LENGTH_LONG);

                if (mOnFailureRestart != -1) {
                    new LazyCameraLoader(-1).execute(mOnFailureRestart);
                }
			}
			
		}
	}
}
