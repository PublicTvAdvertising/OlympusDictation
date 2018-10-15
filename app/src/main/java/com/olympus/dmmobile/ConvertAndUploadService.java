package com.olympus.dmmobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;

import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.network.NetworkConnectivityListener.RetryUploadListener;
import com.olympus.dmmobile.recorder.DictateActivity;
import com.olympus.dmmobile.webservice.Base64_Encoding;
import com.olympus.dmmobile.webservice.OlyDMSSLSocketFactory;
import com.olympus.dmmobile.webservice.OlyDMTrustManager;
import com.olympus.dmmobile.webservice.WebserviceHandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * 
 * This class handles the process of DSS file conversion and uploading the dictations to the
 * ILS,which is executed in the background.
 *
 * @version 1.0.2
 */

public class  ConvertAndUploadService extends Service implements RetryUploadListener {
	
	private Intent mBaseIntent=null;
	private Thread mConvertThread = null;
	private Thread mUploadThread = null;
	private Thread mQueryThread = null;
	private Handler mHandler5Minute = null;
	private Handler mHandler30Seconds = null;
	private Cursor mConvertCursor = null;
	private Cursor mUploadCursor = null;
	private Cursor mQueryCursor = null;
	private File mFileUpload = null;
	private File mFileQuery = null;
	private File mFileDir = null;
	private File mFileConvert = null;
	private FileInputStream mFileInputStream = null;
	private DataOutputStream mOutputStream = null;
	private InputStream mInputStream = null;
	private HttpsURLConnection mHttpsURLConnection = null;
	private HttpURLConnection mHttpURLConnection = null;
	private HttpResponse mHttpResponse = null;
	private HttpClient mHttpClient = null;
	private HttpParams mHttpParameters = null;
	private HttpGet mHttpGet = null;
	private URI mURIQuerying = null;
	private URL mUploadAddress = null;
	private URLConnection mUrlConnection = null;
	private StringBuffer mStringBuffer = null;
	private BufferedInputStream mBufferedInputStream = null;
	private SimpleDateFormat mDateFormat = null;
	private Date mDate = null;
	private String[] mChildren = null;
	private final String mBoundary = "------------ILSBoundary8FD83EF0C2254A9B";
	private final String PREFS_NAME = "Config";
	private String mUploadResult = "";
	private String mDictationString = "";
	private String mBase64Value = "";
	private String mDictString = "";
	private String mJobData = "";
	private String mPrefUUID = null;
	private String mEmail = null;
	private String mGetEmail = null;
	private String mActivation = "";
	private String mGetuuid = null;
	private String mUrl = null;
	private String mGeturl = null;
	private String mQueryResult = null;
	private String mTempString1 = "";
	private String mTempString2 = "";
	private String mErrorMessage = null;
	private String mPathQuery = null;
	private String mPreviousUploadedTime = null;
	private final long mTimeLimitForHttpError = 300*1000;
	private final long mTimeLimitForWaitingToSend = 30*1000;
	private long mIdleTime = -1;
	private final int mTimeLimit = 20*1000;
	private final int mMaxBufferSize = 1024*1024;
	private int mConvertingGroupId = -1;
	private int mGroupId = 0;
	private int mBytesRead = 0;
	private int mBytesAvailable = 0;
	private int mBufferSize = 0;
	private int mTotalUploadSize = 0;
	private int mPositionUpload = 0;
	private int mPositionQuery = 0;
	private byte[] mBufferArray = null;
	private boolean isConvertExecuting = false;
	private boolean isUploadThreadExecuting = false;
	private boolean isQueryExecuting = false;
	private boolean isNotServerError = false;
	private boolean hasUploadCounting = false;
	private boolean hasQueryCounting = false;
	private boolean isQueryTimerStarted = false;
	private boolean isOutputStreaming = false;
	private boolean isInputStreaming = false;
	private boolean isCurrentUploadingIsGoesToTimeOut = false;
	private boolean isFileExists = false;
	private boolean isToUseFixedLength=false;
	private boolean hasIdleStateRealeased = false;
	private SharedPreferences mSharedPreferences = null;
	private SharedPreferences.Editor mEditor = null;
	//private SchemeRegistry mSchemeRegistry = null;
	//private ThreadSafeClientConnManager mSafeClientConnManager = null;
	//private TrustManager[] mTrustAllCerts = null;
	private SSLContext mSslContext = null;
	
	private DMApplication mDMApplication = null;
	private DatabaseHandler mDbHandlerConvert = null;
	private DatabaseHandler mDbHandlerUpload = null;
	private DatabaseHandler mDbHandlerQuerying = null;
	private UploadStatusChangeListener mStatusChangeListener = null;
	private Base64_Encoding mBaseEncoding = null;
	private DSSConverter mDSSConverter=null;
	private ArrayList<DictationCard> mGroupOfDictationCard = null;
	private ArrayList<FilesCard> mFilesCards = null;
	private DictationCard mUploadCard = null;
	private DictationCard mQueryCard = null;
	private DictationCard mConvertCard = null;
	private FilesCard mFilesCard = null;
	private DictationUploadFileXmlParser mUploadFileXmlParser = null;
	private DictationQueryXmlParser mQueryXmlParser = null;
	private List<DictationUploadFileXmlParser.AttributeObjects> mUploadingAttributeObjects = null;
	private List<DictationUploadFileXmlParser.JobDataObjects> mJobDataObjects = null;
	private List<DictationQueryXmlParser.AttributeObjects> mQueryAttributeObjects = null;

	@Override
	public void onCreate() {
		ExceptionReporter.register(this);
		super.onCreate();
		mDMApplication = (DMApplication)getApplication();
		mDbHandlerConvert = new DatabaseHandler(getApplicationContext());
		mDbHandlerUpload = new DatabaseHandler(getApplicationContext());//Get common database from base application
		mDbHandlerQuerying = new DatabaseHandler(getApplicationContext());
		mHandler5Minute = new Handler();
		mHandler30Seconds = new Handler();
		mDMApplication.setUploadServiceContext(this);
		android.support.v4.app.NotificationCompat.Builder builder;
		NotificationManager notifManager=null;
		if (notifManager == null) {
			notifManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_HIGH;
			NotificationChannel mChannel = notifManager.getNotificationChannel("1");
			if (mChannel == null) {
				mChannel = new NotificationChannel("1", "Service", importance);
				mChannel.enableVibration(true);
				mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
				notifManager.createNotificationChannel(mChannel);
			}
			builder = new android.support.v4.app.NotificationCompat.Builder(getApplicationContext(), "1");


			builder.setContentTitle("Back ground servrice")                            // required
					// required
					.setContentText("Service running..") // required
					.setDefaults(Notification.DEFAULT_ALL)
					.setAutoCancel(true)

					.setSmallIcon(R.drawable.ic_launcher)
					.setColor(getResources().getColor(R.color.black))


					.setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
			Notification notification = builder.build();
			startForeground(1, notification);

		}
		else {
			builder = new android.support.v4.app.NotificationCompat.Builder(getApplicationContext(), "1");

			builder.setContentTitle("Back ground servrice")                            // required
					.setSmallIcon(R.drawable.ic_launcher)   // required
					.setColor(getResources().getColor(R.color.black))
					.setContentText("Service running..") // required
					.setDefaults(Notification.DEFAULT_ALL)
					.setAutoCancel(true)


					.setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
					.setPriority(Notification.PRIORITY_HIGH);
			Notification notification = builder.build();
			startForeground(1, notification);

		}


		/*
		 * checks Android build version of device and set the streaming mode.
		 */
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
			isToUseFixedLength=true;
		
		
		/*
		 * Register BroadCastReceiver 'com.olympus.dmmobile.action.Test' to communicate between Service and other components.
		 */
		registerReceiver(mMessengerReceiver, new IntentFilter("com.olympus.dmmobile.action.Test"));
		try
		{
			mSharedPreferences = getSharedPreferences(PREFS_NAME, 0);
			/*
			 * To check the application's ODP account is activated or not.
			 */
			if(!mSharedPreferences.getString("Activation", mActivation).equalsIgnoreCase("Not Activated"))
			{
				getSettingsAttribute();
				mDbHandlerUpload.updateOnLaunch();
	  	  		onUpdateList();
			}
			else
				onMoveAllToTimeOut();
		}catch (Exception e) {}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mDMApplication.setUploadServiceContext(this);


		return START_STICKY;
	}
	
	/**
	 * Get all server settings configured from shared preferences for ILS Communication.
	 */
	private void getSettingsAttribute()
	{
		try {
			mSharedPreferences =getSharedPreferences(PREFS_NAME, 0);
			if(mSharedPreferences.getString("UUID", mGetuuid)!=null)
				mPrefUUID=mSharedPreferences.getString("UUID", mGetuuid);
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			if(mSharedPreferences.getString(getResources().getString(R.string.server_url_key), mGeturl)!=null)
			{
			    mUrl = mSharedPreferences.getString(getResources().getString(R.string.server_url_key),mGeturl);
			    mUrl = mUrl+"/smartphone";
			    mDMApplication.setUrl(mUrl);
			}
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		    if(mSharedPreferences.getString(getResources().getString(R.string.email_key), mGetEmail)!=null)
		    	mEmail=mSharedPreferences.getString(getResources().getString(R.string.email_key), mGetEmail);
		    mBaseEncoding=new Base64_Encoding();
			mBase64Value=mBaseEncoding.base64(mPrefUUID+":"+mEmail);
		}
		catch (Exception e) {}
	}
	
	/**
	 * Call the converter in sequential order, and perform all operations together.
	 * 
	 * </p>All the DSS file conversions starts in the order of groupId. 
	 * When there are more than one dictation in a group, they wait for all other dictation to complete conversion, and are added to upload queue.
	 * Each dictation is given to DSS file converter which returns 
	 * ArrayList of FilesCard that contains the details of converted file. 
	 **/
	private void onConvertFile(){
		mConvertThread = new Thread(new Runnable(){
			@Override
			public void run() {
				/*
				 * To check conversion thread is currently executing or not.
				 */
				if(!isConvertExecuting)
				{
					isConvertExecuting=true;//The conversion thread is on running state.
					try
					{
						mConvertCursor=mDbHandlerConvert.getConvertionDictation(mDMApplication.getCurrentGroupId());//Get next dictation with the order of groupId.
						/*
						 * To check next dictation is available or not for conversion.
						 */
						if(mConvertCursor.getCount()==1&&mConvertCursor.moveToFirst())
						{
							mConvertCard=mDbHandlerConvert.getSelectedDictation(mConvertCursor);
							mConvertCursor.close();
							
							mConvertingGroupId=mConvertCard.getGroupId();//Get current dictation group id.
							mFileConvert = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mConvertCard.getSequenceNumber()
												+"/"+mConvertCard.getDictFileName()+".wav");
							/*
							 * To check the file associated to the dictation is existing or not. If the WAV file doesn't
							 * exist, then change dictation status to 'Sending Failed'.
							 */
							if (!mFileConvert.exists()) {
								mFileConvert=null;
								//System.out.println(mConvertCard.getDictationId()+"<<<<<convert id>>>>>>"+mFileConvert.getPath());
								mConvertCard.setStatus(DictationStatus.SENDING_FAILED.getValue());
								mDbHandlerConvert.updateDictationStatusConvert(mConvertCard.getDictationId(),mConvertCard.getStatus());
								/*
								 * To check, whether the current group of dictations are converted or not.
								 * If all are converted then add them to Uploading Process.
								 */
								if(mDbHandlerConvert.getNonConvertedGroupCount(mConvertingGroupId)==0)
									mDbHandlerConvert.updateMainStatusConvert(200,mConvertingGroupId);
								mConvertCursor=null;
								mConvertCard=null;
								isConvertExecuting=false;
								mConvertingGroupId=-1;
								onUpdateList();
								return;
							}
							else
								mFileConvert=null;
							mConvertCard.setStatus(DictationStatus.SENDING.getValue());
							mDbHandlerConvert.updateDictationStatusConvert(mConvertCard.getDictationId(),mConvertCard.getStatus());
							onUpdateList();
							mDSSConverter=null;
							mDSSConverter=new DSSConverter();
							mFilesCards = mDSSConverter.convert(mConvertCard);//Start conversion process.
							/*
							 * If the result of conversion process is 'null' value, then change that dictation status to 'Conversion Failed'.
							 */
							if(mFilesCards!=null)
							{
								/*
								 * If the mFilesCards size is 'zero', then change that dictation status to 'Conversion Failed'.
								 */
								if(mFilesCards.size()>0)
								{
									if(mFilesCards.size()==1)
									{
										mFileConvert=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mConvertCard.getSequenceNumber()
												+"/"+mConvertCard.getDictationName()+"."+DMApplication.getDssType(mConvertCard.getDssVersion()));
										/*
										 * To check whether the converted file exists or not. If not exists then change the dictation status to 'Conversion Failed'.
										 */
										if(mFileConvert.exists())
										{
											mConvertCard.setIsConverted(1);
											mDbHandlerConvert.insertFiles(mFilesCards.get(0));
										}
										else
										{
											mConvertCard.setStatus(DictationStatus.CONVERTION_FAILED.getValue());
											mDbHandlerConvert.updateDictationStatusConvert(mConvertCard.getDictationId(),mConvertCard.getStatus());
											mConvertCard.setIsConverted(0);
											mConvertCard.setGroupId(0);
											mDbHandlerConvert.updateGroupId(mConvertCard.getDictationId(),mConvertCard.getGroupId());
										}
									}
									else
									{
										mConvertCard.setIsConverted(1);
										for(int i=0;i<mFilesCards.size();i++)
											mDbHandlerConvert.insertFiles(mFilesCards.get(i));
									}
								}
								else
								{
									mConvertCard.setStatus(DictationStatus.CONVERTION_FAILED.getValue());
									mDbHandlerConvert.updateDictationStatusConvert(mConvertCard.getDictationId(),mConvertCard.getStatus());
									mConvertCard.setIsConverted(0);
									mConvertCard.setGroupId(0);
									mDbHandlerConvert.updateGroupId(mConvertCard.getDictationId(),mConvertCard.getGroupId());
								}
							}
							else
							{
								mConvertCard.setStatus(DictationStatus.CONVERTION_FAILED.getValue());
								mDbHandlerConvert.updateDictationStatusConvert(mConvertCard.getDictationId(),mConvertCard.getStatus());
								mConvertCard.setIsConverted(0);
								mConvertCard.setGroupId(0);
								mDbHandlerConvert.updateGroupId(mConvertCard.getDictationId(),mConvertCard.getGroupId());
							}
							mDbHandlerConvert.updateIsConverted(mConvertCard);
							/*
							 * To check, whether the current group of dictations are converted or not.
							 * If all are converted, then add them for Uploading Process by changing their
							 * main status to '200'
							 */
							if(mDbHandlerConvert.getNonConvertedGroupCount(mConvertingGroupId)==0)
								mDbHandlerConvert.updateMainStatus(200,mConvertingGroupId);
							mFilesCards=null;
							mConvertCursor=null;
							mConvertCard=null;
							isConvertExecuting=false;
							mConvertingGroupId=-1;
							onUpdateList();
							return;
						}
						else
						{
							mConvertingGroupId=-1;
							isConvertExecuting=false;
							mConvertCursor.close();
							mConvertCursor=null;
							mConvertCard=null;
							mFilesCards=null;
							return;
						}
					}
					catch (Exception e) {
						isConvertExecuting=false;
						mConvertCursor=null;
						mConvertCard=null;
						mFilesCards=null;
						onUpdateList();
						return;
					}
				}
			}
		});
		mConvertThread.start();
	}
	
	/**
	 * This method handles Uploading of dictations after the conversion process.
	 * The dictations send from FlashAir are directly added to upload queue.
	 *
	 **/
	private void onUploadFile(){
			mUploadThread = new Thread(){
				@Override
				public void run() 
				{
					try 
					{
						if(!isUploadThreadExecuting)
						{
							isUploadThreadExecuting=true;
							mUploadCursor=null;
							mGroupOfDictationCard=null;
							mUploadCursor=mDbHandlerUpload.getUploadingDictation();//Get next group of dictations for uploading
							/*
							 * To check the dictations are available or not
							 */
							if(mUploadCursor.getCount()>0&&mUploadCursor.moveToFirst())
							{
								mUploadCard=null;
								mGroupOfDictationCard=new ArrayList<DictationCard>();
								hasIdleStateRealeased = false;
								isCurrentUploadingIsGoesToTimeOut = false;
								do {
									mUploadCard = null;
									mUploadCard = mDbHandlerUpload.getSelectedDictation(mUploadCursor);
									mUploadCard.setFilesList(mDbHandlerUpload.getFileList(mUploadCard.getDictationId()));
									isFileExists=true;
									if(mUploadCard.getFilesList().size()>0) {
										for(int i=0;i<mUploadCard.getFilesList().size();i++)
										{
											mFilesCard = null;
											mFileUpload = null;
											mFilesCard = mUploadCard.getFilesList().get(i);
											mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mUploadCard.getSequenceNumber()
													+"/"+mFilesCard.getFileName()+"."+DMApplication.getDssType(mUploadCard.getDssVersion()));
											if(!mFileUpload.exists())
											{
												//System.out.println(mUploadCard.getDictationId()+"<<<<<id>>>>>>"+mFileUpload.getPath());
												mUploadCard=mDbHandlerUpload.getDictationCardWithId(mUploadCard.getDictationId());
												mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mUploadCard.getSequenceNumber()
														+"/"+mFilesCard.getFileName()+"."+DMApplication.getDssType(mUploadCard.getDssVersion()));
												if(!mFileUpload.exists())
												{
													isFileExists=false;
													break;
												}
											}
										}
									}
									else {
										mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mUploadCard.getSequenceNumber()
												+"/"+mUploadCard.getDictationName()+"."+DMApplication.getDssType(mUploadCard.getDssVersion()));
										if(mFileUpload.exists()) {
											mFilesCard = new FilesCard();
											mFilesCard.setFileId(mUploadCard.getDictationId());
											mFilesCard.setFileIndex(0);
											mFilesCard.setFileName(mUploadCard.getDictationName());
											mDbHandlerUpload.insertFiles(mFilesCard);
											mUploadCard.setIsConverted(1);
											mUploadCard.setFilesList(mDbHandlerUpload.getFileList(mUploadCard.getDictationId()));
										}
										else {
											isFileExists = false;
											mUploadCard.setIsConverted(0);
										}
										mDbHandlerUpload.updateIsConverted(mUploadCard);
									}
									mFilesCard = null;
									mFileUpload = null;
									if(!isFileExists) {
										mUploadCard.setStatus(DictationStatus.SENDING_FAILED.getValue());
										mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
									}
									else
									{
										if(mUploadCard.getIsThumbnailAvailable()==1) {
											mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mUploadCard.getSequenceNumber()
													+"/"+mUploadCard.getDictFileName()+".jpg");
											if(!mFileUpload.exists()) {
												mUploadCard.setIsThumbnailAvailable(0);
												mDbHandlerUpload.updateIsThumbnailAvailable(mUploadCard);
											}
										}
										mFileUpload=null;
										if(mUploadCard.getStatus()!=DictationStatus.SENDING.getValue())
										{
											mUploadCard.setStatus(DictationStatus.SENDING.getValue());
											mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
										}
										mGroupOfDictationCard.add(mUploadCard);
									}
								}while(mUploadCursor.moveToNext());
								
								mUploadCursor.close();
								onUpdateList();
								
								onChangeStreamTime();
								if(mGroupOfDictationCard.size()>1) {
									isOutputStreaming=false;
									isCurrentUploadingIsGoesToTimeOut=false;
									isNotServerError=true;
									mOutputStream=null;
									mUploadAddress=null;
							        mUploadResult=null;
							        /*
							         * To check, their is any network available or not, except flash air connection.
							         */
							        if(DMApplication.isONLINE()) {
										try {
											mDictationString=getMultipleDictationsUTFString(mGroupOfDictationCard);
											mTempString1=mBoundary
													+"\n"
													+"Content-Disposition: form-data; name=\"ils-request\""
													+"\n"
													+"Content-Type: application/xml; charset=\"UTF-8\""
													+"\n\n"
													+mDictationString.trim();
											mTempString2="\n"+mBoundary+"--".trim();
											/*
											 * If the Android build version is 3.0 or later then calculate the expected size of streaming.
											 */
											if(isToUseFixedLength) {
												mTotalUploadSize=mTempString1.trim().getBytes().length;
												for(int i=0;i<mGroupOfDictationCard.size();i++)
													mTotalUploadSize=mTotalUploadSize+ getDSSDataSize(mGroupOfDictationCard.get(i),0);
												for(int i=0;i<mGroupOfDictationCard.size();i++)
													if(mGroupOfDictationCard.get(i).getIsThumbnailAvailable()!=0)
														mTotalUploadSize=mTotalUploadSize+ getImageDataSize(mGroupOfDictationCard.get(i));
												mTotalUploadSize=mTotalUploadSize+mTempString2.getBytes().length;
											}
											
											mUploadAddress = new URL(mUrl+"/"+mPrefUUID+"/dictation");
											/*
											 * Set default Socket with respect to system build version to handle 
											 * SSL peer exception.
											 */
											if (isToUseFixedLength)
												mSslContext = SSLContext.getInstance(SSLSocketFactory.TLS);
											else
												mSslContext = SSLContext.getInstance(SSLSocketFactory.SSL);
											
											// create custom trust manager
											OlyDMTrustManager olyDMTrustManager = new OlyDMTrustManager();
									        TrustManager[] tm = new TrustManager[]{olyDMTrustManager};
									        // initialize the SSLContext with custom trust manager
											mSslContext.init(null, tm , new java.security.SecureRandom());//Generate default socket certificate
											
											mUrlConnection = mUploadAddress.openConnection();
											/*
											 * To Check an ODP's URL is HTTP/HTTPS
											 */
											if(mUrlConnection instanceof HttpsURLConnection) {
												mHttpsURLConnection = (HttpsURLConnection)mUrlConnection;
												mHttpsURLConnection.setReadTimeout(6*mTimeLimit);//set response read time out for the established connection
												mHttpsURLConnection.setConnectTimeout(mTimeLimit);//set maximum time limit to connect.
												mHttpsURLConnection.setRequestMethod(HttpPost.METHOD_NAME);
												/*
												 * Set default certificate to the httpUrlConnection to handle SSL certificate exception.
												 */
												mHttpsURLConnection.setSSLSocketFactory(mSslContext.getSocketFactory());
	//											connection.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
												mHttpsURLConnection.setDoInput(true);
												mHttpsURLConnection.setDoOutput(true);
												mHttpsURLConnection.setUseCaches(false);
												mHttpsURLConnection.setAllowUserInteraction(true);
												/*
												 * Setting header values to the httpConnection.
												 */
												mHttpsURLConnection.setRequestProperty("Host",mUploadAddress.getHost());
												mHttpsURLConnection.setRequestProperty("Content-Type", 
														"multipart/form-data; boundary=----------ILSBoundary8FD83EF0C2254A9B");
												mHttpsURLConnection.setRequestProperty("X-ILS-Authorization",
														"Basic "+ mBase64Value.trim());
												mHttpsURLConnection.setRequestProperty("Accept-Encoding","identity");
												mHttpsURLConnection.setRequestProperty("Connection","Keep-Alive");
	//											connection.setRequestProperty("Connection", "close");
												/*
												 * Set stream mode for the httpUrlConnection with respect to 
												 * the build version.
												 */
												if (isToUseFixedLength)
													mHttpsURLConnection.setFixedLengthStreamingMode(mTotalUploadSize);
												else {
													mHttpsURLConnection.setRequestProperty("Transfer-Encoding","chunked");
													mHttpsURLConnection.setChunkedStreamingMode(0);
												}
												mHttpsURLConnection.connect();
												mTotalUploadSize=0;
												
												/*
												 * Get established connection's 'OutputStream' for writing data in to the connection.
												 */
												mOutputStream = new DataOutputStream(mHttpsURLConnection.getOutputStream());
											}
											else {
												mHttpURLConnection = (HttpURLConnection)mUrlConnection;
												mHttpURLConnection.setReadTimeout(6*mTimeLimit);//set response read time out for the established connection
												mHttpURLConnection.setConnectTimeout(mTimeLimit);//set maximum time limit to connect.
												mHttpURLConnection.setRequestMethod(HttpPost.METHOD_NAME);
	//											connection.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
												mHttpURLConnection.setDoInput(true);
												mHttpURLConnection.setDoOutput(true);
												mHttpURLConnection.setUseCaches(false);
												mHttpURLConnection.setAllowUserInteraction(true);
												/*
												 * Setting header values to the httpConnection.
												 */
												mHttpURLConnection.setRequestProperty("Host",mUploadAddress.getHost());
												mHttpURLConnection.setRequestProperty("Content-Type", 
														"multipart/form-data; boundary=----------ILSBoundary8FD83EF0C2254A9B");
												mHttpURLConnection.setRequestProperty("X-ILS-Authorization",
														"Basic "+ mBase64Value.trim());
												mHttpURLConnection.setRequestProperty("Accept-Encoding","identity");
												mHttpURLConnection.setRequestProperty("Connection","Keep-Alive");
	//											connection.setRequestProperty("Connection", "close");
												/*
												 * Set stream mode for the httpUrlConnection with respect to 
												 * the build version.
												 */
												if (isToUseFixedLength)
													mHttpURLConnection.setFixedLengthStreamingMode(mTotalUploadSize);
												else {
													mHttpURLConnection.setRequestProperty("Transfer-Encoding","chunked");
													mHttpURLConnection.setChunkedStreamingMode(0);
												}
												mHttpURLConnection.connect();
												mTotalUploadSize=0;
												
												/*
												 * Get established connection's 'OutputStream' for writing data in to the connection.
												 */
												mOutputStream = new DataOutputStream(mHttpURLConnection.getOutputStream());
											}
											
											/*
											 * Streaming values in to httpConnection without storing any bytes of data.
											 */
											mOutputStream.writeBytes(mTempString1);
											for(mPositionUpload=0;mPositionUpload<mGroupOfDictationCard.size();mPositionUpload++)
											{/*
											 * Write bytes of each dictation file from the group as one by one.
											 */
												mUploadCard=mGroupOfDictationCard.get(mPositionUpload);
												if(!mUploadCard.getDictationName().trim().equalsIgnoreCase(""))
												{
													onChangeStreamTime();
													mOutputStream.writeBytes("\n");
													mOutputStream.writeBytes(mBoundary);
													mOutputStream.writeBytes("\n");
													
													mOutputStream.writeBytes("Content-Disposition: attachment; filename="+
															'"'+mUploadCard.getDictationName()+"."+
															DMApplication.getDssType(mUploadCard.getDssVersion())+'"');
													mOutputStream.writeBytes("\n");
													mOutputStream.writeBytes("Content-Type: application/x-olydss");
													mOutputStream.writeBytes("\n");
													mOutputStream.writeBytes("Content-Transfer-Encoding: binary");
													mOutputStream.writeBytes("\n\n");
													
													mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"
															+mUploadCard.getSequenceNumber()+"/"
															+mUploadCard.getDictationName()+"."
															+DMApplication.getDssType(mUploadCard.getDssVersion()));
													mFileInputStream = new FileInputStream(mFileUpload);
													mBytesAvailable = mFileInputStream.available();
											        mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
											        mBufferArray = new byte[mBufferSize];
											        mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
											        /*
											         * Read and write the bytes of data from corresponding file of dictation 
											         * in to the connection.
											         */
											        while (mBytesRead > 0) {
											            //outputStream.flush();
											        	onChangeStreamTime();
											            mOutputStream.write(mBufferArray,0,mBufferSize);
											            mBytesAvailable = mFileInputStream.available();
											            mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
											            mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
											            System.gc();
											        }
											        mBufferArray=null;
											        mBytesRead=0;
											        mFileInputStream.close();
											        //outputStream.flush();
											        /*
											         * To check, whether the thumbnail is available or not.
											         */
											        if(mUploadCard.getIsThumbnailAvailable()==1)
													{
											        	onChangeStreamTime();
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes(mBoundary);
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes("Content-Disposition: attachment; filename="+
															'"'+mUploadCard.getDictationName()+".jpg"+'"');
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes("Content-Type: image/jpeg");
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes("Content-Transfer-Encoding: binary");
														mOutputStream.writeBytes("\n\n");
														
														mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"
																		+mUploadCard.getSequenceNumber()
																		+"/"+mUploadCard.getDictFileName()+".jpg");
														mFileInputStream = new FileInputStream(mFileUpload);
														mBytesAvailable = mFileInputStream.available();
													    mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
													    mBufferArray = new byte[mBufferSize];
													    mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
													    /*
												         * Read and write the bytes of data from corresponding file of dictation 
												         * in to the connection.
												         */
													    while (mBytesRead > 0) {
													    	onChangeStreamTime();
													        mOutputStream.write(mBufferArray, 0, mBufferSize);
													        mBytesAvailable = mFileInputStream.available();
													        mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
													        mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
													    }
													    mBufferArray=null;
													    mBytesRead=0;
													    mFileInputStream.close();
													    //outputStream.flush();
													    System.gc();
													}
												}
											}
									        mOutputStream.writeBytes(mTempString2);
									        //outputStream.flush();
									        isOutputStreaming=false;
									        mOutputStream.close();//close OutputStream
									        mOutputStream=null;
	
									        isInputStreaming = true;
									        onChangeStreamTime();
									        /*
									         * Read the response of httpUrlConnection.
									         */
									        if(mUrlConnection instanceof HttpsURLConnection)
									        	mInputStream = mHttpsURLConnection.getInputStream();
									        else
									        	mInputStream = mHttpURLConnection.getInputStream();
									        onChangeStreamTime();
									        mBufferedInputStream= new BufferedInputStream(mInputStream,4);
									        mStringBuffer =new StringBuffer();
									        mBufferArray=new byte[4];
											int c=0;
											while((c=mBufferedInputStream.read(mBufferArray))!=-1){
												for(int j=0;j<c;j++)
													mStringBuffer.append((char)mBufferArray[j]);
											}
											mBufferedInputStream.close();
											mBufferedInputStream=null;
											mUploadResult=mStringBuffer.toString().trim();
											mStringBuffer=null;
											mInputStream.close();
											isInputStreaming = false;
											mInputStream=null;
								        }
								        catch(Exception e){
								        	mUploadResult="http_error";
								        	//e.printStackTrace();
								        }
										finally{
											if(mUrlConnection instanceof HttpsURLConnection)
									        	mHttpsURLConnection.disconnect();
									        else
									        	mHttpURLConnection.disconnect();
											mHttpsURLConnection = null;
											mHttpURLConnection = null;
										}
										mFileUpload=null;
										if(hasIdleStateRealeased) {
											hasIdleStateRealeased = false;
											isUploadThreadExecuting = false;
											onUpdateList();
											return;
										}
										/*
										 * To check the request/streaming has successfully completed or not.
										 */
										if(!(mUploadResult.equalsIgnoreCase("http_error")||mUploadResult.equalsIgnoreCase("")))
										{
											/*
											 * Parse the response of uploaded dictation.
											 */
											mUploadFileXmlParser=new DictationUploadFileXmlParser(mUploadResult);
											mUploadFileXmlParser.parse(getLanguage());
											mUploadingAttributeObjects=mUploadFileXmlParser.getAttributeObjects();
											mJobDataObjects=mUploadFileXmlParser.getJobDataObjects();
											
											mUploadResult=mUploadingAttributeObjects.get(0).getResultCode().trim();
											/*
											 * checks the result code and perform corresponding actions.
											 * If the result code is '2000'(Success) then set corresponding job no;
											 *  and transfer id.
											 */
											if(mUploadResult.equalsIgnoreCase("2000"))
											{	
												/*
												 * Set each dictations job number and transfer id from parsed values. And move these dictations 
												 * to querying queue.
												 */
												for(int j=0;j<mJobDataObjects.size();j++)
												{
													mUploadCard=mGroupOfDictationCard.get(j);
													mUploadCard.getFilesList().get(0).setTransferId(mJobDataObjects.get(j).getTransferId());
													mUploadCard.getFilesList().get(0).setJobNumber(mJobDataObjects.get(j).getJobNumber());
													mDbHandlerUpload.updateTransferIdAndJobNo(mUploadCard.getFilesList().get(0));
													mUploadCard.setGroupId(0);
													mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
													mDbHandlerUpload.updateQueryPriority(mUploadCard.getDictationId());
													mUploadCard.setStatus(DictationStatus.WAITING_TO_SEND1.getValue());
													mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
													mDbHandlerUpload.updateMainStatusByDicts(300,mUploadCard.getDictationId());
												}
												isNotServerError=true;
												mUploadingAttributeObjects=null;
												mJobDataObjects=null;
											}
											/*
											 * If the response is server error, then the dictation retry for three times.
											 * After completing the retry count, then dictations move to 'Time Out' status
											 */
											else if(mUploadResult.equalsIgnoreCase("5000")||mUploadResult.equalsIgnoreCase("5001"))
												isNotServerError=false;
											/*
											 * when any critical error happens, set all dictations in 'Sending' process 
											 * to 'Timeout' except the dictations in the conversion queue.
											 */
											else if(mUploadResult.equalsIgnoreCase("4000")||mUploadResult.equalsIgnoreCase("4006")||
													mUploadResult.equalsIgnoreCase("4007")||mUploadResult.equalsIgnoreCase("4008")||
													mUploadResult.equalsIgnoreCase("4009")||mUploadResult.equalsIgnoreCase("5002"))
											{
												mErrorMessage=null;
												
												if(!mDMApplication.isTimeOutDialogOnFront())
												{
													if(mUploadResult.equalsIgnoreCase("5002"))
														mErrorMessage=mUploadFileXmlParser.getMessage();
													mDMApplication.setErrorCode(mUploadResult);
													onTimeOutDilaoge();
												}
												isNotServerError=true;
												if(mUploadResult.equalsIgnoreCase("4007"))
												{
													mSharedPreferences =getSharedPreferences(PREFS_NAME, 0);
						                 			mEditor=mSharedPreferences.edit();
						                 		    mEditor.putString("Activation" ,"Not Activated");
						                 		    mEditor.commit();
												}
												onMoveAllToTimeOut();
											}
											/*
											 * When any other error occurs such as, 'bad request','fail to activate', then the status of
											 *  dictation is changed to 'Timeout'.
											 */
											else if(mUploadResult.equalsIgnoreCase("4001")||mUploadResult.equalsIgnoreCase("4002")||
													mUploadResult.equalsIgnoreCase("4003")||mUploadResult.equalsIgnoreCase("4004")||
													mUploadResult.equalsIgnoreCase("4005"))
											{
												isNotServerError=true;
												for(int i=0;i<mGroupOfDictationCard.size();i++)
												{
													mUploadCard=mGroupOfDictationCard.get(i);
													mUploadCard.setStatus(DictationStatus.TIMEOUT.getValue());
													mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
												}
												mGroupOfDictationCard=null;
											}
										}
										/*
										 * If any error occurs during uploading, then the dictation is kept for retrying after 5 minutes
										 */
										else
										{
											isNotServerError=true;
											for(int i=0;i<mGroupOfDictationCard.size();i++)
											{
												mUploadCard=mGroupOfDictationCard.get(i);
												mUploadCard.setStatus(DictationStatus.RETRYING1.getValue());
												mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
											}
											mGroupOfDictationCard=null;
											if(!hasUploadCounting&&!hasQueryCounting)
											{
												hasUploadCounting=true;
												onHttpRetrying();//Call to start five minute timer.
											}
											hasUploadCounting=true;
											isNotServerError=true;
										}
										/*
										 * checks whether the connection is closed due to server error or not.
										 */
										if(!isNotServerError){
											// checks retry count
											if(mDbHandlerUpload.getRetryCount(mGroupOfDictationCard.get(0).getFilesList().get(0))<2){
												mGroupId=mDbHandlerUpload.getGroupId();
												for(int i=0;i<mGroupOfDictationCard.size();i++)
												{
													mUploadCard=mGroupOfDictationCard.get(i);
													mUploadCard.setStatus(DictationStatus.RETRYING3.getValue());
													mUploadCard.getFilesList().get(0).setRetryCount(1+mDbHandlerUpload.getRetryCount(mUploadCard.getFilesList().get(0)));
													mDbHandlerUpload.updateRetryCount(mUploadCard.getFilesList().get(0));
													mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
													mUploadCard.setGroupId(mGroupId);
													mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
												}
											}
											else
											{
												for(int j=0;j<mGroupOfDictationCard.size();j++)
												{
													mUploadCard=mGroupOfDictationCard.get(j);
													mUploadCard.getFilesList().get(0).setRetryCount(0);
													mDbHandlerUpload.updateRetryCount(mUploadCard.getFilesList().get(0));
													mUploadCard.setStatus(DictationStatus.TIMEOUT.getValue());
													mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
												}
											}
											isNotServerError = true;
											isUploadThreadExecuting=false;
											onUpdateList();
											return;
										}
										else
										{
											isUploadThreadExecuting=false;
											onUpdateList();
											return;
										}
									}
							        /*
							         * If the httpConnection isn't established due to network problem, then change status of 
							         * dictation to 'Retrying' state. 
							         */
							        else
									{
										isNotServerError=true;
										for(int i=0;i<mGroupOfDictationCard.size();i++)
										{
											mUploadCard=mGroupOfDictationCard.get(i);
											mUploadCard.setStatus(DictationStatus.RETRYING2.getValue());
											mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
										}
										mGroupOfDictationCard=null;
										isUploadThreadExecuting=false;
										onUpdateList();
										return;
									}
								}
								
								else
								{
									if(mGroupOfDictationCard.size()==0||mUploadCard==null)
									{
										mGroupOfDictationCard=null;
										isUploadThreadExecuting=false;
										onUpdateList();
										return;
									}
									mUploadCard=mGroupOfDictationCard.get(0);
									mGroupOfDictationCard=null;
									/*
									 * if any error occurs with FilesCard, then change the status of dictation to
									 * 'sending Failed'.
									 */
									if(mUploadCard.getFilesList()==null||mUploadCard.getFilesList().size()==0)
									{
										mUploadCard = mDbHandlerUpload.getDictationCardWithId(mUploadCard.getDictationId());
										if(mUploadCard!=null)
										{
											mUploadCard.setFilesList(mDbHandlerUpload.getFileList(mUploadCard.getDictationId()));
											if(mUploadCard.getFilesList()==null||mUploadCard.getFilesList().size()==0)
											{
												mUploadCard.setStatus(DictationStatus.SENDING_FAILED.getValue());
												mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
												mUploadCard.setIsConverted(0);
												mDbHandlerUpload.updateIsConverted(mUploadCard);
												mDbHandlerUpload.updateMainStatusByDicts(0,mUploadCard.getDictationId());
												mUploadCard.setGroupId(0);
												mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
												mUploadCard=null;
												isUploadThreadExecuting=false;
												onUpdateList();
												return;
											}
										}
										else
										{
											isUploadThreadExecuting=false;
											onUpdateList();
											return;
										}
									}
									// upload multiple files,if any
									for(mPositionUpload=0;mPositionUpload<mUploadCard.getFilesList().size();mPositionUpload++)
									{
										isUploadThreadExecuting=true;
										isCurrentUploadingIsGoesToTimeOut=false;
										if(mPositionUpload!=0)
										{
											try
											{
												mUploadCard = mDbHandlerUpload.getDictationCardWithId(mUploadCard.getDictationId());
												mUploadCard.setFilesList(mDbHandlerUpload.getFileList(mUploadCard.getDictationId()));
											}
											catch (Exception e) {}
										}
										if(mUploadCard.getStatus()==DictationStatus.TIMEOUT.getValue()||mUploadCard.getStatus()==DictationStatus.SENDING_FAILED.getValue())
										{
											isUploadThreadExecuting=false;
											onUpdateList();
											break;
										}
										
										isNotServerError=true;
										mOutputStream=null;
										mUploadAddress=null;
								        mUploadResult=null;
								        mFilesCard=mUploadCard.getFilesList().get(mPositionUpload);
								        /*
								         * To check whether the current file is already uploaded or not(
								         * only applicable for split condition).
								         */
								        if(!mFilesCard.getTransferId().trim().equalsIgnoreCase(""))
								        {
								        	/*
								        	 * If there is any file pending to upload, then goes to next iteration
								        	 */
								        	if(mPositionUpload<(mUploadCard.getFilesList().size()-1))
								        	{
								        		mFilesCard=null;
								        		continue;
								        	}
								        	else
								        	{
								        		mFilesCard=null;
								        		if(mDbHandlerUpload.getSplitUploadPendingCount(mUploadCard.getDictationId())==0)
								        			mDbHandlerUpload.updateMainStatusByDicts(300,mUploadCard.getDictationId());
								        		else
								        		{
								        			mUploadCard.setGroupId(mDbHandlerUpload.getGroupId());
									        		mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
								        		}
								        		mUploadCard=null;
								        		isUploadThreadExecuting=false;
												onUpdateList();
												break;
								        	}
								        }
								        /*
								         * checks whether there is any network available or not, except flash air connection.
								         */
								        if(DMApplication.isONLINE())
										{
											try 
											{
												mDictationString=getDictationUTFString();
												mTempString1=mBoundary
														+"\n"
														+"Content-Disposition: form-data; name=\"ils-request\""
														+"\n"
														+"Content-Type: application/xml; charset=\"UTF-8\""
														+"\n\n"
														+mDictationString.trim();
												mTempString2="\n"+mBoundary+"--".trim();
												/*
												 * If Android build version is 3.0 or later then calculate the expected size of streaming.
												 */
												if(isToUseFixedLength)
												{
													mTotalUploadSize=mTempString1.trim().getBytes().length;
													if(!mFilesCard.getFileName().trim().equalsIgnoreCase(""))
														mTotalUploadSize=mTotalUploadSize+ getDSSDataSize(mUploadCard,mPositionUpload);
													if(mUploadCard.getIsThumbnailAvailable()!=0&&mPositionUpload==0)
														mTotalUploadSize=mTotalUploadSize+ getImageDataSize(mUploadCard);
													mTotalUploadSize=mTotalUploadSize+mTempString2.getBytes().length;
												}
												mUploadAddress=new URL(mUrl+"/"+mPrefUUID+"/dictation");
												/*
												 * Set default Socket with respect to the build version to handle
												 * SSL peer exception.
												 */
												if (isToUseFixedLength)
													mSslContext = SSLContext.getInstance(SSLSocketFactory.TLS);
												else
													mSslContext = SSLContext.getInstance(SSLSocketFactory.SSL);
												
												//create custom trust manager
												OlyDMTrustManager olyDMTrustManager = new OlyDMTrustManager();
										        TrustManager[] tm = new TrustManager[]{olyDMTrustManager};
												// initialize SSLContext with custom trust manager
										        mSslContext.init(null, tm ,new java.security.SecureRandom());//Generate default socket certificate
												
												mUrlConnection = mUploadAddress.openConnection();
												if(mUrlConnection instanceof HttpsURLConnection) {
													mHttpsURLConnection=(HttpsURLConnection)mUploadAddress.openConnection();
													mHttpsURLConnection.setReadTimeout(6*mTimeLimit);//set response read time out for the established connection
													mHttpsURLConnection.setConnectTimeout(mTimeLimit);//set maximum time limit to connect.
													mHttpsURLConnection.setRequestMethod(HttpPost.METHOD_NAME);
													/*
													 * Set default certificate to the httpUrlConnection to handle SSL certificate 
													 * exception.
													 */
													mHttpsURLConnection.setSSLSocketFactory(mSslContext.getSocketFactory());
	//												connection.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
													mHttpsURLConnection.setDoInput(true);
													mHttpsURLConnection.setDoOutput(true);
													mHttpsURLConnection.setUseCaches(false);
													mHttpsURLConnection.setAllowUserInteraction(true);
													/*
													 * Setting header values to the httpConnection
													 */
													mHttpsURLConnection.setRequestProperty("Host",mUploadAddress.getHost());
													mHttpsURLConnection.setRequestProperty("Content-Type", 
															"multipart/form-data; boundary=----------ILSBoundary8FD83EF0C2254A9B");
													mHttpsURLConnection.setRequestProperty("X-ILS-Authorization",
															"Basic "+ mBase64Value.trim());
													mHttpsURLConnection.setRequestProperty("Accept-Encoding","identity");
													mHttpsURLConnection.setRequestProperty("Connection","Keep-Alive");
	//												connection.setRequestProperty("Connection", "close");
													/*
													 * Set stream mode for the httpUrlConnection with respect to 
													 * the build version.
													 */
													if (isToUseFixedLength)
														mHttpsURLConnection.setFixedLengthStreamingMode(mTotalUploadSize);
													else {
														mHttpsURLConnection.setRequestProperty("Transfer-Encoding","chunked");
														mHttpsURLConnection.setChunkedStreamingMode(0);
													}
													mHttpsURLConnection.connect();
													mTotalUploadSize=0;
						
													/*
													 * get the OutputStream of connection for writing data in to the connection.
													 */
													mOutputStream = new DataOutputStream(mHttpsURLConnection.getOutputStream());
												}
												else {
													mHttpURLConnection = (HttpURLConnection)mUploadAddress.openConnection();
													mHttpURLConnection.setReadTimeout(6*mTimeLimit);//set response read time out for the established connection
													mHttpURLConnection.setConnectTimeout(mTimeLimit);//set maximum time limit to connect.
													mHttpURLConnection.setRequestMethod(HttpPost.METHOD_NAME);
	//												connection.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
													mHttpURLConnection.setDoInput(true);
													mHttpURLConnection.setDoOutput(true);
													mHttpURLConnection.setUseCaches(false);
													mHttpURLConnection.setAllowUserInteraction(true);
													/*
													 * Setting header values to the httpConnection
													 */
													mHttpURLConnection.setRequestProperty("Host",mUploadAddress.getHost());
													mHttpURLConnection.setRequestProperty("Content-Type", 
															"multipart/form-data; boundary=----------ILSBoundary8FD83EF0C2254A9B");
													mHttpURLConnection.setRequestProperty("X-ILS-Authorization",
															"Basic "+ mBase64Value.trim());
													mHttpURLConnection.setRequestProperty("Accept-Encoding","identity");
													mHttpURLConnection.setRequestProperty("Connection","Keep-Alive");
	//												connection.setRequestProperty("Connection", "close");
													/*
													 * Set stream mode for the httpUrlConnection with respect to 
													 * the build version.
													 */
													if (isToUseFixedLength)
														mHttpURLConnection.setFixedLengthStreamingMode(mTotalUploadSize);
													else {
														mHttpURLConnection.setRequestProperty("Transfer-Encoding","chunked");
														mHttpURLConnection.setChunkedStreamingMode(0);
													}
													mHttpURLConnection.connect();
													mTotalUploadSize=0;
						
													/*
													 * get the OutputStream of connection for writing data in to the connection.
													 */
													mOutputStream = new DataOutputStream(mHttpURLConnection.getOutputStream());
												}
												
												
												mOutputStream.writeBytes(mTempString1);
												isOutputStreaming=true;
												if(!mFilesCard.getFileName().trim().equalsIgnoreCase(""))
												{
													onChangeStreamTime();
													mOutputStream.writeBytes("\n");
													mOutputStream.writeBytes(mBoundary);
													mOutputStream.writeBytes("\n");
													
													mOutputStream.writeBytes("Content-Disposition: attachment; filename="+
															'"'+mFilesCard.getFileName()+"."+
															DMApplication.getDssType(mUploadCard.getDssVersion())+'"');
													mOutputStream.writeBytes("\n");
													mOutputStream.writeBytes("Content-Type: application/x-olydss");
													mOutputStream.writeBytes("\n");
													mOutputStream.writeBytes("Content-Transfer-Encoding: binary");
													mOutputStream.writeBytes("\n\n");
													
													mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"
															+mUploadCard.getSequenceNumber()+"/"
															+mFilesCard.getFileName()+"."
															+DMApplication.getDssType(mUploadCard.getDssVersion()));
													mFileInputStream = new FileInputStream(mFileUpload);
													mBytesAvailable = mFileInputStream.available();
											        mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
											        mBufferArray = new byte[mBufferSize];
											        mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
											        /*
											         * Read and write the binary data of corresponding file in to the connection.
											         */
											        while (mBytesRead > 0) {
											        	onChangeStreamTime();
											            mOutputStream.write(mBufferArray,0,mBufferSize);
											            mBytesAvailable = mFileInputStream.available();
											            mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
											            mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
//											            outputStream.flush();
											            System.gc();
											        }
											        mBufferArray=null;
											        mBytesRead=0;
											        mFileInputStream.close();
//											        outputStream.flush();
											        /*
											         * To check, whether the thumbnail is available or not.
											         */
											        if(mUploadCard.getIsThumbnailAvailable()==1&&mPositionUpload==0)
													{
											        	onChangeStreamTime();
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes(mBoundary);
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes("Content-Disposition: attachment; filename="+
															'"'+mUploadCard.getDictationName()+".jpg"+'"');
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes("Content-Type: image/jpeg");
														mOutputStream.writeBytes("\n");
														mOutputStream.writeBytes("Content-Transfer-Encoding: binary");
														mOutputStream.writeBytes("\n\n");
														
														mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"
																		+mUploadCard.getSequenceNumber()
																		+"/"+mUploadCard.getDictFileName()
																		+".jpg");
														mFileInputStream = new FileInputStream(mFileUpload);
														mBytesAvailable = mFileInputStream.available();
													    mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
													    mBufferArray = new byte[mBufferSize];
													    mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
													    /*
												         * Read and write the binary data of corresponding file in to the connection.
												         */
													    while (mBytesRead > 0) {
													    	onChangeStreamTime();
													        mOutputStream.write(mBufferArray, 0, mBufferSize);
													        mBytesAvailable = mFileInputStream.available();
													        mBufferSize = Math.min(mBytesAvailable, mMaxBufferSize);
													        mBytesRead = mFileInputStream.read(mBufferArray, 0, mBufferSize);
													    }
													    mBufferArray=null;
													    mBytesRead=0;
													    mFileInputStream.close();
													    //outputStream.flush();
													    System.gc();
													}
												}
										        mOutputStream.writeBytes(mTempString2);
										        //outputStream.flush();
										        isOutputStreaming=false;
										        
										        mOutputStream.close();//close OutputStream
										        mOutputStream=null;
										        isInputStreaming=true;
										        onChangeStreamTime();
										        /*
										         * Read the response of httpUrlconnection.
										         */
										        if(mUrlConnection instanceof HttpsURLConnection)
										        	mInputStream = mHttpsURLConnection.getInputStream();
										        else
										        	mInputStream = mHttpURLConnection.getInputStream();
										        onChangeStreamTime();
												mBufferedInputStream = new BufferedInputStream(mInputStream,4);
												mStringBuffer=new StringBuffer();
												mBufferArray=new byte[4];
												int c=0;
												while((c=mBufferedInputStream.read(mBufferArray))!=-1){
													for(int j=0;j<c;j++)
														mStringBuffer.append((char)mBufferArray[j]);
												}
												mBufferedInputStream.close();
												mBufferedInputStream=null;
												mUploadResult=mStringBuffer.toString().trim();
												mStringBuffer=null;
												mInputStream.close();
												mInputStream=null;
												isInputStreaming=false;
									        }
									        catch(Exception e){
									        	//e.printStackTrace();
									        	mUploadResult="http_error";
									        }
											finally{
												if(mUrlConnection instanceof HttpsURLConnection)
										        	mHttpsURLConnection.disconnect();
										        else
										        	mHttpURLConnection.disconnect();
												mHttpsURLConnection = null;
												mHttpURLConnection = null;
											}
										}
								        if(hasIdleStateRealeased) {
											hasIdleStateRealeased = false;
											isUploadThreadExecuting = false;
											onUpdateList();
											return;
										}
								        mFileUpload=null;
										if(mUploadResult!=null)
										{
											/*
											 * When any one of the split file goes to 'Timeout', then break
											 * the uploading process of the dictation.
											 */
											if(isCurrentUploadingIsGoesToTimeOut)
											{
												isUploadThreadExecuting=false;
												isCurrentUploadingIsGoesToTimeOut=false;
												onUpdateList();
												break;
											}
											isCurrentUploadingIsGoesToTimeOut=false;
											/*
											 * checks whether the request/streaming has successfully completed or not. 
											 */
											if(!(mUploadResult.equalsIgnoreCase("http_error")||mUploadResult.equalsIgnoreCase("")))
											{
												/*
												 * Parse the response of uploaded dictation.
												 */
												mUploadFileXmlParser=new DictationUploadFileXmlParser(mUploadResult);
												mUploadFileXmlParser.parse(getLanguage());
												mUploadingAttributeObjects=mUploadFileXmlParser.getAttributeObjects();
												mJobDataObjects=mUploadFileXmlParser.getJobDataObjects();
												mUploadResult=mUploadingAttributeObjects.get(0).getResultCode().trim();
												/*
												 *checks the result code and perform appropriate action.
												 * If the result code is '2000'(Success) then assign job no;
												 *  and transfer id to the FilesCard.
												 */
												if(mUploadResult.equalsIgnoreCase("2000"))
												{
													mFilesCard.setTransferId(mJobDataObjects.get(0).getTransferId());
													mFilesCard.setJobNumber(mJobDataObjects.get(0).getJobNumber());
													mUploadCard.getFilesList().set(mPositionUpload,mFilesCard);
													mDbHandlerUpload.updateTransferIdAndJobNo(mFilesCard);
													mDbHandlerUpload.updateQueryPriority(mUploadCard.getDictationId());
													mUploadingAttributeObjects=null;
													mJobDataObjects=null;
													isNotServerError=true;
													/*
													 * if any split file of a dictation is uploaded, then it is moved to query queue and continue with uploading of
													 * other split files.
													 */
													if(mPositionUpload<(mUploadCard.getFilesList().size()-1))
													{
														
														mDbHandlerUpload.updateMainStatusByDicts(400,mUploadCard.getDictationId());
														mUploadCard.setGroupId(mDbHandlerUpload.getGroupId());
														mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
														mDbHandlerUpload.updateSplitInternalStatus(mUploadCard.getDictationId() , DictationStatus.WAITING_TO_SEND1.getValue());
														onUpdateList();
														continue;
													}
													else if(mPositionUpload>0&&mDbHandlerUpload.getSplitUploadPendingCount(mUploadCard.getDictationId())>0)
													{
														mDbHandlerUpload.updateMainStatusByDicts(400,mUploadCard.getDictationId());
														mUploadCard.setGroupId(mDbHandlerUpload.getGroupId());
														mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
														mDbHandlerUpload.updateSplitInternalStatus(mUploadCard.getDictationId() , DictationStatus.WAITING_TO_SEND1.getValue());
														mUploadCard=null;
														isUploadThreadExecuting=false;
														onUpdateList();
														break;
													}
													/*
													 * When all split files are uploaded, the dictation status is changed to 'Waiting to Send' and is moved to query queue
													 */
													else
													{
														mDbHandlerUpload.updateSplitInternalStatus(mUploadCard.getDictationId() , 0);
														mDbHandlerUpload.updateMainStatusByDicts(300,mUploadCard.getDictationId());
														mUploadCard.setStatus(DictationStatus.WAITING_TO_SEND1.getValue());
														mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
														mUploadCard.setGroupId(0);
														mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
														isNotServerError=true;
													}
												}
												/*
												 * If the response is server error, then the dictation retry's three times.
												 * After completing retry count, then dictation status is changed to 'Time Out'
												 */
												else if(mUploadResult.equalsIgnoreCase("5000")||mUploadResult.equalsIgnoreCase("5001"))
													isNotServerError=false;
												/*
												 * when any critical error happens, set all dictations in 'Sending' process 
												 * to 'Timeout' except the dictations in the conversion queue.
												 */
												else if(mUploadResult.equalsIgnoreCase("4000")||mUploadResult.equalsIgnoreCase("4006")||
														mUploadResult.equalsIgnoreCase("4007")||mUploadResult.equalsIgnoreCase("4008")||
														mUploadResult.equalsIgnoreCase("4009")||mUploadResult.equalsIgnoreCase("5002"))
												{
													mErrorMessage=null;
													
													if(!mDMApplication.isTimeOutDialogOnFront())
													{
														if(mUploadResult.equalsIgnoreCase("5002"))
															mErrorMessage=mUploadFileXmlParser.getMessage();
														mDMApplication.setErrorCode(mUploadResult);
														onTimeOutDilaoge();
													}
													isNotServerError=true;
													if(mUploadResult.equalsIgnoreCase("4007"))
													{
														mSharedPreferences =getSharedPreferences(PREFS_NAME, 0);
							                 			mEditor=mSharedPreferences.edit();
							                 		    mEditor.putString("Activation" ,"Not Activated");
							                 		    mEditor.commit();
													}
													onMoveAllToTimeOut();
												}
												/*
												 * When any other error occurs such as, 'bad request','fail to activate', then the status of
												 *  dictation is changed to 'Timeout'.
												 */
												else if(mUploadResult.equalsIgnoreCase("4001")||mUploadResult.equalsIgnoreCase("4002")||
														mUploadResult.equalsIgnoreCase("4003")||mUploadResult.equalsIgnoreCase("4004")||
														mUploadResult.equalsIgnoreCase("4005"))
												{
													isNotServerError=true;
													mUploadCard.setStatus(DictationStatus.TIMEOUT.getValue());
													mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
												}
												/*
												 * checks whether the connection is closed due to server error or not.
												 */
												if(!isNotServerError){
													
													if(mUploadCard.getFilesList().get(mPositionUpload).getRetryCount()<2){
														mUploadCard.setStatus(DictationStatus.RETRYING3.getValue());
														mUploadCard.setGroupId(mDbHandlerUpload.getGroupId());
														mDbHandlerUpload.updateGroupId(mUploadCard.getDictationId(),mUploadCard.getGroupId());
														mUploadCard.getFilesList().get(mPositionUpload).setRetryCount(1+mDbHandlerUpload.getRetryCount(mUploadCard.getFilesList().get(mPositionUpload)));
														mDbHandlerUpload.updateRetryCount(mUploadCard.getFilesList().get(mPositionUpload));
													}
													else{
														mUploadCard.getFilesList().get(mPositionUpload).setRetryCount(0);
														mDbHandlerUpload.updateRetryCount(mUploadCard.getFilesList().get(mPositionUpload));
														mUploadCard.setStatus(DictationStatus.TIMEOUT.getValue());
													}
													isNotServerError = true;
													mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
												}
												isUploadThreadExecuting=false;
												onUpdateList();
												break;
											}
											else
											{
												mUploadCard.setStatus(DictationStatus.RETRYING1.getValue());
												mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
												if(!hasUploadCounting&&!hasQueryCounting)
												{
													hasUploadCounting=true;
													onHttpRetrying();
												}
												hasUploadCounting=true;
												isUploadThreadExecuting=false;
												onUpdateList();
												break;
											}
										}
										 /*
								         * If the httpConnection isn't established due to network problem, then change status of 
								         * dictation to 'Retrying' state. 
								         */
										else
										{
											mUploadCard.setStatus(DictationStatus.RETRYING2.getValue());
											mDbHandlerUpload.updateDictationStatus(mUploadCard.getDictationId(),mUploadCard.getStatus());
											isUploadThreadExecuting=false;
											onUpdateList();
											break;
										}
									}
								}
							}
							else
							{
								isUploadThreadExecuting=false;
								mUploadCursor.close();
								mGroupOfDictationCard=null;
								mPreviousUploadedTime = null;
								mUploadCard=null;
								mUploadCursor=null;
								mUploadThread=null;
								mFileUpload=null;
								mFilesCard=null;
								return;
							}
						}
					} 
					catch (Exception e) {
						//e.printStackTrace();
						isUploadThreadExecuting=false;
						onUpdateList();
						return;
					}
				}
			};
			mUploadThread.start();
	}
	
	/**
	 *  This method is used to query the status of uploaded dictations.
	 **/
	private void onQueryDictation(){
		mQueryThread = new Thread()
		{
			@Override
			public void run() 
			{
				try
				{
					if(!isQueryExecuting)
					{
						isQueryExecuting=true;
						mQueryCursor=mDbHandlerQuerying.getQueryDictation();
						/*
						 * checks whether any dictations are available in query queue.
						 */
						if(mQueryCursor.getCount()==1&&mQueryCursor.moveToFirst())
						{
							mQueryResult=null;
							do{
								mQueryCard=mDbHandlerQuerying.getSelectedDictation(mQueryCursor);
								mQueryCard.setFilesList(mDbHandlerQuerying.getFileList(mQueryCard.getDictationId()));
							}while(mQueryCursor.moveToNext());
							mQueryCursor.close();
							if(mQueryCard.getFilesList()!=null&&mQueryCard.getFilesList().size()>0)
							{
								mPositionQuery=0;
								for(mPositionQuery=0;mPositionQuery<mQueryCard.getFilesList().size();mPositionQuery++)
								{
									isQueryExecuting=true;
									if(mPositionQuery>0)
									{
										mQueryCard=mDbHandlerQuerying.getDictationCardWithId(mQueryCard.getDictationId());
										mQueryCard.setFilesList(mDbHandlerQuerying.getFileList(mQueryCard.getDictationId()));
									}
									if(mQueryCard.getStatus()==DictationStatus.SENT.getValue()||mQueryCard.getStatus()==DictationStatus.SENDING_FAILED.getValue()
											||mQueryCard.getStatus()==DictationStatus.TIMEOUT.getValue())
									{
										mDbHandlerQuerying.updateMainStatusByDicts(0,mQueryCard.getDictationId());
										isQueryExecuting=false;
										onUpdateList();
										break;
									}
									if(mDbHandlerQuerying.getSplitQueryCount(mQueryCard.getDictationId())==
											mDbHandlerQuerying.getSplitUploadPendingCount(mQueryCard.getDictationId()))
									{
										isQueryExecuting=false;
										mDbHandlerQuerying.updateMainStatusByDicts(200,mQueryCard.getDictationId());
										onUpdateList();
										break;
									}
									if(mQueryCard.getFilesList().get(mPositionQuery).getTransferStatus()==0||
											mQueryCard.getFilesList().get(mPositionQuery).getTransferId().equalsIgnoreCase(""))
									{
										if(mPositionQuery<(mQueryCard.getFilesList().size()-1))
											continue;
										else
										{
											if(mQueryCard.getFilesList().get(mPositionQuery).getTransferId().equalsIgnoreCase(""))
												mDbHandlerQuerying.updateMainStatusByDicts(200,mQueryCard.getDictationId());
											else
											{
												if(mDbHandlerQuerying.getCheckAlreadySentQueryCount(mQueryCard.getDictationId())<1)
												{
													mQueryCard.setSentDate(mDMApplication.getDeviceTime());
													mQueryCard.setIsResend(0);
													mDbHandlerQuerying.updateStatusAndSentDate(mQueryCard);
													mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),DictationStatus.SENT.getValue());
												}
												else
													mDbHandlerQuerying.updateQueryPriority(mQueryCard.getDictationId());
											}
											isQueryExecuting=false;
											mQueryCard=null;
											onUpdateList();
											break;
										}
									}
									try 
									{
										mHttpClient = null;
										mHttpGet = null;
										mHttpParameters= new BasicHttpParams();
										/*
										 * Set default parameters and socket factory to the httpCliet to handle SSL Exception and 
										 * security exception.
										 */
										
										HttpConnectionParams.setConnectionTimeout(mHttpParameters, mTimeLimit);
									    HttpConnectionParams.setSoTimeout(mHttpParameters, mTimeLimit);
										
										WebserviceHandler webserviceHandler = new WebserviceHandler();
										// create SSLContext instance
										SSLContext sslContext = webserviceHandler.createSSLContext();
										// create custom socket factory instance
										OlyDMSSLSocketFactory socketFactory = new OlyDMSSLSocketFactory(sslContext, new BrowserCompatHostnameVerifier());
										//DefaultHttpClient httpClient = new DefaultHttpClient(/*mSafeClientConnManager,*/httpParameters);
									    DefaultHttpClient mHttpClient = (DefaultHttpClient) webserviceHandler.createHttpClient(socketFactory,mHttpParameters);
									    
									    mHttpGet= new HttpGet();		        
//								        request.setURI(new URI("http://olyns.cloudapp.net/smartphone/AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA/dictation/"
//								        		+mQueryCard.getFilesList().get(0).getTransferId()+"/status"));  
								        mHttpGet.setParams(mHttpParameters);
								        mHttpParameters=null;
								        mURIQuerying = new URI(mUrl+"/"+mPrefUUID+"/dictation/"
								        		+mQueryCard.getFilesList().get(mPositionQuery).getTransferId()+"/status");
								        //System.out.println("query>>>"+mUrl+"/"+mPrefUUID+"/dictation/"
								        		//+mQueryCard.getFilesList().get(mPositionQuery).getTransferId()+"/status");
								        mHttpGet.setURI(mURIQuerying);
								        mHttpGet.setHeader("Host", mURIQuerying.getHost());
								        mHttpGet.addHeader("X-ILS-Authorization","Basic "+mBase64Value.trim());
								        /*
								         * checks whether any network connection is available or not, except flash air connection.
								         */
								        if(DMApplication.isONLINE())
								        {
								        	mQueryResult = "";
								        	mHttpResponse = mHttpClient.execute(mHttpGet);
									        mQueryResult = EntityUtils.toString(mHttpResponse.getEntity());
									        mHttpResponse=null;
									        mQueryXmlParser=new DictationQueryXmlParser(mQueryResult);
									        mQueryAttributeObjects=mQueryXmlParser.parse(getLanguage());
										}
									}catch (final Exception e) {
								    	mQueryResult="http_error";
									}
									try
									{
										mQueryCard=mDbHandlerQuerying.getDictationCardWithId(mQueryCard.getDictationId());
										mQueryCard.setFilesList(mDbHandlerQuerying.getFileList(mQueryCard.getDictationId()));
										if(mQueryCard.getStatus()==DictationStatus.TIMEOUT.getValue())
										{
											isQueryExecuting=false;
											onUpdateList();
											break;
										}
									}catch (Exception e) {}
									if(mQueryResult!=null)
									{
										if(mUploadCard!=null)
										{
											if(mUploadCard.getDictationId()==mQueryCard.getDictationId()&&mUploadCard.getStatus()==DictationStatus.TIMEOUT.getValue())
											{
												isQueryExecuting=false;
												onUpdateList();
												break;
											}
										}
										
										if(mQueryResult.equalsIgnoreCase("http_error")||mQueryResult.equalsIgnoreCase(""))
										{
											if(!hasUploadCounting&&!hasQueryCounting)
											{
												hasQueryCounting=true;
												onHttpRetrying();
											}
											hasQueryCounting=true;
											if(mQueryCard.getStatus()==DictationStatus.WAITING_TO_SEND1.getValue())
											{
												mQueryCard.setStatus(DictationStatus.RETRYING1.getValue());
												mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),mQueryCard.getStatus());
											}
											else
												mDbHandlerQuerying.updateSplitInternalStatus(mQueryCard.getDictationId(),DictationStatus.RETRYING1.getValue());
											isQueryExecuting=false;
											onUpdateList();
											break;
										}
										else 
										{
											mQueryResult=mQueryAttributeObjects.get(0).getResultCode();
											/*
											 * Check the response from ILS.
											 */
											if(mQueryResult.equalsIgnoreCase("2000"))
											{
												mQueryCard.getFilesList().get(mPositionQuery).setTransferId(mQueryAttributeObjects.get(0).getTranferId());
												mQueryCard.getFilesList().get(mPositionQuery).setTransferStatus(Integer.parseInt(mQueryAttributeObjects.get(0).getStatusCode()));
												mDbHandlerQuerying.updateFileStatus(mQueryCard.getFilesList().get(mPositionQuery));
												
												if(mQueryCard.getFilesList().get(mPositionQuery).getTransferStatus()==1)
												{
													/*
													 * Start 30 seconds timer if not started.
													 */
													if(!isQueryTimerStarted)
													{
														isQueryTimerStarted=true;
														onQueryRetrying();
													}
													/*
													 * Check whether the dictation has split files or not.
													 */
													if(mQueryCard.getStatus()==DictationStatus.WAITING_TO_SEND1.getValue())
													{
														if(mDbHandlerQuerying.getSplitUploadPendingCount(mQueryCard.getDictationId())==0)
														{
															mQueryCard.setStatus(DictationStatus.WAITING_TO_SEND2.getValue());
															mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),mQueryCard.getStatus());
														}
													}
													else
														mDbHandlerQuerying.updateSplitInternalStatus(mQueryCard.getDictationId(),DictationStatus.WAITING_TO_SEND2.getValue());
													/*
													 * For split condition.
													 */
													if((mQueryCard.getFilesList().size()-(mPositionQuery+1))!=mDbHandlerQuerying.getSplitUploadPendingCount(mQueryCard.getDictationId()))
														continue;
													else
													{
														mDbHandlerQuerying.updateQueryPriority(mQueryCard.getDictationId());
														isQueryExecuting=false;
														onUpdateList();
														return;
													}
												}
												else if(mQueryCard.getFilesList().get(mPositionQuery).getTransferStatus()==2)
												{
													mQueryCard.setStatus(DictationStatus.SENDING_FAILED.getValue());
													mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),mQueryCard.getStatus());
													mDbHandlerQuerying.updateMainStatusByDicts(0,mQueryCard.getDictationId());
													onStopStreamingWhileQueryChange();
												}
												else
												{
													/*
													 * Checks whether the splits are uploaded or not.
													 */
													if(mDbHandlerQuerying.getSplitQueryPendingCount(mQueryCard.getDictationId())==mQueryCard.getFilesList().size())
													{
														mQueryCard.setStatus(DictationStatus.SENT.getValue());
														mQueryCard.setSentDate(mDMApplication.getDeviceTime());
														mQueryCard.setIsResend(0);
														mDbHandlerQuerying.updateStatusAndSentDate(mQueryCard);
														mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),mQueryCard.getStatus());
														try
														{
															if(mQueryCard.getIsFlashAir()==0)
															{
																mPathQuery=DMApplication.DEFAULT_DIR+"/Dictations/"
																		+mQueryCard.getSequenceNumber()+"/";
																mFileDir= new File(mPathQuery);
																mPathQuery=mPathQuery+mQueryCard.getDictFileName().trim();
																/*
																 * Remove all converted files from directory.
																 */
															    if (mFileDir.isDirectory()) {
															    	mChildren= mFileDir.list();
															        for (int i=0; i<mChildren.length; i++) {
															        	mFileQuery = new File(mFileDir, mChildren[i]);
															        	if(!mFileQuery.getAbsolutePath().trim().equalsIgnoreCase(mPathQuery.trim()+".wav")&&
															        			!mFileQuery.getAbsolutePath().trim().equalsIgnoreCase(mPathQuery.trim()+".jpg"))
															        		mFileQuery.delete();
															        }
															        mFileQuery=null;
															    }
															    mChildren=null;
															    mFileDir=null;
															}
															mPathQuery=null;
														}
														catch (Exception e) {}
													}
													else
													{
														if(mPositionQuery<=(mQueryCard.getFilesList().size()-2))
															continue;
														else
															mDbHandlerQuerying.updateQueryPriority(mQueryCard.getDictationId());
													}
												}
											}
											/*
											 * when any critical error happens, set all dictations in 'Sending' process 
											 * to 'Timeout' except the dictations in the conversion queue.
											 */
											else if(mQueryResult.equalsIgnoreCase("4000")||mQueryResult.equalsIgnoreCase("4006")||
													mQueryResult.equalsIgnoreCase("4007")||mQueryResult.equalsIgnoreCase("4008")||
													mQueryResult.equalsIgnoreCase("4009")||mQueryResult.equalsIgnoreCase("5002"))
											{
												mErrorMessage=null;
												if(!mDMApplication.isTimeOutDialogOnFront())
												{
													if(mQueryResult.equalsIgnoreCase("5002"))
														mErrorMessage=mQueryXmlParser.getMessage();
													mDMApplication.setErrorCode(mQueryResult);
													onTimeOutDilaoge();
												}
												if(mQueryResult.equalsIgnoreCase("4007"))
												{
													mSharedPreferences =getSharedPreferences(PREFS_NAME, 0);
						                 			mEditor=mSharedPreferences.edit();
						                 		    mEditor.putString("Activation" ,"Not Activated");
						                 		    mEditor.commit();
												}
												onMoveAllToTimeOut();
											}
											/*
											 * When any other error occurs such as, 'bad request','fail to activate', then the status of
											 *  dictation is changed to 'Timeout'.
											 */
											else if(mQueryResult.equalsIgnoreCase("4001")||mQueryResult.equalsIgnoreCase("4002")||
													mQueryResult.equalsIgnoreCase("4003")||mQueryResult.equalsIgnoreCase("4004")||
													mQueryResult.equalsIgnoreCase("4005"))
											{
												mQueryCard.getFilesList().get(mPositionQuery).setTransferId("");
												mQueryCard.getFilesList().get(mPositionQuery).setTransferStatus(-1);
												mQueryCard.setStatus(DictationStatus.TIMEOUT.getValue());
												mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),mQueryCard.getStatus());
												onStopStreamingWhileQueryChange();
											}
											isQueryExecuting=false;
											onUpdateList();
											break;
										}
									}
									/*
									 * When there is no network connection.
									 */
									else
									{
										if(mQueryCard.getStatus()==DictationStatus.WAITING_TO_SEND1.getValue())
										{
											mQueryCard.setStatus(DictationStatus.RETRYING2.getValue());
											mDbHandlerQuerying.updateDictationStatus(mQueryCard.getDictationId(),mQueryCard.getStatus());
										}
										else
											mDbHandlerQuerying.updateSplitInternalStatus(mQueryCard.getDictationId(),DictationStatus.RETRYING2.getValue());
										isQueryExecuting=false;
										onUpdateList();
										break;
									}
								}
							}
							else
							{
								isQueryExecuting=false;
								onUpdateList();
								return;
							}
						}
						else
						{
							mQueryCursor.close();
							mQueryCursor=null;
							mQueryCard=null;
							isQueryExecuting=false;
							return;
						}
					}
				}
				catch (Exception e) {
					isQueryExecuting=false;
					onUpdateList();
					return;
				}
			}
		};
		mQueryThread.start();
	}
	
	
	/**
	 * Stop the streaming of file to the server, if any error occurs during querying 
	 * of split files.
	 */
	private void onStopStreamingWhileQueryChange()
	{
		try
		{
			if(mUploadCard!=null&&mQueryCard!=null)
			{
				if(mUploadCard.getDictationId()==mQueryCard.getDictationId())
				{
					isCurrentUploadingIsGoesToTimeOut=true;
					mUploadCard=null;
					if(isInputStreaming)
						mInputStream.close();
					if(isOutputStreaming)
						mOutputStream.close();
					mHttpsURLConnection.disconnect();
				}
			}
		}
		catch (Exception e) {}
		isInputStreaming=false;
		isOutputStreaming=false;
	}
	
	@Override
	public void onDestroy() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopForeground(true); //true will remove notification
		}
		unregisterReceiver(mMessengerReceiver);
		super.onDestroy();
	}
	
	/**
	 * BroadcastReceiver which is used to communicate between Service and other components.
	 */
	private BroadcastReceiver mMessengerReceiver=new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			try{
				getSettingsAttribute();
				mDMApplication=(DMApplication)getApplication();
				mDMApplication.setUploadServiceContext(ConvertAndUploadService.this);
				if(intent.getBooleanExtra("isWantToUpdate",false))
					onUpdateList();
				else if(intent.getBooleanExtra("isWantWholeToTimeOut",false))
					onMoveAllToTimeOut();
				else if(intent.getBooleanExtra("isWantToCheckStreaming",false))
					onResendDictations();
			}
			catch (Exception e) {}
		}
	};
	
	/**
	 * Creates UTF String for single dictation.
	 * 
	 * @return UTF String
	 */
	private String getDictationUTFString()
	{
		mJobData="";
		mJobData=mJobData.trim()+"<jobdata>";
		if(!mFilesCard.getFileName().trim().equalsIgnoreCase(""))
			mJobData=mJobData+"<dictation>"+mFilesCard.getFileName().trim()+"."+
			DMApplication.getDssType(mUploadCard.getDssVersion())+"</dictation>";
		if(mUploadCard.getIsThumbnailAvailable()!=0&&mFilesCard.getFileIndex()==0)
			mJobData=mJobData+"<image>"+mUploadCard.getDictationName().trim()+".jpg</image>";
		mJobData=mJobData.trim()+"</jobdata>";
		mDictString="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<ils-request appversion=\""+mDMApplication.getApplicationVersion()+"\">"
		        + "<delivery>"+mUploadCard.getDeliveryMethod()+"</delivery>"+
                mJobData+
                "</ils-request>";
		mJobData="";
		return mDictString;
	}
	
	/**
	 * Creates UTF String for multiple dictations.
	 * 
	 * @return UTF String
	 */
	private String getMultipleDictationsUTFString(ArrayList<DictationCard> dictationCards)
	{
		mDictString="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<ils-request appversion=\""+mDMApplication.getApplicationVersion()+"\">"
		        + "<delivery>"+dictationCards.get(0).getDeliveryMethod()+"</delivery>";
		mJobData="";
		for(int i=0;i<dictationCards.size();i++)
		{
			mJobData=mJobData.trim()+"<jobdata>";
			String fileName=dictationCards.get(i).getDictationName();
			if(fileName!=null)
				mJobData=mJobData+"<dictation>"+fileName.trim()+"."+
						DMApplication.getDssType(dictationCards.get(i).getDssVersion())+"</dictation>";
			else
				continue;
			if(dictationCards.get(i).getIsThumbnailAvailable()==1)
				mJobData=mJobData+"<image>"+fileName.trim()+".jpg</image>";
			mJobData=mJobData.trim()+"</jobdata>";
		}
		mDictString=mDictString+mJobData+"</ils-request>";
		return mDictString;
	}
	
	/**
	 * Calculate total size of uploading dictation file with header.
	 * 
	 * @param dCard dictation
	 * @param filePathPosition split position
	 * 
	 * @return size of dictation
	 */
	private int getDSSDataSize(DictationCard dCard,int filePathPosition)
	{
		int size=0;
		try 
		{
			mDictString="\n"
						+mBoundary
						+"\n"
						+"Content-Disposition: attachment; filename="+
							'"'+dCard.getFilesList().get(filePathPosition).getFileName()+
							"."+DMApplication.getDssType(dCard.getDssVersion())+'"'
						+"\n"
						+"Content-Type: application/x-olydss"
						+"\n"
						+"Content-Transfer-Encoding: binary"
						+"\n\n";
			size=mDictString.getBytes().length;
			mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+dCard.getSequenceNumber()+"/"+
					dCard.getFilesList().get(filePathPosition).getFileName()+"."+
					DMApplication.getDssType(dCard.getDssVersion()));
			mFileInputStream = new FileInputStream(mFileUpload);
			mFileUpload=null;
			mBytesAvailable = mFileInputStream.available();
			dCard.getFilesList().get(filePathPosition).setFileSize(mBytesAvailable);
			size=size+mBytesAvailable;
			mBytesAvailable=0;
			mFileInputStream.close();
		} 
		catch(Exception e)
		{
			size=0;
		}
		return size;
	}
	
	/**
	 * Calculate total size of image with header.
	 * 
	 * @param dCard dictation
	 * 
	 * @return size of image
	 */
	private int getImageDataSize(DictationCard dCard)
	{
		int size=0;
		try 
		{
			mDictString="\n"
						+mBoundary
						+"\n"
						+"Content-Disposition: attachment; filename="+
							'"'+dCard.getDictationName()+".jpg"+'"'
						+"\n"
						+"Content-Type: image/jpeg"
						+"\n"
						+"Content-Transfer-Encoding: binary"
						+"\n\n";
			size=mDictString.getBytes().length;
			mFileUpload=new File(DMApplication.DEFAULT_DIR+"/Dictations/"+dCard.getSequenceNumber()+"/"+
					dCard.getDictFileName()+".jpg");
			mFileInputStream = new FileInputStream(mFileUpload);
			mFileUpload=null;
			mBytesAvailable = mFileInputStream.available();
			size=size+mBytesAvailable;
			mBytesAvailable=0;
			mFileInputStream.close();
		}
		catch (Exception e) 
		{
			size=0;
		}
		return size;
	}
	
	/**
	 * Listener to update the status of dictations in recordings list.
	 * 
	 * @version 1.0.1
	 */
	public interface UploadStatusChangeListener {
		/**
		 * Invokes when status of the Dictation changes.  
		 */
		public void onUploadStatusChanged();
	}
	
	/**
	 * @see com.olympus.dmmobile.network.NetworkConnectivityListener.RetryUploadListener#onRetryUploadListener()
	 */
	@Override
	public void onRetryUploadListener() {
		try
		{
			if(mQueryCard==null)
				isQueryExecuting=false;
			if(DMApplication.isONLINE())
			{
				getSettingsAttribute();
				mDbHandlerUpload.updateUploadRetryDictation(DictationStatus.RETRYING2.getValue());
				mDbHandlerQuerying.updateQuerying5MinRetryDictation(DictationStatus.RETRYING2.getValue());
				if(!hasUploadCounting)
					mDbHandlerUpload.updateUploadRetryDictation(DictationStatus.RETRYING1.getValue());
				if(!hasQueryCounting)
					mDbHandlerQuerying.updateQuerying5MinRetryDictation(DictationStatus.RETRYING1.getValue());
				if(!isQueryTimerStarted)
					mDbHandlerQuerying.updateQuery30SecRetryDictation();
			}
		}
		catch (Exception e) {}
		onUpdateList();
	}
	
	/**
	 * initiate all threads and change the status of dictation in recordings list. 
	 */
	private void onUpdateList()
	{
		try
		{
			if(!DMApplication.isONLINE())
				mDMApplication.setDictateUploading(true);
			/**
			 * Check the current view of application is Recordings list or not.
			 */
			if(mDMApplication.getContext() instanceof DMActivity&&mDMApplication.getTabPos()>0)
			{
				mDMApplication.setUploadServiceContext(ConvertAndUploadService.this);
				mStatusChangeListener = (UploadStatusChangeListener)mDMApplication.getContext();
				mStatusChangeListener.onUploadStatusChanged();
			}
			System.gc();
			if(!isQueryExecuting)
				onQueryDictation();
			if(!isUploadThreadExecuting)
				onUploadFile();
			if(!isConvertExecuting&&!mDMApplication.isWaitConvertion())
				onConvertFile();
		}
		catch (Exception e) {}
	}
	
	/**
	 * shows error dialog
	 */
	private void onTimeOutDilaoge()
	{
		try
		{
			if(!mDMApplication.isTimeOutDialogOnFront())
			{
				mDMApplication.setTimeOutDialogOnFront(true);
				if(mDMApplication.getContext() instanceof SplashscreenActivity)
					mDMApplication.setWantToShowDialog(true);
				else
				{
					mBaseIntent = new Intent(mDMApplication, CustomDialog.class);
					if(mErrorMessage!=null&&mDMApplication.getErrorCode().equalsIgnoreCase("5002"))
						mDMApplication.setErrorMessage(mErrorMessage);
					mBaseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(mBaseIntent);
					mBaseIntent=null;
				}
			}
		}
		catch (Exception e) {}
	}
	
	/**
	 * This method is used to change the status of all dictations that are currently sending to 'TIMEOUT' when any critical 
	 * error occurs
	 */
	private void onMoveAllToTimeOut()
	{
		try {
			mDbHandlerUpload.updateGroupOfStatusToTimeout(mConvertingGroupId);
			isUploadThreadExecuting = false;
			isQueryExecuting = false;
			if(mUploadCard!=null&&mQueryCard!=null)
				if(mUploadCard.getDictationId()==mQueryCard.getDictationId())
					isCurrentUploadingIsGoesToTimeOut=true;
			onStopStreamingWhileQueryChange();
			mUploadCard=null;
			mQueryCard=null;
			onUpdateList();
		}
		catch (Exception e) {}
	}
	
	/**
	 * To get the current language of application.
	 * 
	 * @return current language
	 */
	private String getLanguage(){
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		switch (Integer.parseInt(mSharedPreferences.getString(getResources().getString(R.string.language_key),"1"))) {
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
	
	/**
	 * starts the timer when any HTTP error occurs (5 minute)
	 */
	private void onHttpRetrying()
	{
		mHandler5Minute.postDelayed(new Runnable() {
		    public void run() {
		    	try
				{
					if(hasUploadCounting&&DMApplication.isONLINE())
						mDbHandlerUpload.updateUploadRetryDictation(DictationStatus.RETRYING1.getValue());
					else if(hasUploadCounting)
						mDbHandlerUpload.updateWholeUploadToRetryDictation();
					if(hasQueryCounting&&DMApplication.isONLINE())
						mDbHandlerQuerying.updateQuerying5MinRetryDictation(DictationStatus.RETRYING1.getValue());
					else if(hasQueryCounting)
						mDbHandlerQuerying.updateWholeQueryToRetryDictation();
				}
				catch (Exception e) {}
				hasUploadCounting=false;
				hasQueryCounting=false;
				onUpdateList();
			}
		},mTimeLimitForHttpError);
	}
	
	/**
	 * This method is used to start a timer (30 seconds) for Query dictation.
	 */
	private void onQueryRetrying()
	{
		mHandler30Seconds.postDelayed(new Runnable() {
		    public void run() {
		    	try
				{
					if(DMApplication.isONLINE())
						mDbHandlerQuerying.updateQuery30SecRetryDictation();
					else
						mDbHandlerQuerying.updateWholeQueryToRetryDictation();
				}
				catch (Exception e) {}
				isQueryTimerStarted=false;
				onUpdateList();
			}
		},mTimeLimitForWaitingToSend);
	}
	
	/**
	 * Reset Uploading time to handle idle state.
	 */
	private void onChangeStreamTime() {
		try {
			mDate = new Date();
			mDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
			mPreviousUploadedTime = mDateFormat.format(mDate);
		}
		catch(Exception e) {}
	}
	
	/**
	 * This method is used to re-send dictation when user taps send all button in outbox tab.
	 */
	private void onResendDictations() {
		try {
			if(mPreviousUploadedTime!=null) {
				mDate = new Date();
				if(mDateFormat.parse(mDateFormat.format(mDate)).getTime()>mDateFormat.parse(mPreviousUploadedTime).getTime())
					mIdleTime = mDateFormat.parse(mDateFormat.format(mDate)).getTime()-mDateFormat.parse(mPreviousUploadedTime).getTime();
				else
					mIdleTime = (mDateFormat.parse(mDateFormat.format(mDate)).getTime()*60*1000)-mDateFormat.parse(mPreviousUploadedTime).getTime();
				/*
				 * To check the uploading dictations is in idle state(more than 40 Seconds).
				 */
				if(mIdleTime>(2*mTimeLimit)) {
					if(mUrlConnection!=null&&mUploadCard!=null&&isUploadThreadExecuting)
					{
						if(isInputStreaming||isOutputStreaming){
							hasIdleStateRealeased = true;
							isCurrentUploadingIsGoesToTimeOut = true;
							isInputStreaming=false;
							isOutputStreaming=false;
							if(mUploadCard.getGroupId()<1)
								mDbHandlerUpload.updateGroupOfDictationsId(mUploadCard.getGroupId(),DictationStatus.SENDING.getValue(),true);
							else
								mDbHandlerUpload.updateGroupOfDictationsId(mUploadCard.getGroupId(),DictationStatus.SENDING.getValue(),false);
							try {
								if(isInputStreaming)
									mInputStream.close();
								else if(isOutputStreaming)
									mOutputStream.close();
								if(mUrlConnection instanceof HttpsURLConnection)
									mHttpsURLConnection.disconnect();
								else
									mHttpURLConnection.disconnect();
							}
							catch(Exception e)
							{
								isUploadThreadExecuting = false;
							}
							isUploadThreadExecuting = false;
							mUploadCard=null;
						}
						else if(mHttpsURLConnection!=null&&mUrlConnection instanceof HttpsURLConnection)
							mHttpsURLConnection.disconnect();
						else if(mHttpURLConnection!=null&&mUrlConnection instanceof HttpURLConnection)
							mHttpURLConnection.disconnect();
					}
				}
			}
		}
		catch(Exception e) {}
	}
}
