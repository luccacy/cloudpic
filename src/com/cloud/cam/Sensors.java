package com.cloud.cam;

import java.util.LinkedList;
import java.util.Queue;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Sensors {
	public Queue<SensorValue> acc_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> gra_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> gyr_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> mag_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> ori_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> rot_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> accel_queue = new LinkedList<SensorValue>();
	public Queue<SensorValue> sensor_queue = null;

	public static final int GYROSCOPE = 1;
	public static final int ACCELEROMETER = 2;
	public static final int GRAVITY = 3;
	public static final int MAGNETIC = 4;
	public static final int ORIENTATION = 5;
	public static final int ROTATION = 6;
	public static final int ACCELERRATION = 7;
	public int chosenSensorType = 2;

	private float max_acc;
	private float max_gra;
	private float max_gyr;
	private float max_mag;
	private float max_ori;
	private float max_rot;
	private float max_accel;
	private float max_sensor_value;

	public int MAX_POINTS = 400;
	private int R = MAX_POINTS / 2;
	private int zero_x = 0;
	private int zero_y = 0;

	private Paint paint = new Paint();

	private static final Sensors mSensors = new Sensors();

	private Sensors() {
	}

	public static Sensors getInstance() {
		return mSensors;
	}

	public void draw(Canvas canvas) {
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(3);

		int zero_x = canvas.getWidth() / 2;
		int zero_y = canvas.getHeight() / 4;

		canvas.drawLine(zero_x - R, zero_y, zero_x + R, zero_y, paint);
		canvas.drawLine(zero_x, zero_y - R, zero_x, zero_y + R, paint);

		if (chosenSensorType == GYROSCOPE) {
			sensor_queue = gyr_queue;
			max_sensor_value = max_gyr;
		} else if (chosenSensorType == ACCELEROMETER) {
			sensor_queue = acc_queue;
			max_sensor_value = max_acc;
		} else if (chosenSensorType == GRAVITY) {
			sensor_queue = gra_queue;
			max_sensor_value = max_gra;
		} else if (chosenSensorType == MAGNETIC) {
			sensor_queue = mag_queue;
			max_sensor_value = max_mag;
		} else if (chosenSensorType == ORIENTATION) {
			sensor_queue = ori_queue;
			max_sensor_value = max_ori;
		} else if (chosenSensorType == ROTATION) {
			sensor_queue = rot_queue;
			max_sensor_value = max_rot;
		} else if (chosenSensorType == ACCELERRATION) {
			sensor_queue = accel_queue;
			max_sensor_value = max_accel;
		} else {
			return;
		}

		// draw accelerometer line

		int acc_n = this.sensor_queue.size();
		if (acc_n > 0) {
			int starty_x = zero_y
					- (int) (this.sensor_queue.peek().x / max_sensor_value * (R / 1));
			int starty_y = zero_y
					- (int) (this.sensor_queue.peek().y / max_sensor_value * (R / 1));
			int starty_z = zero_y
					- (int) (this.sensor_queue.peek().z / max_sensor_value * (R / 1));
			int startx = zero_x + R - acc_n;
			int i = 0;

			if (acc_n <= MAX_POINTS) {
				for (SensorValue acc : this.sensor_queue) {
					int endx = zero_x + R - acc_n + i;
					int endy_x = (int) (zero_y - (float) ((R / 1) * (acc.x / max_sensor_value)));
					int endy_y = (int) (zero_y - (float) ((R / 1) * (acc.y / max_sensor_value)));
					int endy_z = (int) (zero_y - (float) ((R / 1) * (acc.z / max_sensor_value)));
					paint.setColor(Color.WHITE);
					canvas.drawLine(startx, starty_x, endx, endy_x, paint);
					paint.setColor(Color.GREEN);
					canvas.drawLine(startx, starty_y, endx, endy_y, paint);
					paint.setColor(Color.RED);
					canvas.drawLine(startx, starty_z, endx, endy_z, paint);
					startx = endx;
					starty_x = endy_x;
					starty_y = endy_y;
					starty_z = endy_z;
					i++;
				}
			}
		}

	}

	public void setMaxAcc(float max_acc) {
		this.max_acc = max_acc;
	}

	public void setMaxGra(float max_gra) {
		this.max_gra = max_gra;
	}

	public void setMaxGyr(float max_gyr) {
		this.max_gyr = max_gyr;
	}

	public void setMaxMag(float max_mag) {
		this.max_mag = max_mag;
	}

	public void setMaxOri(float max_ori) {
		this.max_ori = max_ori;
	}

	public void setMaxRot(float max_rot) {
		this.max_rot = max_rot;
	}

	public void setMaxAccel(float max_accel) {
		this.max_accel = max_accel;
	}
}

class SensorValue {
	public float x;
	public float y;
	public float z;

	public SensorValue(float x, float y, float z) {
		this.x = x;
		this.z = z;
		this.y = y;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setZ(float z) {
		this.z = z;
	}
}
