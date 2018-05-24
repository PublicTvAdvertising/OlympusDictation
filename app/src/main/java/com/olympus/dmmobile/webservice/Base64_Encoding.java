package com.olympus.dmmobile.webservice;


import android.app.Activity;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
/**
 * Base64_Encoding class is used to convert the string to base encoded form
 * @version 1.0.1
 * 
 *
 */

public class Base64_Encoding extends Activity{
	/**
	 * 
	 * mBase64Data is the converted base64 data
	 * mDatabytes is byte array in  which the string is converted to bytes
	 * 
	 * 
	 */
	private String mBase64Data;
	private byte[] mDatabytes=null; 
	/**
	 * @param textData is the string that is required to convert into base64 data
	 * @return Processed base64data
	 * 
	 * 
	 */
	public String base64(String textData) {
				
					     try {
							mDatabytes = textData.getBytes("UTF-8");
					    }
			catch (UnsupportedEncodingException e1) {
			    	e1.printStackTrace();
			    }
		  mBase64Data = Base64.encodeToString(mDatabytes, Base64.NO_WRAP);
		
	      return mBase64Data.trim();
		
	}

}
