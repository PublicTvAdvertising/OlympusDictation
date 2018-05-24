package com.olympus.dmmobile;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
/**
 * 
 * Class to show dialog if any on launch application
 * 
 * 
 *
 */
public class CustomLaunchDialog extends Activity {
	private String mResultcode;
	private DMApplication dmApplication=null;
	private Locale locale;
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    dmApplication=(DMApplication)getApplication();
	    dmApplication.setTimeOutDialogOnFront(true);
	    setTheme(android.R.style.Theme_Translucent_NoTitleBar);
	    setCurrentLanguage(dmApplication.getCurrentLanguage());	   
	    mResultcode=getIntent().getStringExtra("Resultcode");
	    if(!mResultcode.equalsIgnoreCase(getResources().getString(R.string.Dictate_Network_Notavailable))){
	    AlertDialog.Builder alert = new AlertDialog.Builder(CustomLaunchDialog.this);
	    alert.setTitle(dmApplication.validateResponse(mResultcode));
	    if(mResultcode.equalsIgnoreCase("5002"))
	    alert.setMessage(getIntent().getStringExtra("Message"));
	    else
	    alert.setMessage(dmApplication.errorOnlaunch(mResultcode));
	    alert.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok),new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});
		alert.create().show();
	    }
	    else{
	    	AlertDialog.Builder alert = new AlertDialog.Builder(CustomLaunchDialog.this);
		    alert.setMessage(getResources().getString(R.string.Dictate_Network_Notavailable));
		    alert.setTitle(getResources().getString(R.string.Alert));
		    alert.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok),new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			});
			alert.create().show();
	    }
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		dmApplication.setTimeOutDialogOnFront(false);
	}
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		dmApplication.setTimeOutDialogOnFront(false);
		finish();
	}
	 /**
	 * Method to find the locale from sharedpreference
	 * 
	 * @param value is the value in shared preference used to get locale 
	 * 
	 * 
	 */
	private void setCurrentLanguage(int value){
		 switch (value) {
			case 1:
				setLocale("en");
			break;
			case 2:
				setLocale("de");
			break;
			case 3:
				setLocale("fr");
			break;
			case 4:
				setLocale("es");
			break;
			case 5:
				setLocale("sv");
			break;
			case 6:
				setLocale("cs");
			break;
			default:
				setLocale("en");
			break;
		}
	 }
	/**
	 * Method used to set the language for the current activity
	 * 
	 * @param lang is the locale get from sharedpreference
	 * 
	 * 
	 */
	private void setLocale(String lang) {
       locale = new Locale(lang);
       Resources res = getResources();
       DisplayMetrics dm = res.getDisplayMetrics();
       Configuration conf = res.getConfiguration();
       conf.locale = locale;
       res.updateConfiguration(conf, dm);
       res=null;
       dm=null;
       conf=null;
       lang=null;
	}
}
