package com.olympus.dmmobile.recorder;

import com.olympus.dmmobile.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
/**
 * 
 * @version 1.0.1
 *
 */
public class OverwriteSignalometer extends SeekBar {
	/**
	 * Default constructor of class.
	 */
	public OverwriteSignalometer(Context context) {
		super(context);
		setupMeterUI();
	}

	/**
	 * Default constructor of class.
	 */
	public OverwriteSignalometer(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupMeterUI();
	}

	/**
	 * Default constructor of class.
	 */
	public OverwriteSignalometer(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setupMeterUI();
	}

	/**
	 * Setting up the Overwrite Signalometer UI using the custom drawable
	 * dictate_overwrite_signalometer_bg.xml.
	 */
	void setupMeterUI() {
		this.setProgressDrawable(getResources().getDrawable(
				R.drawable.dictate_overwrite_signalometer_bg));
	}
}
