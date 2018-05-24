package com.olympus.dmmobile;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DatabaseHandler class is used for all the DataBase related operations.
 * DataBase creation, table creation, insertion, deletion and updation are handled in this class
 * 
 * @version 1.0.1
 */
public class DatabaseHandler extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 2;
	private static final String DATABSE_NAME = "dmmob_db";
	private static final String TABLE_DICTATIONS = "dictations";
	private static final String TABLE_FILES = "files";
	private static final String TABLE_RECIPIENTS = "table_recipients";
	
	
	private final String DICTATION_ID = "_id";
	private final String AUTHOR = "author";
	public final String COMMENT = "comment";
	private final String CONVERTED = "converted";
	private final String CREATED_AT = "created_at";
	private final String DELIVERY_METHOD = "delivery_method";
	public final String DICTATION_NAME = "dictation_name";
	private final String DICTATION_FILE_NAME = "dictation_file_name";
	private final String DSS_VERSION = "dss_version";
	public final String DURATION = "duration";
	public final String ISACTIVE = "is_active";
	public final String ISFLASHAIR = "is_flashair";
	private final String JOB_NUMBER = "job_number";
	public final String PRIORITY = "priority";
	public final String REC_END_DATE = "rec_end_date";
	private final String REC_START_DATE = "rec_start_date";
	public final String SENT_DATE = "sent_date";
	public final String SEQ_NUMBER = "sequence_number";
	public final String STATUS = "status";
	private final String THUMBNAIL_AVAILABLE = "thumbnail_available";
	private final String THUMBNAIL_TYPE = "thumbnail_type";
	public final String WORKTYPE = "worktype";
	private final String DSS_COMPRESSION_MODE = "dss_compression_mode";
	private final String IS_ENCRYPT = "is_encrypt";
	private final String DSS_ENCRYPTION_PASSWORD = "dss_encryption_password";
	private final String IS_SPLITTABLE = "is_splittable";
	private final String GROUP_ID = "group_id";
	private final String SPLIT_INTERNAL_STATUS = "split_internal_status";
	private final String ENCRYPTION_VERSION = "encryption_version";
	private final String MAIN_STATUS = "main_status";
	private final String QUERY_PRIORITY = "query_priority";
	public final String IS_RESEND = "IS_RESEND";
	
	private final String FILE_ID = "_id";
	private final String FILE_NAME = "file_name";
	private final String FILE_SIZE = "file_size";
	private final String INDEX = "file_index";
	private final String TRANSFER_ID = "transfer_id";
	private final String TRANSFER_STATUS = "transfer_status";
	private final String RETRY_COUNT = "retry_count";
	
	public final String ID = "_id";
	public final String EMAIL = "email";
	public final String NAME = "name";
	
	private DictationCard dCard=null;
	private DictationCard mDictCard=null;
	
	private final String CREATE_DICTATION_TABLE = "CREATE TABLE " + TABLE_DICTATIONS + "(" + DICTATION_ID + " INTEGER PRIMARY KEY," 
			+ DICTATION_NAME + " TEXT," + DICTATION_FILE_NAME + " TEXT," + AUTHOR + " TEXT," + PRIORITY + " INTEGER,"  + STATUS + " INTEGER," 
			+ WORKTYPE + " TEXT," + DURATION + " INTEGER,"+ CREATED_AT + " NUMERIC," + COMMENT + " TEXT," + THUMBNAIL_AVAILABLE + " INTEGER," 
			+ THUMBNAIL_TYPE + " TEXT,"+ REC_START_DATE + " NUMERIC," + REC_END_DATE + " NUMERIC," + JOB_NUMBER + " INTEGER," + SEQ_NUMBER + " INTEGER," 
			+ CONVERTED + " INTEGER," + DELIVERY_METHOD + " INTEGER," + DSS_COMPRESSION_MODE + " INTEGER," + IS_ENCRYPT + " INTEGER,"
			+ DSS_ENCRYPTION_PASSWORD + " TEXT," + DSS_VERSION + " INTEGER," + ISACTIVE + " INTEGER," + ISFLASHAIR + " INTEGER,"
			+ SENT_DATE + " NUMERIC," + IS_SPLITTABLE + " INTEGER," + GROUP_ID + " INTEGER,"+ SPLIT_INTERNAL_STATUS + " INTEGER," 
			+ ENCRYPTION_VERSION + " INTEGER," + MAIN_STATUS + " INTEGER," + QUERY_PRIORITY + " INTEGER," + IS_RESEND + " INTEGER)";
	

	private final String CREATE_FILES_TABLE = "CREATE TABLE " + TABLE_FILES + "(" + FILE_ID + " INTEGER NOT NULL," + FILE_NAME + " TEXT," 
			+ RETRY_COUNT + " INTEGER," + FILE_SIZE + " INTEGER," + INDEX + " INTEGER," + JOB_NUMBER + " INTEGER," + TRANSFER_ID + " TEXT,"
			+ TRANSFER_STATUS + " INTEGER," + "FOREIGN KEY(" + FILE_ID + ") REFERENCES " + TABLE_DICTATIONS + "(" + DICTATION_ID + ")" + ")";
	
	private final String CREATE_TABLE_RECIPIENTS = "CREATE TABLE " + TABLE_RECIPIENTS + "(" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
			+ EMAIL + " TEXT," + NAME + " TEXT)";


	/**
	 * Constructor of DatabaseHandler with one parameter.
	 * @param context Context of current activity.
	 */
	public DatabaseHandler(Context mContext) {
		super(mContext, DATABSE_NAME, null, DATABASE_VERSION);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_DICTATION_TABLE);
		db.execSQL(CREATE_FILES_TABLE);
		db.execSQL(CREATE_TABLE_RECIPIENTS);
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (newVersion) {
        	case 2:
        		db.execSQL(CREATE_TABLE_RECIPIENTS);
        		db.execSQL("ALTER TABLE " + TABLE_DICTATIONS + " ADD COLUMN " + IS_RESEND);
        	break;
        	case 3:
        		if(oldVersion==1) {

				}
				else if(oldVersion==2) {

				}
            break;
		}
	}

	/**
	 * Opens Database to write or read data.
	 * @return SQLiteDatabase will be returned.
	 */
	private SQLiteDatabase openDB(){
		
		return this.getWritableDatabase();
	}
	
	public void closeDB(){
		//openedConnections--;
	   // if (openedConnections == 0) {
//	    	mDatabase.close();
	   // }
		
	}
	
	/**
	 * Fetches file list of particular Dictation
	 * @param id Id of particular Dictation as int
	 * @return Returns list of files as ArrayList which contains FilesCard
	 */
	public ArrayList<FilesCard> getFileList(int id){
		ArrayList<FilesCard> fileList = new ArrayList<FilesCard>();
		FilesCard fCard = new FilesCard();
		SQLiteDatabase db = openDB();
		try
		{
			Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+id+"'", null);
			if(cursor.moveToFirst()){
				do{
					fCard = new FilesCard();
					fCard.setFileId(cursor.getInt(cursor.getColumnIndex(FILE_ID)));
					fCard.setFileName(cursor.getString(cursor.getColumnIndex(FILE_NAME)));
					fCard.setFileSize(cursor.getLong(cursor.getColumnIndex(FILE_SIZE)));
					fCard.setFileIndex(cursor.getInt(cursor.getColumnIndex(INDEX)));
					fCard.setRetryCount(cursor.getInt(cursor.getColumnIndex(RETRY_COUNT)));
					fCard.setJobNumber(cursor.getString(cursor.getColumnIndex(JOB_NUMBER)));
					fCard.setTransferId(cursor.getString(cursor.getColumnIndex(TRANSFER_ID)));
					fCard.setTransferStatus(cursor.getInt(cursor.getColumnIndex(TRANSFER_STATUS)));
					fileList.add(fCard);
					fCard=null;
				}while(cursor.moveToNext());
			}
			cursor.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return fileList;
	}

	/**
	 * Gets particular Dictation details by Id. 
	 * @param id Id of particular Dictation
	 * @return Returns dictation details as DictationCard
	 */
	public DictationCard getDictationCardWithId(int id){
		SQLiteDatabase db = openDB();
		DictationCard dCard = null;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_ID+"='"+id+"'", null);
			if(c.moveToFirst()){
				do{
					dCard = new DictationCard();
					
					dCard.setDictationId(c.getInt(c.getColumnIndex(DICTATION_ID)));
					dCard.setDictationName(c.getString(c.getColumnIndex(DICTATION_NAME)));
					dCard.setDictFileName(c.getString(c.getColumnIndex(DICTATION_FILE_NAME)));
					dCard.setAuthor(c.getString(c.getColumnIndex(AUTHOR)));
					dCard.setPriority(c.getInt(c.getColumnIndex(PRIORITY)));
					dCard.setStatus(c.getInt(c.getColumnIndex(STATUS)));
					dCard.setWorktype(c.getString(c.getColumnIndex(WORKTYPE)));
					dCard.setDuration(c.getLong(c.getColumnIndex(DURATION)));
					dCard.setCreatedAt(c.getString(c.getColumnIndex(CREATED_AT)));
					dCard.setComments(c.getString(c.getColumnIndex(COMMENT)));
					dCard.setIsThumbnailAvailable(c.getInt(c.getColumnIndex(THUMBNAIL_AVAILABLE)));
					dCard.setThumbnailType(c.getString(c.getColumnIndex(THUMBNAIL_TYPE)));
					dCard.setRecStartDate(c.getString(c.getColumnIndex(REC_START_DATE)));
					dCard.setRecEndDate(c.getString(c.getColumnIndex(REC_END_DATE)));
					dCard.setJobNumber(c.getInt(c.getColumnIndex(JOB_NUMBER)));
					dCard.setSequenceNumber(c.getInt(c.getColumnIndex(SEQ_NUMBER)));
					dCard.setIsConverted(c.getInt(c.getColumnIndex(CONVERTED)));
					dCard.setDeliveryMethod(c.getInt(c.getColumnIndex(DELIVERY_METHOD)));
					dCard.setDssCompressionMode(c.getInt(c.getColumnIndex(DSS_COMPRESSION_MODE)));
					dCard.setDssEncryptionPassword(c.getString(c.getColumnIndex(DSS_ENCRYPTION_PASSWORD)));
					dCard.setDssVersion(c.getInt(c.getColumnIndex(DSS_VERSION)));
					dCard.setIsActive(c.getInt(c.getColumnIndex(ISACTIVE)));
					dCard.setIsFlashAir(c.getInt(c.getColumnIndex(ISFLASHAIR)));
					dCard.setSentDate(c.getString(c.getColumnIndex(SENT_DATE)));
					dCard.setFileSplittable(c.getInt(c.getColumnIndex(IS_SPLITTABLE)));
					dCard.setGroupId(c.getInt(c.getColumnIndex(GROUP_ID)));
					dCard.setSplitInternalStatus(c.getInt(c.getColumnIndex(SPLIT_INTERNAL_STATUS)));
					dCard.setEncryptionVersion(c.getInt(c.getColumnIndex(ENCRYPTION_VERSION)));
					dCard.setEncryption(c.getInt(c.getColumnIndex(IS_ENCRYPT)));
					dCard.setMainStatus(c.getInt(c.getColumnIndex(MAIN_STATUS)));
					dCard.setQueryPriority(c.getInt(c.getColumnIndex(QUERY_PRIORITY)));
					dCard.setIsResend(c.getInt(c.getColumnIndex(IS_RESEND)));
				}while(c.moveToNext());
			}
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return dCard;
	}
	
	/**
	 * Gets selected dictation by Cursor.
	 * @param c Cursor contains details of selected Dictation
	 * @return Dictation details as DictatonCard
	 */
	public DictationCard getSelectedDicts(Cursor c){
		mDictCard = new DictationCard();
		
		mDictCard.setDictationId(c.getInt(c.getColumnIndex(DICTATION_ID)));
		mDictCard.setDictationName(c.getString(c.getColumnIndex(DICTATION_NAME)));
		mDictCard.setDictFileName(c.getString(c.getColumnIndex(DICTATION_FILE_NAME)));
		mDictCard.setAuthor(c.getString(c.getColumnIndex(AUTHOR)));
		mDictCard.setPriority(c.getInt(c.getColumnIndex(PRIORITY)));
		mDictCard.setStatus(c.getInt(c.getColumnIndex(STATUS)));
		mDictCard.setWorktype(c.getString(c.getColumnIndex(WORKTYPE)));
		mDictCard.setDuration(c.getLong(c.getColumnIndex(DURATION)));
		mDictCard.setCreatedAt(c.getString(c.getColumnIndex(CREATED_AT)));
		mDictCard.setComments(c.getString(c.getColumnIndex(COMMENT)));
		mDictCard.setIsThumbnailAvailable(c.getInt(c.getColumnIndex(THUMBNAIL_AVAILABLE)));
		mDictCard.setThumbnailType(c.getString(c.getColumnIndex(THUMBNAIL_TYPE)));
		mDictCard.setRecStartDate(c.getString(c.getColumnIndex(REC_START_DATE)));
		mDictCard.setRecEndDate(c.getString(c.getColumnIndex(REC_END_DATE)));
		mDictCard.setJobNumber(c.getInt(c.getColumnIndex(JOB_NUMBER)));
		mDictCard.setSequenceNumber(c.getInt(c.getColumnIndex(SEQ_NUMBER)));
		mDictCard.setIsConverted(c.getInt(c.getColumnIndex(CONVERTED)));
		mDictCard.setDeliveryMethod(c.getInt(c.getColumnIndex(DELIVERY_METHOD)));
		mDictCard.setDssCompressionMode(c.getInt(c.getColumnIndex(DSS_COMPRESSION_MODE)));
		mDictCard.setDssEncryptionPassword(c.getString(c.getColumnIndex(DSS_ENCRYPTION_PASSWORD)));
		mDictCard.setDssVersion(c.getInt(c.getColumnIndex(DSS_VERSION)));
		mDictCard.setIsActive(c.getInt(c.getColumnIndex(ISACTIVE)));
		mDictCard.setIsFlashAir(c.getInt(c.getColumnIndex(ISFLASHAIR)));
		mDictCard.setSentDate(c.getString(c.getColumnIndex(SENT_DATE)));
		mDictCard.setFileSplittable(c.getInt(c.getColumnIndex(IS_SPLITTABLE)));
		mDictCard.setGroupId(c.getInt(c.getColumnIndex(GROUP_ID)));
		mDictCard.setSplitInternalStatus(c.getInt(c.getColumnIndex(SPLIT_INTERNAL_STATUS)));
		mDictCard.setEncryptionVersion(c.getInt(c.getColumnIndex(ENCRYPTION_VERSION)));
		mDictCard.setEncryption(c.getInt(c.getColumnIndex(IS_ENCRYPT)));
		mDictCard.setMainStatus(c.getInt(c.getColumnIndex(MAIN_STATUS)));
		mDictCard.setQueryPriority(c.getInt(c.getColumnIndex(QUERY_PRIORITY)));
		mDictCard.setIsResend(c.getInt(c.getColumnIndex(IS_RESEND)));
		
		return mDictCard;
	}
	
	/**
	 * Gets selected dictation by Cursor.
	 * @param c Cursor contains details of selected Dictation
	 * @return Dictation details as DictatonCard
	 */
	public DictationCard getSelectedDictation(Cursor c){
		dCard = new DictationCard();
		
		dCard.setDictationId(c.getInt(c.getColumnIndex(DICTATION_ID)));
		dCard.setDictationName(c.getString(c.getColumnIndex(DICTATION_NAME)));
		dCard.setDictFileName(c.getString(c.getColumnIndex(DICTATION_FILE_NAME)));
		dCard.setAuthor(c.getString(c.getColumnIndex(AUTHOR)));
		dCard.setPriority(c.getInt(c.getColumnIndex(PRIORITY)));
		dCard.setStatus(c.getInt(c.getColumnIndex(STATUS)));
		dCard.setWorktype(c.getString(c.getColumnIndex(WORKTYPE)));
		dCard.setDuration(c.getLong(c.getColumnIndex(DURATION)));
		dCard.setCreatedAt(c.getString(c.getColumnIndex(CREATED_AT)));
		dCard.setComments(c.getString(c.getColumnIndex(COMMENT)));
		dCard.setIsThumbnailAvailable(c.getInt(c.getColumnIndex(THUMBNAIL_AVAILABLE)));
		dCard.setThumbnailType(c.getString(c.getColumnIndex(THUMBNAIL_TYPE)));
		dCard.setRecStartDate(c.getString(c.getColumnIndex(REC_START_DATE)));
		dCard.setRecEndDate(c.getString(c.getColumnIndex(REC_END_DATE)));
		dCard.setJobNumber(c.getInt(c.getColumnIndex(JOB_NUMBER)));
		dCard.setSequenceNumber(c.getInt(c.getColumnIndex(SEQ_NUMBER)));
		dCard.setIsConverted(c.getInt(c.getColumnIndex(CONVERTED)));
		dCard.setDeliveryMethod(c.getInt(c.getColumnIndex(DELIVERY_METHOD)));
		dCard.setDssCompressionMode(c.getInt(c.getColumnIndex(DSS_COMPRESSION_MODE)));
		dCard.setDssEncryptionPassword(c.getString(c.getColumnIndex(DSS_ENCRYPTION_PASSWORD)));
		dCard.setDssVersion(c.getInt(c.getColumnIndex(DSS_VERSION)));
		dCard.setIsActive(c.getInt(c.getColumnIndex(ISACTIVE)));
		dCard.setIsFlashAir(c.getInt(c.getColumnIndex(ISFLASHAIR)));
		dCard.setSentDate(c.getString(c.getColumnIndex(SENT_DATE)));
		dCard.setFileSplittable(c.getInt(c.getColumnIndex(IS_SPLITTABLE)));
		dCard.setGroupId(c.getInt(c.getColumnIndex(GROUP_ID)));
		dCard.setSplitInternalStatus(c.getInt(c.getColumnIndex(SPLIT_INTERNAL_STATUS)));
		dCard.setEncryptionVersion(c.getInt(c.getColumnIndex(ENCRYPTION_VERSION)));
		dCard.setEncryption(c.getInt(c.getColumnIndex(IS_ENCRYPT)));
		dCard.setMainStatus(c.getInt(c.getColumnIndex(MAIN_STATUS)));
		dCard.setQueryPriority(c.getInt(c.getColumnIndex(QUERY_PRIORITY)));
		dCard.setIsResend(c.getInt(c.getColumnIndex(IS_RESEND)));
		
		return dCard;
	}
	
	/**
	 * Get Dictations by status which is ordered by Dictation modified date in descending order.
	 * @param status Dictation status.
	 * @return Dictations as Cursor.
	 */
	public Cursor getDicts(int status){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+status+"' ORDER BY "+REC_END_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Gets particular Dictation details by Id. 
	 * @param dictationId Dictation Id.
	 * @return Dictation details as Cursor
	 */
	public Cursor getDictationWithId(int dictationId){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_ID+"='"+dictationId+"'", null);
		return cursor;
	}
	
	/**
	 * Get Dictations which is in Pending status and not active.
	 * @return Dictations as Cursor.
	 */
	public Cursor getDictationsInPendingAll(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+ISACTIVE+"='0' AND "+STATUS+"='"+DictationStatus.PENDING.getValue()+"'", null);
		return cursor;
	}
	
	/**
	 * Get Dictations which is in Outbox Tab. 
	 * @return Dictations as Cursor.
	 */
	public Cursor getOutboxDictations(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DICTATIONS + " WHERE " + STATUS + "='" + DictationStatus.OUTBOX.getValue() 
				+ "' OR " + STATUS+"='" + DictationStatus.SENDING.getValue() + "' OR " + STATUS+"='" + DictationStatus.TIMEOUT.getValue() 
				+ "' " + "OR " + STATUS + "='" + DictationStatus.RETRYING1.getValue() + "' OR " + STATUS + "='"+DictationStatus.RETRYING2.getValue() 
				+ "' OR "+STATUS+"='"+ DictationStatus.RETRYING3.getValue() + "' OR " + STATUS + "='" + DictationStatus.SENDING_FAILED.getValue() 
				+ "' OR " + STATUS + "='" + DictationStatus.CONVERTION_FAILED.getValue() + "' OR " + STATUS + "='" + DictationStatus.WAITING_TO_SEND1.getValue()
				+"'" + " OR " + STATUS + "='" + DictationStatus.WAITING_TO_SEND2.getValue() + "' ORDER BY " + STATUS + " ASC," + REC_END_DATE + " DESC", null);
		return cursor;
	}
	
	/**
	 * Get Dictations which is in Outbox Tab and available to send.
	 * @return Dictations as Cursor.
	 */
	public Cursor getOutboxDictationsToEnableSendAll(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DICTATIONS + " WHERE ("
		+ STATUS + "='" + DictationStatus.TIMEOUT.getValue() + "' OR " + STATUS + "='" + DictationStatus.SENDING_FAILED.getValue() 
		+ "' OR " + STATUS + "='" + DictationStatus.CONVERTION_FAILED.getValue() + "') OR (" + CONVERTED + "=1 AND ("+STATUS + "='" 
		+ DictationStatus.SENDING.getValue() + "' OR " + STATUS + "='" + DictationStatus.RETRYING1.getValue() 
		+ "' OR " + STATUS + "='" + DictationStatus.RETRYING2.getValue() + "' OR " + STATUS + "='" 
		+ DictationStatus.RETRYING3.getValue() + "'))", null);
		return cursor;
	}
	
	/**
	 * Count No; of Dictations as re-send operation present in Outbox
	 * @return Dictations as Cursor.
	 */
	public int getOutboxSendAllCount(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DICTATIONS + " WHERE (" + STATUS + "='" 
		+ DictationStatus.SENDING.getValue() + "' OR " + STATUS + "='" + DictationStatus.RETRYING1.getValue() 
		+ "' OR " + STATUS + "='" + DictationStatus.RETRYING2.getValue() + "' OR " + STATUS + "='" 
		+ DictationStatus.RETRYING3.getValue() + "') AND " + CONVERTED + "=1", null);
		if(cursor!=null)
			return cursor.getCount();
		else
			return 0;
	}
	
	
	/**
	 * Get Dictations which is in Outbox tab and available to delete
	 * @return cursor
	 */
	public Cursor getOutboxDictationsToEnableDeleteAll(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.OUTBOX.getValue()+"' OR "
				+STATUS+"='"+DictationStatus.TIMEOUT.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENDING_FAILED.getValue()+
				"' OR "+STATUS+"='"+DictationStatus.CONVERTION_FAILED.getValue()+"'", null);
		return cursor;
	}
	
	/**
	 * Get Dictations based on the delivery method
	 * @param delivery Delivery method value as int
	 * @return Dictations as Cursor.
	 */
	public Cursor getOutboxDictationsBasedOnDelivery(int delivery){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.OUTBOX.getValue()+"' OR "+STATUS+"='"+DictationStatus.TIMEOUT.getValue()+"' " +
				 "OR "+STATUS+"='"+DictationStatus.SENDING_FAILED.getValue()+"' OR "+STATUS+"='"+DictationStatus.CONVERTION_FAILED.getValue()+"') AND "+IS_SPLITTABLE+"='"+delivery+"'", null);
		return cursor;
	}
	
	/**
	 * Get Dictations which is already sent.
	 * @return Dictations as Cursor.
	 */
	public Cursor getSentDictations(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.SENT.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENT_VIA_EMAIL.getValue()+"' ORDER BY "+SENT_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Inserts Dictation details. Dictation details will be passed as DictationCard.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void insertDictation(DictationCard dCard){
		
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DICTATION_ID, dCard.getDictationId());
		values.put(DICTATION_NAME, dCard.getDictationName());
		values.put(DICTATION_FILE_NAME, dCard.getDictFileName());
		values.put(AUTHOR, dCard.getAuthor());
		values.put(PRIORITY, dCard.getPriority());
		values.put(STATUS, dCard.getStatus());
		values.put(WORKTYPE, dCard.getWorktype());
		values.put(DURATION, dCard.getDuration());
		values.put(CREATED_AT, dCard.getCreatedAt());
		values.put(COMMENT, dCard.getComments());
		values.put(THUMBNAIL_AVAILABLE, dCard.getIsThumbnailAvailable());
		values.put(THUMBNAIL_TYPE, dCard.getThumbnailType());
		values.put(REC_START_DATE, dCard.getRecStartDate());
		values.put(REC_END_DATE, dCard.getRecEndDate());
		values.put(JOB_NUMBER, dCard.getJobNumber());
		values.put(SEQ_NUMBER, dCard.getSequenceNumber());
		values.put(CONVERTED, dCard.getIsConverted());
		values.put(DELIVERY_METHOD, dCard.getDeliveryMethod());
		values.put(DSS_COMPRESSION_MODE, dCard.getDssCompressionMode());
		values.put(DSS_ENCRYPTION_PASSWORD, dCard.getDssEncryptionPassword());
		values.put(DSS_VERSION, dCard.getDssVersion());
		values.put(ISACTIVE, dCard.getIsActive());
		values.put(ISFLASHAIR, dCard.getIsFlashAir());
		values.put(SENT_DATE, dCard.getSentDate());
		values.put(IS_SPLITTABLE, dCard.isFileSplittable());
		values.put(GROUP_ID, dCard.getGroupId());
		values.put(SPLIT_INTERNAL_STATUS, dCard.getSplitInternalStatus());
		values.put(ENCRYPTION_VERSION, dCard.getEncryptionVersion());
		values.put(IS_ENCRYPT, dCard.isEncryption());
		values.put(MAIN_STATUS, dCard.getMainStatus());
		values.put(QUERY_PRIORITY, dCard.getQueryPriority());
		values.put(IS_RESEND, dCard.isResend());
		db.insert(TABLE_DICTATIONS, null, values);
		closeDB();
	}
	
	/**
	 * Updates Dictation which is already present in Database. Dictation details passed as DictationCard.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateDictation(DictationCard dCard){
		
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DICTATION_ID, dCard.getDictationId());
		values.put(DICTATION_NAME, dCard.getDictationName());
		values.put(DICTATION_FILE_NAME, dCard.getDictFileName());
		values.put(AUTHOR, dCard.getAuthor());
		values.put(PRIORITY, dCard.getPriority());
		values.put(STATUS, dCard.getStatus());
		values.put(WORKTYPE, dCard.getWorktype());
		values.put(DURATION, dCard.getDuration());
		values.put(CREATED_AT, dCard.getCreatedAt());
		values.put(COMMENT, dCard.getComments());
		values.put(THUMBNAIL_AVAILABLE, dCard.getIsThumbnailAvailable());
		values.put(THUMBNAIL_TYPE, dCard.getThumbnailType());
		values.put(REC_START_DATE, dCard.getRecStartDate());
		values.put(REC_END_DATE, dCard.getRecEndDate());
		values.put(JOB_NUMBER, dCard.getJobNumber());
		values.put(SEQ_NUMBER, dCard.getSequenceNumber());
		values.put(CONVERTED, dCard.getIsConverted());
		values.put(DELIVERY_METHOD, dCard.getDeliveryMethod());
		values.put(DSS_COMPRESSION_MODE, dCard.getDssCompressionMode());
		values.put(DSS_ENCRYPTION_PASSWORD, dCard.getDssEncryptionPassword());
		values.put(DSS_VERSION, dCard.getDssVersion());
		values.put(ISACTIVE, dCard.getIsActive());
		values.put(ISFLASHAIR, dCard.getIsFlashAir());
		values.put(SENT_DATE, dCard.getSentDate());
		values.put(IS_SPLITTABLE, dCard.isFileSplittable());
		values.put(GROUP_ID, dCard.getGroupId());
		values.put(SPLIT_INTERNAL_STATUS, dCard.getSplitInternalStatus());
		values.put(ENCRYPTION_VERSION, dCard.getEncryptionVersion());
		values.put(IS_ENCRYPT, dCard.isEncryption());
		values.put(MAIN_STATUS, dCard.getMainStatus());
		values.put(QUERY_PRIORITY, dCard.getQueryPriority());
		values.put(IS_RESEND, dCard.isResend());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Get Dictations ordered by priority and dictation modified date. 
	 * @param status Dictation status.
	 * @return Dictations as Cursor.
	 */
	public Cursor getPriorities(int status){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+status+"' ORDER BY "+
				PRIORITY+" DESC, "+REC_END_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Get Dictations which is in Outbox tab ordered by priority and dictation modified date. 
	 * @return Dictations as Cursor.
	 */
	public Cursor getPrioritiesOutbox(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.OUTBOX.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENDING.getValue()+"' OR " +
				""+STATUS+"='"+DictationStatus.TIMEOUT.getValue()+"' OR "+STATUS+"='"+DictationStatus.RETRYING1.getValue()+"' OR "+STATUS+"='"+
				DictationStatus.RETRYING2.getValue()+"' OR "+STATUS+"='"+DictationStatus.RETRYING3.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENDING_FAILED.getValue()+"' OR "
				+STATUS+"='"+DictationStatus.CONVERTION_FAILED.getValue()+"' OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND1.getValue()+"'" +
				" OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND2.getValue()+"') ORDER BY "+PRIORITY+" DESC, "+STATUS+" ASC, "+REC_END_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Get Dictations which is already sent ordered by priority and sent date.
	 * @return Dictations as Cursor.
	 */
	public Cursor getPrioritiesSent(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.SENT.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENT_VIA_EMAIL.getValue()+"') ORDER BY "+PRIORITY+" DESC, "+SENT_DATE+" DESC", null);
		return cursor;
	}
	
	
	/**
	 * Deletes particular Dictation. 
	 * @param dCard Dictation details as DictationCard.
	 */
	public void deleteDictation(DictationCard dCard){
		SQLiteDatabase db = openDB();
		db.delete(TABLE_DICTATIONS, DICTATION_ID+"="+"'"+dCard.getDictationId()+"'", null);
		db.delete(TABLE_FILES, FILE_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Discards currently active Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void discardDictation(DictationCard dCard){
		SQLiteDatabase db = openDB();
		db.delete(TABLE_DICTATIONS, DICTATION_NAME+"="+"'"+dCard.getDictationName()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates status of particular Dictation based on status.
	 * @param dictationId Id of particular Dictation. 
	 * @param status Status of particular Dictation.
	 */
	public void updateDictationStatus(int dictationId,int status){
		try
		{
			switch (status) {
				case 0:
					updateCommonStatus(dictationId,status);
					break;
				case 18:
					updateCommonStatus2(dictationId,status);
					break;
				case 2:
					updateCommonStatus(dictationId,status);
					break;
				case 3:
					updateCommonStatus(dictationId,status);
					break;
				case 20:
					updateCommonStatus(dictationId,status);
					break;
				case 10:
					updateCommonStatus2(dictationId,status);
					break;
				case 22:
					updateCommonStatus(dictationId,status);
					break;
				case 25:
					updateCommonStatus(dictationId,status);
					break;
				default:
					SQLiteDatabase db = openDB();
					ContentValues values = new ContentValues();
					values.put(STATUS,status);
					db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dictationId+"'", null);
					values=null;
					closeDB();
					db=null;
				break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates status of particular Dictation based on status.
	 * @param dictationId Id of particular Dictation. 
	 * @param status Status of particular Dictation.
	 */
	private void updateCommonStatus(int dictationId,int status)
	{
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,status);
		values.put(SPLIT_INTERNAL_STATUS,0);
		values.put(GROUP_ID,0);
		values.put(MAIN_STATUS,0);
		values.put(QUERY_PRIORITY,0);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dictationId+"'", null);
		if(status==DictationStatus.PENDING.getValue())
			db.delete(TABLE_FILES, FILE_ID+"="+"'"+dictationId+"'", null);
		else
		{
			values = new ContentValues();
			values.put(RETRY_COUNT,0);
			db.update(TABLE_FILES, values, FILE_ID+"="+"'"+dictationId+"'", null);
		}
		values=null;
		closeDB();
		db=null;
	}
	
	private void updateCommonStatus2(int dictationId,int status)
	{
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,status);
		values.put(ISACTIVE,0);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dictationId+"'", null);
		values=null;
		closeDB();
		db=null;
	}
	
	/**
	 * Updates status of particular Dictation based on status.
	 * @param dictationId Id of particular Dictation. 
	 * @param status Status of particular Dictation.
	 */
	public void updateDictationStatusConvert(int dictationId,int status){
		try
		{
			switch (status) {
				case 0:
					updateCommonStatusConvert(dictationId,status);
					break;
				case 18:
					updateCommonStatusConvert2(dictationId,status);
					break;
				case 2:
					updateCommonStatusConvert(dictationId,status);
					break;
				case 3:
					updateCommonStatusConvert(dictationId,status);
					break;
				case 20:
					updateCommonStatusConvert(dictationId,status);
					break;
				case 10:
					updateCommonStatusConvert2(dictationId,status);
					break;
				case 22:
					updateCommonStatusConvert(dictationId,status);
					break;
				case 25:
					updateCommonStatusConvert(dictationId,status);
					break;
				default:
					SQLiteDatabase db = openDB();
					ContentValues values = new ContentValues();
					values.put(STATUS,status);
					db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dictationId+"'", null);
					values=null;
					closeDB();
					db=null;
				break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates status of particular Dictation based on status.
	 * @param dictationId Id of particular Dictation. 
	 * @param status Status of particular Dictation.
	 */
	private void updateCommonStatusConvert(int dictationId,int status)
	{
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,status);
		values.put(SPLIT_INTERNAL_STATUS,0);
		values.put(GROUP_ID,0);
		values.put(MAIN_STATUS,0);
		values.put(QUERY_PRIORITY,0);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dictationId+"'", null);
		if(status==DictationStatus.PENDING.getValue())
			db.delete(TABLE_FILES, FILE_ID+"="+"'"+dictationId+"'", null);
		else
		{
			values = new ContentValues();
			values.put(RETRY_COUNT,0);
			db.update(TABLE_FILES, values, FILE_ID+"="+"'"+dictationId+"'", null);
		}
		values=null;
		closeDB();
		db=null;
	}
	
	private void updateCommonStatusConvert2(int dictationId,int status)
	{
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,status);
		values.put(ISACTIVE,0);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dictationId+"'", null);
		values=null;
		closeDB();
		db=null;
	}
	
	/**
	 * Update dictation status with respect to group Id.
	 * 
	 * @param groupId Group Id
	 * @param status Dictation Status
	 * @param mUpdateGroupId boolean
	 */
	public void updateGroupOfDictationsId(int groupId,int status,boolean mUpdateGroupId)
	{
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,status);
		if(mUpdateGroupId)
			values.put(GROUP_ID,getGroupId());
		db.update(TABLE_DICTATIONS, values,GROUP_ID + "=" + groupId + " AND (" + STATUS + "=" + DictationStatus.SENDING.getValue() 
				+ " OR " + STATUS + "=" + DictationStatus.RETRYING1.getValue() + " OR " + STATUS + "=" + DictationStatus.RETRYING2.getValue()
				+ " OR " + STATUS + "=" + DictationStatus.RETRYING3.getValue() + ") AND " + CONVERTED + "=1", null);
		values=null;
		closeDB();
		db=null;
	}
	
	/**
	 * Update dictation Status and Active values.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateDictationStatusAndIsActive(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS, dCard.getStatus());
		values.put(ISACTIVE, dCard.getIsActive());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dCard.getDictationId()+"'", null);
		closeDB();
		
	}
	
	/**
	 * Update dictation Status and Dictation modified date values.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateDictationStatusAndEndDate(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS, dCard.getStatus());
		values.put(REC_END_DATE, dCard.getRecEndDate());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dCard.getDictationId()+"'", null);
		closeDB();
		
	}
	
	/**
	 * Update dictation Status, sent date, split internal status, main status and query priority.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateStatusAndSentDate(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS, dCard.getStatus());
		values.put(SENT_DATE, dCard.getSentDate());
		values.put(IS_RESEND, dCard.isResend());
		values.put(SPLIT_INTERNAL_STATUS,0);
		values.put(MAIN_STATUS,0);
		values.put(QUERY_PRIORITY,0);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dCard.getDictationId()+"'", null);
		closeDB();
		
	}
	
	/**
	 * Update sent date of Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateSentDate(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(SENT_DATE, dCard.getSentDate());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dCard.getDictationId()+"'", null);
		closeDB();
		
	}
	
	/**
	 * Update settings attributes of a Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateSettingsAttributes(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DSS_VERSION, dCard.getDssVersion());
		values.put(ENCRYPTION_VERSION, dCard.getEncryptionVersion());
		values.put(IS_ENCRYPT, dCard.isEncryption());
		values.put(DELIVERY_METHOD, dCard.getDeliveryMethod());
		values.put(DSS_ENCRYPTION_PASSWORD, dCard.getDssEncryptionPassword());
		values.put(IS_SPLITTABLE, dCard.isFileSplittable());
		values.put(AUTHOR, dCard.getAuthor());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+"'"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates whether the Dictation is active or not.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateIsActive(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(ISACTIVE, dCard.getIsActive());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+dCard.getDictationId(), null);
		closeDB();
		
	}
	
	/**
	 * Updates all active Dictations to inactive.
	 */
	public void updateAllIsActive(){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(ISACTIVE, 0);
		db.update(TABLE_DICTATIONS, values, null, null);
		closeDB();
		
	}
	
	/**
	 * Updates TransferId and Transfer Status of the Dictation.
	 * @param fCard Files details of Dictation as FilesCard.
	 */
	public void updateFileStatus(FilesCard fCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(TRANSFER_ID, fCard.getTransferId());
		values.put(TRANSFER_STATUS, fCard.getTransferStatus());
		db.update(TABLE_FILES, values,FILE_ID+"='"+fCard.getFileId()+"' AND "+INDEX+"='"+fCard.getFileIndex()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates priority of a Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updatePriority(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(PRIORITY, dCard.getPriority());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates whether the Dictation is converted or not. 
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateIsConverted(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(CONVERTED, dCard.getIsConverted());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		if(dCard.getIsConverted()==0)
			db.delete(TABLE_FILES, FILE_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	/**
	 * Updates Dictation name with the passed name if id matches.
	 * @param dictName Dictation name to be updated.
	 * @param dictId Dictation Id.
	 */
	public void updateDictName(String dictName, int dictId){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DICTATION_NAME, dictName);
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dictId+"'", null);
		closeDB();
	}
	/**
	 * Updates Dictation name with the passed name.
	 * @param dCard Dictation details as DictationCard.
	 * @param dictName Dictation name to be updated.
	 * @return True if Dictation name is updated and False if not updated.
	 */
	public boolean updateDictationName(DictationCard dCard, String dictName){
		boolean isUpdated;
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_NAME+"='"+dictName+"' COLLATE NOCASE", null);
		if(cursor.getCount()<1){
			dCard.setDictationName(dictName);
			ContentValues values = new ContentValues();
			values.put(DICTATION_NAME, dCard.getDictationName());
			db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
			isUpdated = true;
		}else{
			isUpdated = false;
		}
		cursor.close();
		closeDB();
		return isUpdated;
	}
	
	 
	/**
	 * Checks whether Dictation name already exists or not. 
	 * @param dictName Dictation name to check existence.
	 * @return True if Dictation exists and False if not.
	 */
	public boolean checkDictationNameExists( String dictName){
		boolean exists;
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_NAME+"='"+dictName+"' COLLATE NOCASE", null);
		if(cursor.getCount()<1){
			exists = false;
		}else{
			exists = true;
		}
		cursor.close();
		closeDB();
		return exists;
	}
	
	
	public boolean checkDictNameExists( String dictName){
			boolean exists;
			SQLiteDatabase db = openDB();
			Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_NAME+"='"+dictName+"' AND "+STATUS+"!='"+DictationStatus.NEW.getValue()+"' COLLATE NOCASE", null);
			if(cursor.getCount()<1){
				exists = false;
			}else{
				exists = true;
			}
			cursor.close();
			closeDB();
			return exists;
		}
	 
	 /**
	  * Checks whether Dictation name or Id exists.
	 * @param dictName Dictation name to check.
	 * @param id Dictation Id.
	 * @return True if exists, False if not.
	 */
	public boolean checkDictNameOrIdExists( String dictName, int id){
			boolean exists;
			SQLiteDatabase db = openDB();
			Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_NAME+"='"+dictName+"' COLLATE NOCASE OR "+DICTATION_ID+"='"+id+"'", null);
			if(cursor.getCount()<1){
				exists = false;
			}else{
				exists = true;
			}
			cursor.close();
			closeDB();
			return exists;
		}
	 
	/**
	 * Update Record started date of Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateRecStartDate(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(CREATED_AT, dCard.getCreatedAt());
		values.put(REC_START_DATE, dCard.getRecStartDate());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates Author name of the Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateAuthor(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DICTATION_NAME, dCard.getDictationName());
		values.put(DICTATION_FILE_NAME, dCard.getDictFileName());
		values.put(AUTHOR, dCard.getAuthor());
		values.put(ISACTIVE, dCard.getIsActive());
		values.put(SEQ_NUMBER, dCard.getSequenceNumber());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates Record stop date and Duration of Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateRecStopDateAndDur(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(REC_END_DATE, dCard.getRecEndDate());
		values.put(DURATION, dCard.getDuration());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates transfer id and job number of Dictation.
	 * @param filesCard Files details of a Dictation as FilesCard.
	 */
	public void updateTransferIdAndJobNo(FilesCard filesCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(TRANSFER_ID, filesCard.getTransferId());
		values.put(JOB_NUMBER, filesCard.getJobNumber());
		values.put(RETRY_COUNT,0);
		db.update(TABLE_FILES, values,FILE_ID+"='"+filesCard.getFileId()+"' AND "+INDEX+"='"
						+filesCard.getFileIndex()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates worktype of the Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateWorktype(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(WORKTYPE, dCard.getWorktype());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates delivery method of the Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateDeliveryMethod(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DELIVERY_METHOD, dCard.getDeliveryMethod());
		values.put(STATUS, dCard.getStatus());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	
	
	/**
	 * Updates files details of a Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateDictationsFiles(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues vals = new ContentValues();
		vals.put(STATUS, dCard.getStatus());
		vals.put(THUMBNAIL_AVAILABLE, dCard.getIsThumbnailAvailable());
		vals.put(DSS_COMPRESSION_MODE, dCard.getDssCompressionMode());
		vals.put(ENCRYPTION_VERSION, dCard.getEncryptionVersion());
		vals.put(IS_ENCRYPT, dCard.isEncryption());
		vals.put(DSS_ENCRYPTION_PASSWORD, dCard.getDssEncryptionPassword());
		vals.put(DSS_VERSION, dCard.getDssVersion());
		vals.put(JOB_NUMBER, dCard.getJobNumber());
		//vals.put(REC_END_DATE, dCard.getRecEndDate());
		
		db.update(TABLE_DICTATIONS, vals, DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Insert files details of the dictation to Database.
	 * @param fCard Files details of the Dictation as FilesCard.
	 */
	public void insertFiles(FilesCard fCard){
		SQLiteDatabase db = openDB();
		ContentValues fileVals = new ContentValues();
		fileVals.put(FILE_ID, fCard.getFileId());
		fileVals.put(FILE_NAME, fCard.getFileName());
		fileVals.put(FILE_SIZE, fCard.getFileSize());
		fileVals.put(INDEX, fCard.getFileIndex());
		fileVals.put(RETRY_COUNT, fCard.getRetryCount());
		fileVals.put(TRANSFER_ID, fCard.getTransferId());
		fileVals.put(TRANSFER_STATUS, fCard.getTransferStatus());
		try {
			db.insert(TABLE_FILES, null, fileVals);
		}
		catch(Exception e) {
			db.insert(TABLE_FILES, null, fileVals);
		}
		
		closeDB();
	}
	
	public void insertRecipient(String mName, String mEmail){
		SQLiteDatabase db = openDB();
		ContentValues mValues = new ContentValues();
		mValues.put(NAME, mName);
		mValues.put(EMAIL, mEmail);
		db.insert(TABLE_RECIPIENTS, null, mValues);
	}
	
	public void updateRecipient(int mID, String mEmail){
		SQLiteDatabase db = openDB();
		ContentValues mValues = new ContentValues();
		mValues.put(EMAIL, mEmail);
		db.update(TABLE_RECIPIENTS, mValues, ID + "=" + mID, null);
	}
	
	public Cursor getReceipent(int mID){
		SQLiteDatabase db = openDB();
		return db.rawQuery("SELECT * FROM " + TABLE_RECIPIENTS + " WHERE " + ID + "=" + mID, null);
	}
	public Cursor getAllRecipient(){
		SQLiteDatabase db = openDB();
		return db.rawQuery("SELECT * FROM " + TABLE_RECIPIENTS, null);
	}
	
	
	
	public void deleteRecipient(int mID){
		SQLiteDatabase db = openDB();
		db.delete(TABLE_RECIPIENTS, ID + "=" + mID, null);
		closeDB();
	}
	
	/**
	 * Update thumbnail availability of Dictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateIsThumbnailAvailable(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(THUMBNAIL_AVAILABLE, dCard.getIsThumbnailAvailable());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Updates properties of aDictation.
	 * @param dCard Dictation details as Dictation card.
	 */
	public void updateDictationProperty(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues vals = new ContentValues();
		vals.put(DICTATION_NAME, dCard.getDictationName());
		vals.put(WORKTYPE, dCard.getWorktype());
		vals.put(PRIORITY, dCard.getPriority());
		vals.put(THUMBNAIL_AVAILABLE, dCard.getIsThumbnailAvailable());
		vals.put(COMMENT, dCard.getComments());
		db.update(TABLE_DICTATIONS, vals, DICTATION_ID+"='"+dCard.getDictationId()+"'", null);
		closeDB();
	}
	
	/**
	 * Get Dictations filtered with the given search string.
	 * @param searchString Search value entered by the user as String.
	 * @return Dictations as cursor.
	 */
	public Cursor getSearchFilteredDictations(String searchString){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.PENDING.getValue()+"' AND ("
		+DICTATION_NAME+" LIKE \"%"
		+searchString+"%\" ESCAPE '\\' OR "+WORKTYPE+" LIKE \"%"+searchString+"%\" ESCAPE '\\' OR "+COMMENT+" LIKE \"%"+searchString+
		"%\" ESCAPE '\\') ORDER BY "+REC_END_DATE+" DESC", null);
		
		return cursor;
	}
	
	/**
	 * Get Outbox Dictations filtered with the given search string.
	 * @param searchString Search value entered by the user as String.
	 * @return Dictations as cursor.
	 */
	public Cursor getSearchFilteredOutboxDictations(String searchString){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.OUTBOX.getValue()+"' OR "
				+STATUS+"='"+DictationStatus.SENDING.getValue()+"' OR "+STATUS+"='"+DictationStatus.TIMEOUT.getValue()+"' " +
				"OR "+STATUS+"='"+DictationStatus.RETRYING1.getValue()+"' OR "+STATUS+"='"+DictationStatus.RETRYING2.getValue()+
				"' OR "+STATUS+"='"+DictationStatus.RETRYING3.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENDING_FAILED.getValue()+
				"' OR "+STATUS+"='"+DictationStatus.CONVERTION_FAILED.getValue()+"' OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND1.getValue()+"'" +
				" OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND2.getValue()+"') AND ("+DICTATION_NAME+" LIKE \"%"
				+searchString+"%\" ESCAPE '\\' OR "+WORKTYPE+" LIKE \"%"+searchString+"%\" ESCAPE '\\' OR "+COMMENT+" LIKE \"%"+searchString+
				"%\" ESCAPE '\\') ORDER BY "+STATUS+" ASC, "+REC_END_DATE+" DESC", null);
		return cursor;
	}
	

	/**
	 * Get Sent Dictations filtered with the given search string.
	 * @param searchString Search value entered by the user as String.
	 * @return Dictations as cursor.
	 */
	public Cursor getSearchFilteredSentDictations(String searchString){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.SENT.getValue()+"' OR "
		+STATUS+"='"+DictationStatus.SENT_VIA_EMAIL.getValue()+"') AND ("+DICTATION_NAME+" LIKE \"%"
		+searchString+"%\" ESCAPE '\\' OR "+WORKTYPE+" LIKE \"%"+searchString+"%\" ESCAPE '\\' OR "+COMMENT+" LIKE \"%"+searchString+
		"%\" ESCAPE '\\') ORDER BY "+SENT_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Get Dictations filtered with search string and ordered with priority in descending order.
	 * @param searchString Search value entered by the user as String.
	 * @return Dictations as cursor.
	 */
	public Cursor getSearchFilteredAndPrioritisedDicts(String searchString){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.PENDING.getValue()+"' AND ("
		+DICTATION_NAME+" LIKE \"%"
		+searchString+"%\" ESCAPE '\\' OR "+WORKTYPE+" LIKE \"%"+searchString+"%\" ESCAPE '\\' OR "+COMMENT+" LIKE \"%"+searchString+
		"%\" ESCAPE '\\') ORDER BY "+PRIORITY+" DESC, "+REC_END_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Get Outbox Dictations filtered with search string and ordered with priority in descending order.
	 * @param searchString Search value entered by the user as String.
	 * @return Dictations as cursor.
	 */
	public Cursor getSearchFilteredAndPrioritisedOutboxDictations(String searchString){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.OUTBOX.getValue()+"' OR "+
				STATUS+"='"+DictationStatus.SENDING.getValue()+"' OR "+STATUS+"='"+DictationStatus.TIMEOUT.getValue()+"' " +
				"OR "+STATUS+"='"+DictationStatus.RETRYING1.getValue()+"' OR "+STATUS+"='"+DictationStatus.RETRYING2.getValue()+"' OR "+
				STATUS+"='"+DictationStatus.RETRYING3.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENDING_FAILED.getValue()+"' OR "+
				STATUS+"='"+DictationStatus.CONVERTION_FAILED.getValue()+"' OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND1.getValue()
				+"' OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND2.getValue()+"') AND ("
				+DICTATION_NAME+" LIKE \"%" +searchString+"%\" ESCAPE '\\' OR "+WORKTYPE+" LIKE \"%"+searchString+"%\" ESCAPE '\\' OR "
				+COMMENT+" LIKE \"%"+searchString+"%\" ESCAPE '\\') ORDER BY "+PRIORITY+" DESC, "+STATUS+" ASC, "+REC_END_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Get Sent Dictations filtered with search string and ordered with priority in descending order.
	 * @param searchString Search value entered by the user as String.
	 * @return Dictations as cursor.
	 */
	public Cursor getSearchFilteredAndPrioritisedSentDictations(String searchString){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.SENT.getValue()+"' OR "+
		STATUS+"='"+DictationStatus.SENT_VIA_EMAIL.getValue()+"') AND ("+DICTATION_NAME+" LIKE \"%"
		+searchString+"%\" ESCAPE '\\' OR "+WORKTYPE+" LIKE \"%"+searchString+"%\" ESCAPE '\\' OR "+COMMENT+" LIKE \"%"+searchString+
		"%\" ESCAPE '\\') ORDER BY "+PRIORITY+" DESC, "+SENT_DATE+" DESC", null);
		return cursor;
	}
	
	/**
	 * Checks whether any Dictation is in active state or not.
	 * @return Active Dictation as cursor.
	 */
	public Cursor checkActiveDictationExists(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+ISACTIVE+"='"+1+"'", null);
		if(cursor.moveToFirst()){
			return cursor;
		}else{
			cursor = null;
			return cursor;
		}
	}
	/**
	 * Checks whether any Dictation is in active state or not which has greater than 0 secs duration.
	 * @return Active Dictation as cursor.
	 */
	public Cursor checkActiveDictationExistsWithDuration(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+ISACTIVE+"='"+1+"' AND "+DURATION+">'0' AND "+ISFLASHAIR+"='0'", null);
		if(cursor.moveToFirst()){
			return cursor;
		}else{
			return null;
		}
		
		
	}
	/**
	 * Checks whether any Dictation is in active state or not which has 0 secs duration.
	 * @return Active Dictation as cursor.
	 */
	public Cursor checkActiveDictationExistsWithNoDuration(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+ISACTIVE+"='"+1+"' AND "+DURATION+"='0'", null);
		if(cursor.moveToFirst()){
			return cursor;
		}else{
			return cursor;
		}
	}
	
	/**
	 * Updates status and active state of a Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateStatusAndActive(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS, dCard.getStatus());
		values.put(ISACTIVE, dCard.getIsActive());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"="+dCard.getDictationId(), null);
		closeDB();
	}
	
	/**
	 * Updates duration and active state of a Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateDurationAndStatus(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS, dCard.getStatus());
		values.put(DURATION, dCard.getDuration());
		values.put(REC_END_DATE, dCard.getRecEndDate());
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"="+dCard.getDictationId(), null);
		closeDB();
	}
	
	
	/**
	 * Updates all dictations in 'Sending' has move to 'Timeout' status(when critical error occurs).
	 * 
	 * @param groupId groupId for the dictation's in conversion process as int.
	 */
	public void updateGroupOfStatusToTimeout(int groupId){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.TIMEOUT.getValue());
		values.put(SPLIT_INTERNAL_STATUS,0);
		values.put(GROUP_ID,0);
		values.put(MAIN_STATUS,0);
		values.put(QUERY_PRIORITY,0);
		db.update(TABLE_DICTATIONS, values,GROUP_ID+"!="+groupId+" AND "+STATUS+"!="+DictationStatus.PENDING.getValue()+" AND "+STATUS+"!="+DictationStatus.SENT.getValue()+
				" AND "+STATUS+"!="+DictationStatus.SENT_VIA_EMAIL.getValue()+" AND "+STATUS+"!="+DictationStatus.NEW.getValue()+" AND "+STATUS+"!="+DictationStatus.SENDING_FAILED.getValue()+
				" AND "+STATUS+"!="+DictationStatus.CONVERTION_FAILED.getValue(), null);

		closeDB();
	}
	
	/**
	 * Delete dictations based on the keep sent value in Settings.
	 * @param option Keep sent value as int.
	 */
	public void deleteDcitationFromKeepSent(int option){
		SQLiteDatabase db = openDB();
		String opt = getKeepSentOption(option);
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+STATUS+"='"+DictationStatus.SENT.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENT_VIA_EMAIL.getValue()+"') AND "+SENT_DATE+"<=date('now','"+opt+"')", null);
		if(cursor.moveToFirst()){
			do{
				String path = DMApplication.DEFAULT_DIR+DMApplication.DEFAULT_DICTATIONS_DIR+
						cursor.getInt(cursor.getColumnIndex(SEQ_NUMBER));
				DMApplication.deleteDir(path);
			}while(cursor.moveToNext());
		}
		db.delete(TABLE_DICTATIONS,"("+STATUS+"='"+DictationStatus.SENT.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENT_VIA_EMAIL.getValue()+"') AND "+SENT_DATE+"<=date('now','"+opt+"')" , null);
		cursor.close();
		closeDB();
	}
	
	/**
	 * Deletes files list for the particular Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void deleteFileList(DictationCard dCard){
		SQLiteDatabase db = openDB();
		db.delete(TABLE_FILES,FILE_ID+"="+dCard.getDictationId() , null);
		closeDB();
	}
	
	
	/**
	 * Get group id of dictations sending to Server.
	 * @return Group id.
	 */
	public int getGroupId(){
		SQLiteDatabase db = openDB();
		int groupId = 0;
		try
		{
			Cursor c = db.rawQuery("SELECT MAX("+GROUP_ID+") FROM "+TABLE_DICTATIONS, null);
			if(c.moveToFirst()){
				groupId = c.getInt(0);
			}
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		groupId++;
		return groupId;
	}
	
	/**
	 * Updates group id of dictations sending to Server.
	 * @param dictationId Id of Dictation.
	 * @param groupId Group id to update.
	 */
	public void updateGroupId(int dictationId, int groupId){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(GROUP_ID, groupId);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+dictationId, null);
		closeDB();
	}
	
	/**
	 * Updates group id of dictations sending to Server.
	 * @param dictationId Id of Dictation.
	 * @param groupId Group id to update.
	 */
	public void updateGroupIdDictsView(int dictationId, int groupId){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(GROUP_ID, groupId);
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+dictationId, null);
		closeDB();
	}
	
	/**
	 * Updates duration of Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateDuration(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(DURATION, dCard.getDuration());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+dCard.getDictationId(), null);
		closeDB();
	}
	
	/**
	 * Updates recording end date of Dictation.
	 * @param dCard Dictation details as DictationCard.
	 */
	public void updateRecEndDate(DictationCard dCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(REC_END_DATE, dCard.getRecEndDate());
		db.update(TABLE_DICTATIONS, values, DICTATION_ID+"="+dCard.getDictationId(), null);
		closeDB();
	}
	
	/**
	 * Update retry count for all FilesCard in the dictation.
	 * 
	 * @param count retry count
	 */
	public void updateAllRetryCount(int count){
		try
		{
			SQLiteDatabase db = openDB();
			ContentValues values = new ContentValues();
			values.put(RETRY_COUNT, count);
			db.update(TABLE_FILES, values, null, null);
			closeDB();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update retry count for a specific FilesCard in the dictation.
	 * 
	 * @param fCard Files details of Dictation as FilesCard.
	 */
	public void updateRetryCount(FilesCard fCard){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(RETRY_COUNT, fCard.getRetryCount());
		db.update(TABLE_FILES, values,FILE_ID+"="+fCard.getFileId()+" AND "+INDEX+"="+fCard.getFileIndex(), null);
		closeDB();
	}
	
	/**
	 * Get retry count of a specific file in a dictation.
	 * @param fCard Files details of Dictation as FilesCard.
	 * @return retry count
	 */
	public int getRetryCount(FilesCard fCard){
		SQLiteDatabase db = openDB();
		int count=0;
		try
		{
			Cursor c = db.rawQuery("SELECT "+RETRY_COUNT+" FROM "+TABLE_FILES+" WHERE "+
					FILE_ID+"='"+fCard.getFileId()+"' AND "+INDEX+"='"+fCard.getFileIndex()+"'", null);
			if(c.moveToFirst())
				count=c.getInt(c.getColumnIndex(RETRY_COUNT));
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return count;
	}
	
	/**
	 * Get no; of non-converted dictation card a group.
	 * @param groupId current converting groupId
	 * @return no; of non-converted dictation card a group.
	 */
	public int getNonConvertedGroupCount(int groupId){
		SQLiteDatabase db = openDB();
		int count=0;
		Cursor c = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+GROUP_ID+"='"+groupId+"' AND "+CONVERTED+"='0' AND "+GROUP_ID+">'0'", null);
		if(c.moveToFirst())
			count= c.getCount();
		c.close();
		closeDB();
		return count;
	}
	
	/**
	 * Checks whether Dictation name already exists or not. 
	 * @param dictName Dictation name to check existence.
	 * @return True if Dictation exists and False if not.
	 */
	public boolean isDictationExists( String dictName){
		boolean exists;
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+DICTATION_NAME+"='"+dictName+"' COLLATE NOCASE", null);
		if(cursor.getCount()<1){
			exists = false;
		}else{
			exists = true;
		}
		cursor.close();
		closeDB();
		return exists;
	}
	
	/**
	 * Gets new Dictation with no recording in it.
	 * @return Dictation details as Cursor.
	 */
	public Cursor getNewDictationExisting(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.NEW.getValue()+"' AND "+DURATION+"='0' AND "+ISFLASHAIR+"='0'", null);
		return cursor;
	}
	
	/**
	 * Gets new Dictation existing with 0 secs or greater duration. 
	 * @return Dictation details as Cursor.
	 */
	public Cursor getNewDictationExistingWithDuration(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.NEW.getValue()+"' AND "+DURATION+">'0'", null);
		return cursor;
	}
	
	/**
	 * Get keep sent option in days.
	 * @param option Option value as int.
	 * @return Keep sent option in days as String.
	 */
	private String getKeepSentOption(int option){
		switch (option) {
			case 1:
				return "-1 day";
			case 2:
				return "-3 days";
			case 3:
				return "-7 days";
			case 4:
				return "-14 days";
			case 5:
				return "-1 month";
			default:
				return null;
		}
	}
	
	/**
	 * Get next dictation card for conversion from conversion queue.
	 * 
	 * @return dictation as Cursor
	 */
	public Cursor getConvertionDictation(int mCurrentGroupId){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DICTATIONS+" WHERE " + STATUS + "=" + DictationStatus.OUTBOX.getValue() 
				+ " AND " + CONVERTED+"=0  AND " + ISFLASHAIR+"=0  AND " + STATUS + "!="+ DictationStatus.SENT.getValue() + "  AND "+STATUS
				+"!="+DictationStatus.SENT_VIA_EMAIL.getValue() + " AND " + STATUS + "!="+ DictationStatus.UNKNOWN.getValue() 
				+ " AND " + GROUP_ID+ "=(SELECT MIN(" + GROUP_ID + ") FROM "+TABLE_DICTATIONS + " WHERE " + STATUS+"=" 
				+ DictationStatus.OUTBOX.getValue() + " AND " + CONVERTED+"=0 AND " + ISFLASHAIR+"=0 AND " + STATUS+"!=" 
				+ DictationStatus.SENT.getValue() + "  AND " + STATUS + "!=" + DictationStatus.SENT_VIA_EMAIL.getValue() + " AND " + GROUP_ID 
				+ ">0 AND " + GROUP_ID + "!=" + mCurrentGroupId +") ORDER BY " + GROUP_ID +" ASC , "+ DICTATION_ID+" ASC LIMIT 1", null);
		return cursor;
	}
	
	/**
	 * Get next group of dictation card for uploading from uploading queue.
	 * 
	 * @return group of dictation as Cursor
	 */
	public Cursor getUploadingDictation(){
		SQLiteDatabase db = openDB();
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE ("+MAIN_STATUS+"='200' OR "+MAIN_STATUS+"='400')" 
				+ " AND ("+STATUS+"='"+DictationStatus.SENDING.getValue()+"' OR "+STATUS+"='"+DictationStatus.RETRYING3.getValue()
				+ "') AND "+CONVERTED+"='1' AND "+GROUP_ID+"=(SELECT MIN("+GROUP_ID+") FROM "
				+ TABLE_DICTATIONS+" WHERE ("+MAIN_STATUS+"='200' OR "+MAIN_STATUS+"='400') AND ("+STATUS+"='"+DictationStatus.SENDING.getValue()+"' OR "
				+ STATUS+"='"+DictationStatus.RETRYING3.getValue()+"') AND "+CONVERTED+"=1 AND "+GROUP_ID+">0) AND "+GROUP_ID+">0 ORDER BY "
				+ GROUP_ID+" ASC,"+ DICTATION_ID+" ASC," + REC_END_DATE +" DESC", null);
		return cursor;
	}
	
	/**
	 * Get next dictation card for querying from querying queue.
	 * 
	 * @return dictation as Cursor
	 */
	public Cursor getQueryDictation(){
		SQLiteDatabase db = openDB();
		Cursor cursor=db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE (("+MAIN_STATUS+"='300'" +" AND "+STATUS
				+ "='"+DictationStatus.WAITING_TO_SEND1.getValue()+"') OR ("+MAIN_STATUS+"='400' AND "
				+ QUERY_PRIORITY+">'0' AND "+STATUS+"='"+DictationStatus.SENDING.getValue()+"' AND "+STATUS+"!='"+DictationStatus.RETRYING3.getValue()
				+ "' AND "+SPLIT_INTERNAL_STATUS+"='"+DictationStatus.WAITING_TO_SEND1.getValue()+"')) AND "+STATUS+"!='"
				+ DictationStatus.SENT.getValue()+"' AND "+STATUS+"!='"+DictationStatus.SENT_VIA_EMAIL.getValue() 
				+ "' AND " + CONVERTED + "=1 ORDER BY "+ QUERY_PRIORITY+" ASC LIMIT 1", null);
		return cursor;
	}
	
	/**
	 * Update main status i.e, conversion,uploading and querying of all dictation in a group. 
	 * @param mainStatus main status of a dictation
	 * @param groupId current executing groupId
	 */
	public void updateMainStatus(int mainStatus,int groupId){
		try
		{
			SQLiteDatabase db = openDB();
			ContentValues values = new ContentValues();
			values.put(MAIN_STATUS, mainStatus);
			db.update(TABLE_DICTATIONS, values, GROUP_ID+" = "+groupId+" AND "+STATUS+
					"!="+DictationStatus.SENT.getValue()+" AND "+STATUS+"!="+DictationStatus.SENT_VIA_EMAIL.getValue()+
					" AND "+STATUS+"!="+DictationStatus.TIMEOUT.getValue()+" AND "+STATUS+"!="+DictationStatus.SENDING_FAILED.getValue()+
					" AND "+STATUS+"!="+DictationStatus.PENDING.getValue()+" AND "+STATUS+"!="+DictationStatus.NEW.getValue()+"", null);
			closeDB();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update main status i.e, conversion,uploading and querying of all dictation in a group. 
	 * @param mainStatus main status of a dictation
	 * @param groupId current executing groupId
	 */
	public void updateMainStatusConvert(int mainStatus,int groupId){
		try
		{
			SQLiteDatabase db = openDB();
			ContentValues values = new ContentValues();
			values.put(MAIN_STATUS, mainStatus);
			db.update(TABLE_DICTATIONS, values, GROUP_ID+" = "+groupId+" AND "+STATUS+
					"!="+DictationStatus.SENT.getValue()+" AND "+STATUS+"!="+DictationStatus.SENT_VIA_EMAIL.getValue()+
					" AND "+STATUS+"!="+DictationStatus.TIMEOUT.getValue()+" AND "+STATUS+"!="+DictationStatus.SENDING_FAILED.getValue()+
					" AND "+STATUS+"!="+DictationStatus.PENDING.getValue()+" AND "+STATUS+"!="+DictationStatus.NEW.getValue()+"", null);
			closeDB();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update main status i.e, conversion,uploading and querying of a dictation. 
	 * @param mainStatus main status of a dictation
	 * @param dictationId dictation Id.
	 */
	public void updateMainStatusByDicts(int mainStatus,int dictationId){
		try
		{
			SQLiteDatabase db = openDB();
			ContentValues values = new ContentValues();
			values.put(MAIN_STATUS, mainStatus);
			db.update(TABLE_DICTATIONS, values, DICTATION_ID+" = "+dictationId, null);
			closeDB();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get total no; of file's pending to upload in a dictation.
	 * @param dictationId dictation Id
	 * @return pending file's count
	 */
	public int getSplitUploadPendingCount(int dictationId){
		SQLiteDatabase db = openDB();
		int columns=0;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+dictationId+"' AND "+TRANSFER_ID+"=''", null);
			if(c.moveToFirst())
				columns=c.getCount();
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return columns;
	}
	
	/**
	 * Get total no; of file's for upload in a dictation.
	 * @param dictationId dictation Id
	 * @return pending file's count
	 */
	public int getSplitUploadedCount(int dictationId){
		SQLiteDatabase db = openDB();
		int columns=0;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+dictationId+"' AND "+TRANSFER_ID+"!=''", null);
			if(c.moveToFirst())
				columns=c.getCount();
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return columns;
	}
	
	/**
	 * Get total no; of file's for upload in a dictation.
	 * @param dictationId dictation Id
	 * @return pending file's count
	 */
	public int getCountTotalNoOfSplits(int dictationId){
		SQLiteDatabase db = openDB();
		int columns=0;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+dictationId+"'", null);
			if(c.moveToFirst())
				columns=c.getCount();
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return columns;
	}
	
	/**
	 * Get total no; of file's pending to send in a dictation.
	 * @param dictationId dictation Id
	 * @return pending file's count
	 */
	public int getSplitQueryPendingCount(int dictationId){
		SQLiteDatabase db = openDB();
		int columns=0;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+dictationId+"' AND "+TRANSFER_STATUS+"='0'", null);
			if(c.moveToFirst())
				columns=c.getCount();
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return columns;
	}
	
	/**
	 * Get total no; of sent file's in a dictation.
	 * @param dictationId dictation Id
	 * @return sent file's count
	 */
	public int getSplitQueryCount(int dictationId){
		SQLiteDatabase db = openDB();
		int columns=0;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+dictationId
					+"' AND ("+TRANSFER_STATUS+"!='1' OR "+TRANSFER_ID+"!='')", null);
			if(c.moveToFirst())
				columns=c.getCount();
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return columns;
	}
	
	/**
	 * Update whole dictations in a specific status to 'Sending'(Upload)
	 * @param status specific status
	 */
	public void updateUploadRetryDictation(int status){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.SENDING.getValue());
		db.update(TABLE_DICTATIONS, values,"("+MAIN_STATUS+"=200 OR "+MAIN_STATUS+"=400) AND "+STATUS+"="+status, null);
		closeDB();
	}
	
	/**
	 * Update whole dictations in a specific status to 'Waiting to Send'(due to httpError).
	 * @param status specific status
	 */
	public void updateQuerying5MinRetryDictation(int status){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.WAITING_TO_SEND1.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=300 AND "+STATUS+"="+status, null);
		values = new ContentValues();
		values.put(SPLIT_INTERNAL_STATUS,DictationStatus.WAITING_TO_SEND1.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=400 AND "+SPLIT_INTERNAL_STATUS+"="+status, null);
		closeDB();
	}
	
	/**
	 * Update whole dictations in a querying waiting to 'Waiting to Send'(reload to query queue).
	 */
	public void updateQuery30SecRetryDictation(){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.WAITING_TO_SEND1.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=300 AND "+STATUS+"="+DictationStatus.WAITING_TO_SEND2.getValue()+"", null);
		values = new ContentValues();
		values.put(SPLIT_INTERNAL_STATUS,DictationStatus.WAITING_TO_SEND1.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=400 AND "+SPLIT_INTERNAL_STATUS+"="+DictationStatus.WAITING_TO_SEND2.getValue(), null);
		closeDB();
	}
	
	/**
	 * Update record id of querying dictations to handle query queue.
	 * @param dictationId dictation Id.
	 */
	public void updateQueryPriority(int dictationId){
		SQLiteDatabase db = openDB();
		Cursor c=db.rawQuery("SELECT MAX("+QUERY_PRIORITY+") FROM "+TABLE_DICTATIONS,null);
		ContentValues values = new ContentValues();
		int queryPriority=0;
		if(c.moveToFirst())
			queryPriority=c.getInt(0);
		c.close();
		queryPriority++;
		values.put(QUERY_PRIORITY,queryPriority);
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"="+dictationId, null);
		closeDB();
	}
	
	/**
	 * Update/reload whole dictations in sending process, for restart the interrupted dictations.
	 */
	public void updateOnLaunch(){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.OUTBOX.getValue());
		db.update(TABLE_DICTATIONS, values,STATUS+"="+DictationStatus.SENDING.getValue()+" AND "+CONVERTED+"=0 AND "+ISFLASHAIR+"=0", null);
		values = new ContentValues();
		values.put(STATUS,DictationStatus.SENDING.getValue());
		db.update(TABLE_DICTATIONS, values,"("+MAIN_STATUS+"=200 OR "+MAIN_STATUS+"=400) AND "+STATUS+"!="+DictationStatus.SENT.getValue()+" AND "
				+ STATUS+"!="+DictationStatus.SENT_VIA_EMAIL.getValue()+" AND "+STATUS+"!="+DictationStatus.SENDING_FAILED.getValue()+" AND "
				+ STATUS+"!="+DictationStatus.TIMEOUT.getValue()+" AND " + STATUS+"!="+DictationStatus.UNKNOWN.getValue(), null);
		values = new ContentValues();
		values.put(STATUS,DictationStatus.WAITING_TO_SEND1.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=300 AND "+STATUS+"!="+DictationStatus.SENT.getValue()+" AND "
				+ STATUS+"!="+DictationStatus.SENT_VIA_EMAIL.getValue()+"  AND "+ STATUS+"!="+DictationStatus.SENDING_FAILED.getValue()
				+ " AND "+STATUS+"!="+DictationStatus.TIMEOUT.getValue() + " AND " + STATUS+"!="+DictationStatus.UNKNOWN.getValue(), null);
		closeDB();
	}
	
	/**
	 * Update whole dictations in a upload queue to 'Retrying' status, when their is any Network Connection problem.
	 */
	public void updateWholeUploadToRetryDictation(){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.RETRYING2.getValue());
		values.put(MAIN_STATUS,200);
		db.update(TABLE_DICTATIONS, values,"("+MAIN_STATUS+"=200 OR "+MAIN_STATUS+"=400) AND "+STATUS+"="+DictationStatus.RETRYING1.getValue()+"", null);
		closeDB();
	}
	
	/**
	 * Update whole dictations in a query queue to 'Retrying' status, when their is any Network Connection problem.
	 */
	public void updateWholeQueryToRetryDictation(){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(STATUS,DictationStatus.RETRYING2.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=300 AND "+STATUS+"="+DictationStatus.WAITING_TO_SEND2.getValue()+"", null);
		values = new ContentValues();
		values.put(SPLIT_INTERNAL_STATUS,DictationStatus.RETRYING2.getValue());
		db.update(TABLE_DICTATIONS, values,MAIN_STATUS+"=400 AND "+SPLIT_INTERNAL_STATUS+"="+DictationStatus.WAITING_TO_SEND2.getValue(), null);
		closeDB();
	}
	
	/**
	 * Update the dictation split status for handle splitted dictations.
	 * @param dictationId dictation id
	 * @param status dictation internal status
	 */
	public void updateSplitInternalStatus(int dictationId,int status){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(SPLIT_INTERNAL_STATUS,status);
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"="+dictationId, null);
		closeDB();
	}
	
	/**
	 * Get the total no; of files added to querying queue.
	 * @param dictationId  dictation id
	 * @return uploaded file's count
	 */
	public int toCheckMoveToQuerying(int dictationId){
		SQLiteDatabase db = openDB();
		Cursor c=db.rawQuery("SELECT * FROM " + TABLE_FILES + " WHERE " + FILE_ID + "=" + dictationId + " AND "
				+ TRANSFER_ID + "!='' AND " + JOB_NUMBER + "!='' AND " + FILE_ID + " IN (SELECT " + DICTATION_ID + " FROM "
				+ TABLE_DICTATIONS + " WHERE " + DICTATION_ID + "=" + dictationId + " AND (" + STATUS + "=" 
				+ DictationStatus.RETRYING1.getValue() + " OR " + STATUS + "=" + DictationStatus.RETRYING2.getValue() 
				+ "  OR " + STATUS + "=" + DictationStatus.TIMEOUT.getValue() + ") AND " + CONVERTED + "=1)",null);
		if(c.moveToFirst())
			return c.getCount();
		c.close();
		return 0;
	}
	
	/**
	 * Get whole dictations name in an Application.
	 * @return whole dictation's name
	 */
	public ArrayList<String> getDictationNames(){
		SQLiteDatabase db = openDB();
		ArrayList<String > dNameList = new ArrayList<String>();
		Cursor cursor = db.rawQuery("SELECT "+DICTATION_NAME+" FROM "+TABLE_DICTATIONS, null);
		if(cursor.moveToFirst()){
			do{
				dNameList.add(cursor.getString(cursor.getColumnIndex(DICTATION_NAME)).toLowerCase());
			}while(cursor.moveToNext());
		}
		cursor.close();
		return dNameList;
	}
	
	/**
	 * Update transfer id and related parameters as default in a dictation.
	 * @param dictationId dictation id.
	 */
	public void updateWholeFileTransferId(int dictationId){
		SQLiteDatabase db = openDB();
		ContentValues values = new ContentValues();
		values.put(TRANSFER_ID,"");
		values.put(TRANSFER_STATUS,"");
		db.update(TABLE_FILES, values,FILE_ID+"='"+dictationId+"'", null);
		values = new ContentValues();
		values.put(CONVERTED,0);
		values.put(MAIN_STATUS,0);
		values.put(GROUP_ID,0);
		values.put(SPLIT_INTERNAL_STATUS, 0);
		db.update(TABLE_DICTATIONS, values,DICTATION_ID+"='"+dictationId+"'", null);
		closeDB();
	}
	
	/**
	 * Get total no; of sending failed(i.e,conversion failed, sending failed,timeout) items.
	 * @return total no; of sending failed items.
	 */
	public int getSendingFailedDictations(){
		SQLiteDatabase db = openDB();
		int count=0;
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_DICTATIONS+" WHERE "+STATUS+"='"+DictationStatus.OUTBOX.getValue()+"' OR "+STATUS+"='"+DictationStatus.SENDING_FAILED.getValue()+"' OR "
				+ STATUS+"='"+DictationStatus.SENDING.getValue()+"' OR "+STATUS+"='"+DictationStatus.TIMEOUT.getValue()+"' OR "+STATUS+"='"
				+DictationStatus.RETRYING1.getValue()+"' OR "+STATUS+"='"+DictationStatus.RETRYING2.getValue()+"' OR "
				+ STATUS+"='"+DictationStatus.RETRYING3.getValue()+"' OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND1.getValue()+
				"' OR "+STATUS+"='"+DictationStatus.WAITING_TO_SEND2.getValue()+"'", null);
		if(cursor.moveToFirst())
			count=cursor.getCount();
		return count;
	}
	
	/**
	 * Check whether the dictation is already sent or not
	 * @param dictationId dictation id
	 * @return if 0 then the dictation is already sent
	 */
	public int getCheckAlreadySentQueryCount(int dictationId){
		SQLiteDatabase db = openDB();
		int columns=0;
		try
		{
			Cursor c = db.rawQuery("SELECT * FROM "+TABLE_FILES+" WHERE "+FILE_ID+"='"+dictationId
					+"' AND ("+TRANSFER_STATUS+"=1 OR "+TRANSFER_STATUS+"=2)", null);
			if(c.moveToFirst())
				columns=c.getCount();
			c.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		closeDB();
		return columns;
	}
	
	/**
	 * Rename the dictations when 'author' is changed
	 * @param mValue Author Name
	 */
	public void updateDicationName(String mValue) {
		String mSubName = "";
		if(mValue == null || mValue.length() == 0)
			mValue = "AUTHOR";
		if(mValue.length() > 4)
			mSubName = mValue.substring(0, 4);
		else
			mSubName = mValue;
		SQLiteDatabase db = openDB();
		db.execSQL("UPDATE " + TABLE_DICTATIONS + " SET " + DICTATION_NAME + "=('" + mSubName + "'||SUBSTR('0000',length(" 
				+ SEQ_NUMBER + ")+1)||" + SEQ_NUMBER + ")," + AUTHOR + "='" + mValue + "' WHERE " + DICTATION_NAME + "=(SUBSTR(" 
				+ AUTHOR + ",0,5)||SUBSTR('0000',length(" + SEQ_NUMBER + ")+1)||" + SEQ_NUMBER + ") AND ('" + mSubName 
				+ "'||SUBSTR('0000',length(" + SEQ_NUMBER + ")+1)||" + SEQ_NUMBER + ") NOT IN(SELECT " + DICTATION_NAME 
				+ " COLLATE NOCASE FROM " + TABLE_DICTATIONS + ")  AND (" + STATUS + "=" + DictationStatus.PENDING.getValue() 
				+ " OR " + STATUS + "=" + DictationStatus.NEW.getValue() + ")" );
	}
}
