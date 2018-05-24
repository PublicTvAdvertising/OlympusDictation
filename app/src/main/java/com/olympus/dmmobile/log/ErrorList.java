package com.olympus.dmmobile.log;

/**
 * Class used to hold the details of exception occurred
 * @version 1.0.1
 *
 */
public class ErrorList {

	private String stackTrace;
	private String time;
	
	public String getStackTrace() {
		return stackTrace;
	}
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
	
}
