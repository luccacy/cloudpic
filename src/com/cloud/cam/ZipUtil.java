package com.cloud.cam;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil{
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
