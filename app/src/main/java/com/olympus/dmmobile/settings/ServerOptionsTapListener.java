package com.olympus.dmmobile.settings;

/**
 * Interface that define callback methods for quadruple tap event
 * @version 1.2.0
 */
public interface ServerOptionsTapListener {
	/**
	 * callback method to update the activity when a quadruple tap event is occurred 
	 * @param enableDeveloperOptions the state to show server options in user/developer mode.
	 */
    void onServerOptionsClicked(boolean enableDeveloperOptions);
}
