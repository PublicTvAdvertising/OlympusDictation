package com.olympus.dmmobile;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;

/**
 * This class helps to show dialogs over all views irrespective of the Activity context.
 * 
 * @version 1.0.1
 */
public class CustomDialog extends Activity {
	private Locale locale;
	private DMApplication dmApplication=null;
	private String mErrorCode=null;
	private DatabaseHandler mDbHandler=null;
	private AlertDialog mAlertDialog=null;
	private AlertDialog.Builder alertBuilder=null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    dmApplication=(DMApplication)getApplication();
	    dmApplication.setTimeOutDialogOnFront(true);
	    mDbHandler=dmApplication.getDatabaseHandler();
	    setTheme(android.R.style.Theme_Translucent_NoTitleBar);
//	    bundle=getIntent().getExtras();
	    setCurrentLanguage(dmApplication.getCurrentLanguage());
	    alertBuilder = new AlertDialog.Builder(this);
//	    System.out.println("ResponseCode  "+bundle.getString("ResponseCode"));
	    mErrorCode=dmApplication.getErrorCode();
	    alertBuilder.setCancelable(true);
	    alertBuilder.setTitle(dmApplication.validateResponse(mErrorCode));
	    if(mDbHandler.getSendingFailedDictations()<=0)
	    {
		    if(mErrorCode.trim().equalsIgnoreCase("5002"))
		    	alertBuilder.setMessage(dmApplication.getErrorMessage());
		    else
		    	alertBuilder.setMessage(dmApplication.errorOnlaunch(mErrorCode));
	    }
	    else
	    {
	    	if(mErrorCode.trim().equalsIgnoreCase("5002"))
	    		alertBuilder.setMessage(dmApplication.getErrorMessage()+" "+getString(R.string.Ils_Result_Resend_Selected));
		    else
		    	alertBuilder.setMessage(dmApplication.errorOnlaunch(mErrorCode)+" "+getString(R.string.Ils_Result_Resend_Selected));
	    }
	    alertBuilder.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok),new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mAlertDialog.dismiss();
			}
		});
	    mAlertDialog=alertBuilder.create();
	    mAlertDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dmApplication.setTimeOutDialogOnFront(false);
				finish();
			}
		});
	    mAlertDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				dmApplication.setTimeOutDialogOnFront(false);
				finish();
			}
		});
	    mAlertDialog.show();
		dmApplication.setErrorMessage("");
		dmApplication.setWantToShowDialog(false);
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
	 * To get current language's code from an Application preference.
	 * 
	 * @param value language code as int
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
	 * To set current view's language.
	 * 
	 * @param lang current language code from an Application preferences.
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
