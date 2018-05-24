package com.olympus.dmmobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;


/**
 * This class is used to parse Query Dictation Response.
 * 
 * @version 1.0.1
 */
public class DictationQueryXmlParser {
	private final String ROOT_ELEMENT="ils-response";
	private final String SUB_ELEMENT_RESULT="result";
	private final String SUB_ELEMENT_STATUS="status";
	private final String RESULT_ATTRIBUTE="code";
	private final String STATUS_CODE="transferid";
	private String displayMessage="displaymessage";
	private String message="message";
	private String lang="lang";
	private String resutMessage=null;
	
	private String xml="";
	private List<AttributeObjects> objectsAttributes= new ArrayList<AttributeObjects>();
	
	/**
	 * Constructor
	 * 
	 * @param str Upload Dictation Response
	 */
	public DictationQueryXmlParser(String str) {
		this.xml=str;
	}

	/**
	 * To convert XML string to the byte stream.
	 * @return InputStream
	 */
	protected InputStream getInputStream() 
	{
		ByteArrayInputStream is=new ByteArrayInputStream(xml.getBytes());
		return is;
	}

	/**
	 * This method parse the XML response string
	 * 
	 * @param language current language of the application
	 * 
	 * @return list of attributes
	 */
	public List<AttributeObjects> parse(final String language)
	{
		final AttributeObjects attributeObjects= new AttributeObjects();
		
		RootElement root = new RootElement(ROOT_ELEMENT);
		
		Element itemResult = root.getChild(SUB_ELEMENT_RESULT);
		Element itemJobData = root.getChild(SUB_ELEMENT_STATUS);
		Element itemDisplayMsg = root.getChild(displayMessage);
		final Element itemMessage = itemDisplayMsg.getChild(message);
		
		itemResult.setEndElementListener(new EndElementListener()
		{
			public void end()
			{
				objectsAttributes.add(attributeObjects.copy());
			}
		});
		itemJobData.setEndElementListener(new EndElementListener()
		{
			public void end()
			{
				objectsAttributes.add(attributeObjects.copy());
				objectsAttributes.set(0,attributeObjects);
			}
		});
		itemResult.setStartElementListener(new StartElementListener(){
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue(RESULT_ATTRIBUTE)!=null)
					attributeObjects.setResultCode(attributes.getValue(RESULT_ATTRIBUTE));
			}
		});
		itemJobData.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue(STATUS_CODE)!=null)
					attributeObjects.setTranferId(attributes.getValue(STATUS_CODE));
			}
		});
		itemJobData.setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				attributeObjects.setStatusCode(body);
			}
		});
		
		itemMessage.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue(lang)!=null)
					if(attributes.getValue(lang).equalsIgnoreCase(language))
					{
						itemMessage.setEndTextElementListener(new EndTextElementListener() {
							@Override
							public void end(String body) {
								if(resutMessage==null)
									resutMessage=body;
							}
						});
					}
			}
		});
		
		try 
		{
			Xml.parse(this.getInputStream(), Xml.Encoding.ISO_8859_1, root.getContentHandler());
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return objectsAttributes;
	}
	
	/**
	 * Get server side exception message.
	 * 
	 * @return server side exception
	 */
	public String getMessage()
	{
		return resutMessage;
	}
	
	/**
	 * Object class for the attribute list.
	 */
	public class AttributeObjects{
		private String resultCode="";
		private String statusCode="";
		private String tranferId="";
		
		public String getResultCode() {
			return resultCode;
		}
		public void setResultCode(String resultCode) {
			this.resultCode = resultCode;
		}
		public String getStatusCode() {
			return statusCode;
		}
		public void setStatusCode(String statusCode) {
			this.statusCode = statusCode;
		}
		public String getTranferId() {
			return tranferId;
		}
		public void setTranferId(String tranferId) {
			this.tranferId = tranferId;
		}
		public AttributeObjects copy()
		{
			AttributeObjects copy = new AttributeObjects();
			copy.resultCode=getResultCode();
			copy.statusCode=getStatusCode();
			copy.tranferId=getTranferId();
			return copy;
		}
	}
}
