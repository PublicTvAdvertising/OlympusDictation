package com.olympus.dmmobile.flashair;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import com.olympus.dmmobile.DMActivity;
import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;

/**
 * FlashAirMain is used to establish connection with FlashAir.
 *
 * @version 1.0.1
 */
public class FlashAirMain extends Activity {

    private ExceptionReporter mReporter;            // Error Logger

    private boolean mIsStateChanged = false;     // True for stateChanged otherwise false
    private boolean mIsListening = false;        // True if listening for BroadcastReceiver otherwise False
    private boolean mStartFlag = false;
    private boolean isScanOver = false;
    private boolean wifiSettingsInvoked = false;
    private boolean isOnceStarted = false;
    private int mNetworkId = -1;
    private String mSelectedSsid;    // Selected SSID
    private ImageButton mBtnUpdate = null;
    private ListView mScanResultView = null;
    private Button mBtnDone = null;
    private WifiScanReciever mReciever = null;    //Receives Wifi connected/disconnect, state changed actions
    private WifiManager mWifiManager = null;     //To manage and handle Wifi connectivity
    private List<ScanResult> mScanResults = null; // holds wifi scanned results
    private State mState = null;
    private ProgressDialog mProgressDialog = null;
    private ProgressDialog mScanDialog = null;
    private ConnectionTask mConnectionTask = null;
    private ScanTask mScanTask = null;
    private DMApplication dmApplication = null;
    private CountDownTimer timer = null;
    private ArrayList<String> flashAirNames = null;
    private Locale mLocale = null;
    private ConnectivityManager mConnectivityManager = null;
    private WifiInfo mWifiInfo = null;
    private SharedPreferences pref = null;
    SharedPreferences.Editor editor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mReporter = ExceptionReporter.register(this);
        super.onCreate(savedInstanceState);
        pref = getSharedPreferences("ssid", MODE_PRIVATE);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_flash_air_main);
        final ConnectivityManager connection_manager =
                (ConnectivityManager) this.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkRequest.Builder request = new NetworkRequest.Builder();
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

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
        dmApplication = (DMApplication) getApplication();
        setCurrentLanguage(dmApplication.getCurrentLanguage());
        mBtnUpdate = (ImageButton) findViewById(R.id.btn_update_wifi_list);
        mScanResultView = (ListView) findViewById(R.id.list_scan_res);
        mBtnDone = (Button) findViewById(R.id.btn_done_flashair_connect);
        if (savedInstanceState == null)
            dmApplication.setPreviousNetworkSSID(getCurrentSsid());
        else {
            dmApplication.setPreviousNetworkSSID(savedInstanceState.getString("PreviousNetworkSSID"));
            dmApplication.setNetWorkId(savedInstanceState.getInt("NetworkID", -1));
        }
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mReciever = new WifiScanReciever();
        registerReceiver(mReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));     //Registering BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION
        mReciever.startListening(getApplicationContext());       //Receiver start listening for Broadcast
        mScanTask = new ScanTask(this);
        mScanTask.execute();
        mBtnUpdate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mScanTask = new ScanTask(FlashAirMain.this);
                mScanTask.execute();
            }
        });
        mBtnDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedSsid != null) {
                    if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                        mStartFlag = true;
                        isConfigured(mSelectedSsid);
                    } else
                        showNoWifiConnectionDialog();
                }
            }
        });
        mScanResultView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adView, View v, int pos,
                                    long arg3) {
                mSelectedSsid = adView.getItemAtPosition(pos).toString();
                dmApplication.setFlashAirSSID(mSelectedSsid);
                mBtnDone.setEnabled(true);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("PreviousNetworkSSID", dmApplication.getPreviousNetworkSSID());
        outState.putInt("NetworkID", dmApplication.getNetWorkId());
    }

    /**
     * Wifi scanning will be started if wifi state is enabled.
     */
    public void startScan() {
        isScanOver = false;
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)
            mWifiManager.startScan();     //Starts Scanning
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showNoWifiConnectionDialog();
                    if (!mScanTask.isCancelled()) {
                        mScanTask.cancel(true);
                        mScanDialog.dismiss();
                    }
                }
            });
        }
    }

    /**
     * To get currently connected Wifi Network's SSID
     */
    private String getCurrentSsid() {
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            mWifiInfo = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
            if (mWifiInfo != null && !mWifiInfo.getSSID().equals(""))
                return mWifiInfo.getSSID();
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (wifiSettingsInvoked) {
            wifiSettingsInvoked = false;
            mStartFlag = true;
        }
        setCurrentLanguage(dmApplication.getCurrentLanguage());
        String ssid = getCurrentSsid();
        if (isOnceStarted && ssid != null && flashAirNames != null) {
            if (mScanResultView.getCheckedItemPosition() < 0) {
                mScanResultView.setItemChecked(flashAirNames.indexOf(ssid.trim()), true);
                mSelectedSsid = ssid;
            }
            isOnceStarted = false;
        }
        registerReceiver(mReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        onEnableOrDisableDone();
    }

    @Override
    protected void onDestroy() {
        try {


            unregisterReceiver(mReciever);
//			if(mWifiManager!=null)
//			{
//				mWifiManager.setWifiEnabled(false);
//
//			}
        } catch (IllegalArgumentException e) {
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
//        if (DMApplication.COMINGFROM.equalsIgnoreCase("flash_air")) {
//            disableFlashAirWifi(getCurrentbsid(FlashAirMain.this));
//        }
        super.onPause();
    }

    /**
     * Shows dialog when no wifi connection available.
     */
    public void showNoWifiConnectionDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(FlashAirMain.this);
        alert.setTitle(getResources().getString(R.string.Flashair_Alert_WiFi_Connection));
        alert.setMessage(getResources().getString(R.string.Flashair_Alert_WiFi_Accessible));
        alert.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.create().show();
    }


    /**
     * Checks whether the particular wifi network is configured or not.
     * If configured connection will be established.
     *
     * @param name
     */
    public void isConfigured(String name) {
        boolean configured = false;
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        if (networks.size() > 0) {
            for (WifiConfiguration item : networks) {
				/* Checks whether the particular flashair SSID is present in the
				 configured Wifi networks  */
                if (item.SSID.toString().replace("\"", "").equals(name.trim())) {
                    configured = true;
                    mNetworkId = item.networkId;
                    break;
                }
            }
            if (configured) {
                // If flashair configured, registering Reciever for Wifi state
                // change action and Calling connectionTask
                registerReceiver(mReciever, new IntentFilter(
                        WifiManager.NETWORK_STATE_CHANGED_ACTION));

                //Registers Receiver for NETWORK_STATE_CHANGED_ACTION
                mConnectionTask = new ConnectionTask(FlashAirMain.this);
                mConnectionTask.execute(name);
                timer = new CountDownTimer(50000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {

                        if (!mConnectionTask.isCancelled())
                            mConnectionTask.cancel(true);
                        mProgressDialog.dismiss();
                        mStartFlag = false;
                        AlertDialog.Builder alert = new AlertDialog.Builder(
                                FlashAirMain.this);
                        alert.setTitle(getResources().getString(
                                R.string.Flashair_Alert_Connection_Error));
                        alert.setMessage(getResources()
                                .getString(
                                        R.string.Flashair_Alert_Cannot_Establish_Connection));
                        alert.setNegativeButton(
                                getResources().getString(
                                        R.string.Dictate_Alert_Ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        dialog.dismiss();
                                        mReciever.mStateFlag = false;
                                        mWifiManager.disconnect();
                                        timer = null;
                                    }
                                });
                        alert.create().show();
                        onEnableOrDisableDone();

                    }
                };
                timer.start();

            } else {
                new AlertDialog.Builder(FlashAirMain.this)
                        .setTitle(getResources().getString(R.string.Connect_Flashair))
                        .setPositiveButton(getResources().getString(R.string.Dictate_Alert_Yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        wifiSettingsInvoked = true;
                                        mStartFlag = false;
                                        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)); // Opens Wifi Settings
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.Dictate_Alert_No),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                    }
                                }).create().show();
            }

        } else {

            new AlertDialog.Builder(FlashAirMain.this)

                    .setTitle(getResources().getString(R.string.Connect_Flashair))
                    .setPositiveButton(getResources().getString(R.string.Dictate_Alert_Yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    wifiSettingsInvoked = true;
                                    mStartFlag = false;
                                    startActivity(new Intent(
                                            android.provider.Settings.ACTION_WIFI_SETTINGS));    // Opens Wifi Settings
                                }
                            })
                    .setNegativeButton(getResources().getString(R.string.Dictate_Alert_No),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {

                                }
                            }).create().show();

        }
    }

    private void onEnableOrDisableDone() {
        if (mScanResultView.getCheckedItemPosition() < 0)
            mBtnDone.setEnabled(false);
        else
            mBtnDone.setEnabled(true);
    }

    /**
     * Reading wep configuration of wifi and connects to that particular Wifi.
     */
    public void readWepConfig(String ssid) {
        WifiConfiguration config;
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        if (networks.size() > 0) {
            for (WifiConfiguration item : networks) {
                if (item.SSID.toString().replace("\"", "").equals(ssid.trim())) {
                    config = item;
                    mWifiManager.disconnect();
                    int res = config.networkId;
                    mWifiManager.enableNetwork(res, true);
                    mWifiManager.reconnect();

                }
            }
        }
    }

    /**
     * Starts the FlashAir browser.
     */
    public void startBrowser() {

        new AsyncTask<Void, Void, Void>() {

            long timeToHold;

            @Override
            protected void onPreExecute() {
                // TODO Auto-generated method stub
                super.onPreExecute();
                timeToHold = System.currentTimeMillis() + 1000;
            }

            @Override
            protected Void doInBackground(
                    Void... params) {
                // TODO Auto-generated method stub

                while (System.currentTimeMillis() < timeToHold) {

                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                // TODO Auto-generated method stub
                super.onPostExecute(result);

                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }

                isOnceStarted = true;
                dmApplication.setNetWorkId(mNetworkId);
                Intent intent = new Intent(FlashAirMain.this, FlashAirBrowser.class);
                intent.putExtra("ssid", mSelectedSsid);
                startActivity(intent);
            }

        }.execute();


    }

    /**
     * Enumerator state
     */
    public enum State {
        UNKNOWN,

        CONNECTED,

        NOT_CONNECTED
    }

    /**
     * Broadcastreciever to receive WIFI connected/disconnected and WIFI state changed
     * actions
     */
    public class WifiScanReciever extends BroadcastReceiver {

        private boolean mStateFlag = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {   //if receives SCAN_RESULTS_AVAILABLE_ACTION
                if (!isScanOver) {
                    mSelectedSsid = null;
                    onEnableOrDisableDone();
                    flashAirNames = new ArrayList<String>();
                    if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {


                        mScanResults = mWifiManager.getScanResults();
                        for (ScanResult result : mScanResults) {
                            if (result.BSSID.toString().startsWith("e8:e0:b7")
                                    || result.BSSID.toString().startsWith("00:0b:5d")
                                    || result.BSSID.toString().startsWith("b8:6b:23")
                                    || result.BSSID.toString().startsWith("00:e0:00")
                                    || result.BSSID.toString().startsWith("ec:21:e5")) {   //Checks whether scanResults contain flashair
                                //Log.e("ssid matches", result.SSID.toString());
                                flashAirNames.add(result.SSID);
                            } else {
                                Log.e("flashair", "ssid:" + result.SSID.toString() + " BSSID:" + result.BSSID.toString());
                            }
                        }
                    }
                    if (flashAirNames != null) {
                        if (flashAirNames.size() < 1) {
                            isScanOver = true;
                            mScanResultView.setAdapter(new ArrayAdapter<String>(FlashAirMain.this,
                                    android.R.layout.simple_list_item_checked, flashAirNames));
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    FlashAirMain.this);
                            alert.setTitle(getResources().getString(
                                    R.string.Flashair_Alert_WiFi_Connection));
                            alert.setMessage(getResources()
                                    .getString(
                                            R.string.Flashair_Alert_Connection_Notfound));
                            alert.setNegativeButton(
                                    getResources()
                                            .getString(R.string.Dictate_Alert_Ok),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alert.create().show();
                        } else {
                            mScanResultView.setAdapter(new ArrayAdapter<String>(FlashAirMain.this,
                                    android.R.layout.simple_list_item_checked, flashAirNames));  //Shows only Flashair list
                            isScanOver = true;
                        }
                    }
                    onEnableOrDisableDone();
                    mScanDialog.dismiss();
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {    //if receives CONNECTIVITY_ACTION
                // checking whether wifi is connected or not
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (noConnectivity)
                    mState = State.NOT_CONNECTED;
                else
                    mState = State.CONNECTED;
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {   //if receives NETWORK_STATE_CHANGED_ACTION
                NetworkInfo netInfo = intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (NetworkInfo.State.CONNECTING.equals(netInfo.getState())) {
                    if (mStartFlag) {
                        mStartFlag = false;
                        mStateFlag = true;
                    }
                }
                if (netInfo.isConnected()) {
                    if (mStateFlag) {
                        mIsStateChanged = checkConnectedToDesiredWifi(
                                FlashAirMain.this, mSelectedSsid);
                        if (mIsStateChanged) {
                            mStateFlag = false;
                            dmApplication.setFlashAirState(true);
                            //mProgressDialog.dismiss();
                            if (timer != null) {
                                timer.cancel();
                                timer = null;
                            }

                            startBrowser();

                            //
                        }
                    }
                }
            }
        }

        /**
         * Starts listening for BroadcastReceiver .
         */
        public synchronized void startListening(Context context) {
            if (!mIsListening) {
                registerReceiver(mReciever, new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));
                mIsListening = true;
            }
        }

        /**
         * Stops listening for BroadcastReceiver.
         */
        public synchronized void stopListening() {
            if (mIsListening) {
                unregisterReceiver(mReciever);
                mIsListening = false;
            }
        }

        /**
         * Wifi state will be returned.
         */
        public State getState() {
            return mState;
        }

    }


    /**
     * Checks whether the device is connected to the passed Ssid or not.
     *
     * @param context Context of the current activity.
     * @param ssid    Ssid of wifi network.
     * @return True if connected to ssid, False if not connected to ssid
     */
    public boolean checkConnectedToDesiredWifi(Context context, String ssid) {
        boolean connected = false;
        String desiredMacAddress = ssid;
        //System.out.println("desiredMacAddress  "+desiredMacAddress);
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifi = wifiManager.getConnectionInfo();
        if (wifi != null && !desiredMacAddress.equalsIgnoreCase("")) {
            String bssid = wifi.getSSID();
            connected = desiredMacAddress.equalsIgnoreCase(bssid.replaceAll("\"", ""));
        }
        return connected;
    }

    /**
     * Asynctask used Establish connection with FlashAir card
     */
    class ConnectionTask extends AsyncTask<String, Void, Void> {

        FlashAirMain activity;

        public ConnectionTask(FlashAirMain activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(getResources().getString(R.string.Connecting_to) + " " + mSelectedSsid + "...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(String... arg) {
            readWepConfig(arg[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
//			if(mProgressDialog!=null)
//				mProgressDialog.dismiss();

        }
    }

    /**
     * Asynctask used to scan for wifi networks.
     */
    class ScanTask extends AsyncTask<String, Void, Void> {

        FlashAirMain activity;


        public ScanTask(FlashAirMain activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            mScanDialog = new ProgressDialog(activity);
            mScanDialog.setMessage(getResources().getString(R.string.Flashair_Label_Scan_Flashair));
            mScanDialog.setCancelable(false);
            mScanDialog.show();
        }

        @Override
        protected Void doInBackground(String... arg) {
            startScan();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //dialog.dismiss();
        }
    }

    /**
     * Sets current Language.
     *
     * @param value Language value passed as int.
     */
    public void setCurrentLanguage(int value) {
        int Val = 1;
        Val = value;
        if (Val == 1)
            setLocale("en");
        else if (Val == 2)
            setLocale("de");
        else if (Val == 3)
            setLocale("fr");
        else if (Val == 4)
            setLocale("es");
        else if (Val == 5)
            setLocale("sv");
        else if (Val == 6)
            setLocale("cs");
    }

    /**
     * Set passed language to Locale.
     *
     * @param lang Language value as String.
     */
    public void setLocale(String lang) {
        mLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = mLocale;
        res.updateConfiguration(conf, dm);
    }

    public void disableFlashAirWifi(String bssid) {

            mSelectedSsid = null;
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            flashAirNames = new ArrayList<String>();
            if ((ActivityCompat.checkSelfPermission(FlashAirMain.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {


                mScanResults = mWifiManager.getScanResults();

                if (bssid.startsWith("e8:e0:b7")
                        || bssid.startsWith("00:0b:5d")
                        || bssid.startsWith("b8:6b:23")
                        || bssid.startsWith("00:e0:00")
                        || bssid.startsWith("ec:21:e5")) {
                    mWifiManager.setWifiEnabled(false);

                }




        }
    }

    public static String getCurrentbsid(Context context) {
        String bssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null) {
                bssid = connectionInfo.getBSSID();
            }
        }
        return bssid;
    }
}
