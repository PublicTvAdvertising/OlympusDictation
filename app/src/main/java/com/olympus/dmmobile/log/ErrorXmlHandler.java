package com.olympus.dmmobile.log;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ErrorXmlHandler is used to parse error stack trace.
 * 
 * @version 1.0.1
 */
public class ErrorXmlHandler extends DefaultHandler {

	private static final String TAG_ERROR = "error";
	private static final String TAG_STACK_TRACE = "stacktrace";
	private static final String TAG_TIME = "time";
	private static ArrayList<ErrorList> errorList = new ArrayList<ErrorList>();
	private ErrorList mError;
	private boolean mCurrent = false;
	private StringBuilder mCurrentValue = null;
	
	@Override
	public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException{
		mCurrent = true;
		if(localName.equalsIgnoreCase(TAG_ERROR)){
			mError = new ErrorList();
		}
		else if(localName.equals(TAG_STACK_TRACE)){
			mCurrentValue = new StringBuilder();
		}
		else if(localName.equals(TAG_TIME)){
			mCurrentValue = new StringBuilder();
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)throws SAXException {
		mCurrent = false;
		if(localName.equals(TAG_STACK_TRACE)){
			mError.setStackTrace(mCurrentValue.toString());
		}
		else if(localName.equals(TAG_TIME)){
			mError.setTime(mCurrentValue.toString());
		}
		else if(localName.equals(TAG_ERROR)){
			errorList.add(mError);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)throws SAXException {
		if(mCurrent){
			mCurrentValue.append(ch, start, length);
		}
	}
	
	public static ArrayList<ErrorList> getErrorList(){
		return errorList;
	}
}
