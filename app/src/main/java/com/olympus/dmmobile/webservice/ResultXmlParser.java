package com.olympus.dmmobile.webservice;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import org.xml.sax.Attributes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
/**
 * 
 * Class used for parsing the result XML
 * @version 1.0.1
 * 
 *
 */


public class ResultXmlParser {

	private static final String ROOT_ELEMENT="ils-response";	
	private String mResult="result";
	private String mResultCode="";
	private String mDisplayMessage="displaymessage";
	private String mMessage="message";
	private String mLang="lang";
	private String mResultMessage=null;
	
	private String xml="";
	/**
	 * Constructor
	 * 
	 * @param str
	 */
	public ResultXmlParser(String str) {
		// TODO Auto-generated constructor stub
		this.xml=str;
		mResultMessage=null;
	}
/**
 * Method to convert XML into byte array stream
 * 
 * @return processed result
 */
	protected InputStream getInputStream()  {
		
		ByteArrayInputStream is=new ByteArrayInputStream(xml.getBytes());
		return is;
	}
	/**
	 * Method  to parse the XML string
	 * 
	 * @param language current application language
	 * 
	 * @return processed result
	 */
	public String parse(final String language) {
		RootElement root = new RootElement(ROOT_ELEMENT);
		Element item1 = root.getChild(mResult);
		Element item2 = root.getChild(mDisplayMessage);
		final Element item3 = item2.getChild(mMessage);
		
		item1.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue("code")!=null)
					mResultCode=attributes.getValue("code");
			}
		});
		
		item3.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue(mLang)!=null)
					if(attributes.getValue(mLang).equalsIgnoreCase(language))
					{
						item3.setEndTextElementListener(new EndTextElementListener() {
							@Override
							public void end(String body) {
								if(mResultMessage==null)
									mResultMessage=body;
							}
						});
					}
			}
		});
		
		
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.ISO_8859_1, root.getContentHandler());
		} 
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return mResultCode;
	}
	
	public String getMessage()
	{
		return mResultMessage;
	}
}
