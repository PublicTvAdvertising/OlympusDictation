package com.olympus.dmmobile;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.recorder.DictateActivity;
import com.olympus.dmmobile.webservice.Base64_Encoding;
import com.olympus.dmmobile.webservice.ResultXmlParser;
import com.olympus.dmmobile.webservice.Settingsparser;
import com.olympus.dmmobile.webservice.WebserviceHandler;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;
/**
 * Class to set the splash screen when the application launches
 * 
 * @version 1.0.1
 */
public class SplashscreenActivity extends Activity {
	
	private boolean isNewerVersion = false;
	private final int CHILD_ACTIVITY_REQ_CODE = 1111;
	private SharedPreferences.Editor mEditor ;
	public static final String PREFS_NAME = "Config";
	private SharedPreferences sharedPreferences;
	private String mUrl;
	private ExceptionReporter mReporter;
	private WebserviceHandler mWebserviceHandler;
	private String base64value;
	private String mGetSettingsResponse;
	private ResultXmlParser resultXmlParser=null;
	private String mResultcode;
	private String mManufacture;
	private String mOsversion;
	private String mPrefmanufacture;
	private String mPrefosversion;
	private String mUpdaterequest;
	private String mUpdateresponse;
	private String mUUID;
	private String prefUUID;
	private String mGetuuid;
	private String mSettingsConfig;
	private String mActivation;
	private String mGeturl;
	private String mEmail;
	private String mGetemail;
	private SharedPreferences pref;
	private Settingsparser settingParser;
	private String mAudioEncrypt;
	private String mAudioPassword;
	private String mAudioFormat;
	private String mAudioDelivery;
	private String mWorktypeListname;
	private String mWorktype=null;
	private List<Settingsparser.WorkTypeListObjects> worktypeobject;
	private WebserviceHandler webserviceHandler;
	private Base64_Encoding baseEncoding;
	private String onLaunch;
	private final int SPLASH_TIME = 2 * 1000;// 3 seconds
	private Locale locale;
	private String mGetEmailWorktype;
	private CountDownTimer countDownTimerForSplash=null;
	private int mLanguageVal;
	private boolean isWebServiceCalled=false;
	private String mAuthor;
	private TextView uuidText;
	private DatabaseHandler mDbHandler;
	private String mCheckServermail;
	private String currentLanguage=null;
	private TextView mTxtAppver;
	private DMApplication dmApplication=null;
	//private ImageView mOlymCap;
	private Intent baseIntent=null;
	private DictationCard card=null;
	private WebServicegetSettings webServicegetSettings=null;
	private WebServiceUpdate webServiceUpdate=null;
	private final String FORCEQUIT_PREF_NAME = "force_quit";
	private Handler mHandler = null;
	private Intent mBaseIntent = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mReporter = ExceptionReporter.register(this);
		super.onCreate(savedInstanceState);
    	if(savedInstanceState != null) 
    		isNewerVersion = savedInstanceState.getBoolean("isNewerVersion");
    	else
    		isNewerVersion = false;
		if(!isNewerVersion) {
			if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
			else
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    	setContentView(R.layout.splashscreen);
			if (!isTaskRoot()
					&& getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
					&& getIntent().getAction() != null
					&& getIntent().getAction().equals(Intent.ACTION_MAIN)) {

				finish();
				return;
			}
			dmApplication=(DMApplication)getApplication();
	    	mTxtAppver = (TextView)findViewById(R.id.uuidappText);
	    	//mOlymCap=(ImageView)findViewById(R.id.olymcaption);
	    	dmApplication.setContext(this);
	    	dmApplication.setFlashAirState(false);



			mBaseIntent= new Intent(SplashscreenActivity.this, ConvertAndUploadService.class);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(mBaseIntent);
			} else {
				startService(mBaseIntent);
			}



	    	Locale.getDefault().getDisplayLanguage();
	    	try {
		    	mTxtAppver.setText("for Android Ver " + dmApplication.getApplicationVersion());
		    	isNewerVersion = isCurrentVertionIsKitKatAndAbove();
	    	}
	    	catch(Exception e){}
	    	uuidText=(TextView)findViewById(R.id.uuidText);
	    	uuidText.setVisibility(View.INVISIBLE);
	    	setLanguage();   
	    	mDbHandler = dmApplication.getDatabaseHandler();
	    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			webserviceHandler=new WebserviceHandler();
			pref= PreferenceManager.getDefaultSharedPreferences(this);
			mCheckServermail = pref.getString(getResources().getString(R.string.send_key), "");
			baseEncoding=new Base64_Encoding();	
	    	setUUID();
		    getSettingsAttribute();
			if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				 baseIntent= new Intent(SplashscreenActivity.this,NoSdcard.class);
				 startActivity(baseIntent);
				 finish();
			}
			else {
				mHandler = new Handler();
			    if(mCheckServermail.equalsIgnoreCase("1")||mCheckServermail.equalsIgnoreCase(""))
			    	onStartChildActivity();
			    else if(mCheckServermail.equalsIgnoreCase("2")){
			    	 pref =getSharedPreferences(PREFS_NAME, 0);
					 mSettingsConfig=pref.getString("Activation", mActivation);
				     if(!mSettingsConfig.equalsIgnoreCase("Not Activated")) {
				    	 countDownTimerForSplash = new SplashTimer(SPLASH_TIME, 50);
						 countDownTimerForSplash.start();
				     }
				     else
				    	 onStartChildActivity();
			     }
			}
		}
		else 
			finish();
	}
	/**
	 * Method to get the settings attribute from shared preference
	 */
	private void getSettingsAttribute() {
		pref =this.getSharedPreferences(PREFS_NAME, 0);
		if(pref.getString("UUID", mGetuuid)!=null)
			prefUUID=pref.getString("UUID", mGetuuid);
	    pref = PreferenceManager.getDefaultSharedPreferences(this);
	    if(pref.getString(getResources().getString(R.string.server_url_key), mGeturl)!=null) {
	    	mUrl=pref.getString(getResources().getString(R.string.server_url_key),mGeturl);
	    	mUrl = mUrl+"/smartphone";
	    	dmApplication.setUrl(mUrl);
	    }
	    if(pref.getString(getResources().getString(R.string.email_key), mGetemail)!=null)
	    	mEmail=pref.getString(getResources().getString(R.string.email_key), mGetemail);
	    if(pref.getString(getResources().getString(R.string.author_key),mAuthor)==null) {
	    	mEditor=pref.edit();
	    	mEditor.putString(getResources().getString(R.string.author_key),"AUTHOR");
	    	mEditor.commit();
	    }
	}
	/**
	 * Method to set uuid in the preference
	 * 
	 */
	private void setUUID() {
		mUUID=UUID.randomUUID().toString();
		StringBuilder sb = new StringBuilder(mUUID);
	    for (int index = 0; index < sb.length(); index++) {
	        char c = sb.charAt(index);
	        if (Character.isLowerCase(c)) 
	            sb.setCharAt(index, Character.toUpperCase(c));
	    }
		sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		prefUUID=sharedPreferences.getString("UUID", mGetuuid);
		if(prefUUID==null) {
			mEditor=sharedPreferences.edit(); 
			mEditor.putString("UUID",sb.toString());
			mEditor.commit();
		     pref = PreferenceManager.getDefaultSharedPreferences(this);
		     if(pref.getString(getResources().getString(R.string.Worktype_Email_Key),mGetEmailWorktype)==null) {
		    	 mEditor=pref.edit();
		    	 mEditor.putString(getResources().getString(R.string.Worktype_Email_Key),"FAX:LETTER:MEMO:REPORT");
		    	 mEditor.commit();
		     }
		}
		sb=null;
		mSettingsConfig=sharedPreferences.getString("Activation", mActivation);
		if(mSettingsConfig==null) {
			mEditor=sharedPreferences.edit();
			mEditor.putString("Activation" ,"Not Activated");
			mEditor.commit();
		}
	}     
	/**
	* Method to get the device configuration from preference
	* 
	*/
	private void getDeviceConfig() {
		// Log.e("Enter", "Function");
		sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		mPrefmanufacture=sharedPreferences.getString("Manufacturer", mManufacture);
		mPrefosversion=sharedPreferences.getString("Osversion", mOsversion);
		sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		mSettingsConfig=sharedPreferences.getString("Activation", mActivation);
		 //Log.e("mSett",mSettingsConfig);
		if(!mSettingsConfig.equalsIgnoreCase("Not Activated")||!mSettingsConfig.equalsIgnoreCase("")){
			 if(mPrefmanufacture!=null&&mPrefosversion!=null) {
				 //Log.e("ONLINE", String.valueOf(DMApplication.isONLINE()));
				 mUpdaterequest="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
						  + "<ils-request appversion=\""+dmApplication.getApplicationVersion()+"\">"
						  +"<update>"
				          +"<smartphone>" 
		                  +"<model>"+Build.MANUFACTURER+" "+Build.MODEL+"</model>"
		                  +"<osversion>"+Build.VERSION.RELEASE+"</osversion>" 
		                  +"</smartphone>" 
		                  +"</update>"
		                  +"</ils-request>";
				 base64value=baseEncoding.base64(prefUUID+":"+mEmail);
				if(DMApplication.isONLINE()) {
					webServiceUpdate=new WebServiceUpdate();
					webServiceUpdate.execute();
				}
			 }
		 }
	}	
	/**
	 * Method to get settings from server
	 */
	private void getSettings() {                      
		sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		mSettingsConfig=sharedPreferences.getString("Activation", mActivation);
		if(mSettingsConfig!=null&&!mSettingsConfig.equalsIgnoreCase("Not Activated")) {
			 base64value=baseEncoding.base64(prefUUID+":"+mEmail);
			 if(DMApplication.isONLINE()) {
				 webServicegetSettings = new WebServicegetSettings();
				 webServicegetSettings.execute();
			 }
		}
		
	}
	/**
	 * Asynchronous class used to update OS version or device Model
	 * 
	 */
	private class WebServiceUpdate extends AsyncTask<Void, Void, Boolean> { 	
    	@Override
		protected Boolean doInBackground(Void... params) {
    		try {
	    		mWebserviceHandler=new WebserviceHandler();
	    		mUpdateresponse = mWebserviceHandler.service_Update(mUrl + "/" + prefUUID, mUpdaterequest, base64value);
	    		if(mUpdateresponse!=null && !mUpdateresponse.trim().equalsIgnoreCase("") 
	    				&& !mUpdateresponse.equalsIgnoreCase("TimeOut")) {
	    			resultXmlParser = new ResultXmlParser(mUpdateresponse);
			    	mResultcode = resultXmlParser.parse(getLanguage());
			    	return true;
	    		}
			}
    		catch (Exception e) {}
    		return false;
		}
		@Override
	    protected void onPostExecute(Boolean isSuccess) {
			if(isSuccess) {
				if(!mUpdateresponse.trim().equalsIgnoreCase("")) {
					if((mResultcode.equalsIgnoreCase("2000"))) {
						 setDeviceConfig();
						 getSettings();
					}
					else {
						if(mResultcode.equalsIgnoreCase("4007")){
	                    	sharedPreferences =getSharedPreferences(PREFS_NAME, 0);
	                    	mEditor=sharedPreferences.edit();
	                    	mEditor.putString("Activation" ,"Not Activated");
	                    	mEditor.commit();
						}
						if(mResultcode.equalsIgnoreCase("4000")||mResultcode.equalsIgnoreCase("4006")||
                			   mResultcode.equalsIgnoreCase("4007")||mResultcode.equalsIgnoreCase("4008")
                			   ||mResultcode.equalsIgnoreCase("4009")||mResultcode.equalsIgnoreCase("5002")) {
							onStartNotActivatedActivity(null);
							onMoveWholeToTimeOut();
						}
					}
				}
				else
					getSettings();
			}
			else
				 getSettings();
	    }		
	}
	/**
	 * Asynchronous class to get settings from server
	 * 
	 */
	private class WebServicegetSettings extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			try {
			    mGetSettingsResponse = webserviceHandler.service_Settings(dmApplication.getUrl()+"/"+prefUUID+"/"+"Settings", base64value);
			    if(mGetSettingsResponse!=null && !mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
				    settingParser = new Settingsparser(mGetSettingsResponse);
				    settingParser.parse(getLanguage());
				    return true;
				}
			}
			catch (Exception e) {}
		    return false;
		}
		@Override
	    protected void onPostExecute(Boolean isSuccess){
			if(isSuccess) {
				 mResultcode = settingParser.getRootObjects().get(0).getResult_code();
				 if(mResultcode.equalsIgnoreCase("2000")) {
		   			 resultStatus();
		   			 setConfiguration();
		   		}
				else {
					 if(mResultcode.equalsIgnoreCase("4007")) {
					    sharedPreferences =getSharedPreferences(PREFS_NAME, 0);
					    mEditor=sharedPreferences.edit();
					    mEditor.putString("Activation" ,"Not Activated");
					    mEditor.commit();
					 }
					 if(mResultcode.equalsIgnoreCase("4000") || mResultcode.equalsIgnoreCase("4006")||
              			   mResultcode.equalsIgnoreCase("4007") || mResultcode.equalsIgnoreCase("4008")
              			   ||mResultcode.equalsIgnoreCase("4009") || mResultcode.equalsIgnoreCase("5002")) {
						 onStartNotActivatedActivity(settingParser.getMessage());
						 onMoveWholeToTimeOut();
					 }
				 }
			}
		}
	}
	/**
	 * Method to show 'Not Activated' dialog when Account is not activated/deactivated state.
	 * @param mMessage alert message
	 */
	private void onStartNotActivatedActivity(String mMessage) {
		if(!dmApplication.isTimeOutDialogOnFront()) {
			if(!(dmApplication.getContext() instanceof SplashscreenActivity )) {
				dmApplication.setTimeOutDialogOnFront(true);
				dmApplication.setWantToShowDialog(false);
				baseIntent = new Intent(dmApplication.getContext(), CustomLaunchDialog.class);
				baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				baseIntent.putExtra("Resultcode", mResultcode);
				if(mMessage != null)
					baseIntent.putExtra("Message", mMessage);
				dmApplication.getContext().startActivity(baseIntent);
				baseIntent=null;
			}
			else {
				dmApplication.setResultCode(mResultcode);
				dmApplication.setMessage(mMessage);
			}
		}
	}
	/**
	 * Method to send 'Critical Error' notification to the background service & move all Outbox dictation to 'Timeout' status.
	 */
	private void onMoveWholeToTimeOut() {
		baseIntent = new Intent("com.olympus.dmmobile.action.Test");
		baseIntent.putExtra("isWantWholeToTimeOut",true);
		sendBroadcast(baseIntent);
		baseIntent=null;
	}
	/**
	 * Method to set the values into preference
	 * 
	 */
	private void setConfiguration() 	{
		 pref = PreferenceManager.getDefaultSharedPreferences(this);
		 mEditor = pref.edit();
		 mEditor.putString(getResources().getString(R.string.Audio_delivery), mAudioDelivery);
		 mEditor.putString(getResources().getString(R.string.Audio_Encryption_key), mAudioEncrypt);
		 mEditor.putString(getResources().getString(R.string.Audio_Format_key), mAudioFormat);
		 mEditor.putString(getResources().getString(R.string.Audio_Password_key), mAudioPassword);
		 mEditor.putString(getResources().getString(R.string.Worktype_Server_key), mWorktype);
		 mEditor.putString(getResources().getString(R.string.Worktype_List_name_Key), mWorktypeListname);
		 mEditor.putString(getResources().getString(R.string.author_key), mAuthor);
		 mEditor.commit();
		 pref = PreferenceManager.getDefaultSharedPreferences(this);
		 if(pref.getString(getResources().getString(R.string.author_key), "").equalsIgnoreCase(mAuthor)) {
			 mDbHandler.updateDicationName(mAuthor);
		     if(dmApplication.getContext() instanceof DMActivity && dmApplication.getTabPos() == 0) 
		    	 ((DMActivity) dmApplication.getContext()).onUpdateFromSplash();
		     else if(dmApplication.getContext() instanceof DictateActivity)
		    	 ((DictateActivity) dmApplication.getContext()).onRefreshDictation();
		 }
	}
	/**
	 * Method to get the setting status from parser
	 * 
	 * 
	 */
	private void resultStatus()	{	
	     worktypeobject=settingParser.getWorkTypeListObjects();
	     for(int i=0;i<worktypeobject.size();i++) {
	    	 if(mWorktype==null)
	    		 mWorktype=worktypeobject.get(i).getWorktype();
	    	 else 
	    	    mWorktype=mWorktype+":"+worktypeobject.get(i).getWorktype();
	     }
	     mAudioDelivery=settingParser.getSettingsObjects().get(0).getDelivery();
	     mAudioEncrypt=settingParser.getAudioObjects().get(0).getEncryption();
	     mAudioFormat=settingParser.getAudioObjects().get(0).getFormat();
	     mAudioPassword=settingParser.getAudioObjects().get(0).getPassword();
	     mWorktypeListname=settingParser.getSettingsObjects().get(0).getWorktypelist();
	     mAuthor=settingParser.getSettingsObjects().get(0).getAuthor();
	}
	/**
	 * Method to set device configuration into preference
	 */
	private void setDeviceConfig() {
		 sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		 mEditor=sharedPreferences.edit();
		 mEditor.putString("App-Version",dmApplication.getApplicationVersion() );
		 mEditor.putString("Manufacturer", Build.MANUFACTURER+" "+Build.MODEL);
		 mEditor.putString("Osversion", Build.VERSION.RELEASE);
		 mEditor.commit();
	}
	/**
	 *Method to set animation for splash screen 
	 *
	 *
	 */
	private void onStartChildActivity() {
		try {
			mHandler.postDelayed(new Runnable() {
				public void run() {	
					onLaunchShow();
					overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
					if(!isNewerVersion)
						SplashscreenActivity.this.finish();
					else
						onPerformAfterSplah();
				}
			}, SPLASH_TIME);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * Method to start error service class 
	 */
	 private void Errorservice() {
		 baseIntent=new Intent(SplashscreenActivity.this, Errorservice.class);
//		 startService(baseIntent);
		 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			 startForegroundService(baseIntent);
		 } else {
			 startService(baseIntent);
		 }
	 }
	 /**
	  * Method to determine whether new dictation or recordings screen to show first
	  */
	 private void onLaunchShow() {
		 pref = PreferenceManager.getDefaultSharedPreferences(this);
		 Cursor cur = null;
		 File file = null;
		 if(pref.getBoolean(FORCEQUIT_PREF_NAME,  false)){
			cur =  mDbHandler.checkActiveDictationExists();
			if(cur != null) {
				DictationCard dCard = mDbHandler.getSelectedDictation(cur);
				file = new File(DMApplication.DEFAULT_DIR.getAbsolutePath()+DMApplication.DEFAULT_DICTATIONS_DIR+
						dCard.getSequenceNumber()+"/"+dCard.getDictFileName()+".wav");
				if(file.exists()) {
					dCard.setDuration((file.length()/32000)*1000);
					dCard.setRecEndDate(dmApplication.getDeviceTime());
					dCard.setStatus(DictationStatus.PENDING.getValue());
					mDbHandler.updateDurationAndStatus(dCard);
				}
				SharedPreferences.Editor editor = pref.edit();
				editor.putBoolean(FORCEQUIT_PREF_NAME, false);
				editor.commit();
				file = null;
				dCard = null;
				cur.close();
			}
		 }
		 else{
			 cur =  mDbHandler.getNewDictationExistingWithDuration();
			 if(cur.moveToFirst()) {
				 DictationCard dCard = mDbHandler.getSelectedDictation(cur);
				 dCard.setStatus(DictationStatus.PENDING.getValue());
				 mDbHandler.updateDictationStatus(dCard.getDictationId(), dCard.getStatus());
			 }
			 cur.close();
		 }
		 if(pref.getBoolean(DMApplication.EDIT_COPY_FORCE_QUIT,  false)) {
			 String path = pref.getString(DMApplication.EDIT_COPY_DESTINATION, "");
			 if(!path.equals("")) {
				 file = new File(path);
				 if(file.exists())
						file.delete();
				 SharedPreferences.Editor editor = pref.edit();
				 editor.putBoolean(DMApplication.EDIT_COPY_FORCE_QUIT, false);
				 editor.putString(DMApplication.EDIT_COPY_DESTINATION, "");
				 editor.commit();
				 file = null;
			 }
			
		 }
		 onLaunch=pref.getString(getResources().getString(R.string.onlaunch_show_key), "");
		 if(onLaunch.equalsIgnoreCase("") || onLaunch.equalsIgnoreCase("2")) 
			 baseIntent= new Intent(SplashscreenActivity.this, DMActivity.class);
		 else {
			 card = null;
			 cur= mDbHandler.checkActiveDictationExistsWithDuration();
			 baseIntent= new Intent(SplashscreenActivity.this, DictateActivity.class);
			 baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			 if(cur != null) {
				card = mDbHandler.getSelectedDictation(cur);
				if(card.getStatus()==DictationStatus.SENT.getValue()||card.getStatus()==DictationStatus.SENT_VIA_EMAIL.getValue())
					baseIntent.putExtra(DMApplication.START_MODE_TAG,DMApplication.MODE_REVIEW_RECORDING);
				else
					baseIntent.putExtra(DMApplication.START_MODE_TAG,DMApplication.MODE_EDIT_RECORDING);
				baseIntent.putExtra(DMApplication.DICTATION_ID, card.getDictationId());
				dmApplication.lastDictMailSent = false;
				cur.close();
				mDbHandler.closeDB();
			 }
			 else {
				 baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_NEW_RECORDING);
				 dmApplication.fromWhere = 3;
				 dmApplication.isExecuted = true;
			 }
		 }
		 baseIntent.putExtra("isFromSlash", true);
		 if(!isNewerVersion) {
			 startActivity(baseIntent);
			 finish();	
		 }
		 else 
			 startActivityForResult(baseIntent, CHILD_ACTIVITY_REQ_CODE);
		 card = null;
	 }
	 /**
	  * Method used to set the language for the current activity
	  * 
	  * @param lang is the locale get from shared preferences
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
	 }
	 /**
		 * Method to set the current OS language into preference on first launch
		 * 
		 * 
		 */
		 private void setLanguage() {
			 pref = PreferenceManager.getDefaultSharedPreferences(this);
			 currentLanguage=(pref.getString(getResources().getString(R.string.language_key), ""));
			 if(currentLanguage!=null&&!currentLanguage.equalsIgnoreCase(""))
				 mLanguageVal=Integer.parseInt(currentLanguage);
			 else {
				 if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(new Locale("en").getDisplayLanguage()))
					 mLanguageVal=1;
				 else if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(new Locale("de").getDisplayLanguage()))
					 mLanguageVal=2;
				 else if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(new Locale("fr").getDisplayLanguage()))
					 mLanguageVal=3;
				 else if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(new Locale("es").getDisplayLanguage()))
					 mLanguageVal=4;
				 else if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(new Locale("sv").getDisplayLanguage()))
					 mLanguageVal=5;
				 else if(Locale.getDefault().getDisplayLanguage().equalsIgnoreCase(new Locale("cs").getDisplayLanguage()))
					 mLanguageVal=6;
				 else
					mLanguageVal=1;
				 mEditor=pref.edit(); 
				 mEditor.putString(getResources().getString(R.string.language_key),String.valueOf(mLanguageVal));
			     mEditor.commit();
			 }
			 dmApplication.setCurrentLanguage(mLanguageVal);
			 if(mLanguageVal==1)
				setLocale("en");
			 else if(mLanguageVal==2)
				setLocale("de");
			 else if(mLanguageVal==3)
				setLocale("fr");
			 else if(mLanguageVal==4)
				setLocale("es");
			 else if(mLanguageVal==5)
				setLocale("sv");
			 else if(mLanguageVal==6)
				setLocale("cs");
		 }
		 /**
		  * Timer class used to call asynchronous class
		  * 
		  * 
		  *
		  */
	private class SplashTimer extends CountDownTimer {
		public SplashTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		@Override
		public void onFinish() {
			onLaunchShow();
			System.gc();
			if(!isNewerVersion)
				SplashscreenActivity.this.finish();
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
			countDownTimerForSplash.cancel();
			countDownTimerForSplash=null;
		}
		@Override
		public void onTick(long millisUntilFinished) {
			super.notifyAll();
			if(DMApplication.isONLINE() && !isWebServiceCalled) {
				 isWebServiceCalled=true;				 
				 getDeviceConfig();
				 //Errorservice();
			}
		}
	}

	/**
	 * Method to unbind all the views from layout
	 * 
	 * @param view widgets used in layout
	 * 
	 * 
	 */
	
	private void unbindDrawables(View view) {
		if (view.getBackground() != null)
            view.getBackground().setCallback(null);
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
        	for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }  
        view=null;
	}
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    if(!isNewerVersion)
	    	onPerformAfterSplah();
	}
	   /**
	    * Method to get language from preference
	    *                                  
	    * @return current language
	    * 
	    * 
	    */
	private String getLanguage(){
		 pref = PreferenceManager.getDefaultSharedPreferences(this);
		 switch (Integer.parseInt(pref.getString(getResources().getString(R.string.language_key),"1"))) {
			case 1:
				return "en";
			case 2:
				return "de";	
			case 3:
				return "fr";
			case 4:
				return "es";
			case 5:
				return "sv";
			case 6:
				return "cs";
			default:
				return "en";
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("isNewerVersion", isNewerVersion);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}
	/**
	 * Pick current system OS version.
	 * @return isNewversion
	 */
	private boolean isCurrentVertionIsKitKatAndAbove() {
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
	        return false;
	    else
	    	return true;
	}
	/**
	 * Unbind/release memory
	 */
	private void onPerformAfterSplah() {
		unbindDrawables(findViewById(R.id.uuidText));
	    unbindDrawables(findViewById(R.id.uuidappText));
	    //unbindDrawables(findViewById(R.id.olymcaption));
	    unbindDrawables(findViewById(R.id.relSplash));
	    System.gc();
	}
}
