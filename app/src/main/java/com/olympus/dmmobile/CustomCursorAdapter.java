package com.olympus.dmmobile;

import java.io.File;
import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.olympus.dmmobile.recorder.DictateActivity;
import com.olympus.dmmobile.recorder.Utilities;

/**
 * CustomCursorAdapter is CursorAdapter which is customized with Layout.
 * This adapter is used with the Dictation ListViews in Pending, Outbox and Sent Tabs. 
 * Every operations related to ListView are handled in this class.
 * @version 1.0.1
 */
@SuppressLint("NewApi")
public class CustomCursorAdapter extends CursorAdapter implements OnClickListener {

	private int mSendOption = 0;
	private final String PREFS_NAME = "Config";
	private Button btnResend = null;
	private Button btnReview = null;
	private Button btnEditCopy = null;
	private Button btnCancel = null;
	private ViewHolder holder = null;
	private Activity mActivity = null;
	public ArrayList<Integer> mCheckList = null;
	private RecordingSelectedListener mListener = null;
	private AlertDialog.Builder mBuilder = null;
	private AlertDialog mAlertDialog = null;
	private ListItemAction mEditCopyListener = null;
	private DatabaseHandler mDbHandler;
	private LayoutInflater inflater;
	private DMApplication dmApplication = null;
	private Intent baseIntent = null;
	private File mFile = null;
	private View layoutView = null;
	private DictationCard dictationCard = null;
	private View listView = null;
	private String formattedDur = null;
	private SharedPreferences mSharedPreferences = null;

	/**
	 * Constructor of this class with two parameters.
	 * @param mActivity Activity reference
	 * @param cursor The cursor contains all data from DataBase
	 */
	public CustomCursorAdapter(Activity mActivity, Cursor cursor) {
		super(mActivity, cursor);
		this.mActivity = mActivity;
		this.mListener = (RecordingSelectedListener) mActivity;
		this.mEditCopyListener = (ListItemAction) mActivity;
		this.inflater = LayoutInflater.from(mActivity);
		this.mCheckList = new ArrayList<Integer>();
		dmApplication = (DMApplication) mActivity.getApplication();
		this.mDbHandler = dmApplication.getDatabaseHandler();
	}

	public void updateSendDeleteButtons(boolean state) {
		mListener.updateButtonState(state);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		listView = convertView;
		if (convertView == null) {
			listView = inflater.inflate(R.layout.tab_list_layout, parent, false);
			holder = new ViewHolder();
			holder.dictationName = (TextView) listView.findViewById(R.id.text_tab_list_dictation_name);
			holder.dictationWorktype = (TextView) listView.findViewById(R.id.text_tab_list_worktype);
			holder.dictationComment = (TextView) listView.findViewById(R.id.text_tab_list_comment);
			holder.dictationDate = (TextView) listView.findViewById(R.id.text_tab_list_dictation_date);
			holder.status = (TextView) listView.findViewById(R.id.text_tab_list_dictation_status);
			holder.checkToSend = (CheckBox) listView.findViewById(R.id.chkToSend);
			holder.priority = (ImageView) listView.findViewById(R.id.img_tab_list_priority);
			holder.editDictation = (ImageButton) listView.findViewById(R.id.img_tab_list_edit_dictation);
			holder.imgFlashAir = (ImageView) listView.findViewById(R.id.img_is_flashair);
			holder.pendingLayout = (RelativeLayout) listView.findViewById(R.id.relativeTabDictateDetails);
			holder.pendingLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View mView) {
					getCursor().moveToPosition((Integer) mView.getTag());
					dictationCard = mDbHandler.getSelectedDictation(getCursor());
					if (dictationCard.getIsFlashAir() == 0) {
						if(dmApplication.getTabPos() == 1 && dictationCard.isResend() == 1)
							return;
						int id = dictationCard.getSequenceNumber();
						dmApplication.lastDictMailSent = false;
						startNewDictateActivity(id);
					}
					else 
						onResendFlashAir();
				}
			});
			holder.checkToSend.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View mView) {
					int id = (Integer) mView.getTag();
					CheckBox chk = (CheckBox) mView;
					boolean isCheck = chk.isChecked();
					if (isCheck) 
						mCheckList.add(id);
					else 
						mCheckList.remove((Integer) id);
					mListener.onRecordingSelected(id, isCheck);
				}
			});
			holder.editDictation.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getCursor().moveToPosition((Integer) v.getTag());
					dictationCard = mDbHandler.getSelectedDictation(getCursor());
					if(dmApplication.getTabPos() == 1 && dictationCard.isResend() == 1)
						return;
					mFile = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
							+ dictationCard.getSequenceNumber() + "/"
							+ dictationCard.getDictFileName() + ".wav");
					if (mFile.exists()) {
						if (!(dictationCard.getDuration() < 1000)) {
							if (dmApplication.getTabPos() != 2) {
								onHideKeyBoard();
								baseIntent = new Intent(mActivity, DictationPropertyActivity.class);
								baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
								if(dmApplication.getTabPos() == 1 && dictationCard.isResend() == 1) {
//									baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_REVIEW_RECORDING);
								}
								else {
									if (dictationCard.getIsActive() == 1)
										baseIntent.putExtra(DMApplication.START_MODE_TAG, "");
									else
										baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_EDIT_RECORDING);
									DictationPropertyActivity.ComingFromRecordings = true;
								}
								baseIntent.putExtra(DMApplication.ACTIVITY_MODE_TAG, "dm");
								baseIntent.putExtra(DMApplication.DICTATION_ID, dictationCard.getDictationId());
								mActivity.startActivity(baseIntent);
							}
							else {
								dmApplication.setPropertyClicked(true);
								promptSentDictDialog();
							}
						} 
						else {
							DictationPropertyActivity.ComingFromRecordings = false;
							showSourceNotFoundToast();
						}
					} 
					else {
						showSourceNotFoundToast();
					}
					mFile = null;
					System.gc();
				}
			});
			holder.imgFlashAir.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View mView) {
					getCursor().moveToPosition((Integer) mView.getTag());
					dictationCard = mDbHandler.getSelectedDictation(getCursor());
					onResendFlashAir();
				}
			});
			listView.setTag(holder);
		}
		else
			holder = (ViewHolder) listView.getTag();
		Cursor cursor = getCursor();
		cursor.moveToPosition(position);
		holder.checkToSend.setTag(cursor.getInt(cursor.getColumnIndex(mDbHandler.SEQ_NUMBER)));
		holder.editDictation.setTag(getCursor().getPosition());
		holder.pendingLayout.setTag(getCursor().getPosition());
		holder.imgFlashAir.setTag(getCursor().getPosition());
		holder.dictationName.setText(cursor.getString(cursor.getColumnIndex(mDbHandler.DICTATION_NAME)));

		holder.dictationWorktype.setText(cursor.getString(cursor.getColumnIndex(mDbHandler.WORKTYPE)));
		holder.dictationComment.setText(cursor.getString(cursor.getColumnIndex(mDbHandler.COMMENT)));
		int prio = cursor.getInt(cursor.getColumnIndex(mDbHandler.PRIORITY));
		switch (prio) {
			case 0:
				holder.priority.setVisibility(View.INVISIBLE);
			break;
			case 1:
				holder.priority.setVisibility(View.VISIBLE);
			break;
		}
		if (mCheckList.contains(holder.checkToSend.getTag()))
			holder.checkToSend.setChecked(true);
		else
			holder.checkToSend.setChecked(false);

		int dictStat = cursor.getInt(cursor.getColumnIndex(mDbHandler.STATUS));
		formattedDur = Utilities.getDurationInTimerFormat(cursor.getLong(cursor
				.getColumnIndex(mDbHandler.DURATION)));
		switch (cursor.getInt(cursor.getColumnIndex(mDbHandler.ISFLASHAIR))) {
			case 0:
				holder.imgFlashAir.setVisibility(View.GONE);
				holder.editDictation.setVisibility(View.VISIBLE);
				if (dictStat == DictationStatus.SENT.getValue()
						|| dictStat == DictationStatus.SENT_VIA_EMAIL.getValue()) {
					holder.dictationDate.setTypeface(null, Typeface.BOLD);
					holder.dictationDate.setText(dmApplication.getLocalizedDateAndTime(cursor.getString(cursor
						 .getColumnIndex(mDbHandler.SENT_DATE))) + "   " + formattedDur);
				} else {
					holder.dictationDate.setText(dmApplication.getLocalizedDateAndTime(cursor.getString(cursor
						.getColumnIndex(mDbHandler.REC_END_DATE))) + "   " + formattedDur);
					holder.dictationDate.setTypeface(null, Typeface.NORMAL);
				}
				switch (dictStat) {
					case 18:
						holder.status.setText("");
						if (cursor.getInt(cursor.getColumnIndex(mDbHandler.IS_RESEND)) == 1)
							onEnableOrDisable(false);
						else
							onEnableOrDisable(true);
						if (dmApplication.isOnEditState() && dmApplication.getTabPos() == 1) {
							holder.checkToSend.setVisibility(View.INVISIBLE);
							holder.checkToSend.setChecked(false);
							int id = (Integer) holder.checkToSend.getTag();
							mCheckList.remove((Integer) id);
							mListener.onRecordingSelected(id, false);
						} else
							holder.checkToSend.setVisibility(View.VISIBLE);
					break;
					case 10:
						onChangeStatusUI(R.color.status_blue, R.string.Property_sending);
						onEnableOrDisable(false);
					break;
					case 9:
						onChangeStatusUI(R.color.status_blue, R.string.Property_sending);
						onEnableOrDisable(false);
					break;
					case 22:
						onChangeStatusUI(R.color.status_red, R.string.Property_Timeout);
						onEnableOrDisable(true);
					break;
					case 11:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Retrying);
						onEnableOrDisable(false);
					break;
					case 12:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Retrying);
						onEnableOrDisable(false);
					break;
					case 13:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Retrying);
						onEnableOrDisable(false);
					break;
					case 25:
						onChangeStatusUI(R.color.status_red, R.string.Property_Conversion_Failed);
						onEnableOrDisable(true);
					break;
					case 15:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Waiting_Send);
						onEnableOrDisable(false);
					break;
					case 16:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Waiting_Send);
						onEnableOrDisable(false);
					break;
					case 20:
						onChangeStatusUI(R.color.status_red, R.string.property_sending_failed);
						onEnableOrDisable(true);
					break;
					case 3:
						holder.status.setText(mActivity.getResources().getString(R.string.Property_Via_Email));
						onEnableOrDisable(true);
					break;
					default:
						holder.status.setText("");
						onEnableOrDisable(true);
					break;
				}
				if ((cursor.getInt(cursor.getColumnIndex(mDbHandler.ISACTIVE)) == 1 
						&& cursor.getInt(cursor.getColumnIndex(mDbHandler.IS_RESEND)) != 1)
						|| ((dictStat == DictationStatus.SENT.getValue() || dictStat == DictationStatus.SENT_VIA_EMAIL
								.getValue()) && !dmApplication.isEditMode())) {
					holder.checkToSend.setVisibility(View.INVISIBLE);
				}
			break;
			case 1:
				holder.imgFlashAir.setVisibility(View.VISIBLE);
				holder.editDictation.setVisibility(View.GONE);
				if (dictStat == DictationStatus.SENT.getValue() || dictStat == DictationStatus.SENT_VIA_EMAIL.getValue()) {
					holder.dictationDate.setText(dmApplication.getLocalizedDateAndTime(cursor.getString(cursor
									.getColumnIndex(mDbHandler.SENT_DATE))));
					holder.dictationDate.setTypeface(null, Typeface.BOLD);
				} else {
	
					holder.dictationDate.setText(dmApplication
							.getLocalizedDateAndTime(cursor.getString(cursor
									.getColumnIndex(mDbHandler.REC_END_DATE))));
					holder.dictationDate.setTypeface(null, Typeface.NORMAL);
				}
				switch (dictStat) {
					case 18:
						holder.status.setText("");
						if (dmApplication.isOnEditState()
								&& dmApplication.getTabPos() == 1) {
							holder.checkToSend.setVisibility(View.INVISIBLE);
							holder.checkToSend.setChecked(false);
							int id = (Integer) holder.checkToSend.getTag();
							mCheckList.remove((Integer) id);
							mListener.onRecordingSelected(id, false);
						} else
							holder.checkToSend.setVisibility(View.VISIBLE);
					break;
					case 10:
						onChangeStatusUI(R.color.status_blue, R.string.Property_sending);
						onEnableOrDisable(false);
					break;
					case 9:
						onChangeStatusUI(R.color.status_blue, R.string.Property_sending);
						onEnableOrDisable(false);
					break;
					case 22:
						onChangeStatusUI(R.color.status_red, R.string.Property_Timeout);
						holder.checkToSend.setVisibility(View.VISIBLE);
					break;
					case 11:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Retrying);
						onEnableOrDisable(false);
					break;
					case 12:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Retrying);
						onEnableOrDisable(false);
					break;
					case 13:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Retrying);
						onEnableOrDisable(false);
					break;
					case 25:
						onChangeStatusUI(R.color.status_red, R.string.Property_Conversion_Failed);
						holder.checkToSend.setVisibility(View.VISIBLE);
					break;
					case 15:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Waiting_Send);
						onEnableOrDisable(false);
					break;
					case 16:
						onChangeStatusUI(R.color.status_blue, R.string.Property_Waiting_Send);
						onEnableOrDisable(false);
					break;
					case 20:
						onChangeStatusUI(R.color.status_red, R.string.property_sending_failed);
						holder.checkToSend.setVisibility(View.VISIBLE);
					break;
					case 3:
						holder.status.setText(mActivity.getResources().getString(R.string.Property_Via_Email));
						onEnableOrDisable(false);
					break;
					case 2:
						holder.status.setText("");
						holder.checkToSend.setVisibility(View.INVISIBLE);
						holder.pendingLayout.setEnabled(true);
					break;
					default:
						holder.status.setText("");
						onEnableOrDisable(false);
					break;
				}
			break;
		}
		if (dmApplication.getTabPos() == 2 && dmApplication.isEditMode())
			holder.checkToSend.setVisibility(View.VISIBLE);
		return listView;
	}

	/**
	 * Enable or Disable views based on the input value
	 * @param isToEnable Input value to enable or disable as Boolean
	 */
	private void onEnableOrDisable(boolean isToEnable) {
		if (isToEnable) {
			holder.checkToSend.setVisibility(View.VISIBLE);
			holder.pendingLayout.setEnabled(true);
			holder.editDictation.setEnabled(true);
		} else {
			if (dmApplication.getTabPos() == 1) {
				holder.checkToSend.setChecked(false);
				int id = (Integer) holder.checkToSend.getTag();
				mCheckList.remove((Integer)id);
				mListener.onRecordingSelected(id, false);
			}
			holder.checkToSend.setVisibility(View.INVISIBLE);
			holder.pendingLayout.setEnabled(false);
			holder.editDictation.setEnabled(false);
		}
	}

	/**
	 * ViewHolder class used in Adapter to keep the Views used with the Listview.
	 *
	 */
	private class ViewHolder {
		TextView dictationName = null;
		TextView dictationDate = null;
		TextView dictationWorktype = null;
		TextView dictationComment = null;
		TextView status = null;
		CheckBox checkToSend = null;
		ImageView priority = null;
		RelativeLayout pendingLayout = null;
		ImageButton editDictation = null;
		ImageView imgFlashAir = null;
	}
	
	public void initCheckList() {
		mCheckList.clear();
	}
	/**
	 * Update active dictation status
	 */
	private void onUpdateActiveDictation() {
		Cursor cur = mDbHandler.checkActiveDictationExistsWithDuration();
		if (cur != null) {
			DictationCard card = mDbHandler.getSelectedDictation(cur);
			if (card.getStatus() == DictationStatus.SENT.getValue() 
					|| card.getStatus() == DictationStatus.SENT_VIA_EMAIL.getValue()
					|| card.isResend() == 1) {
				card.setIsActive(0);
				mDbHandler.updateAllIsActive();
			}
			else {
				card.setIsActive(0);
				card.setStatus(DictationStatus.PENDING.getValue());
				mDbHandler.updateStatusAndActive(card);
			}
			cur.close();
		}
	}

	/**
	 * Invoke DictateActivity when user tap on a Dictation.
	 * @param id Id of the tapped Dictation
	 */
	private void startNewDictateActivity(int id) {
		mFile = new File(DMApplication.DEFAULT_DIR + "/Dictations/" + dictationCard.getSequenceNumber() + "/"
				+ dictationCard.getDictFileName() + ".wav");
		if (mFile.exists()) {
			if (!(dictationCard.getDuration() < 1000)) {
				baseIntent = new Intent(mActivity, DictateActivity.class);
				baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				switch (dmApplication.getTabPos()) {
					case 0:
						onHideKeyBoard();
						onUpdateActiveDictation();
						dmApplication.flashair = false;
						if (dmApplication.newCreated) 
							dmApplication.newCreated = false;
						if (dictationCard.getIsActive() == 1)
							DictateActivity.IsAlreadyActive = true;
						else
							DictateActivity.IsAlreadyActive = false;
						dmApplication.setRecordingsClickedDMActivity(true);
						baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_EDIT_RECORDING);
						baseIntent.putExtra(DMApplication.DICTATION_ID, dictationCard.getDictationId());
						mActivity.startActivity(baseIntent);
						if (mCheckList.contains(id)) {
							mCheckList.remove((Integer) id);
							mListener.onRecordingSelected(id, false);
						}
					break;
					case 1:
						onUpdateActiveDictation();
						if(dictationCard.isResend() == 1) {
							dmApplication.setPropertyClicked(false);
							onStartReviewActivity();
						}
						else {
							onHideKeyBoard();
							dmApplication.flashair = false;
							if (dmApplication.newCreated) 
								dmApplication.newCreated = false;
							dictationCard.setStatus(DictationStatus.PENDING.getValue());
							mDbHandler.updateDictationStatus(dictationCard.getDictationId(), dictationCard.getStatus());
							baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_EDIT_RECORDING);
							baseIntent.putExtra(DMApplication.DICTATION_ID, dictationCard.getDictationId());
							dmApplication.outBoxFlag = true;
							mActivity.startActivity(baseIntent);
							if (mCheckList.contains(id)) {
								mCheckList.remove((Integer) id);
								mListener.onRecordingSelected(id, false);
							}
						}
					break;
					case 2:
						dmApplication.setPropertyClicked(false);
						promptSentDictDialog();
					break;
				}
			} 
			else 
				showSourceNotFoundToast();
		} 
		else 
			showSourceNotFoundToast();
		mFile = null;
		System.gc();
	}
	
	/**
	 * Shows Toast when file is not available in the Sdcard.
	 */
	private void showSourceNotFoundToast() {
		Toast.makeText(mActivity, mActivity.getResources().getString(R.string.SourceNotfound), Toast.LENGTH_SHORT).show();
		dictationCard.setIsActive(0);
		mDbHandler.updateIsActive(dictationCard);
		notifyDataSetChanged();
	}

	/**
	 * Shows Dialog when Tap on already Sent Dictation.
	 * Dialog has Review, EditCopy and Cancel options.
	 */
	private void promptSentDictDialog() {
		if(mAlertDialog!=null && mAlertDialog.isShowing())
			return;
		if(dictationCard.getIsFlashAir() != 1) {
			mFile = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
					+ dictationCard.getSequenceNumber() + "/"
					+ dictationCard.getDictFileName() + ".wav");
			if (mFile.exists()) {
				inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				mBuilder = new AlertDialog.Builder(mActivity);
				mBuilder.setCancelable(false);
				mBuilder.setTitle(mActivity.getResources().getString(
						R.string.Recording_Alerts_Sent_Dictation));
				mBuilder.setMessage(mActivity.getResources().getString(
						R.string.Recording_Alerts_Do_Sent_Dictation_Message));
				mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
				mSendOption = Integer.parseInt( mSharedPreferences.getString(mActivity.getString(R.string.send_key), "1"));
				if(dictationCard.getStatus() == 3 || mSendOption == 1) 
					layoutView = inflater.inflate(R.layout.dialog_send_default_view, (ViewGroup) mActivity.findViewById(R.id.view_send_dialog));
				else {
					layoutView = inflater.inflate(R.layout.dialog_send, (ViewGroup) mActivity.findViewById(R.id.view_send_dialog));
					btnResend = (Button) layoutView.findViewById(R.id.btn_resend);
					btnResend.setOnClickListener(this);
				}
				btnReview = (Button) layoutView
						.findViewById(R.id.btn_send_dialog_send);
				btnEditCopy = (Button) layoutView
						.findViewById(R.id.btn_send_dialog_pending);
				btnCancel = (Button) layoutView
						.findViewById(R.id.btn_send_dialog_cancel);
				btnReview.setText(mActivity.getResources().getString(
						R.string.Recording_Alerts_Review));
				btnEditCopy.setText(mActivity.getResources().getString(
						R.string.Recording_Alerts_Edit_Copy));
				btnReview.setOnClickListener(this);
				btnEditCopy.setOnClickListener(this);
				btnCancel.setOnClickListener(this);
				mBuilder.setView(layoutView);
				mAlertDialog = mBuilder.create();
				mAlertDialog.show();
			} 
			else
				Toast.makeText(mActivity, mActivity.getResources().getString(R.string.SourceNotfound),
						Toast.LENGTH_SHORT).show();
			mFile = null;
		}
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return null;
	}
	
	@Override
	public void onClick(View mView) {
		switch (mView.getId()) {
			case R.id.btn_resend:
				mAlertDialog.dismiss();
				if(onCheckActivation()) 
					onTakeAction(true);
			break;
			case R.id.btn_send_dialog_pending:
				onTakeAction(false);
			break;
			case R.id.btn_send_dialog_send:
				onUpdateActiveDictation();
				onStartReviewActivity();
				mAlertDialog.dismiss();
			break;
			case R.id.btn_send_dialog_cancel:
				mAlertDialog.dismiss();
			break;
		}
	}
	/**
	 * Invokes Review view when user chose review option.
	 */
	private void onStartReviewActivity() {
		onHideKeyBoard();
		dmApplication.flashair = false;
		Cursor cur = mDbHandler.checkActiveDictationExists();
		if (cur != null) 
			mDbHandler.updateAllIsActive();
		dmApplication.lastDictMailSent = false;
		if (dmApplication.newCreated) 
			dmApplication.newCreated = false;
		cur = null;
		baseIntent = new Intent(mActivity, DictateActivity.class);
		baseIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		baseIntent.putExtra(DMApplication.START_MODE_TAG, DMApplication.MODE_REVIEW_RECORDING);
		baseIntent.putExtra(DMApplication.DICTATION_ID, dictationCard.getDictationId());
		mActivity.startActivity(baseIntent);
	}
	/**
	 * Check available disk space for Re-send/edit copy action
	 * @param isResend
	 */
	private void onTakeAction(boolean isResend) {
		long fileSize = 0;
		File file = null;
		if(dictationCard.getIsFlashAir() == 1) {
			file = new File(DMApplication.DEFAULT_DIR + DMApplication.DEFAULT_DICTATIONS_DIR + dictationCard.getDictationId() + "/"
					+ dictationCard.getDictFileName() + "." +  DMApplication.getDssType(dictationCard.getDssVersion()));
			if (file.exists()) 
				fileSize = file.length();
		}
		else {
			file = new File(DMApplication.DEFAULT_DIR + DMApplication.DEFAULT_DICTATIONS_DIR + dictationCard.getDictationId() + "/"
					+ dictationCard.getDictFileName() + ".wav");
			if (file.exists()) {
				fileSize = file.length();
				if(dictationCard.getIsThumbnailAvailable() == 1) {
					file = new File(DMApplication.DEFAULT_DIR + DMApplication.DEFAULT_DICTATIONS_DIR + dictationCard.getDictationId() + "/"
							+ dictationCard.getDictFileName() + ".jpg");
					if (file.exists())
						fileSize = fileSize + file.length();
				}
			}
		}
		fileSize = fileSize + dmApplication.MINIMUM_SIZE_REQUIRED;
		if (dmApplication.getAvailableDiskSpace() > fileSize) {
			dmApplication.lastDictMailSent = false;
			if (dmApplication.newCreated) 
				dmApplication.newCreated = false;
			if(isResend)
				mEditCopyListener.onItemSeleted(1, dictationCard);
			else
				mEditCopyListener.onItemSeleted(2, dictationCard);
			mAlertDialog.dismiss();
		}
		else {
			mAlertDialog.dismiss();
			mAlertDialog = new AlertDialog.Builder(mActivity).create();
			mAlertDialog.setTitle(mActivity.getResources().getString(R.string.Dictate_No_Space));
			mAlertDialog.setMessage(mActivity.getResources().getString(R.string.Dictate_Low_Memory));
			mAlertDialog.setButton(mActivity.getResources().getString(R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
			});
			mAlertDialog.show();
		}
		file = null;
	}
	/**
	 * Method to update real time status changes.
	 * @param mColor color of specific status
	 * @param mIdString status string
	 */
	private void onChangeStatusUI(int mColor, int mIdString) {
		holder.status.setTextColor(mActivity.getResources().getColor(mColor));
		holder.status.setText(mActivity.getResources().getString(mIdString));
	}
	/**
	 * Invokes re-send flash air view
	 */
	private void onResendFlashAir() {
		if(mAlertDialog != null && mAlertDialog.isShowing())
			return;
		if(dictationCard.getStatus() == 2) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
			if(Integer.parseInt( mSharedPreferences.getString(mActivity.getString(R.string.send_key), "1"))==1)
				return;
			mFile = new File(DMApplication.DEFAULT_DIR + "/Dictations/" + dictationCard.getSequenceNumber() + "/"
					+ dictationCard.getDictFileName() + "." + DMApplication.getDssType(dictationCard.getDssVersion()));
			if (mFile.exists()) {
				mBuilder = new AlertDialog.Builder(mActivity);
				mBuilder.setCancelable(false);
				mBuilder.setTitle(mActivity.getResources().getString(R.string.Recording_Alerts_Sent_Dictation));
				mBuilder.setMessage(mActivity.getResources().getString(R.string.Recording_Alerts_Do_Sent_Dictation_Message));
				mBuilder.setPositiveButton(
						mActivity.getString(R.string.resend),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								if(onCheckActivation())
									onTakeAction(true);
							}
						});
				mBuilder.setNegativeButton(mActivity.getString(R.string.Recording_Alerts_Cancel),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
				});
				mAlertDialog = mBuilder.create();
				mAlertDialog.show();
			} 
			else
				Toast.makeText(mActivity, mActivity.getResources().getString(R.string.SourceNotfound),
						Toast.LENGTH_SHORT).show();
			mFile = null;
		}
	}
	/**
	 * To check application is activated/not.
	 * @return isActivated
	 */
	private boolean onCheckActivation() {
		mSharedPreferences = mActivity.getSharedPreferences(PREFS_NAME, 0);
		if(mSharedPreferences.getString("Activation", "").equalsIgnoreCase("Not Activated")) {
			mBuilder = new AlertDialog.Builder(mActivity);
			mBuilder.setTitle(mActivity.getString(R.string.Ils_Result_Not_Activated));
			mBuilder.setMessage(mActivity.getString(R.string.Flashair_Alert_Activate_Account));
			mBuilder.setPositiveButton(mActivity.getString(R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			mBuilder.create().show();
			return false;
		}
		return true;
	}
	/**
	 * Hide keyboard when user navigates to another view.
	 */
	private void onHideKeyBoard() {
		((InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
				mActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}
}
