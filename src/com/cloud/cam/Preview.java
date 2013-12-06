package com.cloud.cam;

import java.io.BufferedWriter;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class Preview extends SurfaceView implements SurfaceHolder.Callback,SensorEventListener{
	private static String TAG = "Preview";
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private MediaRecorder mMediaRecorder;
	private boolean isPreview;
	public boolean isRecording = false;
	public boolean isSavingPreview = false;
	private Paint paint = new Paint();
	
	private File sensorFile = null;

	public float accelerometer_x;
	public float accelerometer_y;
	public float accelerometer_z;
	private static int acc_n=1;
	
	public float gravity_x;
	public float gravity_y;
	public float gravity_z;
	
	public float gyroscope_x;
	public float gyroscope_y;
	public float gyroscope_z;
		
	public float magnetic_x;
	public float magnetic_y;
	public float magnetic_z;
	
	public float linear_acceleration_x;
	public float linear_acceleration_y;
	public float linear_acceleration_z;
	
	public float orientation_x;
	public float orientation_y;
	public float orientation_z;
	
	public float ungyroscope_x;
	public float ungyroscope_y;
	public float ungyroscope_z;
	
	public float unmagnetic_x;
	public float unmagnetic_y;
	public float unmagnetic_z;
	
	public float rotation_x;
	public float rotation_y;
	public float rotation_z;
	
	public float gamerotation_x;
	public float gamerotation_y;
	public float gamerotation_z;
	
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
	
	public void savePreviewAndSensors(){
		if(isRecording){
			return;
		}
		Activity activity = (Activity)this.getContext();
		ImageButton view = (ImageButton)activity.findViewById(R.id.save_preview);
		
		if(!isSavingPreview){
			
			isSavingPreview = true;
			view.setImageResource(isSavingPreview ? R.drawable.saving : R.drawable.gallery);		
		}else{
			isSavingPreview = false;
			view.setImageResource(isSavingPreview ? R.drawable.saving : R.drawable.gallery);	
			
			//write fifo to tell sendthread to send media
			MainActivity main_activity = (MainActivity)Preview.this.getContext();
			try {
				main_activity.pos.write(1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void videoRecorder() {
		if(isSavingPreview){
			return;
		}
		
		if (!isRecording) {
			if (isPreview) {
				if (mCamera != null) {
					mCamera.setPreviewCallback(null);
					mCamera.stopPreview();
					mCamera.release();
					mCamera = null;
				}
			}
			
			Activity activity = (Activity)this.getContext();
			ImageButton view = (ImageButton)activity.findViewById(R.id.record_video);
			view.setImageResource(isRecording ? R.drawable.off : R.drawable.on);

			if (mMediaRecorder == null)
				mMediaRecorder = new MediaRecorder();
			else
				mMediaRecorder.reset();
			
			MainActivity main_activity = (MainActivity)Preview.this.getContext();
			File videoFile = main_activity.getOutputMediaFile(main_activity.MEDIA_TYPE_VIDEO);
			String videoName = videoFile.getAbsolutePath();
			
			//push file to queue for sendthread
			try{
				main_activity.queue.put(videoFile);
			}catch (Exception e) {
	            e.printStackTrace();
	        }

			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mMediaRecorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_HIGH));
			mMediaRecorder.setOutputFile(videoName);
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
			ImageButton view = (ImageButton)activity.findViewById(R.id.record_video);
			view.setImageResource(isRecording ? R.drawable.off : R.drawable.on);
			
			mMediaRecorder.stop();
			releaseMediaRecorder();
			isRecording = false;

			try {
				mCamera = Camera.open();
				mCamera.setPreviewDisplay(mHolder);
				MainActivity main_activity = (MainActivity)Preview.this.getContext();
				mCamera.setPreviewCallback(new StreamIt(main_activity, Preview.this));
				mCamera.startPreview();
				isPreview = true;
				
				//write the fifo to start send media file
				main_activity.pos.write(1);
				
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
		String sensor_values = null;
		
		MainActivity main_activity = (MainActivity)Preview.this.getContext();
		if(main_activity.settingWindow.chosenSensorType == 1){
			sensor_values =  "gyroscope("+gyroscope_x+", "+gyroscope_y+", "+gyroscope_z+")";
		}else if(main_activity.settingWindow.chosenSensorType == 2){
			sensor_values = "accelerometer("+accelerometer_x+", "+accelerometer_y+", "+accelerometer_z+")";		
		}else if(main_activity.settingWindow.chosenSensorType == 3){
			sensor_values = "gravity("+gravity_x+", "+gravity_y+", "+gravity_z+")";
			
		}else if(main_activity.settingWindow.chosenSensorType == 4){
			sensor_values = "magnetic("+magnetic_x+", "+magnetic_y+", "+magnetic_z+")";
			
		}else if(main_activity.settingWindow.chosenSensorType == 7){
			sensor_values = "linear_acceleration("+linear_acceleration_x+", "+linear_acceleration_y+", "+linear_acceleration_z+")";
			
		}else if(main_activity.settingWindow.chosenSensorType == 5){
			sensor_values = "rotation("+rotation_x+", "+rotation_y+", "+rotation_z+")";
			
		}else if(main_activity.settingWindow.chosenSensorType == 6){
			sensor_values = "orientation("+orientation_x+", "+orientation_y+", "+orientation_z+")";
			
		}else{
			sensor_values = "invalid sensor type";
		}
		
		canvas.drawText(sensor_values, canvas.getWidth() / 2,
				canvas.getHeight() / 5, paint);
		
		//draw line
//		if(main_activity.settingWindow.chosenSensorType == 2){
//			canvas.drawLine(100, 400, 700, 400, paint);
//			canvas.drawLine(400,100, 400, 700,paint);
//			
//			int i = 0;
//			int startx = 100 ;
//			int starty = ((int)(150 * (this.acc_queue.peek().x/9.8)) + 400);
//			for(Acc acc: this.acc_queue){
//				int endx = 100 + i;
//				int endy = ((int)(150 * (acc.x / 9.8)) + 400);
//				//int endy = (int)(150 * (acc.x / 9.8)) + 400;
		
//				canvas.drawLine(startx, starty, endx, endy, paint);
//				startx = endx;
//				starty = endy;
//				i++;
//			}
//		}

		canvas.restore();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {			
			if(holder == null){
				Log.e(TAG, "===null holder===");
			}
			
			mCamera.setPreviewDisplay(mHolder);
			
			
			MainActivity main_activity = (MainActivity)Preview.this.getContext();
			mCamera.setPreviewCallback(new StreamIt(main_activity, Preview.this));
			mCamera.startPreview();

			isPreview = true;
			this.setWillNotDraw(false);
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.

		if (mCamera != null) {
			if (isPreview) {
				
				mCamera.stopPreview();
				isPreview = false;
			}
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null; // 记得释放
		}
		
		mHolder = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

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

		// start preview with new settings
		try {
			 mCamera.setPreviewDisplay(mHolder);
			 MainActivity main_activity = (MainActivity)Preview.this.getContext();
			 mCamera.setPreviewCallback(new StreamIt(main_activity,Preview.this));
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
	
	class Acc{
		public float x;
		public float y;
		public float z;
		
		public Acc(float x, float y, float z){
			this.x = x;
			this.z = z;
			this.y = y;
		}
		
		public void setX(float x){
			this.x = x;
		}
		public void setY(float y){
			this.y = y;
		}
		public void setZ(float z){
			this.z = z;
		}
	}
	
	public Queue<Acc> acc_queue = new LinkedList<Acc>();;

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		// TODO Auto-generated method stub
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
			accelerometer_x = event.values[0];
			accelerometer_y = event.values[1];
			accelerometer_z = event.values[2];
			
//			if(acc_n < 600){
//				Acc acc = new Acc(accelerometer_x,accelerometer_y,accelerometer_z);
//				acc_queue.add(acc);
//			}else if(acc_n == 600){
//				Acc acc = acc_queue.remove();
//				acc.setX(accelerometer_x);
//				acc.setY(accelerometer_y);
//				acc.setZ(accelerometer_z);
//				acc_queue.add(acc);
//				acc_n--;
//			}
//			
//			this.acc_n++;
			
		}else if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
			gravity_x = event.values[0];
			gravity_y = event.values[1];
			gravity_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			gyroscope_x = event.values[0];
			gyroscope_y = event.values[1];
			gyroscope_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
			magnetic_x = event.values[0];
			magnetic_y = event.values[1];
			magnetic_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
			linear_acceleration_x = event.values[0];
			linear_acceleration_y = event.values[1];
			linear_acceleration_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){
			orientation_x = event.values[0];
			orientation_y = event.values[1];
			orientation_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED){
			ungyroscope_x = event.values[0];
			ungyroscope_y = event.values[1];
			ungyroscope_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED){
			unmagnetic_x = event.values[0];
			unmagnetic_y = event.values[1];
			unmagnetic_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
			rotation_x = event.values[0];
			rotation_y = event.values[1];
			rotation_z = event.values[2];
		}else if(event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR){
			gamerotation_x = event.values[0];
			gamerotation_y = event.values[1];
			gamerotation_z = event.values[2];
		}
		
		if(isSavingPreview || isRecording){
			
			if(sensorFile == null){
				MainActivity main_activity = (MainActivity)Preview.this.getContext();
				sensorFile = main_activity.getOutputMediaFile(main_activity.MEDIA_TYPE_SENSOR);
				
				if(!sensorFile.exists()){
						try {
							sensorFile.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				
				}
			}
			
			String sensor_values = null;
			FileWriter fileWritter;
			try {
				String nowtime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
				sensor_values = nowtime + "_accelerometer("+accelerometer_x+", "+accelerometer_y+", "+accelerometer_z+")\r\n";
				sensor_values += nowtime + "_gravity("+gravity_x+", "+gravity_y+", "+gravity_z+")\r\n";
				sensor_values += nowtime + "_gyroscope("+gyroscope_x+", "+gyroscope_y+", "+gyroscope_z+")\r\n";
				sensor_values += nowtime + "_magnetic("+magnetic_x+", "+magnetic_y+", "+magnetic_z+")\r\n";
				sensor_values += nowtime + "_linear_acceleration("+linear_acceleration_x+", "+linear_acceleration_y+", "+linear_acceleration_z+")\r\n";
				sensor_values += nowtime + "_orientation("+orientation_x+", "+orientation_y+", "+orientation_z+")\r\n";
				sensor_values += nowtime + "_rotation("+rotation_x+", "+rotation_y+", "+rotation_z+")\r\n\r\n";
				fileWritter = new FileWriter(sensorFile.getAbsolutePath(),true);
				BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
		        bufferWritter.write(sensor_values);
		        bufferWritter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
		}
		
		
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

				final int padding = (int) (14 * scale + 0.5f); // convert dps to
																// pixels
				final int offset_y = (int) (32 * scale + 0.5f); // convert dps
																// to pixels
				canvas.save();

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


		clear_toast = new Toast(activity);
		View text = new RotatedTextView(message, activity);
		clear_toast.setView(text);
		clear_toast.setDuration(Toast.LENGTH_SHORT);
		clear_toast.show();

		return clear_toast;
	}

	public void setFocus(String focus_value) {
		
		if(isRecording){
			return;
		}
		
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

    		mCamera.setParameters(parameters);
        }
	}
	
    public void onResume() {
		if( MyDebug.LOG )
			Log.d(TAG, "onResume");

		try {
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(mHolder);
			MainActivity main_activity = (MainActivity)Preview.this.getContext();
			mCamera.setPreviewCallback(new StreamIt(main_activity, Preview.this));
			mCamera.startPreview();
			isPreview = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void onPause() {
//		if (mCamera != null) {
//			if (isPreview) {
//				//mCamera.setPreviewCallback(null);
//				mCamera.stopPreview();
//				isPreview = false;
//			}
//			mCamera.release();
//			mCamera = null; // 记得释放
//		}
    }

    
    public void takePhoto(){
		if(isRecording){
			return;
		}
    	mCamera.takePicture(shutterCallback, null, jpegCallback);
    }
    
    ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			// Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				// Write to SD Card
				MainActivity main_activity = (MainActivity)Preview.this.getContext();
				File picFile = main_activity.getOutputMediaFile(main_activity.MEDIA_TYPE_PHOTO);
				String picName = picFile.getAbsolutePath();
				
				FileOutputStream outputStream = new FileOutputStream(picFile);
				
				outputStream.write(data);
				outputStream.close();
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
				
				
				String sensorFileName = picName.replace("jpg", "txt");
                FileOutputStream sensorStream = new FileOutputStream(sensorFileName); 
                String sensorValues = "accelerometer:\r\n" + 
                		"x=" + Preview.this.accelerometer_x + "\r\n" +
                		"y=" + Preview.this.accelerometer_y + "\r\n" +
                		"z=" + Preview.this.accelerometer_z + "\r\n";
                 sensorValues += "\r\ngravity:\r\n" + 
                		"x=" + Preview.this.gravity_x + "\r\n" +
                		"y=" + Preview.this.gravity_y + "\r\n" +
                		"z=" + Preview.this.gravity_z + "\r\n";
                 sensorValues += "\r\ngyroscope:\r\n" + 
                 		"x=" + Preview.this.gyroscope_x + "\r\n" +
                 		"y=" + Preview.this.gyroscope_y + "\r\n" +
                 		"z=" + Preview.this.gyroscope_z + "\r\n";
                 sensorValues += "\r\nmagnetic:\r\n" + 
                  		"x=" + Preview.this.magnetic_x + "\r\n" +
                  		"y=" + Preview.this.magnetic_y + "\r\n" +
                  		"z=" + Preview.this.magnetic_z + "\r\n";
                 sensorValues += "\r\nlinear acceleration:\r\n" + 
                   		"x=" + Preview.this.linear_acceleration_x + "\r\n" +
                   		"y=" + Preview.this.linear_acceleration_y + "\r\n" +
                   		"z=" + Preview.this.linear_acceleration_z + "\r\n";
                 sensorValues += "\r\norientation:\r\n" + 
                    		"x=" + Preview.this.orientation_x + "\r\n" +
                    		"y=" + Preview.this.orientation_y + "\r\n" +
                    		"z=" + Preview.this.orientation_z + "\r\n";  
                 sensorValues += "\r\nrotation vector:\r\n" + 
                  		"x=" + Preview.this.rotation_x + "\r\n" +
                  		"y=" + Preview.this.rotation_y + "\r\n" +
                  		"z=" + Preview.this.rotation_z + "\r\n"; 
                
                 
                byte [] bytes = sensorValues.getBytes(); 
                
                sensorStream.write(bytes);
                sensorStream.close();
				

				mCamera.startPreview();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};
	
}

class StreamIt implements Camera.PreviewCallback {
	static private int i=0;
	private MainActivity mActivity;
	private Preview mPreview;
	public StreamIt(MainActivity activity, Preview preview){
		this.mActivity = activity;
		this.mPreview = preview;
	}
	
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {   
        
    	if(this.mPreview.isSavingPreview && !this.mPreview.isRecording){
	        Size size = camera.getParameters().getPreviewSize();          
	        try{ 
	            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);  
	            if(image!=null){
	            	ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
	            	
	                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, bytestream); 
	                
					File picFile = this.mActivity.getOutputMediaFile(this.mActivity.MEDIA_TYPE_FRAME);
					String picName = picFile.getAbsolutePath();
	                
	                FileOutputStream picStream = new FileOutputStream(picFile);           
	                picStream.write(bytestream.toByteArray());
	                picStream.close();
	                
	                String sensorFileName = picName.replace("jpg", "txt");
	                FileOutputStream sensorStream = new FileOutputStream(sensorFileName); 
	                String sensorValues = "accelerometer:\r\n" + 
	                		"x=" + this.mPreview.accelerometer_x + "\r\n" +
	                		"y=" + this.mPreview.accelerometer_y + "\r\n" +
	                		"z=" + this.mPreview.accelerometer_z + "\r\n";
	                 sensorValues += "\r\ngravity:\r\n" + 
	                		"x=" + this.mPreview.gravity_x + "\r\n" +
	                		"y=" + this.mPreview.gravity_y + "\r\n" +
	                		"z=" + this.mPreview.gravity_z + "\r\n";
	                 sensorValues += "\r\ngyroscope:\r\n" + 
	                 		"x=" + this.mPreview.gyroscope_x + "\r\n" +
	                 		"y=" + this.mPreview.gyroscope_y + "\r\n" +
	                 		"z=" + this.mPreview.gyroscope_z + "\r\n";
	                 sensorValues += "\r\nmagnetic:\r\n" + 
	                  		"x=" + this.mPreview.magnetic_x + "\r\n" +
	                  		"y=" + this.mPreview.magnetic_y + "\r\n" +
	                  		"z=" + this.mPreview.magnetic_z + "\r\n";
	                 sensorValues += "\r\nlinear acceleration:\r\n" + 
	                   		"x=" + this.mPreview.linear_acceleration_x + "\r\n" +
	                   		"y=" + this.mPreview.linear_acceleration_y + "\r\n" +
	                   		"z=" + this.mPreview.linear_acceleration_z + "\r\n";
	                 sensorValues += "\r\norientation:\r\n" + 
	                    		"x=" + this.mPreview.orientation_x + "\r\n" +
	                    		"y=" + this.mPreview.orientation_y + "\r\n" +
	                    		"z=" + this.mPreview.orientation_z + "\r\n";  
	                 sensorValues += "\r\nrotation vector:\r\n" + 
	                  		"x=" + this.mPreview.rotation_x + "\r\n" +
	                  		"y=" + this.mPreview.rotation_y + "\r\n" +
	                  		"z=" + this.mPreview.rotation_z + "\r\n"; 
	                
	                 
	                byte [] bytes = sensorValues.getBytes(); 
	                
	                sensorStream.write(bytes);
	                sensorStream.close();     
	                
	                try{
	                	this.mActivity.queue.put(picName);
	    			}catch (Exception e) {
	    	            e.printStackTrace();
	    	        }
	            }  
	        
	        }catch(Exception ex){  
	            Log.e("Sys","Error:"+ex.getMessage());  
	        }    
    	}
    }
}
