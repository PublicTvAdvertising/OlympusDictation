package com.olympus.dmmobile;

/**
 * DictationStatus is an enumerator class used for the various status used with the dictation.
 * 
 * @version 1.0.1
 */
public enum DictationStatus {

	SENDING(10), RETRYING1(11), RETRYING2(12), RETRYING3(13), WAITING_TO_SEND1(15), WAITING_TO_SEND2(16),
	OUTBOX(18), SENDING_FAILED(20), TIMEOUT(22), CONVERTION_FAILED(25), UNKNOWN(35), NEW(-1), PENDING(0), SENT(2), SENT_VIA_EMAIL(3);
	
	int status;
	
	private DictationStatus(int status){
		this.status = status;
	}
	
	public int getValue(){
		return status;
	}
	
}
