package com.olympus.dmmobile.recorder;

import com.olympus.dmmobile.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
/**
 * @version 1.0.1
 */
public class Signalometer extends SeekBar {
	/**
	 * Default constructor of class.
	 */
	public Signalometer(Context context) {
		super(context);
		setUpMeterUI();
	}
	/**
	 * Default constructor of class.
	 */
	public Signalometer(Context context, AttributeSet attrs) {
		super(context, attrs);
		setUpMeterUI();
	}
	/**
	 * Default constructor of class.
	 */
	public Signalometer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setUpMeterUI();
	}
	/**
	 * Setting up the Insert Signalometer UI using the custom drawable
	 * dictate_signalometer_bg.xml .
	 */
	void setUpMeterUI() {

		this.setProgressDrawable(getResources().getDrawable(
				R.drawable.dictate_signalometer_bg));
	}
}
