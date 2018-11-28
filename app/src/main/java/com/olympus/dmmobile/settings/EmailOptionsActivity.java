package com.olympus.dmmobile.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;
/**
 * class used to save and configure email settings.
 * 
 * @version 1.0.1
 *
 */
public class EmailOptionsActivity extends Activity {

	private ExceptionReporter mReporter; // Error Logger
	private ChipsMultiAutoCompleteTextview mEditRecipients;
	private EditText mEditSubject;
	private EditText mEditMessage;
	private Button mContactchooser;
	private String mailRecip;
	private String[] mail;
	private String mSetEmail;
	private String[] mMail;
	private String mPrefMail = null;
	private static final int CONTACT_PICKER_RESULT = 1001;	
	private Button delthione;
	private Locale mLocale;
	private DMApplication dmApplication=null;
	private AlertDialog.Builder mAlertDialog;
	private int mPauseFlag=0;
	private SharedPreferences pref;
	private boolean isContactPermission = false;
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
		setContentView(R.layout.email_options_settings);
		dmApplication=(DMApplication)getApplication();
		setCurrentLanguage(dmApplication.getCurrentLanguage());
		dmApplication=(DMApplication)getApplication();
		dmApplication.setContext(this);
		setTitle(getResources().getString(R.string.sendoption_email));
		mContactchooser = (Button) findViewById(R.id.mailBtn);
		mEditRecipients = (ChipsMultiAutoCompleteTextview) findViewById(R.id.edit_recipients);
		mEditSubject = (EditText) findViewById(R.id.edit_subject);
		mEditMessage = (EditText) findViewById(R.id.edit_message_body);
		getWindow()
		.setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		delthione=(Button) findViewById(R.id.small_delete);
		delthione.performClick();
		mEditRecipients.setSelection(mEditRecipients.getText().length());	
		delthione.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View arg0) {
					delthione.setSoundEffectsEnabled(false);
					if(mEditRecipients.getText().length()!=0){
					int start = mEditRecipients.getSelectionStart();
					int end = mEditRecipients.getSelectionEnd();
					mEditRecipients.getText().replace(Math.min(start, end),
							Math.max(start, end), ",", 0, 1);
				}
			}
		});
		setValues();
		mContactchooser.setOnClickListener(new OnClickListener() {

			@RequiresApi(api = Build.VERSION_CODES.M)
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				pref=getSharedPreferences("Conpermissions",MODE_PRIVATE);
				isContactPermission=pref.getBoolean("Conperm",false);
				if(checkContactPermission()&&isContactPermission){
						InputMethodManager imm = (InputMethodManager)getSystemService(
									      Context.INPUT_METHOD_SERVICE);
									imm.hideSoftInputFromWindow(mEditRecipients.getWindowToken(), 0);
								    mEditRecipients.setSelection(mEditRecipients.getText().length());
										    if(dmApplication.getShowAlert()==0){
										    doLaunchContactPicker(v);
						    }}
			}
		});
		mEditRecipients.setOnEditorActionListener(new OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
								if (actionId == EditorInfo.IME_ACTION_DONE) {
									int start = mEditRecipients.getSelectionStart();
									int end = mEditRecipients.getSelectionEnd();
									mEditRecipients.getText().replace(Math.min(start, end),
											Math.max(start, end), ",", 0, 1);
						}
						return false;
					}
		});
		mEditRecipients.setOnKeyListener(new OnKeyListener() {           
			        @Override
			        public boolean onKey(View v, int key, KeyEvent event) {
			        	if((event.getAction()==KeyEvent.ACTION_DOWN)&&(key == KeyEvent.KEYCODE_ENTER)) {
							            	int start = mEditRecipients.getSelectionStart();
											int end = mEditRecipients.getSelectionEnd();
											mEditRecipients.getText().replace(Math.min(start, end),
													Math.max(start, end), ",", 0, 1);
							                return true;
			            }
			            return false;
			        }
	    });
	}
	@Override
	public void onBackPressed() {
				delthione.performClick();	
				if(dmApplication.getShowAlert()==0){
								mSetEmail = mEditRecipients.getText().toString().trim();
							if (mSetEmail.contains(",")) {
					
										mMail = mSetEmail.split(",");
										for (int i = 0; i < mMail.length; i++) {
													if (isValidEmail((mMail[i]))) {
																if (mPrefMail == null) {
																	mPrefMail = mMail[i];
																} else {
																	mPrefMail = mPrefMail + "," + mMail[i];
																}
													}
													else{
																showMessageAlert(getResources().getString(
																		R.string.Settings_Max_Recipients));
													}
										}
							} else {
								mPrefMail = "";
							}
							
							// Adding values to SharedPreference
									SharedPreferences pref = PreferenceManager
											.getDefaultSharedPreferences(this);
									Editor editor = pref.edit();
									editor.putString(getResources().getString(R.string.recipient_key),
											mPrefMail);
									editor.putString(getResources().getString(R.string.subject_key),
											mEditSubject.getText().toString());
									editor.putString(getResources().getString(R.string.message_key),
											mEditMessage.getText().toString());
									editor.commit();
									Intent intent=new Intent(this,SettingsActivity.class);
									startActivity(intent);
									finish();
				}
	}


	/**
	 * Method to set the values from preference to edittext
	 * 
	 * 
	 */
	public void setValues() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		mailRecip = pref.getString(
				getResources().getString(R.string.recipient_key), "");
		mEditSubject.setText(pref.getString(
				getResources().getString(R.string.subject_key), "Dictation"));
		mEditMessage.setText(pref.getString(
				getResources().getString(R.string.message_key), ""));
		if (mailRecip.length() > 0 && !mailRecip.contains(",")) {
						mail = new String[1];
						mail[0] = mailRecip + ",";
						if (mail != null) {
										for (int i = 0; i < mail.length; i++) {
													if (mEditRecipients.getText().toString().equals("")) {
														mEditRecipients.setText(mail[i] + ",");
														
													} else {
														mEditRecipients.setText(mEditRecipients.getText()
																.toString().trim()
																+ mail[i] + ",");
								
													}
										}
						}
						
		} else if (mailRecip.length() > 0 && mailRecip.contains(",")) {
			mail = mailRecip.split(",");
			if (mail != null) {
						for (int i = 0; i < mail.length; i++) {
									if (mEditRecipients.getText().toString().equals("")) {
										mEditRecipients.setText(mail[i] + ",");
				
									} else {
										mEditRecipients.setText(mEditRecipients.getText()
												.toString().trim()
												+ mail[i] + ",");
									}
						}
			}
			
		}
	}
/**
 * method to get the mail from contact list
 * 
 * @param view
 * 
 * 
 */
	public void doLaunchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK
				/*,contactPickerIntentContacts.CONTENT_URI */);
		contactPickerIntent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Cursor cursor = null;
				String email = "";
				try {

					Uri result = data.getData();	
					String id = result.getLastPathSegment();
					cursor = this.getContentResolver().query(Email.CONTENT_URI,
							null, Email._ID + "=?", new String[] { id },
							null);
					
					int emailColIdx = cursor.getColumnIndex(Email.DATA);
					cursor.moveToFirst();
					email = cursor.getString(emailColIdx);
					if (cursor.moveToFirst()) {
						email = cursor.getString(emailColIdx);
					 	
					} else {
						
					}
				} catch (Exception e) {
					
				} finally {
					if (cursor != null) {
						cursor.close();
					}
               if(isValidEmail(email)){
					if (mEditRecipients.getText().toString().equals("")) {
						
						int start = mEditRecipients.getSelectionStart();
						int end = mEditRecipients.getSelectionEnd();
						
						mEditRecipients.getText().replace(Math.min(start, end),
								Math.max(start, end), email, 0, email.length());
						getWindow()
								.setSoftInputMode(
										WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

					} else {
						
						int start = mEditRecipients.getSelectionStart();
						int end = mEditRecipients.getSelectionEnd();
						mEditRecipients.getText().replace(Math.min(start, end),
								Math.max(start, end), email, 0, email.length());
						getWindow()
								.setSoftInputMode(
										WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

					}
               }
               else{
            	  
               }
					
				}
				break;
			}
		} else {
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		setCurrentLanguage(dmApplication.getCurrentLanguage());
		mEditRecipients.setSelection(mEditRecipients.getText().length());
		getWindow()
		.setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		delthione.setVisibility(View.INVISIBLE);
		if(mPauseFlag!=1)
		delthione.performClick();
		mPauseFlag=0;

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
	 /**
	  * method to unbind all the views form layout
	  * 
	  * @param view widgets used in layout
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
	 @Override
	    protected void onDestroy()
	    {
	            super.onDestroy();

	            unbindDrawables(findViewById(R.id.mailBtn));
	            unbindDrawables(findViewById(R.id.edit_recipients));
	            unbindDrawables(findViewById(R.id.edit_subject));
	            unbindDrawables(findViewById(R.id.edit_message_body));
	            unbindDrawables(findViewById(R.id.small_delete));
	            System.gc();
	    }
	 @Override
	 protected void onPause(){
		 super.onPause();
		 mPauseFlag=1;
	 }
	 /**
	  * Method to show alert dialog
	  * 
	  * @param Message is the message displayed in alert dialog
	  * 
	  * 
	  */
	 public void showMessageAlert(String Message){
			dmApplication.setShowAlert(1);
			mAlertDialog= new AlertDialog.Builder(EmailOptionsActivity.this);
			mAlertDialog.setMessage(Message);
			mAlertDialog.setCancelable(false);
			mAlertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog,int which){
					dmApplication.setShowAlert(0);
				}
			});
			mAlertDialog.show(); 
		}
	 /**
	  * method to check email is valid or not
	  * 
	  * 
	  * @param email 
	  * 
	  * @return boolean value whether it is valid or not
	  * 
	  * 
	  */
	 public boolean isValidEmail(String email)
	 {
	     boolean isValidEmail = false;

	     String emailExpression = "^(?!.*\\.{2})[A-Z0-9a-z._%+-]+@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
	     CharSequence inputStr = email;

	     Pattern pattern = Pattern.compile(emailExpression, Pattern.CASE_INSENSITIVE);
	     Matcher matcher = pattern.matcher(inputStr);
	     if (matcher.matches())
	     {
	         isValidEmail = true;
	     }
	     return isValidEmail;
	 }
//	@TargetApi(Build.VERSION_CODES.M)
//	@RequiresApi(api = Build.VERSION_CODES.M)
//	private boolean checkAndRequestPermissions() {
//
//		int readContactsPermission = ContextCompat.checkSelfPermission(this,
//
//
//				Manifest.permission.READ_CONTACTS);
//
//
//		List<String> listPermissionsNeeded = new ArrayList<>();
//		if (readContactsPermission != PackageManager.PERMISSION_GRANTED) {
//			listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
//		}
//
//		if (!listPermissionsNeeded.isEmpty()) {
//
//			ActivityCompat.requestPermissions(this,
//
//
//					listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
//			return false;
//		}
//
//
//		return true;
//	}
	public boolean checkContactPermission()
	{
		int permissiontakeCamera = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_CONTACTS);
		int[] perm = {permissiontakeCamera};
		String[] stringPerm = {Manifest.permission.READ_CONTACTS};
		ActivityCompat.requestPermissions(this, stringPerm, 1);
		return true;

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (permissions.length == 0) {
			return;
		}
		boolean allPermissionsGranted = true;
		if (grantResults.length > 0) {
			for (int grantResult : grantResults) {
				if (grantResult != PackageManager.PERMISSION_GRANTED) {
					allPermissionsGranted = false;
					break;
				}
			}
		}
		if (!allPermissionsGranted) {
			boolean somePermissionsForeverDenied = false;
			for (String permission : permissions) {
				if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
					//denied
					Log.e("denied", permission);
				} else {
					if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
						//allowed
						Log.e("allowed", permission);
					} else {
						//set to never ask again
						Log.e("set to never ask again", permission);
						somePermissionsForeverDenied = true;
					}
				}
			}
			if (somePermissionsForeverDenied) {
				final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
				alertDialogBuilder.setTitle("Permissions Required")
						.setMessage("To excute this action, tap SETTINGS go to App info> Permissions, then allow the following permission and try again\n\nPermission(Contacts)")
						.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
										Uri.fromParts("package", getPackageName(), null));
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						})
						.setCancelable(false)
						.create()
						.show();
			}
		} else {
			pref=getSharedPreferences("Conpermissions",MODE_PRIVATE);
			SharedPreferences.Editor editor = getSharedPreferences("Conpermissions", MODE_PRIVATE).edit();
			editor.putBoolean("Conperm",true);
			editor.commit();
			isContactPermission = true;
		}
	}

}
