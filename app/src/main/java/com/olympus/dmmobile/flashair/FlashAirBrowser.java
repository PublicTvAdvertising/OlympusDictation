package com.olympus.dmmobile.flashair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.olympus.dmmobile.BuildConfig;
import com.olympus.dmmobile.DMActivity;
import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.DatabaseHandler;
import com.olympus.dmmobile.DictationCard;
import com.olympus.dmmobile.DictationStatus;
import com.olympus.dmmobile.FilesCard;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.recorder.DictateActivity;

/**
 * FlashAirBrowser class is used to manage retrieving file details and downloading of files from FlashAir.
 * 
 * @version 1.0.1
 */
public class FlashAirBrowser extends FragmentActivity implements FolderSelectedListener {

	private ExceptionReporter mReporter;			// Error Logger
	
	private final String HOME_URL = "http://flashair/command.cgi?op=100&DIR=/";
	private String mFragmentUrl = null;
	public static String selAll ;
    public static String filePath = "/";
	public static String mSelSsid = null;
	public static ArrayList<DownloadFiles> mSelectedList = new ArrayList<DownloadFiles>();
	public static ArrayList<String> mPaths = new ArrayList<String>();
	public static boolean selAllFlag = true;
	private Locale locale;
	private DMApplication dmApplication = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mReporter = ExceptionReporter.register(this);
		super.onCreate(savedInstanceState);
		if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}else{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.flashair_browser_fragment_layout);
		dmApplication = (DMApplication) getApplication();
		setCurrentLanguage(dmApplication.getCurrentLanguage());
		
		selAll = getResources().getString(R.string.Flashair_Label_Select_All);
		mPaths.add("/");
		if (savedInstanceState == null) {
			// Do first time initialization -- add initial fragment.
			mSelSsid = getIntent().getExtras().getString("ssid");
			Fragment newFragment = DirectoryFragment.newInstance(HOME_URL);
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(R.id.flashair_fragment_container, newFragment).commit();
		}
		else {
			mFragmentUrl = savedInstanceState.getString("url");
			mSelSsid = savedInstanceState.getString("SelSsid");
		}

	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("url", mFragmentUrl);
		outState.putString("SelSsid", mSelSsid);
	}

	@Override
	public void onNewFolderSelected(String url) {
		selAllFlag = true;
		selAll = getResources().getString(R.string.Flashair_Label_Select_All);
		mSelectedList.clear();
		addFragmentToStack(url);
		System.gc();
	}
	
	@Override
	public void onBackPressed() {
		try {
			mPaths.remove(mPaths.size()-1);
			selAllFlag = true;
			selAll = getResources().getString(R.string.Flashair_Label_Select_All);
			if(mSelectedList != null)
				mSelectedList.clear();
			System.gc();
			super.onBackPressed();
		}
		catch (Exception e) {
			finish();
		}
	}
	
	/**
	 * Used to add fragment to back stack when user taps on a folder.
	 * @param url Url to fetch the file/folder details from FlashAir.
	 */
	void addFragmentToStack(String url) {

		mFragmentUrl = url;
		// Instantiate a new fragment.
		Fragment newFragment = DirectoryFragment.newInstance(url);
		// Add the fragment to the activity, pushing this transaction
		// on to the back stack.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.flashair_fragment_container, newFragment);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.addToBackStack(null);
		ft.commit();
		

	}

	@Override
	protected void onDestroy() {
		System.gc();
		super.onDestroy();
	}
	
	
	/**
	 * It is a Fragment class to show the files/folder list retrieved from Flashair and navigates between folder list. 
	 */
	public static class DirectoryFragment extends ListFragment {

		private FolderSelectedListener listener;

		private final String HOME = "http://flashair/command.cgi?op=100&DIR=";
		private final int SEND_REQ_CODE = 100;
		private String mStrUrl;
		private String mFoldPath = "/";
		

		private URL url;
		private HttpURLConnection mHttpConnection;
		private InputStream mInputStream;
		private BufferedReader mBufferedReader;
		private Reader mReader;

		private Button btnSend;
		private ImageButton btnUpdate;
		private TextView textSsid;
		private TextView textPath;

		private DirectoryListAdapter mAdapter;
		
		private SharedPreferences mSharedPrefs;
		
		private final String PREF_NAME = "email_prefs";
		private final String PREF_KEY = "selected_client";
		
		private ArrayList<FlashAirFiles> mFilesList;
		private ArrayList<FlashAirFiles> mTempFileList;
		private ArrayList<DownloadFiles> mDownloadList;
		private ArrayList<DownloadFiles> mTempList ;
		private ArrayList<DictationCard> mDownloadedDictCards;
		private FlashAirFiles mFlashFile ;
		private boolean mConnFlag = true;
		private boolean mSeparateFlag = false;
		private int mFileCount = 0;
		private int mFileNo = 0;
		private int mSelPos = 0;
		private AlertDialog mSendAlertDialog;
		private boolean mSendOption;
		private int mServerOption;
		private long emailGroupFileSize=0;
		private boolean hasMoreThan23MB=false;
		private AlertDialog mAlertDialog;
		private DictationCard mDictcard;
		private String mAuthor;
		private File mDictationDir;
		private DatabaseHandler mDbHandler;
		private int groupId=0;
		private int seqNumber;
		private AlertDialog alertDialog=null;
		private DMApplication dmApplication=null;
		
		private final int CONVERT_MB = 1024 * 1024;
		private boolean isNew = false;
		private boolean sourceNotFound = false;

		static DirectoryFragment newInstance(String url) {
			DirectoryFragment f = new DirectoryFragment();

			// Supply url input as an argument.
			Bundle args = new Bundle();
			args.putString("url", url);
			f.setArguments(args);

			return f;
		}

		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mStrUrl = getArguments().getString("url");
			//Log.e("fragment url", mStrUrl);
			setHasOptionsMenu(true);
			dmApplication=(DMApplication)getActivity().getApplication();
			mDbHandler = dmApplication.getDatabaseHandler();
			if(savedInstanceState != null )
				mDownloadedDictCards = savedInstanceState.getParcelableArrayList("DownloadedDictCards");
			final ConnectivityManager connection_manager =
					(ConnectivityManager) getActivity().getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkRequest.Builder request = new NetworkRequest.Builder();
			request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

			connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

				@Override
				public void onAvailable(Network network) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						connection_manager.bindProcessToNetwork(network);

					}
					else
					{
						ConnectivityManager.setProcessDefaultNetwork(network);
					}
				}
			});
			}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putParcelableArrayList("DownloadedDictCards", mDownloadedDictCards);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.flashair_browser, container,
					false);
			btnSend = (Button) v.findViewById(R.id.btn_send_flashair_files);
			btnSend.setEnabled(false);
			btnUpdate = (ImageButton) v.findViewById(R.id.btn_update_files);
			textSsid = (TextView) v.findViewById(R.id.text_sel_ssid);
			textPath = (TextView) v.findViewById(R.id.text_flashair_folder_name);
			textPath.setText(mPaths.get(mPaths.size()-1));
			textSsid.setText(mSelSsid);
			btnSend.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					int mSendOptions=1;
					mDownloadedDictCards = new ArrayList<DictationCard>();
					SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(getActivity());
					mSendOptions = Integer.parseInt(sharedPref.getString(getResources().getString(R.string.send_key), "2"));
					//mSendOptions = 2;
					if(mSendOptions == 1)
						mSendOption=true;
					else
						mSendOption=false;
					//Log.e("mSendOptions  ", mSendOptions+"  option"+mSendOption);
					showSendDialog();
				}
			});
			
			btnUpdate.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					mSelectedList.clear();
					isEnableSend(mSelectedList.size());
					new ReadURLTask().execute(mStrUrl);
				}
			});

			return v;
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.activity_flash_air_main, menu);
		}
		
		@Override
		public void onPrepareOptionsMenu(Menu menu) {
			MenuItem mI = menu.findItem(R.id.menu_select_all);
			mI.setTitle(selAll);
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_select_all:
				mSelectedList.clear();
				if(mFilesList.size()>0){
					for (int i = 0; i < mFilesList.size(); i++) {

						String type = mFilesList.get(i).getType();
						if (type.equals("file")) {
							if (selAllFlag) {
								DownloadFiles dF = new DownloadFiles();
								dF.setName(mFilesList.get(i).getName());
								dF.setPath(mFilesList.get(i).getPath());
								dF.setSize(mFilesList.get(i).getSize());
								mSelectedList.add(dF);
								mFilesList.get(i).setCheck(true);
								mFilesList.get(i).setDownFile(dF);

							} else {
								mFilesList.get(i).setCheck(false);

							}
						}
					}
					mAdapter.notifyDataSetChanged();
					if (selAllFlag) {
						selAllFlag = false;
						item.setTitle(getResources().getString(R.string.Flashair_Label_Deselect_All));
						isEnableSend(mSelectedList.size());
					} else {
						selAllFlag = true;
						item.setTitle(getResources().getString(R.string.Flashair_Label_Select_All));
						isEnableSend(mSelectedList.size());
					}
				}
				return true;

			default:
				return super.onOptionsItemSelected(item);
			}
		}
		
		/**
		 * Invokes when connection fails with Flashair.
		 */
		public void ifConnectionFails() {
			if (mConnFlag) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						getActivity());
				dialog.setTitle(getResources().getString(R.string.Flashair_Alert_Connection_Error));
				dialog.setMessage(getResources().getString(R.string.Flashair_Alert_Error_Recieving_Data));
				dialog.setCancelable(false);
				dialog.setNegativeButton(getResources().getString(R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mConnFlag = false;
								new ReadURLTask().execute(mStrUrl);
								dialog.dismiss();
							}
						});
				dialog.create().show();
			} else {
				Intent intent = new Intent(getActivity(), DMActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				getActivity().finish();
			}
		}

		/**
		 * Invokes when a folder is selected from the folder list.
		 * @param pos Position of the folder.
		 */
		public void folderSelected(int pos) {
			String s = null;
			StringBuilder str_path = new StringBuilder(HOME);
			try {
				
				str_path.append(URLEncoder.encode(mFilesList.get(pos).getPath(), "UTF8").replace("%2F", "/"));
				str_path.append("/");
			
			
				s = URLEncoder.encode(mFilesList.get(pos).getName(), "UTF8");
			} catch (Exception e) {
			}
			str_path.append(s);
			mFoldPath = str_path.toString();
			mPaths.add("/"+mFilesList.get(pos).getName());
			listener.onNewFolderSelected(mFoldPath);
		}

		@Override
		public void onListItemClick(ListView l, View v, int pos, long id) {
			mSelPos = pos;
			if(mFilesList.size()>0){
				if (mFilesList.get(pos).getType().equalsIgnoreCase("folder")) {
					folderSelected(mSelPos);
					v.setClickable(false);
				} else if (mFilesList.get(pos).getType().equalsIgnoreCase("file")) {
					v.setClickable(false);
				}
			}

		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			listener = (FolderSelectedListener) activity;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			new ReadURLTask().execute(mStrUrl);
		}

		/**
		 * Invokes when device memory is low. A dialog will be shown.
		 */
		private void showLowMemoryDialog(){
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
			alert.setTitle(getResources().getString(R.string.Dictate_No_Space));
			alert.setMessage(getResources().getString(R.string.Dictate_Low_Memory));
			alert.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			alert.create().show();
		}
		
		/**
		 * Shows custom dialog when file downloading happens from Flashair.
		 */
		public void showSendDialog() {
			AlertDialog.Builder builder;
			Context mContext = getActivity();
			LayoutInflater inflater = (LayoutInflater)
			        mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.flashair_custom_dialog,null);
			Button btnSendFiles = (Button) layout
					.findViewById(R.id.btn_send_option);
			Button btnCancel = (Button) layout
					.findViewById(R.id.btn_send_cancel);
			if(mSendOption){
				btnSendFiles.setText(getResources().getString(R.string.Button_Email));
			}else
				btnSendFiles.setText(getResources().getString(R.string.server_title));
			btnSendFiles.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					DMApplication.COMINGFROM="flash_air";
					mDownloadList = new ArrayList<DownloadFiles>();
					mDownloadList.addAll(mSelectedList);
					groupId=mDbHandler.getGroupId();
					hasMoreThan23MB=false;
					if(mSendOption){
						mFileCount = mDownloadList.size();
						if(calculateFileSizeToDownload(mDownloadList))
							executeSendTask(mDownloadList);
						else{
							showLowMemoryDialog();
						}
							
					}else{
						mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
						mServerOption = Integer.parseInt(mSharedPrefs.getString(getResources().getString(R.string.Audio_delivery), "1"));
						//mServerOption = 1;
						switch (mServerOption) {
						case 1:
							
							onServerOptionEmail();
							break;
						case 2:
							mFileCount = mDownloadList.size();
							if(calculateFileSizeToDownload(mDownloadList))
								executeSendTask(mDownloadList);
							else{
								showLowMemoryDialog();
							}
							break;
						case 3:
							promptServerOptions();
							break;
						default:
							break;
						}
						
					}
					mSendAlertDialog.dismiss();
				}
			});

			btnCancel.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mSendAlertDialog.dismiss();
				}
			});
			builder = new AlertDialog.Builder(mContext);
			builder.setView(layout);
			mSendAlertDialog = builder.create();
			mSendAlertDialog.show();
		}

		/**
		 * Invokes when delivery option is Email.
		 */
		private void onServerOptionEmail(){
			ArrayList<String> list = new ArrayList<String>();
			ArrayList<DownloadFiles> tList = new ArrayList<DownloadFiles>();
			
			isEnableSend(mDownloadList.size());
			for (int i = 0; i < mDownloadList.size(); i++) {
				float s =(float) mDownloadList.get(i).getSize()/CONVERT_MB;
				if(s > 23) {    //23
					list.add(mDownloadList.get(i).getName());
					tList.add(mDownloadList.get(i));
				}
			}
			mDownloadList.removeAll(tList);
			mFileCount = mDownloadList.size();
			tList = null;
			if(list.size()>0)
				showFileSizeOverDialog(list);
			else
				if(calculateFileSizeToDownload(mDownloadList))
					executeSendTask(mDownloadList);
				else
					showLowMemoryDialog();
		}
		
		/**
		 * Calculates file size which is to be downloaded.
		 * @param dList List of files to be downloaded as ArrayList
		 * @return True if files size is less than available device memory, false if files size is greater.
		 */
		public boolean calculateFileSizeToDownload(ArrayList<DownloadFiles> dList){
			long size = 0;
			for(int i=0;i<dList.size();i++) {
				size = size + (dList.get(i).getSize()/(1024 * 1024));
			}
			if(size < getAvailableSpaceInMB())
				return true;
			else
				return false;
		}
		
		/**
		 * Dialog will be shown if file size is above 23 mb if delivery option is Email.
		 * @param list List of files with file size above 23 mb.
		 */
		public void showFileSizeOverDialog(ArrayList<String> list){
			
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
			alert.setTitle(getResources().getString(R.string.Flashair_Alert_Size_limitation));
			LinearLayout layout = new LinearLayout(getActivity());
			layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			layout.setOrientation(LinearLayout.VERTICAL);
			TextView text = new TextView(getActivity());
			text.setText(getResources().getString(R.string.Flashair_Alert_FTP_option));
			text.setTextColor(Color.WHITE);
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			text.setPadding(30, 20, 30, 20);
			ListView modeList = new ListView(getActivity());
			modeList.setCacheColorHint(Color.TRANSPARENT);
			modeList.setBackgroundColor(Color.WHITE);
			ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_item, list);
			modeList.setAdapter(modeAdapter);
			layout.addView(text);
			layout.addView(modeList);
			alert.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(calculateFileSizeToDownload(mDownloadList))
						executeSendTask(mDownloadList);
					else {
						showLowMemoryDialog();
					}
				}
			});
			alert.setView(layout);
			alert.create().show();
		}
		
		/**
		 * Dialog with server options will be shown after files are downloaded from FlashAir.
		 */
		private void promptServerOptions() {
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity());
			mBuilder.setCancelable(false);
			View layout = inflater.inflate(R.layout.activity_dictate_property_image_menu, (ViewGroup) getActivity()
							.findViewById(R.id.relative_property_image_menu));
			Button mEmail = (Button) layout
					.findViewById(R.id.btn_property_image_takephoto);
			mEmail.setText(getResources().getString(R.string.Button_Email));
			Button mFtp = (Button) layout
					.findViewById(R.id.btn_property_image_choose_existing);
			mFtp.setText(getResources().getString(R.string.Button_FTP));
			Button mCancel = (Button) layout
					.findViewById(R.id.btn_property_image_cancel);
			mEmail.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mServerOption = 1;
					mAlertDialog.dismiss();
					onServerOptionEmail();
				}
			});
			mFtp.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mServerOption = 2;
					mAlertDialog.dismiss();
					mFileCount = mDownloadList.size();
					if(calculateFileSizeToDownload(mDownloadList))
						executeSendTask(mDownloadList);
					else{
						showLowMemoryDialog();
					}
				}
			});
			mCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mAlertDialog.dismiss();
				}
			});
			
			mBuilder.setView(layout);
			mAlertDialog = mBuilder.create();
			mAlertDialog.show();
		}

		/**
		 * Get available memory space of device in MB.
		 * @return Memory space available as Long.
		 */
		private long getAvailableSpaceInMB() {
			final long SIZE_KB = 1024L;
			final long SIZE_MB = SIZE_KB * SIZE_KB;
			return dmApplication.getAvailableDiskSpace() / SIZE_MB;
		}
		
		/**
		 * Invokes when files send separately.
		 * @param sendList List of files to be sent separately.
		 */
		public void sendSeparateFile(ArrayList<DownloadFiles> sendList){
			float pairSize = 0.0f;
			
			mSeparateFlag = true;
			mTempList = new ArrayList<DownloadFiles>();
			for (int i = 0; i < sendList.size(); i++) {
				pairSize += (float) sendList.get(i).getSize()/CONVERT_MB;
				if(pairSize > 23)
					break;
				else
					mTempList.add(sendList.get(i));
			}
			sendList.removeAll(mTempList);
			for(DownloadFiles df : mTempList){
				if(df.getStatus()){
					shareViaEmail(mTempList);
					break;
				}
			}
			
		}
		
		/** 
		 * Comparator to sort ArrayList based on file size 
		 */
		class SizeComparator implements Comparator<DownloadFiles>{

			@Override
			public int compare(DownloadFiles lhs, DownloadFiles rhs) {
				
				return Float.compare(lhs.getSize(), rhs.getSize());
			}
			
		}
		
		
		/** 
		 * Execute Asynctask used to Download and send files which are selected to send.
		 * */
		public void executeSendTask(ArrayList<DownloadFiles> dList) {
			try{
				new SendFilesTask(dList).execute();
			}catch(Exception e){
				
			}
		}

		/**
		 * Convert date in dos format to regular date 
		 * @param d Date in dos format.
		 * @return Date in standard format.
		 */
		public static String parseDosDate(int d) {
			int day = d & 31;
			int month = (d & 480) >> 5;
			int year = 1980 + ((d & 65024) >> 9);
			String date = null;

			if (day >= 1 && day <= 31 && month >= 1 && month <= 12
					&& year <= 2040) {
				Calendar cal = Calendar.getInstance();
				cal.set(year, month - 1, day);
				SimpleDateFormat dd = new SimpleDateFormat("yyyy/MM/dd");
				date = dd.format(cal.getTime());
			}
			return date;
		}

		
		/**
		 * Convert time in dos format to regular time .
		 * @param t Time in Dos format.
		 * @return Time in Standard format.
		 */
		public static String parseDosTime(int t) {
			int sec = 2 * (t & 31);
			int min = (t & 2016) >> 5;
			int hour = (t & 63488) >> 11;
			String time = null;
			if (sec <= 60 && min <= 59 && hour <= 23) {
				Calendar cal1 = Calendar.getInstance();
				cal1.set(0, 0, 0, hour, min, sec);
				SimpleDateFormat tt = new SimpleDateFormat("HH:mm:ss");
				time = tt.format(cal1.getTime());
			}
			return time;
		}

		/**
		 * Formats time in HH:mm format.
		 * @param timeString Time string in HH:mm:ss format.
		 * @return Time in HH:mm format.
		 */
		public String getTimeInFormat(String timeString){
			SimpleDateFormat oldFormat = new SimpleDateFormat("HH:mm:ss");
			Date date = null;
			try {
				date = oldFormat.parse(timeString);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			SimpleDateFormat newFormat = new SimpleDateFormat("HH:mm");
			return newFormat.format(date);
		}
		
		/**
		 * Formats date in yyyy-MM-dd HH:mm:ss format.
		 * @param dateString Date in yyyy/MM/dd HH:mm:ss format.
		 * @return Date in yyyy-MM-dd HH:mm:ss format.
		 */
		public String getDateInFormat(String dateString){
			SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = null;
			try {
				date = oldFormat.parse(dateString);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return newFormat.format(date);
		}
		
		/** 
		 * Shows dialog with download Summary .
		 * */
		public void showSummary() {
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
			alertBuilder.setTitle(getResources().getString(R.string.Flashair_Label_DownloadSummary));
			alertBuilder.setMessage(getResources().getString(R.string.Flashair_Label_Files_Downloaded)
					.replace("*", String.valueOf(mDownloadedDictCards.size())).replace("#", String.valueOf(mSelectedList.size())));
			alertBuilder.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			alertDialog=alertBuilder.create();
			alertDialog.setCancelable(true);
			alertDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if(mSendOption){
						for(DownloadFiles df : mDownloadList){
							if(df.getStatus()){
								shareViaEmail(mDownloadList);
								break;
							}
						}
					}
					else if(hasMoreThan23MB)
						onSendToServerEmailHasAbove23MB();
				}
			});
			alertDialog.show();
			refreshFileList(mDownloadList);
		}
		/**
		 * Dialog will be shown when selected file size exceeds email attachment limit.
		 */
		private void onSendToServerEmailHasAbove23MB()
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
			alert.setCancelable(true);
			alert.setMessage(getResources().getString(R.string.FileSizeMaximum));
			alert.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok),new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					hasMoreThan23MB=false;
					dialog.dismiss();
				}
			});
			alert.create().show();
		}
		
		/**
		 * Converts file size in bytes to human readable format.
		 * @param size Size in bytes as long.
		 * @return Size in humar readable format as String.
		 */
		public static String toReadableFileSize(long size) {
		    if(size <= 0) return "0 KB";
		    final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		    return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
		}
		
		/**
		 * Dialog will be shown when file is not available in sdcard.
		 */
		public void showSourceNotFoundDialog(){
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
			alert.setTitle(getResources().getString(R.string.Settings_Error));
			alert.setMessage(getResources().getString(R.string.SourceNotfound));
			alert.setNegativeButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			alert.create().show();
		}
		
		
		
		/** 
		 * Reads the Url and parse the contents like files/folder details.
		 * */
		public class ReadURLTask extends AsyncTask<String, Void, Void> {

			ProgressDialog dialog;
			boolean errorFlag = false;

			@Override
			public void onPreExecute() {
				mFilesList = null;
				mFilesList = new ArrayList<FlashAirFiles>();
				mTempFileList = new ArrayList<FlashAirFiles>();
				dialog = new ProgressDialog(getActivity());
				sourceNotFound = false;
				errorFlag = false;
				dialog.setMessage(getResources().getString(R.string.Dictate_Loading));
				dialog.setCancelable(false);
				dialog.show();

			}

			@Override
			protected Void doInBackground(String... params) {
				String str = params[0];

				try {
					//str = URLEncoder.encode(str, "UTF-8");
					url = new URL(str);
					mHttpConnection = (HttpURLConnection) url.openConnection();
					mHttpConnection.setConnectTimeout(20000);
					mInputStream = new BufferedInputStream(
							mHttpConnection.getInputStream());
					mReader = new InputStreamReader(mInputStream);
					mBufferedReader = new BufferedReader(mReader);

					String line = null;

					while ((line = mBufferedReader.readLine()) != null) {
						if (!line.equalsIgnoreCase("WLANSD_FILELIST")) {
							List<String> list = Arrays.asList(line.split(","));
							FlashAirFiles flashFiles = new FlashAirFiles();
							int type = Integer.parseInt(list.get(3));
							if (type == 32 || type == 33 || type == 0 || type == 1) {
								if (list.get(1).endsWith(".MP3") || list.get(1).endsWith(".mp3")
										|| list.get(1).endsWith(".WAV") || list.get(1).endsWith(".wav")
										|| list.get(1).endsWith(".DSS") || list.get(1).endsWith(".dss")
										|| list.get(1).endsWith(".DS2") || list.get(1).endsWith(".ds2")) {
									flashFiles.setType("file");
									flashFiles.setName(list.get(1));
									flashFiles.setPath(list.get(0));
									flashFiles.setSize(Integer.parseInt(list
											.get(2)));
									
									flashFiles.setDate(parseDosDate(Integer
											.parseInt(list.get(4))));
									flashFiles.setTime(parseDosTime(Integer
											.parseInt(list.get(5))));
									flashFiles.setCheck(false);
									//mFilesList.add(flashFiles.copy());
									mTempFileList.add(flashFiles.copy());
								
								}
							} else if (type == 16 || type == 17 || type == 48 || type == 49 ) {
								flashFiles.setType("folder");
								flashFiles.setName(list.get(1));
								flashFiles.setPath(list.get(0));
								flashFiles
										.setSize(Integer.parseInt(list.get(2)));
								flashFiles.setDate(list.get(4));
								flashFiles.setTime(list.get(5));
								mFilesList.add(flashFiles.copy());
							}

						}
					}
					mFilesList.addAll(mTempFileList);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					System.out.println(" Flashair error : MalformedURLException - "+e.getMessage());
					errorFlag = true;
				}catch(SocketTimeoutException ste){
					System.out.println(" Flashair error : SocketTimeoutException - "+ste.getMessage());
					ste.printStackTrace();
					errorFlag = true;
				}catch (FileNotFoundException e) {
					System.out.println(" Flashair error : FileNotFoundException - "+e.getMessage());
					e.printStackTrace();
					sourceNotFound = true;
					//errorFlag = true;
				} catch (IOException e) {
					System.out.println(" Flashair error : IOException - "+e.getMessage());
					e.printStackTrace();
					errorFlag = true;
					
				} catch (Exception exc) {
					System.out.println(" Flashair error : Exception - "+exc.getMessage());
					exc.printStackTrace();
					errorFlag = true;
				}

				return null;
			}

			@Override
			public void onPostExecute(Void result) {
				mAdapter = null;
				mAdapter = new DirectoryListAdapter(getActivity(), mFilesList);
				setListAdapter(mAdapter);
				textSsid.setText(mSelSsid);
				if (errorFlag) {
					ifConnectionFails();
					errorFlag = false;
				}
				if(sourceNotFound){
					showSourceNotFoundDialog();
					
					sourceNotFound = false;
				}
				dialog.dismiss();
			}

		}

		/**
		 * Initialize dictation data before downloading the file from FlashAir.
		 * @param downFile The file to be downloaded.
		 */
		public void initializePreDictData(DownloadFiles downFile) {
				mDictcard = new DictationCard();
				for(FlashAirFiles fAFile : mFilesList){
					if(fAFile.getName().equals(downFile.getName())){
					mFlashFile = fAFile;
					break;
					}
				}
				Cursor cur = mDbHandler.checkActiveDictationExistsWithNoDuration();
				if (cur.moveToFirst()) {
					isNew = false;
					mDictcard = mDbHandler.getSelectedDictation(cur);
					seqNumber = mDictcard.getSequenceNumber();
					mDictationDir = new File(DMApplication.DEFAULT_DIR.getAbsolutePath() + "/Dictations/"
							 + mDictcard.getSequenceNumber());
					if (!mDictationDir.exists())
						mDictationDir.mkdirs();
					mDictcard.setIsActive(0);
					mDbHandler.updateIsActive(mDictcard);
					dmApplication.flashair = true;
				}else{
					isNew = true;
					DictateActivity.SavedDicID = -1;
					SharedPreferences mSharedPreferenece = PreferenceManager
							.getDefaultSharedPreferences(getActivity());
					seqNumber = mSharedPreferenece.getInt("seq_number", 0000) + 1;
					if(seqNumber > 99999999)
						seqNumber = 0;
					mDictationDir = new File(DMApplication.DEFAULT_DIR.getAbsolutePath() + "/Dictations/"
									 + seqNumber);
					if (!mDictationDir.exists())
						mDictationDir.mkdirs();
					Editor edit = mSharedPreferenece.edit();
					edit.putInt("seq_number", seqNumber);
					edit.commit();
				}
				if(cur!=null)
					cur.close();
		}
		
		/**
		 * Initializes Dictation details as DictationCard after downloading the file from FlashAir. When
		 * the delivery option 'Email', then dictations are also grouped.
		 * 
		 * @return Dictation details as DictationCard.
		 */
		public DictationCard initializeDictationCard(){
			
			String fileName = mFlashFile.getName();
			
			
			//String fileExtn = fileName.split("\\.")[1];
			String fileExtn = "";

			int i = fileName.lastIndexOf('.');
			if (i > 0) 
				fileExtn = fileName.substring(i+1);
			if(fileExtn.equalsIgnoreCase("DSS"))
				mDictcard.setDssVersion(1);
			else if(fileExtn.equalsIgnoreCase("DS2"))
				mDictcard.setDssVersion(10);
			else if(fileExtn.equalsIgnoreCase("WAV"))
				mDictcard.setDssVersion(111);
			else if(fileExtn.equalsIgnoreCase("MP3"))
				mDictcard.setDssVersion(222);
			if(!mSendOption) {
				mDictcard.setStatus(DictationStatus.RETRYING2.getValue());
				mDictcard.setMainStatus(200);
			}
			else
				mDictcard.setStatus(DictationStatus.UNKNOWN.getValue());
			String file = "";
			int j = fileName.lastIndexOf('.');
			if (j > 0) 
				file = fileName.substring(0, j);
			mDictcard.setDictationId((int) seqNumber);
			mDictcard.setDictationName(file);
			mDictcard.setDictFileName(file);
			mDictcard.setAuthor(mAuthor);
			mDictcard.setCreatedAt(getDateInFormat(mFlashFile.getDate()+" "+mFlashFile.getTime()));
			mDictcard.setRecStartDate(getDateInFormat(mFlashFile.getDate()+" "+mFlashFile.getTime()));
			mDictcard.setRecEndDate(getDateInFormat(mFlashFile.getDate()+" "+mFlashFile.getTime()));
			mDictcard.setPriority(0);
			mDictcard.setIsActive(0);
			mDictcard.setWorktype("");
			mDictcard.setSequenceNumber((int) seqNumber);
			mDictcard.setIsFlashAir(1);
			mDictcard.setDeliveryMethod(mServerOption);
			mDictcard.setDuration(0);
			mDictcard.setIsConverted(1);
			if(mServerOption==1) {
				String fName=DMApplication.DEFAULT_DIR+"/Dictations/"+mDictcard.getSequenceNumber()+"/"+
						mDictcard.getDictFileName();
				emailGroupFileSize=emailGroupFileSize+dmApplication.getFileSize(fName,mDictcard.getDssVersion());
				if(emailGroupFileSize>23*1024*1024)
				{
					hasMoreThan23MB=true;
					groupId=mDbHandler.getGroupId();
					emailGroupFileSize=dmApplication.getFileSize(fName,mDictcard.getDssVersion());
				}
				mDictcard.setGroupId(groupId);
				mDictcard.setFileSplittable(1);
			}
			else if(mServerOption==2) {
				groupId=mDbHandler.getGroupId();
				mDictcard.setGroupId(groupId);
			}
			if(isNew)
				mDbHandler.insertDictation(mDictcard);
			else
				mDbHandler.updateDictation(mDictcard);
			
			FilesCard fCard = new FilesCard();
			fCard.setFileId((int) seqNumber);
			fCard.setFileIndex(0);
			fCard.setFileName(file);
			ArrayList<FilesCard> fList = new ArrayList<FilesCard>();
			fList.add(fCard);
			mDictcard.setFilesList(fList);
			mDbHandler.insertFiles(fCard);
		
			return mDictcard;
		}
		
		/** 
		 * AsyncTask used to download files from the FlashAir card. 
		 * */
		private class SendFilesTask extends AsyncTask<Void, Integer, Void> {

			ArrayList<DownloadFiles> dList;
			ProgressDialog dialog;
			static final String BASE_URL = "http://flashair";
			private InputStream input;
			private OutputStream output;

			boolean stateFlag = true;
			int lenghtOfFile;

			public SendFilesTask(ArrayList<DownloadFiles> dList) {
				this.dList = dList;

			}

			@Override
			protected void onPreExecute() {
				dialog = new ProgressDialog(getActivity());
				dialog.setCancelable(false);
				dialog.setTitle(dList.get(mFileNo).getName());
				dialog.setMessage(mFileNo + 1 + "/" + mFileCount + " "
							+ getResources().getString(R.string.Flashair_Label_Downloading));
				dialog.setIndeterminate(false);
				dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				dialog.setMax((int) (dList.get(mFileNo).getSize()/1024));
				dialog.show();
			}

			@Override
			protected Void doInBackground(Void... params) {

				if (dList.size() > 0) {
					String fileName = null;
					try {
						initializePreDictData(dList.get(mFileNo));
						fileName = dList.get(mFileNo).getName();
						StringBuilder downUrl = new StringBuilder(BASE_URL);
						downUrl.append(URLEncoder.encode(dList.get(mFileNo).getPath()).replace("%2F", "/"));
						downUrl.append("/");
						downUrl.append(URLEncoder.encode(fileName, "UTF8"));

						url = new URL(downUrl.toString());
						URLConnection conexion = url.openConnection();
						conexion.setConnectTimeout(5000);
						conexion.connect();

						lenghtOfFile = conexion.getContentLength() / 1024;
						dialog.setMax(lenghtOfFile);
						input = new BufferedInputStream(
								conexion.getInputStream());
						conexion.setReadTimeout(5000);
						File dir = mDictationDir;
						if (dir.exists() == false) {
							dir.mkdirs();
						}
						File file = new File(dir, fileName);

						output = new FileOutputStream(file);

						byte data[] = new byte[1024];

						int total = 0;
						int count = 0;
						while ((count = input.read(data)) != -1) {
							total += count;
							publishProgress(total / 1024);
							output.write(data, 0, count);
						}
						output.flush();
						output.close();
						input.close();
					} catch (FileNotFoundException fnfe) {
						fnfe.getLocalizedMessage();
						mDownloadList.get(mFileNo).setStatus(false);
						stateFlag = false;
					} catch (UnknownHostException uhe) {
						uhe.getLocalizedMessage();
						mDownloadList.get(mFileNo).setStatus(false);
						stateFlag = false;
					} catch (SocketTimeoutException ste) {
						ste.getLocalizedMessage();
						mDownloadList.get(mFileNo).setStatus(false);
						stateFlag = false;
					}catch (ConnectException ce) {
						ce.getLocalizedMessage();
						mDownloadList.get(mFileNo).setStatus(false);
						stateFlag = false;
					} catch (SocketException se) {
						se.getLocalizedMessage();
						File fil = new File(mDictationDir, fileName);
						
						if (fil.length() != lenghtOfFile) {
							mDownloadList.get(mFileNo).setStatus(false);
							stateFlag = false;
							fil.delete();
						} else {
							mDownloadList.get(mFileNo).setStatus(true);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				dialog.setProgress(values[0]);
			}

			@Override
			protected void onPostExecute(Void result) {
				if (stateFlag){
					mDownloadList.get(mFileNo).setStatus(true);
					mDownloadedDictCards.add(initializeDictationCard());
					
				}
				dialog.dismiss();
				
				if (mFileNo < dList.size() - 1) {
					mFileNo++;
					executeSendTask(dList);
				} else {
					mFileNo = 0;
					emailGroupFileSize=0;
					showSummary();
				}

			}

		}
		
		/**
		 * Creates custom chooser for email clients. 
		 * */
		public void shareViaEmail(ArrayList<DownloadFiles> list) {
			
			final ArrayList< DownloadFiles> sList = list;
			mSharedPrefs = getActivity().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
			String prefPackName = mSharedPrefs.getString(PREF_KEY, "");
			if(prefPackName.equals("")){
			
				Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
				sendIntent.setType("message/rfc822");
				
				List<ResolveInfo> activities = getActivity().getPackageManager().queryIntentActivities(sendIntent, 0);
				List<ResolveInfo> activities1 = new ArrayList<ResolveInfo>();
				  if (!activities.isEmpty()){
					  
				        for (int i = 0; i < activities.size();i++) {
				        	//Log.w("log", "info.activityInfo.packageName.toLowerCase()***  "+activities.get(i).activityInfo.packageName.toLowerCase());
				            if (activities.get(i).activityInfo.packageName.toLowerCase().contains("mail") 
				            		|| activities.get(i).activityInfo.packageName.toLowerCase().contains("gm") ) {
				            	activities1.add(activities.get(i));
				            }
				        }
				  }
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle("Select Email client");
				builder.setCancelable(false);
				final ChooserIntentListAdapter adapter = new ChooserIntentListAdapter(getActivity(), R.layout.flashair_email_chooser_row, activities1.toArray());
				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ResolveInfo info = (ResolveInfo) adapter.getItem(which);
						String packageName = info.activityInfo.packageName;
						Editor editor = mSharedPrefs.edit();
						editor.putString(PREF_KEY, packageName);
						editor.commit();
						sendEmail(sList, packageName);
						
						}
				});
				builder.create().show();
			}else{
				sendEmail(sList, prefPackName);
			}
			
		}
		
		
		/**
		 * Invokes email client to send downloaded flashair files if send option is Email. 
		 * @param sendList List of files to be sent.
		 * @param packageName Package name of the email client to be invoked.
		 */
		public void sendEmail(ArrayList<DownloadFiles> sendList, String packageName){
			Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			emailIntent.setType("message/rfc822");
			SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(getActivity());
			String[] recipient;
			if(pref.getString(getResources().getString(R.string.recipient_key),"").contains(","))
				recipient=pref.getString(getResources().getString(R.string.recipient_key),"").split(",");
			else
				recipient=new String[]{pref.getString(getResources().getString(R.string.recipient_key),"")};
			emailIntent.putExtra(Intent.EXTRA_EMAIL,recipient);
			emailIntent.putExtra(Intent.EXTRA_SUBJECT,pref.getString(getResources().getString(R.string.subject_key),"Dictations"));
			emailIntent.putExtra(Intent.EXTRA_TEXT,pref.getString(getResources().getString(R.string.message_key),"Please find the attached files"));
			ArrayList<Uri> uris = new ArrayList<Uri>();
			
			for(int i = 0;i < sendList.size();i++){
				if(sendList.get(i).getStatus()){
					File file = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mDownloadedDictCards.get(i).getSequenceNumber(), sendList.get(i).getName());
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						uris.add(FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", file));
					}
					else
					{
						uris.add(Uri.fromFile(file));
					}
//					Uri uri = Uri.fromFile(file);
//					uris.add(uri);
				}
			}
			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			emailIntent.setPackage(packageName);
			startActivityForResult(emailIntent, SEND_REQ_CODE);
			sendList.clear();
			
		}
		
		@Override
		public void onActivityResult(int requestCode, int resultCode,
				Intent data) {
			//System.out.println(" result code "+resultCode);
			if(requestCode == SEND_REQ_CODE){
				for(DictationCard dCard : mDownloadedDictCards){
					dCard.setStatus(DictationStatus.SENT_VIA_EMAIL.getValue());
					dCard.setSentDate(dmApplication.getDeviceTime());
					mDbHandler.updateStatusAndSentDate(dCard);
				}
				refreshFileList(mDownloadList);
			}
		}
		
		
		/**
		 * Refresh file/folder list.
		 * @param ddList
		 */
		private void refreshFileList(ArrayList<DownloadFiles> ddList){
			isEnableSend(0);
			mSelectedList.clear();
			for (int i = 0; i < mFilesList.size(); i++) {
				String type = mFilesList.get(i).getType();
				if (type.equals("file")) {
					mFilesList.get(i).setCheck(false);
				}
			}
			if(mAdapter != null)
				mAdapter.notifyDataSetChanged();
		}
		
		/**
		 * List Adapter for listing the directories and files from the FlashAir
		 * card
		 */
		private class DirectoryListAdapter extends BaseAdapter {
			private ArrayList<FlashAirFiles> list;

			private LayoutInflater mInflater;
			final int MB = 1024 * 1024;
			public DirectoryListAdapter(Context context,
					ArrayList<FlashAirFiles> results) {
				list = results;
				mInflater = LayoutInflater.from(context);

			}

			public int getCount() {
				return list.size();
			}

			public Object getItem(int position) {
				return list.get(position);
			}

			public long getItemId(int position) {
				return position;
			}

			public View getView(int position, View convertView, ViewGroup parent) {
				final ViewHolder holder = new ViewHolder();
				if (convertView == null) {
					convertView = mInflater.inflate(
							R.layout.flash_air_list_row, null);
					holder.relLayoutFile = (RelativeLayout) convertView
							.findViewById(R.id.rel_lay_for_file);
					holder.img = (ImageView) convertView
							.findViewById(R.id.folder_img);
					holder.img_next_item = (ImageView) convertView
							.findViewById(R.id.img_next_item);
					holder.name = (TextView) convertView
							.findViewById(R.id.file_name);
					holder.folderName = (TextView) convertView
							.findViewById(R.id.folder_name);
					holder.date = (TextView) convertView
							.findViewById(R.id.text_date);
					holder.time = (TextView) convertView
							.findViewById(R.id.text_time);
					holder.fileSize = (TextView) convertView
							.findViewById(R.id.text_file_size);
					holder.selection = (CheckBox) convertView
							.findViewById(R.id.chkbox_select);
					holder.selection.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							int pos = getListView().getPositionForView(v);
							FlashAirFiles element = (FlashAirFiles) holder.selection
									.getTag();
							DownloadFiles dFiles = new DownloadFiles();
							if (list.get(pos).isChecked()) {
								holder.selection.setChecked(false);
								element.setCheck(false);
								DownloadFiles dF = (DownloadFiles) list
										.get(pos).getDownFile();
								mSelectedList.remove(dF);
								isEnableSend(mSelectedList.size());

							} else {
								holder.selection.setChecked(true);
								element.setCheck(true);
								dFiles.setName(list.get(pos).getName());
								dFiles.setPath(list.get(pos).getPath());
								dFiles.setSize(list.get(pos).getSize());
								mSelectedList.add(dFiles);
								element.setDownFile(dFiles);
								isEnableSend(mSelectedList.size());

							}
						}
					});

					convertView.setTag(holder);
					holder.selection.setTag(list.get(position));
				} else {
					((ViewHolder) convertView.getTag()).selection.setTag(list
							.get(position));
				}

				ViewHolder hold = (ViewHolder) convertView.getTag();
				String name ;
				if (list.get(position).getType().equalsIgnoreCase("file")) {
					name = list.get(position).getName();
					if(name.endsWith("MP3") || name.endsWith("mp3"))
						hold.img.setImageResource(R.drawable.flashair_mp3_icon);
					else if(name.endsWith("WAV") || name.endsWith("wav"))
						hold.img.setImageResource(R.drawable.flashair_wav_icon);
					else if(name.endsWith("DSS") || name.endsWith("dss"))
						hold.img.setImageResource(R.drawable.flashair_dss_icon);
					else if(name.endsWith("DS2") || name.endsWith("ds2"))
						hold.img.setImageResource(R.drawable.flashair_dss_pro_icon);
					hold.img_next_item.setVisibility(4);
					hold.selection.setVisibility(0);
					hold.name.setText(name);
					hold.selection.setChecked(list.get(position).isChecked());
					hold.relLayoutFile.setVisibility(View.VISIBLE);
					hold.folderName.setVisibility(View.GONE);
					hold.date.setText(list.get(position).getDate());
					hold.time.setText(getTimeInFormat(list.get(position).getTime()));

					float s = (float) list.get(position).getSize() / MB;
					String fileSize = toReadableFileSize(list.get(position).getSize());
					hold.fileSize.setText(fileSize);
					
				} else {
					hold.img.setImageResource(R.drawable.flashair_folder);
					hold.img_next_item.setVisibility(0);
					hold.selection.setVisibility(4);
					hold.folderName.setVisibility(View.VISIBLE);
					hold.folderName.setText(list.get(position).getName());
					hold.relLayoutFile.setVisibility(View.GONE);

				}

				return convertView;
			}

			class ViewHolder {
				ImageView img;
				ImageView img_next_item;
				TextView name;
				TextView folderName;
				TextView date;
				TextView time;
				TextView fileSize;
				CheckBox selection;
				RelativeLayout relLayoutFile;

			}

		}

		/**
		 *  Enable or Disable Send button based on the selection of files 
		 *  */
		private void isEnableSend(int size) {
			if (size == 0) {
				btnSend.setEnabled(false);
				btnSend.setText(getResources().getString(R.string.Flashair_Label_Send));
				selAll = getResources().getString(R.string.Flashair_Label_Select_All);
				selAllFlag = true;
			} else {
				btnSend.setEnabled(true);
				btnSend.setText(getResources().getString(R.string.Flashair_Label_Send)+"(" + size + ")");
				selAll = getResources().getString(R.string.Flashair_Label_Deselect_All);
				selAllFlag = false;
			}
		}

	}
	/**
	 * Sets current Language.
	 * @param value Language value as int
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
	  * Set language to locale with passed Language.
	 * @param lang Language string passed.
	 */
	private void setLocale(String lang) {
		 
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
	 
	 /*public class GetPassThruModeTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			
			try {
				URL passThruUrl = new URL(BASE_URL+PASS_THRU_CODE);

				HttpURLConnection connection = (HttpURLConnection) passThruUrl.openConnection();
				InputStream response = connection.getInputStream();
				String resp = convertStreamToString(response);
				dmApplication.setPassThruMode(true);
				System.out.println(" pass thru response  "+resp);
			} catch (IOException e) {
				dmApplication.setPassThruMode(false);
			} catch (Exception e) {
				dmApplication.setPassThruMode(false);
			}
			
			return null;
		}
		 
	 }*/
	 
	 private static String convertStreamToString(InputStream is) throws Exception {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    StringBuilder sb = new StringBuilder();
		    String line = null;

		    while ((line = reader.readLine()) != null) {
		        sb.append(line);
		    }

		    is.close();

		    return sb.toString();
		}

}
