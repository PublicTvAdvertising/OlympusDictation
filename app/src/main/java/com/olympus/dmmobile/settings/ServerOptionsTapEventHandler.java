package com.olympus.dmmobile.settings;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

/**
 * Class to handle tap events on server options in the settings
 * @version 1.2.0
 */
public class ServerOptionsTapEventHandler {
    private final long COUNTDOWN_DELAY = 800;		
    private final long COUNTDOWN_INTERVAL = 800;
    private final int QUADRUPLE_TAP = 4;
    private final int SINGLE_TAP = 1;
    private CountDownTimer timer;
    private ServerOptionsTapListener tapListener;
    private int tapCounter = 0;
    private boolean listenTapEvents = true;
    
    /**
     * constructor method which initializes tap listener
     * @param context context of the activity which listen for tap events
     */
    ServerOptionsTapEventHandler(Context context){
        tapListener = (ServerOptionsTapListener)context;
        initializeCountDownTimer();
    }
    
    /**
     * method to initialize the CountDownTimer
     */
    private void initializeCountDownTimer() {
        timer = new CountDownTimer(COUNTDOWN_DELAY,COUNTDOWN_INTERVAL){

            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                if(tapCounter == SINGLE_TAP){
                	// if number of taps captured is 1, notify the activity to show server options as user mode
                    tapListener.onServerOptionsClicked(false);
                }else if (tapCounter == QUADRUPLE_TAP){
                	// if number of taps captured is 4, notify the activity to show server options as developer mode
                    tapListener.onServerOptionsClicked(true);
                }
                cancel();
                resetTapCounter();
                listenTapEvents = true;		//listen for next tap events
            }
        };
    }

    /**
     * method used to start listening for number of taps
     */
    public void listenTapEvents(){
        if(tapCounter == 0 && listenTapEvents){
        	listenTapEvents = true;
            timer.start();
        }
    }
    /*public void cancelTapListener(){
        if(timer != null){
            timer.cancel();
        }
        resetTapCounter();
    }*/
    
    /**
     * callback method to update each tap event
     */
    public void onTapEvent(){
    	if(listenTapEvents){
    		tapCounter++;
        }
    	// when quadruple taps occur, finish the CountDownTimer
        if(tapCounter == QUADRUPLE_TAP){
        	listenTapEvents = false;		// don't listen for tap events until the current task is finished.
        	timer.onFinish();
        }
    }
    
    /**
     * method to reset the counter
     */
    public void resetTapCounter(){
        tapCounter = 0;
    }
    /**
     * method to check whether the tap event listener is running or not
     * @return returns true if tap events are listening
     */
    public boolean isTapListening(){
    	return listenTapEvents;
    }

}
