package com.olympus.dmmobile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AmrInputStream;
import android.support.v4.util.LogWriter;
import android.util.Log;

/**
 * AMRConverter is a helper class which is used to convert 16 bit PCM to AMR-NB (8 kHz) using AmrInputStream
 *
 * @version 1.0.1
 */

public class AMRConverter {
    static {
        System.loadLibrary("native_lib");
    }

    /**
     * native method which is used for sample rate conversion.
     *
     * @param sourceFile the path of audio file which needs to be re-sampled.
     * @return return 0 if sample rate conversion is success.
     */
    public native int resample(String sourceFile);

    FileOutputStream out;
    AmrInputStream aStream;
    ByteArrayInputStream bis;
    int len = 0;

    /**
     * Prepare the AMR file and starts sample rate conversion process.
     *
     * @param source path of PCM source file
     * @param dest   path of destination file
     * @return returns 1 if success, else 0
     */
    int startConv;
    public int convert(String source,String dest){
        try {
            File file=new File(dest);
            if(!file.exists()){
                file.createNewFile();
            }
            out= new FileOutputStream(file);
            // AMR header format
            byte[] HEADER = {0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A};
            int i=0;
            while(i!=6){
                out.write(HEADER[i]);
                i++;
            }
            // change the sample rate of pcm file from 16 kHz to 8 kHz
            startConv=0;
            int ret;
            //LogWriter.writeLog("Resample","Starting");
            Thread.sleep(50);
            ret = resample(source);
            Thread.sleep(50);
            //LogWriter.writeLog("Resample","Completed->"+ret);
            return ret;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block

           // LogWriter.writeLogException("convert",e);
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
           // LogWriter.writeLogException("convert",e);
            e.printStackTrace();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
          //  LogWriter.writeLogException("convert",e);
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * this method is called from native with the re-sampled data, which is then encoded to AMR
     * by using AmrInputStream and save as AMR file.
     *
     * @param sBuffer re-sampled buffer
     */
    private void encode(short[] sBuffer){

        byte[] buffer=new byte[sBuffer.length*2];
      //  LogWriter.writeLog("Encode","Lenghth="+sBuffer.length);
        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(sBuffer);

        bis=new ByteArrayInputStream(buffer);
        aStream = new AmrInputStream(bis);
        byte[] x = new byte[1024];

        try {
            //LogWriter.writeLog("Encode","Reading resampled->"+startConv);
            Thread.sleep(100);
            ++startConv;
            while ((len=aStream.read(x)) > 0)
            {   out.write(x,0,len);
            }
            aStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
          //  LogWriter.writeLogException("Encode",e);
            e.printStackTrace();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
         //   LogWriter.writeLogException("Encode",e);
            e.printStackTrace();
        }
    }
    /**
     * close FileOutputStream
     */
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}