package com.olympus.dmmobile.settings;

/**
 * model class used to keep all the details of a recipient
 * @version 1.2.0
 */
public class Recipient {
	private int id;		
	private String name;	// name of the recipient
	private String email;	// email address of the recipient
	private boolean selected;	// recipient selection state
	private boolean deleteButtonVisible;
	
	public static final String EMAIL_COLUMN = "email";
	public static final String NAME_COLUMN = "name";
	public static final String ID_COLUMN = "_id";
	
	public static final String SELECTED_RECIPIENT_NAME_TAG = "selected_recipient_name";
	public static final String SELECTED_RECIPIENT_EMAIL_TAG = "selected_recipient_email";
	public static final String RECIPIENT_FORCE_UPDATE_TAG = "recipient_force_update";
	
	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getText(){
		
		return getEmail();
	}
	public boolean isDeleteButtonVisible() {
		return deleteButtonVisible;
	}
	public void setDeleteButtonVisible(boolean deleteButtonVisible) {
		this.deleteButtonVisible = deleteButtonVisible;
	}

}
