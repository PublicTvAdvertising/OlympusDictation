package com.olympus.dmmobile;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import com.olympus.dmmobile.network.NetworkConnectivityListener;

/**
 * DMApplication is the class which maintain global application state.
 * This class can be accessed globally from everywhere in the application.
 * @version 1.0.1
 */
public class DMApplication extends Application{
	
	private int mNetWorkId = -1;
	private String FlashAirSSID;
	private String PreviousNetworkSSID;
	private boolean FlashAirState;
	private static boolean ONLINE=false;
	
	public static File DEFAULT_DIR;
	public static final String DEFAULT_DICTATIONS_DIR = "/Dictations/";
	private Context context;
	private Context uploadServiceContext=null;
	private boolean dictateUploading=false;
	private int tabPos;
	private NetworkConnectivityListener networkConnectivityListener=null;
	private String dmUrl;
	
	static final int	DSSFORMAT_DSS_SP = 0;
	static final int	DSSFORMAT_DS2_SP = 1;
	static final int	DSSFORMAT_DS2_QP = 2;
	
	static final int	DSS_ENCRYPTION_STANDARD = 0;
	static final int	DSS_ENCRYPTION_HIGH = 1;
	static final boolean DSS_ENCRYPTION_ENABLED=true;
	static final boolean DSS_ENCRYPTION_DISABLED=false;
	
	static final int	DSS_PRIORITY_NORMAL = 0;	
	static final int	DSS_PRIORITY_HIGH = 1;	
	
	public final int MINIMUM_SIZE_REQUIRED = 96000;//3 Seconds PCM file with 16 KHz(previous -- 32000 one second PCM file with 16 KHz && 1 MB -- 1073741824)
	
	public static final String ACTIVITY_MODE_TAG="activity_mode";
	public static final String START_MODE_TAG="StartMode";
	public static final String MODE_NEW_RECORDING="new";
	public static final String MODE_EDIT_RECORDING="edit";
	public static final String MODE_REVIEW_RECORDING="review";
	public static final String MODE_COPY_RECORDING="copy";
	public static final String MODE_LAUNCH_RECORDING="launch";
	public static final String DICTATION_CARD_KEY="dict_card";
	public static final String DICTATION_ID="dict_id";
	public static final String EDIT_COPY_FORCE_QUIT="edit_copy_forcequit";
	public static final String EDIT_COPY_DESTINATION="edit_copy_destination";
	private boolean timeOutDialogOnFront = false;
	private boolean isWantToShowDialog = false;
	private String ErrorCode = "";
	private String ErrorMessage = "";
	private int setCurrentLanguage;
	private boolean isPriorityOn;
	private boolean isPropertyClicked;
	private boolean isOnEditState=true;
	public boolean onSetPending = false;
	private boolean editMode;
	public String passMode;
	private static File file=null,fileDir=null;
	private boolean isRecordingsClickedDMActivity=false;
	private boolean waitConvertion=false;
	private static double value=0;
	private static double fileSize=0;
	private static double duration=0;
	private long fileLength=0;
	private int Alertvalue=0;
	private DatabaseHandler databaseHandler=null;
	public String worktypeSwiped = null;
	public boolean lastDictMailSent = false;
	public boolean outBoxFlag = false;
	private Intent mBaseIntent = null;
	public boolean newCreated = false;
	public int fromWhere ;
	public boolean flashair = false;
	public boolean isExecuted = false;
	private String mApplicationVersion = "1.0.1";
	private int mCurrentGroupId = 0;
	private int deletedId = 0;
	private final String ilsRequestAppVersion = "1.1.0";
	private String mResultCode = null;
	private String mMessage = null;
	private StatFs mStatFs = null;

	public int getNetWorkId() {
		return mNetWorkId;
	}

	public void setNetWorkId(int mNetWorkId) {
		this.mNetWorkId = mNetWorkId;
	}
	
	public String getIlsRequestAppVersion() {
		return ilsRequestAppVersion;
	}

	public String getApplicationVersion() {
		return mApplicationVersion;
	}

	public void setApplicationVersion(String mApplicationVersion) {
		this.mApplicationVersion = mApplicationVersion;
	}

	public DatabaseHandler getDatabaseHandler() {
		return databaseHandler;
	}

	public boolean isWaitConvertion() {
		return waitConvertion;
	}

	public void setWaitConvertion(boolean waitConvertion) {
		this.waitConvertion = waitConvertion;
	}
	public boolean isRecordingsClickedDMActivity() {
		return isRecordingsClickedDMActivity;
	}

	public void setRecordingsClickedDMActivity(boolean isRecordingsClickedDMActivity) {
		this.isRecordingsClickedDMActivity = isRecordingsClickedDMActivity;
	}
	public boolean isEditMode() {
		return editMode;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	public boolean isOnEditState() {
		return isOnEditState;
	}

	public void setOnEditState(boolean isOnEditState) {
		this.isOnEditState = isOnEditState;
	}

	public boolean isPropertyClicked() {
		return isPropertyClicked;
	}

	public void setPropertyClicked(boolean isPropertyClicked) {
		this.isPropertyClicked = isPropertyClicked;
	}

	public boolean isPriorityOn() {
		return isPriorityOn;
	}

	public void setPriorityOn(boolean isPriorityOn) {
		this.isPriorityOn = isPriorityOn;
	}

	public String getErrorCode() {
		return ErrorCode;
	}

	public void setErrorCode(String errorCode) {
		ErrorCode = errorCode;
	}

	public String getErrorMessage() {
		return ErrorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		ErrorMessage = errorMessage;
	}
	
	public boolean isWantToShowDialog() {
		return isWantToShowDialog;
	}

	public void setWantToShowDialog(boolean isWantToShowDialog) {
		this.isWantToShowDialog = isWantToShowDialog;
	}
	
	public boolean isTimeOutDialogOnFront() {
		return timeOutDialogOnFront;
	}

	public void setTimeOutDialogOnFront(boolean timeOutDialogOnFront) {
		this.timeOutDialogOnFront = timeOutDialogOnFront;
	}
	
	public int getTabPos() {
		return tabPos;
	}
	public void setTabPos(int tabPos) {
		this.tabPos = tabPos;
	}

	public boolean isDictateUploading() {
		return dictateUploading;
	}
	public void setDictateUploading(boolean dictateUploading) {
		this.dictateUploading = dictateUploading;
	}
	public Context getUploadServiceContext() {
		return uploadServiceContext;
	}
	public void setUploadServiceContext(Context uploadServiceContext) {
		DMApplication.this.uploadServiceContext = uploadServiceContext;
	}
	public Context getContext() {
		return context;
	}
	public void setContext(Context context) {
		DMApplication.this.context = context;
	}
	public boolean isFlashAirState() {
		return FlashAirState;
	}
	public void setFlashAirState(boolean FlashAirState) {
		this.FlashAirState = FlashAirState;
	}
	public String getFlashAirSSID() {
		return FlashAirSSID;
	}

	public void setFlashAirSSID(String flashAirSSID) {
		FlashAirSSID = flashAirSSID;
	}

	public String getPreviousNetworkSSID() {
		return PreviousNetworkSSID;
	}

	public void setPreviousNetworkSSID(String previousNetworkSSID) {
		this.PreviousNetworkSSID = previousNetworkSSID;
	}
	
	@Override
	public void onCreate() {
		System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1)
			System.setProperty("http.keepAlive", "false");
		super.onCreate();
		try {
			mApplicationVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (Exception e1) {
		}
		databaseHandler=new DatabaseHandler(getApplicationContext());
		FlashAirState=false;
		/*
		 * To start a BroadcastReceiver for getting Network Connection states.
		 */
		networkConnectivityListener=new NetworkConnectivityListener();
		networkConnectivityListener.startListening(this);
		try{
			DEFAULT_DIR = new File(getExternalFilesDir(null).getAbsolutePath());
			if(DEFAULT_DIR!=null){
				if(!DEFAULT_DIR.exists())
					DEFAULT_DIR.mkdirs();
			}
		}catch(Exception e){	
		}	
		/*
		 * To start background service.
		 */
		mBaseIntent= new Intent(DMApplication.this, ConvertAndUploadService.class);
		startService(mBaseIntent);
	}
	//************* Network Connectivity**************
	public static boolean isONLINE() 
	{
		return ONLINE;
	}
	public static void setONLINE(boolean oNLINE) 
	{
		ONLINE = oNLINE;
	}
	
	public String getUrl(){
		return dmUrl;
	}
	public void setUrl(String url){
		dmUrl=url;
	}
	
	/**
	 * Get format of DSS file as string.
	 * @param version DSS version id
	 * @return DSS file format
	 */
	public static String getDssType(int version){
		switch (version) {
		case 1:
			return "DSS";
		case 10:
			return "DS2";
		case 11:
			return "DS2";
		case 111:
			return "wav";
		case 222:
			return "MP3";
		default:
			return "";
		}
	}
	
	/**
	 * Validate response code received from server.
	 * @param mResultcode The result code received from server.
	 * @return The corresponding validation message as String.
	 */
	public String validateResponse(String mResultcode)
	{
		int code=Integer.parseInt(mResultcode);
		switch(code) {
			case 4000:
				return (getContext().getString(R.string.Ils_Result_Global_Client_Error));
			case 4001:
				return (getContext().getString(R.string.Ils_Result_Bad_request));
			case 4002:
				return (getContext().getString(R.string.Ils_Result_Fail_Activate));
			case 4003:
				return (getContext().getString(R.string.ILs_Result_Another_UUID_Activated));
			case 4004:
				return (getContext().getString(R.string.Ils_Result_Unauthorized));
			case 4005:
				return (getContext().getString(R.string.Ils_Result_Resource_Not_Found));
			case 4006:
				return (getContext().getString(R.string.Ils_Result_Client_Update));
			case 4007:
				return (getContext().getString(R.string.Ils_Result_Not_Activated));
			case 4008:
				return (getContext().getString(R.string.Ils_Result_License_Expired));
			case 4009:
				return (getContext().getString(R.string.Ils_Result_Account_Disabled));
			case 5000:
				return (getContext().getString(R.string.Ils_Result_Global_Server_Error));
			case 5001:
				return (getContext().getString(R.string.Ils_Result_Internal_Server_error));
			case 5002:
				return (getContext().getString(R.string.Ils_Result_Service_Unavailable));
			case 1000:
				return (getContext().getString(R.string.Settings_Success));
			case 2000:
				return (getContext().getString(R.string.Settings_Success));
			case 4010:
				return (getContext().getString(R.string.Ils_Result_Code_4010));
			case 4011:
				return (getContext().getString(R.string.Ils_Result_Code_4011));
			case 4012:
				return (getContext().getString(R.string.Ils_Result_Code_4012));
			default:
				return "";
		}
	}
	/**
	 * Validate error code received from server.
	 * @param mResultcode The result code received from server.
	 * @return The corresponding error message as String.
	 */
	public String validateErrorMessage(String mResultcode)
	{
		int code=Integer.parseInt(mResultcode);
		switch (code) {
			case 4000:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 4001:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 4002:
				return (getContext().getString(R.string.Settings_Error_Correct));
			case 4003:
				return (getContext().getString(R.string.ILs_Result_Already_Activated));
			case 4004:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 4005:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 4006:
				return (getContext().getString(R.string.Ils_Result_Need_Client_Update));
			case 4007:
				return (getContext().getString(R.string.Ils_Result_Activate_Server));
			case 4008:
				return (getContext().getString(R.string.Ils_Result_License_Access_Service));
			case 4009:
				return (getContext().getString(R.string.Account_Disabled));
			case 5000:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 5001:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 5002:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 1000:
				return (getContext().getString(R.string.Settings_Success));
			case 2000:
				return (getContext().getString(R.string.Settings_Server_Connection));
			case 4010:
				return (getContext().getString(R.string.Ils_Result_No_Unused_Licence));
			case 4011:
				return (getContext().getString(R.string.Ils_Result_Invalid_Arguments));
			case 4012:
				return (getContext().getString(R.string.Ils_Result_Another_Email_Activated));
			default:
				return "";
		}
	}
	
	public String errorOnlaunch(String mResultcode){
		int code=Integer.parseInt(mResultcode);
		switch (code) {
			case 4000:
				return (getContext().getString(R.string.Ils_Result_Unexpected_Error));
			case 4006:
				return (getContext().getString(R.string.Ils_Result_Need_Client_Update));
			case 4007:
				return (getContext().getString(R.string.Ils_Result_Activate_Server));
			case 4008:
				return (getContext().getString(R.string.Ils_Result_License_Access_Service));
			case 4009:
				return (getContext().getString(R.string.Account_Disabled));
			default:
				return "";
		}
	}
	/**
	 * Formats date in yyMMdd format.
	 * @param date Date in yyyy-MM-dd HH:mm:ss format.
	 * @return Date in yyMMdd format
	 */
	public static String getFormattedDate(String date){
		date=date.trim();
		if(date!=null)
		{
			if(!date.equalsIgnoreCase(""))
			{
				String oldFormat = "yyyy-MM-dd HH:mm:ss";
			    String newFormat = "yyMMdd";
		
			    SimpleDateFormat sdf1 = new SimpleDateFormat(oldFormat);
			    SimpleDateFormat sdf2 = new SimpleDateFormat(newFormat);
		
			    try {
			        date=sdf2.format(sdf1.parse(date));
			    } catch (ParseException e) {
			        e.printStackTrace();
			    }
			}
		}
		else
			date="";
	    return date;
	}
	/**
	 * Formats time in HHmmss format.
	 * @param date Date in yyyy-MM-dd HH:mm:ss format.
	 * @return Time in HHmmss format.
	 */
	public static String getFormattedTime(String date){
		date=date.trim();
		if(date!=null)
		{
			if(!date.equalsIgnoreCase(""))
			{
				//String oldFormat = "MMM d, yyyy h:mm a";
				String oldFormat = "yyyy-MM-dd HH:mm:ss";
			    String newFormat = "HHmmss";
		
			    SimpleDateFormat sdf1 = new SimpleDateFormat(oldFormat);
			    SimpleDateFormat sdf2 = new SimpleDateFormat(newFormat);
			    
		
			    try {
			        date=sdf2.format(sdf1.parse(date));
			    } catch (ParseException e) {
			        e.printStackTrace();
			    }
			}
		}
		else
			date="";
	    return date;
	}
	
	/**
	 * Deletes files and directory of a Dictation being deleted.
	 * @param path Path of the Dictation directory.
	 * @return True if deletion success, else false.
	 */
	public static boolean deleteDir(String path) {
		fileDir = new File(path);
	    if (fileDir.isDirectory()) {
	        String[] children = fileDir.list();
	        for (int i=0; i<children.length; i++) {
	        	file = new File(fileDir, children[i]);
	        	if(file.exists())
	        		file.delete();
	        }
	    }
	    return fileDir.delete();
	}
	
	
	/**
	 * Gets device time in yyyy-MM-dd HH:mm:ss format.
	 * @return Time as String.
	 */
	public String getDeviceTime(){
		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = df.format(c.getTime());
		c = null;
		df = null;
        return date;
	}
	
	/**
	 * Get language code of the current language.
	 * @return Language code as String.
	 */
	public String getLanguage()
	 {
		 int mLanguageVal=setCurrentLanguage;
		 switch (mLanguageVal) {
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
	 * The passed date string will be localized based on the current language set in the application.
	 * @param dateString The Date passed.
	 * @return Localized date as String.
	 */
	public String getLocalizedDateAndTime(String dateString){
		try
		{
			if(dateString==null)
				return "";
			if(dateString.equalsIgnoreCase(""))
				return "";
			SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = null;
			try {
				date = oldFormat.parse(dateString);
			} catch (ParseException e1) {
				e1.printStackTrace();
				return "";
			}
			Locale locale = new Locale(getLanguage());
			SimpleDateFormat localeDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
			String localeDateString = localeDateFormat.format(date);
			oldFormat = null;
			date = null;
			locale = null;
			localeDateFormat = null;
			return localeDateString;
		}
		catch (Exception e) {
			return "";
		}
		
	}

	public int getCurrentLanguage(){
		return setCurrentLanguage;
	}
	public void setCurrentLanguage(int Lang){
		setCurrentLanguage=Lang;
	}
	
	/**
	 * To find expected size of file after conversion for a Dictation.
	 * @param type DSS file format
	 * @param fileName file name
	 * @return file size
	 */
	public static double getExpectedDSSFileSize(int type,String fileName){
		value=0;
		fileSize=0;
		duration=0;
		try
		{
			file=new File(fileName+".wav");
			if(file.exists()){
				fileSize=file.length();
				duration=(fileSize/32000)*1000;
				switch(type){
					case 1:
						value= ((double)(Math.ceil((duration / 888 * 1536 ) + 1024) / Math.pow(2, 20)));
						break;
					case 10:
						value= ((double)(Math.ceil((duration / 888 * 1536 ) + 1024) / Math.pow(2, 20)));
						break;
					case 11:
						value= ((double)(Math.ceil((duration / 4048 * 14336 ) + 1536) /Math.pow(2, 20)));
						break;
				}
			}else{
				return 0;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		file=null;
		fileSize=0;
		duration=0;
		value=value*1024*1024;
		return value;
	}
	
	/**
	 * To find size of dictation file.
	 * @param fileName file name
	 * @param type file type
	 * @return file size
	 */
	public long getFileSize(String fileName,int type){
		fileLength=0;
		file=new File(fileName+"."+getDssType(type));
		if(file.exists())
			fileLength=file.length();
		file=null;
		return fileLength;
	}
	
	/**
	 * To get file size of image file attached to dictation.
	 * @param fileName
	 * @return fileLength
	 */
	public long getImageFileSize(String fileName){
		fileLength=0;
		file=new File(fileName+".jpg");
		if(file.exists())
			fileLength=file.length();
		file=null;
		return fileLength;
	}
	public int getShowAlert() {
		return Alertvalue;
	}

	public void setShowAlert(int alert) {
		Alertvalue = alert;
	}
	
	/**
	 * Edits the Search string if it has any escape characters.
	 * @param searchString Search string entered by the user.
	 * @return Edited string.
	 */
	public String editStringHasEscape(String searchString){
		if(searchString.contains("_")){
			searchString = searchString.replace("_", "\\_");
		}else if(searchString.contains("%")){
			searchString =  searchString.replace("%", "\\%");
		}else if(searchString.contains("\\")){
			searchString =  searchString.replace("\\", "\\"+"\\");
		}else if(searchString.contains("'")){
			searchString =  searchString.replace("'", "\\'");
		}else if(searchString.contains("\"")){
			searchString =  searchString.replace("\"", "\\"+"\""+"\"");
		}
		return searchString.trim();
		
	}
	
	
	public int getCurrentGroupId() {
		return mCurrentGroupId;
	}

	public void setCurrentGroupId(int mCurrentGroupId) {
		this.mCurrentGroupId = mCurrentGroupId;
	}

	/**
	 * @return the deletedId
	 */
	public int getDeletedId() {
		return deletedId;
	}

	/**
	 * @param deletedId the deletedId to set
	 */
	public void setDeletedId(int deletedId) {
		this.deletedId = deletedId;
	}
	
	public String getResultCode() {
		return mResultCode;
	}

	public void setResultCode(String mResultCode) {
		this.mResultCode = mResultCode;
	}

	public String getMessage() {
		return mMessage;
	}

	public void setMessage(String mMessage) {
		this.mMessage = mMessage;
	}
	
	/**
	 * Calculate the remaining space in Device storage.
	 * 
	 * @return long value of space available in Device storage
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public long getAvailableDiskSpace() {
		mStatFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) 
			return ((long)mStatFs.getAvailableBlocks() * (long)mStatFs.getBlockSize());
		else 
			return ((long)mStatFs.getAvailableBlocksLong() * (long)mStatFs.getBlockSizeLong());
	}
}
