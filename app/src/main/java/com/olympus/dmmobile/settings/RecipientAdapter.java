package com.olympus.dmmobile.settings;

import static com.olympus.dmmobile.settings.RecipientActivity.SWIPE_MENU_CREATE;
import static com.olympus.dmmobile.settings.RecipientActivity.SWIPE_MENU_DONT_CREATE;
import static com.olympus.dmmobile.settings.RecipientActivity.SWIPE_MENU_TYPES;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.DatabaseHandler;
import com.olympus.dmmobile.R;

/**
 * Adapter class to set and manage items in the listview
 * @version 1.2.0
 *
 */
public class RecipientAdapter extends BaseAdapter {
	
	/**
	 * Interface that define callback methods to update the activity when a recipient is deleted
	 * @version 1.2.0
	 */
	interface OnRecipientDeleteListener {
		/**
		 * callback method to update the activity when a recipient is deleted
		 */
		void onRecipientDeleted();
	}

	OnRecipientDeleteListener deleteListener;
	private Context context;
	private LayoutInflater inflater;
	private ArrayList<Recipient> itemList;
	private ViewHolder viewHolder;
	private boolean isUpdated = false;
	
	
	/**
	 * constructor to set the adapter with list items
	 * @param context context of the activity
	 * @param list item list
	 */
	public RecipientAdapter(Context context, ArrayList<Recipient> list) {
		this.context = context;
		this.itemList = list;
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		deleteListener = (OnRecipientDeleteListener) context;
	}
	
	@Override
	public int getItemViewType(int position) {
		
		if(position == 0) 
			return SWIPE_MENU_DONT_CREATE;
		else 
			return SWIPE_MENU_CREATE;
	}
	
	@Override
	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return SWIPE_MENU_TYPES;
	}
	

	@Override
	public int getCount() {
		if (itemList != null)
			return itemList.size();
		else
			return 0;
	}

	@Override
	public Object getItem(int position) {
		if (itemList != null)
			return itemList.get(position);
		else
			return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
				
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.recipient_list_view,
					parent, false);
			viewHolder = new ViewHolder();
			viewHolder.checkedTextView = (CheckedTextView) convertView
					.findViewById(R.id.recipient_textview);
//			viewHolder.tvCheckedText = (TextView) convertView
//					.findViewById(R.id.tv_checked_text);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (position == 0) {
			viewHolder.checkedTextView
					.setText(itemList.get(position).getName());
		} else {
			viewHolder.checkedTextView

					.setText(itemList.get(position).getText());
		}
		if (itemList.get(position).isSelected()) {
			viewHolder.checkedTextView.setChecked(true);
		} else {
			viewHolder.checkedTextView.setChecked(false);
		}
		
		viewHolder.position = position;
		return convertView;
	}
	
	/**
	 * method to toggle the selection of list item
	 * @param currentPosition position of item
	 */
	public void toggleSelectedItem(int currentPosition) {
		String recipientToCheck = "";
		
		for (int index = 0; index < itemList.size(); index++) {
			if (index == currentPosition) {
				itemList.get(index).setSelected(true);
				
				if(itemList.get(index).getName().equalsIgnoreCase(context.getResources().getString(R.string.Follow_Server_Settings))){
					recipientToCheck = itemList.get(index).getName();
				}else{
					recipientToCheck = itemList.get(index).getEmail();
				}
				
				if(didSelectionChanged(recipientToCheck)){
					isUpdated = true;
				}else{
					isUpdated = false;
				}
				
			} else {
				itemList.get(index).setSelected(false);
			}
		}
	}
	
	/**
	 * method to check whether the previous selection has changed or not
	 * @param selectedRecipient previous selected recipient
	 * @return returns true if the selection has changed, else false
	 */
	private boolean didSelectionChanged(String selectedRecipient){
		if(selectedRecipient.equalsIgnoreCase(readSelectedRecipientEmailFromPreference())){
			return false;
		}
		return true;
	}
	
	public void toggleDeleteVisibility(int currentPosition, boolean visibility) {
		for (int index = 1; index < itemList.size(); index++) {
			if (index == currentPosition) {
				itemList.get(index).setDeleteButtonVisible(visibility);
			} else {
				itemList.get(index).setDeleteButtonVisible(false);
			}
		}
		notifyDataSetInvalidated();
	}
	
	/**
	 * method to delete the item at given position
	 * @param deletePosition position of item to delete
	 */
	public  void deleteItem( int deletePosition) {
		DMApplication dmApplication = (DMApplication) context
				.getApplicationContext();
		DatabaseHandler dbHandler = dmApplication.getDatabaseHandler();

		dbHandler.deleteRecipient(itemList.get(deletePosition).getId());
		itemList.remove(deletePosition);
		
		deleteListener.onRecipientDeleted();
	}
	
	/**
	 * method to update the value of edited item in the listview
	 * @param id item id
	 * @param value new value to set
	 */
	public void updateEditedItem(int id, String value){
		for(int i = 0; i<itemList.size(); i++){
			if(itemList.get(i).getId() == id){
				itemList.get(i).setEmail(value);
			}
		}
		notifyDataSetChanged();
	}
	
	/**
	 * method to save selected recipient to preference
	 * @param position position of item in listview
	 */
	public void saveSelectedRecipientToPreference(int position) {
		String selected_recipient_name = "";
		String selected_recipient_email = "";
		if (itemList
				.get(position)
				.getName()
				.equalsIgnoreCase(
						context.getResources().getString(
								R.string.Follow_Server_Settings))) {
			selected_recipient_name = itemList.get(position).getName();
		} else {
			selected_recipient_name = itemList.get(position).getName();
			selected_recipient_email = itemList.get(position).getEmail();
		}
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor mEditor = pref.edit();
		mEditor.putString(Recipient.SELECTED_RECIPIENT_NAME_TAG , selected_recipient_name);
		mEditor.putString(Recipient.SELECTED_RECIPIENT_EMAIL_TAG , selected_recipient_email);
		mEditor.commit();
		isUpdated = true;
	}
	
	/**
	 * method to read the selected recipient from preference
	 */
	private String readSelectedRecipientEmailFromPreference() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String email = pref.getString(
				Recipient.SELECTED_RECIPIENT_EMAIL_TAG,
				"");
		return email;
	}
	
	/**
	 * method to get email address of selected recipient
	 */
	public String getSelectedRecipientEmail(){
		int position = 0;
		for(int index = 0; index < itemList.size(); index++){
			if(itemList.get(index).isSelected()){
				position = index;
			}
		}
		if (itemList
				.get(position)
				.getName()
				.equalsIgnoreCase(
						context.getResources().getString(
								R.string.Follow_Server_Settings))) {
			//recipient = itemList.get(position).getName();
			return itemList.get(position).getName();
		} else {
			return itemList.get(position).getEmail();
		}
		
	}
	
	/**
	 * method to get the Recipient item at given position
	 * @param position item position
	 * @return Recipient item
	 */
	public Recipient getRecipient(int position){
		if(itemList != null){
			return itemList.get(position);
		}else{
			return null;
		}
	}
	
	/**
	 * method to check whether the recipient selection has changed or not
	 * @return returns true, if the recipient selection has changed
	 */
	public boolean isRecipientUpdated() {
		return isUpdated;
	}
	/**
	 * method to set update status
	 */
	public void setUpdateStatus(boolean status){
		isUpdated = status;
	}

	public class ViewHolder {
		CheckedTextView checkedTextView;
		TextView tvCheckedText;
		int position;
	}
	
	/**
	 * method to get item list of the adapter
	 * @return item list
	 */
	public ArrayList<Recipient>  getListArray(){
		return itemList;
	}

}
