package com.olympus.dmmobile.flashair;

/**
 * FlashAirFiles class is used to handle Flashair files details. 
 * 
 * @version 1.0.1
 */
public class FlashAirFiles
{
	private String path = "";
	private String name = "";
	private long size = 0;
	private String date = "";
	private String type = "";
	private String time = "";
	private boolean check = false;
	private Object downFile = "";
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
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
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
	public void setCheck(boolean check){
		this.check = check;
	}
	
	public boolean isChecked(){
		return check;
	}
	
	public void setDownFile(Object downFile){
		this.downFile = downFile;
	}
	
	public Object getDownFile(){
		return downFile;
	}
	
	public FlashAirFiles copy()
	{
		FlashAirFiles copy=new FlashAirFiles();
		copy.path=getPath();
		copy.name=getName();
		copy.size=getSize();
		copy.date=getDate();
		copy.type=getType();
		copy.time=getTime();
		copy.check = isChecked();
		return copy;
	}
}