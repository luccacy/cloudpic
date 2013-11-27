package com.cloud.cam;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;



public class SettingWindow {
	
	private String[] list = {"gyroscope","accelerometer","gravity","magnetic", 
							"orientation", "rotation", "linear_acceleration"};
	private PopupWindow popwindow;
	private ListView listview;
	private int NUM_OF_VISIBLE_LIST_ROWS = 6;
	private ImageButton SettingBtn;
	private HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	public static final int GYROSCOPE=1;
	public static final int ACCELEROMETER=2;
	public static final int GRAVITY=3;
	public static final int MAGNETIC=4;
	public static final int ORIENTATION=5;
	public static final int ROTATION=6;
	public static final int ACCELERRATION=7;
	public int chosenSensorType=2;
	
	public SettingWindow(){
		
	}
	

	private void iniPopupWindow(MainActivity main_activity) {
		
		View layout = main_activity.inflater.inflate(R.layout.sensor_listview, null);
		listview = (ListView) layout.findViewById(R.id.list);
		popwindow = new PopupWindow(layout);
		popwindow.setFocusable(true);// 

		final RadioAdapter adapter = new RadioAdapter(main_activity);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                        long arg3) {
                        	map.clear();
                            map.put(arg2, 100);
                                
                                if(list[arg2].equals("gyroscope")){
                                	chosenSensorType=GYROSCOPE;
                                }else if(list[arg2].equals("accelerometer")){
                                	chosenSensorType=ACCELEROMETER;
                                }else if(list[arg2].equals("gravity")){
                                	chosenSensorType=GRAVITY;
                                }else if(list[arg2].equals("magnetic")){
                                	chosenSensorType=MAGNETIC;
                                }else if(list[arg2].equals("orientation")){
                                	chosenSensorType=ORIENTATION;
                                }else if(list[arg2].equals("rotation")){
                                	chosenSensorType=ROTATION;
                                }else if(list[arg2].equals("linear_acceleration")){
                                	chosenSensorType=ACCELERRATION;
                                }else{
                                	chosenSensorType=-1;
                                }
                                adapter.notifyDataSetChanged();
                        }
                });
		listview.measure(View.MeasureSpec.UNSPECIFIED,
				View.MeasureSpec.UNSPECIFIED);
		popwindow.setWidth(listview.getMeasuredWidth()*2);
		popwindow.setHeight((listview.getMeasuredHeight() + 20)
				* NUM_OF_VISIBLE_LIST_ROWS);
		popwindow.setBackgroundDrawable(new BitmapDrawable());
		popwindow.setOutsideTouchable(true);
	}
    
    class RadioHolder{
        
        private TextView item;
        private RadioButton radio;
        public RadioHolder(View view){
                
                this.item = (TextView) view.findViewById(R.id.item_text);
                this.radio = (RadioButton) view.findViewById(R.id.item_radio);
        }
    }

    class RadioAdapter extends BaseAdapter{
         
        private Context context;
        public RadioAdapter(Context context){
                this.context = context;
        }
         
        @Override
        public int getCount() {
                // TODO Auto-generated method stub
                return list.length;
        }

        @Override
        public Object getItem(int arg0) {
                // TODO Auto-generated method stub
                return list[arg0];
        }

        @Override
        public long getItemId(int position) {
                // TODO Auto-generated method stub
                return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                // TODO Auto-generated method stub
                RadioHolder holder;
                if(convertView == null){
                        convertView = LayoutInflater.from(context).inflate(
                                        R.layout.sensor_item, null);
                        holder = new RadioHolder(convertView);
                        convertView.setTag(holder);
                }else{
                        holder = (RadioHolder) convertView.getTag();
                }
                holder.radio.setChecked(map.get(position) == null ? false : true);
                holder.item.setText(list[position]);
                return convertView;
        }
         
}


	
    public void showListView(MainActivity main_activity){

		iniPopupWindow(main_activity);
		SettingBtn = (ImageButton) main_activity.findViewById(R.id.setting);
		
		if (popwindow.isShowing()) {
			popwindow.dismiss();
		} else {
			popwindow.showAsDropDown(SettingBtn);
			
		}
    }
    
    public void onClickedBtn(MainActivity main_activity){
    	showListView(main_activity);
    }
}
