package com.olympus.dmmobile;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import java.io.File;
import com.olympus.dmmobile.webservice.Base64_Encoding;
import com.olympus.dmmobile.webservice.ResultXmlParser;
import com.olympus.dmmobile.webservice.WebserviceHandler;
/**
 * Service class to send error info into server
 * 
 * 
 *
 */
public class Errorservice extends Service {
	private SharedPreferences.Editor editor ;
	public static final String PREFS_NAME = "Config";
	private SharedPreferences sharedPreferences;
	private SharedPreferences pref;
	private boolean errorConfig;
	private static final String LOG_PATH = "/Android/data/com.olympus.dmmobile/files/Log/log.xml";
	private boolean errorStatus;
	private String errorRequest;
	private ResultXmlParser resultXmlParser;
	private Base64_Encoding baseEncoding;
	String errorStatusResponse;
	private String base64value;
	private String mFilename;
	private String mResultcode;
	private String prefUUID;
	private String mEmail;
	private String mUrl;
	private String mActivation;
	private String getActivation;
	private String mGetuuid;
	private WebserviceHandler mWebserviceHandler;
	private DMApplication dmApplication=null; 
	private String language;
	@Override
	public void onCreate() {
		dmApplication=(DMApplication)getApplication();
		dmApplication.setContext(this);
		super.onCreate();
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		getErrorConfiguration();
		return super.onStartCommand(intent, flags, startId);
	}
	/**
	 * method to get the attributes from prefernce
	 * 
	 * 
	 */
public void getErrorConfiguration()
{
sharedPreferences=this.getSharedPreferences(PREFS_NAME, 0);
errorConfig=sharedPreferences.getBoolean("ErrorStatus", errorStatus);
mActivation=sharedPreferences.getString("Activation", getActivation);
prefUUID=sharedPreferences.getString("UUID", mGetuuid);
pref=PreferenceManager.getDefaultSharedPreferences(this);
mUrl=pref.getString(getResources().getString(R.string.server_url_key),"");
mEmail=pref.getString(getResources().getString(R.string.email_key), "");

if(errorConfig==true&&mActivation.equalsIgnoreCase("Activated"))
{
	errorRequest="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			  + "<ils-request appversion=\""+dmApplication.getApplicationVersion()+"\">"
	          +"<smartphone>" 
              +"<model>"+Build.MANUFACTURER+" "+Build.MODEL+"</model>"
              +"<osversion>"+Build.VERSION.RELEASE+"</osversion>" 
              +"</smartphone>" 
              +"</ils-request>";
	          baseEncoding=new Base64_Encoding();
              mFilename=Environment.getExternalStorageDirectory().getAbsolutePath()+LOG_PATH;
              base64value=baseEncoding.base64(prefUUID+":"+mEmail);
              if(DMApplication.isONLINE())
              new WebServiceErrorinfo().execute();
              else
              stopSelf();
             
}
else
{
	stopSelf();
}
}
/**
 * Asynchronous class to send error info to server
 * 
 * 
 *
 */
private class WebServiceErrorinfo extends AsyncTask<Void, Void, Void>
{ 	
	@Override
	protected void onPreExecute ()
	{	              
    	super.onPreExecute();
	}

	@Override
	protected Void doInBackground(Void... params) 
	{		
		mWebserviceHandler=new WebserviceHandler();
		errorStatusResponse=mWebserviceHandler.service_Errorinfo(base64value, errorRequest, mFilename,mUrl+"/"+"smartphone"+"/"+prefUUID+"/"+"errlog");
		if(errorStatusResponse!=null) {
		resultXmlParser=new ResultXmlParser(errorStatusResponse);
    	mResultcode =resultXmlParser.parse(getLanguage());
		}
		return null;
	}
	@Override
    protected void onPostExecute(Void result)
    {
		if(errorStatusResponse!=null) {
			String Message=dmApplication.validateResponse(mResultcode);
			if((Message.equalsIgnoreCase(getResources().getString(R.string.Settings_Success))))
				 setErrorConfig();
		}
    }
    }
/**
 * Method to set the error send status in preference
 * 
 * 
 */
public void setErrorConfig()
{
     sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
     editor=sharedPreferences.edit();
     editor.putBoolean("ErrorStatus", false);
     editor.commit();
     File file=new File(mFilename);
     if(file.exists())
     { 
    	 file.delete();
     }  
     stopSelf();
}
/**
 * Method to get language from prefernce
 * 
 * @return current application language
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
}
