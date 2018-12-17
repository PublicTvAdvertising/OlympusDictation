package com.olympus.dmmobile.settings;



import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.olympus.dmmobile.DMActivity;
import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.PartialRegexInputFilter;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.webservice.Base64_Encoding;
import com.olympus.dmmobile.webservice.ResultXmlParser;
import com.olympus.dmmobile.webservice.Settingsparser;
import com.olympus.dmmobile.webservice.WebserviceHandler;
/**
 * Class used to configure server settings and handle server operations , such as Activation and update settings.
 * 
 * @version 1.0.1
 *
 */
public class ServerOptionsActivity extends FragmentActivity {
	
	private ExceptionReporter mReporter;			//Error Logger
    private TextView mTextViewURL;
	private EditText mEditServerUrl;
	private EditText mEditUser;
	private EditText mEditPassword;
	private EditText mEditEmail;
	private EditText mEditAuthor;
	private Button mBtnCheckConn;
	private Button mBtnRecipient;


	private String mMail;
	//private String mActvation;
	private String mResultcode;
	private AlertDialog.Builder mAlertDialog;
	private WebserviceHandler mWebserviceHandler;
	private ResultXmlParser mResultXmlParser=null;
	private String mActivationResponse=null;
	private String mGetSettingsResponse;
	private ProgressDialog mProgressDialog=null;
	private String mBasevalue;
	private Settingsparser mSettingParser;
	private List<Settingsparser.WorkTypeListObjects> mWorktypeobject;
	private Base64_Encoding mBaseEncoding;
	private String Url;
	private SharedPreferences.Editor mEditor ;
	public static final String PREFS_NAME = "Config";
	private String mWorktype=null;
	private String mSavedUrl;
	private String mUsername;
	private String mPassword;
	private String mEmail;
	private String mAudioEncrypt;
	private String mAudioPassword;
	private String mAudioFormat;
	private String mAudioDelivery;
	private String mWorktypeListname;
	private String mUUID;
	private String mGetuuid;
	private SharedPreferences sharedPreferences;
	private String mAuthor;
	private Locale mLocale;
	private DMApplication dmApplication=null;
	private int flag=0;
	private String mgetUrl,mgetUser,mgetPass,mgetMail;
	private Intent mBaseIntent=null;
    private boolean enableDeveloperOptions;
    private final String DEFAULT_URL = "https://www.dictation-portal.com";
    private String SERVER_URL = "";
    private final String RegexCapitalLetter = "^[A-Z0-9_]*$";

    public static String ENABLE_DEVELOPER_OPTIONS = "developerMode";
    private final int FORCED_ACTIVATION = 1;
    private final int NOT_FORCED = 0;
    private static final int UPDATE_RECIPIENTS = 1003;
	/** Called when the activity is first created. */
    boolean didAlreadySelectedRecipientDeleted = false;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mReporter = ExceptionReporter.register(this);
	    super.onCreate(savedInstanceState);
//		if (isMobileDataEnabled()) {
//			if(hasActiveInternetConnection(this))
//			{
//				DMApplication.setONLINE(true);
//			}
//
//		}

		if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        enableDeveloperOptions = getIntent().getBooleanExtra(ENABLE_DEVELOPER_OPTIONS,false);
	    setTitle(getResources().getString(R.string.sendoption_server));
	    dmApplication=(DMApplication)getApplication();
	    setContentView(R.layout.server_option_settings);

	    setCurrentLanguage(dmApplication.getCurrentLanguage());   
		dmApplication.setContext(this);
	    getUUID();
	    mWebserviceHandler=new WebserviceHandler();
	    mBaseEncoding=new Base64_Encoding();
        mTextViewURL = (TextView)findViewById(R.id.text_url);
	    mEditServerUrl = (EditText)findViewById(R.id.edit_server_url);
	    mEditUser = (EditText)findViewById(R.id.edit_server_user);
	    mEditPassword = (EditText)findViewById(R.id.edit_server_password);
	    mEditEmail = (EditText)findViewById(R.id.edit_email);
	    mEditAuthor = (EditText)findViewById(R.id.edit_author_id);
	    mBtnCheckConn = (Button)findViewById(R.id.button_check_server);
	    mBtnRecipient = (Button)findViewById(R.id.recipient);
	    
	    mEditAuthor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16), new InputFilter.AllCaps(),
                new PartialRegexInputFilter(RegexCapitalLetter)});

        if(!enableDeveloperOptions){
            mEditServerUrl.setVisibility(View.GONE);
            mTextViewURL.setVisibility(View.GONE);
        }
	    setInitialValues();
	    setRecipientValues();
	    mBtnRecipient.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent recipientIntent = new Intent(ServerOptionsActivity.this,RecipientActivity.class);
				startActivityForResult(recipientIntent,UPDATE_RECIPIENTS);
			}
		});
	    
	    mBtnCheckConn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                // modified for ver 1.1.0
                if(!enableDeveloperOptions){
                    SERVER_URL = DEFAULT_URL;
                }else{
                    SERVER_URL = mEditServerUrl.getText().toString();
                }
				     
				   if(mEditUser.getText().toString().trim().length()==0||mEditPassword.getText().toString().length()==0||mEditEmail.getText().toString().trim().length()==0||SERVER_URL.equalsIgnoreCase("http://")||(SERVER_URL.trim().length()<=0)||(!Patterns.WEB_URL.matcher(SERVER_URL.toLowerCase().trim()).matches()) ){
				    	 showMessageAlert1(getResources().getString(R.string.Settings_Error_Correct),getResources().getString(R.string.Settings_Error));
				     }
				   else if(!isValidEmail(mEditEmail.getText().toString().trim())){
					   showMessageAlert1(getResources().getString(R.string.Settings_Error_Correct),getResources().getString(R.string.Settings_Error));
				   }else{
						mMail=mEditEmail.getText().toString().trim();
						mBasevalue=mBaseEncoding.base64(mUUID+":"+mEditEmail.getText().toString().trim());

					   try {
						   Thread.sleep(50);
					   } catch (InterruptedException e) {
						   e.printStackTrace();
					   }
					   if(DMApplication.isONLINE()){
			    	activateThisDevice(NOT_FORCED);
					flag=0;
			    }
			    else {
			        flag=1;
			        showMessageAlertNonetwork(getResources().getString(R.string.Dictate_Network_Notavailable),getResources().getString(R.string.Alert));
			    }
					}
			}
		});
	}
	private void setRecipientValues() {
		String selectedRecipientName = readSelectedRecipientNameFromPreference();
		String selectedRecipientEmail = readSelectedRecipientEmailFromPreference();
		String selectedRecipient = "";
		//System.out.println("## selectedRecipientName - "+selectedRecipientName);
		if(selectedRecipientName.equalsIgnoreCase("")){
			selectedRecipientName = selectedRecipientEmail;
		}
		
		if(selectedRecipientName.equalsIgnoreCase("Use Portal Settings (Default)") || selectedRecipientName.equalsIgnoreCase("Použít nastavení portálu (výchozí)") 
				|| selectedRecipientName.equalsIgnoreCase("Portaleinstellungen verwenden (Standardeinstellung)") || selectedRecipientName.equalsIgnoreCase("Usar ajustes de portal (por defecto)")
				|| selectedRecipientName.equalsIgnoreCase("Utiliser les paramètres du portail (par défaut)") || selectedRecipientName.equalsIgnoreCase("Använd portalinställningar (standard)")){
			selectedRecipient = getResources().getString(R.string.Follow_Server_Settings);
			//System.out.println("Selected recipient - "+selectedRecipient);
			saveSelectedRecipientToPreference(selectedRecipient,"");
		}else{
			selectedRecipient = selectedRecipientEmail;
		}
		
		setSelectedRecipient(selectedRecipient);
		
	}
	/**
	 * Set the intial values from preference to edittext in server activity
	 * 
	 * 
	 * 
	 */
	public void setInitialValues(){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mSavedUrl=pref.getString(getResources().getString(R.string.server_url_key), "https://www.dictation-portal.com/");
       // Log.e("DEBUG","INITIAL URL - "+SERVER_URL);
       // Toast.makeText(ServerOptionsActivity.this,"INITIAL "+SERVER_URL,Toast.LENGTH_SHORT).show();
		mgetUser=pref.getString(getResources().getString(R.string.login_user_key), "");
		mgetPass=pref.getString(getResources().getString(R.string.login_password_key), "");
		mgetMail=pref.getString(getResources().getString(R.string.email_key), "");
		mAuthor = pref.getString(getResources().getString(R.string.author_key),"" );
		mEditServerUrl.setText(mSavedUrl);
		mEditUser.setText(mgetUser);
		mEditPassword.setText(mgetPass);
		mEditEmail.setText(mgetMail);
		mEditAuthor.setText(mAuthor);
		
	}
	/**
	 * asynchronous class used to activate the server
	 */
	public class WebServiceActivation extends AsyncTask<Void, Void, Void>{
		int forced;
		String author;
		String recipientEmail;
		String recipientName;
		//String[] emailID;
		//String[] contactList;
		boolean followServerSettings;
		Rfc822Token[] tokens = null;
		
		public WebServiceActivation(int forced,String author,String name,String email,boolean followServerSettings) {
			 this.forced = forced;
			 this.author = author;
			 //this.recipientName = name;
			 this.recipientEmail = email;
			 this.followServerSettings = followServerSettings;
			// System.out.println("activation - recipientEmail - "+recipientEmail);
			// System.out.println("activation - follow server settings - "+followServerSettings);
		}
    	@Override
    	protected void onPreExecute () {
    		mProgressDialog = new ProgressDialog(ServerOptionsActivity.this);
    		mProgressDialog.setCancelable(false);
    		mProgressDialog.setMessage(getResources()
					.getString(R.string.Dictate_Loading));
    		mProgressDialog.show();
        	super.onPreExecute();
        	
        	if(!enableDeveloperOptions){
                SERVER_URL = DEFAULT_URL;
            }else{
                SERVER_URL = mEditServerUrl.getText().toString();
            }
        	
        	if (!TextUtils.isEmpty(recipientEmail)) {
				tokens = Rfc822Tokenizer.tokenize(recipientEmail);
				
			}
    	}
		@Override
		protected Void doInBackground(Void... params) {
			StringBuilder request = new StringBuilder();
			
			String requestHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    				+"<ils-request appversion=\""+dmApplication.getIlsRequestAppVersion()+"\">"
    				+"<auth>"
    				+"<uuid>"+mUUID+"</uuid>"
    				+"<email><![CDATA["+mEditEmail.getText().toString()+"]]></email>"
    		        +"</auth>" 
    		        +"<customer>"
    		        +"<id><![CDATA["+mEditUser.getText().toString()+"]]></id>" 
    		        +"<pwd><![CDATA["+mEditPassword.getText().toString()+"]]></pwd>"
    		        +"</customer>"
    		        +"<smartphone>" 
    		        +"<model><![CDATA["+Build.MANUFACTURER+" "+Build.MODEL+"]]></model>"
    		        +"<osversion><![CDATA["+Build.VERSION.RELEASE+"]]></osversion>"
    		        +"<author>"+author+"</author>"			
    		        +"<recipients>";
			
			
			String recipientNode = "";
			
			//***********************
			if (tokens !=null && tokens.length > 0) {
				for (Rfc822Token token : tokens) {
					/*System.out.println("name : " + token.getName());
					System.out.println("value : " + token.getAddress());*/
					if(token.getName() != null && !token.getName().equalsIgnoreCase("")){
						recipientNode +=  "<address>"
	        			        +"<email><![CDATA["+token.getAddress()+"]]></email>"
	        			        +"<name><![CDATA["+token.getName()+"]]></name>"
	        			        +"</address>";
	    			}else{
	    				recipientNode += "<address>" + "<email><![CDATA[" + token.getAddress()
								+ "]]></email>" + "</address>";
	    			}
				}
			}
			
			
			/*for(String contact:contactList){
    			String[] contactDetails = contact.split(",");
    			
    			if(contactDetails.length > 1){
    				recipientNode +=  "<address>"
        			        +"<email><![CDATA["+contactDetails[1]+"]]></email>"
        			        +"<name>"+contactDetails[0]+"</name>"
        			        +"</address>";
    			}else{
    				recipientNode += "<address>" + "<email><![CDATA[" + contactDetails[0]
							+ "]]></email>" + "</address>";
    			}
    		}*/
			
			
			String requestFooter = "</recipients>"
                    +"<forced>"+forced+"</forced>"
    		        +"</smartphone>" 
    		        +"</ils-request>";
			
			
			request.append(requestHeader);
			if(!followServerSettings){
				request.append(recipientNode);
				request.append(requestFooter);
			}else{
				request.append(requestFooter);
			}
			Url=mEditServerUrl.getText().toString().trim();    // replace Url with SERVER_URL
			mActivationResponse = mWebserviceHandler.service_Activation(SERVER_URL+"/smartphone", request.toString());
			if(mActivationResponse!=null&&!mActivationResponse.equalsIgnoreCase("TimeOut")) {	
				mResultXmlParser=new ResultXmlParser(mActivationResponse);
		    	mResultcode=mResultXmlParser.parse(getLanguage());
			 }          
			return null;
		}
		@Override
	    protected void onPostExecute(Void result) {
			 //System.out.println("Result code: "+mResultcode);
			 //System.out.println("Resp : "+mActivationResponse);
			if(mActivationResponse==null) {
				mProgressDialog.dismiss();
				if(DMApplication.isONLINE()){
					showMessageAlert1(getResources().getString(R.string.Ils_Result_Cannot_Connect_Server),getResources().getString(R.string.Settings_Error));
				}else{
					showMessageAlertNonetwork(getResources().getString(R.string.Dictate_Network_Notavailable),getResources().getString(R.string.Alert));
				}
				
			 }
			 else if(mActivationResponse.equalsIgnoreCase("TimeOut")){
				 mProgressDialog.dismiss();
			 }
			 else {
				
			   String Message=dmApplication.validateResponse(mResultcode);
			   String Alert=dmApplication.validateErrorMessage(mResultcode);
   		       if(Message.equalsIgnoreCase(getResources().getString(R.string.Settings_Success))) {
   		    	   	setRecipientForceUpdate(false);
   			        new WebServicegetSettings().execute();
   			         
   		        }
   		      else {
   		    	  if(mProgressDialog.isShowing())
   		    		mProgressDialog.dismiss();
   		    	  if(mResultcode.equalsIgnoreCase("5002")){
   		    		  showMessageAlert1(mResultXmlParser.getMessage(), Message);
   		    	  }else if(mResultcode.equalsIgnoreCase("4012")){
   		    		  //show alert and activate the device with force value as 1
   		    		  forceUserToActivate(Message, Alert);
   		    	  }
   		    	  else if(mResultcode.equalsIgnoreCase("4003")){
   		    		  //show alert and activate the device with force value as 1
   		    		 forceUserToActivate(Message, Alert);
 		    	  }
   		    	  else{
   		    		  showMessageAlert1(Alert,Message);
   		    	  }
   		    	  
   		      }
			 }
			
		} 
    }
	/**
	 * method used to perform ODP activation with the given server options
	 * @param force value used to set force activation or normal activation. 
	 */
	private void activateThisDevice(int force){
		
		String name = readSelectedRecipientNameFromPreference();
		String email = "";
		String recipientEmail = readSelectedRecipientEmailFromPreference();
		boolean followServerSettings = false;
		
		if(name.equalsIgnoreCase("")){
			name = recipientEmail;
		}
		if (name.equalsIgnoreCase("Use Portal Settings (Default)") || name.equalsIgnoreCase("Použít nastavení portálu (výchozí)") 
				|| name.equalsIgnoreCase("Portaleinstellungen verwenden (Standardeinstellung)") || name.equalsIgnoreCase("Usar ajustes de portal (por defecto)")
				|| name.equalsIgnoreCase("Utiliser les paramètres du portail (par défaut)") || name.equalsIgnoreCase("Använd portalinställningar (standard)")) {
			name = "";
			email = "";
			followServerSettings = true;
		}
		new WebServiceActivation(force,getAuthorName(),name,recipientEmail,followServerSettings).execute();
	}
	
	/**
	 * asynchronous class used to get settings from server
	 */
	private class WebServicegetSettings extends AsyncTask<Void, Void, Void>{	
    	@Override
    	protected void onPreExecute (){  	
        	super.onPreExecute();
    	}
		@Override
		protected Void doInBackground(Void... params) {
		     mGetSettingsResponse=mWebserviceHandler.service_Settings(SERVER_URL+"/smartphone/"+mUUID.trim()+"/"+"Settings", mBasevalue);
		     if(mGetSettingsResponse!=null&&!mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
		    	 mSettingParser=new Settingsparser(mGetSettingsResponse);
		    	 mSettingParser.parse(getLanguage());
		    }
		     return null;
		}
		@Override
	    protected void onPostExecute(Void result){
			//System.out.println("Get Settings response - "+mGetSettingsResponse);
			String Message;
			String Alert;
			if(mProgressDialog.isShowing())
				mProgressDialog.dismiss();
			if(mGetSettingsResponse==null) {
				showMessageAlert1(getResources().getString(R.string.Settings_Error_Correct),getResources().getString(R.string.Ils_Result_Fail_Activate));
			}
			else if(mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
				
			}
			else {			    
			    mResultcode=mSettingParser.getRootObjects().get(0).getResult_code();
			    if(mResultcode.equalsIgnoreCase("2000")){
			    	 Alert=dmApplication.validateErrorMessage(mResultcode);
			    	 Message=dmApplication.validateResponse(mResultcode);
			    }
			    else{
			    	if(mResultcode.equalsIgnoreCase("4000")||mResultcode.equalsIgnoreCase("4006")||
			    			mResultcode.equalsIgnoreCase("4007")||mResultcode.equalsIgnoreCase("4008")||
			    			mResultcode.equalsIgnoreCase("4009")||mResultcode.equalsIgnoreCase("5002")) {
			    		mBaseIntent = new Intent("com.olympus.dmmobile.action.Test");
			    		mBaseIntent.putExtra("isWantWholeToTimeOut",true);
						sendBroadcast(mBaseIntent);
						mBaseIntent=null;
			    	}
			         Message=dmApplication.validateResponse(mResultcode);
			         Alert=dmApplication.validateErrorMessage(mResultcode);
			    }
			   if(mResultcode.equalsIgnoreCase("2000")) {
					 mSavedUrl=mEditServerUrl.getText().toString();
		   			 mEmail=mEditEmail.getText().toString();
		   			 mUsername=mEditUser.getText().toString();
		   			 mPassword=mEditPassword.getText().toString();
		   			 mEditAuthor.setText(mSettingParser.getSettingsObjects().get(0).getAuthor());
		   			 resultStatus();
		   			 setConfiguration();
		   			 setDeviceConfig();
		   			 showMessageAlert1(getResources().getString(R.string.Settings_Server_Connection),getResources().getString(R.string.Settings_Success));
		      }
		      else {
		    	  if(mResultcode.equalsIgnoreCase("5002"))
		    		  Alert=mSettingParser.getMessage();  
		    	  showMessageAlert1(Alert, Message);
		      }
				
			}
		}
	    }
	
	
	/**
	 * class used to update recipients to ODP in the background
	 * @version 1.1.0
	 */
	public class UpdateRecipientTask extends AsyncTask<Void, Void, Void>{
		
		String recipientEmail;
		String recipientName;
		boolean followServerSettings;
		String request;
		String odpResponse;
		ResultXmlParser odpResponseParser;
		Rfc822Token[] tokens = null;
		
		/**
		 * constructor to initialize the values
		 * @param name name of the recipient
		 * @param email email of the recipients
		 * @param followServerSettings value set to true if portal settings are used as recipients 
		 */
		public UpdateRecipientTask(String name,String email,boolean followServerSettings) {
			 this.recipientName = name;
			 this.recipientEmail = email;
			 this.followServerSettings = followServerSettings;
		}
    	@Override
    	protected void onPreExecute () {
    		mProgressDialog = new ProgressDialog(ServerOptionsActivity.this);
    		mProgressDialog.setCancelable(false);
    		mProgressDialog.setMessage(getResources()
					.getString(R.string.Dictate_Loading));
    		mProgressDialog.show();
    		
    		// set URL for developer/user mode.
    		if(!enableDeveloperOptions){
                SERVER_URL = DEFAULT_URL;
            }else{
                SERVER_URL = mEditServerUrl.getText().toString();
            }
    		
    		// tokenize the recipient string
    		if (!TextUtils.isEmpty(recipientEmail)) {
				tokens = Rfc822Tokenizer.tokenize(recipientEmail);
			}
    		
    		super.onPreExecute();
    	}
		@Override
		protected Void doInBackground(Void... params) {
			// create the request for update recipients
			StringBuilder request = new StringBuilder();
			String requestHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+"<ils-request appversion=\""+dmApplication.getIlsRequestAppVersion()+"\">"
				    +"<recipients>";
			
			String recipientNodeWithFooter = "";
			
			// create address tag with recipient name and email
			if (tokens != null && tokens.length > 0) {
				for (Rfc822Token token : tokens) {
					
					if(token.getName() != null && !token.getName().equalsIgnoreCase("")){
	    				recipientNodeWithFooter +=  "<address>"
	        			        +"<email><![CDATA["+token.getAddress()+"]]></email>"
	        			        +"<name><![CDATA["+token.getName()+"]]></name>"
	        			        +"</address>";
	    			}else{
	    				recipientNodeWithFooter += "<address>" + "<email><![CDATA[" + token.getAddress()
								+ "]]></email>" + "</address>";
	    			}
				}
			}
			String requestFooter = "</recipients>"
					+"</ils-request>";
			
			mBasevalue = mBaseEncoding.base64(mUUID+":"+mEditEmail.getText().toString().trim());
			request.append(requestHeader);
			if(!followServerSettings){
				request.append(recipientNodeWithFooter);
				request.append(requestFooter);
			}else{
				// discard the address tag if the portal settings is chosen as recipients
				request.append(requestFooter);
			}
			 odpResponse = mWebserviceHandler.updateRecipientsToODP(SERVER_URL+"/smartphone/"+mUUID.trim()+"/"+"recipients", request.toString(),mBasevalue);
			 if(odpResponse!=null&&!odpResponse.equalsIgnoreCase("TimeOut")) {	
				 odpResponseParser=new ResultXmlParser(odpResponse);
		    	 try {
					mResultcode=odpResponseParser.parse(getLanguage());
				} catch (Exception e) {
					mResultcode = null;
				}
			 }          
			return null;
		}
		@Override
	    protected void onPostExecute(Void result) {
	    
			if(odpResponse==null || mResultcode == null) {
				mProgressDialog.dismiss();
				if(!DMApplication.isONLINE()){
					showMessageAlertNonetwork(getResources().getString(R.string.Dictate_Network_Notavailable),getResources().getString(R.string.Alert));
					if(didAlreadySelectedRecipientDeleted){
						setRecipientValues();
					}
					
				}else{
					showMessageAlert1(getResources().getString(R.string.Ils_Result_Cannot_Connect_Server),getResources().getString(R.string.Settings_Error));
				}
			 }
			 else if(odpResponse.equalsIgnoreCase("TimeOut")){
				 mProgressDialog.dismiss();
			 }
			 else {
			   String Message=dmApplication.validateResponse(mResultcode);
			   String Alert=dmApplication.validateErrorMessage(mResultcode);
   		       if(Message.equalsIgnoreCase(getResources().getString(R.string.Settings_Success))) {
   		    	if(mProgressDialog.isShowing())
   		    		mProgressDialog.dismiss();
   		    	   showMessageAlert1(getResources().getString(R.string.update_recipient_success_message),Message);
   		    	   // update the force update status in shared preference 
   		    	   setRecipientForceUpdate(false);
   		         }
   		      else {
   		    	  if(mProgressDialog.isShowing())
   		    		mProgressDialog.dismiss();
   		    	  if(mResultcode.equalsIgnoreCase("5002")){
   		    		  showMessageAlert1(mResultXmlParser.getMessage(), Message);
   		    	  }
   		    	  else{
   		    		  showMessageAlert1(Alert,Message);
   		    	  }
   		    	  
   		      }
			 }
			
		} 
    }
	
	/**
	 * method to get author name entered in the author edit text
	 * @return author name
	 */
	private String getAuthorName(){
		return mEditAuthor.getText().toString().trim();
	}
	
	/**
	 * method to show alert and perform force activation with ODP.
	 * @param title	title of the alert dialog
	 * @param message message to show in alert dialog
	 */
	public void forceUserToActivate(String title,String message){
		mAlertDialog= new AlertDialog.Builder(ServerOptionsActivity.this);
		mAlertDialog.setTitle(title);
		mAlertDialog.setMessage(message);
		mAlertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Yes), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,int which){
				activateThisDevice(FORCED_ACTIVATION);
			}
		});
		mAlertDialog.setNegativeButton(R.string.Dictate_Alert_No, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		mAlertDialog.show(); 
	}
   /**
    * To show alert
    * 
    * @param Message to display in dialog
    * 
    * @param Alert title of the dialog
    * 
    * 
    */
	public void showMessageAlert1(String Message,String Alert){
		mAlertDialog= new AlertDialog.Builder(ServerOptionsActivity.this);
		mAlertDialog.setTitle(Alert);
		mAlertDialog.setMessage(Message);
		mAlertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,int which){
				mEditEmail.clearFocus();
				mEditPassword.clearFocus();
				mEditServerUrl.clearFocus();
				mEditUser.clearFocus();
			}
		});
		mAlertDialog.show(); 
	}
	/**
	 * To show alert when there is no network
	 * 
	 * @param Message display in dialog
	 * 
	 * @param Alert title of the dialog
	 * 
	 * 
	 */
	public void showMessageAlertNonetwork(String Message,String Alert){
		mAlertDialog= new AlertDialog.Builder(ServerOptionsActivity.this);
		mAlertDialog.setTitle(Alert);
		mAlertDialog.setMessage(Message);
		mAlertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,int which){
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ServerOptionsActivity.this);
				SERVER_URL=pref.getString(getResources().getString(R.string.server_url_key), mEditServerUrl.getText().toString());
				mgetUser=pref.getString(getResources().getString(R.string.login_user_key), mEditUser.getText().toString());
				mgetPass=pref.getString(getResources().getString(R.string.login_password_key), mEditPassword.getText().toString());
				mgetMail=pref.getString(getResources().getString(R.string.email_key), mEditEmail.getText().toString());
				mEditServerUrl.setText(SERVER_URL);
				mEditUser.setText(mgetUser);
				mEditPassword.setText(mgetPass);
				mEditEmail.setText(mgetMail);
				mEditEmail.clearFocus();
				mEditPassword.clearFocus();
				mEditServerUrl.clearFocus();
				mEditUser.clearFocus();
			}
		});
		mAlertDialog.show(); 
	}
	/**
	 * to set settings attribute in shared preference
	 * 
	 * 
	 * 
	 */
	public void setConfiguration() 	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mEditor = pref.edit();
		 mEditor.putString(getResources().getString(R.string.server_url_key), mSavedUrl);        // replaced mUrl with SERVER_URL
		 mEditor.putString(getResources().getString(R.string.login_user_key), mUsername);
		 mEditor.putString(getResources().getString(R.string.login_password_key), mPassword);
		 mEditor.putString(getResources().getString(R.string.email_key), mEmail);  
		 mEditor.putString(getResources().getString(R.string.Audio_delivery), mAudioDelivery);
		 mEditor.putString(getResources().getString(R.string.Audio_Encryption_key), mAudioEncrypt); 
		 mEditor.putString(getResources().getString(R.string.Audio_Format_key), mAudioFormat);
		 mEditor.putString(getResources().getString(R.string.Audio_Password_key), mAudioPassword);
		 mEditor.putString(getResources().getString(R.string.Worktype_Server_key), mWorktype);
		 mEditor.putString(getResources().getString(R.string.Worktype_List_name_Key), mWorktypeListname);
		 mEditor.putString(getResources().getString(R.string.author_key), mAuthor);		 
		 mEditor.commit();
		 pref = PreferenceManager.getDefaultSharedPreferences(this);
		 if(pref.getString(getResources().getString(R.string.author_key), "").equalsIgnoreCase(mAuthor)) 
			 dmApplication.getDatabaseHandler().updateDicationName(mSettingParser.getSettingsObjects().get(0).getAuthor().trim());
		 dmApplication.setUrl(mSavedUrl+"/smartphone");    
		 mWorktype=null;
		 mgetMail=mEmail;
		 mgetUser=mUsername;
		 mgetPass=mPassword;
	}
	/**
	 * to get settings attribute from parser
	 * 
	 * 
	 */
	public void resultStatus()	{	
		mWorktypeobject=mSettingParser.getWorkTypeListObjects();
	     for(int i=0;i<mWorktypeobject.size();i++) {
	    	 if(mWorktype==null)	{
	    		 mWorktype=mWorktypeobject.get(i).getWorktype();
	    	 }
	    	 else 	{  	 
	    	    mWorktype=mWorktype+":"+mWorktypeobject.get(i).getWorktype();
	    	 }
	    }
	     mAudioDelivery=mSettingParser.getSettingsObjects().get(0).getDelivery();
	     mAudioEncrypt=mSettingParser.getAudioObjects().get(0).getEncryption();
	     mAudioFormat=mSettingParser.getAudioObjects().get(0).getFormat();
	     mAudioPassword=mSettingParser.getAudioObjects().get(0).getPassword();
	     mWorktypeListname=mSettingParser.getSettingsObjects().get(0).getWorktypelist();
	     mAuthor=mSettingParser.getSettingsObjects().get(0).getAuthor();
	 
	}
	/**
	 * to set device configuration in shared preference
	 * 
	 * 
	 */
		
	public void setDeviceConfig() 	{
		 sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		 mEditor=sharedPreferences.edit();
		 mEditor.putString("Activation","Activated");
		 mEditor.putString("App-Version",dmApplication.getApplicationVersion() );
		 mEditor.putString("Manufacturer", Build.MANUFACTURER+" "+Build.MODEL);
		 mEditor.putString("Osversion", Build.VERSION.RELEASE);
		 mEditor.commit();
	}
	private boolean isDeviceActivated(){
		sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		if(sharedPreferences.getString("Activation", "").equalsIgnoreCase("Activated")){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * method to get uuid from shared prefernce
	 * 
	 * 
	 */
	public void getUUID() {
		 sharedPreferences =this.getSharedPreferences(PREFS_NAME, 0);
		 mUUID=sharedPreferences.getString("UUID", mGetuuid).trim();
		 
	}
	@Override
	public void onBackPressed() {
		    if((!mSavedUrl.equalsIgnoreCase(mEditServerUrl.getText().toString())
		    		||!mgetUser.equals(mEditUser.getText().toString())
		    		||!mgetPass.equals(mEditPassword.getText().toString())
		    		||!mgetMail.equals(mEditEmail.getText().toString())) 
		    		||!mAuthor.equals(mEditAuthor.getText().toString()) && flag==0){
		    	

		    	new MyDialogFragment(ServerOptionsActivity.this).show(getSupportFragmentManager(), "MyDialog");
		    }
		    else{
		    	moveTosettings();
		    }
		
	}
	

	/**
	 * Method to move from server activity to settings activity
	 * 
	 * 
	 */
	public void moveTosettings() {
		mBaseIntent=new Intent(this,SettingsActivity.class);
		startActivity(mBaseIntent);
		finish();
	}
	
	/**
	 * method to show alert dialog with given message
	 * @param Message message to show in alert dialog
	 */
	public void showMessageAlert(String Message){
		mAlertDialog= new AlertDialog.Builder(ServerOptionsActivity.this);
		mAlertDialog.setMessage(Message);
		mAlertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,int which){
				mEditEmail.clearFocus();
				mEditPassword.clearFocus();
				mEditServerUrl.clearFocus();
				mEditUser.clearFocus();
			}
		});
		mAlertDialog.show(); 
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
	 * @param lang is the locale get from shared preference
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
	 @Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
			 setCurrentLanguage(dmApplication.getCurrentLanguage());
		}
	 /**
	  * Method to check mail is valid or not
	  * 
	  * @param target is the target mail
	  * 
	  * @return boolean whether it is valid or not
	  * 
	  * 
	  */
	 public final static boolean isValidEmail(CharSequence target) {
			if (target == null) {
				return false;
			} else {
				return android.util.Patterns.EMAIL_ADDRESS.matcher(target)
						.matches();
			}
		}
	 /**
	  * Method to unbind all the views from layout
	  * 
	  * @param view
	  * 
	  * 
	  */
	 private void unbindDrawables(View view) {
	            if (view.getBackground() != null){
	                    view.getBackground().setCallback(null);
	            }
	            if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
	                    for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
	                            unbindDrawables(((ViewGroup) view).getChildAt(i));
	                    }
	                    ((ViewGroup) view).removeAllViews();
	            }
	    }
	 @Override
	    protected void onDestroy() {
	            super.onDestroy();

	            unbindDrawables(findViewById(R.id.edit_server_url));
	            unbindDrawables(findViewById(R.id.edit_server_user));
	            unbindDrawables(findViewById(R.id.edit_server_password));
	            unbindDrawables(findViewById(R.id.edit_email));
	            unbindDrawables(findViewById(R.id.button_check_server));
	            System.gc();
	    }
	 /**
	 * method to find the locale from sharedpreference
	 *
	 */
	 private String getLanguage(){
		 sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		 switch (Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.language_key),"1"))) {
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
	@SuppressLint("ValidFragment")
	public class MyDialogFragment extends DialogFragment{
		    Context mContext;
		    public MyDialogFragment(Context context) {
		        mContext = context;
		    }

		public MyDialogFragment() {
		}

		@Override
		    public Dialog onCreateDialog(Bundle savedInstanceState) {
		        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
		        alertDialogBuilder.setTitle(getResources().getString(R.string.Alert));
		        alertDialogBuilder.setMessage(getResources().getString(R.string.Settings_Change_Server));
		       
		        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.Settings_Activate), new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	dialog.dismiss();
		            	 if(mEditUser.getText().toString().trim().length()==0||mEditPassword.getText().toString().trim().length()==0||mEditEmail.getText().toString().trim().length()==0||mEditServerUrl.getText().toString().equalsIgnoreCase("http://")||(mEditServerUrl.getText().toString().trim().length()<=0)||(!Patterns.WEB_URL.matcher(mEditServerUrl.getText().toString().trim()).matches()) )
					    	 showMessageAlert1(getResources().getString(R.string.Settings_Error_Correct),getResources().getString(R.string.Settings_Error));
					   else if(!isValidEmail(mEditEmail.getText().toString().trim()))
						   showMessageAlert1(getResources().getString(R.string.Settings_Error_Correct),getResources().getString(R.string.Settings_Error));
						else{
							mMail=mEditEmail.getText().toString().trim();
							mBasevalue=mBaseEncoding.base64(mUUID+":"+mEditEmail.getText().toString().trim());
				    if(dmApplication.isONLINE()){
						activateThisDevice(NOT_FORCED);
						flag=0;
				    }
				    else{
				    	flag=1;
				    	showMessageAlertNonetwork(getResources().getString(R.string.Dictate_Network_Notavailable),getResources().getString(R.string.Alert));
				    }
						}
						mEditEmail.clearFocus();
						mEditPassword.clearFocus();
						mEditServerUrl.clearFocus();
						mEditUser.clearFocus();
						mEditAuthor.clearFocus();
					}
		            
		        });
		        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.Settings_Discard), new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	dialog.dismiss();
						moveTosettings();
		            }
		        });


		        return alertDialogBuilder.create();
		    }
		}
	 @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
		case UPDATE_RECIPIENTS:
			if(resultCode == RESULT_OK){
				// check if the recipients needs to be updated
				if(data.getBooleanExtra("update", false)){
						// if the currently selected recipient is edited / deleted, recipient must be updated
						if(data.getBooleanExtra("isEditAlreadySelectedItem", false)){
							setRecipientValues();
						}
						
						didAlreadySelectedRecipientDeleted = data.getBooleanExtra("didAlreadySelectedRecipientDeleted", false);
						String recipientEmail = data.getStringExtra(Recipient.SELECTED_RECIPIENT_EMAIL_TAG);
						
						boolean followServerSettings = false;
						// check if the selected recipient is portal settings in all languages
						if (recipientEmail.equalsIgnoreCase("Use Portal Settings (Default)") || recipientEmail.equalsIgnoreCase("Použít nastavení portálu (výchozí)") 
								|| recipientEmail.equalsIgnoreCase("Portaleinstellungen verwenden (Standardeinstellung)") || recipientEmail.equalsIgnoreCase("Usar ajustes de portal (por defecto)")
								|| recipientEmail.equalsIgnoreCase("Utiliser les paramètres du portail (par défaut)") || recipientEmail.equalsIgnoreCase("Använd portalinställningar (standard)")) {
							
							followServerSettings = true;
							
						}else{
							//setSelectedRecipient(recipientEmail);	
						}
						
						// update recipients to ODP only if the device is activated
						if(isDeviceActivated()){
							if(!hasServerConfigChanged()){
								new UpdateRecipientTask("", recipientEmail,followServerSettings).execute();
							}
						}else{
							onShowNotActivatedDialog();
						}
						saveSelectedRecipientToPreference("", recipientEmail);
						setSelectedRecipient(recipientEmail);
						
				}
			}
			
			break;

		default:
			break;
		}
	}
	 /**
	  * method to show error dialog with not activated message
	  */
	 private void onShowNotActivatedDialog() {
		 	AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
			mBuilder.setTitle(getString(R.string.Settings_Error));
			mBuilder.setMessage(getString(R.string.Flashair_Alert_Activate_Account));
			mBuilder.setPositiveButton(getString(R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			mBuilder.create().show();
		}
	 
	 /**
	  * method to set selected recipient to recipient field
	  * @param text
	  */
	private void setSelectedRecipient(String text) {
		mBtnRecipient.setText(text);
	}
	
	/**
	 * method which set value in shared preference to update the recipients forcefully
	 * @param status value to set the state
	 */
	private void setRecipientForceUpdate(boolean status){
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(ServerOptionsActivity.this);
		SharedPreferences.Editor mEditor = pref.edit();
		mEditor.putBoolean(Recipient.RECIPIENT_FORCE_UPDATE_TAG , status);
		mEditor.commit();
	}
	
	/**
	 * method to read selected recipient name from shared preference
	 * @return name of saved recipient
	 */
	private String readSelectedRecipientNameFromPreference() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String name = pref.getString(
				Recipient.SELECTED_RECIPIENT_NAME_TAG,
				getResources().getString(R.string.Follow_Server_Settings));
		return name;
	}
	
	/**
	 * method to read selected recipient emaill adress from shared preference
	 * @return returns email of selected recipient
	 */
	private String readSelectedRecipientEmailFromPreference() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String email = pref.getString(
				Recipient.SELECTED_RECIPIENT_EMAIL_TAG,
				"");
		return email;
	}
	
	/**
	 * method to check whether server configuration values has modified or not
	 * @return
	 */
	private boolean hasServerConfigChanged(){
		if((!mSavedUrl.equalsIgnoreCase(mEditServerUrl.getText().toString())
	    		||!mgetUser.equals(mEditUser.getText().toString())
	    		||!mgetPass.equals(mEditPassword.getText().toString())
	    		||!mgetMail.equals(mEditEmail.getText().toString()))){
				return true;
		}else{
			return false;
		}
	}
	
	/**
	 * method to validate email address
	 * @param email email to validate
	 * @return returns true if the given email is valid, else false
	 */
	public boolean isValidEmail(String email) {
		boolean isValidEmail = false;
		String emailExpression = "^(?!.*\\.{2})[A-Z0-9a-z._%+-]+@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
		//String emailExpression = "^(?!.*\\.{2})[A-Z0-9a-z._%+-]+@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
		//String emailExpression = "^[A-Z0-9a-z_+-]+([&%]?+\\.[A-Z0-9a-z_-]+)?@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$";
		CharSequence inputStr = email;

		Pattern pattern = Pattern.compile(emailExpression,
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(inputStr);
		if (matcher.matches()) {
			isValidEmail = true;
		}
		return isValidEmail;
	}
	
	/**
	 * method to save the selected recipient in shared preference
	 * @param name name of the selected recipient
	 * @param email email of the selected recipient
	 */
	private void saveSelectedRecipientToPreference(String name, String email){
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(ServerOptionsActivity.this);
		SharedPreferences.Editor mEditor = pref.edit();
		mEditor.putString(Recipient.SELECTED_RECIPIENT_NAME_TAG , name);
		mEditor.putString(Recipient.SELECTED_RECIPIENT_EMAIL_TAG , email);
		mEditor.commit();
	}
	public Boolean isMobileDataEnabled() {
		Object connectivityService = getSystemService(CONNECTIVITY_SERVICE);
		ConnectivityManager cm = (ConnectivityManager) connectivityService;

		try {
			Class<?> c = Class.forName(cm.getClass().getName());
			Method m = c.getDeclaredMethod("getMobileDataEnabled");
			m.setAccessible(true);
			return (Boolean) m.invoke(cm);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	boolean icConnected = false;
	public boolean hasActiveInternetConnection(Context context) {
	new CheckCon().execute();

		return icConnected;
	}

	public class CheckCon extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
				urlc.setRequestProperty("User-Agent", "Test");
				urlc.setRequestProperty("Connection", "close");
				urlc.setConnectTimeout(1500);
				urlc.connect();
				icConnected = urlc.getResponseCode() == 200;
				return (icConnected);
			} catch (IOException e) {

			}
			return icConnected;
		}


	}

}

