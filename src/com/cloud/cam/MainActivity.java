package com.cloud.cam;


import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;


class MyDebug {
	static final boolean LOG = true;
}

class Conf{
	static final String server_ip = "192.168.1.112";
	static final String server_port = "7878";
	static final String version = "v1";
	static final String server_url = "http://" + server_ip + ":" + server_port + "/" + version;
	static final String account = "admin";
	static final String admin_url = server_url + "/" + account;
	
	static final String mediaDir = "/sdcard/DCIM/cloudcam";
}

public class MainActivity extends Activity {
	private final String TAG = "MainActivity";
    private Preview mPreview;
    private ImageButton mSetting;
    private boolean isRecording = false;
    private SensorManager mSensorManager = null;
	private Sensor mSensorAccelerometer = null;
	private Sensor mSensorGravity = null;
	private Sensor mSensorGyroscope = null;
	private Sensor mSensorLineAcceleration = null;
	private Sensor mSensormegnetic = null;
	private Sensor mOrientation = null;
	private Sensor mUngyroscope = null;
	private Sensor mUnmegnetic = null;
	private Sensor mRotation = null;
	private Sensor mGameRotation = null;
		
	private PopupWindow popupWindow;
	private LinearLayout layout;
	private ListView listView;
    
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_FRAME = 3;
    public static final int MEDIA_TYPE_PHOTO = 4;
    public static final int MEDIA_TYPE_SENSOR = 5;
    private static int nFrame = 1;
    private static String lastTime = null;
    
    private File framesDir = null;
    private File photosDir = null;
    private File sensorsDir = null;
    private File videosDir = null;
    public String cacheDir = null;
    public boolean isFirstTime = true;
    
    public LayoutInflater inflater = null ;
    public SettingWindow settingWindow = null;
    
    public SendThread sendThread = null;
    public PipedOutputStream pos_send = null;
    public PipedInputStream pis_send = null;
    public BlockingQueue queue_send = null;
    
    public RecvThread recvThread = null;
    public PipedOutputStream pos_recv = null;
    public PipedInputStream pis_recv = null;
    public BlockingQueue queue_recv = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
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
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null ) {
			Log.e(TAG, "found Gravity");
			mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ) {
			Log.e(TAG, "found Gravity");
			mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null ) {
			Log.e(TAG, "found Gravity");
			mSensorLineAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null ) {
			Log.e(TAG, "found Gravity");
			mSensormegnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null ) {
			Log.e(TAG, "found Gravity");
			mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED) != null ) {
			Log.e(TAG, "found Gravity");
			mUngyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED) != null ) {
			Log.e(TAG, "found Gravity");
			mUnmegnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ) {
			Log.e(TAG, "found Gravity");
			mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		if( mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null ) {
			Log.e(TAG, "found Gravity");
			mGameRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		}
		else {
				Log.e(TAG, "no support for Gravity");
		}
		
		
        // Create our Preview view and set it as the content of our activity.
        mPreview = new Preview(this, savedInstanceState);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        
        mSetting = (ImageButton) findViewById(R.id.setting);
        inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        settingWindow = new SettingWindow();
        
        //create send media thread
        pos_send = new PipedOutputStream();
        pis_send = new PipedInputStream();
        queue_send = new ArrayBlockingQueue(1);
        
        pos_recv = new PipedOutputStream();
        pis_recv = new PipedInputStream();
        queue_recv = new ArrayBlockingQueue(1);
        try {
            pis_send.connect(pos_send);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        sendThread = new SendThread(pis_send, queue_send, pos_recv, queue_recv);
        sendThread.start();
        

        try {
            pis_recv.connect(pos_recv);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        recvThread = new RecvThread(pis_recv, queue_recv);
        recvThread.start();
        
	}
	
    @Override
    protected void onResume() {
		if( MyDebug.LOG )
			Log.d(TAG, "onResume");
        super.onResume();
        
        if(mSensorAccelerometer != null){
        	mSensorManager.registerListener(this.mPreview, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorGravity != null){
        	mSensorManager.registerListener(this.mPreview, mSensorGravity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorGyroscope!= null){
        	mSensorManager.registerListener(this.mPreview, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorLineAcceleration!= null){
        	mSensorManager.registerListener(this.mPreview, mSensorLineAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensormegnetic!= null){
        	mSensorManager.registerListener(this.mPreview, mSensormegnetic, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mOrientation!= null){
        	mSensorManager.registerListener(this.mPreview, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mUngyroscope!= null){
        	mSensorManager.registerListener(this.mPreview, mUngyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mUnmegnetic!= null){
        	mSensorManager.registerListener(this.mPreview, mUnmegnetic, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mRotation!= null){
        	mSensorManager.registerListener(this.mPreview, mRotation, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mGameRotation!= null){
        	mSensorManager.registerListener(this.mPreview, mGameRotation, SensorManager.SENSOR_DELAY_NORMAL);
        }
        
        this.mPreview.onResume();
    }

    @Override
    protected void onPause() {
		if( MyDebug.LOG )
			Log.d(TAG, "onPause");
        super.onPause();
        mSensorManager.unregisterListener(this.mPreview);
        this.mPreview.onPause();
        this.isFirstTime = true;
        
        try {
			this.pos_send.write(0);
			this.pos_recv.write(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
//    @Override
//    protected void onStop() {
//		if( MyDebug.LOG )
//			Log.d(TAG, "onStop");
//        super.onPause();
//        mSensorManager.unregisterListener(this.mPreview); 
//        
//    }
   
	public void onClickedRecordVideo(View view){
		this.mPreview.videoRecorder();
	}

	public void onClickedSetting(View view){
		settingWindow.onClickedBtn(this);
	}

	public void onClickedAutoFocus(View view){
		this.mPreview.setFocus("focus_mode_auto");
	}
	
	public void onClickedTakePhoto(View view){
		this.mPreview.takePhoto();
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
	
	public void onClickedSavePreview(View view){
		this.mPreview.savePreviewAndSensors();
	}
	
	public File getOutputMediaFile(int type){

    	File mediaStorageDir = new File("/sdcard/DCIM/cloudcam");
    	
        if( !mediaStorageDir.exists() ) {
            if( !mediaStorageDir.mkdirs() ) {
        		if( MyDebug.LOG )
        			Log.e(TAG, "failed to create directory");
                return null;
            }
	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(mediaStorageDir)));
        }
        
        if(isFirstTime){
        	String starttime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        	framesDir = new File("/sdcard/DCIM/cloudcam/" + starttime + "/frames");
        	photosDir = new File("/sdcard/DCIM/cloudcam/" + starttime + "/photos");
        	sensorsDir = new File("/sdcard/DCIM/cloudcam/" + starttime + "/sensors");
        	videosDir = new File("/sdcard/DCIM/cloudcam/" + starttime + "/videos");
        	cacheDir = "/sdcard/DCIM/cloudcam/" + starttime;
        	
        	if( !framesDir.exists() ) {
                if( !framesDir.mkdirs() ) {
            		if( MyDebug.LOG )
            			Log.e(TAG, "failed to create frames directory");
                    return null;
                }
    	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(framesDir)));
            }
        	
        	if( !photosDir.exists() ) {
                if( !photosDir.mkdirs() ) {
            		if( MyDebug.LOG )
            			Log.e(TAG, "failed to create frames directory");
                    return null;
                }
    	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(photosDir)));
            }
        	
        	if( !sensorsDir.exists() ) {
                if( !sensorsDir.mkdirs() ) {
            		if( MyDebug.LOG )
            			Log.e(TAG, "failed to create frames directory");
                    return null;
                }
    	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(sensorsDir)));
            }
        	
        	if( !videosDir.exists() ) {
                if( !videosDir.mkdirs() ) {
            		if( MyDebug.LOG )
            			Log.e(TAG, "failed to create frames directory");
                    return null;
                }
    	        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(videosDir)));
            }
        	
        	isFirstTime = false;
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if(timeStamp.equals(lastTime)){
        	nFrame++;
        }else{
        	nFrame=1;
        }
        File mediaFile;
        if( type == MEDIA_TYPE_IMAGE ) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        }
        else if( type == MEDIA_TYPE_VIDEO ) {
            mediaFile = new File(videosDir.getPath() + File.separator +
            "VID_"+ timeStamp +  ".mp4");
        }
        else if(type == MEDIA_TYPE_FRAME){
        	mediaFile = new File(framesDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + "_" + nFrame + ".jpg");
        }
        else if (type == MEDIA_TYPE_PHOTO){
        	mediaFile = new File(photosDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        }
        else if (type == MEDIA_TYPE_SENSOR){
        	mediaFile = new File(sensorsDir.getPath() + File.separator +
                    "sensors_values.txt");
        }
        else {
            return null;
        }

		if( MyDebug.LOG ) {
			Log.d(TAG, "TYPE returns: " + type);
			Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
		}
		lastTime = timeStamp;
        return mediaFile;
    }
}


