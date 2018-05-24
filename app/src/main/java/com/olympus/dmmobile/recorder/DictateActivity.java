package com.olympus.dmmobile.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ToggleButton;
import com.olympus.dmmobile.AMRConverter;
import com.olympus.dmmobile.CustomDialog;
import com.olympus.dmmobile.CustomLaunchDialog;
import com.olympus.dmmobile.DMActivity;
import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.DatabaseHandler;
import com.olympus.dmmobile.DictationCard;
import com.olympus.dmmobile.DictationPropertyActivity;
import com.olympus.dmmobile.DictationStatus;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.flashair.ChooserIntentListAdapter;
import com.olympus.dmmobile.network.NetWorkAndNotActivatedDialog;
import com.olympus.dmmobile.recorder.DMAudioRecorder.AudioRecordStateListener;
import com.olympus.dmmobile.webservice.Base64_Encoding;
import com.olympus.dmmobile.webservice.Settingsparser;
import com.olympus.dmmobile.webservice.WebserviceHandler;

/**
 * 
 * @version 1.0.1
 * 
 */
// @SuppressLint("DefaultLocale")
public class DictateActivity extends Activity implements OnClickListener,
		SeekBar.OnSeekBarChangeListener, AudioRecordStateListener {

	static {
		System.loadLibrary("native_lib");
	}

	/**
	 * native function which performs insertion or overwrite process
	 * 
	 * @param target
	 *            target WAV file
	 * @param source
	 *            source WAV file
	 * @param temp
	 *            temp file for insertion
	 * @param position
	 *            position in target file at which the insertion or overwrite
	 *            process is carried out
	 * @param process_type
	 *            type of process, 0 for overwrite and 1 for insertion
	 * @return return the length of audio data that has been inserted or
	 *         overwritten
	 */
	public native long process(String target, String source, String temp,
			long position, int process_type);

	private enum MediaMode {
		PLAY, PAUSE, START
	};

	public static String AUDIO_FILE_NAME;
	public static final String AUDIO_FILE_EXTENTION = ".wav";
	private static String AUDIO_FILE_FOLDERNAME = "Dictations";
	private static int RECORDING_NOTIFY_ID = 55555;
	private static int PLAYER_NOTIFY_ID = 44444;

	private final long MAXIMUM_LIMIT_SIZE = 1073741824;

	public static DMAudioRecorder RECORDER;
	public static boolean fromShake = false;
	public static boolean IsAlreadyActive = false;
	private int InsideMMduration;
	private int amoungToupdate;
	private MediaMode playButtonState = MediaMode.PAUSE;
	private int cAmplitude = 0;
	private int isRewinding = 0;
	private int isForwarding = 0;
	private boolean isFolderButtonTouched = false;
	private AlertDialog.Builder alertDialog;
	private long totalDuration;
	private long currentDuration;
	public boolean InterruptCallDialogshown = false;
	private long savedCurrentPos;
	private long savedFileSizeDuration;
	public static boolean isCallActive = false;
	public static int SavedDicID;
	private String TotalDur;
	private String passedModeName;
	private String passedFileName;
	private DictationCard passedDictationCard;
	private String mAuthor; // get author from settings
	private String mAuthorName = "AUTHOR";
	private String mDictationName;
	private String mCreatedDate;
	private String mRecStartDate;
	private String mRecStopDate;
	private boolean mFavSelected = false;
	private boolean isRecording = false;
	public boolean OverwriteLimitExceeded = false;
	private boolean isVcvaDetectionRunning = false;
	private boolean ActivityInitialPlay = false;
	public static boolean isInsertionProcessRunning = false;
	private boolean isVcvaEnabled = false;
	private boolean isPushToTalk = false;
	public static boolean isInsert = false;
	public boolean isPropertyClicked = false;
	boolean isSofyKeyBoardshown = false;
	private boolean isNew = true;
	public boolean isIntentPlay = true;
	private boolean isBackpressed = false;
	private boolean checkForwordIsEnabled = false;
	private BluetoothAdapter mBluetoothAdapter;
	private AlertDialog.Builder mBuilder = null;
	private AlertDialog mAlertDialog = null;
	private TimerTask task;
	private Timer timer = null;
	private TimerTask mpTimertask;
	private Timer mptimer = null;
	private View activityRootView;
	private TextView tvTimeLeft, tvTimeRight, tvSubtitle, tvWorktype;
	private Button bNew, bRecord, bRewind, bForward, bPlay;
	private EditText tvTitle;
	private ImageView imPhoto;
	private CheckBox imFav;
	private ImageButton imbFolder, imbFiles, imbSend, imbDelete;
	private ToggleButton tbOverwrite;
	private SeekBar Graphbar;
	private Signalometer mSignalometer;
	private OverwriteSignalometer mOverwriteSignalometer;
	private MediaPlayer mMediaPlayer = null;
	private Handler mHandler;
	private SoundWaveGraph mWavegraph;
	private static File mDictationDir;
	private DictationCard dictCard;
	private AlertDialog mSendDialog;
	private boolean bluetoothConnectivity;
	private String mCheckServermail;
	private String mMailworktype;
	private String mServerWorktype;
	private ArrayList<String> mListValues;
	private PowerManager pmPowerManager;
	private PowerManager.WakeLock pmWakeLock;
	private Locale locale;
	private int mLanguageVal;
	private DatabaseHandler mDbHandler;
	private SharedPreferences mSharedPreferenece;
	private String mWorktype = "";
	private SharedPreferences pref;
	private SharedPreferences prefs;
	private SharedPreferences vcvaPref;
	private String[] mWorktypes = null;
	private int seqNumber;
	public static boolean isOncePaused; // flag to check if paused once
	public static long pausePosition = 0; // position at which the player is
	private int blinkRecordVariable = 0;
	private boolean blinkRecordBoolean = true; // paused
	private boolean brecordisRecording = false;
	private long currentBytePosition = 0;
	private int process_type = 0;
	private long temp_size = 0;
	private boolean continue_process = true;
	private long fileSize = 0;
	private long passedTotalDuration;
	private long tempFileSize = 0;
	private boolean isOnceWindowFocusChanged = false;
	private DMApplication dmApplication = null;
	private String filePath = null;
	private double cardSize = 0;
	private final double maxSize = 23 * 1024 * 1024;
	// getSettings...
	public static final String PREFS_NAME = "Config";
	private SharedPreferences sharedPreferences;
	private String mSettingsConfig;
	private String mActivation;
	private String base64value;
	private Base64_Encoding baseEncoding;
	private String prefUUID;
	private String mEmail;
	private String mGetemail;
	private String mGetuuid;
	private String mUrl;
	private String mGetSettingsResponse;
	private WebserviceHandler webserviceHandler;
	private Settingsparser settingParser;
	private String mResultcode;
	private SharedPreferences.Editor editor;
	private String mAudioEncrypt = "-1";
	private String mAudioPassword;
	private String mAudioFormat;
	private String mAudioDelivery;
	private String mWorktypeListname;
	private List<Settingsparser.WorkTypeListObjects> worktypeobject;
	private int isSplittable = 0;
	private boolean hasMultipleSplits = false;
	private boolean hasNoEncript = false;
	private boolean rewindPressed = false;
	private boolean isCriticalErrorOccures = false;
	private boolean isNavigatedToAnotherScreen = false;
	private PowerManager mPowerManager;
	private WindowManager mWindowManager;
	private WakeLock mWakeLock;
	private boolean IsOtherRecorderActive = false;
	// send via email(amr)
	private final String FORCEQUIT_PREF_NAME = "force_quit";
	private final String EMAIL_PREF_NAME = "email_prefs";
	private final String EMAIL_PREF_KEY = "selected_client";
	private final int EMAIL_SEND_REQ_CODE = 100;
	private DeviceLockInterrupt deviceLockInterrupt;
	// private CallInterrupt callInterrupt;
	private Intent baseIntent;
	private File file = null, mtempFile = null;
	private FileChannel srcChannel = null;
	private FileChannel dstChannel = null;
	private boolean isrecordingOnclick = false;
	private AudioManager audioManager;
	private AlertDialog.Builder alert;
	public final static String WAS_DESTROYED = "destroyed_state";
	public final static String SEQ_NUMBER_KEY = "seq_number";
	public final static String DONT_SHOW_KEY = "dont_show";
	private String Activation;
	public static boolean isReview = false;
	private Bitmap bitmap;
	private boolean isSendClicked = false;
	private ProgressDialog dialog;
	private boolean isKeyboardShown = false;
	private AlertDialog aDialog;
	private boolean rewindStratPlay = false;
	private int sNumber;
	private int dictCount;
	private String language;
	private boolean playmodeRewindFrwdworking = false;
	public static String debugDictName;
	private String keepName = null;
	private boolean retainFlag = false;
	private boolean limitFlag = false;
	private boolean wasResume = false;
	private boolean isBeyondLimit = false;
	private AMRConverter amrConverter = null;
	private StatFs stat = null;
	private boolean keyboardFlag = false;
	private boolean oldDictExistFlag = false;

	OutgoingCallReceiver outGoingCallReciever;
	MediaRecorder mTestRecorder;

	private static final double THRESHOLD = 0.50;
	private long FREE_SPACE_REQUIRED = 0;
	private AlertDialog mInterruptAlert = null;

	int actionFlag = 0;

	private Runnable rewindThread = new Runnable() {
		public void run() {

			if (isRewinding == 1) {

				int rewCurrentPosition = Graphbar.getProgress();
				rewCurrentPosition = rewCurrentPosition - 5;
				if (rewCurrentPosition >= 1) {

					if (!isInsertionProcessRunning) {
						Graphbar.setProgress(rewCurrentPosition);
						if (mMediaPlayer != null) {
							mMediaPlayer
									.seekTo((int) getMMcurrentPos(rewCurrentPosition));
						}
						mediaPlayerUpdateTimer(getMMcurrentPos(rewCurrentPosition));
						pausePosition = getMMcurrentPos(rewCurrentPosition);
						if(checkForwordIsEnabled) {
							if (Graphbar.getMax() > Graphbar.getProgress()) 
								bForward.setBackgroundResource(R.drawable.dictate_forward);
							checkForwordIsEnabled = false;
						}
					}

				} else {
					if (!isInsertionProcessRunning) {
						Graphbar.setProgress(0);
						if (mMediaPlayer != null) {
							mMediaPlayer.seekTo(0);
						}
						mediaPlayerUpdateTimer(0);
						disableRewind();
						pausePosition = 0;
					}
				}
				mHandler.post(this);
			}
		}

	};

	private Runnable forwardThread = new Runnable() {
		public void run() {
			if (isForwarding == 1) {
				int fwdCurrentPosition = Graphbar.getProgress();
				fwdCurrentPosition = fwdCurrentPosition + 5;
				if (fwdCurrentPosition < Graphbar.getMax()) {
					Graphbar.setProgress(fwdCurrentPosition);
					if (mMediaPlayer != null) {
						mMediaPlayer
								.seekTo((int) getMMcurrentPos(fwdCurrentPosition));
					}
					mediaPlayerUpdateTimer(getMMcurrentPos(fwdCurrentPosition));
					pausePosition = getMMcurrentPos(fwdCurrentPosition);
				} else {
					disableForward();
					Graphbar.setProgress(Graphbar.getMax());
					bNew.setEnabled(true);
					bNew.setBackgroundResource(R.drawable.dictate_new_selector);
					mediaPlayerUpdateTimer(getMMcurrentPos(Graphbar.getMax()));
					if (!isReview) {
						recorderSetEnable(true);
					}
				}
				mHandler.post(this);
			}
		}
	};

	public static String getFilename() {

		String fileName = null;
		try {

			fileName = (mDictationDir.getAbsolutePath() + "/" + AUDIO_FILE_NAME + AUDIO_FILE_EXTENTION);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileName;
	}

	private long getAvailableSpaceInMB() {
		final long SIZE_KB = 1024L;
		final long SIZE_MB = SIZE_KB * SIZE_KB;
		return dmApplication.getAvailableDiskSpace() / SIZE_MB;
	}

	public static boolean isSdcardmounted() {
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) 
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		else 
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		/**
		 * REGISTERING BROADCASTRECEIVER FOR DEVICE LOCK,DEVICE POWER OFF
		 * 
		 */
		DMAudioRecorder.drawWavGraphOnce = true;
		ActivityInitialPlay = true;
		isIntentPlay = true;
		deviceLockInterrupt = new DeviceLockInterrupt();
		dmApplication = (DMApplication) getApplication();
		dmApplication.newCreated = true;
		setCurrentLanguage(dmApplication.getCurrentLanguage());
		IntentFilter filter = new IntentFilter(Intent.ACTION_SHUTDOWN);
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction("android.intent.action.PHONE_STATE");
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		baseIntent = this.registerReceiver(deviceLockInterrupt, filter);

		outGoingCallReciever = new OutgoingCallReceiver();
		IntentFilter outgoingCallFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
		baseIntent = this.registerReceiver(outGoingCallReciever, outgoingCallFilter);
		audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		/**
		 * Get an instance of the PowerManager
		 */
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

		/**
		 * Get an instance of the WindowManager
		 */
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mWindowManager.getDefaultDisplay();

		/**
		 * Create a bright wake lock
		 */
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dictation_layout);
		mountViewsToJava();
		setClickListeners();
		setRewindandForward();
		showGraph();

		dmApplication.setContext(this);

		isOncePaused = false;
		mDbHandler = dmApplication.getDatabaseHandler();
		mHandler = new Handler();
		mWavegraph = new SoundWaveGraph(DictateActivity.this);
		enableDictationDatas(true);
		webserviceHandler = new WebserviceHandler();
		tvWorktype.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (!isKeyboardShown)
					tvTitle.setCursorVisible(false);
				promptWorktypeList();
			}
		});
		if(savedInstanceState == null) {
			if(getIntent().getBooleanExtra("isFromSlash", false)) {
				if(dmApplication.getResultCode() != null)
					onStartNotActivatedActivity();
				else
					NetWorkAndNotActivatedDialog.getInstance().onShowDialog(this);
			}
		}
	}
	
	/**
	 * Method to show 'Not Activated' dialog when Account is not activated/deactivated state.
	 */
	private void onStartNotActivatedActivity() {
		if(!dmApplication.isTimeOutDialogOnFront()) {
			dmApplication.setTimeOutDialogOnFront(true);
			dmApplication.setWantToShowDialog(false);
			baseIntent = new Intent(dmApplication.getContext(), CustomLaunchDialog.class);
			baseIntent.putExtra("Resultcode", dmApplication.getResultCode());
			baseIntent.putExtra("Message", dmApplication.getMessage());
			dmApplication.getContext().startActivity(baseIntent);
			baseIntent=null;
			dmApplication.setResultCode(null);
			dmApplication.setMessage(null);
		}
	}
	/**
	 * Cancel notification when the Notification ID is passed.
	 * 
	 * @param ctx
	 *            is the context
	 * @param notifyId
	 *            is the Notification ID passed.
	 * 
	 */
	public static void CancelNotification(Context ctx, int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx
				.getSystemService(ns);
		nMgr.cancel(notifyId);
	}

	/**
	 * @return Int value of VCVA level set by user.
	 */
	private int getVcvaLevel() {
		return vcvaPref.getInt(
				getResources().getString(R.string.vcva_seekbar_key), 5);
	}

	/**
	 * @return boolean State of VCVA, User Enabled or Disabled.
	 */
	private boolean getVcvaState() {
		return vcvaPref.getBoolean(getResources().getString(R.string.vcva_key),
				false);
	}

	/**
	 * @return boolean State of PushToTalk, User Enabled or Disabled.
	 */
	private boolean getPushToTalkState() {
		return vcvaPref.getBoolean(
				getResources().getString(R.string.push_to_talk_key), false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if ((isForwarding == 1 || isRewinding == 1)) {
			if (isForwarding == 1) {
				isForwarding = 0;

				if (!(Graphbar.getProgress() == Graphbar.getMax())) {
					bForward.setBackgroundResource(R.drawable.dictate_forward);
				} else {
					bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
				}
			} else if (isRewinding == 1) {
				isRewinding = 0;
				if (!(Graphbar.getProgress() == 0)) {
					bRewind.setBackgroundResource(R.drawable.dictate_rewind);
				} else {
					bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
				}
			}

			if (playButtonState == MediaMode.PLAY) {
				playmodeRewindFrwdworking = true;
			}
		}
		if (!isBeyondLimit)
			if (!limitFlag) {
				if (isRecording && !isBackpressed) {
					if (!isPushToTalk) {
						NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
						int icon = R.drawable.ic_launcher;
						CharSequence notiTriggerText = "";
						long whenShow = System.currentTimeMillis();
						Notification mNotify = new Notification(icon,
								notiTriggerText, whenShow);
						Context context = getApplicationContext();
						CharSequence notifyTitle = getResources().getString(
								R.string.category_recording)
								+ "...";
						CharSequence notifySubTitle = dictCard
								.getDictationName();
						if ((!isReview && !(passedModeName
								.equals(DMApplication.MODE_EDIT_RECORDING)))) {
							
							passedModeName = "";
						}
						Intent notificationIntent = new Intent(this,
								DictateActivity.class);
						PendingIntent contentIntent = PendingIntent
								.getActivity(this, 0, notificationIntent, 0);
						notificationIntent.putExtra(
								DMApplication.START_MODE_TAG, passedModeName);

						mNotify.setLatestEventInfo(context, notifyTitle,
								notifySubTitle, contentIntent);
						notificationManager
								.notify(RECORDING_NOTIFY_ID, mNotify);
					}
				} else if (isBackpressed && isRecording) {

					recorderStopRecording();
					dictCard.setStatus(DictationStatus.PENDING.getValue());
					mDbHandler.updateDictationStatus(dictCard.getDictationId(),
							dictCard.getStatus());
				} else if ((!isRecording
						|| (Graphbar.getProgress() != Graphbar.getMax())) && !isNew) {
					if (!isReview && !(passedModeName.equals(DMApplication.MODE_EDIT_RECORDING))) {
						SavedDicID = dictCard.getDictationId();
						passedModeName = "";
						Intent intent = getIntent();
						intent.putExtra(DMApplication.START_MODE_TAG,
								passedModeName);
						setIntent(intent);
					}
				}
				if ((Graphbar.getProgress() == Graphbar.getMax() || (Graphbar.getProgress() != Graphbar.getMax())) && !isReview && !isNew) {
					SavedDicID = dictCard.getDictationId();
					if(!(passedModeName.equals(DMApplication.MODE_EDIT_RECORDING)))
						passedModeName = "";
					dmApplication.passMode = "";
					Intent intent = getIntent();
					intent.putExtra(DMApplication.START_MODE_TAG,passedModeName);
					setIntent(intent);
				}
				if (passedModeName.equals(DMApplication.MODE_COPY_RECORDING)) {
					dictCard.setRecEndDate(dmApplication.getDeviceTime());
					mDbHandler.updateRecEndDate(dictCard);
				}

				if (mMediaPlayer != null) {
					if (mMediaPlayer.isPlaying()) {
						SavedDicID = dictCard.getDictationId();
						passedModeName = "";
						Intent intent = getIntent();
						intent.putExtra(DMApplication.START_MODE_TAG,
								passedModeName);
						setIntent(intent);
					}
				}
				if (isPushToTalk && isRecording) {
					recorderStopRecording();
				}
				if (isKeyboardShown) {
					keepName = tvTitle.getText().toString();
					retainFlag = true;
				}
				if (!isNew && !isReview && !isPropertyClicked
						&& !passedModeName.equals("")) {

					if (!(Graphbar.getProgress() != Graphbar.getMax())) {
						updateTimerandgraph();
					}

				}
				if (passedModeName.equals(DMApplication.MODE_REVIEW_RECORDING)) {
					if (timer != null || mptimer != null) {
						timer.cancel();
						task.cancel();
						mptimer.cancel();
						mpTimertask.cancel();
					}
				}
				if (isNew && !isFolderButtonTouched) {
					dmApplication.lastDictMailSent = true;
				}

				if (isReview) {
					if (Graphbar.getProgress() != Graphbar.getMax()
							&& Graphbar.getProgress() != 0) {
						SavedDicID = dictCard.getDictationId();
						passedModeName = "";
						Intent intent = getIntent();
						intent.putExtra(DMApplication.START_MODE_TAG,
								passedModeName);
						setIntent(intent);
					}
				}
				if (isReview && playButtonState == MediaMode.PAUSE) {
					if (timer != null || mptimer != null) {
						timer.cancel();
						task.cancel();
						mptimer.cancel();
						mpTimertask.cancel();
					}
					playButtonState = MediaMode.START;
				}
			}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		bitmap = null;
		prefs = PreferenceManager
				.getDefaultSharedPreferences(DictateActivity.this);
		SharedPreferences.Editor editor = prefs.edit();
		/**
		 * UNREGISTERING BROADCASTRECEIVER FOR DEVICELOCK
		 */
		if (!isBeyondLimit)
			if (!limitFlag)
				if (isRecording) {
					CancelNotification(this, RECORDING_NOTIFY_ID);
					recorderStopRecording();

					if (!isReview) {
						if (dictCard.getStatus() < 1) {
							dictCard.setStatus(DictationStatus.PENDING
									.getValue());
							mDbHandler.updateDictationStatus(
									dictCard.getDictationId(),
									dictCard.getStatus());
						}
					}
					editor.putBoolean(WAS_DESTROYED, true);
					editor.commit();
				} else {
					if (!isNew && !isReview) {
						dictCard.setStatus(DictationStatus.PENDING.getValue());
						mDbHandler
								.updateDictationStatus(
										dictCard.getDictationId(),
										dictCard.getStatus());
					}
					editor.putBoolean(WAS_DESTROYED, false);
					editor.commit();
				}

		this.unregisterReceiver(deviceLockInterrupt);
		this.unregisterReceiver(outGoingCallReciever);
		/**
		 * Avoid Memory leaks
		 */
		unbindDrawables(findViewById(R.id.dictate_root_view));
		System.gc();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.toggle_rec_over:

			if (isInsert) {
				process_type = 0;
				isInsert = false;
				if (!Graphbar.isShown()) {
					if (!isNew) {
						showOverwriteSignalometer();
					}
				} else {
					if (!isNew) {
						drawOverwriteGraph();
					}
				}
			} else {
				process_type = 1;
				isInsert = true;
				if (!Graphbar.isShown()) {
					if (!isNew) {
						showSignalometer();
					}
				} else {
					if (!isNew) {
						drawWaveGraph();
					}
				}
			}

			break;
		case R.id.b_play:
			if (!isCallActive) {
				tvTitle.setCursorVisible(false);
				if (isRecording) {
					if (ActivityInitialPlay) {
						ActivityInitialPlay = false;
					}
					recorderStopRecording();
					if (mWakeLock.isHeld()) {
						mWakeLock.release();
					}
					break;
				}
				if (playButtonState == MediaMode.PLAY) {
					if (mMediaPlayer.isPlaying()) {
						if (mMediaPlayer != null) {
							playButtonState = MediaMode.PAUSE;
							if (!isReview) {
								recorderSetEnable(true);
							} else {
								bNew.setEnabled(true);
								bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							}
							mMediaPlayer.pause();
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
							}
							OverwriteLimitExceeded = false;
							savedCurrentPos = getMMcurrentPos(Graphbar
									.getProgress());
							pausePosition = mMediaPlayer.getCurrentPosition();
							fileSize = getPlayBackFileSize();
							continue_process = false;
							bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
						}
					}
				} else if (playButtonState == MediaMode.PAUSE) {
					if (mMediaPlayer != null) {
						playButtonState = MediaMode.PLAY;
						recorderSetEnable(false);
						if (Graphbar.getProgress() == Graphbar.getMax()) {
							Graphbar.setProgress(0);
						}
						mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar
								.getProgress()));
						mMediaPlayer.start();
						bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
					}
				} else if (playButtonState == MediaMode.START) {
					mediaPlayerPrepareAudioFile(true);
					bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
					recorderSetEnable(false);
				}
			} else {
				DialogWhileActiveCall();
			}
			break;
		case R.id.new_rec:
			tvTitle.setCursorVisible(false);
			if(isPushToTalk && dictCard != null && isKeyboardShown) {
				hideKeyboard();
				if(!onSaveDictationName())
					return;
			}
			if (!isNew) {
				if (!isBeyondLimit)
					if (!limitFlag)
						if (!isReview) {
							dictCard.setStatus(DictationStatus.PENDING
									.getValue());
							mDbHandler.updateDictationStatus(
									dictCard.getDictationId(),
									dictCard.getStatus());
						}
				wasResume = false;
				dmApplication.fromWhere = 2;
				dmApplication.isExecuted = true;
				if (dictCard != null) {
					if (dictCard.getIsActive() == 1)
						oldDictExistFlag = true;
				}
				newRecordingClicked();
				setThumbnail();
			}
			break;
		case R.id.sound_fav:
			if (imFav.isChecked()) {
				if (dictCard != null) {
					dictCard.setPriority(1);
					mDbHandler.updatePriority(dictCard);
				}
			} else {
				if (dictCard != null) {
					dictCard.setPriority(0);
					mDbHandler.updatePriority(dictCard);
				}
			}
			break;
		case R.id.record:
			if (timer != null || mptimer != null) {
				timer.cancel();
				task.cancel();
				mptimer.cancel();
				mpTimertask.cancel();
			}

			if (isRecording) {
				isrecordingOnclick = false;
				if (ActivityInitialPlay) {
					ActivityInitialPlay = false;
				}
				if (!isVcvaEnabled) {
					if (passedTotalDuration >= 1000) {
						tvTitle.setCursorVisible(false);
						recorderStopRecording();
						bPlay.setBackgroundResource(R.drawable.dictate_play_selector);

						hideKeyboard();
					}
				} else {
					if (passedTotalDuration >= 1000) {
						recorderStopRecording();
						bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
					}
				}
				getWindow().getDecorView().findViewById(android.R.id.content)
						.setKeepScreenOn(false);

				if (mWakeLock.isHeld()) {
					mWakeLock.release();
				}

			} else {
				getWindow().getDecorView().findViewById(android.R.id.content)
						.setKeepScreenOn(true);
				if (mWakeLock.isHeld()) {
					mWakeLock.acquire();
				}
				isrecordingOnclick = true;
				if (!isCallActive) {
					if (!isVcvaEnabled) {
						if (isInsert) {
							showSignalometer();
						} else {
							showOverwriteSignalometer();
						}
					}
				}
				if (mMediaPlayer == null) {
					ActivityInitialPlay = true;
				}
				tvTimeRight.setVisibility(ProgressBar.VISIBLE);
				tvTimeRight.setVisibility(ProgressBar.VISIBLE);
				if (isOncePaused) {
					if (!isInsertionProcessRunning) {
						if (!isCallActive) {
							hideKeyboard();
							tvTitle.setCursorVisible(false);
							if (validateSpaceAndSizeLimit()) {
								recorderTriggerStart();
							}

						} else {
							DialogWhileActiveCall();

						}
					}
				} else {
					if (!isCallActive) {
						hideKeyboard();
						tvTitle.setCursorVisible(false);
						if (validateSpaceAndSizeLimit()) {
							recorderTriggerStart();
						}

					} else {
						DialogWhileActiveCall();
					}
				}

			}
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_HOME) {
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Enables and disables title,Subtitle,worktype and favorite views.
	 * 
	 * @param isEnabled
	 *            decides the views should be enabled or disabled.
	 */
	private void enableDictationDatas(boolean isEnabled) {
		if (isEnabled) {
			tvSubtitle.setEnabled(true);
			tvTitle.setEnabled(true);
			tvWorktype.setEnabled(true);
			imFav.setEnabled(true);
		} else {
			tvSubtitle.setEnabled(false);
			tvTitle.setEnabled(false);
			tvWorktype.setEnabled(false);
			imFav.setEnabled(false);
		}
	}

	/**
	 * Enable and disable play and rewind buttons.
	 * 
	 * @param isEnabled
	 *            Decide views should be enabled or disabled.
	 */
	private void enableEverything(boolean isEnabled) {
		if (isEnabled) {

			bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind);
			bPlay.setEnabled(true);
			bRewind.setEnabled(true);
		} else {

			bRecord.setBackgroundResource(R.drawable.dictate_recording);
			bPlay.setBackgroundResource(R.drawable.dictate_pause_sel);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
			bPlay.setEnabled(false);
			bRewind.setEnabled(false);
		}
	}
	/**
	 * Save edited dictation name.
	 * @return isSuccees
	 */
	private boolean onSaveDictationName() {
		String title = tvTitle.getText().toString().trim();
		if (!title.equalsIgnoreCase(dictCard.getDictationName())) 
			return validateDictationName(title);
		else {
			tvTitle.setText(title);
			tvTitle.setCursorVisible(false);
			dictCard.setDictationName(title);
			mDbHandler.updateDictName(dictCard.getDictationName(), dictCard.getDictationId());
		}
		return true;
	}

	/**
	 * Hides the soft keyboard if its shown.
	 */
	private void hideKeyboard() {
		if (isKeyboardShown) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}
	}

	/**
	 * Deletes current active recording.
	 * 
	 * @param path
	 *            File path where the dictation is saved.
	 */
	private void DeleteCurrentRecording(String path) {
		final String mfile = path;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.Record_Save_Actionsheet_Message)
				.setCancelable(false)
				.setPositiveButton(R.string.Button_Discard,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
//								if(dmApplication.getDeletedId()==0)
								mDbHandler.deleteDictation(dictCard);
								boolean deleted = DMApplication.deleteDir(mfile);
								if (deleted) {
									wasResume = false;
									newRecordingClicked();
								}
							}
						})
				.setNegativeButton(R.string.Recording_Alerts_Cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onBackPressed() {
		if (!isInsertionProcessRunning) {
			if (isRecording) {
				if (!isOncePaused) {
					if (passedTotalDuration > 1000) {
						super.onBackPressed();
					}
					isBackpressed = true;
					dmApplication.onSetPending = false;
					if (!isBeyondLimit)
						if (!limitFlag)
							if (!isNew && !isReview) {
								dictCard.setStatus(DictationStatus.PENDING
										.getValue());
								mDbHandler.updateDictationStatus(
										dictCard.getDictationId(),
										dictCard.getStatus());
							}
					if (isNew) {
						if (isInsert) {
							tbOverwrite.performClick();
						}
					} else {
						if (isInsert) {
							isInsert = false;
						}
					}
				}
			} else {
				super.onBackPressed();
				if (passedTotalDuration > 1000) {
					super.onBackPressed();
				}
				isBackpressed = true;
				dmApplication.onSetPending = false;
				if (!isBeyondLimit)
					if (!limitFlag)
						if (!isNew && !isReview) {
							dictCard.setStatus(DictationStatus.PENDING
									.getValue());
							mDbHandler.updateDictationStatus(
									dictCard.getDictationId(),
									dictCard.getStatus());
						}
				if (isNew) {
					if (isInsert) {
						tbOverwrite.performClick();
					}
				} else {
					if (isInsert) {
						isInsert = false;
					}
				}
			}
		}

		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()) {
				mediaPlayerDistroy();
			}
		}
	}

	/**
	 * Checks whether dictation exists or not .
	 * 
	 * @param auth
	 *            Author name.
	 * @param seqNo
	 *            Sequence number of Dictation.
	 */
	public void checkDictationExists(String auth, int seqNo) {
		String formattedSeqNumber = String.format("%04d", seqNo);
		mDictationName = auth + formattedSeqNumber;
		boolean isDictExist = mDbHandler.checkDictNameOrIdExists(
				mDictationName, seqNo);
		if (seqNo <= 9999) {
			if (isDictExist) {
				checkDictationExists(auth, seqNo + 1);
			} else {
				dictCount = 0;
				mDictationName = auth + formattedSeqNumber;
				seqNumber = seqNo;
			}
		} else {
			isBeyondLimit = true;
			new CheckAvailableDictationSlotTask(this).execute();
		}
	}

	/**
	 * Initializes DictationData when new Dictation is created.
	 */
	public void initializeDictationData() {
		dmApplication.isExecuted = false;
		limitFlag = false;
		isBeyondLimit = false;
		Cursor cur = mDbHandler.getNewDictationExisting();
		mSharedPreferenece = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		Intent intent = getIntent();
		intent.putExtra(DMApplication.START_MODE_TAG,
				DMApplication.MODE_NEW_RECORDING);
		setIntent(intent);

		if (cur.moveToFirst()) {
			dictCard = mDbHandler.getSelectedDicts(cur);
			cur.close();
			seqNumber = dictCard.getDictationId();
			dictCard.setIsActive(1);
			mDbHandler.updateAuthor(dictCard);
			mDictationDir = new File(
					DMApplication.DEFAULT_DIR.getAbsolutePath() + "/"
							+ AUDIO_FILE_FOLDERNAME + "/"
							+ dictCard.getSequenceNumber());
			if (!mDictationDir.exists())
				mDictationDir.mkdirs();
			AUDIO_FILE_NAME = dictCard.getDictFileName();

		} else {

			seqNumber = mSharedPreferenece.getInt(SEQ_NUMBER_KEY, 0000) + 1;
			if (seqNumber > 99999999) {
				seqNumber = 0;
			}

			sNumber = seqNumber % 10000;
			String formattedSeqNumber = String.format("%04d", sNumber);
			mAuthor = mSharedPreferenece.getString(
					getResources().getString(R.string.author_key), "AUTH");
			mAuthorName = mSharedPreferenece.getString(
					getResources().getString(R.string.author_key), "AUTHOR");
			if(mAuthorName.trim().length() == 0) 
				mAuthorName = "AUTHOR";
			if (mAuthor.length() > 4)
				mAuthor = mAuthor.substring(0, 4);
			else if (mAuthor.length() == 0)
				mAuthor = "AUTH";
			mDictationName = mAuthor + formattedSeqNumber;
			dictCount = 0;
			if (seqNumber <= 9999) {
				checkDictationExists(mAuthor, seqNumber);
			} else {
				isBeyondLimit = true;
				new CheckAvailableDictationSlotTask(this).execute();
			}
			if (!isBeyondLimit)
				initializeDictationCard();

		}
		if (!isBeyondLimit) {
			if (!limitFlag) {
				Editor edit = mSharedPreferenece.edit();
				edit.putInt(SEQ_NUMBER_KEY, seqNumber);
				edit.commit();

				setViewStates(dictCard);
			}
		}

	}

	/**
	 * DictationCard will be initialized based on the Dictation data.
	 */
	public void initializeDictationCard() {
		if (!limitFlag) {
			AUDIO_FILE_NAME = mDictationName;
			mDictationDir = new File(
					DMApplication.DEFAULT_DIR.getAbsolutePath() + "/"
							+ AUDIO_FILE_FOLDERNAME + "/" + seqNumber);
			if (!mDictationDir.exists())
				mDictationDir.mkdirs();
			dictCard = new DictationCard();
			dictCard.setDictationId(seqNumber);
			dictCard.setDictationName(mDictationName);
			dictCard.setDictFileName(mDictationName);
			dictCard.setAuthor(mAuthorName);
			dictCard.setStatus(DictationStatus.NEW.getValue());
			dictCard.setPriority(0);
			dictCard.setIsActive(1);
			dictCard.setWorktype("");
			dictCard.setSequenceNumber(seqNumber);
			mDbHandler.insertDictation(dictCard);
		} else {

			AlertDialog.Builder alert = new AlertDialog.Builder(
					DictateActivity.this);
			alert.setCancelable(false);
			alert.setTitle(getResources().getString(R.string.Alert));
			alert.setMessage("Cannot create dictation name, please delete some dictations and try again");
			alert.setNegativeButton(
					getResources().getString(R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							if (dmApplication.fromWhere == 1) {
								dmApplication.fromWhere = 0;
								finish();
							} else if (dmApplication.fromWhere == 3
									|| dmApplication.fromWhere == 4) {
								dmApplication.fromWhere = 0;
								finish();
								Intent intentFolder = new Intent(
										DictateActivity.this, DMActivity.class);
								intentFolder
										.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
								startActivity(intentFolder);
								dmApplication
										.setRecordingsClickedDMActivity(false);

							}
							if (oldDictExistFlag) {
								oldDictExistFlag = false;
								dictCard.setIsActive(1);
								mDbHandler.updateIsActive(dictCard);

								updateTimerandgraph();
								mediaPlayerPrepareAudioFile(false);
							}
							dictCard = null;
						}
					});
			alert.create().show();
		}

		if (!limitFlag) {
			Editor edit = mSharedPreferenece.edit();
			edit.putInt(SEQ_NUMBER_KEY, seqNumber);
			edit.commit();

			setViewStates(dictCard);

		}
	}

	/**
	 * This AsyncTask is used to find available Dictation slot if the Dictation
	 * sequence number reaches 9999.
	 * 
	 */
	private class CheckAvailableDictationSlotTask extends
			AsyncTask<Void, Void, Void> {

		Context context;
		ProgressDialog dialog;

		public CheckAvailableDictationSlotTask(Context context) {
			this.context = context;
			// continueFlag = false;
		}

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(context);
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			ArrayList<String> dNames = mDbHandler.getDictationNames();
			for (int i = 0; i <= 9999; i++) {
				int sNum = seqNumber % 10000;
				mDictationName = mAuthor + String.format("%04d", sNum);
				boolean isContain = dNames.contains(mDictationName
						.toLowerCase());
				if (isContain)
					seqNumber++;
				else
					break;
				if (i == 9999)
					limitFlag = true;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			initializeDictationCard();
			if (dmApplication.fromWhere == 2) {
				isNew = false;
				dmApplication.fromWhere = 0;
			}
			if (!limitFlag)
				initializeRecorderViews();
			if (wasResume)
				if (!limitFlag) {
					Cursor c = mDbHandler.getDictationWithId(dictCard
							.getDictationId());
					if (c.moveToFirst())
						dictCard = mDbHandler.getSelectedDicts(c);
					c.close();
				}
			dialog.dismiss();
		}

	}

	/**
	 * Sets values to the views.
	 * 
	 * @param dCard
	 *            Dictation details as DictationCard.
	 */
	public void setViewStates(DictationCard dCard) {
		tvTitle.setEnabled(true);
		tvWorktype.setEnabled(true);
		imFav.setEnabled(true);
		tvTitle.setText(dCard.getDictationName());
		tvTitle.setCursorVisible(false);
		if (dCard.getWorktype() == null || dCard.getWorktype().equals(""))
			tvWorktype.setText(getResources().getString(
					R.string.Settings_Select));
		else
			tvWorktype.setText(dCard.getWorktype());
		int prio = dCard.getPriority();
		switch (prio) {
		case 0:
			imFav.setChecked(false);
			break;
		case 1:
			imFav.setChecked(true);
			break;
		default:
			break;
		}
		imPhoto.setImageResource(R.drawable.dictate_cam);

	}

	/**
	 * Invokes when Send button is Tapped. Send Dialog will be shown based on
	 * the Send option in an Application Settings.
	 */
	private void prompSendDialog() {
		pref = PreferenceManager
				.getDefaultSharedPreferences(DictateActivity.this);
		final int mSendOptions = Integer.parseInt(pref.getString(getResources()
				.getString(R.string.send_key), "1"));
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder alert = new AlertDialog.Builder(
				DictateActivity.this);
		View layout = inflater.inflate(R.layout.dialog_send_default_view,
				(ViewGroup) this.findViewById(R.id.view_send_dialog));
		Button send = (Button) layout.findViewById(R.id.btn_send_dialog_send);
		Button pending = (Button) layout
				.findViewById(R.id.btn_send_dialog_pending);
		Button cancel = (Button) layout
				.findViewById(R.id.btn_send_dialog_cancel);
		if (mSendOptions == 1)
			send.setText(getResources().getString(R.string.Button_Sendviaemail));
		else if (mSendOptions == 2)
			send.setText(getResources().getString(R.string.Button_Sendtosever));
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSendDialog.dismiss();
				if (mSendOptions == 2) {
					pref = DictateActivity.this.getSharedPreferences(
							PREFS_NAME, 0);
					mSettingsConfig = pref.getString("Activation", mActivation);
					if (mSettingsConfig != null
							&& !mSettingsConfig
									.equalsIgnoreCase("Not Activated"))
						if (DMApplication.isONLINE()) {
							getSettingsAttribute();
							new WebServiceGetSettings().execute();
						} else {
							onSetRecentSettings();
							if (mAudioDelivery.trim().equalsIgnoreCase("3"))
								promptSendRecordings();
							else {
								if (mAudioDelivery.equalsIgnoreCase("1"))
									isSplittable = 1;
								else
									isSplittable = 0;
								onAfterSettingsDelivery();
							}
						}
					else {
						alertDialog = new AlertDialog.Builder(
								DictateActivity.this);
						alertDialog.setTitle(getResources().getString(
								R.string.Ils_Result_Not_Activated));
						alertDialog.setMessage(getResources().getString(
								R.string.Flashair_Alert_Activate_Account));
						alertDialog.setPositiveButton(
								getResources().getString(
										R.string.Dictate_Alert_Ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});
						alertDialog.create().show();
					}
				} else if (mSendOptions == 1)
					onUploadHandler(false);
			}
		});
		pending.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dictCard.setStatus(DictationStatus.PENDING.getValue());
				dictCard.setRecEndDate(dmApplication.getDeviceTime());
				mDbHandler.updateDictationStatusAndEndDate(dictCard);
				mSendDialog.dismiss();
			}
		});
		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mSendDialog.dismiss();
			}
		});
		alert.setView(layout);
		mSendDialog = alert.create();
		mSendDialog.show();
	}

	/**
	 * Shows notification message, when Encryption's for the dictation in server
	 * is not defined or zero.
	 */
	private void promptSecurityDialog() {

		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder alert = new AlertDialog.Builder(
				DictateActivity.this);
		alert.setCancelable(false);
		View layout = inflater.inflate(R.layout.security_alert_dialog,
				(ViewGroup) this.findViewById(R.id.rel_security_dialog));
		final CheckBox checkBox = (CheckBox) layout
				.findViewById(R.id.check_dont_show);

		alert.setPositiveButton(
				getResources().getString(R.string.Dictate_Alert_Ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mSharedPreferenece = PreferenceManager
								.getDefaultSharedPreferences(DictateActivity.this);
						editor = mSharedPreferenece.edit();
						editor.putBoolean(DONT_SHOW_KEY, checkBox.isChecked());
						editor.commit();
						dialog.dismiss();
						if (hasMultipleSplits)
							onSendToServerEmailHasAbove23MB();
					}
				});
		alert.setView(layout);
		mSendDialog = alert.create();
		mSendDialog.show();
	}

	@Override
	public void updateGraph(long duration) {
		passedTotalDuration = duration;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				vcvaStopDetection();
			}
		});
	}

	/**
	 * When recording happens callback comes here. Singnalometer and timer
	 * updation while recording is done here.
	 * 
	 * @param status
	 * @param cAmplitude
	 * @param current_duration
	 *            Duration of the temp file recorded till the current second.
	 * @param totalDuration
	 *            Total duration of the file
	 */
	@Override
	public void updateProgress(boolean status, int cAmplitude,
			long current_duration, long totalDuration) {
		//System.out.println("cur time"+current_duration + "total duration "+totalDuration);
		final int temp = cAmplitude;
		passedTotalDuration = totalDuration;
		recorderUpdateTimer(current_duration, temp);
		if (isNew) {
			if (totalDuration == 1000) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (isRecording && !isPushToTalk) {
							enableEverything(true);
						}
						if (isPushToTalk) {
							if (brecordisRecording) {
							} else {
								recorderStopRecording();
								bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
							}
						}
					}
				});
			} else if (totalDuration <= 999) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						enableEverything(false);
					}
				});
			}
		}
		runOnUiThread(new Runnable() {
			public void run() {
				if (isRecording) {
					if (isVcvaEnabled) {
						vcvaStartDetection();
					}
					if (isInsert) {
						mSignalometer.setProgress(temp);
					} else {
						mOverwriteSignalometer.setProgress(temp);

					}
				}
			}
		});
	}

	/**
	 * Dictation Property View will be triggered.
	 */
	public void triggerDictationProperty() {
		if ((Graphbar.getProgress() != Graphbar.getMax())) {
			SavedDicID = dictCard.getDictationId();
			passedModeName = "";
		}

		if (isNew && !isFolderButtonTouched) {
			dmApplication.lastDictMailSent = true;
		}
		Intent propertyIntent = new Intent(this,
				DictationPropertyActivity.class);
		propertyIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		propertyIntent.putExtra(DMApplication.START_MODE_TAG, passedModeName);
		propertyIntent.putExtra(DMApplication.ACTIVITY_MODE_TAG, "dictate");
		propertyIntent.putExtra(DMApplication.DICTATION_ID,
				dictCard.getDictationId());

		startActivity(propertyIntent);
	}

	/**
	 * All the functionalities and UI updations while graph seekbar progress
	 * changes.
	 * 
	 * @param seekBar
	 *            seekbar view.
	 * @param progress
	 *            current progress of seekbar.
	 * @param fromTouch
	 *            user touched or not.
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch) {

		if (Graphbar.getMax() != progress && progress != 0) {
			if (isRewinding != 1 && isForwarding != 1) {
				enableRewindandForward();
			}
		} else {
			if (Graphbar.getMax() == progress) {
				disableForward();
			} else if (progress == 0) {
				disableRewind();
			}
		}

		if (mMediaPlayer != null) {
			if (!mMediaPlayer.isPlaying()) {
				mediaPlayerUpdateTimer(getMMcurrentPos(Graphbar.getProgress()));
			}
		} else if (mMediaPlayer == null) {
			tvTimeLeft.setText(""
					+ Utilities.getDurationInTimerFormat(Graphbar.getProgress()
							* amoungToupdate));
		}
	}

	/**
	 * User touch's it starts tracking and callback comes here.
	 */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

		if (isVcvaEnabled && isRecording) {
			if (ActivityInitialPlay) {
				ActivityInitialPlay = false;
			}
		}
		if (isRecording) {
			recorderStopRecording();
			bRecord.setBackgroundResource(R.drawable.dictate_record_selector);
		}
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
			}

		}
	}

	/**
	 * User stops touching it stops tracking and callback comes here.
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (Graphbar.getProgress() < Graphbar.getMax()
				&& Graphbar.getProgress() >= 0) {
			OverwriteLimitExceeded = false;
			isOncePaused = true;
		}
		savedCurrentPos = getMMcurrentPos(Graphbar.getProgress());
		continue_process = false;
		int currentPosition = seekBar.getProgress();
		seekBar.setProgress(currentPosition);
		pausePosition = getMMcurrentPos(currentPosition);
		savedCurrentPos = getMMcurrentPos(currentPosition);
		mediaPlayerUpdateTimer(getMMcurrentPos(currentPosition));
		if (playButtonState == MediaMode.PLAY) {
			mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
			mMediaPlayer.start();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

	}

	@Override
	protected void onResume() {
		super.onResume();
		isNavigatedToAnotherScreen = false;
		checkForwordIsEnabled = false;
		DictationPropertyActivity.ComingFromRecordings = false;
		audioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		isForwarding = 0;
		isRewinding = 0;
		rewindPressed = false;
		OverwriteLimitExceeded = false;
		isFolderButtonTouched = false;
		Graphbar.setMax(500);
		setCurrentLanguage(dmApplication.getCurrentLanguage());
		if (isPushToTalk) {
			bRecord.setOnTouchListener(null);
		} else {
			bRecord.setOnClickListener(null);
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		vcvaPref = PreferenceManager.getDefaultSharedPreferences(this);
		isVcvaEnabled = getVcvaState();
		isPushToTalk = getPushToTalkState();
		isBackpressed = false;
		IsOtherRecorderActive = false;
		isPropertyClicked = false;

		if (!isRecording && passedModeName != "") {
			isOncePaused = false;
		}

		if (isPushToTalk) {
			if (mMediaPlayer != null) {
				if (!mMediaPlayer.isPlaying() && !isReview) {
					bRecord.setBackgroundResource(R.drawable.dictate_record);
				}
			}
			bRecord.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {

					switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:

						if (timer != null || mptimer != null) {
							timer.cancel();
							task.cancel();
							mptimer.cancel();
							mpTimertask.cancel();
						}
						getWindow().getDecorView()
								.findViewById(android.R.id.content)
								.setKeepScreenOn(true);
						if (mWakeLock.isHeld()) {
							mWakeLock.acquire();
						}
						tvTitle.setCursorVisible(false);

						tvTimeRight.setVisibility(View.VISIBLE);

						brecordisRecording = true;

						if (validateSpaceAndSizeLimit()) {
							if (!isCallActive) {
								if (!isInsertionProcessRunning) {
									if (isInsert) {
										showSignalometer();
									} else {
										showOverwriteSignalometer();
									}
									recorderTriggerStart();
								}
							} else {
								DialogWhileActiveCall();
							}
						}
						break;

					case MotionEvent.ACTION_UP:
						getWindow().getDecorView()
								.findViewById(android.R.id.content)
								.setKeepScreenOn(false);
						brecordisRecording = false;
						if (isRecording) {
							ActivityInitialPlay = false;
							if ((passedTotalDuration > 1000) && !isOncePaused) {
								recorderStopRecording();
							} else if (isOncePaused || !isNew) {
								recorderStopRecording();
							}
							if (passedTotalDuration > 1000) {
								mediaPlayerSetEnable(true);
								mediaPlayerUpdatebottomBar(true);
							}
						}
						break;
					}
					return false;
				}
			});
		} else {
			bRecord.setOnClickListener(this);
		}
		dmApplication.setFlashAirState(false);

		pmPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		pmWakeLock = pmPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"DoNotDimScreen");

		if (isRecording) {
			CancelNotification(this, RECORDING_NOTIFY_ID);
		}
		if (!isRecording) {
			passedModeName = getIntent().getStringExtra(DMApplication.START_MODE_TAG);
			try {
				if (!dmApplication.passMode.equals(DMApplication.MODE_COPY_RECORDING)
						&& !dmApplication.passMode.equals(DMApplication.MODE_REVIEW_RECORDING)) {
					if (!isNew
							&& (Graphbar.getProgress() == Graphbar.getMax() || Graphbar.getProgress() == 0)) {
						if (passedModeName.equals(""))
							passedModeName = dmApplication.passMode;
					}
				}
			} catch (Exception e) {
			}
			if (passedModeName != null) {
				if (!passedModeName.equals(DMApplication.MODE_NEW_RECORDING)) {
					if (!dmApplication.lastDictMailSent) {
						Bundle bundle = getIntent().getExtras();
						int id = bundle.getInt(DMApplication.DICTATION_ID);
						dictCard = mDbHandler.getDictationCardWithId(id);
						if(dictCard != null && dictCard.isResend() == 1) {
							isNew = false;
							isReview = true;
							passedModeName = DMApplication.MODE_REVIEW_RECORDING;
						}
					}
					else {
						dmApplication.lastDictMailSent = false;
						passedModeName = DMApplication.MODE_NEW_RECORDING;
						Intent intent = getIntent();
						intent.putExtra(DMApplication.START_MODE_TAG, passedModeName);
						setIntent(intent);
					}
				}
			} else {

				if (!InterruptCallDialogshown) {
					passedModeName = "";
					Bundle bundle = getIntent().getExtras();
					int id = bundle.getInt(DMApplication.DICTATION_ID);
					dictCard = mDbHandler.getDictationCardWithId(id);
				}
				if (InterruptCallDialogshown) {
					CancelNotification(this, RECORDING_NOTIFY_ID);
				}

			}
			if (playmodeRewindFrwdworking) {
				mMediaPlayer.start();
				playmodeRewindFrwdworking = false;
			}
			if (dmApplication.flashair) {
				dmApplication.flashair = false;
				passedModeName = DMApplication.MODE_EDIT_RECORDING;
			}
			if (passedModeName != null) {
				if (passedModeName.equals(DMApplication.MODE_NEW_RECORDING)) {
					wasResume = true;
					initNewDictation();
					isReview = false;

				} else if (passedModeName
						.equals(DMApplication.MODE_EDIT_RECORDING)
						|| passedModeName
								.equals(DMApplication.MODE_COPY_RECORDING)) {
					if (timer != null || mptimer != null) {
						timer.cancel();
						task.cancel();
						mptimer.cancel();
						mpTimertask.cancel();
					}
					if (mDbHandler.isDictationExists(dictCard
							.getDictationName())) {
						isReview = false;
						if (!IsAlreadyActive) {
							isOncePaused = false;
						}
						dictCard.setIsActive(1);
						mDbHandler.updateIsActive(dictCard);
						dictCard.setIsConverted(0);
						mDbHandler.updateIsConverted(dictCard);
						dictCard.setFilesList(null);

						if (dmApplication.isRecordingsClickedDMActivity()) {
							if (!IsAlreadyActive) {
								if (isInsert) {
									tbOverwrite.performClick();
								}
							}
						} else if (passedModeName
								.equals(DMApplication.MODE_COPY_RECORDING)) {
							if (isInsert) {
								tbOverwrite.performClick();
							}

						}

						mDbHandler.deleteFileList(dictCard);
						if (passedModeName
								.equals(DMApplication.MODE_COPY_RECORDING)) {
							dictCard.setStatus(DictationStatus.NEW.getValue());
							mDbHandler.updateDictationStatus(
									dictCard.getDictationId(),
									dictCard.getStatus());
						}
						AUDIO_FILE_NAME = dictCard.getDictFileName();
						mDictationDir = new File(
								DMApplication.DEFAULT_DIR.getAbsolutePath()
										+ "/" + AUDIO_FILE_FOLDERNAME + "/"
										+ dictCard.getSequenceNumber());
						setViewStates(dictCard);
						mediaPlayerSetEnable(true);

						isNew = false;
						bNew.setEnabled(true);
						tbOverwrite.setEnabled(true);
						tbOverwrite.setBackgroundResource(R.drawable.toggle);
						bNew.setBackgroundResource(R.drawable.dictate_new_selector);
						if (isInsert) {
							drawWaveGraph();
						} else {
							drawOverwriteGraph();
						}

						enableDictationDatas(true);
						updateTimerandgraph();

						if (!IsAlreadyActive) {
							Graphbar.setProgress(Graphbar.getMax());
						}
						if(Graphbar.getProgress()<1){
							disableRewind();
						}
						mediaPlayerPrepareAudioFile(false);
						bRecord.setEnabled(true);
						if (!isPushToTalk) {
							bRecord.setBackgroundResource(R.drawable.dictate_record_selector);
						} else {
							bRecord.setBackgroundResource(R.drawable.dictate_record);
						}
						imbSend.setEnabled(true);
						imbDelete.setEnabled(true);
						if (!bRecord.isEnabled()) {
							bRecord.setEnabled(true);
						}
						imbSend.setImageResource(R.drawable.dictate_send_selector);
						imbDelete
								.setImageResource(R.drawable.dictate_delete_selector);

						if (passedModeName
								.equals(DMApplication.MODE_COPY_RECORDING)
								&& dmApplication.isPropertyClicked()) {
							Graphbar.setProgress(Graphbar.getMax());
							dmApplication.setPropertyClicked(false);
							triggerDictationProperty();
							isPropertyClicked = true;
						}
					} else {
						setPassedModeToNew();
					}
					tvTimeRight.setVisibility(View.VISIBLE);
				} else if (passedModeName
						.equals(DMApplication.MODE_REVIEW_RECORDING)) {
					if (timer != null || mptimer != null) {
						timer.cancel();
						task.cancel();
						mptimer.cancel();
						mpTimertask.cancel();
					}
					bForward.setEnabled(false);
					tvTimeRight.setVisibility(View.VISIBLE);
					isReview = true;
					if (dictCard != null) {
						if (mDbHandler.isDictationExists(dictCard
								.getDictationName())) {
							if (isInsert) {
								tbOverwrite.performClick();
							}
							dictCard.setIsActive(1);
							mDbHandler.updateIsActive(dictCard);
							AUDIO_FILE_NAME = dictCard.getDictFileName();
							mDictationDir = new File(
									DMApplication.DEFAULT_DIR.getAbsolutePath()
											+ "/" + AUDIO_FILE_FOLDERNAME + "/"
											+ dictCard.getSequenceNumber());
							setViewStates(dictCard);
							isNew = false;
							if (isInsert) {
								drawWaveGraph();
							} else {
								drawOverwriteGraph();
							}

							tbOverwrite.setEnabled(false);
							tbOverwrite
									.setBackgroundResource(R.drawable.toggle_disble);
							updateTimerandgraph();
							enableDictationDatas(false);
							mediaPlayerPrepareAudioFile(false);
							bRecord.setEnabled(false);
							imbSend.setEnabled(false);
							imbDelete.setEnabled(false);
							imbSend.setImageResource(R.drawable.dictate_send_sel);
							imbDelete
									.setImageResource(R.drawable.dictate_delete_sel);
							bRecord.setBackgroundResource(R.drawable.dictate_record_sel);
							bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							bNew.setEnabled(true);
							bPlay.setEnabled(true);
							bRewind.setEnabled(true);
							Graphbar.setProgress(Graphbar.getMax());
							if (dmApplication.isPropertyClicked()) {
								dmApplication.setPropertyClicked(false);
								triggerDictationProperty();
							}
						} else if (passedModeName.equals("")) {
							dictCard = mDbHandler
									.getDictationCardWithId(dictCard
											.getDictationId());
							setViewStates(dictCard);

						} else {
							setPassedModeToNew();
						}

					} else {
						Cursor cur = mDbHandler.checkActiveDictationExists();
						if (cur == null) {
							setPassedModeToNew();
						}
					}
				} else if (passedModeName.equals("")) {
					if (IsAlreadyActive) {
						if (!dmApplication.lastDictMailSent) {
							Bundle bundle = getIntent().getExtras();
							int id = bundle.getInt(DMApplication.DICTATION_ID);
							dictCard = mDbHandler.getDictationCardWithId(id);

							AUDIO_FILE_NAME = dictCard.getDictFileName();
							mDictationDir = new File(
									DMApplication.DEFAULT_DIR.getAbsolutePath()
											+ "/" + AUDIO_FILE_FOLDERNAME + "/"
											+ dictCard.getSequenceNumber());
						} else {
							dmApplication.lastDictMailSent = false;
						}
					} else {
						dictCard = mDbHandler.getDictationCardWithId(SavedDicID);
					}
					setViewStates(dictCard);
					if (isReview) {
						tvTitle.setEnabled(false);
						tvWorktype.setEnabled(false);
						imFav.setEnabled(false);
					}
				}
				if (passedModeName.equals(DMApplication.MODE_COPY_RECORDING)) {
					Graphbar.setProgress(Graphbar.getMax());
				}
			}

		}
		if (dmApplication.outBoxFlag) {
			if (isInsert) {
				tbOverwrite.performClick();
			}
			if (isInsert) {
				drawWaveGraph();
			} else {
				drawOverwriteGraph();
			}
			dmApplication.outBoxFlag = false;
		}
		if (retainFlag) {

			tvTitle.setText(keepName);
			tvTitle.setSelection(keepName.length());
			tvTitle.requestFocus();
			tvTitle.performClick();
			retainFlag = false;
			keyboardFlag = true;
		}

		if (!isBeyondLimit)
			if (!limitFlag) {
				Cursor c = mDbHandler.getDictationWithId(dictCard
						.getDictationId());
				if (c.moveToFirst())
					dictCard = mDbHandler.getSelectedDicts(c);
				c.close();
			}
		// To set thumbnail
		setThumbnail();

		if (fromShake) {

			if (!IsAlreadyActive) {
				if (isInsert) {
					tbOverwrite.performClick();
				}
			}
			if (!isNew && !isReview) {
				bRecord.setEnabled(true);
			}
			if (!isReview && !isPushToTalk) {
				bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
				bRecord.performClick();
				if (passedTotalDuration > 1000) {
					bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
					bRewind.setBackgroundResource(R.drawable.dictate_rewind_selector);
					bPlay.setEnabled(true);
					bRewind.setEnabled(true);
				}
			} else if (isPushToTalk) {
				if (!isNew) {
					if (isInsert) {
						drawWaveGraph();
					} else {
						drawOverwriteGraph();
					}
					if (timer != null || mptimer != null) {

						timer.cancel();
						task.cancel();
						mptimer.cancel();
						mpTimertask.cancel();
					}
					updateTimerandgraph();
					mediaPlayerPrepareAudioFile(false);
				}
			}
			fromShake = false;
		}
		if (dictCard != null)
			debugDictName = dictCard.getDictationName();

		if (dmApplication.isWantToShowDialog()
				&& dmApplication.isTimeOutDialogOnFront()) {
			baseIntent = new Intent(dmApplication, CustomDialog.class);
			baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(baseIntent);
			baseIntent = null;
		}
		dmApplication.setWantToShowDialog(false);
	}

	/**
	 * Invokes when new dictation is clicked from Recordings view.
	 */
	private void initNewDictation() {
		if (isIntentPlay) {
			newRecordingClicked();
		} else {
			isIntentPlay = true;
		}
	}

	/**
	 * Sets the thumbnail image if it is available.
	 */
	private void setThumbnail() {
		/* To set thumbnail */
		if (dictCard != null && imPhoto != null) {
			File imgFile = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
					+ dictCard.getSequenceNumber() + "/"
					+ dictCard.getDictFileName() + ".jpg");
			if (dictCard.getIsThumbnailAvailable() == 1 && imgFile.exists()) {

				imPhoto.setImageBitmap(decodeSampledBitmapFromResource(
						imgFile.getAbsolutePath(), imPhoto.getWidth(),
						imPhoto.getHeight()));
				imgFile = null;
			} else
				imPhoto.setImageResource(R.drawable.dictate_cam);
		}
	}

	/**
	 * Mode will be changed to New.
	 */
	private void setPassedModeToNew() {
		passedModeName = DMApplication.MODE_NEW_RECORDING;
		Intent intent = getIntent();

		intent.putExtra(DMApplication.START_MODE_TAG, passedModeName);
		setIntent(intent);
		wasResume = true;
		initNewDictation();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (!isOnceWindowFocusChanged) {
			if (dictCard != null && imPhoto != null) {
				File imgFile = new File(DMApplication.DEFAULT_DIR
						+ "/Dictations/" + dictCard.getSequenceNumber() + "/"
						+ dictCard.getDictFileName() + ".jpg");
				if (dictCard.getIsThumbnailAvailable() == 1 && imgFile.exists()) {
					imPhoto.setImageBitmap(decodeSampledBitmapFromResource(
							imgFile.getAbsolutePath(), imPhoto.getWidth(),
							imPhoto.getHeight()));
				}
			}
			isOnceWindowFocusChanged = true;
		}
	}

	public long getBytePosition(long ms, long totaldur, long fileSize) {
		long bytePos = 0;
		try {
			bytePos = (long) (((float) fileSize * (float) ms) / (float) totaldur);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bytePos;
	}

	/**
	 * Calculates the size of Audio file.
	 * 
	 * @return File size as long value.
	 */
	public long getPlayBackFileSize() {
		File playFile = new File(getFilename());
		if (playFile.exists()) {
			return (playFile.length() - 44);
		} else {
			return -1; // no file exists
		}
	}

	/**
	 * This method calls the native function 'process' to perform insertion or
	 * overwrite process.
	 */
	public void process_audio() {
		if (isOncePaused) {
			fileSize = (getPlayBackFileSize()); // ADDED
			totalDuration = getMilliseconsFromAFile(getFilename());
			if (fileSize != -1) {
				currentBytePosition = getBytePosition(pausePosition,
						totalDuration, fileSize);
				long rem = (currentBytePosition) % 2; // 32
				currentBytePosition = (currentBytePosition - rem);// ; //+15360


				temp_size = process(getFilename(), getTempFileName(),
						mDictationDir.getAbsolutePath(), currentBytePosition,
						process_type);

				// need modification
				currentBytePosition += temp_size;

				fileSize = (getPlayBackFileSize());
				totalDuration = getMilliseconsFromAFile(getFilename());

				pausePosition = byteToMilliSeconds(currentBytePosition,
						totalDuration, fileSize);

				RECORDER.resetTempFile();
			}

		}
	}

	public long byteToMilliSeconds(long bytePos, long totaldur, long fileSize) {
		long var = 0;
		try {
			var = (long) (((float) (bytePos) * (float) totaldur) / (float) fileSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return var;
	}

	public String getTempFileName() {
		File file = new File(mDictationDir, "template.wav");
		tempFileSize = file.length();

		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return (file.getAbsolutePath());
	}

	/**
	 * Enables and disables the views linked to the recorder.
	 * 
	 * @param isEnable
	 *            decided whether to enable or disable the views.
	 */
	private void recorderSetEnable(boolean isEnable) {

		if (isEnable) {
			if (!isPushToTalk) {
				bRecord.setBackgroundResource(R.drawable.dictate_record_selector);
			} else {
				bRecord.setBackgroundResource(R.drawable.dictate_record);
			}
			bNew.setBackgroundResource(R.drawable.dictate_new_selector);
			bRecord.setEnabled(true);
			bNew.setEnabled(true);
			tbOverwrite.setEnabled(true);
			tbOverwrite.setBackgroundResource(R.drawable.toggle);
		} else {
			bRecord.setBackgroundResource(R.drawable.dictate_record_sel);
			bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
			bRecord.setEnabled(false);
			bNew.setEnabled(false);
			tbOverwrite.setEnabled(false);
			tbOverwrite.setBackgroundResource(R.drawable.toggle_disble);
		}
	}

	/**
	 * While any interruption occurs while recording or playing a dialog is
	 * displays. String is decided here.
	 * 
	 * @param isForRecord
	 *            indicates while recording or playing.
	 * @return String to display in dialog.
	 */
	private String getInterruptionString(boolean isForRecord) {
		String interruptString = "";
		if (isForRecord) {
			if (Graphbar.getProgress() != 0
					&& Graphbar.getProgress() != Graphbar.getMax()) {
				if (isInsert) {
					interruptString = getResources().getString(
							R.string.Dictate_Alert_Continue_Insert_Message);
				} else {
					interruptString = getResources().getString(
							R.string.Dictate_Alert_Continue_Overwrite_Message);
				}
			} else {
				interruptString = getResources().getString(
						R.string.Continue_Recording);
			}

		}
		return interruptString;
	}

	/**
	 * UI updation are done while recorder starts and stops.
	 * 
	 * @param IsUIRecording
	 */
	private void recorderUpdateUi(boolean IsUIRecording) {
		if (IsUIRecording) {
			if (!isPushToTalk) {
				bRecord.setBackgroundResource(R.drawable.dictate_recording_selector);
			} else {
				bRecord.setBackgroundResource(R.drawable.dictate_recording);
			}
			bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
			bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind);

			bNew.setEnabled(false);
			bPlay.setEnabled(true);
			bRewind.setEnabled(true);

		} else {
			if (!isPushToTalk) {
				bRecord.setBackgroundResource(R.drawable.dictate_record_selector);
			} else {
				bRecord.setBackgroundResource(R.drawable.dictate_record);
			}
			bNew.setBackgroundResource(R.drawable.dictate_new_selector);
			bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind);

			bNew.setEnabled(true);
			bPlay.setEnabled(true);
			bRewind.setEnabled(true);
		}
	}

	/**
	 * Enables and disables the bottom bar when needed.
	 * 
	 * @param IsUIRecording
	 *            decides to enable or disable.
	 */
	private void mediaPlayerUpdatebottomBar(boolean IsUIRecording) {
		if (IsUIRecording) {
			imbFolder.setImageResource(R.drawable.dictate_folder_selector);
			imbFiles.setImageResource(R.drawable.dictate_files_selector);
			imbFolder.setEnabled(true);
			imbFiles.setEnabled(true);

			if (!isReview) {
				imbSend.setImageResource(R.drawable.dictate_send_selector);
				imbDelete.setImageResource(R.drawable.dictate_delete_selector);
				imbSend.setEnabled(true);
				imbDelete.setEnabled(true);
			}

		} else {
			imbFolder.setImageResource(R.drawable.dictate_folder_sel);
			imbFiles.setImageResource(R.drawable.dictate_files_sel);
			imbFolder.setEnabled(false);
			imbFiles.setEnabled(false);

			if (!isReview) {
				imbSend.setImageResource(R.drawable.dictate_send_sel);
				imbDelete.setImageResource(R.drawable.dictate_delete_sel);
				imbSend.setEnabled(false);
				imbDelete.setEnabled(false);
			}
		}
	}

	/**
	 * All the functionalities of the recorder is been initialized and starts
	 * recording.
	 */
	private void recorderTriggerStart() {
		//enableRecordUI(false);
		//System.out.println("**** record started");
		//System.out.println("ui disabled");
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(FORCEQUIT_PREF_NAME, true);
		editor.commit();
		muteNotificationSounds(true);
		//do {
			//System.out.println("creating recorder -->>");
			RECORDER = DMAudioRecorder.getHandler(this);
		//} while (!(RECORDER.getState() == DMAudioRecorder.State.INIT));

		if (RECORDER.getState() != DMAudioRecorder.State.REC) {

			recorderStartRecording();

			if (dictCard.getRecStartDate() == null
					|| dictCard.getRecStartDate().equals("")) {
				dictCard.setCreatedAt(dmApplication.getDeviceTime());
				dictCard.setRecStartDate(dmApplication.getDeviceTime());
				mDbHandler.updateRecStartDate(dictCard);
			}

			if (!isNew) {
				recorderUpdateUi(true);
			} else {
				enableEverything(false);
			}
			isRecording = true;
			tbOverwrite.setEnabled(false);
			tbOverwrite.setBackgroundResource(R.drawable.toggle_disble);
			mediaPlayerUpdatebottomBar(false);
			bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
			bForward.setEnabled(false);
			if (isPushToTalk) {
				mediaPlayerSetEnable(false);
				bPlay.setBackgroundResource(R.drawable.dictate_pause_sel);
			}
		}

	}

	/**
	 * Starts recording when the user gives the input.
	 */
	private void recorderStartRecording() {

		RECORDER.bindActivity(this);

		RECORDER.setVCVAState(isVcvaEnabled, getVcvaLevel());
		if (isOncePaused) {
			RECORDER.setFile(getTempFileName(), DMAudioRecorder.FILE_TEMPLATE);
			RECORDER.setAudioState(isOncePaused, isInsert, getFilename(),
					getPlayBackFileSize(), pausePosition,
					getMilliseconsFromAFile(getFilename()));
		} else {
			RECORDER.setFile(getFilename(), DMAudioRecorder.FILE_EXISTING);
		}

		RECORDER.prepare();
		setBluetoothSource();
		RECORDER.start();
		bForward.setEnabled(false);
		bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
	}

	/**
	 * Updates the timer while recording in progress.
	 * 
	 * @param current_duration
	 *            total duration of the recorded file.
	 * @param temp
	 *            total duration of the template file.
	 */
	private void recorderUpdateTimer(final long current_duration, final int temp) {

		final long total_duration = getMilliseconsFromAFile(getFilename());
		final long totaldurOfFile = total_duration + current_duration;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (isOncePaused) {

					if (isInsert) {

						if (isRecording) {
							tvTimeLeft.setVisibility(View.INVISIBLE);
						} else {
							tvTimeLeft.setVisibility(View.VISIBLE);
						}
						savedFileSizeDuration = total_duration;

						tvTimeRight.setText(Utilities.getDurationInTimerFormat(totaldurOfFile));
					} else {
						if ((pausePosition + current_duration) > total_duration) {

							if (isrecordingOnclick) {
								OverwriteLimitExceeded = true;
							}
							savedFileSizeDuration = (pausePosition + current_duration);
							if (isRecording) {
								tvTimeLeft.setVisibility(View.INVISIBLE);
							} else {
								tvTimeLeft.setVisibility(View.VISIBLE);
							}
							if(!isNavigatedToAnotherScreen) 
								tvTimeRight.setText(Utilities.getDurationInTimerFormat(savedFileSizeDuration));
							if(!isReview) {
								totalDuration = getMilliseconsFromAFile(getFilename());
								TotalDur = Utilities.getDurationInTimerFormat(totalDuration);
							}

						} 
						else {
							tvTimeLeft.setText(Utilities.getDurationInTimerFormat((pausePosition + current_duration)));
						}
					}

				} else {

					if (isRecording) {
						tvTimeLeft.setVisibility(View.INVISIBLE);
					} else {
						tvTimeLeft.setVisibility(View.VISIBLE);
					}
					savedFileSizeDuration = total_duration;
					tvTimeRight.setText(Utilities.getDurationInTimerFormat(total_duration));
				}
			}
		});

	}

	/**
	 * All the functionalities of recorder is stopped here.
	 */
	private void recorderTriggerStop() {
		if (audioManager.isBluetoothScoOn()) {
			audioManager.setBluetoothScoOn(false);
			audioManager.stopBluetoothSco();
		}
		isRecording = false;
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(FORCEQUIT_PREF_NAME, false);
		editor.commit();
		RECORDER.stop();

		if (!IsOtherRecorderActive) {
			bForward.setBackgroundResource(R.drawable.dictate_forward);
			bForward.setEnabled(true);
			if (Thread.currentThread().isAlive()) {
				Thread.currentThread().interrupt();
			}
			dictCard.setRecEndDate(dmApplication.getDeviceTime());
			mDbHandler.updateRecEndDate(dictCard);

			if (isOncePaused) {
				if (isVcvaEnabled) {
					if (isVcvaDetectionRunning) {
						if (!isInsertionProcessRunning) {
							new InsertOrOverwriteTask().execute();
						}
					}
				} else {
					if (!isInsertionProcessRunning) {
						new InsertOrOverwriteTask().execute();
					}
				}
			} else {
				if (isInsert) {
					drawWaveGraph();
				} else {
					drawOverwriteGraph();
				}
				updateTimerandgraph();
				disableForward();

			}
		}
	}

	/**
	 * Stops recording.
	 */
	private void recorderStopRecording() {

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if ((mBluetoothAdapter.isEnabled()) && bluetoothConnectivity == true) {

		} else if (!mBluetoothAdapter.isEnabled()
				|| bluetoothConnectivity == false) {

		}
		muteNotificationSounds(false);
		mSignalometer.setVisibility(View.INVISIBLE);
		mOverwriteSignalometer.setVisibility(View.INVISIBLE);
		if (RECORDER != null) {
			if (RECORDER.getState() == DMAudioRecorder.State.REC) {
				isNew = false;
				isIntentPlay = false;
				playButtonState = MediaMode.START;
				recorderTriggerStop();
				recorderUpdateUi(false);
				mediaPlayerSetEnable(true);

				tbOverwrite.setEnabled(true);
				tbOverwrite.setBackgroundResource(R.drawable.toggle);
				mediaPlayerUpdatebottomBar(true);
				passedModeName = DMApplication.MODE_EDIT_RECORDING;
				Intent intent = getIntent();

				intent.putExtra(DMApplication.START_MODE_TAG,
						DMApplication.MODE_EDIT_RECORDING);

				intent.putExtra(DMApplication.DICTATION_ID,
						dictCard.getDictationId());

				setIntent(intent);
				if (!isReview) {
					recorderUpdateUi(false);
					tbOverwrite.setEnabled(true);
					tbOverwrite.setBackgroundResource(R.drawable.toggle);
					imbSend.setEnabled(true);
					imbDelete.setEnabled(true);
					imbSend.setImageResource(R.drawable.dictate_send_selector);
					imbDelete
							.setImageResource(R.drawable.dictate_delete_selector);
				}
			}
		}
		tvTimeLeft.setVisibility(View.VISIBLE);

	}

	/**
	 * When the recording stops the Graph seekbar and the timer is updated here.
	 */
	private void updateTimerandgraph() {

		Graphbar.setMax(500);
		totalDuration = getMilliseconsFromAFile(getFilename());
		dictCard.setDuration(totalDuration);
		mDbHandler.updateDuration(dictCard);
		fileSize = getPlayBackFileSize();
		amoungToupdate = (int) totalDuration / 500;
		if (amoungToupdate == 0) {
			amoungToupdate = (int) passedTotalDuration / 500;
		}
		if (isOncePaused) {
			if (!isInsert) {
				Graphbar.setProgress((int) getGBcurrentPos(pausePosition));
				mediaPlayerUpdateTimer((int) pausePosition);
				if (Graphbar.getMax() == Graphbar.getProgress()) {
					disableForward();
				}
			} else {
				Graphbar.setProgress((int) getGBcurrentPos((int) pausePosition) - 1);
				mediaPlayerUpdateTimer((int) (pausePosition) - 1);
			}
		} else {
			if (!isCallActive) {
				Graphbar.setProgress(Graphbar.getMax());
			}
			mediaPlayerUpdateTimer(getMMcurrentPos(Graphbar.getMax()));
			disableForward();
		}
	}

	/**
	 * If the VCVA is enabled and start recording. While the VCVA stops
	 * detection the updation in the views will be done here.
	 */
	private void vcvaStopDetection() {
		if (isVcvaDetectionRunning) {
			isNew = false;
			isVcvaDetectionRunning = false;
			if (isOncePaused) {
				if (!isInsertionProcessRunning) {
					new InsertOrOverwriteTask().execute();
				}
			} else {
				if (isInsert) {
					drawWaveGraph();
				} else {
					drawOverwriteGraph();
				}

				disableForward();
				updateTimerandgraph();
			}

			bPlay.setEnabled(true);
			bRewind.setEnabled(true);

			bRecord.setEnabled(true);

			bRewind.setBackgroundResource(R.drawable.dictate_rewind);
			bRecord.setBackgroundResource(R.drawable.dictate_record_selector);

		} else {
			if (isInsert) {
				drawWaveGraph();
			} else {
				drawOverwriteGraph();
			}

			disableForward();
			updateTimerandgraph();
			bPlay.setEnabled(true);
			bRewind.setEnabled(true);
			bRecord.setEnabled(true);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind);
			if (ActivityInitialPlay) {
				bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
			}
		}
	}

	/**
	 * Calculates total duration of the file.
	 * 
	 * @param filePath
	 *            Path of that particular audio file.
	 * @return milliseconds in long value.
	 */
	private long getMilliseconsFromAFile(String filePath) {
		long millisec = getDurationInMS(getPlayBackFileSize());
		return millisec;
	}

	/**
	 * Calculates media player current position.
	 * 
	 * @param val
	 *            Seekbar's current position.
	 * @return mediaplayer's current position in long value.
	 */
	public long getMMcurrentPos(long val) {
		return val * amoungToupdate;
	}

	/**
	 * Calculates the seekbar's position using media player current position.
	 * 
	 * @param mediplayerpos
	 *            media player current position.
	 * @return Seekbar's current position in long value.
	 */
	public long getGBcurrentPos(long mediplayerpos) {
		return mediplayerpos / amoungToupdate;
	}

	/**
	 * Calculates the total duration of a audio file.
	 * 
	 * @param length
	 *            total size of the file.
	 * @return duration in long value.
	 */
	public long getDurationInMS(long length) {
		return (long) (((float) length / 32000f) * 1000);
	}

	/**
	 * If the VCVA is enabled and start recording. While the VCVA start
	 * detection the updation in the views will be done here.
	 */
	private void vcvaStartDetection() {

		if (isInsert) {
			showSignalometer();
		} else {
			showOverwriteSignalometer();
		}
		if (!isVcvaDetectionRunning) {
			disableForward();
			isVcvaDetectionRunning = true;
			bPlay.setEnabled(true);
			bRewind.setEnabled(true);
			imbFolder.setEnabled(false);
			imbDelete.setEnabled(false);
			imbFiles.setEnabled(false);
			imbSend.setEnabled(false);
			bNew.setEnabled(false);

			bRewind.setBackgroundResource(R.drawable.dictate_rewind);
			bRecord.setBackgroundResource(R.drawable.dictate_recording_selector);
			imbFolder.setImageResource(R.drawable.dictate_folder_sel);
			imbDelete.setImageResource(R.drawable.dictate_delete_sel);
			imbFiles.setImageResource(R.drawable.dictate_files_sel);
			imbSend.setImageResource(R.drawable.dictate_send_sel);
			bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
		}
	}

	/**
	 * All the views related to the media player will be enabled or disabled.
	 * 
	 * @param isEnable
	 *            Decided whether to enable or disable
	 */
	private void mediaPlayerSetEnable(boolean isEnable) {

		if (isEnable) {
			bRewind.setEnabled(true);
			bPlay.setEnabled(true);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind);
			if (!isPushToTalk) {
				bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
			} else {
				bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
			}
		} else {
			bRewind.setEnabled(false);
			bPlay.setEnabled(false);
			bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
			bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
		}
	}

	/**
	 * initializes instance of a media player.
	 */
	private void mediaPlayerIntiate() {
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

	}

	/**
	 * Set the source file to media player.
	 */
	private void mediaPlayerSetSource() {
		UpdateAudioHeader(getFilename());
		String audioFile = getFilename();

		try {
			mMediaPlayer.reset();
			mMediaPlayer.setDataSource(audioFile);
			mMediaPlayer.prepare();

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initializes the MediaPlayer instance. Set the source to media player.
	 * Also Initializes the update timer task and another TimerTask which
	 * updates the progressbar while playing the audio.
	 * 
	 * @param playIt
	 *            Decides whether MediaPlayer start to play or not.
	 */
	private void mediaPlayerPrepareAudioFile(final boolean playIt) {

		mediaPlayerIntiate();
		mediaPlayerSetSource();
		// Setting the seekbar max to 500
		Graphbar.setMax(500);
		if (playIt) {

			// Commented because while overwriting and overwrite exceeds the max
			// of file, if user clicked rewind button while recording is active
			// the files starts playing
			// from home pos.

			
			if (Graphbar.getMax() == Graphbar.getProgress()) {
			 Graphbar.setProgress(0); }
			 
			if (isVcvaEnabled) {
				bRecord.setEnabled(false);
				bRecord.setBackgroundResource(R.drawable.dictate_record_sel);
				bNew.setEnabled(false);
			}
			mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
			mMediaPlayer.start();
			playButtonState = MediaMode.PLAY;
		} else {
			mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
			bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
			playButtonState = MediaMode.PAUSE;

		}
		InsideMMduration = mMediaPlayer.getDuration();
		amoungToupdate = InsideMMduration / 500;
		// TimerTask for updating the SeekBar while playing.
		task = new TimerTask() {
			@Override
			public void run() {
				Graphbar.post(new Runnable() {
					@Override
					public void run() {

						if (mMediaPlayer != null) {
							if (playButtonState == MediaMode.PLAY) {
								if (mMediaPlayer.isPlaying()) {
									if (!(amoungToupdate
											* Graphbar.getProgress() >= InsideMMduration)) {
										int tempP = Graphbar.getProgress();
										tempP += 1;
										Graphbar.setProgress(tempP);
										enableRewindandForward();
									}

								}
							}
						}
					}
				});
			}
		};
		timer = new Timer();
		timer.schedule(task, 0, amoungToupdate);
		// TimerTask for updating the timer while playing
		mpTimertask = new TimerTask() {
			@Override
			public void run() {
				Graphbar.post(new Runnable() {
					@Override
					public void run() {

						if (mMediaPlayer != null) {
							if (playButtonState == MediaMode.PLAY) {
								if (mMediaPlayer.isPlaying()) {
									mediaPlayerUpdateTimer(mMediaPlayer
											.getCurrentPosition());
								}
							}
						}
					}
				});
			}
		};
		// Scheduled in each second
		mptimer = new Timer();
		mptimer.schedule(mpTimertask, 0, 1000);
		// OnCompletion listener for reseting the player view when the
		// MediaPlayer playing gets stopped.
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			public void onCompletion(MediaPlayer mp) {
				disableForward();
				playButtonState = MediaMode.PAUSE;
				if (!isReview) {
					mediaPlayerUpdatebottomBar(true);
				} else {
					imbFolder.setEnabled(true);
					imbFiles.setEnabled(true);

					imbFolder
							.setImageResource(R.drawable.dictate_folder_selector);
					imbFiles.setImageResource(R.drawable.dictate_files_selector);
				}
				pausePosition = getMMcurrentPos(Graphbar.getMax());
				Graphbar.setProgress(Graphbar.getMax());
				tvTimeLeft.setText(tvTimeRight.getText());
				bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
				if (!isReview) {
					recorderSetEnable(true);
				} else {
					bNew.setBackgroundResource(R.drawable.dictate_new_selector);
					bNew.setEnabled(true);
					bRecord.setEnabled(false);
					bRecord.setBackgroundResource(R.drawable.dictate_record_sel);
				}
			}
		});
	}

	/**
	 * Destroys the MediaPlayer instance initiated for a particular recorded
	 * file.
	 */
	private void mediaPlayerDistroy() {
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()) {
				playButtonState = MediaMode.PAUSE;
				if (!isReview) {
					recorderSetEnable(true);
				} else {
					bNew.setEnabled(true);
					bNew.setBackgroundResource(R.drawable.dictate_new_selector);
				}
				mediaPlayerUpdatebottomBar(true);
				mMediaPlayer.pause();
				if (Graphbar.getProgress() < Graphbar.getMax()
						&& Graphbar.getProgress() > 0) {
					isOncePaused = true;
				}
				savedCurrentPos = getMMcurrentPos(Graphbar.getProgress());
				pausePosition = mMediaPlayer.getCurrentPosition();
				continue_process = false;
				bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
			}
		}
	}

	/**
	 * Update the timer TextViews in the Player and recorder mode.
	 * 
	 * @param currentPosition
	 *            MediaPlayer current position wile playing forward or rewind.
	 */
	private void mediaPlayerUpdateTimer(long currentPosition) {
		totalDuration = getMilliseconsFromAFile(getFilename());
		currentDuration = currentPosition;

		TotalDur = Utilities.getDurationInTimerFormat(totalDuration);
		tvTimeLeft.setVisibility(View.VISIBLE);
		tvTimeRight.setText(TotalDur);
		if (Graphbar.getProgress() == Graphbar.getMax()) {
			tvTimeLeft.setText(""
					+ Utilities.getDurationInTimerFormat(totalDuration));
		} else {
			tvTimeLeft.setText(""
					+ Utilities.getDurationInTimerFormat(currentDuration));
		}
	}

	/**
	 * Disable the forward button.
	 */
	private void disableForward() {
		if (!isVcvaEnabled && !isCallActive) {
			Graphbar.setProgress(Graphbar.getMax());
		}
		bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
		bRewind.setBackgroundResource(R.drawable.dictate_rewind);
		if (!isRecording) {
			isOncePaused = false;
		}
	}

	/**
	 * Disable the rewind button.
	 */
	private void disableRewind() {
		if (!isVcvaEnabled) {
			Graphbar.setProgress(0);
		}
		if (!isRecording) {
			bForward.setBackgroundResource(R.drawable.dictate_forward);
		}
		bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
	}

	/**
	 * Enable the rewind and forward Button views.
	 */
	private void enableRewindandForward() {
		bRewind.setEnabled(true);
		if (!isRecording) {
			bForward.setEnabled(true);
			bForward.setBackgroundResource(R.drawable.dictate_forward);
		}
		bRewind.setBackgroundResource(R.drawable.dictate_rewind);
	}

	/**
	 * Disable the rewind button and the forward button views.
	 */
	private void disableRewindandForward() {
		bRewind.setEnabled(false);
		bForward.setEnabled(false);
		bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
		bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
	}

	/**
	 * In the frame it brings the Signalometer to front
	 */
	private void showSignalometer() {

		mSignalometer.bringToFront();
		Graphbar.setVisibility(View.INVISIBLE);
		mOverwriteSignalometer.setVisibility(View.INVISIBLE);
		mSignalometer.setVisibility(View.VISIBLE);

	}

	/**
	 * In the frame it brings the overwrite Signalometer to front
	 */
	private void showOverwriteSignalometer() {

		mOverwriteSignalometer.bringToFront();
		Graphbar.setVisibility(View.INVISIBLE);
		mSignalometer.setVisibility(View.INVISIBLE);
		mOverwriteSignalometer.setVisibility(View.VISIBLE);

	}

	/**
	 * graph and the Signalometer are placed in a frame. This function brings
	 * the graph frame to front.
	 */
	private void showGraph() {

		Graphbar.bringToFront();
		mSignalometer.setVisibility(View.INVISIBLE);
		mOverwriteSignalometer.setVisibility(View.INVISIBLE);
		Graphbar.setVisibility(View.VISIBLE);

	}

	/**
	 * Generate the bitmap of the particular Recoded file and will set that as
	 * the drawable for the seekbar using Layerlist.
	 */
	private void drawWaveGraph() {
		if (mSignalometer.isShown()) {
			mSignalometer.setVisibility(View.INVISIBLE);
		} else {
			mOverwriteSignalometer.setVisibility(View.INVISIBLE);
		}
		Drawable d = new BitmapDrawable(getResources(),
				mWavegraph.getSoundWavegraph());
		int tempProgress = Graphbar.getProgress();
		Graphbar.setProgress(0);
		Rect bounds = Graphbar.getProgressDrawable().getBounds();
		Graphbar.setProgressDrawable(d);
		Graphbar.getProgressDrawable().setBounds(bounds);
		Graphbar.setProgress(tempProgress);

		Graphbar.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
		showGraph();
	}

	/**
	 * Makes the SeekBar invisible. SeekBar that is used to draw the graph.
	 */
	private void MakeGraphFrameInvisible() {

		if (!Graphbar.isShown()) {
			showGraph();
		}
		Graphbar.setVisibility(ProgressBar.INVISIBLE);
		mSignalometer.setVisibility(ProgressBar.INVISIBLE);
		mOverwriteSignalometer.setVisibility(ProgressBar.INVISIBLE);
		tvTimeLeft.setVisibility(ProgressBar.INVISIBLE);
		tvTimeRight.setVisibility(ProgressBar.INVISIBLE);
	}

	/**
	 * Generate the bitmaps of the particular Recoded file and will set that as
	 * the drawable for the SeekBar using LayerList.
	 */
	private void drawOverwriteGraph() {

		// generate new drawables and make new LayerList
		Drawable primaryProgress = new BitmapDrawable(getResources(),
				mWavegraph.getSoundWavegraphforOverWrite(true));
		Drawable secondaryprogress = new BitmapDrawable(
				mWavegraph.getSoundWavegraphforOverWrite(false));
		ClipDrawable drawableToClip = new ClipDrawable(secondaryprogress,
				Gravity.LEFT, ClipDrawable.HORIZONTAL);
		LayerDrawable mLayerList = new LayerDrawable(new Drawable[] {
				primaryProgress, drawableToClip });

		// Refresh the widget save the graphbar status to temp values
		int tempProgress = Graphbar.getProgress();
		Graphbar.setProgress(0);

		Rect bounds = Graphbar.getProgressDrawable().getBounds();

		// set the drawable to graphbar
		Graphbar.setProgressDrawable(mLayerList);

		// Replace the temp values to new graphbar
		Graphbar.getProgressDrawable().setBounds(bounds);
		Graphbar.setProgress(tempProgress);
		Graphbar.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});
		// If graph not shown bring to front
		if (!Graphbar.isShown()) {
			showGraph();
		}
	}

	/**
	 * When the activity is initialized all the views are mount to java in this
	 * function using findViewById. Some Listenrs are also set here.
	 */
	private void mountViewsToJava() {
		tvTitle = (EditText) findViewById(R.id.sound_title);
		tvSubtitle = (TextView) findViewById(R.id.sound_subtitle);
		tvTimeLeft = (TextView) findViewById(R.id.time_left);
		tvTimeRight = (TextView) findViewById(R.id.time_right);

		tbOverwrite = (ToggleButton) findViewById(R.id.toggle_rec_over);
		tbOverwrite.setSoundEffectsEnabled(false);

		activityRootView = findViewById(R.id.dictate_root_view);
		bRewind = (Button) findViewById(R.id.b_rewind);
		bForward = (Button) findViewById(R.id.b_forward);
		bPlay = (Button) findViewById(R.id.b_play);
		bNew = (Button) findViewById(R.id.new_rec);
		bRecord = (Button) findViewById(R.id.record);
		tvWorktype = (TextView) findViewById(R.id.sound_worktype);
		imbFolder = (ImageButton) findViewById(R.id.b_folder);
		imbSend = (ImageButton) findViewById(R.id.b_export);
		imbDelete = (ImageButton) findViewById(R.id.b_delete);
		imbFiles = (ImageButton) findViewById(R.id.b_flash);
		imFav = (CheckBox) findViewById(R.id.sound_fav);
		imPhoto = (ImageView) findViewById(R.id.sound_image);
		bRewind.setSoundEffectsEnabled(false);
		bForward.setSoundEffectsEnabled(false);
		bPlay.setSoundEffectsEnabled(false);
		bNew.setSoundEffectsEnabled(false);
		bRecord.setSoundEffectsEnabled(false);
		tvWorktype.setSoundEffectsEnabled(false);
		imbFolder.setSoundEffectsEnabled(false);
		imbSend.setSoundEffectsEnabled(false);
		imbDelete.setSoundEffectsEnabled(false);
		imbFiles.setSoundEffectsEnabled(false);
		imFav.setSoundEffectsEnabled(false);
		imPhoto.setSoundEffectsEnabled(false);

		mSignalometer = (Signalometer) findViewById(R.id.seekbar_signalometer);
		mSignalometer.setMax(32767);
		mSignalometer.setProgress(0);

		mOverwriteSignalometer = (OverwriteSignalometer) findViewById(R.id.seekbar_over_signalometer);
		mOverwriteSignalometer.setMax(32767);
		mOverwriteSignalometer.setProgress(0);

		mSignalometer.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		mOverwriteSignalometer.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});

		Graphbar = (SeekBar) findViewById(R.id.seekbar_graph);
		Graphbar.setOnSeekBarChangeListener(this);
		Graphbar.setThumbOffset(0);
		if (isInsert) {
			showSignalometer();
		} else {
			showOverwriteSignalometer();
		}
		tvTitle.setCursorVisible(false);
		tvTitle.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				tvTitle.setCursorVisible(true);
				isSofyKeyBoardshown = true;
				return false;
			}
		});

		tvTitle.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				tvTitle.clearFocus();
				isSofyKeyBoardshown = false;
				String title = tvTitle.getText().toString().trim();
				if (actionId == EditorInfo.IME_ACTION_DONE
						&& !title.equalsIgnoreCase(dictCard.getDictationName())) {
					validateDictationName(title);

				} else {
					tvTitle.setText(title);
					tvTitle.setCursorVisible(false);
					dictCard.setDictationName(title);
					mDbHandler.updateDictName(dictCard.getDictationName(),
							dictCard.getDictationId());
				}

				return false;
			}
		});

		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						Rect r = new Rect();
						// r will be populated with the coordinates of your view
						// that area still visible.
						activityRootView.getWindowVisibleDisplayFrame(r);

						int heightDiff = activityRootView.getRootView()
								.getHeight() - (r.bottom - r.top);
						if (heightDiff > 100) { // if more than 100 pixels, its
												// probably a keyboard...

							isKeyboardShown = true;
						} else {
							if (isKeyboardShown) {

								tvTitle.clearFocus();
								isSofyKeyBoardshown = false;
								String title = tvTitle.getText().toString()
										.trim();
								if (!title.equalsIgnoreCase(dictCard
										.getDictationName())) {
									validateDictationName(title);

								} else {
									tvTitle.setText(title);
									tvTitle.setCursorVisible(false);
									dictCard.setDictationName(title);
									mDbHandler.updateDictName(
											dictCard.getDictationName(),
											dictCard.getDictationId());
								}

								isKeyboardShown = false;
							}
						}
					}
				});

	}

	/**
	 * Validates Dictation name when user edit it.
	 * 
	 * @param dictName
	 *            The edited Dictation name.
	 */
	private boolean validateDictationName(String dictName) {
		if (!dictName.equals("")) {

			boolean isUpdated = mDbHandler.updateDictationName(dictCard,
					dictName);
			tvTitle.setText(dictCard.getDictationName());
			tvTitle.setCursorVisible(false);
			if (!isUpdated) {
				alert = new AlertDialog.Builder(DictateActivity.this);
				alert.setCancelable(false);
				alert.setTitle(getResources().getString(
						R.string.Dictate_Alert_name_exists));
				alert.setMessage(getResources().getString(
						R.string.Dictate_Alert_Already_Exists_Message));
				alert.setPositiveButton(
						getResources().getString(R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								tvTitle.setCursorVisible(false);
								aDialog = null;
							}
						});
				if (aDialog == null) {
					aDialog = alert.create();
				}
				if (!aDialog.isShowing())
					aDialog.show();
				return false;
			}
		} else {
			alert = new AlertDialog.Builder(DictateActivity.this);
			alert.setCancelable(false);
			alert.setTitle(getResources().getString(R.string.Alert));
			alert.setMessage(getResources().getString(
					R.string.Property_enter_valid_dict_name));
			alert.setPositiveButton(
					getResources().getString(R.string.Dictate_Alert_Ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							tvTitle.setText(dictCard.getDictationName());
							tvTitle.setCursorVisible(false);
							dialog.dismiss();
							aDialog = null;
						}
					});
			if (aDialog == null) {
				aDialog = alert.create();
			}
			if (!aDialog.isShowing())
				aDialog.show();

		}
		return true;
	}

	/**
	 * All the ClickListener for the widgets are set here.
	 */
	private void setClickListeners() {
		tbOverwrite.setOnClickListener(this);

		bNew.setOnClickListener(this);
		bPlay.setOnClickListener(this);
		bRewind.setOnClickListener(this);
		bForward.setOnClickListener(this);
		imFav.setOnClickListener(this);

		imbFiles.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					isPropertyClicked = true;
					DictationPropertyActivity.ComingFromRecordings = false;
					dmApplication.setRecordingsClickedDMActivity(false);
					triggerDictationProperty();
					if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							playButtonState = MediaMode.PAUSE;
							if (!isReview) {
								recorderSetEnable(true);
							} else {
								bNew.setEnabled(true);
								bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							}
							mMediaPlayer.pause();
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
							}
							savedCurrentPos = getMMcurrentPos(Graphbar
									.getProgress());
							pausePosition = mMediaPlayer.getCurrentPosition();
							fileSize = getPlayBackFileSize();
							continue_process = false;
							bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
						}
					}
					tvTitle.setCursorVisible(false);
					isNavigatedToAnotherScreen = true;
					isIntentPlay = true;
				}

				return false;
			}
		});
		imbDelete.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							playButtonState = MediaMode.PAUSE;
							if (!isReview) {
								recorderSetEnable(true);
							} else {
								bNew.setEnabled(true);
								bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							}
							mMediaPlayer.pause();
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
							}
							savedCurrentPos = getMMcurrentPos(Graphbar
									.getProgress());
							pausePosition = mMediaPlayer.getCurrentPosition();
							fileSize = getPlayBackFileSize();
							continue_process = false;
							bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
						}
					}
					tvTitle.setCursorVisible(false);
					if (isRecording) {
						enableEverything(true);
						recorderStopRecording();
					}
					DeleteCurrentRecording(mDictationDir.getAbsolutePath());

				}

				return false;
			}
		});
		;
		imbSend.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mResultcode = null;
				tvTitle.setCursorVisible(false);
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							playButtonState = MediaMode.PAUSE;
							if (!isReview) {
								recorderSetEnable(true);
							} else {
								bNew.setEnabled(true);
								bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							}
							mMediaPlayer.pause();
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
							}
							savedCurrentPos = getMMcurrentPos(Graphbar
									.getProgress());
							pausePosition = mMediaPlayer.getCurrentPosition();
							fileSize = getPlayBackFileSize();
							continue_process = false;
							bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
						}
					}
					tvTitle.setCursorVisible(false);
					if (isRecording) {
						enableEverything(true);
						recorderStopRecording();
					}
					if (mSendDialog == null)
						prompSendDialog();
					else if (!mSendDialog.isShowing())
						prompSendDialog();
				}

				return false;
			}
		});
		;
		imbFolder.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					isNavigatedToAnotherScreen = true;
					isFolderButtonTouched = true;
					if (!isBeyondLimit)
						if (!limitFlag)
							if (!isNew && !isReview) {
								dictCard.setStatus(DictationStatus.PENDING
										.getValue());
								mDbHandler.updateDictationStatus(
										dictCard.getDictationId(),
										dictCard.getStatus());
							}
					if (isNew) {
						if (isInsert) {
							tbOverwrite.performClick();
						}
					}
					if (isNew) {
						dmApplication.lastDictMailSent = true;
					}
					Intent intentFolder = new Intent(DictateActivity.this,
							DMActivity.class);
					intentFolder
							.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intentFolder);

					dmApplication.setRecordingsClickedDMActivity(false);
					if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							playButtonState = MediaMode.PAUSE;
							if (!isReview) {
								recorderSetEnable(true);
							} else {
								bNew.setEnabled(true);
								bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							}
							mMediaPlayer.pause();
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
							}
							savedCurrentPos = getMMcurrentPos(Graphbar
									.getProgress());
							pausePosition = mMediaPlayer.getCurrentPosition();
							fileSize = getPlayBackFileSize();
							continue_process = false;
							bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
						}
					}
					tvTitle.setCursorVisible(false);
					if (!isBeyondLimit)
						if (!limitFlag) {
							if (!isNew && (Graphbar.getProgress() != Graphbar.getMax())) {
								SavedDicID = dictCard.getDictationId();
								passedModeName = "";
							}
						}
					isIntentPlay = true;
					dmApplication.onSetPending = true;
				}

				return false;
			}
		});
		;
	}

	/**
	 * When the activity is initialized rewind button and forward button touch
	 * listeners are set here.
	 */
	private void setRewindandForward() {

		bRewind.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						tvTitle.setCursorVisible(false);
						checkForwordIsEnabled = true;
						getWindow().getDecorView()
								.findViewById(android.R.id.content)
								.setKeepScreenOn(false);
						if (mWakeLock.isHeld()) 
							mWakeLock.release();
						bForward.setEnabled(true);
						bRewind.setBackgroundResource(R.drawable.dictate_rewind_press);
						bForward.setBackgroundResource(R.drawable.dictate_forward);
						rewindPressed = true;
						isRewinding = 1;
						if (isRecording) {
							rewindStratPlay = true;
							recorderStopRecording();
							updateTimerandgraph();
							bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
							recorderSetEnable(false);
							if (!isOncePaused) 
								Graphbar.setProgress(Graphbar.getMax());
							else 
								Graphbar.setProgress((int) getGBcurrentPos(pausePosition));
						}
						if (mMediaPlayer != null) {
							if (mMediaPlayer.isPlaying()) 
								mMediaPlayer.pause();
						}
						rewindThread.run();
					break;
					case MotionEvent.ACTION_UP:
						if (Graphbar.getProgress() != 0) 
							bRewind.setBackgroundResource(R.drawable.dictate_rewind);
						if (isRewinding == 1 && !isCallActive) {
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() >= 0) {
								isOncePaused = true;
								if (!tvTimeLeft.getText().toString().equalsIgnoreCase(tvTimeRight.getText().toString()))
									bForward.setBackgroundResource(R.drawable.dictate_forward);
							}
							savedCurrentPos = getMMcurrentPos(Graphbar.getProgress());
							continue_process = false;
							isRewinding = 0;
							if (!isInsertionProcessRunning) {
								if (mMediaPlayer != null) {
									if (playButtonState == MediaMode.PLAY) {
										mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
										mMediaPlayer.start();
									}
									else if (playButtonState == MediaMode.START) {
										if (!rewindStratPlay) {
											mediaPlayerPrepareAudioFile(false);
											mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
										} else {
											mediaPlayerPrepareAudioFile(true);
											mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
											rewindStratPlay = false;
										}
									}
								} else {
									if (ActivityInitialPlay) {
										mediaPlayerPrepareAudioFile(true);
										mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar.getProgress()));
										rewindStratPlay = false;
										ActivityInitialPlay = false;
									}
								}
							}
	
							rewindPressed = false;
						}
					}
					return false;
			}
		});
		bForward.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					tvTitle.setCursorVisible(false);
					isForwarding = 1;
					bForward.setBackgroundResource(R.drawable.dictate_forward_press);
					bRewind.setBackgroundResource(R.drawable.dictate_rewind);
					if (Graphbar.getProgress() < Graphbar.getMax()
							&& Graphbar.getProgress() > 0) {
						isOncePaused = true;
					}
					savedCurrentPos = getMMcurrentPos(Graphbar.getProgress());
					continue_process = false;
					if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							mMediaPlayer.pause();

						}
					}
					forwardThread.run();
					break;

				case MotionEvent.ACTION_UP:
					isForwarding = 0;
					if (Graphbar.getProgress() != Graphbar.getMax()) {
						bForward.setBackgroundResource(R.drawable.dictate_forward);
					}
					if (mMediaPlayer != null && !isCallActive) {
						if (playButtonState == MediaMode.PLAY) {
							if (Graphbar.getProgress() == Graphbar.getMax()) {
								mMediaPlayer
										.seekTo((int) getMMcurrentPos(Graphbar
												.getMax()));
								disableForward();
								playButtonState = MediaMode.PAUSE;
								mediaPlayerUpdatebottomBar(true);
								pausePosition = getMMcurrentPos(Graphbar
										.getMax());
								bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
								if (!isReview) {
									recorderSetEnable(true);
								} else {
									bNew.setBackgroundResource(R.drawable.dictate_new_selector);
									bNew.setEnabled(true);
								}
							} else {
								mMediaPlayer
										.seekTo((int) getMMcurrentPos(Graphbar
												.getProgress()));
								mMediaPlayer.start();
							}
						} else if (playButtonState == MediaMode.START) {
							mediaPlayerPrepareAudioFile(false);
							mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar
									.getProgress()));
						}
					}
					break;
				}
				return false;
			}
		});

	}

	/**
	 * Handles everything in UI and functionality when the new button is
	 * clicked.
	 */
	private void newRecordingClicked() {

		isOncePaused = false;
		passedTotalDuration = 0;
		if (isInsert) {
			tbOverwrite.performClick();
		}
		if (isVcvaEnabled) {
			if (isRecording) {
				recorderStopRecording();
			}
		}
		if (isReview) {
			isReview = false;
		}
		if (timer != null || mptimer != null) {
			timer.cancel();
			task.cancel();
			mptimer.cancel();
			mpTimertask.cancel();
		}
		tbOverwrite.setEnabled(true);
		tbOverwrite.setBackgroundResource(R.drawable.toggle);
		if (!isSendClicked) {
			isSendClicked = false;
			Cursor cur = mDbHandler.checkActiveDictationExistsWithDuration();
			if (cur != null) {

				DictationCard card = mDbHandler.getSelectedDicts(cur);
				if (card.getStatus() == DictationStatus.SENT.getValue()
						|| card.getStatus() == DictationStatus.SENT_VIA_EMAIL.getValue() || card.isResend() == 1) {
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
		} else
			isSendClicked = false;

		if (!isBeyondLimit) {
			initializeDictationData();
			isNew = true;
		} else {

			if (dmApplication.isExecuted)
				initializeDictationData();
		}

		if (!isBeyondLimit)
			initializeRecorderViews();
	}

	/**
	 * Resets all the views in recorder and player when the new button is
	 * clicked and new recorder is initialized.
	 */
	private void initializeRecorderViews() {
		mediaPlayerSetEnable(false);
		passedModeName = DMApplication.MODE_NEW_RECORDING;
		mSignalometer.setProgress(0);
		mOverwriteSignalometer.setProgress(0);
		Graphbar.setProgress(0);
		tvWorktype.setEnabled(true);
		disableRewindandForward();
		isOncePaused = false;
		bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
		bNew.setEnabled(false);
		bRecord.setEnabled(true);
		if (!isPushToTalk) {
			bRecord.setBackgroundResource(R.drawable.dictate_record_selector);
		} else {
			bRecord.setBackgroundResource(R.drawable.dictate_record);
		}
		imbSend.setEnabled(false);
		imbDelete.setEnabled(false);
		tvSubtitle.setEnabled(true);
		imbSend.setImageResource(R.drawable.dictate_send_sel);
		imbDelete.setImageResource(R.drawable.dictate_delete_sel);
		resetDmPlayerStatusView();
		enableDictationDatas(true);
	}

	/**
	 * Resets all the player view. Timer,graph and Signalometer module.
	 */
	private void resetDmPlayerStatusView() {
		tvTimeLeft.setText("00:00");			//0:00:00
		tvTimeRight.setText("00:00");			//0:00:00
		MakeGraphFrameInvisible();
	}

	/**
	 * Calculates the total duration of the file.
	 * 
	 * @param size
	 *            file size of the recording file.
	 * @return duration of total file in milliseconds
	 */
	private long getPlayBackDuration(long size) {
		return (size / 32000) * 1000;
	}

	@Override
	public void vcvaProgress(boolean status, long duration) {
		if (passedTotalDuration > 1000) {
			if (!bRecord.isEnabled()) {
				bRecord.setEnabled(true);
			}
		}
		if (bForward.isEnabled()) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					bForward.setEnabled(false);
				}
			});

		}
		if (blinkRecordBoolean) {
			blinkRecordBoolean = false;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {

					bRecord.setBackgroundResource(R.drawable.dictate_record);

				}
			});
		} else {
			blinkRecordBoolean = true;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					bRecord.setBackgroundResource(R.drawable.dictate_recording);

				}
			});

		}

	}

	/**
	 * Parse the worktypes and set to Listview.
	 */
	public void setWorktype() {
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
				mServerWorktype = getResources().getString(R.string.None) + ":"
						+ mServerWorktype;

				mListValues = new ArrayList<String>(
						Arrays.asList(mServerWorktype.split(":")));
			} else if (mServerWorktype.equalsIgnoreCase("")) {
				mServerWorktype = getResources().getString(R.string.None);
				mListValues.add(mServerWorktype);
			} else if (!mServerWorktype.equalsIgnoreCase("")
					&& !mServerWorktype.contains(":")) {
				mServerWorktype = getResources().getString(R.string.None) + ":"
						+ mServerWorktype;
				mListValues = new ArrayList<String>(
						Arrays.asList(mServerWorktype.split(":")));
			}
		} else if (mCheckServermail.equalsIgnoreCase("1")
				|| mCheckServermail.equalsIgnoreCase("")) {
			sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			mMailworktype = sharedPref.getString(
					getResources().getString(R.string.Worktype_Email_Key), "");
			if (mMailworktype.contains(":")) {
				mMailworktype = getResources().getString(R.string.None) + ":"
						+ mMailworktype;
				mListValues = new ArrayList<String>(Arrays.asList(mMailworktype
						.split(":")));
			} else if (mMailworktype.equalsIgnoreCase("")) {
				mMailworktype = getResources().getString(R.string.None);
				mListValues.add(mMailworktype);
			} else if (!mMailworktype.equalsIgnoreCase("")
					&& !mMailworktype.contains(":")) {

				mMailworktype = getResources().getString(R.string.None) + ":"
						+ mMailworktype;
				mListValues = new ArrayList<String>(Arrays.asList(mMailworktype
						.split(":")));
			}
		}
		mWorktypes = new String[mListValues.size()];
		mWorktypes = mListValues.toArray(mWorktypes);

	}

	/**
	 * Worktype list will be shown.
	 */
	public void promptWorktypeList() {
		String title = "";
		ListView workTypeList = null;
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mBuilder = new AlertDialog.Builder(DictateActivity.this);
		title = getResources().getString(R.string.Select);
		setWorktype();
		View layout = inflater.inflate(
				R.layout.activity_dictate_property_worktype_list,
				(ViewGroup) this.findViewById(R.id.relWorktypeView));

		String setWork = tvWorktype.getText().toString();
		int pos = 0;
		for (int i = 0; i < mWorktypes.length; i++) {
			if (setWork.equalsIgnoreCase(getResources().getString(
					R.string.Settings_Select))) {
				setWork = getResources().getString(R.string.None);
			}
			if (mWorktypes[i].equalsIgnoreCase(setWork)) {
				pos = i;
				break;
			}
		}
		workTypeList = (ListView) layout.findViewById(R.id.lstWorktype);
		workTypeList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		ArrayAdapter<String> referal_adapter = new ArrayAdapter<String>(
				DictateActivity.this,
				android.R.layout.simple_spinner_dropdown_item, mWorktypes);
		workTypeList.setAdapter(referal_adapter);
		workTypeList.setItemChecked(pos, true);
		workTypeList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapterView, View view,
					int pos, long arg3) {
				String selectedWorktype = (String) adapterView
						.getItemAtPosition(pos);
				if (selectedWorktype.equalsIgnoreCase(getResources().getString(
						R.string.None))) {
					tvWorktype.setText(getResources().getString(
							R.string.Settings_Select));
					dictCard.setWorktype("");
				} else {
					tvWorktype.setText(selectedWorktype);
					dictCard.setWorktype(selectedWorktype);
				}
				mDbHandler.updateWorktype(dictCard);
				mAlertDialog.dismiss();
			}
		});
		mBuilder.setView(layout);
		mAlertDialog = mBuilder.create();
		mAlertDialog.setTitle(title);
		mAlertDialog.show();
		if (!isKeyboardShown)
			tvTitle.setCursorVisible(false);
	}

	/**
	 * Sets recording source as Bluetooth.
	 */
	private void setBluetoothSource() {
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		bluetoothConnectivity = pref.getBoolean(
				getResources().getString(R.string.blutooth_input_key), false);
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if ((mBluetoothAdapter.isEnabled()) && bluetoothConnectivity == true) {
			audioManager.setBluetoothScoOn(true);
			audioManager.startBluetoothSco();

		} else if (!mBluetoothAdapter.isEnabled()
				|| bluetoothConnectivity == false) {
			audioManager.setBluetoothScoOn(false);
			audioManager.stopBluetoothSco();

		}
	}

	/**
	 * When the delivery option of dictation in server is client based, then
	 * shows dialog to choose the delivery option(E-mail or FTP).
	 */
	private void promptSendRecordings() {
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder mBuilder = new AlertDialog.Builder(
				DictateActivity.this);
		View layout = inflater.inflate(R.layout.dictate_prompt_dialog_layout,
				(ViewGroup) this
						.findViewById(R.id.relative_property_image_menu));
		Button mEmail = (Button) layout
				.findViewById(R.id.btn_property_image_takephoto);
		Button mFtp = (Button) layout
				.findViewById(R.id.btn_property_image_choose_existing);
		Button mCancel = (Button) layout
				.findViewById(R.id.btn_property_image_cancel);
		Button mPending = (Button) layout
				.findViewById(R.id.btn_property_pending);
		mEmail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isSplittable = 1;
				onAfterSettingsDelivery();
				mAlertDialog.dismiss();
			}
		});
		mFtp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isSplittable = 0;
				onAfterSettingsDelivery();
				mAlertDialog.dismiss();
			}
		});
		mPending.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dictCard.setStatus(DictationStatus.PENDING.getValue());
				dictCard.setRecEndDate(dmApplication.getDeviceTime());
				mDbHandler.updateDictationStatusAndEndDate(dictCard);
				mAlertDialog.dismiss();
			}
		});
		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mAlertDialog.dismiss();
			}
		});
		mBuilder.setView(layout);
		mAlertDialog = mBuilder.create();

		mAlertDialog.show();
	}

	/**
	 * Assign server settings to the dictation, and continue sending operations
	 * to an ODP.
	 */
	private void onAfterSettingsDelivery() {
		pref = PreferenceManager
				.getDefaultSharedPreferences(DictateActivity.this);
		editor = pref.edit();
		editor.putString(getResources().getString(R.string.Audio_delivery),
				mAudioDelivery);
		editor.putString(getResources()
				.getString(R.string.Audio_Encryption_key), mAudioEncrypt);
		editor.putString(getResources().getString(R.string.Audio_Format_key),
				mAudioFormat);
		editor.putString(getResources().getString(R.string.Audio_Password_key),
				mAudioPassword);
		editor.putString(
				getResources().getString(R.string.Worktype_Server_key),
				mWorktype);
		editor.putString(
				getResources().getString(R.string.Worktype_List_name_Key),
				mWorktypeListname);
		editor.putString(getResources().getString(R.string.author_key),
				mAuthorName);
		editor.commit();
		if(mResultcode != null && mResultcode.equalsIgnoreCase("2000")) {
			pref = PreferenceManager.getDefaultSharedPreferences(this);
			if(pref.getString(getResources().getString(R.string.author_key), "").equalsIgnoreCase(mAuthorName)) 
				mDbHandler.updateDicationName(settingParser.getSettingsObjects().get(0).getAuthor());
		}
		onUploadHandler(true);
	}

	/**
	 * Get all basic values from application preferences for ILS Communication.
	 */
	private void getSettingsAttribute() {
		pref = this.getSharedPreferences(PREFS_NAME, 0);
		if (pref.getString("UUID", mGetuuid) != null)
			prefUUID = pref.getString("UUID", mGetuuid);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		mUrl = dmApplication.getUrl();
		if (pref.getString(getResources().getString(R.string.email_key),
				mGetemail) != null)
			mEmail = pref.getString(getResources()
					.getString(R.string.email_key), mGetemail);
		baseEncoding = new Base64_Encoding();
		base64value = baseEncoding.base64(prefUUID + ":" + mEmail);
	}

	/**
	 * Perform basic initialization and prepare to send dictation to server.
	 * 
	 * @param isServer
	 *            send option application's settings
	 */
	private void onUploadHandler(boolean isServer) {
		try {
			dictCard.setStatus(DictationStatus.PENDING.getValue());
			mDbHandler.updateDictationsFiles(dictCard);
			hasMultipleSplits = false;
			hasNoEncript = false;
			pref = PreferenceManager
					.getDefaultSharedPreferences(DictateActivity.this);
			if (isServer) {
				dictCard.setFileSplittable(isSplittable);

				dictCard.setStatus(DictationStatus.OUTBOX.getValue());
				mDbHandler.updateDictationStatus(dictCard.getDictationId(),
						dictCard.getStatus());
				dictCard.setDssVersion(Integer.parseInt(pref.getString(
						getResources().getString(R.string.Audio_Format_key),
						mAudioFormat)));
				dictCard.setDssEncryptionPassword(pref
						.getString(
								getResources().getString(
										R.string.Audio_Password_key),
								mAudioPassword));
				dictCard.setEncryptionVersion(Integer.parseInt(pref
						.getString(
								getResources().getString(
										R.string.Audio_Encryption_key),
								mAudioEncrypt)));
				dictCard.setAuthor(pref.getString(
						getResources().getString(R.string.author_key),
						mAuthorName));
				if (dictCard.getEncryptionVersion() > 0)
					dictCard.setEncryption(1);
				else
					hasNoEncript = true;
				if (isSplittable == 1)
					dictCard.setDeliveryMethod(1);
				else
					dictCard.setDeliveryMethod(2);
				mDbHandler.updateSettingsAttributes(dictCard);
				isSplittable = 0;

				filePath = DMApplication.DEFAULT_DIR + "/Dictations/"
						+ dictCard.getSequenceNumber() + "/"
						+ dictCard.getDictFileName();
				cardSize = 0;
				if (dictCard.getIsThumbnailAvailable() == 1) {
					file = new File(filePath + ".jpg");
					if (!file.exists()) {
						dictCard.setIsThumbnailAvailable(0);
						mDbHandler.updateIsThumbnailAvailable(dictCard);
					} else if (dictCard.isFileSplittable() == 1)
						cardSize = dmApplication.getImageFileSize(filePath);
				}
				file = new File(filePath + ".wav");
				if (file.exists()) {
					if (dictCard.isFileSplittable() == 1) {
						cardSize = cardSize
								+ DMApplication.getExpectedDSSFileSize(
										dictCard.getDssVersion(), filePath);
						if (cardSize > maxSize)
							hasMultipleSplits = true;
					}
					dictCard.setGroupId(mDbHandler.getGroupId());
					mDbHandler.updateGroupId(dictCard.getDictationId(),
							dictCard.getGroupId());
				} else {
					dictCard.setStatus(DictationStatus.SENDING_FAILED
							.getValue());
					mDbHandler.updateDictationStatus(dictCard.getDictationId(),
							dictCard.getStatus());
				}
				file = null;
				dictCard.setIsActive(0);
				mDbHandler.updateIsActive(dictCard);

				baseIntent = new Intent("com.olympus.dmmobile.action.Test");
				baseIntent.putExtra("isWantToUpdate", true);
				sendBroadcast(baseIntent);// send a notification to server.
				if (hasNoEncript && !pref.getBoolean(DONT_SHOW_KEY, false))
					promptSecurityDialog();
				else if (hasMultipleSplits)
					onSendToServerEmailHasAbove23MB();
				isSendClicked = true;
				dmApplication.lastDictMailSent = true;
				wasResume = false;
				dmApplication.fromWhere = 3;
				dmApplication.isExecuted = true;
				newRecordingClicked();

			} else {
				dictCard.setAuthor(pref
						.getString(getResources()
								.getString(R.string.author_key), "AUTHOR"));
				mDbHandler.updateAuthor(dictCard);
				new SendViaEmailTask().execute();
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Shows notification message, when the selected dictation has size more
	 * than 23MB.
	 */
	private void onSendToServerEmailHasAbove23MB() {
		AlertDialog.Builder alert = new AlertDialog.Builder(
				DictateActivity.this);
		alert.setCancelable(true);
		alert.setMessage(getResources().getString(
				R.string.Dictate_Alert_Recording_File_Large_Message));
		alert.setPositiveButton(
				getResources().getString(R.string.Dictate_Alert_Ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alert.create().show();
	}

	/**
	 * Creates custom chooser for available email clients in device. Invokes
	 * when user taps Send via Email.
	 */
	private void shareViaEmail() {
		pref = getSharedPreferences(EMAIL_PREF_NAME, MODE_PRIVATE);
		String prefPackName = pref.getString(EMAIL_PREF_KEY, "");
		if (prefPackName.equals("")) {
			baseIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
			baseIntent.setType("message/rfc822");

			List<ResolveInfo> activities = getPackageManager()
					.queryIntentActivities(baseIntent, 0);
			List<ResolveInfo> activities1 = new ArrayList<ResolveInfo>();
			if (!activities.isEmpty()) {

				for (int i = 0; i < activities.size(); i++) {
					if (activities.get(i).activityInfo.packageName
							.toLowerCase().contains("mail")
							|| activities.get(i).activityInfo.packageName
									.toLowerCase().contains("gm")) {
						activities1.add(activities.get(i));
					}
				}
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Email client");
			final ChooserIntentListAdapter adapter = new ChooserIntentListAdapter(
					this, R.layout.flashair_email_chooser_row,
					activities1.toArray());
			builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ResolveInfo info = (ResolveInfo) adapter.getItem(which);
					String packageName = info.activityInfo.packageName;
					Editor editor = pref.edit();
					editor.putString(EMAIL_PREF_KEY, packageName);
					editor.commit();
					sendEmail(packageName);
				}
			});
			builder.create().show();
		} else {
			sendEmail(prefPackName);
		}
	}

	/**
	 * Email client will be invoked with multiple Dictations as attachment.
	 * 
	 * @param packageName
	 *            Package name of the selected Email client.
	 */
	private void sendEmail(String packageName) {
		isSendClicked = true;
		dictCard.setIsActive(0);
		mDbHandler.updateIsActive(dictCard);
		dictCard.setIsConverted(1);
		mDbHandler.updateIsConverted(dictCard);
		baseIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		baseIntent.setType("text/calendar");
		// emailIntent.setType("message/rfc822");
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		String[] recipient;
		if (pref.getString(getResources().getString(R.string.recipient_key), "")
				.contains(","))
			recipient = pref.getString(
					getResources().getString(R.string.recipient_key), "")
					.split(",");
		else
			recipient = new String[] { pref.getString(
					getResources().getString(R.string.recipient_key), "") };
		baseIntent.putExtra(Intent.EXTRA_EMAIL, recipient);
		baseIntent.putExtra(Intent.EXTRA_SUBJECT, pref.getString(getResources()
				.getString(R.string.subject_key), "Dictations"));
		baseIntent.putExtra(Intent.EXTRA_TEXT, pref.getString(getResources()
				.getString(R.string.message_key),
				"Please find the attached files"));
		ArrayList<Uri> uris = new ArrayList<Uri>();
		file = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
				+ dictCard.getSequenceNumber() + "/",
				dictCard.getDictationName() + ".amr");
		uris.add(Uri.fromFile(file));
		if (dictCard.getIsThumbnailAvailable() == 1) {
			file = new File(DMApplication.DEFAULT_DIR + "/Dictations/"
					+ dictCard.getSequenceNumber() + "/"
					+ dictCard.getDictationName() + ".jpg");
			uris.add(Uri.fromFile(file));
		}
		baseIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		baseIntent.setPackage(packageName);
		mDbHandler.updateDictationStatus(dictCard.getDictationId(),
				DictationStatus.SENT_VIA_EMAIL.getValue());
		startActivityForResult(baseIntent, EMAIL_SEND_REQ_CODE);
		wasResume = false;
		baseIntent = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (EMAIL_SEND_REQ_CODE == requestCode) {
			dmApplication.lastDictMailSent = true;
			dmApplication.fromWhere = 3;
			dmApplication.isExecuted = true;
			newRecordingClicked();

		}
	}

	/**
	 * An AsyncTask to perform Insertion or Overwrite process in the background
	 */
	private class InsertOrOverwriteTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			if (isVcvaEnabled) {
				mSignalometer.setVisibility(View.INVISIBLE);
				mOverwriteSignalometer.setVisibility(View.INVISIBLE);
			}
			if (!isInsertionProcessRunning) {
				dialog = new ProgressDialog(DictateActivity.this);
				dialog.setCancelable(false);
				dialog.show();
				isInsertionProcessRunning = true;
			}

		}

		@Override
		protected Void doInBackground(Void... params) {

			process_audio();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			if (timer != null || mptimer != null) {
				timer.cancel();
				task.cancel();
				mptimer.cancel();
				mpTimertask.cancel();
			}
			if (isInsert) {
				drawWaveGraph();
			} else {
				drawOverwriteGraph();
			}
			updateTimerandgraph();
			if (OverwriteLimitExceeded) {
				RECORDER.resetRecordingMode(getFilename(),
						RECORDER.FILE_EXISTING);
				isOncePaused = false;
			}

			if (!rewindPressed && !isRecording) {
				if (mMediaPlayer != null) {
					if (playButtonState == MediaMode.PLAY) {
						mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar
								.getProgress()));
						mMediaPlayer.start();
					} else if (playButtonState == MediaMode.START) {
						if (!rewindStratPlay) {
							mediaPlayerPrepareAudioFile(false);
							mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar
									.getProgress()));
						} else {
							mediaPlayerPrepareAudioFile(true);
							mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar
									.getProgress()));
							rewindStratPlay = false;
						}
					}
				} else {
					if (ActivityInitialPlay) {
						updateTimerandgraph();
						mediaPlayerPrepareAudioFile(true);
						mMediaPlayer.seekTo((int) getMMcurrentPos(Graphbar
								.getProgress()));
						rewindStratPlay = false;
						ActivityInitialPlay = false;
					}
				}

				rewindPressed = false;
			}
			if (isVcvaEnabled && isRecording) {
				bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
			}
			isInsertionProcessRunning = false;

		}
	}

	/**
	 * To Get updated settings from an ODP Server.
	 */
	private class WebServiceGetSettings extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(DictateActivity.this);
			dialog.setCancelable(false);
			dialog.setMessage(getResources()
					.getString(R.string.Dictate_Loading));
			dialog.show();
			isCriticalErrorOccures = false;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				mGetSettingsResponse = webserviceHandler.onRequestBackgroundSettings(mUrl + "/" + prefUUID + "/Settings", base64value);
				if (mGetSettingsResponse != null) {
					if (!mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
						settingParser = new Settingsparser(mGetSettingsResponse);
						settingParser.parse(getlanguage());
						mResultcode = settingParser.getRootObjects().get(0).getResult_code().trim();
					}
				}
			} catch (Exception e) {
				mGetSettingsResponse = null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			isSplittable = 0;
			try {
				if (mGetSettingsResponse != null) {
					if (!mGetSettingsResponse.equalsIgnoreCase("TimeOut")) {
						if (dialog.isShowing())
							dialog.dismiss();
						/*
						 * perform some actions based on the response.
						 */
						if (mResultcode.equalsIgnoreCase("2000")) {
                            // if the response is success
							onRefreshDictation();
							mWorktype = null;
							worktypeobject = settingParser
									.getWorkTypeListObjects();
							for (int i = 0; i < worktypeobject.size(); i++) {
								if (mWorktype == null) {
									mWorktype = worktypeobject.get(i)
											.getWorktype();
								} else {
									mWorktype = mWorktype
											+ ":"
											+ worktypeobject.get(i)
													.getWorktype();
								}
							}
							mAudioDelivery = settingParser.getSettingsObjects()
									.get(0).getDelivery();
							mAudioEncrypt = settingParser.getAudioObjects()
									.get(0).getEncryption();
							mAudioFormat = settingParser.getAudioObjects()
									.get(0).getFormat();
							mAudioPassword = settingParser.getAudioObjects()
									.get(0).getPassword();
							mWorktypeListname = settingParser
									.getSettingsObjects().get(0)
									.getWorktypelist();
							mAuthorName = settingParser.getSettingsObjects()
									.get(0).getAuthor();
						} else if (mResultcode.equalsIgnoreCase("5000")
								|| mResultcode.equalsIgnoreCase("5001")) {
							onSetRecentSettings();
						}
						/*
						 * To check the result is critical error.
						 */
						else if (mResultcode.equalsIgnoreCase("4000")
								|| mResultcode.equalsIgnoreCase("4006")
								|| mResultcode.equalsIgnoreCase("4007")
								|| mResultcode.equalsIgnoreCase("4008")
								|| mResultcode.equalsIgnoreCase("4009")
								|| mResultcode.equalsIgnoreCase("5002")) {
							isCriticalErrorOccures = true;
							baseIntent = new Intent(
									"com.olympus.dmmobile.action.Test");
							baseIntent.putExtra("isWantWholeToTimeOut", true);
							sendBroadcast(baseIntent);// send a notification to
														// background.
							baseIntent = null;
							if (mResultcode.equalsIgnoreCase("4007")) {
								pref = getSharedPreferences(PREFS_NAME, 0);
								editor = pref.edit();
								editor.putString("Activation", "Not Activated");
								editor.commit();
							}
							/*
							 * show critical error dialog with respect to the
							 * response.
							 */
							if (!dmApplication.isTimeOutDialogOnFront()) {
								dmApplication.setTimeOutDialogOnFront(true);
								dmApplication.setErrorCode(mResultcode);
								baseIntent = new Intent(dmApplication,
										CustomDialog.class);
								if (mResultcode.equalsIgnoreCase("5002"))
									dmApplication.setErrorMessage(settingParser
											.getMessage());
								baseIntent
										.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(baseIntent);
								baseIntent = null;
							}
						} else if (mResultcode.equalsIgnoreCase("4001")
								|| mResultcode.equalsIgnoreCase("4002")
								|| mResultcode.equalsIgnoreCase("4003")
								|| mResultcode.equalsIgnoreCase("4004")
								|| mResultcode.equalsIgnoreCase("4005")) {
							onSetRecentSettings();
						} else
							onSetRecentSettings();
					} else {
						if (dialog.isShowing())
							dialog.dismiss();
						onSetRecentSettings();
					}
				} else {
					if (dialog.isShowing())
						dialog.dismiss();
					onSetRecentSettings();
				}
				if (!isCriticalErrorOccures) {
					if (mAudioDelivery.trim().equalsIgnoreCase("3"))
						promptSendRecordings();
					else {
						if (mAudioDelivery.equalsIgnoreCase("1"))
							isSplittable = 1;
						else
							isSplittable = 0;
						onAfterSettingsDelivery();
					}
				}
			} catch (Exception e) {
			}
		}
	}
	/**
	 * Set dictation name when rename process is performed.
	 */
	public void onRefreshDictation() {
		if(tvTitle != null && dictCard != null) {
			dictCard = mDbHandler.getDictationCardWithId(dictCard.getDictationId());
			tvTitle.setText(dictCard.getDictationName());
		}
	}

	/**
	 * When any httpError occurs during the ILS communication, then get basic
	 * values of server settings from an Application preference for initiate
	 * settings to the dictation.
	 */
	private void onSetRecentSettings() {
		pref = PreferenceManager
				.getDefaultSharedPreferences(DictateActivity.this);
		mAudioDelivery = pref.getString(
				getResources().getString(R.string.Audio_delivery),
				mAudioDelivery);
		mAudioEncrypt = pref.getString(
				getResources().getString(R.string.Audio_Encryption_key),
				mAudioEncrypt);
		mAudioFormat = pref.getString(
				getResources().getString(R.string.Audio_Format_key),
				mAudioFormat);
		mAudioPassword = pref.getString(
				getResources().getString(R.string.Audio_Password_key),
				mAudioPassword);
		mWorktype = pref.getString(
				getResources().getString(R.string.Worktype_Server_key),
				mWorktype);
		mWorktypeListname = pref.getString(
				getResources().getString(R.string.Worktype_List_name_Key),
				mWorktypeListname);
		mAuthorName = pref.getString(
				getResources().getString(R.string.author_key), mAuthorName);
	}

	/**
	 * To convert the selected dictations file to 'amr' in foreground.
	 */
	private class SendViaEmailTask extends AsyncTask<Void, Void, Void> {
		private Dialog dialog;
		private TextView txtvAMRFileName = null, txtvMessage = null;
		private ArrayList<String> removedCards = null;
		private boolean conversionFailed = false;
		private boolean hasNoMemory = false;

		public SendViaEmailTask() {
			removedCards = new ArrayList<String>();
		}

		@Override
		protected void onPreExecute() {
			dialog = new Dialog(DictateActivity.this);
			dialog.setTitle(getResources().getString(
					R.string.Dictate_Alert_File_Conversion));
			dialog.setCancelable(false);
			dialog.setContentView(R.layout.amr_sending_dialog);
			txtvAMRFileName = (TextView) dialog
					.findViewById(R.id.txtvAmrFileName);
			txtvAMRFileName.setText(getResources().getString(
					R.string.Dictate_Alert_Compressing)
					+ " " + dictCard.getDictationName());
			txtvMessage = (TextView) dialog.findViewById(R.id.txtvMessage);
			txtvMessage
					.setText(getResources()
							.getString(
									R.string.Dictate_Alert_Compress_Recording_Background_message));
			dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			filePath = DMApplication.DEFAULT_DIR + "/Dictations/"
					+ dictCard.getSequenceNumber() + "/";
			file = new File(filePath + dictCard.getDictFileName() + ".wav");
			stat = new StatFs(Environment.getExternalStorageDirectory()
					.getPath());
			if (file.exists()) {
				/*
				 * To check there is available memory present for all
				 * dictation's file to 'amr' conversion.
				 */
				if ((file.length() / 19.98) < ((long) stat.getAvailableBlocks() * (long) stat
						.getBlockSize())) {
					if (dictCard.getDuration() > 999) {
						amrConverter = new AMRConverter();// Initialize the
															// 'amr' converter.
						/*
						 * To check the conversion is success or not.
						 */
						if (amrConverter
								.convert(filePath + dictCard.getDictFileName()
										+ ".wav",
										filePath + dictCard.getDictationName()
												+ ".amr") == 0) {
							dictCard.setSentDate(dmApplication.getDeviceTime());
							mDbHandler.updateSentDate(dictCard);
							if (dictCard.getIsThumbnailAvailable() == 1) {
								try {
									file = new File(DMApplication.DEFAULT_DIR
											+ "/Dictations/"
											+ dictCard.getSequenceNumber()
											+ "/" + dictCard.getDictFileName()
											+ ".jpg");
									if (file.exists()) {
										if (!dictCard
												.getDictFileName()
												.equalsIgnoreCase(
														dictCard.getDictationName())) {
											mtempFile = DMApplication.DEFAULT_DIR;
											if (mtempFile.canWrite()) {
												mtempFile = new File(
														DMApplication.DEFAULT_DIR
																+ "/Dictations/"
																+ dictCard
																		.getSequenceNumber()
																+ "/"
																+ dictCard
																		.getDictationName()
																+ ".jpg");
												srcChannel = new FileInputStream(
														file).getChannel();
												dstChannel = new FileOutputStream(
														mtempFile).getChannel();
												dstChannel.transferFrom(
														srcChannel, 0,
														srcChannel.size());
												srcChannel.close();
												dstChannel.close();
												srcChannel = null;
												dstChannel = null;
											}
										}
									} else {
										dictCard.setIsThumbnailAvailable(0);
										mDbHandler
												.updateIsThumbnailAvailable(dictCard);
									}
								} catch (Exception e) {
								}
								file = null;
								mtempFile = null;
							}
						} else
							conversionFailed = true;
					} else
						conversionFailed = true;
				} else
					hasNoMemory = true;
			} else
				removedCards.add(dictCard.getDictationName());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (dialog.isShowing())
				dialog.dismiss();
			if (!hasNoMemory) {
				if (conversionFailed)
					onConvertionFailed();
				else if (removedCards.size() > 0) {
					isSendClicked = true;
					dictCard.setIsActive(0);
					mDbHandler.updateIsActive(dictCard);
					dictCard.setIsConverted(1);
					dictCard.setStatus(DictationStatus.PENDING.getValue());
					mDbHandler.updateDictationStatus(dictCard.getDictationId(),
							DictationStatus.PENDING.getValue());
					mDbHandler.updateIsConverted(dictCard);
					wasResume = false;
					newRecordingClicked();
					onFileMissing(removedCards);
				} else
					shareViaEmail();
			} else {
				/*
				 * Alert when their memory has not available.
				 */
				file = null;
				AlertDialog limitDialog = new AlertDialog.Builder(
						DictateActivity.this).create();
				limitDialog.setTitle(getResources().getString(
						R.string.Dictate_No_Space));
				limitDialog.setMessage(getResources().getString(
						R.string.Dictate_Low_Memory));
				limitDialog.setButton(
						getResources().getString(R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						});
				limitDialog.show();
			}
			System.gc();
		}
	}

	/**
	 * Invokes when Conversion fails. Dialog will be shown with conversion
	 * failed message.
	 */
	private void onConvertionFailed() {
		alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle(getResources().getString(R.string.Settings_Error));
		alertDialog.setMessage(getResources().getString(
				R.string.Property_Conversion_Failed));
		alertDialog.setPositiveButton(
				getResources().getString(R.string.Dictate_Alert_Ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alertDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dictCard.setIsActive(0);
				mDbHandler.updateIsActive(dictCard);
				isSendClicked = true;
				wasResume = false;
				newRecordingClicked();
			}
		});
		alertDialog.create().show();
	}

	/**
	 * Show the missing file names, when the selected dictation's corresponding
	 * file's are not in SDcard.
	 * 
	 * @param filNames
	 *            file names
	 */
	private void onFileMissing(ArrayList<String> filNames) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setMessage(getResources().getString(R.string.SourceNotfound));
		ListView fileList = new ListView(this);
		fileList.setCacheColorHint(Color.TRANSPARENT);
		fileList.setBackgroundColor(Color.WHITE);
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, filNames);
		fileList.setAdapter(modeAdapter);
		alert.setPositiveButton(
				getResources().getString(R.string.Dictate_Alert_Ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alert.setView(fileList);
		alert.create().show();
	}

	/**
	 * BroadcastReceiver which handles most of the interrupts such as
	 * DEVICELOCK(during recording or playing) DEVICESHUTDOWN(recording or
	 * playing) DEVICE POWER OFF(shutdown the device) BATTERY STATE CHANGED(when
	 * battery level <10) DEVICE CALL OCCURED(when call occurs, and call
	 * disconnected)
	 */
	private class DeviceLockInterrupt extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			/**
			 * WHEN SCREEN GETS ON(DURING SCREEN UNLOCK)
			 */
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				if (actionFlag == 1) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {

						e.printStackTrace();
					}

					displayDialogOnScreen(actionFlag);

					actionFlag = 0;
				} else if (actionFlag == 2) {

				}
			}
			/**
			 * WHEN SCREEN OFF(DURING SCREEN LOCK)
			 */
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

				if (!isBeyondLimit)
					if (!limitFlag) {
						if (!isReview && !isNew) {
							
							SavedDicID = dictCard.getDictationId();
							passedModeName = "";
							Intent Tmpintent = getIntent();
							Tmpintent.putExtra(DMApplication.START_MODE_TAG,
									passedModeName);
							setIntent(Tmpintent);
						}
					}

			}
			/**
			 * DEVICE GOT SHUTDOWN
			 */
			else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
				if (isRecording) {
					recorderStopRecording();
					dictCard.setStatus(DictationStatus.PENDING.getValue());
					mDbHandler.updateDictationStatus(dictCard.getDictationId(),
							dictCard.getStatus());
				}
			}

			else if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {

			}
			/**
			 * DEVICE CALL OCCURED
			 */
			else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
					.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
				try {
					isCallActive = true;
					dmApplication.lastDictMailSent = false;
					if (isForwarding == 1 || isRewinding == 1) {
						if (timer != null || mptimer != null) {
							timer.cancel();
							task.cancel();
							mptimer.cancel();
							mpTimertask.cancel();
						}
						playButtonState = MediaMode.START;
						bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
					}
					if (isRecording) {
						recorderStopRecording();
						if (Graphbar.getProgress() < Graphbar.getMax()
								&& Graphbar.getProgress() > 0) {
							isOncePaused = true;
						} else {
							Graphbar.setProgress(Graphbar.getMax());
						}
						actionFlag = 1;
						if (!isPushToTalk) {
							displayDialogOnScreen(actionFlag);
						}
					} else if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							actionFlag = 2;
							playButtonState = MediaMode.PAUSE;
							recorderSetEnable(true);
							mediaPlayerUpdatebottomBar(true);
							mMediaPlayer.pause();
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
							}
							OverwriteLimitExceeded = false;
							savedCurrentPos = getMMcurrentPos(Graphbar
									.getProgress());
							pausePosition = mMediaPlayer.getCurrentPosition();
							fileSize = getPlayBackFileSize();
							continue_process = false;
							bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
						}

					}
					if (Graphbar.getProgress() < Graphbar.getMax()
							&& Graphbar.getProgress() > 0) {

						dmApplication.passMode = "";
						SavedDicID = dictCard.getDictationId();
						passedModeName = "";
						Intent Cintent = getIntent();
						Cintent.putExtra(DMApplication.START_MODE_TAG,
								passedModeName);
						setIntent(Cintent);
					}

				} catch (Exception e) {
				}
			}
			/**
			 * DEVICE CALL ENDS
			 */
			else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
					.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
				if (isCallActive) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					actionFlag = 0;
					isCallActive = false;

				}
			}

		}
	}

	public class OutgoingCallReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO: Handle outgoing call event here
			try {
				isCallActive = true;
				if (isRecording) {
					recorderStopRecording();
					actionFlag = 1;
					displayDialogOnScreen(actionFlag);
				} else if (mMediaPlayer.isPlaying()) {
					if (mMediaPlayer != null) {
						actionFlag = 2;
						playButtonState = MediaMode.PAUSE;
						recorderSetEnable(true);
						mediaPlayerUpdatebottomBar(true);
						mMediaPlayer.pause();
						if (Graphbar.getProgress() < Graphbar.getMax()
								&& Graphbar.getProgress() > 0) {
							isOncePaused = true;
						}
						OverwriteLimitExceeded = false;
						savedCurrentPos = getMMcurrentPos(Graphbar
								.getProgress());
						pausePosition = mMediaPlayer.getCurrentPosition();
						fileSize = getPlayBackFileSize();
						continue_process = false;
						bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
					}

				}
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Shows dialog when any type of interrupt occurs while recording or
	 * playing.
	 */
	public void displayDialogOnScreen(final int actionFlag) {
		if (isNew) 
			bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
		else 
			bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
		if(mInterruptAlert != null && mInterruptAlert.isShowing())
			return;
		mInterruptAlert = new AlertDialog.Builder(DictateActivity.this)
				.create();
		mInterruptAlert.setTitle(getResources().getString(R.string.Alert));
		mInterruptAlert.setCancelable(false);
		if (actionFlag == 1)
			mInterruptAlert.setMessage(getInterruptionString(true));
		mInterruptAlert.setButton(AlertDialog.BUTTON_POSITIVE, getResources()
				.getString(R.string.Dictate_Alert_Continue),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (actionFlag == 2) {
							if (mMediaPlayer != null) {
								playButtonState = MediaMode.PLAY;
								recorderSetEnable(false);
								mediaPlayerUpdatebottomBar(false);
								mMediaPlayer.start();
								bPlay.setBackgroundResource(R.drawable.dictate_pause_selector);
							}
						} else if (actionFlag == 1) {

							if (!isCallActive) {
								if (!isPushToTalk && !isVcvaEnabled) {
									if (isInsert) {
										showSignalometer();
									} else {
										showOverwriteSignalometer();
									}
									if (validateSpaceAndSizeLimit()) {
										recorderTriggerStart();
									}
								} else if (isVcvaEnabled) {
									if (validateSpaceAndSizeLimit()) {
										recorderTriggerStart();
									}
								}
							} else {
								DialogWhileActiveCall();
							}
						}
						InterruptCallDialogshown = false;
						dialog.cancel();
					}
				});
		mInterruptAlert.setButton(AlertDialog.BUTTON_NEGATIVE, getResources()
				.getString(R.string.Dictate_Alert_Stop),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mMediaPlayer != null) {
							if (Graphbar.getProgress() < Graphbar.getMax()
									&& Graphbar.getProgress() > 0) {
								isOncePaused = true;
								pausePosition = getMMcurrentPos(Graphbar
										.getProgress());
								OverwriteLimitExceeded = false;
							}
						}
						InterruptCallDialogshown = false;
						dialog.cancel();
					}
				});
		InterruptCallDialogshown = true;
		mInterruptAlert.show();
	}

	/**
	 * Shows dialog when tries to record or play during the call is active.
	 */
	public void DialogWhileActiveCall() {
		if (isNew) {
			bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
			tvTimeRight.setVisibility(View.INVISIBLE);
		} else {
			bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
		}
		AlertDialog alertDialog = new AlertDialog.Builder(DictateActivity.this)
				.create();
		alertDialog.setTitle(R.string.Alert);
		alertDialog.setCancelable(false);
		alertDialog.setMessage(getResources().getString(
				R.string.Dictate_Disconnect_Call));

		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources()
				.getString(R.string.Dictate_Alert_Ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (isNew) {
							bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
						} else {
							if (timer != null || mptimer != null) {
								timer.cancel();
								task.cancel();
								mptimer.cancel();
								mpTimertask.cancel();
							}
							bNew.setEnabled(true);
							bNew.setBackgroundResource(R.drawable.dictate_new_selector);
							updateTimerandgraph();
							mediaPlayerPrepareAudioFile(false);
						}
						dialog.cancel();
					}
				});
		if (isNew) {
			bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
		}
		alertDialog.show();
	}

	/**
	 * Sets Current Language to Locale based on the input value passed.
	 * 
	 * @param value
	 *            Language code as int
	 */
	private void setCurrentLanguage(int value) {
		switch (value) {
		case 1:
			setLocale("en");
			break;
		case 2:
			setLocale("de");
			break;
		case 3:
			setLocale("fr");
			break;
		case 4:
			setLocale("es");
			break;
		case 5:
			setLocale("sv");
			break;
		case 6:
			setLocale("cs");
			break;
		default:
			setLocale("en");
			break;
		}
	}

	/**
	 * Updates Locale with passed Language code.
	 * 
	 * @param lang
	 *            Language code as String.
	 */
	private void setLocale(String lang) {
		locale = new Locale(lang);
		Resources res = getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		Configuration conf = res.getConfiguration();
		conf.locale = locale;
		res.updateConfiguration(conf, dm);
		res = null;
		dm = null;
		conf = null;
		lang = null;
	}

	private void UpdateAudioHeader(String inFile) {

		long totalAudioLen = 0;
		long totalDataLen = 0; // totalAudioLen + 36
		FileInputStream in = null;
		try {
			in = new FileInputStream(inFile);
			totalAudioLen = (in.getChannel().size() - 44);

			int reminder = (int) (totalAudioLen % 2);
			if (reminder > 0) {
				totalAudioLen = totalAudioLen - reminder;
			}

			totalDataLen = (totalAudioLen) + 36;
			RandomAccessFile invFile = new RandomAccessFile(inFile, "rw");
			invFile.seek(4);
			invFile.write((byte) (totalDataLen & 0xff));
			invFile.write((byte) ((totalDataLen >> 8) & 0xff));
			invFile.write((byte) ((totalDataLen >> 16) & 0xff));
			invFile.write((byte) ((totalDataLen >> 24) & 0xff));
			invFile.seek(40);
			invFile.write((byte) (totalAudioLen & 0xff));
			invFile.write((byte) ((totalAudioLen >> 8) & 0xff));
			invFile.write((byte) ((totalAudioLen >> 16) & 0xff));
			invFile.write((byte) ((totalAudioLen >> 24) & 0xff));
			invFile.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		System.gc();
	}

	@Override
	public void onWriteLimitExceeded() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (RECORDER != null) {
					// TODO Auto-generated method stub
					recorderStopRecording();
				} else {
					if (isInsert) {
						showSignalometer();
					} else {
						showOverwriteSignalometer();
					}
				}
				if (!isNew) {
					if (isInsert) {
						drawWaveGraph();
					} else {
						drawOverwriteGraph();
					}
				}

				AlertDialog limitDialog = new AlertDialog.Builder(
						DictateActivity.this).create();
				limitDialog.setTitle(getResources().getString(R.string.Alert));
				limitDialog.setMessage(getResources().getString(
						R.string.Dictate_Alert_Maximum_Recording_Message));
				limitDialog.setCancelable(false);
				limitDialog.setButton(
						getResources().getString(R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (mWakeLock.isHeld()) {
									mWakeLock.release();
								}
								dialog.cancel();
								runOnUiThread(new Runnable() {
									public void run() {
										bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
										updateTimerandgraph();
										mediaPlayerPrepareAudioFile(false);
										bNew.setBackgroundResource(R.drawable.dictate_new_selector);
										mediaPlayerUpdatebottomBar(true);
										mSignalometer
												.setVisibility(View.INVISIBLE);
										mOverwriteSignalometer
												.setVisibility(View.INVISIBLE);
										if (isInsert) {
											drawWaveGraph();
										} else {
											drawOverwriteGraph();
										}
										Graphbar.bringToFront();
									}
								});

								// }

								dialog.cancel();
							}
						});
				limitDialog.show();

			}
		});
	}

	@Override
	public void onLowMemoryAlert() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (timer != null || mptimer != null) {
					timer.cancel();
					task.cancel();
					mptimer.cancel();
					mpTimertask.cancel();
				}
				if (RECORDER != null) {

					recorderStopRecording();
				}
				if (isNew) {
					if (passedTotalDuration < 1000) {
						Graphbar.setVisibility(View.INVISIBLE);
						mSignalometer.setVisibility(View.INVISIBLE);
						mOverwriteSignalometer.setVisibility(View.INVISIBLE);
						tvTimeRight.setVisibility(View.INVISIBLE);
						tvTimeLeft.setVisibility(View.INVISIBLE);
						bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
						bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
					} else {
						isNew = false;
					}
				} else {
					if (isInsert) {
						drawWaveGraph();
					} else {
						drawOverwriteGraph();
					}
				}
				AlertDialog limitDialog = new AlertDialog.Builder(
						DictateActivity.this).create();
				limitDialog.setTitle(getResources().getString(
						R.string.Dictate_No_Space));
				limitDialog.setMessage(getResources().getString(
						R.string.Dictate_Low_Memory));
				limitDialog.setCancelable(false);
				limitDialog.setButton(
						getResources().getString(R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (!isReview) {
									recorderSetEnable(true);

								}
								isRecording = false;
								if (isNew) {
									if (passedTotalDuration > 1000) {
										isNew = false;
										dictCard.setStatus(DictationStatus.PENDING
												.getValue());
										mDbHandler.updateDictationStatus(
												dictCard.getDictationId(),
												dictCard.getStatus());
										mediaPlayerSetEnable(true);
										mediaPlayerUpdatebottomBar(true);
										resetDmPlayerStatusView();
									} else {
										resetDmPlayerStatusView();
										mediaPlayerUpdatebottomBar(true);
									}
									tvTimeLeft.setVisibility(View.INVISIBLE);
									bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
									bNew.setEnabled(false);
								} else {
									runOnUiThread(new Runnable() {
										public void run() {
											bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
											updateTimerandgraph();
											mediaPlayerPrepareAudioFile(false);
											bNew.setBackgroundResource(R.drawable.dictate_new_selector);
											mediaPlayerUpdatebottomBar(true);
											mSignalometer
													.setVisibility(View.INVISIBLE);
											mOverwriteSignalometer
													.setVisibility(View.INVISIBLE);
											if (isInsert) {
												drawWaveGraph();
											} else {
												drawOverwriteGraph();
											}
											Graphbar.bringToFront();
										}
									});

									dialog.cancel();
								}
							}
						});
				limitDialog.show();
			}
		});
	}

	@Override
	public void onNotifyError() {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				IsOtherRecorderActive = true;

				if (isRecording) {
					recorderTriggerStop();
				}
				if (isNew
						&& !passedModeName
								.equals(DMApplication.MODE_COPY_RECORDING)) {
					dmApplication.passMode = "";
					bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
				} else {
					if (isInsert) {
						drawWaveGraph();
					} else {
						drawOverwriteGraph();
					}
					if (!isNew) {
						bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
					}
				}
				if ((isForwarding == 1 || isRewinding == 1)) {
					if (isForwarding == 1) {
						isForwarding = 0;
						if (!(Graphbar.getProgress() == Graphbar.getMax())) {
							bForward.setBackgroundResource(R.drawable.dictate_forward);
						} else {
							bForward.setBackgroundResource(R.drawable.dictate_forward_sel);
						}
					} else if (isRewinding == 1) {
						isRewinding = 0;
						if (!(Graphbar.getProgress() == 0)) {
							bRewind.setBackgroundResource(R.drawable.dictate_rewind);
						} else {
							bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
						}
					}
				} else {
					if (mMediaPlayer != null) {
						if (mMediaPlayer.isPlaying()) {
							if (mMediaPlayer != null) {
								playButtonState = MediaMode.PAUSE;
								if (!isReview) {
									recorderSetEnable(true);
								} else {
									bNew.setEnabled(true);
									bNew.setBackgroundResource(R.drawable.dictate_new_selector);
								}
								mMediaPlayer.pause();
								if (Graphbar.getProgress() < Graphbar.getMax()
										&& Graphbar.getProgress() > 0) {
									isOncePaused = true;
								}
								OverwriteLimitExceeded = false;
								savedCurrentPos = getMMcurrentPos(Graphbar
										.getProgress());
								pausePosition = mMediaPlayer
										.getCurrentPosition();
								fileSize = getPlayBackFileSize();
								continue_process = false;
								bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
							}
						}
					}
				}

				if (!passedModeName.equals(DMApplication.MODE_COPY_RECORDING)
						&& !passedModeName
								.equals(DMApplication.MODE_REVIEW_RECORDING)
						&& !passedModeName
								.equals(DMApplication.MODE_EDIT_RECORDING)) {
					mSignalometer.setVisibility(View.INVISIBLE);
					mOverwriteSignalometer.setVisibility(View.INVISIBLE);
				}
				if (isNew) {
					tvTimeRight.setVisibility(View.INVISIBLE);
				}
				AlertDialog limitDialog = new AlertDialog.Builder(
						DictateActivity.this).create();
				limitDialog.setTitle(getResources().getString(R.string.Alert));
				limitDialog.setMessage(getResources().getString(
						R.string.UnableRecording));
				limitDialog.setCancelable(false);
				limitDialog.setButton(
						getResources().getString(R.string.Dictate_Alert_Ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
								if (prefs != null) {
									SharedPreferences.Editor editor = prefs
											.edit();
									editor.putBoolean(FORCEQUIT_PREF_NAME,
											false);
									editor.commit();
								}
								if (!isReview) {
									recorderSetEnable(true);
								}
								isRecording = false;
								if (isNew) {
									resetDmPlayerStatusView();
									tvTimeLeft.setVisibility(View.INVISIBLE);
									bRewind.setBackgroundResource(R.drawable.dictate_rewind_sel);
									bPlay.setBackgroundResource(R.drawable.dictate_play_sel);
									imbFolder
											.setImageResource(R.drawable.dictate_folder_selector);
									imbFiles.setImageResource(R.drawable.dictate_files_selector);
									imbFolder.setEnabled(true);
									imbFiles.setEnabled(true);
									bNew.setBackgroundResource(R.drawable.dictate_new_button_sel);
									bNew.setEnabled(false);
								} else {
									dialog.cancel();
									if (timer != null || mptimer != null) {
										timer.cancel();
										task.cancel();
										mptimer.cancel();
										mpTimertask.cancel();
									}
									runOnUiThread(new Runnable() {
										public void run() {

											bPlay.setBackgroundResource(R.drawable.dictate_play_selector);
											updateTimerandgraph();
											mediaPlayerPrepareAudioFile(false);
											mediaPlayerUpdatebottomBar(true);
											tvTimeRight
													.setVisibility(View.VISIBLE);
											mSignalometer
													.setVisibility(View.INVISIBLE);
											mOverwriteSignalometer
													.setVisibility(View.INVISIBLE);
											if (isInsert) {
												drawWaveGraph();
											} else {
												drawOverwriteGraph();
											}
											Graphbar.bringToFront();

										}
									});

								}

							}
						});
				limitDialog.show();

			}
		});
	}

	/**
	 * Un-bind all the Drawables from a view. This is done mainly for the memory
	 * Management.
	 * 
	 * @param view
	 *            View that contains all the views.
	 */
	private void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

	/**
	 * Decode the sample sized bitmap from the image path. For memory management
	 * sample sized bitmap is used.
	 * 
	 * @param path
	 *            File path of the image.
	 * @param reqWidth
	 *            Required width of the image.
	 * @param reqHeight
	 *            Required height of the image.
	 * @return Bitmap that can be set to the source.
	 */
	public Bitmap decodeSampledBitmapFromResource(String path, int reqWidth,
			int reqHeight) {

		try {
			// First decode with inJustDecodeBounds=true to check dimensions
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);

			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, reqWidth,
					reqHeight);

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
			String orientString = exif
					.getAttribute(ExifInterface.TAG_ORIENTATION);
			int orientation = orientString != null ? Integer
					.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
			int rotationAngle = 0;
			if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
				rotationAngle = 90;
			if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
				rotationAngle = 180;
			if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
				rotationAngle = 270;
			Matrix matrix = new Matrix();
			matrix.setRotate(rotationAngle, (float) bmp.getWidth() / 2,
					(float) bmp.getHeight() / 2);
			bitmap = Bitmap.createBitmap(bmp, 0, 0, options.outWidth,
					options.outHeight, matrix, true);
			// bmp.recycle();
			bmp = null;
			matrix = null;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bitmap;
	}

	/**
	 * Calculates the sample size for the bitmap that should be generated and
	 * returns the sample size value.
	 * 
	 * @param options
	 *            Bitmap option property.
	 * @param reqWidth
	 *            Required width of the bitmap that should be returned.
	 * @param reqHeight
	 *            Required height of the bitmap that should be returned.
	 * @return Int value of sample size.
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

	public void muteNotificationSounds(boolean state) {
		audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, state);
		audioManager.setStreamMute(AudioManager.STREAM_ALARM, state);
	}

	/**
	 * Returned the user selected language.
	 * 
	 * @return String of user selected language.
	 */
	public String getlanguage() {
		String currentLanguage = null;
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		currentLanguage = (pref.getString(
				getResources().getString(R.string.language_key), ""));
		if (Integer.parseInt(currentLanguage) == 1) {
			language = "en";
		} else if (Integer.parseInt(currentLanguage) == 1) {
			language = "de";
		} else if (Integer.parseInt(currentLanguage) == 2) {
			language = "fr";
		} else if (Integer.parseInt(currentLanguage) == 3) {
			language = "es";
		} else if (Integer.parseInt(currentLanguage) == 4) {
			language = "sv";
		} else if (Integer.parseInt(currentLanguage) == 5) {
			language = "cs";
		}
		return language;
	}


	/**
	 * This method validates whether there is available free space or whether
	 * the size limit (2GB) has been exceeded before the recorder instance is
	 * initiated.
	 * 
	 * @return returns true if the validation succeeded , else false.
	 */
	public boolean validateSpaceAndSizeLimit() {
		FREE_SPACE_REQUIRED = 0;
		long pause_position_in_bytes = 0;
		if (RECORDER != null) {
			if (RECORDER.getState() == DMAudioRecorder.State.REC) {
				// recording already in process
				return false;
			}
			RECORDER.release();
		}
		if (isOncePaused) {
			pause_position_in_bytes = getBytePosition(pausePosition, getMilliseconsFromAFile(getFilename()),
					getPlayBackFileSize());
			if (isInsert) {
				if (pause_position_in_bytes > (getPlayBackFileSize() * THRESHOLD)) 
					FREE_SPACE_REQUIRED = (getPlayBackFileSize() - pause_position_in_bytes);
				else 
					FREE_SPACE_REQUIRED = (getPlayBackFileSize());
			}
		}
		if (dmApplication.getAvailableDiskSpace() > (dmApplication.MINIMUM_SIZE_REQUIRED + FREE_SPACE_REQUIRED)) {
			if (!isNew) {
				if (isOncePaused && !isInsert) {
					return true;
				} else {
					if (((getPlayBackFileSize() + 1024) / MAXIMUM_LIMIT_SIZE) < 2) {
						return true;
					} else {
						// 2 GB limit exceeded
						this.onWriteLimitExceeded();
						return false;
					}
				}

			} else {
				// new dictation
				return true;
			}

		} else {
			// no disc space left to save audio
			this.onLowMemoryAlert();
			return false;

		}
	}

	/**
	 * Enable and disable Corresponding views.
	 * 
	 * @param isEnable
	 *            Decides views should be enabled or disabled
	 */
	private void enableOrDisableViews(boolean isEnable) {
		tvTitle.setEnabled(isEnable);
		tvWorktype.setEnabled(isEnable);
		imFav.setEnabled(isEnable);
		bRecord.setEnabled(isEnable);
		bRecord.setBackgroundResource(R.drawable.dictate_record_sel);
		tbOverwrite.setEnabled(isEnable);
		tbOverwrite.setBackgroundResource(R.drawable.toggle_disble);
		bForward.setEnabled(isEnable);
		bRewind.setEnabled(isEnable);
		bPlay.setEnabled(isEnable);
		imbDelete.setEnabled(isEnable);
		imbFiles.setEnabled(false);
		imbSend.setEnabled(isEnable);
		bNew.setEnabled(true);
		isNew = false;
		bNew.setBackgroundResource(R.drawable.dictate_new_selector);
	}

	/*@Override
	public void enableRecordUI(final boolean state) {
		// TODO Auto-generated method stub
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				bRecord.setEnabled(state);
				System.out.println(" ui state - "+state);
			}
		});
		
	}*/
	
}