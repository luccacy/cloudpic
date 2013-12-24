package com.cloud.cam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

import android.util.Log;

public class RecvThread extends Thread{
	private String TAG = "RecvThread";
	private PipedInputStream pis_recv;
    private BlockingQueue queue_recv;
    HttpURLConnection conn = null;
    private String urlPath = null;
    private String container_name = null;
    private String object_name = null;
    
    private boolean stopped = false;
    private boolean ready = false;
    private static int ntimes = 0;

    private final String HTTP_PUT = "PUT";
    private final String HTTP_GET = "GET";
    private final int CHUNKSIZE = 10 * 1024;
    
    public RecvThread(PipedInputStream pis_recv, BlockingQueue queue_recv) {
        this.pis_recv = pis_recv;
        this.queue_recv = queue_recv;
        this.container_name = "container_test";
    }

    public void run() {
        try {
            while(!stopped)
            {
            	if(pis_recv.read() == 0){
            		stopped = true;
            	}
				
				String filePath = (String)queue_recv.take();
				String destZipPath = filePath + ".zip";
				String resultZipPath = filePath + "_result.zip";
				
				Log.d(TAG, "filepath : " + filePath + "zip filepath : " + destZipPath);
			
				initConn(filePath, HTTP_PUT);
				while(true){
					if(getServerStatus().equals("ready") ){
						ntimes = 0;
						ready = true;
						break;
					}else if(ntimes >= 3){
						ntimes = 0;
						ready = false;
					}else{
						ntimes++;
						Thread.sleep(3000);
					}
					
				}
				closeConn();
				if(ready){
					initConn(filePath, HTTP_GET);
					if(recvFile(resultZipPath) < 0){
						Log.e(TAG, "failed to recv file");
					}
					closeConn();
				}else{
					continue;
				}
				
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
    }
    
    private void initConn(String filePath, String http_method){
    	String [] splitFilePath = filePath.split("\\/");
    	this.object_name = splitFilePath[splitFilePath.length - 1];
    	
    	urlPath = Conf.admin_url + "/" + this.container_name + "/" + this.object_name;
		try {
			URL url = new URL(urlPath);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(http_method);
			conn.setReadTimeout(5 * 1000);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void closeConn(){
    	conn.disconnect();
    }
    
    
    private int recvFile(String filePath){
		try {
			InputStream inStream = conn.getInputStream();

			File file = new File(filePath);
			OutputStream out = new FileOutputStream(file);
			int len = 0;
			byte[] data = new byte[CHUNKSIZE];
			while ((len = inStream.read(data, 0, CHUNKSIZE)) != -1) {
				out.write(data, 0, len);
			}

			return 0;
		} catch(ConnectException e){
			Log.e(TAG, "recvFile connect refused");
			return -1;
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
    }

    private String getServerStatus(){
    	StringBuilder str = new StringBuilder();
		try {
			InputStream inStream = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					inStream));
			str.append(reader.readLine());

		} catch(ConnectException e){
			Log.e(TAG, "getServerStatus connect refused");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str.toString();
    }
}
