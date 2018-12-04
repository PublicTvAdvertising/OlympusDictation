package com.olympus.dmmobile.settings;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.DatabaseHandler;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.settings.RecipientAdapter.OnRecipientDeleteListener;
import com.olympus.dmmobile.utils.swipe.SwipeMenu;
import com.olympus.dmmobile.utils.swipe.SwipeMenuCreator;
import com.olympus.dmmobile.utils.swipe.SwipeMenuItem;
import com.olympus.dmmobile.utils.swipe.SwipeMenuListView;
import com.olympus.dmmobile.utils.swipe.SwipeMenuListView.OnMenuItemClickListener;
import com.olympus.dmmobile.utils.swipe.SwipeMenuListView.OnSwipeListener;

/**
 * Activity used to list the saved recipients and perform actions on them.
 * 
 * @version 1.2.0
 */
public class RecipientActivity extends AppCompatActivity implements
		OnRecipientDeleteListener {

	private ExceptionReporter mReporter; // Error Logger
	private SwipeMenuListView recipientListView; // recipient list view with
	DMApplication dmApplication;
	RecipientAdapter listAdapter; // recipient list adapter
	GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	Button mBtnDeleteRecipient;
	int deletePosition;
FloatingActionButton floatingActionButton;
	// request codes
	private static final int RECIPIENT_EMAIL_REQUEST = 1002;
	private static final int RECIPIENT_EMAIL_EDIT_REQUEST = 1003;
	private static final int SWIPE_MIN_DISTANCE = 60;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	// context menu items id
	private static final int ITEM_EDIT = 1;
	private static final int ITEM_DELETE = 2;
	private static final int ITEM_CANCEL = 3;

	private static final int ITEM_SWIPE_DELETE = 0;

	private final int USE_PORTAL_SETTINGS = 0;
	boolean isEditAlreadySelectedItem = false;
	boolean didAlreadySelectedRecipientDeleted = false;

	public static final int SWIPE_MENU_DONT_CREATE = 0;
	public static final int SWIPE_MENU_CREATE = 1;
	public static final int SWIPE_MENU_TYPES = 2;

	private static int itemIndexSelected;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mReporter = ExceptionReporter.register(this);
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.recipient_activity);

		Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);


		setTitle(getResources().getString(
				R.string.server_options_recipient_title));
		dmApplication = (DMApplication) getApplication();
		recipientListView = (SwipeMenuListView) findViewById(R.id.recipientList);
		recipientListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		registerForContextMenu(recipientListView);
		gestureDetector = new GestureDetector(new CustomGestureDetector());
		gestureListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};

		floatingActionButton=(FloatingActionButton) findViewById(R.id.fab);
		floatingActionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent addRecipientsIntent = new Intent(RecipientActivity.this,
						AddRecipientActivity.class);
				startActivityForResult(addRecipientsIntent, RECIPIENT_EMAIL_REQUEST);
			}
		});

		// set item click listener for recipient listview
		recipientListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long id) {
				listAdapter.toggleSelectedItem(position);
				listAdapter.notifyDataSetChanged();
			}
		});

		SwipeMenuCreator creator = new SwipeMenuCreator() {

			@Override
			public void create(SwipeMenu menu) {

				switch (menu.getViewType()) {
				case 1:

					SwipeMenuItem deleteItem = new SwipeMenuItem(
							getApplicationContext());
					deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
							0x3F, 0x25)));
					deleteItem.setWidth(dp2px(90));
					deleteItem.setIcon(R.drawable.ic_delete_icon);
					menu.addMenuItem(deleteItem);

					break;

				case 0:
					// Dont create swipe menu
					break;
				}

			}
		};
		recipientListView.setMenuCreator(creator);

		// set item click listener for delete item
		recipientListView
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(int position,
							SwipeMenu menu, int index) {

						switch (index) {
						case ITEM_SWIPE_DELETE:

							String recipientToDelete = listAdapter
									.getRecipient(position).getEmail();
							boolean shouldToggleSelection = listAdapter
									.getRecipient(position).isSelected();
							listAdapter.deleteItem(position);
							if (isThisSelectedRecipient(recipientToDelete)) {
								// if selected item is deleted ,change the
								// selection to default settings
								listAdapter
										.saveSelectedRecipientToPreference(USE_PORTAL_SETTINGS);
								if (shouldToggleSelection) {
									listAdapter
											.toggleSelectedItem(USE_PORTAL_SETTINGS);
								}
								setRecipientForceUpdate(true);
								isEditAlreadySelectedItem = true;
								didAlreadySelectedRecipientDeleted = true;
							}
							if (shouldToggleSelection) {
								listAdapter
										.toggleSelectedItem(USE_PORTAL_SETTINGS);
							}

							break;
						}
						return false;
					}
				});

		// set SwipeListener
		recipientListView.setOnSwipeListener(new OnSwipeListener() {

			@Override
			public void onSwipeStart(int position) {
				// swipe start
			}

			@Override
			public void onSwipeEnd(int position) {
				// swipe end
			}
		});

		// set long click listener to show the context menu
		recipientListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {

						itemIndexSelected = position;
						if (position != 0) {

							recipientListView.showContextMenu();
						}
						return true;
					}
				});

		setRecipientListView();

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		updateListView();
	}

	/**
	 * method which set value in shared preference to update the recipients
	 * forcefully
	 * 
	 * @param status
	 *            value to set the state
	 */
	private void setRecipientForceUpdate(boolean status) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(RecipientActivity.this);
		SharedPreferences.Editor mEditor = pref.edit();
		mEditor.putBoolean(Recipient.RECIPIENT_FORCE_UPDATE_TAG, status);
		mEditor.commit();
	}

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getResources().getDisplayMetrics());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, ITEM_EDIT, 0,
				getResources().getString(R.string.Property_Edit));
		menu.add(0, ITEM_DELETE, 0,
				getResources().getString(R.string.Settings_Delete));
		menu.add(0, ITEM_CANCEL, 0,
				getResources().getString(R.string.Button_Cancel));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (item.getItemId() == ITEM_EDIT) {

			Recipient seletedItem = (Recipient) listAdapter
					.getItem(itemIndexSelected);
			int id = seletedItem.getId();
			String email = seletedItem.getEmail() + ",";
			isEditAlreadySelectedItem = seletedItem.isSelected();
			Intent intent = new Intent(RecipientActivity.this,
					AddRecipientActivity.class);
			intent.putExtra("email_edit", true);
			intent.putExtra("id", id);
			intent.putExtra("email", email);
			intent.putExtra("isEditAlreadySelectedItem",
					isEditAlreadySelectedItem);
			startActivityForResult(intent, RECIPIENT_EMAIL_EDIT_REQUEST);

		} else if (item.getItemId() == ITEM_DELETE) {

			String recipientToDelete = listAdapter.getRecipient(
					itemIndexSelected).getEmail();
			boolean shouldToggleSelection = listAdapter.getRecipient(
					itemIndexSelected).isSelected();
			listAdapter.deleteItem(itemIndexSelected);
			if (isThisSelectedRecipient(recipientToDelete)) {
				// save selected recipient to shared preference
				listAdapter
						.saveSelectedRecipientToPreference(USE_PORTAL_SETTINGS);
				// if selected item is deleted ,change the
				// selection to default settings
				if (shouldToggleSelection) {
					listAdapter.toggleSelectedItem(USE_PORTAL_SETTINGS);
				}
				setRecipientForceUpdate(true);
				isEditAlreadySelectedItem = true;
				didAlreadySelectedRecipientDeleted = true;
			}
			if (shouldToggleSelection) {
				// change selection to default
				listAdapter.toggleSelectedItem(USE_PORTAL_SETTINGS);
			}

		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recipient, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();
		if (id == R.id.action_add_recipient) {
			// start AddRecipientActivity to add recipients
			Intent addRecipientsIntent = new Intent(RecipientActivity.this,
					AddRecipientActivity.class);
			startActivityForResult(addRecipientsIntent, RECIPIENT_EMAIL_REQUEST);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case RECIPIENT_EMAIL_REQUEST:
			if (resultCode == RESULT_OK) {
				// get the selected recipient details from AddRecipientActivity
				// result intent
				String name = data.getExtras().getString("name");
				String email = data.getExtras().getString("email");

				saveSelectedRecipient(name, email);
				setRecipientListView();
			}

			break;
		case RECIPIENT_EMAIL_EDIT_REQUEST:
			// get the edited details from AddRecipientActivity result intent
			// and
			// update the recipient with the changes
			isEditAlreadySelectedItem = data.getExtras().getBoolean(
					"isEditAlreadySelectedItem", false);
			if (data.getExtras().getBoolean("edit_success")) {
				int id = data.getExtras().getInt("id");
				String email = data.getExtras().getString("email");
				listAdapter.updateEditedItem(id, email);
			}
			listAdapter.notifyDataSetChanged();
			break;
		default:
			break;
		}
	}

	/**
	 * method to check whether the given recipient already exist in the database
	 * or not
	 * 
	 * @param email
	 *            recipient to check
	 * @return returns true, if the recipient already exists in database
	 */
	private boolean isRecipientAlreadyExists(String email) {
		DatabaseHandler dbHandler = dmApplication.getDatabaseHandler();
		Cursor c = dbHandler.getAllRecipient();
		if (c != null && c.getCount() >= 1) {
			c.moveToFirst();
			for (int index = 0; index < c.getCount(); index++) {
				if (c.getString(c.getColumnIndex(Recipient.EMAIL_COLUMN))
						.equalsIgnoreCase(email)) {
					return true;
				}
				c.moveToNext();
			}
		}
		return false;
	}

	/**
	 * method to save new recipient in database
	 * 
	 * @param name
	 *            name of the recipient
	 * @param email
	 *            email of the recipient
	 */
	private void saveSelectedRecipient(String name, String email) {
		if (!isRecipientAlreadyExists(email)) {
			DatabaseHandler dbHandler = dmApplication.getDatabaseHandler();
			dbHandler.insertRecipient(name, email);

		} else {

		}

	}

	/**
	 * method to set the listview with saved recipients
	 */
	private void setRecipientListView() {
		listAdapter = new RecipientAdapter(RecipientActivity.this,
				getRecipientsArrayList());
		recipientListView.setAdapter(listAdapter);
	}

	/**
	 * method to read currently selected recipient from shared preference
	 */
	private String readSelectedRecipientEmailFromPreference() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String email = pref.getString(Recipient.SELECTED_RECIPIENT_EMAIL_TAG,
				"");
		return email;
	}

	/**
	 * method to check whether the given recipient is currently selected
	 * recipient
	 * 
	 * @param searchkey
	 *            recipient to search
	 * @return returns true if the given recipient is currently selected one
	 */
	private boolean isThisSelectedRecipient(String searchkey) {

		String selectedRecipientEmail = readSelectedRecipientEmailFromPreference();
		if (searchkey.equalsIgnoreCase(selectedRecipientEmail)) {
			return true;
		}
		return false;
	}

	/**
	 * method used to retrieve the saved recipients from database and populate
	 * in a array list to set the adapter for listview
	 * 
	 * @return
	 */
	private ArrayList<Recipient> getRecipientsArrayList() {
		ArrayList<Recipient> recipientList = null;
		DatabaseHandler dbHandler = dmApplication.getDatabaseHandler();
		Cursor c = dbHandler.getAllRecipient();

		recipientList = new ArrayList<Recipient>();
		Recipient firstItem = new Recipient();
		firstItem.setName(getResources().getString(
				R.string.Follow_Server_Settings));
		firstItem.setEmail("");
		firstItem.setSelected(true);
		firstItem.setDeleteButtonVisible(false);
		recipientList.add(firstItem);

		if (c != null && c.getCount() >= 1) {

			c.moveToFirst();
			for (int index = 0; index < c.getCount(); index++) {
				Recipient recipient = new Recipient();
				String email = c.getString(c
						.getColumnIndex(Recipient.EMAIL_COLUMN));
				String name = c.getString(c
						.getColumnIndex(Recipient.NAME_COLUMN));

				recipient.setEmail(email);
				recipient.setName(name);
				recipient
						.setId(c.getInt(c.getColumnIndex(Recipient.ID_COLUMN)));
				recipient.setDeleteButtonVisible(false);

				// set the selection if the item is currently selected recipient
				if (isThisSelectedRecipient(email)) {
					recipient.setSelected(true);
					recipientList.get(0).setSelected(false);
				} else {
					recipient.setSelected(false);
				}

				recipientList.add(recipient);
				c.moveToNext();
			}
		}

		return recipientList;

	}

	@Override
	public void onBackPressed() {
		setActivityResult();
	}

	/**
	 * method used to set the data in an intent and return it as result for the
	 * called activity
	 */
	private void setActivityResult() {
		Bundle result = new Bundle();
		if (listAdapter.isRecipientUpdated()) {
			result.putBoolean("isEditAlreadySelectedItem", false);
			result.putBoolean("update", true);
			setRecipientForceUpdate(true);
		} else if (isEditAlreadySelectedItem || doRecipientForceUpdate()) {
			result.putBoolean("isEditAlreadySelectedItem", true);
			result.putBoolean("update", true);
		} else {
			result.putBoolean("isEditAlreadySelectedItem", false);
			result.putBoolean("update", false);
		}
		result.putBoolean("didAlreadySelectedRecipientDeleted",
				didAlreadySelectedRecipientDeleted);

		result.putString(Recipient.SELECTED_RECIPIENT_NAME_TAG, "");
		result.putString(Recipient.SELECTED_RECIPIENT_EMAIL_TAG,
				listAdapter.getSelectedRecipientEmail());

		Intent intent = new Intent();
		intent.putExtras(result);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * method used to check the state of force update. if the state is set to
	 * true, recipients are updated to ODP.
	 * 
	 * @return
	 */
	public boolean doRecipientForceUpdate() {

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean status = pref.getBoolean(Recipient.RECIPIENT_FORCE_UPDATE_TAG,
				false);
		return status;

	}

	class CustomGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					int x = Math.round(e1.getX());
					int y = Math.round(e1.getY());

					deletePosition = recipientListView.pointToPosition(x, y);
					if (listAdapter != null) {
						listAdapter
								.toggleDeleteVisibility(deletePosition, true);
					}

				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					// left to right
					int x = Math.round(e1.getX());
					int y = Math.round(e1.getY());

					deletePosition = recipientListView.pointToPosition(x, y);
					if (listAdapter != null) {
						listAdapter.toggleDeleteVisibility(deletePosition,
								false);

					}
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

	}

	@Override
	public void onRecipientDeleted() {
		updateListView();
	}

	/**
	 * method to refresh the listview
	 */
	private void updateListView() {
		if (listAdapter != null && recipientListView != null) {
			ArrayList<Recipient> itemList = listAdapter.getListArray();
			listAdapter = new RecipientAdapter(this, itemList);
			recipientListView.setAdapter(listAdapter);
		}
	}

}
