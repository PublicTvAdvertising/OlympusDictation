/*
* Copyright (C) 2010 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.olympus.dmmobile.settings;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;

public class LegalNoticesActivity extends Activity {

	private ExceptionReporter mReporter;			//Error Logger
	private RelativeLayout relTrademark;
	private RelativeLayout relThirdparty;
	private Locale locale;
	private DMApplication dmApplication=null;
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
	    dmApplication=(DMApplication)getApplication();
	    dmApplication.setContext(this);    
	    setContentView(R.layout.legallist_layout);
	    setCurrentLanguage(dmApplication.getCurrentLanguage());
	    setTitle(getResources().getString(R.string.legal_notices_title));
	    relTrademark=(RelativeLayout)findViewById(R.id.relFirst);
	    relThirdparty=(RelativeLayout)findViewById(R.id.relSecond);
	    relThirdparty.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent legalIntent = new Intent(LegalNoticesActivity.this, LegalnoticeActivity.class);
				startActivity(legalIntent);
			}
		});
	    relTrademark.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent legalIntent = new Intent(LegalNoticesActivity.this, TrademarkActivity.class);
				startActivity(legalIntent);
			}
		});
	   	}
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
	 public void setLocale(String lang) {
		 
	        locale = new Locale(lang);
	        Resources res = getResources();
	        DisplayMetrics dm = res.getDisplayMetrics();
	        Configuration conf = res.getConfiguration();
	        conf.locale = locale;
	        res.updateConfiguration(conf, dm);
	      
	    }
	 @Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
			 setCurrentLanguage(dmApplication.getCurrentLanguage());
		}
	
}
