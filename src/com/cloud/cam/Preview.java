package com.cloud.cam;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class Preview extends SurfaceView implements SurfaceHolder.Callback,SensorEventListener {
	private static String TAG = "Preview";
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private MediaRecorder mMediaRecorder;
	private boolean isPreview;
	private boolean isRecording = false;
	private Paint paint = new Paint();

	private float a_x;
	private float a_y;
	private float a_z;
		
	public Preview(Context context, Bundle savedInstanceState) {
		super(context);
		mCamera = Camera.open();

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void videoRecorder() {
		if (!isRecording) {
			if (isPreview) {
				if (mCamera != null) {
					mCamera.stopPreview();
					mCamera.release();
					mCamera = null;
				}
			}
			
			Activity activity = (Activity)this.getContext();
			ImageButton view = (ImageButton)activity.findViewById(R.id.take_photo);
			view.setImageResource(isRecording ? R.drawable.off : R.drawable.on);

			if (mMediaRecorder == null)
				mMediaRecorder = new MediaRecorder();
			else
				mMediaRecorder.reset();

			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mMediaRecorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_HIGH));
			mMediaRecorder.setOutputFile("/sdcard/DCIM/test.mp4");
			mMediaRecorder.setPreviewDisplay(mHolder.getSurface());

			try {
				mMediaRecorder.prepare();
				mMediaRecorder.start();

			} catch (IllegalStateException e) {
				Log.d(TAG, "IllegalStateException preparing MediaRecorder: "
						+ e.getMessage());
				releaseMediaRecorder();

			} catch (IOException e) {
				Log.d(TAG,
						"IOException preparing MediaRecorder: "
								+ e.getMessage());
				releaseMediaRecorder();
			}
			isRecording = true;
		} else {
			Activity activity = (Activity)this.getContext();
			ImageButton view = (ImageButton)activity.findViewById(R.id.take_photo);
			view.setImageResource(isRecording ? R.drawable.off : R.drawable.on);
			
			mMediaRecorder.stop();
			releaseMediaRecorder();
			isRecording = false;

			try {
				mCamera = Camera.open();
				mCamera.setPreviewDisplay(mHolder);
//				mCamera.setPreviewCallback(new StreamIt(isRecording));
				mCamera.startPreview();
				isPreview = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
		canvas.save();

		paint.setColor(Color.WHITE);
		final float scale = getResources().getDisplayMetrics().density;
		paint.setTextSize(14 * scale + 0.5f); // convert dps to pixels
		paint.setTextAlign(Paint.Align.CENTER);
		
		canvas.drawText("x:" + a_x + "y:" + a_y + "z:" +a_z, canvas.getWidth() / 2,
				canvas.getHeight() / 3, paint);

		canvas.restore();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			Log.e(TAG, "=====1======");
			if(holder == null){
				Log.e(TAG, "===null holder===");
			}
			
			mCamera.setPreviewDisplay(mHolder);
			Log.e(TAG, "=====2======");
			mCamera.startPreview();
			Log.e(TAG, "=====3======");
			//mCamera.setPreviewCallback(new StreamIt(isRecording));
			isPreview = true;
			this.setWillNotDraw(false);
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
		Log.e(TAG, "==========surface destroy");
		Log.e(TAG, "==========surface destroy");
		Log.e(TAG, "==========surface destroy");
		if (mCamera != null) {
			if (isPreview) {
				mCamera.stopPreview();
				isPreview = false;
			}
			mCamera.release();
			mCamera = null; // 记得释放
		}
		
		mHolder = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			 mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}
		setPreviewSize();
		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			 mCamera.setPreviewDisplay(mHolder);
			 mCamera.startPreview();
			 isPreview = true;

		} catch (Exception e) {
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		a_x = event.values[0];
		a_y = event.values[1];
		a_z = event.values[2];
		
		this.invalidate();
		//this.showToast(null, "x:" + x + "y:" + y + "z:" + z);
	}

	public Toast showToast(Toast clear_toast, String message) {
		class RotatedTextView extends View {
			private String text = "";
			private Paint paint = new Paint();
			private Rect bounds = new Rect();

			public RotatedTextView(String text, Context context) {
				super(context);

				this.text = text;
			}

			@Override
			protected void onDraw(Canvas canvas) {
				final float scale = getResources().getDisplayMetrics().density;
				paint.setTextSize(14 * scale + 0.5f); // convert dps to pixels
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(Color.rgb(75, 75, 75));
				paint.setShadowLayer(1, 0, 1, Color.BLACK);
				paint.getTextBounds(text, 0, text.length(), bounds);
				/*
				 * if( MyDebug.LOG ) { Log.d(TAG, "bounds: " + bounds); }
				 */
				final int padding = (int) (14 * scale + 0.5f); // convert dps to
																// pixels
				final int offset_y = (int) (32 * scale + 0.5f); // convert dps
																// to pixels
				canvas.save();
				// canvas.rotate(ui_rotation, canvas.getWidth()/2,
				// canvas.getHeight()/2);
				canvas.drawRect(canvas.getWidth() / 2 - bounds.width() / 2
						+ bounds.left - padding, canvas.getHeight() / 2
						+ bounds.top - padding + offset_y, canvas.getWidth()
						/ 2 - bounds.width() / 2 + bounds.right + padding,
						canvas.getHeight() / 2 + bounds.bottom + padding
								+ offset_y, paint);
				paint.setColor(Color.WHITE);
				canvas.drawText(text, canvas.getWidth() / 2 - bounds.width()
						/ 2, canvas.getHeight() / 2 + offset_y, paint);
				canvas.restore();
			}
		}

		if (MyDebug.LOG)
			Log.d(TAG, "showToast");
		if (clear_toast != null)
			clear_toast.cancel();
		Activity activity = (Activity) this.getContext();
		// clear_toast = Toast.makeText(activity.getApplicationContext(),
		// message, Toast.LENGTH_SHORT);
		// clear_toast.show();

		clear_toast = new Toast(activity);
		View text = new RotatedTextView(message, activity);
		clear_toast.setView(text);
		clear_toast.setDuration(Toast.LENGTH_SHORT);
		clear_toast.show();

		return clear_toast;
	}

	public void setFocus(String focus_value) {
		if (MyDebug.LOG)
			Log.d(TAG, "setFocus() " + focus_value);
		Camera.Parameters parameters = mCamera.getParameters();
		if (focus_value.equals("focus_mode_auto")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		} else if (focus_value.equals("focus_mode_infinity")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		} else if (focus_value.equals("focus_mode_macro")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
		} else if (focus_value.equals("focus_mode_fixed")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
		} else if (focus_value.equals("focus_mode_edof")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
		} else if (focus_value.equals("focus_mode_continuous_video")) {
			parameters
					.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		} else {
			if (MyDebug.LOG)
				Log.d(TAG, "setFocus() received unknown focus value "
						+ focus_value);
		}
		mCamera.setParameters(parameters);
		tryAutoFocus();
	}

	private void tryAutoFocus() {
		if (MyDebug.LOG)
			Log.d(TAG, "tryAutoFocus");
		if (mCamera == null) {
			if (MyDebug.LOG)
				Log.d(TAG, "no camera");
		} else if (isRecording) {
			if (MyDebug.LOG)
				Log.d(TAG, "currently taking a photo");
		} else {
			Camera.Parameters parameters = mCamera.getParameters();
			String focus_mode = parameters.getFocusMode();
			if (MyDebug.LOG)
				Log.d(TAG, "focus_mode is " + focus_mode);
			if (focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
					|| focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
				if (MyDebug.LOG)
					Log.d(TAG, "try to start autofocus");
				Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						if (MyDebug.LOG)
							Log.d(TAG, "autofocus complete: " + success);
					}
				};
				mCamera.autoFocus(autoFocusCallback);
			}
		}
	}
	
	private void setPreviewSize() {
		if( MyDebug.LOG )
			Log.d(TAG, "setPreviewSize()");
		if( mCamera == null ) {
			return;
		}
		// set optimal preview size
    	Camera.Parameters parameters = mCamera.getParameters();
		if( MyDebug.LOG )
			Log.d(TAG, "current preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
    	Camera.Size current_size = parameters.getPictureSize();
		if( MyDebug.LOG )
			Log.d(TAG, "current size: " + current_size.width + ", " + current_size.height);
        List<Camera.Size> preview_sizes = parameters.getSupportedPreviewSizes();
        if( preview_sizes.size() > 0 ) {
	        Camera.Size best_size = preview_sizes.get(0);
	        for(Camera.Size size : preview_sizes) {
	    		if( MyDebug.LOG )
	    			Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
	        	if( size.width*size.height > best_size.width*best_size.height ) {
	        		best_size = size;
	        	}
	        }
            parameters.setPreviewSize(best_size.width, best_size.height);
    		if( MyDebug.LOG )
    			Log.d(TAG, "new preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);

    		/*List<int []> fps_ranges = parameters.getSupportedPreviewFpsRange();
    		if( MyDebug.LOG ) {
		        for(int [] fps_range : fps_ranges) {
	    			Log.d(TAG, "    supported fps range: " + fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " to " + fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
		        }
    		}
    		int [] fps_range = fps_ranges.get(fps_ranges.size()-1);
	        parameters.setPreviewFpsRange(fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);*/
    		mCamera.setParameters(parameters);
        }
	}
	
    public void onResume() {
		if( MyDebug.LOG )
			Log.d(TAG, "onResume");

		try {
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(mHolder);
//			mCamera.setPreviewCallback(new StreamIt(isRecording));
//			mCamera.startPreview();
			isPreview = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void onPause() {
		if( MyDebug.LOG )
			Log.d(TAG, "onPause");

		if (mCamera != null) {
			if (isPreview) {
				mCamera.stopPreview();
				isPreview = false;
			}
			mCamera.release();
			mCamera = null; // 记得释放
		}
    }
}

class StreamIt implements Camera.PreviewCallback {
	private String TAG = "PreviewCallback";
	private boolean isRecording;
	public StreamIt(boolean isRecording){
		this.isRecording = isRecording;
	}
	
	
	public void onPreviewFrame(byte[] data, Camera camera) {

		// 刚刚拍照的文件名
		if (true) {

			String fileName = "IMG_"
					+ new SimpleDateFormat("yyyyMMdd_HHmmss")
							.format(new Date()).toString() + ".jpg";
			File sdRoot = Environment.getExternalStorageDirectory();
			String dir = "/DCIM/";
			File mkDir = new File(sdRoot, dir);
			if (!mkDir.exists())
				mkDir.mkdirs();
			File pictureFile = new File(sdRoot, dir + fileName);
			if (!pictureFile.exists()) {
				try {
					pictureFile.createNewFile();
					Camera.Parameters parameters = camera.getParameters();
					Size size = parameters.getPreviewSize();
					YuvImage image = new YuvImage(data,
							parameters.getPreviewFormat(), size.width,
							size.height, null);
					FileOutputStream filecon = new FileOutputStream(pictureFile);
					image.compressToJpeg(
							new Rect(0, 0, image.getWidth(), image.getHeight()),
							90, filecon);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
