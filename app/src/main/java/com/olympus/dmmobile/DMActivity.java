package com.olympus.dmmobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.olympus.dmmobile.ConvertAndUploadService.UploadStatusChangeListener;
import com.olympus.dmmobile.flashair.ChooserIntentListAdapter;
import com.olympus.dmmobile.flashair.FlashAirMain;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.network.NetWorkAndNotActivatedDialog;
import com.olympus.dmmobile.network.NetworkConnectivityListener;
import com.olympus.dmmobile.recorder.DictateActivity;
import com.olympus.dmmobile.settings.SettingsActivity;
import com.olympus.dmmobile.webservice.Base64_Encoding;
import com.olympus.dmmobile.webservice.Settingsparser;
import com.olympus.dmmobile.webservice.WebserviceHandler;

/**
 * Class DMActivity is the base class for Recordings view. All the operations in
 * Recordings view is handled by this class.
 *
 * @version 1.0.1
 */
public class DMActivity extends FragmentActivity implements
        OnPageChangeListener, OnTabChangeListener, OnClickListener,
        RecordingSelectedListener, UploadStatusChangeListener, ListItemAction,
        OnEditorActionListener {

    /* Native library */
    static {
        System.loadLibrary("native_lib");
    }

    /**
     * native function to take copy of a dictation in sent list for editing
     *
     * @param size
     * @param sourceFile path of source file to copy
     * @param destFile   path of destination file
     * @return return 1 if success , else return 0.
     */
    public native int editCopy(long size, String sourceFile, String destFile);

    private ExceptionReporter mReporter; // Error Logger

    private String SEND_TAG;
    private String SEND_ALL_TAG;
    private String DEL_TAG;
    private String DEL_ALL_TAG;
    private final String HIGH_PRIORITY_TAG = "high_priority";
    private final String LOW_PRIORITY_TAG = "low_priority";

    private TabHost mTabHost;
    private DMActivityViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter = null;
    private OutboxTabFragment mOutboxFragment;
    private PendingTabFragment mPendingFragment;
    private SendTabFragment mSendFragment;
    private ImageView mNewDictation;
    private ImageButton mFlashAir;
    private Button mBtnEdit;
    private Button mBtnSendDelete;
    private Button mBtnSendDeleteAll;
    private Button mBtnCancel;
    private ImageButton mBtnSortPriority;
    private ImageButton mSettingsOption;

    private EditText mEditSearch;
    private ArrayList<Integer> mSelectedList = new ArrayList<Integer>();
    private DatabaseHandler mDbHandler;
    private AlertDialog mAlertDialog = null;
    private AlertDialog.Builder mBuilder;
    private Cursor mCursor = null;
    private CustomCursorAdapter mAdapter;
    private NetworkConnectivityListener mNetworkListener;
    private WifiManager wifi;
    private Locale locale;
    private WebserviceHandler webserviceHandler = null;

    private DMApplication dmApplication = null;
    private final double maxSize = 23 * 1024 * 1024;
    private double cardSize = 0;

    private DictationCard mDictationCard = null;
    private DictationCard mParentCard = null;
    private ArrayList<FilesCard> mFilesCards = null;
    private FilesCard filesCard = null;
    private final String DONT_SHOW_KEY = "dont_show";
    private final String PREFS_NAME = "Config";
    private String mSettingsConfig;
    private String mActivation;
    private String base64value;
    private Base64_Encoding baseEncoding;
    private String prefUUID;
    private String mEmail;
    private String mGetemail;
    private SharedPreferences pref;
    private String mGetuuid;
    private String mUrl;
    private String mGetSettingsResponse;
    private Settingsparser settingParser;
    private String mResultcode;
    private SharedPreferences.Editor editor;
    private String mAudioEncrypt = "-1";
    private String mAudioPassword;
    private String mAudioFormat;
    private String mAudioDelivery;
    private String mWorktypeListname;
    private String mWorktype = "";
    private String filePath = null;
    private String mAuthor;
    private String mAuthorName = "AUTHOR";
    private String dictationName;
    private String mCreatedDate;
    private String Activation;
    private String mCheckServermail;
    private String mFileSource = null;
    private String mFileDestination = null;
    private List<Settingsparser.WorkTypeListObjects> worktypeobject;
    // send via email(amr)
    private final String EMAIL_PREF_NAME = "email_prefs";
    private final String EMAIL_PREF_KEY = "selected_client";
    private final int EMAIL_SEND_REQ_CODE = 100;
    private ArrayList<DictationCard> dictationCards = null;

    private Intent baseIntent = null;
    private DuplicateDictationTask mTaskDuplication = null;
    private ArrayList<Uri> uris = null;
    private File file = null, mtempFile = null;
    private String[] recipient = null;
    private View tabview = null;
    private FileChannel srcChannel = null;
    private FileChannel dstChannel = null;
    private ProgressDialog dialog = null;
    private String language;
    private AMRConverter amrConverter = null;
    private StatFs stat = null;
    private WebServiceGetSettings mWebServiceGetSettings = null;

    private int mKeepSentVal;
    private int sNumber;
    private int seqNumber;
    private int mTextlength;
    private int emailGroupId = 0;
    private int mLanguageVal;
    private int isSplittable = 0;
    private int mTabPosition = 0;
    private int selSize = 0;
    private static int TAB_LENGTH = 0;

    private boolean isCriticalErrorOccures = false;
    private boolean isLocationPermission = false;
    private boolean isNewExists;
    private boolean limitFlag = false;
    private boolean isKeyboardShown = false;
    private boolean flashAirConn = false;
    private boolean hasConvertedFilesExists = true;
    private boolean baseFileExists = true;
    private boolean isSendAll = false;
    private double emailGroupFileSize = 0;
    private boolean hasMultipleSplits = false;
    private boolean hasNoEncript = false;
    private boolean isOnceCalled = false;
    private boolean mEditFlag = false;
    private boolean isOnceExecuted = false;
    private boolean isLimit = false;
    private boolean hasOutboxResent = false;
    private boolean isSendAllInitializing = false;
    private boolean isResend = false;
    private int mActionForDuplication = -1;
    private int mSelectedSize = -1;
    private GetAvailableDictationSlotTask mComputeAvailableDictSlot = null;
    ConvertAndUploadService convertAndUploadService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mReporter = ExceptionReporter.register(this);
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_main);
        Toast.makeText(DMActivity.this, "oncreate", Toast.LENGTH_SHORT).show();
        isNetworkAvailable();
        isOnline();
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {


            boolean firstRun = sharedPref.getBoolean("firstrun", true);
            if (firstRun) {
                String path = DMApplication.DEFAULT_DIR + "/Dictations/";
                File pathFile = new File(path);
                boolean success = deleteDirectory(pathFile);
                if (success) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("firstrun", false);
                    editor.commit();
                }
            }
        }
        dmApplication = (DMApplication) getApplication();
        dmApplication.setContext(this);

        SEND_TAG = this.getString(R.string.Recording_Label_Send);
        SEND_ALL_TAG = this.getString(R.string.Recording_Label_Send_All);
        DEL_TAG = this.getResources()
                .getString(R.string.Recording_Label_Delete);
        DEL_ALL_TAG = this.getString(R.string.Recording_Label_Delete_All);

        mDbHandler = dmApplication.getDatabaseHandler();
        dmApplication.setEditMode(false);

        mKeepSentVal = Integer.parseInt(sharedPref.getString(
                getString(R.string.keep_sent_items_key), "6"));
        mDbHandler.deleteDcitationFromKeepSent(mKeepSentVal); // delete sent
        // items based
        // on Keep sent
        // item value in
        // settings

        mFlashAir = (ImageButton) findViewById(R.id.imgbutton_main_flashair);
        mNewDictation = (ImageView) findViewById(R.id.img_main_new_dictate);
        mBtnEdit = (Button) findViewById(R.id.btn_main_edit);
        mBtnSendDelete = (Button) findViewById(R.id.btnSendDelete);
        mBtnSendDeleteAll = (Button) findViewById(R.id.btnSendDeleteAll);
        mBtnCancel = (Button) findViewById(R.id.btn_main_cancel);
        mBtnSortPriority = (ImageButton) findViewById(R.id.imgbutton_main_priority);
        mSettingsOption = (ImageButton) findViewById(R.id.btnSettingsOption);
        mEditSearch = (EditText) findViewById(R.id.edittext_main_search_dictation);
        mEditSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mBtnSendDelete.setEnabled(false);
        mBtnEdit.setText(getString(R.string.Property_Edit));
        mBtnSortPriority.setTag(LOW_PRIORITY_TAG);
        mBtnSortPriority.setImageResource(R.drawable.priority_button);
        dmApplication.setPriorityOn(false);
        mBtnSendDelete.setText(SEND_TAG);
        mBtnSendDeleteAll.setText(SEND_ALL_TAG);
        mBtnSendDelete.setTag(SEND_TAG);
        mBtnSendDeleteAll.setTag(SEND_ALL_TAG);
        mFlashAir.setOnClickListener(this);
        mNewDictation.setOnClickListener(this);
        mBtnEdit.setOnClickListener(this);
        mBtnSendDelete.setOnClickListener(this);
        mBtnSendDeleteAll.setOnClickListener(this);
        mBtnSortPriority.setOnClickListener(this);
        mSettingsOption.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mViewPager = (DMActivityViewPager) findViewById(R.id.DMActivityViewPager);
        initializeViewPager();
        flashAirButton();
        autoResize(mBtnEdit);
        mTabHost.setup();
        tabview = createTabView(mTabHost.getContext(), "Pending");
        mTabHost.addTab(mTabHost.newTabSpec("pending").setIndicator(tabview)
                .setContent(android.R.id.tabcontent));

        tabview = createTabView(mTabHost.getContext(), "Outbox");
        mTabHost.addTab(mTabHost.newTabSpec("outbox").setIndicator(tabview)
                .setContent(android.R.id.tabcontent));

        tabview = createTabView(mTabHost.getContext(), "Sent");
        mTabHost.addTab(mTabHost.newTabSpec("sent").setIndicator(tabview)
                .setContent(android.R.id.tabcontent));
        tabview = null;
        mTabHost.setOnTabChangedListener(this);
        mPendingFragment = (PendingTabFragment) mViewPagerAdapter.getItem(0);

        mOutboxFragment = (OutboxTabFragment) mViewPagerAdapter.getItem(1);
        mSendFragment = (SendTabFragment) mViewPagerAdapter.getItem(2);

        mEditSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int arg1, int arg2,
                                      int arg3) {
                try {
                    switch (mTabHost.getCurrentTab()) {
                        case 0:
                            if (mPendingFragment == null)
                                mPendingFragment = (PendingTabFragment) mViewPagerAdapter
                                        .getItem(0);
                            mPendingFragment.getListAdapter().getFilter()
                                    .filter(s.toString());
                            break;
                        case 1:
                            if (mOutboxFragment == null)
                                mOutboxFragment = (OutboxTabFragment) mViewPagerAdapter
                                        .getItem(1);
                            mOutboxFragment.getListAdapter().getFilter()
                                    .filter(s.toString());
                            break;
                        case 2:
                            if (mSendFragment == null)
                                mSendFragment = (SendTabFragment) mViewPagerAdapter
                                        .getItem(2);
                            mSendFragment.getListAdapter().getFilter()
                                    .filter(s.toString());
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        mEditSearch.setOnEditorActionListener(this);

        webserviceHandler = new WebserviceHandler();
        final View activityRootView = findViewById(R.id.dmactivity_root_view);

        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect r = new Rect();
                        // r will be populated with the coordinates of your view
                        // that area still visible.
                        activityRootView.getWindowVisibleDisplayFrame(r);

                        int heightDiff = activityRootView.getRootView()
                                .getHeight() - (r.bottom - r.top);
                        if (heightDiff > 100) { // if more than 100 pixels, its
                            // probably a keyboard...
                            isKeyboardShown = true;
                        } else {
                            if (isKeyboardShown) {
                                isKeyboardShown = false;
                            }
                        }
                    }
                });
        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("isFromSlash", false)) {
                if (dmApplication.getResultCode() != null)
                    onStartNotActivatedActivity();
                else
                    NetWorkAndNotActivatedDialog.getInstance().onShowDialog(this);
            }
        }
    }


    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
     * Method to show 'Not Activated' dialog when Account is not activated/deactivated state.
     */
    private void onStartNotActivatedActivity() {
        if (!dmApplication.isTimeOutDialogOnFront()) {
            dmApplication.setTimeOutDialogOnFront(true);
            dmApplication.setWantToShowDialog(false);
            baseIntent = new Intent(dmApplication.getContext(), CustomLaunchDialog.class);
            baseIntent.putExtra("Resultcode", dmApplication.getResultCode());
            baseIntent.putExtra("Message", dmApplication.getMessage());
            dmApplication.getContext().startActivity(baseIntent);
            baseIntent = null;
            dmApplication.setResultCode(null);
            dmApplication.setMessage(null);
        }
    }

    /**
     * Hide keyboard when user navigates to another view.
     */
    private void onHideKeyBoard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    @Override
    protected void onResume() {
        super.onResume();
       //Toast.makeText(DMActivity.this, "onResume", Toast.LENGTH_SHORT).show();

        isNetworkAvailable();
        isOnline();
        mDbHandler = dmApplication.getDatabaseHandler();
        try {
            setCurrentLanguage(dmApplication.getCurrentLanguage());

            if ((mTabHost.getCurrentTab() == 0 || mTabHost.getCurrentTab() == 1)
                    && dmApplication.getDeletedId() > 0) {
                if (mSelectedList != null && mSelectedList.size() > 0
                        && mSelectedList.contains(dmApplication.getDeletedId())) {
                    mSelectedList
                            .remove((Integer) dmApplication.getDeletedId());
                    switch (mTabHost.getCurrentTab()) {
                        case 0:
                            if (mPendingFragment == null)
                                mPendingFragment = (PendingTabFragment) mViewPagerAdapter
                                        .getItem(0);
                            mPendingFragment.getListAdapter().mCheckList
                                    .remove((Integer) dmApplication.getDeletedId());
                            break;
                        case 1:
                            if (mOutboxFragment == null)
                                mOutboxFragment = (OutboxTabFragment) mViewPagerAdapter
                                        .getItem(1);
                            mOutboxFragment.getListAdapter().mCheckList
                                    .remove((Integer) dmApplication.getDeletedId());
                            break;
                    }

                    selSize = mSelectedList.size();
                    enableSendDeleteButton(selSize);

                }
                dmApplication.setDeletedId(0);
            }
            if (dmApplication.onSetPending) {
                dmApplication.onSetPending = false;
                mTabHost.setCurrentTab(0);
                mBtnSortPriority.setImageResource(R.drawable.priority_button);
                mSelectedList.clear();
                // mPendingFragment.refreshList(mEditSearch.getText().toString());
                mPendingFragment.getListAdapter().initCheckList();
                mBtnSortPriority.setTag(LOW_PRIORITY_TAG);
                dmApplication.setPriorityOn(false);
                dmApplication.setOnEditState(true);
                mEditSearch.setText("");
                mEditSearch.clearFocus();
                mBtnEdit.setText(getString(R.string.Property_Edit));
                mEditFlag = false;
                dmApplication.setEditMode(false);
                mBtnSendDelete.setTag(SEND_TAG);
                mBtnSendDeleteAll.setTag(SEND_ALL_TAG);
                onRefreashList();
                selSize = mSelectedList.size();
                enableSendDeleteButton(mSelectedList.size());
                mBtnSendDeleteAll
                        .setText(getString(R.string.Recording_Label_Send_All));
            } else
                onRefreashList();
            // flashButton();
            // mSelectedList.clear();
            // enableSendDeleteButton(mSelectedList.size());
            flashAirButton();
            autoResize(mBtnEdit);
            wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (isOnceCalled) {
                if (dmApplication.getNetWorkId() > 0) {
                    wifi.disableNetwork(dmApplication.getNetWorkId());
                    dmApplication.setNetWorkId(-1);
                }
                if (dmApplication.getPreviousNetworkSSID() != null) {
                    if (dmApplication.getPreviousNetworkSSID().equals(
                            dmApplication.getFlashAirSSID()))
                        wifi.disconnect();
                    else {
                        dmApplication.setDictateUploading(true);
                        connectWifi(dmApplication.getPreviousNetworkSSID());
                    }
                } else
                    wifi.disconnect();
                isOnceCalled = false;
            }

        } catch (Exception e) {
        }
        dmApplication.setFlashAirState(false);

        //super.onResume();
        dmApplication.setTabPos(mTabHost.getCurrentTab());
        dmApplication.setContext(this);
        if (isKeyboardShown) {
            mEditSearch.setText(mEditSearch.getText().toString());
            mEditSearch.setSelection(mEditSearch.getText().toString().length());
        }

        if (dmApplication.isWantToShowDialog()
                && dmApplication.isTimeOutDialogOnFront()) {
            baseIntent = new Intent(dmApplication, CustomDialog.class);
            baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(baseIntent);
            baseIntent = null;
        }
        dmApplication.setWantToShowDialog(false);
        isOnceExecuted = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This method used to start new Dictate view. Intent values and flags for
     * new DictateView are initialised in this method.
     */
    public void startNewDictateActivity() {
        onHideKeyBoard();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        editor.putBoolean(DictateActivity.WAS_DESTROYED, false);
        editor.commit();
        dmApplication.fromWhere = 1;
        dmApplication.flashair = false;
        dmApplication.isExecuted = true;
        baseIntent = new Intent(this, DictateActivity.class);
        baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_NEW_RECORDING);
        startActivity(baseIntent);
    }

    /**
     * This method starts Settings view when user tap on Settings menu.
     */
    public void promptSettings() {
        onHideKeyBoard();
        baseIntent = new Intent(this, SettingsActivity.class);
        baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(baseIntent);
    }

    /**
     * Initializes Fragments and ViewPager adapter is initialized with
     * fragments. Sets adapter to ViewPager.
     */
    public void initializeViewPager() {
        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this,
                PendingTabFragment.class.getName()));
        fragments.add(Fragment.instantiate(this,
                OutboxTabFragment.class.getName()));
        fragments.add(Fragment.instantiate(this,
                SendTabFragment.class.getName()));

        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(),
                fragments);
        new SetAdapterTask().execute();
        mViewPager.setOnPageChangeListener(this);
    }

    /**
     * This method creates TabViews for Pending, Outbox and Sent
     *
     * @param context Context to create each TabView
     * @param text    The Tab name passed as String
     * @return The created TabView
     */
    private static View createTabView(final Context context, final String text) {
        if (text.equals("Pending")) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.pending_tab_bg, null);
            TextView tv = (TextView) view
                    .findViewById(R.id.text_pending_tab_bg);
            tv.setTextColor(Color.BLACK);
            TAB_LENGTH = tv.getText().length();
            autoResize(tv, TAB_LENGTH);
            return view;
        } else if (text.equals("Outbox")) {

            View view = LayoutInflater.from(context).inflate(
                    R.layout.outbox_tab_bg, null);
            TextView tv = (TextView) view.findViewById(R.id.text_outbox_tab_bg);
            autoResize(tv, TAB_LENGTH);

            return view;
        } else if (text.equals("Sent")) {

            View view = LayoutInflater.from(context).inflate(
                    R.layout.send_tab_bg, null);
            TextView tv = (TextView) view.findViewById(R.id.text_send_tab_bg);
            autoResize(tv, TAB_LENGTH);
            return view;
        }

        return null;

    }

    /**
     * SetAdapterTask is an AsyncTask used to set adapter to ViewPager.
     */
    private class SetAdapterTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mViewPager.setAdapter(mViewPagerAdapter);
        }
    }

    private void changeTabTextColor(String text) {

        View v = mTabHost.getTabWidget();
        TextView pendingText = ((TextView) v.findViewById(R.id.text_pending_tab_bg));
        TextView sendText = ((TextView) v.findViewById(R.id.text_send_tab_bg));
        TextView outboxText = ((TextView) v.findViewById(R.id.text_outbox_tab_bg));
        if (text.equals("Pending")) {
            pendingText.setTextColor(Color.BLACK);
            sendText.setTextColor(Color.WHITE);
            outboxText.setTextColor(Color.WHITE);
        } else if (text.equals("Outbox")) {
            pendingText.setTextColor(Color.WHITE);
            sendText.setTextColor(Color.WHITE);
            outboxText.setTextColor(Color.BLACK);
        } else if (text.equals("Sent")) {
            pendingText.setTextColor(Color.WHITE);
            sendText.setTextColor(Color.BLACK);
            outboxText.setTextColor(Color.WHITE);
        }

    }


    /**
     * Connects to Wifi network corresponds to passed Ssid.
     *
     * @param ssid Input ssid value as String
     */
    public void connectWifi(String ssid) {
        WifiConfiguration config;
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifi.disconnect();
        List<WifiConfiguration> networks = wifi.getConfiguredNetworks();

        if (networks.size() > 0) {
            for (WifiConfiguration item : networks) {
                if (item.SSID.toString().contains(ssid)) {
                    config = item;
                    int res = config.networkId;
                    wifi.enableNetwork(res, true);
                }
                item = null;
            }
        }
        networks = null;
    }

    @Override
    public void onTabChanged(String tabId) {
        if (!mEditSearch.getText().toString().equals(""))
            mEditSearch.setText("");
        mTabPosition = mTabHost.getCurrentTab();
        dmApplication.setTabPos(mTabPosition);
        mViewPager.setCurrentItem(mTabPosition);
        mSelectedList.clear();
        dmApplication.setContext(this);
        if (mBtnSendDelete.getTag().toString().equalsIgnoreCase(DEL_TAG))
            mBtnSendDelete.setText(getString(R.string.Recording_Label_Delete));
        else if (mBtnSendDelete.getTag().toString().equalsIgnoreCase(SEND_TAG))
            mBtnSendDelete.setText(getString(R.string.Recording_Label_Send));


        switch (mTabPosition) {
            case 0:
                changeTabTextColor("Pending");
                mBtnSendDelete.setVisibility(View.VISIBLE);
                mBtnSendDeleteAll.setVisibility(View.VISIBLE);
                mBtnSendDelete.setEnabled(false);
                mBtnSendDeleteAll.setEnabled(true);
                if (mPendingFragment == null)
                    mPendingFragment = (PendingTabFragment) mViewPagerAdapter
                            .getItem(0);
                onRefreashList();
                mPendingFragment.getListAdapter().initCheckList();

                break;
            case 1:
                changeTabTextColor("Outbox");
                mBtnSendDelete.setVisibility(View.VISIBLE);
                mBtnSendDeleteAll.setVisibility(View.VISIBLE);
                mBtnSendDelete.setEnabled(false);
                mBtnSendDeleteAll.setEnabled(true);
                if (mOutboxFragment == null)
                    mOutboxFragment = (OutboxTabFragment) mViewPagerAdapter
                            .getItem(1);
                onRefreashList();
if(mOutboxFragment!=null)

                    mOutboxFragment.getListAdapter().initCheckList();



                break;
            case 2:
                changeTabTextColor("Sent");
                mBtnSendDelete.setEnabled(false);
                if (mBtnSendDeleteAll.getTag().toString()
                        .equalsIgnoreCase(DEL_ALL_TAG)) {
                    mBtnSendDeleteAll.setEnabled(true);
                    mBtnSendDelete.setEnabled(false);
                    mBtnSendDelete.setVisibility(View.VISIBLE);
                    mBtnSendDeleteAll.setVisibility(View.VISIBLE);
                } else if (mBtnSendDeleteAll.getTag().toString()
                        .equalsIgnoreCase(SEND_ALL_TAG)) {
                    mBtnSendDelete.setVisibility(View.INVISIBLE);
                    mBtnSendDeleteAll.setVisibility(View.INVISIBLE);
                }
                if (mSendFragment == null)
                    mSendFragment = (SendTabFragment) mViewPagerAdapter.getItem(2);
                onRefreashList();
                mSendFragment.getListAdapter().initCheckList();
                break;

            default:
                break;
        }
        InputMethodManager imm = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditSearch.getWindowToken(), 0);
        mEditSearch.clearFocus();
        System.gc();
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        mTabHost.setCurrentTab(position);
    }

    /**
     * Invokes ConnectFlashAir view.
     */
    public void startFlashAirActivity() {
        onHideKeyBoard();
        isOnceCalled = true;
        dmApplication.setDictateUploading(true);
        baseIntent = new Intent(DMActivity.this, FlashAirMain.class);
        startActivity(baseIntent);
    }

//    private boolean checkAndRequestPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                    2);
//
//        } else {
//
//            return true;
//        }
//        return false;
//    }

    @Override
    public void onClick(View v) {
        mTabPosition = mTabHost.getCurrentTab();
        switch (v.getId()) {
            case R.id.imgbutton_main_flashair:
                pref = getSharedPreferences("Locermissions", MODE_PRIVATE);
                isLocationPermission = pref.getBoolean("Locperm", false);
                if (checkLocationPermission() && isLocationPermission) {
                    final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    boolean isGpsProviderEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    boolean isNetworkProviderEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    if (isGpsProviderEnabled && isNetworkProviderEnabled) {
                        pref = DMActivity.this.getSharedPreferences(PREFS_NAME, 0);
                        mSettingsConfig = pref.getString("Activation", mActivation);
                        pref = PreferenceManager.getDefaultSharedPreferences(this);
                        int sendOption = Integer.parseInt(pref.getString(
                                getString(R.string.send_key), "1"));
                        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifi.isWifiEnabled()) {
                            if (sendOption == 1) {
                                startFlashAirActivity();
                            } else if (sendOption == 2) {
                                if (mSettingsConfig != null
                                        && !mSettingsConfig
                                        .equalsIgnoreCase("Not Activated")) {
                                    startFlashAirActivity();
                                } else
                                    onShowNotActivatedDialog();
                            }
                        } else {
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    DMActivity.this);
                            alert.setTitle(getString(R.string.Flashair_Alert_WiFi_Connection));
                            alert.setMessage(getString(R.string.Flashair_Alert_WiFi_Accessible));
                            alert.setPositiveButton(getString(R.string.Dictate_Alert_Ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alert.create().show();
                        }
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(getResources().getString(R.string.turnOnLocation))
                                .setCancelable(false)
                                .setPositiveButton(getResources().getString(R.string.allow), new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, final int id) {
                                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                    }
                                })
                                .setNegativeButton(getResources().getString(R.string.deny), new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, final int id) {
                                        dialog.cancel();
                                    }
                                });
                        final AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
                break;
            case R.id.img_main_new_dictate:
                dmApplication.setRecordingsClickedDMActivity(true);
                if (dmApplication.newCreated) {
                    dmApplication.newCreated = false;
                }
                startNewDictateActivity();
                break;
            case R.id.btn_main_edit:
                editButtonActions(((Button) v).getText().toString().trim());
                break;
            case R.id.btnSendDelete:
                mActionForDuplication = -1;
                mResultcode = null;
                String sdTag = ((Button) v).getTag().toString();
                if (sdTag.equalsIgnoreCase(SEND_TAG)) {
                    isSendAll = false;
                    onSendButtonClicked();
                } else if (sdTag.equalsIgnoreCase(DEL_TAG))
                    onShowDeleteDialog();
                break;
            case R.id.btnSendDeleteAll:
                mActionForDuplication = -1;
                mResultcode = null;
                String sdAllTag = ((Button) v).getTag().toString();
                if (sdAllTag.equalsIgnoreCase(SEND_ALL_TAG)) {
                    isSendAll = true;
                    onSendButtonClicked();
                } else if (sdAllTag.equalsIgnoreCase(DEL_ALL_TAG))
                    onShowDeleteAllDialog();
                break;
            case R.id.imgbutton_main_priority:

                priorityButtonActions(v.getTag().toString());

                break;
            case R.id.btnSettingsOption:
                promptSettings();
                break;
            case R.id.btn_main_cancel:
                editButtonActions("Done");
                break;
        }

    }


    /**
     * Invoke edit or done actions based on the action input value when user
     * taps Edit button.
     *
     * @param action The Edit action input value as String, it may be edit or done.
     */
    private void editButtonActions(String action) {
        if (mSelectedList != null) {
            selSize = mSelectedList.size();
        } else {
            selSize = 0;
        }
        if (action.equalsIgnoreCase(getString(R.string.Property_Edit))) {
            dmApplication.setOnEditState(false);
            mBtnEdit.setText(getString(R.string.Property_Done));
            mEditFlag = true;
            dmApplication.setEditMode(true);
            mBtnSendDelete.setTag(DEL_TAG);
            mBtnSendDeleteAll.setTag(DEL_ALL_TAG);
            if (selSize > 0)
                mBtnSendDelete
                        .setText(getString(R.string.Recording_Label_Delete)
                                + "(" + selSize + ")");
            else
                mBtnSendDelete
                        .setText(getString(R.string.Recording_Label_Delete));
            mBtnSendDeleteAll
                    .setText(getString(R.string.Recording_Label_Delete_All));
            if (mTabPosition == 2) {
                mBtnSendDelete.setVisibility(View.VISIBLE);
                mBtnSendDeleteAll.setVisibility(View.VISIBLE);
            }
            autoResize(mBtnEdit);
        } else if (action.equalsIgnoreCase(getString(R.string.Property_Done))) {
            dmApplication.setOnEditState(true);
            mBtnEdit.setText(getString(R.string.Property_Edit));
            mEditFlag = false;
            dmApplication.setEditMode(false);
            mBtnSendDelete.setTag(SEND_TAG);
            mBtnSendDeleteAll.setTag(SEND_ALL_TAG);
            if (selSize > 0)
                mBtnSendDelete.setText(getString(R.string.Recording_Label_Send)
                        + "(" + selSize + ")");
            else
                mBtnSendDelete
                        .setText(getString(R.string.Recording_Label_Send));
            mBtnSendDeleteAll
                    .setText(getString(R.string.Recording_Label_Send_All));
            if (mTabPosition == 2) {
                mBtnSendDelete.setVisibility(View.INVISIBLE);
                mBtnSendDeleteAll.setVisibility(View.INVISIBLE);
            }
            autoResize(mBtnEdit);
        }
        onRefreashList();
    }

    /**
     * Invoke priority actions based on the action input value when user taps
     * Priority button.
     *
     * @param action Priority action input value as String, it may be high or low
     *               priority.
     */
    public void priorityButtonActions(String action) {
        if (action.equalsIgnoreCase(LOW_PRIORITY_TAG)) {
            mBtnSortPriority.setImageResource(R.drawable.priority_button_sel);
            mBtnSortPriority.setTag(HIGH_PRIORITY_TAG);
            dmApplication.setPriorityOn(true);

        } else if (action.equalsIgnoreCase(HIGH_PRIORITY_TAG)) {
            mBtnSortPriority.setImageResource(R.drawable.priority_button);
            mBtnSortPriority.setTag(LOW_PRIORITY_TAG);
            dmApplication.setPriorityOn(false);
        }
        onRefreashList();
    }

    /**
     * @see com.olympus.dmmobile.RecordingSelectedListener#onRecordingSelected(int,
     * boolean)
     */
    @Override
    public void onRecordingSelected(int dictId, boolean isChecked) {

        if (isChecked) {
            mSelectedList.add(dictId);
        } else {
            mSelectedList.remove(Integer.valueOf(dictId));
        }
        selSize = mSelectedList.size();
        enableSendDeleteButton(selSize);
    }

    /**
     * Enables or disables Send or Delete button based on the number of
     * Dictations selected.
     *
     * @param size Number of Dictations selected value input as int.
     */
    public void enableSendDeleteButton(int size) {
        if (mEditFlag) {
            if (size > 0) {
                mBtnSendDelete.setEnabled(true);
                mBtnSendDelete
                        .setText(getString(R.string.Recording_Label_Delete)
                                + "(" + selSize + ")");
            } else {
                mBtnSendDelete
                        .setText(getString(R.string.Recording_Label_Delete));
                mBtnSendDelete.setEnabled(false);
            }
        } else {
            if (size > 0) {
                if (mTabPosition == 2) {
                    mBtnSendDelete
                            .setText(getString(R.string.Recording_Label_Send));
                    mBtnSendDelete.setEnabled(false);
                } else {
                    mBtnSendDelete.setEnabled(true);
                    mBtnSendDelete
                            .setText(getString(R.string.Recording_Label_Send)
                                    + "(" + selSize + ")");
                }
            } else {
                mBtnSendDelete
                        .setText(getString(R.string.Recording_Label_Send));
                mBtnSendDelete.setEnabled(false);
            }
        }
    }

    /**
     * Invokes when Send button is Tapped. Send Dialog will be shown based on
     * the Send option in Settings.
     */
    private void onSendButtonClicked() {
        switch (mTabPosition) {
            case 0:
                pref = PreferenceManager.getDefaultSharedPreferences(this);
                /*
                 * To check which is option selected in Settings.
                 */
                switch (Integer.parseInt(pref.getString(
                        getString(R.string.send_key), "1"))) {
                    case 1:
                        onShowSendViaEmailDialog();
                        break;
                    case 2:
                        pref = getSharedPreferences(PREFS_NAME, 0);
                        mSettingsConfig = pref.getString("Activation", mActivation);
                        /*
                         * To check the application's ODP account is activated or not.
                         */
                        if (mSettingsConfig != null
                                && !mSettingsConfig.equalsIgnoreCase("Not Activated"))
                            onShowSendViaServerDialog();
                        else
                            onShowNotActivatedDialog();
                        break;
                }
                ;
                break;
            case 1:
                pref = getSharedPreferences(PREFS_NAME, 0);
                mSettingsConfig = pref.getString("Activation", mActivation);
                /*
                 * To check the application's ODP account is activated or not.
                 */
                if (mSettingsConfig != null
                        && !mSettingsConfig.equalsIgnoreCase("Not Activated")) {
                    /*
                     * if(mDbHandler.getOutboxDictationsToEnableSendAll().getCount()>
                     * 0) onShowSendViaServerDialog(); else
                     * onSendToServerDictation();
                     */
                    if (!isSendAllInitializing) {
                        isSendAllInitializing = true;
                        mBtnSendDeleteAll.setEnabled(false);
                        onSendToServerDictation();
                    }
                } else
                    onShowNotActivatedDialog();
                break;
        }
    }

    /**
     * Invokes dialog when user Taps Send or Send All button if 'Send To Server'
     * is selected in Send options in Settings.
     */
    private void onShowSendViaServerDialog() {
        if (mAlertDialog == null || !mAlertDialog.isShowing()) {
            mBuilder = new AlertDialog.Builder(this);
            if (!isSendAll) {
                mBuilder.setTitle(getString(
                        R.string.Recording_Alerts_Send_Recordings).replace("#",
                        mSelectedList.size() + ""));
                mBuilder.setMessage(getString(
                        R.string.Recording_Alerts_Send_Recordings_Message)
                        .replace("#", mSelectedList.size() + ""));
            } else {
                mBuilder.setTitle(getString(R.string.Recording_Alerts_Send_All_Recordings));
                mBuilder.setMessage(getString(R.string.Recording_Alerts_Send_All_Recordings_Message));
                /*
                 * switch (mTabPosition) { case 0:
                 * mBuilder.setTitle(getString(R.
                 * string.Recording_Alerts_Send_All_Recordings));
                 * mBuilder.setMessage
                 * (getString(R.string.Recording_Alerts_Send_All_Recordings_Message
                 * )); break; case 1:
                 * mBuilder.setTitle(getString(R.string.ResendDictationTitle));
                 * mBuilder
                 * .setMessage(getString(R.string.ResendDictationMessage));
                 * break; }
                 */
            }
            mBuilder.setPositiveButton(
                    isSendAll ? /* (mTabPosition==0? */getString(R.string.Recording_Alerts_Send_All_Server)
                            :
                            /* getString(R.string.Flashair_Label_Send)): */getString(R.string.Button_Sendtosever),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            switch (mTabPosition) {
                                case 0:
                                    onSendToServerAction();
                                    break;
                                case 1:
                                    onSendToServerDictation();
                                    break;
                            }
                        }
                    });
            mBuilder.setNegativeButton(
                    getString(R.string.Recording_Alerts_Cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            mAlertDialog = mBuilder.create();
            mAlertDialog.show();
        }
    }

    /**
     * This method is used to take an appropriate action based on Network availability.
     */
    private void onSendToServerAction() {
        if (DMApplication.isONLINE()) {
            getSettingsAttribute();
            mWebServiceGetSettings = null;
            Intent myService = new Intent(this, ConvertAndUploadService.class);

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(myService);
//            } else {
//                startService(myService);
//            }
            mWebServiceGetSettings = new WebServiceGetSettings();
            mWebServiceGetSettings.execute();
        } else {
            onSetRecentSettings();
            if (mAudioDelivery.trim().equalsIgnoreCase("3"))
                onPromptSendRecordings();
            else {
                if (mAudioDelivery.equalsIgnoreCase("1"))
                    isSplittable = 1;
                else
                    isSplittable = 0;
                onAfterSettingsDelivery();
            }
        }
    }

    /**
     * Invokes dialog when user Taps Send or Send All button if 'Send via Email'
     * is selected in Send options in Settings.
     */
    private void onShowSendViaEmailDialog() {
        mBuilder = new AlertDialog.Builder(this);
        if (!isSendAll) {
            mBuilder.setTitle(getString(
                    R.string.Recording_Alerts_Send_Recordings).replace("#",
                    mSelectedList.size() + ""));
            mBuilder.setMessage(getString(
                    R.string.Recording_Alerts_Send_Recordings_Message).replace(
                    "#", mSelectedList.size() + ""));
        } else {
            mBuilder.setTitle(getString(R.string.Recording_Alerts_Send_All_Recordings));
            mBuilder.setMessage(getString(R.string.Recording_Alerts_Send_All_Recordings_Message));
        }
        mBuilder.setPositiveButton(
                isSendAll ? getString(R.string.Recording_Alerts_Send_All_Email)
                        : getString(R.string.Button_Sendviaemail),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onSendDictationsForEmail();
                    }
                });
        mBuilder.setNegativeButton(getString(R.string.Recording_Alerts_Cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        mBuilder.create().show();
    }

    /**
     * Invokes dialog when user try to send Dictation if 'Send to Server' is
     * selected in Send option of Settings and the server is not activated.
     */
    private void onShowNotActivatedDialog() {
        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle(getString(R.string.Ils_Result_Not_Activated));
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
     * Invokes dialog when user taps Delete button.
     */
    private void onShowDeleteDialog() {
        String tName = null;
        if (mTabPosition == 0)
            tName = getString(R.string.Recording_Label_Pending);
        else if (mTabPosition == 1)
            tName = getString(R.string.Recording_Label_Outbox);
        else if (mTabPosition == 2)
            tName = getString(R.string.Recording_Label_Sent);
        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setCancelable(false);
        mBuilder.setTitle(getString(R.string.Recording_Alerts_Delete_Recordings)
                .replace("#", mSelectedList.size() + ""));
        mBuilder.setMessage(getString(
                R.string.Recording_Alerts_Delete_Pending_Message).replace("#",
                mSelectedList.size() + "").replace("*", tName));
        mBuilder.setPositiveButton(getString(R.string.Recording_Alerts_Delete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        for (int i : mSelectedList) {
                            mDictationCard = mDbHandler
                                    .getDictationCardWithId(i);
                            onDeleteDictations();
                        }
                        onRefreashList();
                        mSelectedList.clear();
                        enableSendDeleteButton(0);
                    }
                });
        mBuilder.setNegativeButton(getString(R.string.Recording_Alerts_Cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        mBuilder.create().show();
    }

    /**
     * Invokes dialog when user taps DeleteAll button.
     */
    private void onShowDeleteAllDialog() {
        String tName = null;
        if (mTabPosition == 0)
            tName = getString(R.string.Recording_Label_Pending);
        else if (mTabPosition == 1)
            tName = getString(R.string.Recording_Label_Outbox);
        else if (mTabPosition == 2)
            tName = getString(R.string.Recording_Label_Sent);
        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle(getString(R.string.Recording_Alerts_Delete_All_recordings));
        mBuilder.setMessage(getString(
                R.string.Recording_Alerts_Delete_Recording_Pending_Message)
                .replace("#", tName));
        mBuilder.setPositiveButton(
                getString(R.string.Recording_Alerts_Delete_All),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (mTabPosition) {
                            case 0:
                                mCursor = mDbHandler.getDictationsInPendingAll();
                                break;
                            case 1:
                                mCursor = mDbHandler
                                        .getOutboxDictationsToEnableDeleteAll();
                                break;
                            case 2:
                                mCursor = mDbHandler.getSentDictations();
                                break;
                            default:
                                break;
                        }
                        if (mCursor.moveToFirst()) {
                            do {
                                mDictationCard = mDbHandler
                                        .getSelectedDicts(mCursor);
                                onDeleteDictations();
                            } while (mCursor.moveToNext());
                        }
                        mCursor.close();
                        onRefreashList();
                        mSelectedList.clear();
                        enableSendDeleteButton(0);
                    }
                });
        mBuilder.setNegativeButton(getString(R.string.Recording_Alerts_Cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        mBuilder.create().show();
    }

    /**
     * Invokes when user taps Delete button on dialog with delete message.
     * Delete selected/all dictations from the database ,and remove file's and
     * directory from SDcard.
     */
    private void onDeleteDictations() {
        mDbHandler.deleteDictation(mDictationCard);
        DMApplication.deleteDir(DMApplication.DEFAULT_DIR + "/Dictations/"
                + mDictationCard.getSequenceNumber());
        mDictationCard = null;
    }

    /**
     * To resize the text size of button when the text is longer than the button
     * size.
     *
     * @param button Button value input as type Button
     */
    public void autoResize(Button button) {
        Spannable span = new SpannableString(button.getText().toString());
        if (button.getText().length() >= 4 && button.getText().length() < 6) {

            span.setSpan(new RelativeSizeSpan(1f), 0,
                    button.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(span);
        } else if (button.getText().length() >= 6
                && button.getText().length() < 8) {

            span.setSpan(new RelativeSizeSpan(.8f), 0, button.getText()
                    .length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(span);
        } else if (button.getText().length() >= 8
                && button.getText().length() < 10) {

            span.setSpan(new RelativeSizeSpan(.7f), 0, button.getText()
                    .length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(span);
        } else if (button.getText().length() >= 10) {

            span.setSpan(new RelativeSizeSpan(.6f), 0, button.getText()
                    .length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(span);
        }
        span = null;
    }

    /**
     * @see com.olympus.dmmobile.ConvertAndUploadService.UploadStatusChangeListener#onUploadStatusChanged()
     */
    @Override
    public void onUploadStatusChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onRefreashList();
            }
        });
    }

    /**
     * Get all basic values from application preferences for ILS Communication.
     */
    private void getSettingsAttribute() {
        pref = this.getSharedPreferences(PREFS_NAME, 0);
        if (pref.getString("UUID", mGetuuid) != null)
            prefUUID = pref.getString("UUID", mGetuuid);
        mUrl = dmApplication.getUrl();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getString(getString(R.string.email_key), mGetemail) != null)
            mEmail = pref.getString(getString(R.string.email_key), mGetemail);
        baseEncoding = new Base64_Encoding();
        base64value = baseEncoding.base64(prefUUID + ":" + mEmail);
    }

    /**
     * To Get updated settings from an ODP Server.
     */
    private class WebServiceGetSettings extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(DMActivity.this);
            isCriticalErrorOccures = false;
            dialog.setCancelable(false);
            dialog.setMessage(getResources()
                    .getString(R.string.Dictate_Loading));
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mGetSettingsResponse = webserviceHandler
                        .onRequestBackgroundSettings(mUrl + "/" + prefUUID
                                + "/Settings", base64value);
                if (mGetSettingsResponse != null) {
                    if (!mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
                        settingParser = new Settingsparser(mGetSettingsResponse);
                        settingParser.parse(getlanguage());
                        mResultcode = settingParser.getRootObjects().get(0).getResult_code().trim();
                    }
                }
            } catch (Exception e) {
                mGetSettingsResponse = null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            isSplittable = 0;
            try {
                if (mGetSettingsResponse != null) {
                    if (!mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
                        if (dialog.isShowing())
                            dialog.dismiss();
                        /*
                         * perform some actions based on the response.
                         */
                        if (mResultcode.equalsIgnoreCase("2000")) {
                            onRefreashList();
                            mWorktype = null;
                            worktypeobject = settingParser
                                    .getWorkTypeListObjects();
                            for (int i = 0; i < worktypeobject.size(); i++) {
                                if (mWorktype == null)
                                    mWorktype = worktypeobject.get(i)
                                            .getWorktype();
                                else
                                    mWorktype = mWorktype
                                            + ":"
                                            + worktypeobject.get(i)
                                            .getWorktype();
                            }
                            mAudioDelivery = settingParser.getSettingsObjects()
                                    .get(0).getDelivery();
                            mAudioEncrypt = settingParser.getAudioObjects()
                                    .get(0).getEncryption();
                            mAudioFormat = settingParser.getAudioObjects()
                                    .get(0).getFormat();
                            mAudioPassword = settingParser.getAudioObjects()
                                    .get(0).getPassword();
                            mWorktypeListname = settingParser
                                    .getSettingsObjects().get(0)
                                    .getWorktypelist();
                            mAuthorName = settingParser.getSettingsObjects()
                                    .get(0).getAuthor();
                        } else if (mResultcode.equalsIgnoreCase("5000")
                                || mResultcode.equalsIgnoreCase("5001"))
                            onSetRecentSettings();
                            /*
                             * To check the result is critical error.
                             */
                        else if (mResultcode.equalsIgnoreCase("4000")
                                || mResultcode.equalsIgnoreCase("4006")
                                || mResultcode.equalsIgnoreCase("4007")
                                || mResultcode.equalsIgnoreCase("4008")
                                || mResultcode.equalsIgnoreCase("4009")
                                || mResultcode.equalsIgnoreCase("5002")) {
                            isCriticalErrorOccures = true;
                            baseIntent = new Intent(
                                    "com.olympus.dmmobile.action.Test");
                            baseIntent.putExtra("isWantWholeToTimeOut", true);
                            sendBroadcast(baseIntent);// send a notification to
                            // background.
                            baseIntent = null;
                            if (mResultcode.equalsIgnoreCase("4007")) {
                                pref = getSharedPreferences(PREFS_NAME, 0);
                                editor = pref.edit();
                                editor.putString("Activation", "Not Activated");
                                editor.commit();
                            }
                            /*
                             * show critical error dialog with respect to the
                             * response.
                             */
                            if (!dmApplication.isTimeOutDialogOnFront()) {
                                dmApplication.setTimeOutDialogOnFront(true);
                                dmApplication.setErrorCode(mResultcode);
                                baseIntent = new Intent(dmApplication,
                                        CustomDialog.class);
                                if (mResultcode.equalsIgnoreCase("5002"))
                                    dmApplication.setErrorMessage(settingParser
                                            .getMessage());
                                baseIntent
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(baseIntent);
                                baseIntent = null;
                            }
                        } else if (mResultcode.equalsIgnoreCase("4001")
                                || mResultcode.equalsIgnoreCase("4002")
                                || mResultcode.equalsIgnoreCase("4003")
                                || mResultcode.equalsIgnoreCase("4004")
                                || mResultcode.equalsIgnoreCase("4005")) {
                            onSetRecentSettings();
                        } else
                            onSetRecentSettings();
                    } else {
                        if (dialog.isShowing())
                            dialog.dismiss();
                        onSetRecentSettings();
                    }
                } else {
                    if (dialog.isShowing())
                        dialog.dismiss();
                    onSetRecentSettings();
                }
                if (!isCriticalErrorOccures) {
                    if (mAudioDelivery.trim().equalsIgnoreCase("3"))
                        onPromptSendRecordings();
                    else {
                        if (mAudioDelivery.equalsIgnoreCase("1"))
                            isSplittable = 1;
                        else
                            isSplittable = 0;
                        onAfterSettingsDelivery();
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * When any httpError occurs during the ILS communication, then get basic
     * values of server settings from an Application preference for initiate
     * settings to the dictation.
     */
    private void onSetRecentSettings() {
        pref = PreferenceManager.getDefaultSharedPreferences(DMActivity.this);
        mAudioDelivery = pref.getString(getString(R.string.Audio_delivery),
                mAudioDelivery);
        mAudioEncrypt = pref.getString(
                getString(R.string.Audio_Encryption_key), mAudioEncrypt);
        mAudioFormat = pref.getString(getString(R.string.Audio_Format_key),
                mAudioFormat);
        mAudioPassword = pref.getString(getString(R.string.Audio_Password_key),
                mAudioPassword);
        mWorktype = pref.getString(getString(R.string.Worktype_Server_key),
                mWorktype);
        mWorktypeListname = pref.getString(
                getString(R.string.Worktype_List_name_Key), mWorktypeListname);
        mAuthorName = pref.getString(getString(R.string.author_key),
                mAuthorName);
    }

    /**
     * Assign server settings to the dictation, and continue sending operations
     * to an ODP.
     */
    private void onAfterSettingsDelivery() {
        pref = PreferenceManager.getDefaultSharedPreferences(DMActivity.this);
        editor = pref.edit();
        editor.putString(getString(R.string.Audio_delivery), mAudioDelivery);
        editor.putString(getString(R.string.Audio_Encryption_key),
                mAudioEncrypt);
        editor.putString(getString(R.string.Audio_Format_key), mAudioFormat);
        editor.putString(getString(R.string.Audio_Password_key), mAudioPassword);
        editor.putString(getString(R.string.Worktype_Server_key), mWorktype);
        editor.putString(getString(R.string.Worktype_List_name_Key),
                mWorktypeListname);
        editor.putString(getString(R.string.author_key), mAuthorName);
        editor.commit();
        if (mResultcode != null && mResultcode.equalsIgnoreCase("2000")) {
            pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (pref.getString(getResources().getString(R.string.author_key), "").equalsIgnoreCase(mAuthorName))
                mDbHandler.updateDicationName(settingParser.getSettingsObjects().get(0).getAuthor());
        }
        if (mActionForDuplication == 1 && mTabPosition == 2)
            onStartDuplicateTask();
        else
            onSendToServerDictation();
    }

    /**
     * When the delivery option of dictation in server is client based, then
     * shows dialog to choose the delivery option(E-mail or FTP).
     */
    private void onPromptSendRecordings() {
        if (mAlertDialog != null) {
            if (mAlertDialog.isShowing())
                mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(DMActivity.this);
        View layout = inflater.inflate(
                R.layout.activity_dictate_property_image_menu, (ViewGroup) this
                        .findViewById(R.id.relative_property_image_menu));
        Button mEmail = (Button) layout
                .findViewById(R.id.btn_property_image_takephoto);
        mEmail.setText(getString(R.string.Button_Email));
        Button mFtp = (Button) layout
                .findViewById(R.id.btn_property_image_choose_existing);
        mFtp.setText(getString(R.string.Button_FTP));
        Button mCancel = (Button) layout
                .findViewById(R.id.btn_property_image_cancel);
        mEmail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isSplittable = 1;
                mAlertDialog.dismiss();
                onAfterSettingsDelivery();
            }
        });
        mFtp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isSplittable = 0;
                mAlertDialog.dismiss();
                onAfterSettingsDelivery();
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
        if (mActionForDuplication != 1)
            mSelectedSize = mSelectedList.size();
        if (mSelectedSize == 0 || isSendAll) {
            mAlertDialog
                    .setTitle(getString(R.string.Recording_Alerts_Send_All_Recordings));
            mAlertDialog
                    .setMessage(getString(R.string.Recording_Alerts_Send_All_Recordings_Message));
        } else {
            mAlertDialog.setTitle(getString(
                    R.string.Recording_Alerts_Send_Recordings).replace("#", mSelectedSize + ""));
            mAlertDialog.setMessage(getString(
                    R.string.Recording_Alerts_Send_Recordings_Message).replace("#", mSelectedSize + ""));
        }
        mAlertDialog.show();
    }

    /**
     * To prepare the dictation for an ODP Communication and file conversion,
     * and also groups the dictations when an ODP delivery option is E-mail.
     */
    private void onSendToServerHandler() {
        try {
            /*
             * To check the dictation is already uploaded or not. When the
             * dictation is already uploaded, then add that dictation to
             * 'Waiting To Send'queue.
             */
            if (mDbHandler
                    .toCheckMoveToQuerying(mDictationCard.getDictationId()) > 0
                    && mDbHandler.getSplitUploadedCount(mDictationCard
                    .getDictationId()) == mDbHandler
                    .getCountTotalNoOfSplits(mDictationCard
                            .getDictationId())) {
                mDictationCard.setStatus(DictationStatus.WAITING_TO_SEND1
                        .getValue());
                mDbHandler.updateDictationStatus(
                        mDictationCard.getDictationId(),
                        mDictationCard.getStatus());
                mDbHandler.updateMainStatus(300, mDictationCard.getGroupId());
                mDbHandler.updateQueryPriority(mDictationCard.getDictationId());
            } else {
                /**
                 * New - Resends Dictation from Outbox.
                 */
                if (mTabPosition == 1
                        && (mDictationCard.getStatus() == DictationStatus.SENDING
                        .getValue()
                        || mDictationCard.getStatus() == DictationStatus.RETRYING1
                        .getValue() || mDictationCard
                        .getStatus() == DictationStatus.RETRYING2
                        .getValue())
                        || mDictationCard.getStatus() == DictationStatus.RETRYING3
                        .getValue()) {
                    if (mDictationCard.getGroupId() < 1)
                        mDbHandler.updateGroupIdDictsView(
                                mDictationCard.getDictationId(),
                                mDbHandler.getGroupId());
                    mDbHandler.updateDictationStatus(
                            mDictationCard.getDictationId(),
                            DictationStatus.SENDING.getValue());
                    if (!hasOutboxResent) {
                        baseIntent = new Intent(
                                "com.olympus.dmmobile.action.Test");
                        baseIntent.putExtra("isWantToCheckStreaming", true);
                        sendBroadcast(baseIntent);
                        hasOutboxResent = true;
                    }
                } else {
                    mDictationCard.setStatus(DictationStatus.OUTBOX.getValue());
                    mDictationCard.setIsActive(0);
                    mDbHandler.updateIsActive(mDictationCard);
                    pref = PreferenceManager
                            .getDefaultSharedPreferences(DMActivity.this);
                    if (mTabPosition == 0) {/*
                     * Assign an ODP server settings
                     * values to the dictation.
                     */
                        mDictationCard.setFileSplittable(isSplittable);

                        mDictationCard.setDssVersion(Integer.parseInt(pref
                                .getString(
                                        getString(R.string.Audio_Format_key),
                                        mAudioFormat)));
                        mDictationCard.setDssEncryptionPassword(pref.getString(
                                getString(R.string.Audio_Password_key),
                                mAudioPassword));
                        mDictationCard
                                .setEncryptionVersion(Integer.parseInt(pref
                                        .getString(
                                                getString(R.string.Audio_Encryption_key),
                                                mAudioEncrypt)));
                        if (mDictationCard.getEncryptionVersion() > 0)
                            mDictationCard.setEncryption(1);
                        else
                            hasNoEncript = true;
                        mDictationCard.setAuthor(pref.getString(
                                getString(R.string.author_key), mAuthorName));
                        if (isSplittable == 1)
                            mDictationCard.setDeliveryMethod(1);
                        else
                            mDictationCard.setDeliveryMethod(2);

                        mDbHandler.updateSettingsAttributes(mDictationCard);
                    }
                    filePath = DMApplication.DEFAULT_DIR + "/Dictations/"
                            + mDictationCard.getSequenceNumber() + "/"
                            + mDictationCard.getDictFileName();
                    cardSize = 0;
                    hasConvertedFilesExists = true;
                    baseFileExists = true;

                    if (mDictationCard.getIsThumbnailAvailable() == 1) {
                        file = new File(filePath + ".jpg");
                        if (!file.exists()) {
                            mDictationCard.setIsThumbnailAvailable(0);
                            mDbHandler
                                    .updateIsThumbnailAvailable(mDictationCard);
                        } else {
                            if (mDictationCard.isFileSplittable() == 1)
                                cardSize = dmApplication
                                        .getImageFileSize(filePath);
                        }
                    }
                    /*
                     * To check selected Tab is 'Outbox' or the dictation isn't
                     * download'd from flash air.
                     */
                    if (mDictationCard.getIsFlashAir() == 0 && mTabPosition == 1) {
                        if (mDictationCard.getIsConverted() == 1) {
                            mDictationCard
                                    .setFilesList(mDbHandler
                                            .getFileList(mDictationCard
                                                    .getDictationId()));
                            for (int i = 0; i < mDictationCard.getFilesList()
                                    .size(); i++) {
                                filesCard = mDictationCard.getFilesList().get(i);
                                if (filesCard.getTransferStatus() == 2) {
                                    filesCard.setJobNumber("");
                                    filesCard.setTransferId("");
                                    mDbHandler
                                            .updateTransferIdAndJobNo(filesCard);
                                }
                                filePath = DMApplication.DEFAULT_DIR
                                        + "/Dictations/"
                                        + mDictationCard.getSequenceNumber()
                                        + "/" + filesCard.getFileName();
                                file = new File(
                                        filePath
                                                + "."
                                                + DMApplication
                                                .getDssType(mDictationCard
                                                        .getDssVersion()));
                                if (!file.exists()) {
                                    cardSize = 0;
                                    mDictationCard.setIsConverted(0);
                                    mDbHandler.updateIsConverted(mDictationCard);
                                    hasConvertedFilesExists = false;
                                    break;
                                } else {
                                    hasConvertedFilesExists = true;
                                    if (mDictationCard.isFileSplittable() == 1)
                                        cardSize = cardSize
                                                + dmApplication
                                                .getFileSize(
                                                        filePath,
                                                        mDictationCard
                                                                .getDssVersion());
                                }
                            }
                        } else {
                            hasConvertedFilesExists = false;
                            mDbHandler.deleteFileList(mDictationCard);
                        }
                    } else
                        hasConvertedFilesExists = false;
                    /*
                     * To check selected Tab is 'Pending', or execute the
                     * converted/flash air file not exists.
                     */
                    if (mTabPosition == 0 || !hasConvertedFilesExists) {
                        filePath = DMApplication.DEFAULT_DIR + "/Dictations/"
                                + mDictationCard.getSequenceNumber() + "/"
                                + mDictationCard.getDictFileName();
                        file = new File(filePath
                                + "."
                                + DMApplication.getDssType(mDictationCard
                                .getDssVersion()));
                        if (mDictationCard.getIsFlashAir() == 1 && file.exists()) {
                            mDictationCard.setIsConverted(1);
                            mDbHandler.updateIsConverted(mDictationCard);
                            filesCard = new FilesCard();
                            filesCard.setFileId(mDictationCard.getDictationId());
                            filesCard.setFileIndex(0);
                            mDbHandler.updateTransferIdAndJobNo(filesCard);
                            mDbHandler.updateFileStatus(filesCard);
                            if (mDictationCard.isFileSplittable() == 1)
                                cardSize = cardSize
                                        + DMApplication.getExpectedDSSFileSize(
                                        mDictationCard.getDssVersion(),
                                        filePath);
                            mDictationCard.setStatus(DictationStatus.SENDING
                                    .getValue());
                            mDbHandler.updateDictationStatus(
                                    mDictationCard.getDictationId(),
                                    mDictationCard.getStatus());
                        } else {
                            file = new File(filePath + ".wav");
                            if (file.exists()) {
                                if (mDictationCard.isFileSplittable() == 1)
                                    cardSize = cardSize
                                            + DMApplication
                                            .getExpectedDSSFileSize(
                                                    mDictationCard
                                                            .getDssVersion(),
                                                    filePath);
                            } else {
                                cardSize = 0;
                                baseFileExists = false;
                                mDictationCard
                                        .setStatus(DictationStatus.SENDING_FAILED
                                                .getValue());
                                mDbHandler.updateDictationStatus(
                                        mDictationCard.getDictationId(),
                                        mDictationCard.getStatus());
                                return;
                            }
                        }
                    }
                    if (emailGroupId < 1) {
                        emailGroupId = mDbHandler.getGroupId();
                        dmApplication.setCurrentGroupId(emailGroupId);
                    }
                    /*
                     * To check the delivery option of dictation is 'FTP' or
                     * not, based on FileSplit status.
                     */
                    if (mDictationCard.isFileSplittable() == 1) {
                        /*
                         * Group dictations, when the file size less than 23 MB
                         */
                        if (cardSize <= maxSize
                                && (baseFileExists || hasConvertedFilesExists)) {
                            emailGroupFileSize = emailGroupFileSize + cardSize;
                            if (emailGroupFileSize > maxSize) {
                                emailGroupId = mDbHandler.getGroupId();
                                dmApplication.setCurrentGroupId(emailGroupId);
                                emailGroupFileSize = cardSize;
                            }
                            mDictationCard.setGroupId(emailGroupId);
                            mDbHandler.updateGroupIdDictsView(
                                    mDictationCard.getDictationId(),
                                    mDictationCard.getGroupId());
                        } else if (cardSize > maxSize) {
                            emailGroupFileSize = 0;
                            hasMultipleSplits = true;
                            mDictationCard.setGroupId(mDbHandler.getGroupId());
                            mDbHandler.updateGroupIdDictsView(
                                    mDictationCard.getDictationId(),
                                    mDictationCard.getGroupId());
                            emailGroupId = mDbHandler.getGroupId();
                            dmApplication.setCurrentGroupId(emailGroupId);
                        }
                    } else if (mDictationCard.isFileSplittable() == 0) {
                        mDictationCard.setGroupId(mDbHandler.getGroupId());
                        mDbHandler.updateGroupIdDictsView(
                                mDictationCard.getDictationId(),
                                mDictationCard.getGroupId());
                        if (mTabPosition == 0)
                            emailGroupId = mDbHandler.getGroupId();
                        else if (emailGroupId == mDictationCard.getGroupId())
                            emailGroupId = mDbHandler.getGroupId();
                        dmApplication.setCurrentGroupId(emailGroupId);
                    }
                    if (mDictationCard.getIsConverted() == 1)
                        mDictationCard.setStatus(DictationStatus.SENDING
                                .getValue());
                    mDbHandler.updateDictationStatus(
                            mDictationCard.getDictationId(),
                            mDictationCard.getStatus());
                    /*
                     * To check the dictation is already converted or not.
                     */
                    if (mDbHandler.getNonConvertedGroupCount(mDictationCard
                            .getGroupId()) == 0)
                        mDbHandler.updateMainStatus(200,
                                mDictationCard.getGroupId());
                    else
                        mDbHandler.updateMainStatus(0,
                                mDictationCard.getGroupId());
                }
            }
            mDictationCard = null;
        } catch (Exception e) {
        }
    }

    /**
     * Prepare selected dictations for convert\ uploading process, and call the
     * background service.
     */
    private void onSendToServerDictation() {
        dmApplication.setWaitConvertion(true);
        emailGroupId = mDbHandler.getGroupId();
        dmApplication.setCurrentGroupId(emailGroupId);
        emailGroupFileSize = 0;
        hasMultipleSplits = false;
        hasNoEncript = false;
        if (isSendAll) {
            switch (mTabPosition) {
                case 0:
                    mCursor = mDbHandler.getDictationsInPendingAll();
                    break;
                case 1:
                    mCursor = mDbHandler.getOutboxDictationsToEnableSendAll();
                    break;
            }
            if (mCursor.moveToFirst()) {
                do {
                    mDictationCard = mDbHandler.getSelectedDicts(mCursor);
                    onSendToServerHandler();
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        } else {
            for (int i : mSelectedList) {
                mDictationCard = mDbHandler.getDictationCardWithId(i);
                onSendToServerHandler();
            }
        }
        onRefreashList();
        mSelectedList.clear();
        switch (mTabPosition) {
            case 0:
                mPendingFragment.getListAdapter().initCheckList();
                break;
            case 1:
                mOutboxFragment.getListAdapter().initCheckList();
                break;
            default:
                break;
        }
        dmApplication.setCurrentGroupId(0);
        dmApplication.setWaitConvertion(false);
        baseIntent = new Intent("com.olympus.dmmobile.action.Test");
        baseIntent.putExtra("isWantToUpdate", true);
        sendBroadcast(baseIntent);
        enableSendDeleteButton(0);
        pref = PreferenceManager.getDefaultSharedPreferences(DMActivity.this);
        if (mTabPosition == 0) {
            if (hasNoEncript && !pref.getBoolean(DONT_SHOW_KEY, false))
                promptSecurityDialog();
            else if (hasMultipleSplits)
                onSendToServerEmailHasAbove23MB();
        }
        hasOutboxResent = false;
        isSendAllInitializing = false;
    }

    /**
     * Prepare the dictation sending process for free User/ the Email option is
     * selected on application settings.
     */
    private void onSendDictationsForEmail() {
        dictationCards = new ArrayList<DictationCard>();
        if (isSendAll) {
            mCursor = mDbHandler.getDictationsInPendingAll();
            if (mCursor.moveToFirst()) {
                do {
                    mDictationCard = mDbHandler.getSelectedDicts(mCursor);
                    dictationCards.add(mDictationCard);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        } else {
            for (int i : mSelectedList) {
                mDictationCard = mDbHandler.getDictationCardWithId(i);
                dictationCards.add(mDictationCard);
            }
        }
        onRefreashList();
        new SendViaEmailTask().execute();
    }

    /**
     * Shows notification message, when the selected dictation has size more
     * than 23MB.
     */
    private void onSendToServerEmailHasAbove23MB() {
        AlertDialog.Builder alert = new AlertDialog.Builder(DMActivity.this);
        alert.setCancelable(true);
        alert.setMessage(getString(R.string.Dictate_Alert_Recording_File_Large_Message));
        alert.setPositiveButton(getString(R.string.Dictate_Alert_Ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alert.create().show();
    }

    /**
     * To convert the selected dictations file to 'amr' in foreground.
     */
    private class SendViaEmailTask extends AsyncTask<Void, Void, Void> {
        private Dialog dialog;
        private TextView txtvAMRFileName = null, txtvMessage = null;
        private ArrayList<String> removedCards = null;
        private boolean conversionFailed = false;
        private boolean hasNoMemory = false;

        public SendViaEmailTask() {
            removedCards = new ArrayList<String>();
        }

        @Override
        protected void onPreExecute() {
            dialog = new Dialog(DMActivity.this);
            dialog.setTitle(getString(R.string.Dictate_Alert_File_Conversion));
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.amr_sending_dialog);
            txtvAMRFileName = (TextView) dialog
                    .findViewById(R.id.txtvAmrFileName);
            txtvMessage = (TextView) dialog.findViewById(R.id.txtvMessage);
            txtvMessage
                    .setText(getString(R.string.Dictate_Alert_Compress_Recording_Background_message));

            dialog.show();
            Log.d("sendVia email", "thread in pre execution");
        }

        @Override
        protected Void doInBackground(Void... params) {

            stat = new StatFs(Environment.getExternalStorageDirectory()
                    .getPath());
            for (int i = 0; i < dictationCards.size(); i++) {
                mDictationCard = dictationCards.get(i);
                filePath = DMApplication.DEFAULT_DIR + "/Dictations/"
                        + mDictationCard.getSequenceNumber() + "/";
                file = new File(filePath + mDictationCard.getDictFileName()
                        + ".wav");
                /*
                 * To check there is available memory present for all
                 * dictation's file to 'amr' conversion.
                 */
                if ((file.length() / 19.98) > ((long) stat.getAvailableBlocks() * (long) stat
                        .getBlockSize())) {
                    hasNoMemory = true;
                    break;
                }
            }
            if (!hasNoMemory) {
                for (int i = 0; i < dictationCards.size(); i++) {
                    mDictationCard = dictationCards.get(i);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtvAMRFileName
                                    .setText(getString(R.string.Dictate_Alert_Compressing)
                                            + " "
                                            + mDictationCard.getDictationName());
                        }
                    });
                    filePath = DMApplication.DEFAULT_DIR + "/Dictations/"
                            + mDictationCard.getSequenceNumber() + "/";
                    file = new File(filePath + mDictationCard.getDictFileName()
                            + ".wav");
                    if (file.exists() && mDictationCard.getDuration() > 999) {
                        amrConverter = new AMRConverter();// Initialize the
                        // 'amr' converter.
                        /*
                         * To check the conversion is success or not.
                         */
                        int val = amrConverter.convert(
                                filePath + mDictationCard.getDictFileName()
                                        + ".wav",
                                filePath + mDictationCard.getDictationName()
                                        + ".amr");
                        if (val == 0) {


                            //   Log.d("sendVia email","conversion successfull");
                            mDictationCard.setSentDate(dmApplication
                                    .getDeviceTime());
                            mDbHandler.updateSentDate(mDictationCard);
                            if (mDictationCard.getIsThumbnailAvailable() == 1) {
                                try {
                                    // Log.d("sendVia email","thumbnailsavailable");
                                    file = new File(DMApplication.DEFAULT_DIR
                                            + "/Dictations/"
                                            + mDictationCard.getSequenceNumber()
                                            + "/"
                                            + mDictationCard.getDictFileName()
                                            + ".jpg");
                                    if (file.exists()) {
                                        //   Log.d("sendVia email","file exists");

                                        if (!mDictationCard
                                                .getDictFileName()
                                                .equalsIgnoreCase(
                                                        mDictationCard
                                                                .getDictationName())) {
                                            //    Log.d("sendVia email","getDictFileName not equal to getDictationName");
                                            mtempFile = DMApplication.DEFAULT_DIR;
                                            if (mtempFile.canWrite()) {
                                                //       Log.d("sendVia email","temp files can write");

                                                mtempFile = new File(
                                                        DMApplication.DEFAULT_DIR
                                                                + "/Dictations/"
                                                                + mDictationCard
                                                                .getSequenceNumber()
                                                                + "/"
                                                                + mDictationCard
                                                                .getDictationName()
                                                                + ".jpg");
                                                srcChannel = new FileInputStream(
                                                        file).getChannel();
                                                dstChannel = new FileOutputStream(
                                                        mtempFile).getChannel();
                                                dstChannel.transferFrom(
                                                        srcChannel, 0,
                                                        srcChannel.size());
                                                srcChannel.close();
                                                dstChannel.close();
                                                srcChannel = null;
                                                dstChannel = null;
                                            }
                                        }
                                    } else {
                                        //Log.d("sendVia email","file not exists");

                                        mDictationCard
                                                .setIsThumbnailAvailable(0);
                                        mDbHandler
                                                .updateIsThumbnailAvailable(mDictationCard);
                                        dictationCards.set(i, mDictationCard);
                                    }
                                } catch (Exception e) {
                                }
                                file = null;
                                mtempFile = null;
                            }

                        } else {
                            //    Log.d("sendVia email","conversion failed");
                            conversionFailed = true;
                            break;
                        }
                    } else {
                        dictationCards.remove(i);
                        i--;
                        removedCards.add(mDictationCard.getDictationName());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d("sendVia email", "thread in post execution");
            if (dialog.isShowing())
                dialog.dismiss();
            if (!hasNoMemory) {
                if (conversionFailed)
                    onConvertionFailed();
                else if (removedCards.size() > 0)
                    onFileMissing(removedCards);
                else if (dictationCards.size() > 0)
                    shareViaEmail();
            } else {
                /*
                 * Alert when their memory has not available.
                 */
                file = null;
                mAlertDialog = new AlertDialog.Builder(DMActivity.this)
                        .create();
                mAlertDialog.setTitle(getString(R.string.Dictate_No_Space));
                mAlertDialog.setMessage(getString(R.string.Dictate_Low_Memory));
                mAlertDialog.setButton(getString(R.string.Dictate_Alert_Ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
                mAlertDialog.show();
            }
            System.gc();
        }
    }

    /**
     * Shows notification message, when Encryption's for the dictation in server
     * is not defined or zero.
     */
    public void promptSecurityDialog() {

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder alert = new AlertDialog.Builder(DMActivity.this);
        alert.setCancelable(false);
        View layout = inflater.inflate(R.layout.security_alert_dialog,
                (ViewGroup) this.findViewById(R.id.rel_security_dialog));
        final CheckBox checkBox = (CheckBox) layout
                .findViewById(R.id.check_dont_show);

        alert.setPositiveButton(getString(R.string.Dictate_Alert_Ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pref = PreferenceManager
                                .getDefaultSharedPreferences(DMActivity.this);
                        editor = pref.edit();
                        editor.putBoolean(DONT_SHOW_KEY, checkBox.isChecked());
                        editor.commit();
                        dialog.dismiss();
                        if (hasMultipleSplits)
                            onSendToServerEmailHasAbove23MB();
                        hasMultipleSplits = false;
                    }
                });
        alert.setView(layout);
        alert.create().show();
    }

    public void autoResize(Button bt, int length) {

        Spannable span = new SpannableString(bt.getText().toString());
        if (length >= 4 && length < 6) {

            span.setSpan(new RelativeSizeSpan(1.2f), 0, bt.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            bt.setText(span);
        } else if (length >= 6 && length < 8) {

            span.setSpan(new RelativeSizeSpan(1f), 0, bt.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            bt.setText(span);
        } else if (length >= 8) {

            span.setSpan(new RelativeSizeSpan(.8f), 0, bt.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            bt.setText(span);
        }

    }

    public static void autoResize(TextView tt, int length) {

        Spannable span = new SpannableString(tt.getText().toString());
        if (length >= 4 && length < 6) {

            span.setSpan(new RelativeSizeSpan(1.1f), 0, tt.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tt.setText(span);
        } else if (length >= 6 && length < 8) {

            span.setSpan(new RelativeSizeSpan(.9f), 0, tt.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tt.setText(span);
        } else if (length >= 8) {

            span.setSpan(new RelativeSizeSpan(.7f), 0, tt.getText().length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tt.setText(span);
        }
    }

    /**
     * Check dictation already exists or not in Database based on Dictation
     * Name.
     *
     * @param auth  Author of Dictation input value as String.
     * @param seqNo Sequence Number of Dictation input value as int.
     * @param dCard Details of the Dictation input value as DictationCard.
     */
    public void checkDictationExists(int seqNo) {
        dictationName = mAuthor + String.format("%04d", seqNo);
        if (seqNo <= 9999) {
            if (mDbHandler.checkDictNameExists(dictationName))
                checkDictationExists(seqNo + 1);
            else
                seqNumber = seqNo;
            isLimit = false;
        } else {
            isLimit = true;
            mComputeAvailableDictSlot = new GetAvailableDictationSlotTask();
            mComputeAvailableDictSlot.execute();
        }
    }

    /**
     * Invokes when Sent dictation is selected for EditCopy & Re-send. Initialization and
     * Copy of Dictation happens here.
     *
     * @see com.olympus.dmmobile.EditCopySelectedListener#onEditCopySelect(com.olympus.dmmobile.DictationCard)
     */
    @Override
    public void onItemSeleted(int mAction, DictationCard mParentCard) {
        mActionForDuplication = mAction;
        this.mParentCard = mParentCard;
        limitFlag = false;
        Cursor cur = mDbHandler.getNewDictationExisting();
        pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (cur.moveToFirst()) {
            isNewExists = true;
            seqNumber = pref.getInt(DictateActivity.SEQ_NUMBER_KEY, 0000);
            DictateActivity.SavedDicID = -1;
        } else {
            isNewExists = false;
            seqNumber = pref.getInt(DictateActivity.SEQ_NUMBER_KEY, 0000) + 1;
            if (seqNumber > 99999999)
                seqNumber = 0;
        }
        sNumber = seqNumber % 10000;
        mAuthorName = pref.getString(getString(R.string.author_key), "AUTHOR");
        if (mAuthorName.length() == 0)
            mAuthorName = "AUTHOR";
        if (mAuthorName.length() > 4)
            mAuthor = mAuthorName.substring(0, 4);
        else
            mAuthor = mAuthorName;
        dictationName = mAuthor + String.format("%04d", seqNumber);
        ;
        if (seqNumber <= 9999) {
            checkDictationExists(seqNumber);
            if (!isLimit)
                initializeFilesForDuplicateDictation();
        } else {
            mComputeAvailableDictSlot = new GetAvailableDictationSlotTask();
            mComputeAvailableDictSlot.execute();
        }
    }

    /**
     * Initializes file for EditCopy of dictation.
     *
     * @param dCard Dictation card of selected Sent dictation for EditCopy
     */
    private void initializeFilesForDuplicateDictation() {
        File mDictationDir = new File(DMApplication.DEFAULT_DIR.getAbsolutePath() + "/"
                + "Dictations" + "/" + seqNumber);
        if (!mDictationDir.exists())
            mDictationDir.mkdirs();
        mFileSource = DMApplication.DEFAULT_DIR.getAbsolutePath() + "/"
                + "Dictations" + "/" + mParentCard.getSequenceNumber() + "/"
                + mParentCard.getDictFileName() + ".";
        mFileDestination = DMApplication.DEFAULT_DIR.getAbsolutePath() + "/"
                + "Dictations" + "/" + seqNumber + "/";
        if (mParentCard.getIsFlashAir() == 1) {
            mFileSource = mFileSource + DMApplication.getDssType(mParentCard.getDssVersion());
            mFileDestination = mFileDestination + mParentCard.getDictFileName() + "." + DMApplication.getDssType(mParentCard.getDssVersion());
        } else {
            mFileSource = mFileSource + "wav";
            mFileDestination = mFileDestination + dictationName + "." + "wav";
        }
        if (mActionForDuplication == 1) {
            mSelectedSize = 1;
            onSendToServerAction();
        } else
            onStartDuplicateTask();
    }

    /**
     * Method to start duplication process for re-send/edit copy dictation
     */
    private void onStartDuplicateTask() {
        mTaskDuplication = new DuplicateDictationTask();
        mTaskDuplication.execute();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onHideKeyBoard();
        if (dmApplication.newCreated)
            dmApplication.newCreated = false;
        dmApplication.flashair = false;
        dmApplication.lastDictMailSent = false;
    }

    /**
     * Invokes when Conversion fails. Dialog will be shown with conversion
     * failed message.
     */
    private void onConvertionFailed() {
        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle(getString(R.string.Settings_Error));
        mBuilder.setMessage(getString(R.string.Property_Conversion_Failed));
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
     * Show the missing file names, when the selected dictation's corresponding
     * file's are not in SDcard.
     *
     * @param filNames file names
     */
    private void onFileMissing(ArrayList<String> filNames) {
        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setCancelable(false);
        mBuilder.setTitle(getString(R.string.SourceNotfound));
        ListView fileList = new ListView(this);
        fileList.setCacheColorHint(Color.TRANSPARENT);
        fileList.setBackgroundColor(Color.WHITE);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_item, filNames);
        fileList.setAdapter(modeAdapter);
        mBuilder.setPositiveButton(getString(R.string.Dictate_Alert_Ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (dictationCards.size() > 0)
                            shareViaEmail();
                    }
                });
        mBuilder.setView(fileList);
        mBuilder.create().show();
    }

    /**
     * Creates custom chooser for available email clients in device. Invokes
     * when user taps Send via Email.
     */
    private void shareViaEmail() {
        pref = getSharedPreferences(EMAIL_PREF_NAME, MODE_PRIVATE);
        if (pref.getString(EMAIL_PREF_KEY, "").equals("")) {
            baseIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
            baseIntent.setType("message/rfc822");

            List<ResolveInfo> activities = getPackageManager()
                    .queryIntentActivities(baseIntent, 0);
            List<ResolveInfo> activities1 = new ArrayList<ResolveInfo>();
            if (!activities.isEmpty()) {

                for (int i = 0; i < activities.size(); i++) {

                    if (activities.get(i).activityInfo.packageName
                            .toLowerCase().contains("mail")
                            || activities.get(i).activityInfo.packageName
                            .toLowerCase().contains("gm")) {
                        activities1.add(activities.get(i));
                    }
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Email client");
            final ChooserIntentListAdapter adapter = new ChooserIntentListAdapter(
                    this, R.layout.flashair_email_chooser_row,
                    activities1.toArray());
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ResolveInfo info = (ResolveInfo) adapter.getItem(which);
                    String packageName = info.activityInfo.packageName;
                    editor = pref.edit();
                    editor.putString(EMAIL_PREF_KEY, packageName);
                    editor.commit();
                    sendEmail(packageName);
                    info = null;
                    packageName = null;
                }
            });
            activities = null;
            activities1 = null;
            builder.create().show();
        } else
            sendEmail(pref.getString(EMAIL_PREF_KEY, ""));
    }

    /**
     * Email client will be invoked with multiple Dictations as attachment.
     *
     * @param packageName Package name of the selected Email client.
     */
    private void sendEmail(String packageName) {
        baseIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        baseIntent.setType("text/calendar");
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        recipient = null;
        if (pref.getString(getString(R.string.recipient_key), "").contains(","))
            recipient = pref.getString(getString(R.string.recipient_key), "")
                    .split(",");
        else
            recipient = new String[]{pref.getString(
                    getString(R.string.recipient_key), "")};
        baseIntent.putExtra(Intent.EXTRA_EMAIL, recipient);
        baseIntent.putExtra(Intent.EXTRA_SUBJECT,
                pref.getString(getString(R.string.subject_key), "Dictations"));
        baseIntent.putExtra(Intent.EXTRA_TEXT, pref.getString(
                getString(R.string.message_key),
                "Please find the attached files"));
        uris = new ArrayList<Uri>();
        for (int i = 0; i < dictationCards.size(); i++) {
            mDictationCard = dictationCards.get(i);
            file = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
                    + dictationCards.get(i).getSequenceNumber() + "/"
                    + mDictationCard.getDictationName() + ".amr");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uris.add(FileProvider.getUriForFile(DMActivity.this, BuildConfig.APPLICATION_ID + ".provider", file));
            } else {
                uris.add(Uri.fromFile(file));
            }
            if (dictationCards.get(i).getIsThumbnailAvailable() == 1) {
                file = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
                        + dictationCards.get(i).getSequenceNumber() + "/"
                        + mDictationCard.getDictationName() + ".jpg");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uris.add(FileProvider.getUriForFile(DMActivity.this, BuildConfig.APPLICATION_ID + ".provider", file));
                } else {
                    uris.add(Uri.fromFile(file));
                }
            }
            mDictationCard.setIsConverted(1);
            mDbHandler.updateIsConverted(mDictationCard);
            mDbHandler.updateDictationStatus(mDictationCard.getDictationId(),
                    DictationStatus.SENT_VIA_EMAIL.getValue());
        }
        mSelectedList.clear();
        enableSendDeleteButton(0);
        mPendingFragment.getListAdapter().initCheckList();
        dictationCards = null;
        mDictationCard = null;
        baseIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        baseIntent.setPackage(packageName);
        startActivityIfNeeded(baseIntent, EMAIL_SEND_REQ_CODE);
        file = null;
        uris = null;
        baseIntent = null;
        recipient = null;
    }

    /**
     * Enable or Disable FlashAir Button.
     */
    private void flashAirButton() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        flashAirConn = pref.getBoolean(getString(R.string.use_flashair_key),
                false);
        if (flashAirConn) {
            mFlashAir.setEnabled(true);
            mFlashAir.setImageResource(R.drawable.dm_activity_img_btn_flashair_sel);
        } else {
            mFlashAir.setEnabled(false);
            mFlashAir.setImageResource(R.drawable.dm_activity_img_btn_flashair);
        }

    }

    @Override
    protected void onPause() {
        Toast.makeText(DMActivity.this, "onPause", Toast.LENGTH_SHORT).show();

        if (dialog != null)
            dialog.dismiss();

        super.onPause();
    }

    /**
     * DuplicateDictationTask is an AyncTask to take Copy of already sent
     * Dictation which is selected for EditCopy/Re-send operation.
     */
    private class DuplicateDictationTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(DMActivity.this);
            dialog.setCancelable(false);
            if (mActionForDuplication == 1)
                dialog.setMessage(getString(R.string.Recording_Alerts_Copying_Resend));
            else
                dialog.setMessage(getString(R.string.Recording_Alerts_Copying_Editing));
            dialog.show();
            editor = pref.edit();
            editor.putBoolean(DMApplication.EDIT_COPY_FORCE_QUIT, true);
            editor.commit();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            editor = pref.edit();
            editor.putString(DMApplication.EDIT_COPY_DESTINATION, mFileDestination);
            editor.commit();
            int isCopied = editCopy(0, mFileSource, mFileDestination);
            if (mParentCard.getIsThumbnailAvailable() == 1) {
                String sourceImage = mFileSource.replace("wav", "jpg");
                String destImage = mFileDestination.replace("wav", "jpg");
                editCopy(0, sourceImage, destImage);
            }
            return isCopied;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 1) {
                editor = pref.edit();
                editor.putInt(DictateActivity.SEQ_NUMBER_KEY, seqNumber);
                editor.commit();
                hasNoEncript = false;
                if (mActionForDuplication == 1) {
                    dmApplication.flashair = true;
                    mDictationCard = mParentCard;
                    mDictationCard.setDictationId(seqNumber);
                    mDictationCard.setSequenceNumber(seqNumber);
                    mDictationCard.setGroupId(mDbHandler.getGroupId());
                    mDictationCard.setJobNumber(0);
                    mDictationCard.setQueryPriority(0);
                    mDictationCard.setSplitInternalStatus(0);
                    mDictationCard.setSentDate("");
                    mDictationCard.setSentTime("");
                    mDictationCard.setIsResend(1);
                    mDictationCard.setIsActive(0);
                    mDictationCard.setFileSplittable(isSplittable);
                    pref = PreferenceManager.getDefaultSharedPreferences(DMActivity.this);
                    mDictationCard.setAuthor(pref.getString(getString(R.string.author_key), mAuthorName));
                    if (isSplittable == 1)
                        mDictationCard.setDeliveryMethod(1);
                    else
                        mDictationCard.setDeliveryMethod(2);
                    if (mDictationCard.getIsFlashAir() == 1) {
                        mDictationCard.setIsConverted(1);
                        mDictationCard.setMainStatus(200);
                        mDictationCard.setStatus(DictationStatus.SENDING.getValue());
                        filesCard = new FilesCard();
                        filesCard.setFileId((int) seqNumber);
                        filesCard.setFileIndex(0);
                        filesCard.setFileName(mDictationCard.getDictFileName());
                        mFilesCards = new ArrayList<FilesCard>();
                        mFilesCards.add(filesCard);
                        mDbHandler.insertFiles(filesCard);
                        mFilesCards = null;
                    } else {
                        mDictationCard.setDssEncryptionPassword(pref.getString(getString(R.string.Audio_Password_key), mAudioPassword));
                        mDictationCard.setEncryptionVersion(Integer.parseInt(pref.getString(getString(R.string.Audio_Encryption_key), mAudioEncrypt)));
                        if (mDictationCard.getEncryptionVersion() > 0)
                            mDictationCard.setEncryption(1);
                        else
                            hasNoEncript = true;
                        mDictationCard.setDssVersion(Integer.parseInt(pref.getString(getString(R.string.Audio_Format_key), mAudioFormat)));
                        mDictationCard.setDictFileName(dictationName);
                        mDictationCard.setMainStatus(0);
                        mDictationCard.setIsConverted(0);
                        mDictationCard.setStatus(DictationStatus.OUTBOX.getValue());

                        cardSize = 0;
                        filePath = DMApplication.DEFAULT_DIR + "/Dictations/" + mDictationCard.getSequenceNumber() + "/"
                                + mDictationCard.getDictFileName();
                        if (mDictationCard.isFileSplittable() == 1) {
                            if (mDictationCard.getIsThumbnailAvailable() == 1)
                                cardSize = dmApplication.getImageFileSize(filePath);
                            cardSize = cardSize + DMApplication.getExpectedDSSFileSize(mDictationCard.getDssVersion(), filePath);
                            if (cardSize > maxSize)
                                hasMultipleSplits = true;
                        }
                    }
                } else {
                    mDictationCard = new DictationCard();
                    mDictationCard.setDictationId((int) seqNumber);
                    mDictationCard.setDictationName(dictationName);
                    mDictationCard.setDictFileName(dictationName);
                    mDictationCard.setAuthor(mAuthorName);
                    mDictationCard.setPriority(mParentCard.getPriority());
                    mDictationCard.setComments(mParentCard.getComments());
                    mDictationCard.setWorktype(mParentCard.getWorktype());
                    mDictationCard.setCreatedAt(dmApplication.getDeviceTime());
                    mDictationCard
                            .setRecStartDate(dmApplication.getDeviceTime());
                    mDictationCard.setRecEndDate(dmApplication.getDeviceTime());
                    mDictationCard.setSequenceNumber((int) seqNumber);
                    mDictationCard.setIsActive(1);
                    mDictationCard.setStatus(DictationStatus.NEW.getValue());
                    mDictationCard.setIsThumbnailAvailable(mParentCard.getIsThumbnailAvailable());
                }
                if (mActionForDuplication != 1 || isNewExists) {
                    Cursor cur = mDbHandler.checkActiveDictationExistsWithDuration();
                    if (cur != null) {
                        DictationCard card = mDbHandler.getSelectedDicts(cur);
                        if (card.getStatus() == DictationStatus.SENT.getValue() || card.getStatus() == DictationStatus.SENT_VIA_EMAIL.getValue())
                            mDbHandler.updateAllIsActive();
                        else {
                            card.setIsActive(0);
                            card.setStatus(DictationStatus.PENDING.getValue());
                            mDbHandler.updateStatusAndActive(card);
                        }
                        cur.close();
                    }
                }
                if (isNewExists)
                    mDbHandler.updateDictation(mDictationCard);
                else
                    mDbHandler.insertDictation(mDictationCard);
                if (mActionForDuplication == 1) {
                    baseIntent = new Intent("com.olympus.dmmobile.action.Test");
                    baseIntent.putExtra("isWantToUpdate", true);
                    sendBroadcast(baseIntent);
                    pref = PreferenceManager.getDefaultSharedPreferences(DMActivity.this);
                    if (hasNoEncript && !pref.getBoolean(DONT_SHOW_KEY, false))
                        promptSecurityDialog();
                    else if (hasMultipleSplits)
                        onSendToServerEmailHasAbove23MB();
                } else {
                    dmApplication.flashair = false;
                    onHideKeyBoard();
                    baseIntent = new Intent(DMActivity.this, DictateActivity.class);
                    baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    baseIntent.putExtra(DMApplication.START_MODE_TAG,
                            DMApplication.MODE_COPY_RECORDING);
                    baseIntent.putExtra(DMApplication.DICTATION_ID,
                            mDictationCard.getDictationId());
                    startActivity(baseIntent);
                }
                System.gc();
            }
            cardSize = 0;
            mActionForDuplication = -1;
            mParentCard = null;
            dialog.dismiss();
            editor = pref.edit();
            editor.putBoolean(DMApplication.EDIT_COPY_FORCE_QUIT, false);
            editor.putString(DMApplication.EDIT_COPY_DESTINATION, "");
            editor.commit();
        }

    }

    /**
     * @see com.olympus.dmmobile.RecordingSelectedListener#updateButtonState(boolean)
     */
    @Override
    public void updateButtonState(boolean enable) {
        if (dmApplication.getCurrentGroupId() == 0) {
            mBtnSendDeleteAll.setEnabled(enable);
            mBtnSendDeleteAll.invalidate();
        }
        /*
         * if(mTabPosition==1&&isSendAll&&mAlertDialog!=null&&mAlertDialog.isShowing
         * ()&&!enable) mAlertDialog.dismiss();
         */
    }

    /**
     * Sets Current Language to Locale based on the input value passed.
     *
     * @param value Language code as int
     */
    private void setCurrentLanguage(int value) {
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
     * Function used to set the language for the current activity
     *
     * @param lang
     *            is the locale get from sharedpreference
     *
     *
     */
    /**
     * Updates Locale with passed Language code.
     *
     * @param lang Language code as String.
     */
    private void setLocale(String lang) {
        locale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
        res = null;
        dm = null;
        conf = null;
        lang = null;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        // Avoid Memory leaks
        // convertAndUploadService.showNotification(this);

        unbindDrawables(findViewById(R.id.dmactivity_root_view));
        System.gc();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    public String getlanguage() {
        String currentLanguage = null;
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(this);
        currentLanguage = (pref.getString(getString(R.string.language_key), ""));
        if (Integer.parseInt(currentLanguage) == 1) {
            language = "en";
        } else if (Integer.parseInt(currentLanguage) == 1) {
            language = "de";
        } else if (Integer.parseInt(currentLanguage) == 2) {
            language = "fr";
        } else if (Integer.parseInt(currentLanguage) == 3) {
            language = "es";
        } else if (Integer.parseInt(currentLanguage) == 4) {
            language = "sv";
        } else if (Integer.parseInt(currentLanguage) == 5) {
            language = "cs";
        }
        return language;
    }

    /**
     * This AsyncTask is used to find available Dictation slot if the Dictation
     * sequence number reaches 9999
     */
    private class GetAvailableDictationSlotTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(DMActivity.this);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<String> dNames = mDbHandler.getDictationNames();
            for (int i = 0; i <= 9999; i++) {
                int sNum = seqNumber % 10000;
                dictationName = mAuthor + String.format("%04d", sNum);
                boolean isContain = dNames
                        .contains(dictationName.toLowerCase());
                if (isContain)
                    seqNumber++;
                else {
                    break;
                }
                if (i == 9999) {
                    dialog.dismiss();
                    limitFlag = true;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!limitFlag) {
                initializeFilesForDuplicateDictation();
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        DMActivity.this);
                alert.setTitle("Alert");
                alert.setMessage("Cannot create dictation name, please delete some dictations and try again");
                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        });
                alert.create().show();
            }
            dialog.dismiss();
        }

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            mEditSearch.clearFocus();
            InputMethodManager imm = (InputMethodManager) this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            return true;
        }

        return false;
    }

    /**
     * Refresh the Dictation List of current visible Tab.
     */
    private void onRefreashList() {
        switch (mTabPosition) {
            case 0:
                if (isOnceExecuted) {
                    if (dmApplication.isPriorityOn())
                        mPendingFragment.onRefreshList(mDbHandler
                                .getSearchFilteredAndPrioritisedDicts(dmApplication
                                        .editStringHasEscape(mEditSearch.getText()
                                                .toString())), mDbHandler
                                .getDictationsInPendingAll());
                    else
                        mPendingFragment.onRefreshList(mDbHandler
                                .getSearchFilteredDictations(dmApplication
                                        .editStringHasEscape(mEditSearch.getText()
                                                .toString())), mDbHandler
                                .getDictationsInPendingAll());
                }
                break;
            case 1:
                if (dmApplication.isPriorityOn()) {
                    if (dmApplication.isOnEditState())
                        mOutboxFragment
                                .onRefreshList(
                                        mDbHandler
                                                .getSearchFilteredAndPrioritisedOutboxDictations(dmApplication
                                                        .editStringHasEscape(mEditSearch
                                                                .getText()
                                                                .toString())),
                                        mDbHandler
                                                .getOutboxDictationsToEnableSendAll());
                    else
                        mOutboxFragment
                                .onRefreshList(
                                        mDbHandler
                                                .getSearchFilteredAndPrioritisedOutboxDictations(dmApplication
                                                        .editStringHasEscape(mEditSearch
                                                                .getText()
                                                                .toString())),
                                        mDbHandler
                                                .getOutboxDictationsToEnableDeleteAll());
                } else {
                    if (dmApplication.isOnEditState())
                        mOutboxFragment.onRefreshList(mDbHandler
                                .getSearchFilteredOutboxDictations(dmApplication
                                        .editStringHasEscape(mEditSearch.getText()
                                                .toString())), mDbHandler
                                .getOutboxDictationsToEnableSendAll());
                    else
                        mOutboxFragment.onRefreshList(mDbHandler
                                .getSearchFilteredOutboxDictations(dmApplication
                                        .editStringHasEscape(mEditSearch.getText()
                                                .toString())), mDbHandler
                                .getOutboxDictationsToEnableDeleteAll());
                }
                break;
            case 2:
                if (dmApplication.isPriorityOn())
                    mSendFragment
                            .onRefreshList(
                                    mDbHandler
                                            .getSearchFilteredAndPrioritisedSentDictations(dmApplication
                                                    .editStringHasEscape(mEditSearch
                                                            .getText().toString())),
                                    mDbHandler.getSentDictations());
                else
                    mSendFragment.onRefreshList(mDbHandler
                            .getSearchFilteredSentDictations(dmApplication
                                    .editStringHasEscape(mEditSearch.getText()
                                            .toString())), mDbHandler
                            .getSentDictations());
                break;
            default:
                break;
        }
    }

    public void onUpdateFromSplash() {
        try {
            onRefreashList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkLocationPermission() {
        boolean check = true;
        int permissiontakeCamera = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int[] perm = {permissiontakeCamera};
        String[] stringPerm = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        for (String permis : stringPerm) {
            if (!(ActivityCompat.checkSelfPermission(this, permis) == PackageManager.PERMISSION_GRANTED)) {

                check = false;
            }
        }
        ActivityCompat.requestPermissions(this, stringPerm, 1);
        return check;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String permissionTxt = "";
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
                        if (permission.equalsIgnoreCase("android.permission.LOCATION")) {
                            if (permissionTxt.equalsIgnoreCase("")) {
                                permissionTxt += "Location";
                            } else {
                                permissionTxt += ",Location";
                            }
                        }
                        if (permission.equalsIgnoreCase("android.permission.WRITE_EXTERNAL_STORAGE")) {
                            if (permissionTxt.equalsIgnoreCase("")) {
                                permissionTxt += "Storage";
                            } else {
                                permissionTxt += ",Storage";
                            }
                        }
                        //set to never ask again
                        Log.e("set to never ask again", permission);
                        somePermissionsForeverDenied = true;
                    }
                }
            }
            if (somePermissionsForeverDenied) {
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Permissions Required")
                        .setMessage(getResources().getString(R.string.permissionAccess) + "\n\n" + getResources().getString(R.string.permission) + "(" + permissionTxt + ")")
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
            pref = getSharedPreferences("Locermissions", MODE_PRIVATE);
            SharedPreferences.Editor editor = getSharedPreferences("Locermissions", MODE_PRIVATE).edit();
            editor.putBoolean("Locperm", true);
            editor.commit();
            isLocationPermission = true;
        }
    }

    private Boolean isNetworkAvailable() {
        boolean isonline = false;
        final ConnectivityManager connection_manager =
                (ConnectivityManager) this.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//        Log.d("networkindConnect", activeNetworkInfo.getTypeName().toString());
        if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) {
            isonline = isOnline();
            if (isonline && activeNetworkInfo.getTypeName().toString().equalsIgnoreCase("MOBILE")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    NetworkRequest.Builder request = new NetworkRequest.Builder();
                    request.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

                    connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                        @Override
                        public void onAvailable(Network network) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                connection_manager.bindProcessToNetwork(network);

                            } else {
                                ConnectivityManager.setProcessDefaultNetwork(network);
                            }
                        }
                    });
                }
            }
            if (isonline && activeNetworkInfo.getTypeName().toString().equalsIgnoreCase("WIFI")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    NetworkRequest.Builder request = new NetworkRequest.Builder();
                    request.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

                    connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                        @Override
                        public void onAvailable(Network network) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                connection_manager.bindProcessToNetwork(network);

                            } else {
                                ConnectivityManager.setProcessDefaultNetwork(network);
                            }
                        }
                    });
                }
            }
        }
        return isonline;
    }

    public Boolean isOnline() {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal == 0);
            Log.d("networkindConnect", String.valueOf(reachable));

            return reachable;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Toast.makeText(DMActivity.this, "onStop", Toast.LENGTH_SHORT).show();

    }
}
