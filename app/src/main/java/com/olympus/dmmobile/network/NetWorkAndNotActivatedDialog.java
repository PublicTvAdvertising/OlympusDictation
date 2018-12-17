package com.olympus.dmmobile.network;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.SplashscreenActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Singleton class to show alert message
 * 
 * @version 1.2.0
 */
public class NetWorkAndNotActivatedDialog {
	
	private static NetWorkAndNotActivatedDialog mDialog = null;
	private Context mContext = null;
	private DMApplication mDmApplication = null;
	private AlertDialog.Builder alert = null;
	private SharedPreferences mPreferences = null;

	/**
	 * get instance of class
	 * @return
	 */
	public static NetWorkAndNotActivatedDialog getInstance() {
		if(mDialog == null)
			mDialog = new NetWorkAndNotActivatedDialog();
		return mDialog;
	}
	/**
	 * Show alert dialog when there is no network connection/the application isn't activated
	 * @param mContext Context of current activity
	 */
	public void onShowDialog(Context mContext) {
		this.mContext = mContext;
		mDmApplication = (DMApplication) mContext.getApplicationContext();
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	    if(mPreferences.getString(mContext.getResources().getString(R.string.send_key), "").equalsIgnoreCase("2")) {
	    	if(!DMApplication.isONLINE())
	    		onShowAlert(true);
	    	else {
	    		if(mDmApplication != null && !mDmApplication.isTimeOutDialogOnFront()) {
	    			mPreferences = mContext.getSharedPreferences(SplashscreenActivity.PREFS_NAME, 0);
					if(mPreferences.getString("Activation", "Not Activated").equalsIgnoreCase("Not Activated")) 
						onShowAlert(false);
	    		}
	    	}
	    }
	    
	}
	
	private void onShowAlert(boolean isNoNetWork) {
		alert = new AlertDialog.Builder(mDmApplication.getContext());
		if (isNoNetWork){ 
			alert.setTitle(mContext.getResources().getString(R.string.Alert));
			alert.setMessage(mContext.getResources().getString(
					R.string.Dictate_Network_Notavailable));
		     alert.setTitle(mContext.getResources().getString(R.string.Alert));
		}
		else {
			alert.setTitle(mContext.getResources().getString(R.string.Ils_Result_Not_Activated));
			alert.setMessage(mContext.getResources().getString(R.string.Flashair_Alert_Activate_Account));
		}
		alert.setPositiveButton(mContext.getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alert.create().show();
	}
}
