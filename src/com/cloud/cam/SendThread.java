package com.cloud.cam;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.util.Log;

public class SendThread extends Thread{
	
	private String TAG = "SendThread";
	private PipedInputStream pis_send;
    private BlockingQueue queue_send;
	private PipedOutputStream pos_recv;
    private BlockingQueue queue_recv;
    private boolean stopped = false;
    
    HttpURLConnection conn = null;
    private String urlPath = null;
    private String container_name = "container_test";
    private String object_name = null;

    public SendThread(PipedInputStream pis_send, BlockingQueue queue_send,
    		PipedOutputStream pos_recv, BlockingQueue queue_recv) {
        this.pis_send = pis_send;
        this.queue_send = queue_send;
        this.pos_recv = pos_recv;
        this.queue_recv = queue_recv;
    }

    public void run() {
        try {
            while(!stopped)
            {
            	if(pis_send.read() == 0){
            		stopped = true;
            	}
				
				String filePath = (String)queue_send.take();
				String destZipPath = filePath + ".zip";
				
				Log.e(TAG, "filepath : " + filePath + "zip filepath : " + destZipPath);
				
				ZipUtil zipUtil = new ZipUtil();
				try {  
					zipUtil.compressedFile(filePath, destZipPath);
					Thread.sleep(2000);
				}catch (Exception e) {  
		            System.out.println("failed to compress...");  
		            e.printStackTrace();  
		        }  
		
				initConn(filePath);
				sendFile(destZipPath);
				closeConn();
				
				this.queue_recv.add(filePath);
				pos_recv.write(1);
				
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void initConn(String filePath){
    	String [] splitFilePath = filePath.split("\\/");
    	this.object_name = splitFilePath[splitFilePath.length - 1];
    	
    	urlPath = Conf.admin_url + "/" + this.container_name + "/" + this.object_name;
		try {
			URL url = new URL(urlPath);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setReadTimeout(5 * 1000);
			conn.setDoOutput(true); // 发送POST请求， 必须设置允许输出
			conn.setUseCaches(false);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    private void closeConn(){
    	conn.disconnect();
    }
    
    private void sendFile(String filePath){
		File file = new File(filePath);
		
		try{
			conn.setRequestProperty("Content-Type", "application/octet-stream");
			conn.setRequestProperty("Transfer-Encoding", "chunked");
			conn.setRequestProperty("x-image-meta-size", ""+file.length());
			conn.setRequestProperty("x-image-meta-name", file.getName());
			
			DataOutputStream outStream = new DataOutputStream(
					conn.getOutputStream());
			
			InputStream inStream = new FileInputStream(file);
			
			byte[] buffer =new byte[10 *1024];
			int rlen = 0;
			
			int nlen = (int) file.length();
			while(nlen > 0){
				if(nlen >= 10*1024){			
					rlen = inStream.read(buffer, 0, 10*1024);
					outStream.write(buffer);
					nlen = nlen - rlen;
				}else{
					buffer =new byte[nlen];
					rlen = inStream.read(buffer, 0, nlen);
					outStream.write(buffer);
					nlen = nlen - rlen;
				}
			}
			
			outStream.flush();
			outStream.close();
			inStream.close();
			int responseCode = conn.getResponseCode();
			
			if (responseCode != 200) {
				Log.e(TAG, "http response code not equil 200");
			}
			
		}catch(ConnectException e){
			Log.e(TAG, "connect refused");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

