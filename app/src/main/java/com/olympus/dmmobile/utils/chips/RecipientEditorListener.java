package com.olympus.dmmobile.utils.chips;


/**
 * Interface that define callback methods for validating the recipients
 * @version 1.2.0
 *
 */
public interface RecipientEditorListener {
	/**
	 * callback method to notify when an invalid recipient is entered
	 */
	void onInvalidRecipientEntered();
	/**
	 * callback method to notify when tap on done button with empty text
	 */
	void onEmptyTextEntered();
	/**
	 * callback method to notify when the entered recipient already exists
	 */
	void onRecipientAlreadyExist();
	/**
	 * callback method to notify when validation succeeds
	 * @param recipientData recipient string
	 */
	void onValidationSuccess(String recipientData);
	/**
	 * callback method to notify when maximum recipient limit is reached
	 */
	void onRecipientLimitExceeded();

}
