package com.cloud.cam;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;


class MyDebug {
	static final boolean LOG = true;
}

public class MainActivity extends Activity {
	private final String TAG = "MainActivity";
    private Preview mPreview;
    private ImageButton mSetting;
    private boolean isRecording = false;
    private SensorManager mSensorManager = null;
	private Sensor mSensorAccelerometer = null;
	
	private PopupWindow popupWindow;
	private LinearLayout layout;
	private ListView listView;
    
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		
		setContentView(R.layout.activity_main);
		
		//register sensor
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ) {
				Log.e(TAG, "found accelerometer");
			mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		else {
				Log.e(TAG, "no support for accelerometer");
		}
		
        // Create our Preview view and set it as the content of our activity.
        mPreview = new Preview(this, savedInstanceState);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        
        mSetting = (ImageButton) findViewById(R.id.setting);
        
	}
	
    @Override
    protected void onResume() {
		if( MyDebug.LOG )
			Log.d(TAG, "onResume");
        super.onResume();
        mSensorManager.registerListener(this.mPreview, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        this.mPreview.onResume();
    }

    @Override
    protected void onPause() {
		if( MyDebug.LOG )
			Log.d(TAG, "onPause");
        super.onPause();
        mSensorManager.unregisterListener(this.mPreview);
        this.mPreview.onPause();
    }
	
//    @Override
//    protected void onStop() {
//		if( MyDebug.LOG )
//			Log.d(TAG, "onStop");
//        super.onPause();
//        mSensorManager.unregisterListener(this.mPreview); 
//        
//    }
    
    public void showSettingWindow(int x, int y) {
//    	layout = (LinearLayout) LayoutInflater.from(MainActivity.this).inflate(
//				R.layout.dialog, null);
//		listView = (ListView) layout.findViewById(R.id.lv_dialog);
//		listView.setAdapter(new ArrayAdapter<String>(MainActivity.this,
//				R.layout.text, R.id.tv_text, title));

		popupWindow = new PopupWindow(MainActivity.this);
		popupWindow.setBackgroundDrawable(new BitmapDrawable());
		popupWindow
				.setWidth(getWindowManager().getDefaultDisplay().getWidth() / 2);
		popupWindow.setHeight(300);
		popupWindow.setOutsideTouchable(true);
		popupWindow.setFocusable(true);
		popupWindow.setContentView(layout);
		// showAsDropDown会把里面的view作为参照物，所以要那满屏幕parent
		// popupWindow.showAsDropDown(findViewById(R.id.tv_title), x, 10);
//		popupWindow.showAtLocation(findViewById(R.id.activity_main), Gravity.LEFT
//				| Gravity.TOP, x, y);
    }
	public void onClickedRecordVideo(View view){
		this.mPreview.videoRecorder();
	}

	public void onClickedSetting(View view){
		showSettingWindow(100,100);
	}

	public void onClickedAutoFocus(View view){
		this.mPreview.setFocus("focus_mode_auto");
	}
	
	public void onClickedVideoShow(View view) {
		String videoPath = "/sdcard/DCIM/cloudcam";
		File mediaStorageDir = new File("/sdcard/DCIM/cloudcam");
        if( !mediaStorageDir.exists() ) {
            if( !mediaStorageDir.mkdirs() ) {
        		if( MyDebug.LOG )
        			Log.e(TAG, "failed to create directory");
            }
	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(mediaStorageDir)));
        }
		
		
		Intent intent = new Intent();
		intent.putExtra("path", videoPath);
//		intent.setClass(this, FileShow.class);
		this.startActivity(intent);
	}
	
	public File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

    	File mediaStorageDir = new File("/sdcard/DCIM/cloudcam");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if( !mediaStorageDir.exists() ) {
            if( !mediaStorageDir.mkdirs() ) {
        		if( MyDebug.LOG )
        			Log.e(TAG, "failed to create directory");
                return null;
            }
	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(mediaStorageDir)));
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if( type == MEDIA_TYPE_IMAGE ) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        }
        else if( type == MEDIA_TYPE_VIDEO ) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        }
        else {
            return null;
        }

		if( MyDebug.LOG ) {
			Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
		}
        return mediaFile;
    }
    

}
