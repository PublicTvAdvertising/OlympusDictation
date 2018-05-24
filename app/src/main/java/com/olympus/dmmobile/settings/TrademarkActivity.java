package com.olympus.dmmobile.settings;



import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;

import java.util.Locale;
/**
 * TrademarkActivity class is used to set the trademark in settings
 * 
 * @version 1.0.1
 * 
 *
 */

public class TrademarkActivity extends Activity {
	/**
	 * mReporter is the object of ExceptionReporter class
	 * mLocale is used for localization , setting the language in application
	 * dmApplication is the object of DMApplication class
	 * 
	 *  
	 */
	
	
	private ExceptionReporter mReporter;			//Error Logger
	private Locale mLocale;
	private DMApplication dmApplication=null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mReporter = ExceptionReporter.register(this);
	    super.onCreate(savedInstanceState);
	    if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}else{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	    setTitle(getResources().getString(R.string.Trademarks));
	    setContentView(R.layout.trademarknotice);
	    dmApplication=(DMApplication)getApplication();
	    setCurrentLanguage(dmApplication.getCurrentLanguage());	   
		dmApplication.setContext(this);
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		 setCurrentLanguage(dmApplication.getCurrentLanguage());
	}
	
	/**
	 * method to find the locale from sharedpreference
	 * 
	 * @param value is the value in shared preference used to get locale 
	 * 
	 *  
	 */
	public void setCurrentLanguage(int value){
		 int Val=1;
		 Val=value;
		 if(Val==1)
				setLocale("en");
			else if(Val==2)
				setLocale("de");
			else if(Val==3)
				setLocale("fr");
			else if(Val==4)
				setLocale("es");
			else if(Val==5)
				setLocale("sv");
			else if(Val==6)
				setLocale("cs");
	 }
	/**
	 * method used to set the language for the current activity
	 * 
	 * @param lang is the locale get from sharedpreference
	 * 
	 * 
	 */
	 public void setLocale(String lang) {
		 
		    mLocale = new Locale(lang);
	        Resources res = getResources();
	        DisplayMetrics dm = res.getDisplayMetrics();
	        Configuration conf = res.getConfiguration();
	        conf.locale = mLocale;
	        res.updateConfiguration(conf, dm);
	      
	    }

}
