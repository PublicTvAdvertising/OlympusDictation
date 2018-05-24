package com.olympus.dmmobile.webservice;


import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import org.xml.sax.Attributes;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Class used for Parsing XML response of settings from ILS 
 * @version 1.0.1
 * 
 *
 */
public class Settingsparser {
	private final String ROOT_ELEMENT="ils-response";
	private final String SUB_ELEMENT_RESULT="result";
	private final String SUB_ELEMENT_SETTINGS="settings";
	
	private final String SETTINGS_CHILD_AUTHOR="author";
	private final String SETTINGS_CHILD_WORKTYPELIST="worktypelist";
	private final String SETTINGS_CHILD_AUDIO="audio";
	private final String SETTINGS_CHILD_DELIVERY="delivery";
	
	private final String WORKTYPE="worktype";
	
	private final String FORMAT="format";
	private final String ENCRIPTION="encryption";
	private final String PASSWORD="password";
	
	private String xml="";
	private List<RootObjects> objectsRoot= new ArrayList<RootObjects>();
	private List<SettingsObjects> objectsSettings= new ArrayList<SettingsObjects>();
	private List<WorkTypeListObjects> objectsWorkType= new ArrayList<WorkTypeListObjects>();
	private List<AudioObjects> objectsAudio= new ArrayList<AudioObjects>();
	private String displayMessage="displaymessage";
	private String message="message";
	private String lang="lang";
	private String resutMessage=null;
	public Settingsparser(String str) {
		this.xml=str;
	};
	/**
	 * Method to convert XML into byte array stream
	 * 
	 * @return processed result
	 */
	protected InputStream getInputStream() {
		ByteArrayInputStream is=new ByteArrayInputStream(xml.getBytes());
		return is;
	}
	/**
	 *  Method  to parse the XML string
	 * @param language current application language
	 */
	public void parse(final String language) {
		final RootObjects rootObjects= new RootObjects();
		final SettingsObjects settingsObjects=new SettingsObjects();
		final WorkTypeListObjects workTypeListObjects=new WorkTypeListObjects();
		final AudioObjects audioObjects=new AudioObjects();		
		RootElement root = new RootElement(ROOT_ELEMENT);		
		Element itemResult = root.getChild(SUB_ELEMENT_RESULT);
		Element itemSettings = root.getChild(SUB_ELEMENT_SETTINGS);
		Element itemSettingsWorkType = itemSettings.getChild(SETTINGS_CHILD_WORKTYPELIST);
		Element itemSettingsAudio = itemSettings.getChild(SETTINGS_CHILD_AUDIO);
		Element itemDisplayMsg = root.getChild(displayMessage);
		final Element itemMessage = itemDisplayMsg.getChild(message);
		itemResult.setEndElementListener(new EndElementListener() {
			public void end() {
				objectsRoot.add(rootObjects.copy());
			}
		});
		itemResult.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue("code")!=null)
				rootObjects.setResult_code(attributes.getValue("code"));
			}
		});
		
		itemSettings.setEndElementListener(new EndElementListener() {
			public void end() {
				objectsSettings.add(settingsObjects.copy());
			}
		});
		itemSettings.getChild(SETTINGS_CHILD_AUTHOR).setEndTextElementListener(new EndTextElementListener() {
			public void end(String body)  {
				settingsObjects.setAuthor(body);
			}
		});
		itemSettingsWorkType.setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				if(attributes.getValue("name")!=null)
					settingsObjects.setWorktypelist(attributes.getValue("name"));
			}
		});
		itemSettings.getChild(SETTINGS_CHILD_DELIVERY).setEndTextElementListener(new EndTextElementListener() {
			public void end(String body)  {
				settingsObjects.setDelivery(body);
			}
		});
		
		itemSettingsWorkType.getChild(WORKTYPE).setEndTextElementListener(new EndTextElementListener() {
			public void end(String body)  {
				if(workTypeListObjects!=null){
				workTypeListObjects.setWorktype(body);
				objectsWorkType.add(workTypeListObjects.copy());
				}
			}
		});
		itemSettingsAudio.setEndElementListener(new EndElementListener() {
			public void end() {
				objectsAudio.add(audioObjects.copy());
			}
		});
		itemSettingsAudio.getChild(FORMAT).setEndTextElementListener(new EndTextElementListener() {
			public void end(String body)  {
				audioObjects.setFormat(body);
			}
		});
		itemSettingsAudio.getChild(ENCRIPTION).setEndTextElementListener(new EndTextElementListener() {
			public void end(String body)  {
				audioObjects.setEncryption(body);
			}
		});
		itemSettingsAudio.getChild(PASSWORD).setEndTextElementListener(new EndTextElementListener() {
			public void end(String body)  {
				audioObjects.setPassword(body);
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
		
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.ISO_8859_1, root.getContentHandler());
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	public List<RootObjects> getRootObjects() {
		return objectsRoot;
	}
	public List<SettingsObjects> getSettingsObjects() {
		return objectsSettings;
	}
	public List<WorkTypeListObjects> getWorkTypeListObjects() {
		return objectsWorkType;
	}
	public List<AudioObjects> getAudioObjects(){
		return objectsAudio;
	}
	/**
	 * getter and setter class for result code
	 * 
	 * 
	 *
	 */
	public class RootObjects {
		private String result_code="";
		
		public String getResult_code() {
			return result_code;
		}
		public void setResult_code(String result_code) {
			this.result_code = result_code;
		}
		
		public RootObjects copy() {
			RootObjects copy = new RootObjects();
			copy.result_code=getResult_code();
			return copy;
		}
	}
	/**
	 * setter and getter class to obtain author,worktype list and delivery
	 * 
	 * 
	 *
	 */
	public class SettingsObjects{
		private String author="";
		private String worktypelist="";
		private String delivery="";
		
		public String getAuthor() {
			return author;
		}
		public void setAuthor(String author) {
			
			this.author = author;
		}
		public String getWorktypelist() {
			return worktypelist;
		}
		public void setWorktypelist(String worktypelist) {
			this.worktypelist = worktypelist;
		}
		public String getDelivery() {
			return delivery;
		}
		public void setDelivery(String delivery) {
			this.delivery = delivery;
		}
		public SettingsObjects copy() {
			SettingsObjects copy = new SettingsObjects();
			copy.author = getAuthor();
			copy.worktypelist = getWorktypelist();
			copy.delivery=getDelivery();
			return copy;
		}
	}
	/**
	 * getter and setter class to get worktype list
	 * 
	 * 
	 *
	 */
	public class WorkTypeListObjects{
		private String worktype="";
		public String getWorktype() {
			return worktype;
		}
		public void setWorktype(String worktype) {
			this.worktype = worktype;
		}
		public WorkTypeListObjects copy()
		{
			WorkTypeListObjects copy = new WorkTypeListObjects();
			copy.worktype=getWorktype();
			return copy;
		}
	}
	/**
	 * getter and setter class to get audio objects
	 * 
	 * 
	 *
	 */
	public class AudioObjects{
		private String format="";
		private String encryption="";
		private String password="";
		
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		public String getEncryption() {
			return encryption;
		}
		public void setEncryption(String encryption) {
			this.encryption = encryption;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public AudioObjects copy() {
			AudioObjects copy = new AudioObjects();
			copy.format = getFormat();
			copy.encryption = getEncryption();
			copy.password=getPassword();
			return copy;
		}
	}
	public String getMessage()
	{
		return resutMessage;
	}
}
