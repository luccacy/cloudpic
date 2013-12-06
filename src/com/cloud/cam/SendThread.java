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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SendThread extends Thread{
	
	private PipedInputStream pis;
    private BlockingQueue queue;

    public SendThread(PipedInputStream pis, BlockingQueue queue) {
        this.pis = pis;
        this.queue = queue;
    }

    public void run() {
        try {
            while(true)
            {
            	System.out.println("read fifo:"+pis.read());
				System.out.println("queue file:"+queue.take());
				
				String filePath = (String)queue.take();
				String destZipPath = filePath + ".zip";
				
				ZipUtil zipUtil = new ZipUtil();
				try {  
					zipUtil.compressedFile(filePath, destZipPath);
				}catch (Exception e) {  
		            System.out.println("failed to compress...");  
		            e.printStackTrace();  
		        }  
			
				sendFile(destZipPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void sendFile(String filePath){
		String urlPath = "http://10.12.13.11:7878/1/2/3/4";
		File file = new File(filePath);
		
		try{
			URL url = new URL(urlPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setReadTimeout(5 * 1000);
			conn.setDoOutput(true); // 发送POST请求， 必须设置允许输出
			conn.setUseCaches(false);
			
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
				throw new RuntimeException("请求url失败");
			}
			
			conn.disconnect();
			
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void getFile(String filePath){
    	
    	String urlPath = "http://10.12.13.11:7878/1/2/3/4";
		try {
			URL url = new URL(urlPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5 * 1000);
			conn.setDoOutput(true); // 发送POST请求， 必须设置允许输出
			conn.setUseCaches(false);

			InputStream inStream = conn.getInputStream();

			File file = new File(filePath);
			OutputStream out = new FileOutputStream(file);
			int len = 0;
			byte[] data = new byte[1024];
			while ((len = inStream.read(data, 0, 1024)) != -1) {
				out.write(data, 0, len);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void getStatus(String filePath){
    	String urlPath = "http://10.12.13.11:7878/1/2/3/4";
		try {
			URL url = new URL(urlPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout(5 * 1000);
			conn.setDoOutput(true); // 发送POST请求， 必须设置允许输出
			conn.setUseCaches(false);
			StringBuilder str = new StringBuilder();
			/****/
			InputStream inStream = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					inStream));
			str.append(reader.readLine());
			System.out.println(str.toString());
//			JSONObject obj = JSONObject.fromObject(str.toString());
//			String status = obj.getString("status");
//			System.out.println(status);
			// File file = new File("test.png");
			// OutputStream out = new FileOutputStream(file);
			// int len = 0;
			// byte[] data = new byte[1024];
			// while ((len = inStream.read(data, 0, 1024)) != -1) {
			// out.write(data, 0, len);
			// }

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}

class ZipUtil{
	private static final int BUFFER = 1024;  
	
	public ZipUtil(){    
    } 
	
	public void compressedFile(String resourcesPath,String targetPath) throws Exception{  
        File resourcesFile = new File(resourcesPath);       
        File targetFile = new File(targetPath);                 
          
        FileOutputStream outputStream = new FileOutputStream(targetPath);  
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(outputStream));  
          
        createCompressedFile(out, resourcesFile, "");           
        out.close();    
    }
	
	public void createCompressedFile(ZipOutputStream out,File file,String dir) throws Exception{  
         
        if(file.isDirectory()){  
            
            File[] files = file.listFiles();  
            
            out.putNextEntry(new ZipEntry(dir+"/"));                
            dir = dir.length() == 0 ? "" : dir +"/";  
                    
            for(int i = 0 ; i < files.length ; i++){  
                createCompressedFile(out, files[i], dir + files[i].getName());        
            }  
        }  
        else{   
            
            FileInputStream fis = new FileInputStream(file);  
              
            out.putNextEntry(new ZipEntry(dir));  
            
            int j =  0;  
            byte[] buffer = new byte[1024];  
            while((j = fis.read(buffer)) > 0){  
                out.write(buffer,0,j);  
            }  
            
            fis.close();  
        }  
    }
	
	public static void decompress(File srcFile, File destFile) throws Exception {  
	      
        CheckedInputStream cis = new CheckedInputStream(new FileInputStream(  
                srcFile), new CRC32());  
  
        ZipInputStream zis = new ZipInputStream(cis);  
  
        decompress(destFile, zis);  
  
        zis.close();  
  
    }
	
	public static void decompress(String srcPath, String destPath)  
            throws Exception {  
  
        File srcFile = new File(srcPath);  
        File destFile = new File(destPath);  
        decompress(srcFile, destFile);  
    }
	
	private static void decompress(File destFile, ZipInputStream zis)  
            throws Exception {  
  
        ZipEntry entry = null;  
        while ((entry = zis.getNextEntry()) != null) {  
  
            // 文件  
            String dir = destFile.getPath() + File.separator + entry.getName();  
  
            File dirFile = new File(dir);  
  
            // 文件检查  
            fileProber(dirFile);  
  
            if (entry.isDirectory()) {  
                dirFile.mkdirs();  
            } else {  
                decompressFile(dirFile, zis);  
            }  
  
            zis.closeEntry();  
        }  
    }  
	
	private static void fileProber(File dirFile) {  
	      
        File parentFile = dirFile.getParentFile();  
        if (!parentFile.exists()) {  
  
            // 递归寻找上级目录  
            fileProber(parentFile);  
  
            parentFile.mkdir();  
        }  
  
    } 
	
	private static void decompressFile(File destFile, ZipInputStream zis)  
            throws Exception {  
  
        BufferedOutputStream bos = new BufferedOutputStream(  
                new FileOutputStream(destFile));  
  
        int count;  
        byte data[] = new byte[BUFFER];  
        while ((count = zis.read(data, 0, BUFFER)) != -1) {  
            bos.write(data, 0, count);  
        }  
  
        bos.close();  
    }  
	
	public void test(){  
		ZipUtil compressedFileUtil = new ZipUtil();  
          
        try {  
            compressedFileUtil.compressedFile("e:\\update4.1", "e:\\update4.1.zip");  
            compressedFileUtil.decompress("e:/update4.1.zip", "d:/update4.1");
            System.out.println("begin zip compress...");  
        } catch (Exception e) {  
            System.out.println("failed to compress...");  
            e.printStackTrace();  
        }  
    }
}