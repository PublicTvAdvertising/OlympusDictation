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
 * This class is used to parse Upload Dictation Response.
 * 
 * @version 1.0.1
 */
public class DictationUploadFileXmlParser {
	private final String ROOT_ELEMENT="ils-response";
	private final String SUB_ELEMENT_RESULT="result";
	private final String SUB_ELEMENT_JOBDATA="jobdata";
	private final String RESULT_ATTRIBUTE="code";
	private final String JOBDATA_ATTRIBUTE="filename";
	private final String JOBDATA_CHILD_TRANFERID="transferid";
	private final String JOBDATA_CHILD_JOBNUMBER="jobnumber";
	private String displayMessage="displaymessage";
	private String message="message";
	private String lang="lang";
	private String resutMessage=null;
	
	private String xml="";
	private List<AttributeObjects> objectsAttributes= new ArrayList<AttributeObjects>();
	private List<JobDataObjects> objectsJobData= new ArrayList<JobDataObjects>();
	
	/**
	 * Constructor
	 * @param str Upload Dictation Response
	 */
	public DictationUploadFileXmlParser(String str) {
		this.xml=str;
		resutMessage=null;
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
	 * @param language current language of an Application
	 */
	public void parse(final String language)
	{
		final AttributeObjects attributeObjects= new AttributeObjects();
		final JobDataObjects jobDataObjects=new JobDataObjects();
		
		RootElement root = new RootElement(ROOT_ELEMENT);
		
		Element itemResult = root.getChild(SUB_ELEMENT_RESULT);
		Element itemJobData = root.getChild(SUB_ELEMENT_JOBDATA);
		Element itemJobDataNumber = itemJobData.getChild(JOBDATA_CHILD_JOBNUMBER);
		Element itemJobDataID = itemJobData.getChild(JOBDATA_CHILD_TRANFERID);
		Element itemDisplayMsg = root.getChild(displayMessage);
		final Element itemMessage = itemDisplayMsg.getChild(message);
		
		itemResult.setEndElementListener(new EndElementListener()
		{
			public void end()
			{
				objectsAttributes.add(attributeObjects.copy());
			}
		});
		itemResult.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue(RESULT_ATTRIBUTE)!=null)
					attributeObjects.setResultCode(attributes.getValue(RESULT_ATTRIBUTE));
			}
		});
		itemJobData.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue(JOBDATA_ATTRIBUTE)!=null)
					attributeObjects.setJobDataFileName(attributes.getValue(JOBDATA_ATTRIBUTE));
			}
		});
		
		itemJobData.setEndElementListener(new EndElementListener()
		{
			public void end()
			{
				objectsJobData.add(jobDataObjects.copy());
				objectsAttributes.set(0,attributeObjects);
			}
		});
		itemJobDataID.setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				jobDataObjects.setTransferId(body);
			}
		});
		itemJobDataNumber.setEndTextElementListener(new EndTextElementListener() {
			@Override
			public void end(String body) {
				jobDataObjects.setJobNumber(body);
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
	}
	
	/**
	 * @return list of attributes
	 */
	public List<AttributeObjects> getAttributeObjects()
	{
		return objectsAttributes;
	}
	
	/**
	 * Get Job Data for each dictations.
	 * 
	 * @return list of job attributes
	 */
	public List<JobDataObjects> getJobDataObjects()
	{
		return objectsJobData;
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
		private String jobDataFileName="";
		
		public String getResultCode() {
			return resultCode;
		}
		public void setResultCode(String resultCode) {
			this.resultCode = resultCode;
		}
		public String getJobDataFileName() {
			return jobDataFileName;
		}
		public void setJobDataFileName(String jobDataFileName) {
			this.jobDataFileName = jobDataFileName;
		}
		public AttributeObjects copy()
		{
			AttributeObjects copy = new AttributeObjects();
			copy.resultCode=getResultCode();
			copy.jobDataFileName=getJobDataFileName();
			return copy;
		}
	}
	
	/**
	 * Object class for the Job details list.
	 */
	public class JobDataObjects{
		private String transferId="";
		private String jobNumber="";

		public String getTransferId() {
			return transferId;
		}
		public void setTransferId(String transferId) {
			this.transferId = transferId;
		}
		public String getJobNumber() {
			return jobNumber;
		}
		public void setJobNumber(String jobNumber) {
			this.jobNumber = jobNumber;
		}
		
		public JobDataObjects copy()
		{
			JobDataObjects copy = new JobDataObjects();
			copy.transferId = getTransferId();
			copy.jobNumber = getJobNumber();
			return copy;
		}
	}
}
