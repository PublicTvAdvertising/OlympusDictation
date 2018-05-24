package com.olympus.dmmobile.recorder;
/**
 * @version 1.0.1
 */
public class Utilities {
	/**
	 * Converts milliseconds to Timer Format
	 * Hours:Minutes:Seconds [00:00:00]
	 * 
	 * @return String with the time in timer format
	 * 
	 * */
	public static String getDurationInTimerFormat(long milliseconds) {
		String hourString = "";		//0:
		String secondsString = "";
		String minutesString = "00:";

		int hours = (int) (milliseconds / (1000 * 60 * 60));
		int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
		int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
		
		if(hours > 0){
			hourString = hours + ":";
		}else{
			hourString = "";
		}
		

		if (minutes > 0 && minutes < 10) {
			minutesString = "0" + minutes + ":";
		} else if (minutes >= 10) {
			minutesString = minutes + ":";
		}

		if (seconds < 10) {
			secondsString = "0" + seconds;
		} else {
			secondsString = "" + seconds;
		}

		hourString = hourString + minutesString + secondsString;

		return hourString;
	}
}
