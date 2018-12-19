package com.olympus.dmmobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.recorder.DictateActivity;
import com.olympus.dmmobile.recorder.Utilities;
import com.olympus.dmmobile.settings.SettingsActivity;

/**
 * DictationPropertyActivity class is used to manage Dictation properties like Dictation Name,
 * Worktype, Priority, Author Thumbnail ,Comments, Dictation status, Modified Time and 
 * Sent Time .
 * 
 * @version 1.0.1
 */
public class DictationPropertyActivity extends FragmentActivity implements  ActivityCompat.OnRequestPermissionsResultCallback{
	
	private ExceptionReporter mReporter;			//Error Logger
	private Button mEdit;
	private EditText mEditDictationname;
	private TextView mWorktype;
	private TextView mTxtDictationname;
	private AlertDialog.Builder mBuilder=null;
	private AlertDialog mAlertDialog=null;
	private int flag=0;
	private ImageView mAuthorphoto;
	private static final int SELECT_PHOTO = 100;
	private AlertDialog.Builder alertDialog;
	private LinearLayout mLinaerlayout;
	private RelativeLayout mShakeTwiceRelative;
	private RelativeLayout mLayoutRoot;
	private Button mTakephoto;
    private Button mChoosePhoto;
    private Button mCancel;
    private Button mDeletephoto;
    private CheckBox mPrioritycheck;
    private ImageView mNewDictate;
    private TextView mTxtDateAndTime;
    private TextView mTxtJobNumber;
    private TextView mTxtStatus;
    private EditText mEditComments;
    private int mPriority;
    private DictationCard mDictCard;
    private DatabaseHandler mDBHandler;
    private String mCheckServermail;
    public static boolean ComingFromRecordings=true;
	private String mMailworktype;
	private String mServerWorktype;
	private ArrayList<String> mListValues;
	private String[] mWorktypes;
	private final int CAMERA_IMAGE_CAPTURE = 0;
	private boolean isOnceWindowFocusChanged=false;
	private File mImageFile,sdCard=null,sourceFile=null,destinationFile=null;
	private ShakeListener mShaker;	// shake twice
	private boolean isShakable;
	private String mPassedMode;
	private String mActivityMode;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	private AlertDialog.Builder alert;
	private String mSelectedWorktype = "";
	private Locale locale;
	
	private DMApplication dmApplication=null;
	private Bitmap bitmap;
	private boolean photoFlag = false;
	private final String TEMP_IMAGE = "TempImage";
	private boolean hasExceptionInImage=false;
	private String outputPath=null,imageInputPath=null;
	private FileChannel srcChannel=null;
	private FileChannel dstChannel=null;
	private ManageChooseImage manageChooseImage=null;
	private Cursor imageCursor=null;
	private boolean isKeyboardShown = false;
	boolean isSofyKeyBoardshown = false;
	private AlertDialog dialog;
	private boolean firstTouch = false;
	private int initialImgFlag = 0;
	
	private ImageButton mBtnSettingsOption;
	private boolean isCamPermissionGrnated=false;

	@Override
	protected void onCreate(Bundle savedInstanceState){
    	mReporter = ExceptionReporter.register(this);
    	super.onCreate(savedInstanceState);
    	if(android.os.Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	setContentView(R.layout.activity_dictation_property);
    	
    	dmApplication=(DMApplication)getApplication();
    	dmApplication.setContext(DictationPropertyActivity.this);
    	setCurrentLanguage(dmApplication.getCurrentLanguage());
    	mDBHandler =dmApplication.getDatabaseHandler();
    	Bundle b = getIntent().getExtras();
    	mActivityMode = b.getString(DMApplication.ACTIVITY_MODE_TAG);
    	mPassedMode = b.getString(DMApplication.START_MODE_TAG);
    	mDictCard = mDBHandler.getDictationCardWithId(b.getInt(DMApplication.DICTATION_ID));
    	mDictCard.setFilesList(mDBHandler.getFileList(mDictCard.getDictationId()));
    	
    	mLayoutRoot = (RelativeLayout)findViewById(R.id.property_activity_root_view);
		mEdit=(Button)findViewById(R.id.btn_property_edit);
		mEditDictationname=(EditText)findViewById(R.id.edittext_property_dictation_name);
		mAuthorphoto=(ImageView)findViewById(R.id.image_property_camera);
		mWorktype=(TextView)findViewById(R.id.text_property_worktype);
		mPrioritycheck=(CheckBox)findViewById(R.id.check_property_priority);
		mTxtDictationname=(TextView)findViewById(R.id.text_property_dictation_name);
		mNewDictate=(ImageView) findViewById(R.id.image_property_new_dictation);
		mTxtDateAndTime = (TextView) findViewById(R.id.text_property_dictate_details);
		mTxtJobNumber = (TextView) findViewById(R.id.text_property_jobnumber);
		mTxtStatus = (TextView) findViewById(R.id.text_property_status);
		mEditComments = (EditText) findViewById(R.id.edit_property_comment);
		mShakeTwiceRelative = (RelativeLayout) findViewById(R.id.relative_property_shake_twice_layout);
		mBtnSettingsOption = (ImageButton)findViewById(R.id.imgBtn_property_SettingsOption);
		mBtnSettingsOption.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				promtSettings();
			}
		});
		InputMethodManager imm = 
		        (InputMethodManager)DictationPropertyActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
		    imm.hideSoftInputFromWindow(mEditDictationname.getWindowToken(), 0);
		mEditComments.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				 if (view.getId() ==R.id.edit_property_comment) {
		                view.getParent().requestDisallowInterceptTouchEvent(true);
		                switch (event.getAction()&MotionEvent.ACTION_MASK){
		                case MotionEvent.ACTION_UP:
		                    view.getParent().requestDisallowInterceptTouchEvent(false);
		                    break;
		                }
		            }
				return false;
			}
		    });
		mEditDictationname.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mEditDictationname.requestFocus();
				mEditDictationname.setCursorVisible(true);
				firstTouch = true;
				return false;
			}
		});
		
		mEditComments.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mEditComments.requestFocus();
				mEditComments.setCursorVisible(true);
				return false;
			}
		});
		mEditDictationname.setText(mDictCard.getDictationName());
		mTxtDictationname.setText(mDictCard.getDictationName());
		mEditComments.setText(mDictCard.getComments());
		if( mDictCard.getWorktype()==null || mDictCard.getWorktype().equals("") )
			mWorktype.setText(getResources().getString(R.string.Settings_Select));
		else
			mWorktype.setText(mDictCard.getWorktype());
		mPriority = mDictCard.getPriority();
		if(mDictCard.getIsThumbnailAvailable()==1)
		{
			flag=1;
			initialImgFlag = flag;
		}
		else{
			flag=0;
			initialImgFlag = flag;
		}
		autoResize(mEdit);
		if(mPriority == 0)
			mPrioritycheck.setChecked(false);
		else if(mPriority == 1)
			mPrioritycheck.setChecked(true);
		long duration = mDictCard.getDuration();
		String formattedDur = Utilities.getDurationInTimerFormat(duration);
		int status = mDictCard.getStatus();
		mTxtStatus.setText(getStatusInString(status));
		if(status == DictationStatus.SENT.getValue()|| status == DictationStatus.SENT_VIA_EMAIL.getValue())
			mTxtDateAndTime.setText(dmApplication.getLocalizedDateAndTime(mDictCard.getRecEndDate())+"  "+formattedDur);
		/*else if(status == -1){
			mTxtDateAndTime.setText(formattedDur);
		}*/else{
			if(!mDictCard.getRecEndDate().equals("") ){
				mTxtDateAndTime.setText(dmApplication.getLocalizedDateAndTime(mDictCard.getRecEndDate())+"  "+formattedDur);
			}else{
				mTxtDateAndTime.setText(formattedDur);
			}
		}
		mTxtJobNumber.setText("");
		if(mDictCard.getFilesList()!= null )
			if(mDictCard.getFilesList().size()>0)
				if(mDictCard.getFilesList().get(0).getJobNumber()!=null)
					mTxtJobNumber.setText(String.valueOf(mDictCard.getFilesList().get(0).getJobNumber()));
	
	    mWorktype.setEnabled(false);
	    mPrioritycheck.setEnabled(false);
	    mEditComments.setEnabled(false);
	    mAuthorphoto.setEnabled(false);
	    mEdit.setTag("2"); 
	    mNewDictate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				
				sharedPreferences = PreferenceManager.getDefaultSharedPreferences(DictationPropertyActivity.this);
				editor = sharedPreferences.edit();
				editor.putBoolean(DictateActivity.WAS_DESTROYED, false);
				editor.commit();
				
				if (mDictCard.getStatus()==DictationStatus.NEW.getValue() && mDictCard.getDuration() > 0 && (mDictCard.getStatus()!=DictationStatus.SENT.getValue() || mDictCard.getStatus()!=DictationStatus.SENT_VIA_EMAIL.getValue())) {
					mDictCard.setStatus(DictationStatus.PENDING.getValue());
					mDBHandler.updateDictationStatus(mDictCard.getDictationId(),
							mDictCard.getStatus());
				}
				if(dmApplication.newCreated){
					dmApplication.newCreated = false;
				}
				dmApplication.fromWhere = 4;
				dmApplication.isExecuted = true;
				dmApplication.flashair = false;
				Intent intent = new Intent(DictationPropertyActivity.this, DictateActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				intent.putExtra(DMApplication.START_MODE_TAG,
						DMApplication.MODE_NEW_RECORDING);
				dmApplication.passMode = DMApplication.MODE_NEW_RECORDING;
				startActivity(intent);
				System.gc();
				finish();			
			}
		});
	    
		mAuthorphoto.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				sharedPreferences = getSharedPreferences("Campermissions", MODE_PRIVATE);
				isCamPermissionGrnated = sharedPreferences.getBoolean("camperm", false);
				if ( checkCameraPermission()&&isCamPermissionGrnated)
				{	String title = mEditDictationname.getText().toString().trim();
				boolean isValid = false;
				if (!title.equalsIgnoreCase(mDictCard.getDictationName())) {
					isValid = validateDictationName(title);
				} else {
					isValid = true;
				}
				if (isValid) {
					mEditDictationname.setText(title);
					mEditDictationname.setSelection(title.length());
					promptImageSelection();
				}
			}
			}
		});
	    mWorktype.setOnClickListener(new OnClickListener(){			
			@Override
			public void onClick(View v){
				String title = mEditDictationname.getText().toString().trim();
				boolean isValid = false;
				if (!title.equalsIgnoreCase(mDictCard.getDictationName())) {
					isValid = validateDictationName(title);
				}else{
					isValid = true;
				}
				if(isValid){
					mEditDictationname.setText(title);
					mEditDictationname.setSelection(title.length());
					promptWorktypeList();	
				}
			}
		});
	    mEdit.setOnClickListener(new OnClickListener(){			
			@Override
			public void onClick(View arg0){
//				if(mEdit.getText().toString().trim().equalsIgnoreCase(getResources().getString (R.string.Property_Edit))){
				//if(mEdit==null)
					
				if(mEdit.getTag().toString().trim().equalsIgnoreCase("2")){
					mEdit.setTag("1");
					mEdit.setText(getResources().getString(R.string.Property_Done));
					mWorktype.setEnabled(true);	
					autoResize(mEdit);
					mPrioritycheck.setEnabled(true);
					mEditComments.setEnabled(true);
					/*mEditComments.setBackgroundColor(getResources().getColor(android.R.color.white));
					mEditComments.setTextColor(getResources().getColor(android.R.color.black));*/
					mEditComments.setBackgroundResource(R.drawable.comment_bg_selector);
					
					mAuthorphoto.setEnabled(true);
					mTxtDictationname.setVisibility(View.INVISIBLE);
					mEditDictationname.setVisibility(View.VISIBLE);
					mEditDictationname.setText(mTxtDictationname.getText().toString());			
				}
//				else if(mEdit.getText().toString().trim().equalsIgnoreCase(getResources().getString (R.string.Property_Done))){
//				mEdit.setText(getResources().getString(R.string.Property_Edit));
				else if(mEdit.getTag().toString().trim().equalsIgnoreCase("1")){
					mDBHandler.updateWholeFileTransferId(mDictCard.getDictationId());
					if (!mEditDictationname.getText().toString().trim().equalsIgnoreCase(mDictCard.getDictationName())) {
						if(!mEditDictationname.getText().toString().trim().equals("")){
							boolean exists = mDBHandler.checkDictationNameExists(mEditDictationname.getText().toString().trim());
							if (exists) {
								mEditDictationname.setText(mDictCard.getDictationName());
								mTxtDictationname.setText(mDictCard.getDictationName());	
								alert = new AlertDialog.Builder(DictationPropertyActivity.this);
								alert.setCancelable(false);
								alert.setTitle(getResources().getString(
										R.string.Dictate_Alert_name_exists));
								alert.setMessage(getResources().getString(
										R.string.Dictate_Alert_Already_Exists_Message));
								alert.setPositiveButton(
										getResources().getString(
												R.string.Dictate_Alert_Ok),
										new DialogInterface.OnClickListener() {
		
											@Override
											public void onClick(DialogInterface dia,
													int which) {
												dia.dismiss();
												onEditDone();
												alert = null;
												dialog = null;
										}
										});
								if(dialog==null)
									dialog = alert.create();
								if(!dialog.isShowing())
									dialog.show();
						
							}else{
								onEditDone();
							}
							
						}else{
							alert = new AlertDialog.Builder(DictationPropertyActivity.this);
							alert.setCancelable(false);
							alert.setTitle(getResources().getString(
									R.string.Alert));
							alert.setMessage(getResources().getString(
									R.string.Property_enter_valid_dict_name));
							alert.setPositiveButton(
									getResources().getString(
											R.string.Dictate_Alert_Ok),
									new DialogInterface.OnClickListener() {
	
										@Override
										public void onClick(DialogInterface dia,
												int which) {
											mEditDictationname.setText(mDictCard.getDictationName());
											dia.dismiss();
											onEditDone();
											alert = null;
											dialog = null;
										}
									});
							if(dialog==null)
								dialog = alert.create();
							if(!dialog.isShowing())
								dialog.show();
						}	
					}else{
						onEditDone();
					}
				}
			}
		});	
	    mPrioritycheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if(isChecked)
					mDictCard.setPriority(1);
				else
					mDictCard.setPriority(0);
			}
		});
	    isShakable=true;
	    mShakeTwiceRelative.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//if (!mPassedMode.equalsIgnoreCase(DMApplication.MODE_REVIEW_RECORDING)) {
				mDictCard = mDBHandler.getDictationCardWithId(mDictCard.getDictationId());
				String title = mEditDictationname.getText().toString().trim();
				boolean isValid = false;
				
				if (!title.equalsIgnoreCase(mDictCard.getDictationName())) {
					isValid = validateDictationName(title);
				}else
					isValid = true;
				if (mDictCard.getStatus() == DictationStatus.NEW.getValue() || mDictCard.getStatus() == DictationStatus.OUTBOX.getValue() ||
						mDictCard.getStatus() == DictationStatus.CONVERTION_FAILED.getValue() ||mDictCard.getStatus() == DictationStatus.SENDING_FAILED.getValue() ||
						mDictCard.getStatus() == DictationStatus.TIMEOUT.getValue() ||mDictCard.getStatus() == DictationStatus.PENDING.getValue() ||
						mDictCard.getStatus() == DictationStatus.SENT.getValue() ||mDictCard.getStatus() == DictationStatus.SENT_VIA_EMAIL.getValue()) {
					if (isValid) {
						if (mDictCard.getIsActive() == 1)
							DictateActivity.IsAlreadyActive = true;
						else
							DictateActivity.IsAlreadyActive = false;
						mEditDictationname.setText(title);
						mEditDictationname.setSelection(title.length());
						if ((mDictCard.getStatus() == DictationStatus.SENDING_FAILED
								.getValue() || mDictCard.getStatus() == DictationStatus.TIMEOUT.getValue()
								|| mDictCard.getStatus() == DictationStatus.CONVERTION_FAILED.getValue()) && mDictCard.isResend() != 1)
							mDBHandler.updateDictationStatus(mDictCard.getDictationId(), DictationStatus.PENDING.getValue());
						DictateActivity.fromShake = true;
						Cursor cur = mDBHandler.checkActiveDictationExists();
						if (cur != null) {
							DictationCard card = mDBHandler
									.getSelectedDictation(cur);
							if (!card.getDictationName().equalsIgnoreCase(
									mDictCard.getDictationName())) {
								card.setIsActive(0);
								mDBHandler.updateIsActive(card);
							}
							cur.close();
						}
						discardImageChange();
						if (ComingFromRecordings) {
							mPassedMode = DMApplication.MODE_EDIT_RECORDING;
						}
						dmApplication.lastDictMailSent = false;
						if(dmApplication.newCreated){
							dmApplication.newCreated = false;
						}
						dmApplication.flashair = false;
						Intent intent = new Intent(DictationPropertyActivity.this,
								DictateActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						intent.putExtra(DMApplication.START_MODE_TAG,
								mPassedMode);
						dmApplication.passMode = mPassedMode;
						/*intent.putParcelableArrayListExtra(
								DMApplication.DICTATION_CARD_KEY, dCardList);*/
						intent.putExtra(DMApplication.DICTATION_ID,
								mDictCard.getDictationId());

						startActivity(intent);
						if(dmApplication.getDeletedId()==0)
							dmApplication.setDeletedId(mDictCard.getDictationId());
						System.gc();
						finish(); // finish dictate property activity
						//}
					}
				}else{
					finish();
				}
			}
		}) ;
	    mShaker = new ShakeListener(this);
	    
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener () 
        {
          public void onShake()
          {
        	  mDictCard = mDBHandler.getDictationCardWithId(mDictCard.getDictationId());
				if (mDictCard.getStatus() == DictationStatus.NEW.getValue()
						|| mDictCard.getStatus() == DictationStatus.OUTBOX
								.getValue()
						|| mDictCard.getStatus() == DictationStatus.CONVERTION_FAILED
								.getValue()
						|| mDictCard.getStatus() == DictationStatus.SENDING_FAILED
								.getValue()
						|| mDictCard.getStatus() == DictationStatus.TIMEOUT
								.getValue()
						|| mDictCard.getStatus() == DictationStatus.PENDING
								.getValue()
						|| mDictCard.getStatus() == DictationStatus.SENT
								.getValue()
						|| mDictCard.getStatus() == DictationStatus.SENT_VIA_EMAIL
								.getValue()) {
					if (mDictCard.getIsActive() == 1) {
						DictateActivity.IsAlreadyActive = true;
					} else
						DictateActivity.IsAlreadyActive = false;
					// if(isShakable &&
					// !mPassedMode.equalsIgnoreCase(DMApplication.MODE_REVIEW_RECORDING)){
					if ((mDictCard.getStatus() == DictationStatus.SENDING_FAILED.getValue() || mDictCard.getStatus() == DictationStatus.TIMEOUT.getValue()
							|| mDictCard.getStatus() == DictationStatus.CONVERTION_FAILED.getValue()) && mDictCard.isResend() != 1)
						mDBHandler.updateDictationStatus(mDictCard.getDictationId(), DictationStatus.PENDING.getValue());
					Cursor cur = mDBHandler.checkActiveDictationExists();
					if (cur != null) {
						DictationCard card = mDBHandler.getSelectedDictation(cur);
						if (!card.getDictationName().equalsIgnoreCase(mDictCard.getDictationName())) {
							card.setIsActive(0);
							mDBHandler.updateIsActive(card);
						}
						cur.close();
					}
					discardImageChange();
					/*
					 * ArrayList<DictationCard> dCardList = new
					 * ArrayList<DictationCard>(); dCardList.add(mDictCard);
					 */
					DictateActivity.fromShake = true;
					if(dmApplication.newCreated){
						dmApplication.newCreated = false;
					}
					dmApplication.flashair = false;
					Intent intent = new Intent(DictationPropertyActivity.this,
							DictateActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(DMApplication.START_MODE_TAG, mPassedMode);
					if (ComingFromRecordings) {
						mPassedMode = DMApplication.MODE_EDIT_RECORDING;
					}
					dmApplication.lastDictMailSent = false;
					/*
					 * intent.putParcelableArrayListExtra(
					 * DMApplication.DICTATION_CARD_KEY, dCardList);
					 */
					intent.putExtra(DMApplication.DICTATION_ID,
							mDictCard.getDictationId());
					startActivity(intent);
					if(dmApplication.getDeletedId()==0)
						dmApplication.setDeletedId(mDictCard.getDictationId());
					isShakable = false;
					System.gc();
					finish(); // finish dictate property activity
				}else{
					finish();
				}
			}
        });
        mShaker.resume();
        mEditDictationname.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String dictName= mEditDictationname.getText().toString().trim();
				if (actionId == EditorInfo.IME_ACTION_DONE &&  !dictName.equalsIgnoreCase(mDictCard.getDictationName())) {
					validateDictationName(dictName);
					
				}else{
					mEditDictationname.setText(dictName);
					mEditDictationname.setCursorVisible(false);
					//mEditDictationname.clearFocus();
				}
				InputMethodManager imm = 
				        (InputMethodManager)DictationPropertyActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
				    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				return false;
			}
		});
     
        mLayoutRoot.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
		    Rect r = new Rect();
		    //r will be populated with the coordinates of your view that area still visible.
		    mLayoutRoot.getWindowVisibleDisplayFrame(r);
		    int heightDiff = mLayoutRoot.getRootView().getHeight() - (r.bottom - r.top);
		    if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
		        isKeyboardShown = true;
		        mEditDictationname.setCursorVisible(true);
		        if(!firstTouch)
		        	mEditDictationname.setSelection(mEditDictationname.getText().length());
		        else
		        	firstTouch = false;
		    }
		    else{
		    	if(isKeyboardShown){
		    		
					String title = mEditDictationname.getText().toString().trim();
					if (!title.equalsIgnoreCase(mDictCard.getDictationName())) {
						validateDictationName(title);
					}else{
						if(mEditDictationname.hasFocus()){
							mEditDictationname.setText(title);
							mEditDictationname.clearFocus();
							mEditDictationname.setCursorVisible(false);
						}
					}
					if(mEditComments.hasFocus()){
						mEditComments.clearFocus();
						mEditComments.setCursorVisible(false);
					}
		    		isKeyboardShown = false;
		    	}
		    }
		 }
		});
        
	}
    
 	/**
 	 * Validates Dictation name which is entered by the user.
 	 * @param dictName User entered Dictation name.
 	 * @return True if valid or False if not valid.
 	 */
 	private boolean validateDictationName(String dictName){
		if( !dictName.equals("")){
			boolean exists = mDBHandler.checkDictationNameExists(
					dictName);
			
			if (exists) {
				mEditDictationname.setText(mDictCard.getDictationName());
				//mEditDictationname.clearFocus();
				alert = new AlertDialog.Builder(DictationPropertyActivity.this);
				alert.setCancelable(false);
				alert.setTitle(getResources().getString(
						R.string.Dictate_Alert_name_exists));
				alert.setMessage(getResources().getString(
						R.string.Dictate_Alert_Already_Exists_Message));
				alert.setPositiveButton(
						getResources().getString(
								R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dia,
									int which) {
								dia.dismiss();
								mEditDictationname.setCursorVisible(false);
								dialog = null;
							}
						});
				if(dialog==null)
					dialog = alert.create();
				if(!dialog.isShowing())
					dialog.show();
				return false;
			}else
				return true;
				
		}else{
			alert = new AlertDialog.Builder(DictationPropertyActivity.this);
			alert.setCancelable(false);
			alert.setTitle(getResources().getString(
					R.string.Alert));
			alert.setMessage(getResources().getString(
					R.string.Property_enter_valid_dict_name));
			alert.setPositiveButton(
					getResources().getString(
							R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dia,
								int which) {
							mEditDictationname.setText(mDictCard.getDictationName());
							mEditDictationname.setCursorVisible(false);
							//mEditDictationname.clearFocus();
							dia.dismiss();
							dialog = null;
						}
					});
			if(dialog==null)
				dialog = alert.create();
			if(!dialog.isShowing())
				dialog.show();
			return false;
		}
	}
   
   
 /**
 * Invokes when done button is tapped. Views will be updated.
 */
public void onEditDone(){
	    mEdit.setTag("2");
		mEdit.setText(getResources().getString(R.string.Property_Edit));
		mWorktype.setEnabled(false);
		mPrioritycheck.setEnabled(false);
		mEditComments.setEnabled(false);
		/*mEditComments.setBackgroundResource(R.drawable.comment_bg_selector);
		mEditComments.setTextColor(getResources().getColor(android.R.color.white));*/
		mEditComments.setBackgroundResource(android.R.color.transparent);
		mAuthorphoto.setEnabled(false);
		autoResize(mEdit);
		mTxtDictationname.setVisibility(View.VISIBLE);
		mEditDictationname.setVisibility(View.INVISIBLE);
		String wtype = mWorktype.getText().toString().trim();
		if(wtype.equalsIgnoreCase(getResources().getString(R.string.Settings_Select)))
			mDictCard.setWorktype("");
		else
			mDictCard.setWorktype(wtype);
		mDictCard.setComments(mEditComments.getText().toString());
		mDictCard.setDictationName(mEditDictationname.getText().toString().trim());
		mDBHandler.updateDictationProperty(mDictCard);
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mEditDictationname.getWindowToken(), 0);
		mEditDictationname.setText(mDictCard.getDictationName());
		mTxtDictationname.setText(mDictCard.getDictationName());	
		setOrDelImage();
   }
   
    @Override
    public void onBackPressed() {
    	
    	if(mEdit.getText().toString().trim().equalsIgnoreCase(getResources().getString(R.string.Property_Edit))){
    		mDictCard.setWorktype(mWorktype.getText().toString().trim());
    	}
    	if(mActivityMode.equals("dictate")){
    		dmApplication.flashair = false;
    		Intent intent = new Intent(DictationPropertyActivity.this , DictateActivity.class);
    		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    		intent.putExtra(DMApplication.START_MODE_TAG, mPassedMode);
    		dmApplication.passMode = mPassedMode;
    		intent.putExtra(DMApplication.DICTATION_ID, mDictCard.getDictationId());
    		//bundle.putParcelableArrayList(DMApplication.DICTATION_CARD_KEY, dCardList);
    		//intent.putExtras(bundle);
			startActivity(intent);
    	}else
    		dmApplication.onSetPending = false;
    	
    	if(dmApplication.newCreated){
			dmApplication.newCreated = false;
		}
    	dmApplication.lastDictMailSent = false;
    	discardImageChange();
    	super.onBackPressed();
    }
    
    /**
     * Invokes when user discards the Thumbnail attached by pressing device back button 
     * without pressing done button.
     */
    private void discardImageChange(){
    	if(mEdit.getTag().toString().trim().equalsIgnoreCase("1")&& photoFlag){
    		if(initialImgFlag == 1){
    			flag = 1;
    			setThumbnailAvailability(1);
    			sourceFile = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mDictCard.getSequenceNumber()+"/"+TEMP_IMAGE+".jpg");
	    		if(sourceFile.exists())
	    			sourceFile.delete();
    		}else{
	    		sourceFile = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mDictCard.getSequenceNumber()+"/"+TEMP_IMAGE+".jpg");
	    		if(sourceFile.exists())
	    			sourceFile.delete();
	    		if(mDictCard.getIsThumbnailAvailable()!=1){
		    		flag = 0;
		    		setThumbnailAvailability(0);
	    		}
	    		sourceFile = null;
    		}
    	}
    }
    
    /**
     * Sets or deletes Thumbnail file in sdcard.
     */
    private void setOrDelImage(){
    	if(flag == 0){
    		mImageFile = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mDictCard.getSequenceNumber()+"/"+mDictCard.getDictFileName()+".jpg");
			if(mImageFile.exists())
				mImageFile.delete();
			setThumbnailAvailability(0);
    	}else{
    		outputPath="/Dictations/"+mDictCard.getSequenceNumber()+"/"+mDictCard.getDictFileName()+".jpg";
			imageInputPath=DMApplication.DEFAULT_DIR+"/Dictations/"+mDictCard.getSequenceNumber()+"/"+TEMP_IMAGE+".jpg";
			manageChooseImage=new ManageChooseImage();
			manageChooseImage.execute();
    		setThumbnailAvailability(1);
    		sourceFile = null;
    	}
    }
    
  
    
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	if(!isOnceWindowFocusChanged)
    	{
    		mImageFile = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+
    	    		mDictCard.getSequenceNumber()+"/"+mDictCard.getDictFileName()+".jpg");
    		if(mDictCard.getIsThumbnailAvailable() == 1 && mImageFile.exists())
    	    {
    	    	mAuthorphoto.setImageBitmap(decodeSampledBitmapFromResource(mImageFile.getAbsolutePath(),
    	    			mAuthorphoto.getWidth(), mAuthorphoto.getHeight()));
    	    }
    		isOnceWindowFocusChanged=true;
    	}
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/*@Override
    public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
        	case R.id.olympusSetting:
        		promtSettings();
        		return true;
        }
		return super.onOptionsItemSelected(item);
    }*/
	 @Override
     public void onPause()
     {
     	super.onPause();
     	//discardImageChange();
     	mShaker.pause();
     		
     }
	 @Override
     public void onResume()
     {
     	super.onResume(); 
     	setCurrentLanguage(dmApplication.getCurrentLanguage());
     	if(mDictCard.getStatus()==DictationStatus.SENT.getValue()||mDictCard.getStatus()==DictationStatus.SENT_VIA_EMAIL.getValue()
     			||mDictCard.getStatus()==DictationStatus.OUTBOX.getValue() || mDictCard.isResend() == 1){
     		mEdit.setVisibility(View.INVISIBLE);

     		if(mDictCard.getStatus()==DictationStatus.OUTBOX.getValue()){
     			mShakeTwiceRelative.setVisibility(View.INVISIBLE);
     			mShaker.pause();
     		}

     		else if(DictateActivity.coming==1)
			{
				mShakeTwiceRelative.setVisibility(View.INVISIBLE);
				mShaker.pause();
			}
     		else{
     			mShakeTwiceRelative.setVisibility(View.VISIBLE);
     			mShaker.resume();
     		}
     		
     	}else{
     		mEdit.setVisibility(View.VISIBLE);
     		mShakeTwiceRelative.setVisibility(View.VISIBLE);
     		mShaker.resume();
     	}
     	
     	if(mTxtDictationname != null)
     		mTxtDictationname.clearFocus();
     	if(isKeyboardShown){
     		if(mEditDictationname.hasFocus()){
	     		mEditDictationname.setText(mEditDictationname.getText().toString());
	     		mEditDictationname.setSelection(mEditDictationname.getText().length());
	     		mEditDictationname.requestFocus();
     		}else if(mEditComments.hasFocus()){
     			mEditComments.setText(mEditComments.getText().toString());
     			mEditComments.setSelection(mEditComments.getText().length());
     			mEditComments.requestFocus();
     		}
     	}
 	}
	
	 /**
	 * Dialog will be shown when user tap on image icon. 
	 */
	private void promptImageSelection(){
		String title="";
    	if(flag==0){
    		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	mBuilder = new AlertDialog.Builder(DictationPropertyActivity.this);
		    View layout = inflater.inflate(R.layout.activity_dictate_property_image_menu,(ViewGroup)this.findViewById(R.id.relative_property_image_menu));
		    mTakephoto=(Button)layout.findViewById(R.id.btn_property_image_takephoto);
		    mChoosePhoto=(Button)layout.findViewById(R.id.btn_property_image_choose_existing);
		    mCancel=(Button)layout.findViewById(R.id.btn_property_image_cancel);
		    mTakephoto.setOnClickListener(new OnClickListener(){
		    	@Override
		    	public void onClick(View v){
		    		mEditDictationname.clearFocus();
		    		mImageFile = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mDictCard.getSequenceNumber()+"/"+TEMP_IMAGE+".jpg");
					Uri outputFileUri=null;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						outputFileUri = FileProvider.getUriForFile(DictationPropertyActivity.this, BuildConfig.APPLICATION_ID + ".provider", mImageFile);
					}
					else
						{outputFileUri = Uri.fromFile(mImageFile);

						}
					//  Uri
	                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		            startActivityForResult(intent,CAMERA_IMAGE_CAPTURE);
       	    		System.gc();
				    mAlertDialog.dismiss();
				}
			});
		    mChoosePhoto.setOnClickListener(new OnClickListener(){
		    	@Override
		    	public void onClick(View v){
		    		mEditDictationname.clearFocus();
		    		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
					photoPickerIntent.setType("image/*");    	 
					startActivityForResult(photoPickerIntent,SELECT_PHOTO);  
					System.gc();
					mAlertDialog.dismiss(); 
				}
			});	
		    mCancel.setOnClickListener(new OnClickListener(){
		    	@Override
		    	public void onClick(View v){
		    		mAlertDialog.dismiss();	
				}
			});  	  
		    mBuilder.setView(layout);    
		    mAlertDialog = mBuilder.create();
		    mAlertDialog.setTitle(title);
		    mAlertDialog.show();
    	}
    	else if(flag==1){
    		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  	    	mBuilder = new AlertDialog.Builder(DictationPropertyActivity.this);
  		    View layout = inflater.inflate(R.layout.activity_dictate_property_image_menu,(ViewGroup)this.findViewById(R.id.relative_property_image_menu));
  		    mTakephoto=(Button)layout.findViewById(R.id.btn_property_image_takephoto);
  		    mChoosePhoto=(Button)layout.findViewById(R.id.btn_property_image_choose_existing);
  		    mCancel=(Button)layout.findViewById(R.id.btn_property_image_cancel);
  		    mDeletephoto=new Button(this);
  		    if(isLargeScreen()){
  		    	mDeletephoto.setTextSize(28);
  		    }
  		    //mDeletephoto.setTypeface(null,Typeface.BOLD);
  		    mDeletephoto.setText(getResources().getString(R.string.Camera_Delete_Photo));
  		    mLinaerlayout=(LinearLayout)layout.findViewById(R.id.linear_property_image_delete_photo);
  		    LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
  		    mLinaerlayout.addView(mDeletephoto,lp);
  		    mTakephoto.setOnClickListener(new OnClickListener(){
  		    	@Override
  		    	public void onClick(View v){
  		    		mEditDictationname.clearFocus();
  		    		mImageFile = new File(DMApplication.DEFAULT_DIR+"/Dictations/"+mDictCard.getSequenceNumber()+"/"+TEMP_IMAGE+".jpg");
					Uri outputFileUri=null;

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						 outputFileUri=FileProvider.getUriForFile(getApplicationContext(),BuildConfig.APPLICATION_ID + ".provider", mImageFile);
					}
					else
					{
						 outputFileUri = Uri.fromFile(mImageFile);
					}
	    	        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
  		    		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
  				    startActivityForResult(cameraIntent, CAMERA_IMAGE_CAPTURE); 
  				    mAlertDialog.dismiss();
  		    	}
  		    });  		    
  		    mChoosePhoto.setOnClickListener(new OnClickListener(){
  		    	@Override
  		    	public void onClick(View v){
  		    		mEditDictationname.clearFocus();
  		    		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
  		    		photoPickerIntent.setType("image/*");    	 
  		    		startActivityForResult(photoPickerIntent,SELECT_PHOTO);  
  		    		mAlertDialog.dismiss();
  		    	}
  		    });	
  		    mDeletephoto.setOnClickListener(new OnClickListener(){
  		    	@Override
  		    	public void onClick(View arg0){
  		    		mAlertDialog.dismiss();
  		    		deletePhoto(); 
  		    	}
  		    });
  		    mCancel.setOnClickListener(new OnClickListener(){
  		    	@Override
  		    	public void onClick(View v){
  		    		mAlertDialog.dismiss();	
  		    	}
  		    });

  		    mBuilder.setView(layout);    
  		    mAlertDialog = mBuilder.create();
  		    mAlertDialog.setTitle(title);
  		    mAlertDialog.show();
    	}
    	
	}
	
	private boolean isLargeScreen(){
		int screenSize = getResources().getConfiguration().screenLayout &
		        Configuration.SCREENLAYOUT_SIZE_MASK;
		
		if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Invoked when user selects settings menu.
	 */
	private void promtSettings(){
		
		Intent intent=new Intent(this,SettingsActivity.class);
		startActivity(intent);
		if(mDictCard.getStatus() == DictationStatus.NEW.getValue() && mDictCard.getDuration()>0){
			mDictCard.setStatus(DictationStatus.PENDING.getValue());
			mDBHandler.updateDictationStatus(mDictCard.getDictationId(), mDictCard.getStatus());
			mTxtStatus.setText(getResources().getString(R.string.Recording_Label_Pending));
			
		}
	}
	 /**
	 * Deletes thumbnail when user taps delete button in image selection dialog.
	 */
	private void deletePhoto(){
		alertDialog= new AlertDialog.Builder(DictationPropertyActivity.this);
		
		alertDialog.setPositiveButton(getResources().getString (R.string.Camera_Delete_Photo), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,int which){
				
				mAuthorphoto.setImageResource(R.drawable.camera_icon);
				photoFlag = true;
				flag=0;
			}
		});
		alertDialog.setNegativeButton(getResources().getString(R.string.Button_Cancel), new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog,	int which){
				dialog.cancel();
				flag=1;
			}
		});

		// Showing Alert Message
		alertDialog.show(); 
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){  
		try
		{
			if(requestCode==CAMERA_IMAGE_CAPTURE && resultCode==Activity.RESULT_OK){
	         
	            mAuthorphoto.setImageBitmap(decodeSampledBitmapFromResource(mImageFile.getAbsolutePath(),
	            		mAuthorphoto.getWidth(), mAuthorphoto.getHeight()));
	            //setThumbnailAvailability(1);
	            photoFlag = true;
	            flag=1;
	        }
			
			else if(resultCode==RESULT_OK&& null != data){
					Uri selectedImage = data.getData();
					String[] filePathColumn = { MediaStore.Images.Media.DATA };
					imageCursor= getContentResolver().query(selectedImage,filePathColumn, null, null, null);
					imageCursor.moveToFirst();
					int columnIndex = imageCursor.getColumnIndex(filePathColumn[0]);
					filePathColumn=null;
					outputPath= "/Dictations/"+mDictCard.getSequenceNumber()+"/"+TEMP_IMAGE+".jpg";
					imageInputPath=imageCursor.getString(columnIndex);
					imageCursor.close();
					imageCursor=null;
					manageChooseImage=new ManageChooseImage();
					manageChooseImage.execute();
			}
		}
		catch (Exception e) {
		}
		super.onActivityResult(requestCode, resultCode, data);  
	}  
	
	/**
	 * Used to manage image selection from gallery.
	 */
	private class ManageChooseImage extends AsyncTask<Void,Void,Void>
	{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				hasExceptionInImage=false;
				sdCard= DMApplication.DEFAULT_DIR;
                if (sdCard.canWrite()) {
                    sourceFile= new File(imageInputPath);
                    destinationFile= new File(sdCard, outputPath);
                    if (sourceFile.exists()) {
                    	srcChannel= new FileInputStream(sourceFile).getChannel();
                    	dstChannel= new FileOutputStream(destinationFile).getChannel();
                        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                        srcChannel.close();
                        dstChannel.close();
                        srcChannel=null;
                        dstChannel=null;
                    }
                }
            } catch (Exception e) {
            	e.printStackTrace();
            	hasExceptionInImage=true;
            }
	        return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(!hasExceptionInImage)
			{
				try {
					mAuthorphoto.setImageBitmap(decodeSampledBitmapFromResource(DMApplication.DEFAULT_DIR+outputPath, 
							mAuthorphoto.getWidth(), mAuthorphoto.getWidth()));
					flag=1;
					photoFlag = true;
		            //setThumbnailAvailability(1);
				} catch (Exception e) {
	            	e.printStackTrace();
	            }
			}
			imageInputPath=null;
			outputPath=null;
			sdCard=null;
			sourceFile=null;
			destinationFile=null;
			manageChooseImage=null;
		}
	}
	
	private void setThumbnailAvailability(int isAvailable){
		mDictCard.setIsThumbnailAvailable(isAvailable);
		mDBHandler.updateIsThumbnailAvailable(mDictCard);
	}
	
	
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
	
    private void autoResize(Button bt)
	{
		
		
		Spannable span = new SpannableString(bt.getText().toString());
		if(bt.getText().length()>=4&&bt.getText().length()<6)
		{
			
			span.setSpan(new RelativeSizeSpan(1f), 0, bt.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			bt.setText(span);
		} 
		else if(bt.getText().length()>=6&&bt.getText().length()<8)
		{
			
			span.setSpan(new RelativeSizeSpan(.8f), 0, bt.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			bt.setText(span);
		}
		else if(bt.getText().length()>=8&&bt.getText().length()<10)
		{
			
			span.setSpan(new RelativeSizeSpan(.7f), 0, bt.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			bt.setText(span);
		}
		else if(bt.getText().length()>=10)
		{
			
			span.setSpan(new RelativeSizeSpan(.6f), 0, bt.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			bt.setText(span);
		}
	}
    /**
     * Used to parse and set Worktype list .
     */
    private void setWorktype() {
		mListValues = new ArrayList<String>();
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		mCheckServermail = sharedPref.getString(
				getResources().getString(R.string.send_key), "");
		if (mCheckServermail.equalsIgnoreCase("2")) {
			sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			mServerWorktype = sharedPref.getString(
					getResources().getString(R.string.Worktype_Server_key), "");
			if (mServerWorktype.contains(":")) {
				mServerWorktype = getResources().getString(R.string.None) + ":" + mServerWorktype;
				mListValues = new ArrayList<String>(
						Arrays.asList(mServerWorktype.split(":")));
			} else if (mServerWorktype.equalsIgnoreCase("")) {
				mServerWorktype = getResources().getString(R.string.None);
				mListValues.add(mServerWorktype);
			} else if (!mServerWorktype.equalsIgnoreCase("")
					&& !mServerWorktype.contains(":")) {
				mServerWorktype = getResources().getString(R.string.None) + ":" + mServerWorktype;
				mListValues = new ArrayList<String>(
						Arrays.asList(mServerWorktype.split(":")));
			}
		} else if(mCheckServermail.equalsIgnoreCase("1")||mCheckServermail.equalsIgnoreCase("")){
			sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			mMailworktype = sharedPref.getString(
					getResources().getString(R.string.Worktype_Email_Key), "");
			if (mMailworktype.contains(":")) {
				mMailworktype = getResources().getString(R.string.None) + ":" + mMailworktype;
				mListValues = new ArrayList<String>(Arrays.asList(mMailworktype
						.split(":")));
			} else if (mMailworktype.equalsIgnoreCase("")) {
				mMailworktype = getResources().getString(R.string.None);
				mListValues.add(mMailworktype);
			} else if (!mMailworktype.equalsIgnoreCase("")
					&& !mMailworktype.contains(":")) {
				mMailworktype = getResources().getString(R.string.None) + ":" + mMailworktype;
				mListValues = new ArrayList<String>(Arrays.asList(mMailworktype
						.split(":")));
			}
		}
		mWorktypes = new String[mListValues.size()];
		mWorktypes = mListValues.toArray(mWorktypes);

	}

    /**
     * Worktype list will be shown when user taps worktype.
     */
    private void promptWorktypeList() {
		String title = "";
		ListView workTypeList = null;
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mBuilder = new AlertDialog.Builder(DictationPropertyActivity.this);
		title =getResources().getString(R.string.Select) ;
		setWorktype();
		View layout = inflater.inflate(
				R.layout.activity_dictate_property_worktype_list,
				(ViewGroup) this.findViewById(R.id.relWorktypeView));
		String setWork=mWorktype.getText().toString();
		int pos=0;
		for(int i=0;i<mWorktypes.length;i++){
			if(setWork.equalsIgnoreCase(getResources().getString(R.string.Settings_Select))){
				setWork=getResources().getString(R.string.None);
			}
			if(mWorktypes[i].equalsIgnoreCase(setWork)){
				pos=i;
				break;
			}
		}
		workTypeList = (ListView) layout.findViewById(R.id.lstWorktype);
		workTypeList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		ArrayAdapter<String> referal_adapter = new ArrayAdapter<String>(
				DictationPropertyActivity.this,
				android.R.layout.simple_spinner_dropdown_item, mWorktypes);
		workTypeList.setAdapter(referal_adapter);
		workTypeList.setItemChecked(pos, true);
		workTypeList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view,
					int pos, long arg3) {
				mSelectedWorktype = ((TextView) view).getText().toString();
				if(mSelectedWorktype.equalsIgnoreCase(getResources().getString(R.string.None))){
					mWorktype.setText(getResources().getString(R.string.Settings_Select));
					mSelectedWorktype = "";
				}
				else{
					mWorktype.setText(mSelectedWorktype);
				}
				mAlertDialog.dismiss();
			}
		});
		mBuilder.setView(layout);
		mAlertDialog = mBuilder.create();
		mAlertDialog.setTitle(title);
		mAlertDialog.show();
	}
    /**
     * Sets current language based on the input value.
     * @param value Language option.
     */
    private void setCurrentLanguage(int value){
		 int Val=1;
		 Val=value;
		 if(Val==1)
				setLocale("en");
			else if(Val==2)
				setLocale("de");
			else if(Val==3)
				setLocale("fr");
			else if(Val==4)
				setLocale("es");
			else if(Val==5)
				setLocale("sv");
			else if(Val==6)
				setLocale("cs");
	 }
    /**
     * Set locale with the language passed.
     * @param lang Language passed as String.
     */
    private void setLocale(String lang) {
		 
	        locale = new Locale(lang);
	        Resources res = getResources();
	        DisplayMetrics dm = res.getDisplayMetrics();
	        Configuration conf = res.getConfiguration();
	        conf.locale = locale;
	        res.updateConfiguration(conf, dm);
	      
	    }
    /**
     * Unbind drawables used when this class is destroyed.
     * @param view
     */
    private void unbindDrawables(View view)
	{
    	if (view.getBackground() != null)
        {
                view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView))
        {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
                {
                        unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                ((ViewGroup) view).removeAllViews();
        }
	}
	@Override
	protected void onDestroy()
    {
		super.onDestroy();
		
		
		bitmap = null;
        unbindDrawables(findViewById(R.id.btn_property_edit));
        unbindDrawables(findViewById(R.id.edittext_property_dictation_name));
        unbindDrawables(findViewById(R.id.image_property_camera));
        unbindDrawables(findViewById(R.id.text_property_worktype));
        unbindDrawables(findViewById(R.id.check_property_priority));
        unbindDrawables(findViewById(R.id.text_property_dictation_name));
        unbindDrawables(findViewById(R.id.image_property_new_dictation));
        unbindDrawables(findViewById(R.id.edittext_property_dictation_name));
        unbindDrawables(findViewById(R.id.text_property_dictate_details));
        unbindDrawables(findViewById(R.id.text_property_jobnumber));
        unbindDrawables(findViewById(R.id.text_property_status));
        unbindDrawables(findViewById(R.id.edit_property_comment));
        unbindDrawables(findViewById(R.id.relative_property_shake_twice_layout));
        System.gc();
    }
	
	/**
	 * The image taken with camera or selected from gallery will be resized to fit with the image view.
	 * @param path Image path.
	 * @param reqWidth Required width of the image.
	 * @param reqHeight Required height of the image.
	 * @return Bitmap of the image.
	 */
	private Bitmap decodeSampledBitmapFromResource(String path,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        ExifInterface exif = null;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
        bitmap = Bitmap.createBitmap(bmp, 0, 0, options.outWidth, options.outHeight, matrix, true);
        bmp = null;
        matrix = null;
        
        return bitmap;
    }
    
	/**
	 * Calculates Sample size for the image to be resized.
	 * @param options Bitmap Factory options
	 * @param reqWidth Required width of the image.
	 * @param reqHeight Required height of the image.
	 * @return Sample size as int.
	 */
	public int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}
	
/**
 * Get Dictation status in String.
 * @param status Status as int.
 * @return Status in String.
 */
public String getStatusInString(int status){
		
		switch (status) {
			case -1:
				return getResources().getString(R.string.Property_New);
			case 0:
				return getResources().getString(R.string.Property_Pending);
			case 18:
				return getResources().getString(R.string.Property_Outbox);
			case 2:
				return getResources().getString(R.string.Property_Dictate_Sent);
			case 3:
				return getResources().getString(R.string.Property_Via_Email);
			case 20:
				return getResources().getString(R.string.property_sending_failed);
			case 10:
				return getResources().getString(R.string.Property_sending);
			case 22:
				return getResources().getString(R.string.Property_Timeout);
			case 11:
				return getResources().getString(R.string.Property_Retrying);
			case 12:
				return getResources().getString(R.string.Property_Retrying);
			case 13:
				return getResources().getString(R.string.Property_Retrying);
			case 25:
				return getResources().getString(R.string.Property_Conversion_Failed);
			case 15:
				return getResources().getString(R.string.Property_Waiting_Send);
			case 16:
				return getResources().getString(R.string.Property_Waiting_Send);
			default:
				return getResources().getString(R.string.Property_Pending);
		}
	}
	public boolean checkCameraPermission()
	{boolean check=true;
		int permissiontakeCamera = ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA);

		int writeStoragePermission = ContextCompat.checkSelfPermission(this,


				Manifest.permission.WRITE_EXTERNAL_STORAGE);

		int readStoragePermission = ContextCompat.checkSelfPermission(this,


				Manifest.permission.READ_EXTERNAL_STORAGE);
		int[] perm = {permissiontakeCamera,writeStoragePermission,readStoragePermission};
		String[] stringPerm = {Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
		ActivityCompat.requestPermissions(this, stringPerm, 1);
        for (String permis : stringPerm) {
            if( !(ActivityCompat.checkSelfPermission(this, permis) == PackageManager.PERMISSION_GRANTED)) {

                check = false;
            }
        }
		return check;

		}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		String permissionTxt = "";
	if (permissions.length == 0) {
			return;
		}
		boolean allPermissionsGranted = true;
		if (grantResults.length > 0) {
			for (int grantResult : grantResults) {
				if (grantResult != PackageManager.PERMISSION_GRANTED) {
					allPermissionsGranted = false;
					break;
				}
			}
		}
		if (!allPermissionsGranted) {
			boolean somePermissionsForeverDenied = false;
			for (String permission : permissions) {
				if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
					//denied
					Log.e("denied", permission);
				} else {
					if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
						//allowed
						Log.e("allowed", permission);
					} else {
						if (permission.equalsIgnoreCase("android.permission.CAMERA")) {
							if (permissionTxt.equalsIgnoreCase("")) {
								permissionTxt += "Camera";
							} else {
								permissionTxt += ",Camera";
							}
						}
						if (permission.equalsIgnoreCase("android.permission.WRITE_EXTERNAL_STORAGE")) {
							if (permissionTxt.equalsIgnoreCase("")) {
								permissionTxt += "Storage";
							} else {
								permissionTxt += ",Storage";
							}
						}
						//set to never ask again
						Log.e("set to never ask again", permission);
						somePermissionsForeverDenied = true;
					}
				}
			}
			if (somePermissionsForeverDenied) {
				final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
				alertDialogBuilder.setTitle("Permissions Required")
						.setMessage(getResources().getString(R.string.permission)+"\n\n"+getResources().getString(R.string.permission)+"(" + permissionTxt + ")")
						.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
										Uri.fromParts("package", getPackageName(), null));
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						})
						.setCancelable(false)
						.create()
						.show();
			}
		} else {
			sharedPreferences=getSharedPreferences("Campermissions",MODE_PRIVATE);
			SharedPreferences.Editor editor = getSharedPreferences("Campermissions", MODE_PRIVATE).edit();
			editor.putBoolean("camperm",true);
			editor.commit();
			isCamPermissionGrnated = true;
		}
	}
	}
