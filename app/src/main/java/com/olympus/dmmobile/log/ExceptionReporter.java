package com.olympus.dmmobile.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;

import com.olympus.dmmobile.DMApplication;

/**
 * ExceptionReporter is used to capture Uncaught Exception happened in the application. 
 * The captured error will be written to  an XML file in SD card.
 * 
 * @version 1.0.1
 */
public final class ExceptionReporter {

	private static final String TAG = ExceptionReporter.class.getSimpleName();
	private static final String FILENAME = "log.xml"; // Stacktrace to be written in this file in internal memory
	
	private static final String TAG_ERROR_LOG = "errorlog";
	private static final String TAG_ERROR = "error";
	private static final String TAG_STACK_TRACE = "stacktrace";
	private static final String TAG_TIME = "time";
	private Context mContext;  
	private Handler mHandler;  //Handler to handle uncaught exception
	public static final String PREFS_NAME = "Config";
	private SharedPreferences.Editor editor;
	private SharedPreferences pref;
	private XmlSerializer mSerializer;   //XmlSerializer to serialize statcktrace for writing it into xml file
	private StringWriter mWriter;   //String writer to write serialized stacktrace into file
	private FileOutputStream mFileOut;  

	/**
	 * Registers this context and returns an error handler object
	 * to be able to manually report errors.
	 * 
	 * @param context The context
	 * @return The error handler which can be used to manually report errors
	 */
	public static ExceptionReporter register(Context context) {
		UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
		if(handler instanceof Handler) {
			Handler errHandler = (Handler) handler;
			errHandler.errorHandler.setContext(context);
			return errHandler.errorHandler;
		}
		else {
			ExceptionReporter errHandler = new ExceptionReporter(handler, context);
			Thread.setDefaultUncaughtExceptionHandler(errHandler.mHandler);
			return errHandler;
		}
	}

	private void setContext(Context context) {
		if (context.getApplicationContext() != null) {
			this.mContext = context.getApplicationContext();
		} 
		else {
			this.mContext = context;
		}
	}

	private ExceptionReporter(UncaughtExceptionHandler defaultHandler, Context context) {
		this.mHandler = new Handler(defaultHandler);
		this.setContext(context);
	}
	
	/**
	 * Handler to report and save Uncaught Exception happened in the app.
	 */
	private class Handler implements UncaughtExceptionHandler {
		private UncaughtExceptionHandler subject;
		private ExceptionReporter errorHandler;

		private Handler(UncaughtExceptionHandler subject) {
			this.subject = subject;
			this.errorHandler = ExceptionReporter.this;
		}

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			try {
				reportException(thread, ex);
			} catch (Exception e) {
			}
			subject.uncaughtException(thread, ex);
		}

	}

	/**
	 * Saves the Exception stacktrace into file.
	 * @param stackTrace Error Stacktrace as String.
	 * @param time The Time when error occured.
	 */
	private void saveException(String stackTrace, String time){
		try{
			//Creates XmlSerializer in order to write xml data
			mSerializer = Xml.newSerializer();
			mWriter = new StringWriter();
			mSerializer.setOutput(mWriter);
			File dir = new File(DMApplication.DEFAULT_DIR+"/Log");
			if(!dir.exists())
				dir.mkdirs();
			File file = new File(dir, FILENAME);
			if(!file.exists()){
				mFileOut = new FileOutputStream(file);
				//Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null)
				mSerializer.startDocument("UTF-8", true);
				mSerializer.startTag(null, TAG_ERROR_LOG);
			}else{
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader reader = sp.getXMLReader();
				ErrorXmlHandler handler = new ErrorXmlHandler();
				reader.setContentHandler(handler);
				reader.parse(new InputSource(new FileInputStream(file)));
				ArrayList<ErrorList> eList = ErrorXmlHandler.getErrorList();
				mSerializer.startDocument("UTF-8", true);
				mSerializer.startTag(null, TAG_ERROR_LOG);
				mFileOut = new FileOutputStream(file);
				for(int i = 0;i < eList.size(); i++){
					mSerializer.startTag(null, TAG_ERROR);
					mSerializer.startTag(null, TAG_STACK_TRACE);
					mSerializer.text(eList.get(i).getStackTrace());
					mSerializer.endTag(null, TAG_STACK_TRACE);
					mSerializer.startTag(null, TAG_TIME);
					mSerializer.text(eList.get(i).getTime());
					mSerializer.endTag(null, TAG_TIME);
					mSerializer.endTag(null, TAG_ERROR);
				}
			}
			mSerializer.startTag(null, TAG_ERROR);
			mSerializer.startTag(null, TAG_STACK_TRACE);
			mSerializer.text(stackTrace);
			mSerializer.endTag(null, TAG_STACK_TRACE);
			mSerializer.startTag(null, TAG_TIME);
			mSerializer.text(time);
			mSerializer.endTag(null, TAG_TIME);
			mSerializer.endTag(null, TAG_ERROR);
			mSerializer.endTag(null, TAG_ERROR_LOG);
			mSerializer.endDocument();
			mSerializer.flush();
			mFileOut.write(mWriter.toString().getBytes());
			mFileOut.close();
			setErrorConfig();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * Reports exception occurred in the app.
	 * 
	 * @param ex Occurred Exception.
	 */
	private void reportException(Thread thread, Throwable ex) {
		final Writer writer = new StringWriter();
		final PrintWriter pWriter = new PrintWriter(writer);
		ex.printStackTrace(pWriter);
		String stackTrace = writer.toString();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateTime = format.format(new Date());
		saveException(stackTrace, dateTime);	
	}
	/**
	 * Set error status in preference.
	 */
	public void setErrorConfig()
	{
	     pref =mContext.getSharedPreferences(PREFS_NAME, 0);
	     editor = pref.edit();
	     editor.putBoolean("ErrorStatus", true);
	     editor.commit();
	}

}
