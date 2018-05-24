package com.olympus.dmmobile.network;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import com.olympus.dmmobile.DMApplication;

/**
 * This class always listen to the network state of the device and update the application with the current state of network.
 * 
 * @version 1.0.1
 */
public class NetworkConnectivityListener {
	
	private final String CONNECTIVITY_ACTION_LOLLIPOP = "com.olympus.dmmobile.network.CONNECTIVITY_ACTION_LOLLIPOP";
	private Context mContext = null;
	private boolean mListening = false;
	private boolean isNewVersion = false;
	private State mState = null;
	private NetworkBroadcastReceiver mReceiver = null;
    private RetryUploadListener retryUploadListener=null;
    private DMApplication mDMApplication=null;
    
    /**
     * This class is a subclass of BroadcastReceiver, which receive a notification whenever the connection state changes.
     * The current network state received is updated to application via DMApplication
     * 
     */
    public class NetworkBroadcastReceiver extends BroadcastReceiver {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		mDMApplication = (DMApplication)context.getApplicationContext();
    		if(!isNewVersion) {
	    		String action = intent.getAction();
	    		if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION) || mListening == false) {
		            return;
	    		}
	    		boolean noConnectivity =intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
	    		if (noConnectivity||mDMApplication.isFlashAirState()) {
	    			mState = State.NOT_CONNECTED;
	    			DMApplication.setONLINE(false);
	    		} 
	    		else {
	    			mState = State.CONNECTED;
	    			DMApplication.setONLINE(true);
	    			onRetryUpload();
	    		}
    		}
    	}	
	}
    /**
     * various states of Network connection.
     */
	public enum State {
        UNKNOWN,
        CONNECTED,
        NOT_CONNECTED
    }
	
	/**
	 * Initialize the BroadcastReceiver and set default state to the Network Connection State.
	 */
	public NetworkConnectivityListener() {
		mState=State.UNKNOWN;
		mReceiver=new NetworkBroadcastReceiver();
	}
	
	/**
	 * Listen to the network state by registering to a Receiver with the action 'CONNECTIVITY_ACTION'
	 * 
	 * @param context base application context
	 */
	public synchronized void startListening(Context context)  {
        if (!mListening) {
            mContext = context;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(CONNECTIVITY_ACTION_LOLLIPOP);
            context.registerReceiver(mReceiver, filter);
            mListening = true;
            registerConnectivityActionLollipop();
        }
    }
	
	/**
	 * Stop/Close the BroadcastReceiver.
	 */
	public synchronized void stopListening() {
        if (mListening) {
            mContext.unregisterReceiver(mReceiver);
            mContext = null;
            mListening = false;
        }
    }
	
	/**
	 * Listener to notify the network state to background service
	 */
	public interface RetryUploadListener{
		/**
		 * Invokes when status of the Network Connection changed.
		 */
		public void onRetryUploadListener();
	}
	/**
	 * To provide a callback to the background service when the network state changes
	 */
	private void onRetryUpload() {
		if(mDMApplication.isDictateUploading()) {
			retryUploadListener = (RetryUploadListener)mDMApplication.getUploadServiceContext();
			retryUploadListener.onRetryUploadListener();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void registerConnectivityActionLollipop() {
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
	        return;
	    isNewVersion = true;
	    mDMApplication=(DMApplication)mContext.getApplicationContext();
	    ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkRequest.Builder builder = new NetworkRequest.Builder();
	    connectivityManager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback() {
	        @Override
	        public void onAvailable(Network network) {
	        	DMApplication.setONLINE(true);
	        	onRetryUpload();
	        }
	        @Override
	        public void onLost(Network network) {
	        	DMApplication.setONLINE(false);
	        }
	    });
	}
}


