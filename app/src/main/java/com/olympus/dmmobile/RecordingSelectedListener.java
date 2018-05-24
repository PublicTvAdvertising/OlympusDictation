package com.olympus.dmmobile;


/**
 * This Listener listens for recording selection and update button states.
 * 
 * @version 1.0.1
 */
public interface RecordingSelectedListener {
	/**
	 * This method is invoked when user selects Dictation from Pending, Outbox or Sent list.
	 * @param dictPos Dictation position value as int.
	 * @param isChecked Whether the selected Dictation is checked or not as boolean.
	 */
	public void onRecordingSelected(int dictPos, boolean isChecked);
	/**
	 * This method is to update send all or delete all button
	 */
	public void updateButtonState(boolean enable); 	
}
