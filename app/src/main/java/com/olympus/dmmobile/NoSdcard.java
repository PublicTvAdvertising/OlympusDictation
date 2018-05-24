package com.olympus.dmmobile;

import android.app.Activity;
import android.os.Bundle;

/**
 * 
 * This class is used to show an alert message, if the SD Card is not mounted when the application is launched.
 *
 */
public class NoSdcard extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nosdcard);
	}
}
