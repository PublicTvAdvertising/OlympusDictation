package com.olympus.dmmobile.flashair;

/**
 * DownloadFiles class contains getter and setter methods used to set the properties of 
 * files which are downloaded from FlashAir.
 * 
 * @version 1.0.1
 */
public class DownloadFiles {

	String path;
	String name;
	long size;
	boolean status = false;
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	
	public boolean getStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
	
	public DownloadFiles copy()
	{
		DownloadFiles copy=new DownloadFiles();
		copy.path=getPath();
		copy.name=getName();
		copy.size=getSize();
		
		return copy;
	}
	
}
