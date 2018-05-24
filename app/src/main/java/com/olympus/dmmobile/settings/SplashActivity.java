package com.olympus.dmmobile.settings;



import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;

import java.util.Locale;
/**
 * Class used to set about screen in settings
 * 
 * @version 1.0.1
 *
 */
public class SplashActivity extends Activity{
	private ExceptionReporter mReporter;	
    public static final String PREFS_NAME = "Config";
    private DMApplication dmApplication=null;
    private SharedPreferences mSharedPreferences;//Error Logger
	private TextView mUuid;
	private TextView mApp;
	private String mprefUUID;
	private Locale mlocale;
	private String mAppversion;
	//private ImageView mOlymCap;
	private RelativeLayout mRelSplash;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mReporter = ExceptionReporter.register(this);
	    super.onCreate(savedInstanceState);
	    if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}else{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	    setContentView(R.layout.splashscreen);
	    dmApplication=(DMApplication)getApplication();
		dmApplication.setContext(this);
	    setCurrentLanguage(dmApplication.getCurrentLanguage());
	    mRelSplash=(RelativeLayout)findViewById(R.id.relSplash);
	    mApp=(TextView)findViewById(R.id.uuidappText);
	    mUuid=(TextView)findViewById(R.id.uuidText);
	    //mOlymCap=(ImageView)findViewById(R.id.olymcaption);
	    appUUID();
	    try{
	    	mAppversion=getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
	    	mApp.setText("for Android Ver "+mAppversion);
	    	}
	    	catch(Exception e){
	    		
	    	}
	    autoResize(mUuid, mUuid.getText().length());
	}
	/**
	 * method used to get uuid stored in shared preference
	 * 
	 * 
	 */
	
	public void appUUID()
	{
		mSharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		if(mSharedPreferences.getString("UUID", "")!=null)
		{
			mprefUUID=mSharedPreferences.getString("UUID", "");
			mUuid.setText("UUID:"+mprefUUID);
		}
	}
	/**
	 * method used to adjust the length of textview in the layout
	 * 
	 * @param tt is the textview
	 * @param length is the length of textview
	 * 
	 * 
	 */
	
	public void autoResize(TextView tt,int length) {

		Spannable span = new SpannableString(tt.getText().toString());
		if (length>20) {

			span.setSpan(new RelativeSizeSpan(.7f), 0, tt.getText().length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tt.setText(span);
		
		}

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
		 
		 mlocale = new Locale(lang);
	        Resources res = getResources();
	        DisplayMetrics dm = res.getDisplayMetrics();
	        Configuration conf = res.getConfiguration();
	        conf.locale = mlocale;
	        res.updateConfiguration(conf, dm);
	      
	    }
	 /**
	  * Overriding method used when an physical key back is pressed
	  * 
	  * 
	  */
	 @Override
		public void onBackPressed() 
		{
		    System.gc();
		    Intent intent=new Intent(this,SettingsActivity.class);
			startActivity(intent);
			finish();
		}
	 /**
	  * method is used to unbind all the views in the layout
	  * 
	  * @param view is the base class for widgets
	  * 
	  * 
	  */
	 private void unbindDrawables(View view)
	    {
	            if (view.getBackground() != null)
	            {
	                    view.getBackground().setCallback(null);
	            }
	            if (view instanceof ViewGroup && !(view instanceof AdapterView))
	            {
	                    for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
	                    {
	                            unbindDrawables(((ViewGroup) view).getChildAt(i));
	                    }
	                    ((ViewGroup) view).removeAllViews();
	            }
	    }
	 /**
	  * Overriding method used when an activity is destroyed
	  * 
	  * 
	  */
	 @Override
	    protected void onDestroy()
	    {
	            super.onDestroy();

	            unbindDrawables(findViewById(R.id.uuidText));
	            unbindDrawables(findViewById(R.id.uuidappText));
	            //unbindDrawables(findViewById(R.id.olymcaption));
	            unbindDrawables(findViewById(R.id.relSplash));
	           
	            System.gc();
	    }
}
