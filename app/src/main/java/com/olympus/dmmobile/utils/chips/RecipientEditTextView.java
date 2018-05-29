package com.olympus.dmmobile.utils.chips;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.text.style.ImageSpan;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.olympus.dmmobile.R;

/**
 * RecipientEditTextView is an auto complete text view to show recipients with new chip UI
 * @version 1.2.0
 */
public class RecipientEditTextView extends MultiAutoCompleteTextView implements
		OnItemClickListener,/* Callback, */
		GestureDetector.OnGestureListener, OnDismissListener, OnClickListener,
		TextView.OnEditorActionListener {

	private static final char COMMIT_CHAR_COMMA = ',';

	private static final char NAME_WRAPPER_CHAR = '"';

	private static final char COMMIT_CHAR_SEMICOLON = ';';

	private static final char COMMIT_CHAR_SPACE = ' ';

	private static final String TAG = "RecipientEditTextView";

	private static int DISMISS = "dismiss".hashCode();

	private static final long DISMISS_DELAY = 300;

	// TODO: get correct number/ algorithm from with UX.
	// Visible for testing.
	/* package */static final int CHIP_LIMIT = 2;

	public static final int MAX_CHIPS_PARSED = 64;

	private static int sSelectedTextColor = -1;

	// Resources for displaying chips.
	private Drawable mChipBackground = null;

	private Drawable mChipDelete = null;

	private Drawable mInvalidChipBackground;

	private Drawable mChipBackgroundPressed;

	private float mChipHeight;

	private float mChipFontSize;

	private float mLineSpacingExtra;

	private int mChipPadding;

	private Tokenizer mTokenizer;

	private Validator mValidator;

	private RecipientChip mSelectedChip;

	private int mAlternatesLayout;

	private Bitmap mDefaultContactPhoto;

	private ImageSpan mMoreChip;

	private TextView mMoreItem;

	private final ArrayList<String> mPendingChips = new ArrayList<String>();

	private Handler mHandler;

	private int mPendingChipsCount = 0;

	private boolean mNoChips = false;

	private ArrayList<RecipientChip> mTemporaryRecipients;

	private ArrayList<RecipientChip> mRemovedSpans;

	private boolean mShouldShrink = true;

	// Chip copy fields.
	private GestureDetector mGestureDetector;

	private Dialog mCopyDialog;

	private String mCopyAddress;

	
	private OnItemClickListener mAlternatesListener;

	private int mCheckedItem;

	private TextWatcher mTextWatcher;

	// Obtain the enclosing scroll view, if it exists, so that the view can be
	// scrolled to show the last line of chips content.
	private ScrollView mScrollView;

	private boolean mTriedGettingScrollView;

	private boolean mDragEnabled = false;

	Context mContext;
	RecipientEditorListener editorListener;
	// This pattern comes from android.util.Patterns. It has been tweaked to
	// handle a "1" before
	// parens, so numbers such as "1 (425) 222-2342" match.
	private static final Pattern PHONE_PATTERN = Pattern.compile( // sdd =
																	// space,
																	// dot, or
																	// dash
			"(\\+[0-9]+[\\- \\.]*)?" // +<digits><sdd>*
					+ "(1?[ ]*\\([0-9]+\\)[\\- \\.]*)?" // 1(<digits>)<sdd>*
					+ "([0-9][0-9\\- \\.][0-9\\- \\.]+[0-9])"); // <digit><digit|sdd>+<digit>

	private final Runnable mAddTextWatcher = new Runnable() {
		@Override
		public void run() {
			if (mTextWatcher == null) {
				mTextWatcher = new RecipientTextWatcher();
				addTextChangedListener(mTextWatcher);
			}
		}
	};
	
	private Runnable mHandlePendingChips = new Runnable() {

		@Override
		public void run() {
			handlePendingChips();
		}

	};

	private Runnable mDelayedShrink = new Runnable() {

		@Override
		public void run() {
			shrink();
		}

	};

	private int mMaxLines;

	private static int sExcessTopPadding = -1;

	private int mActionBarHeight;

	public RecipientEditTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setChipDimensions(context, attrs);
		if (sSelectedTextColor == -1) {
			sSelectedTextColor = context.getResources().getColor(
					android.R.color.white);
		}
		mContext = context;
		editorListener = (RecipientEditorListener) mContext;
		
		 
		 
		mCopyDialog = new Dialog(context);

		
		setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		setOnItemClickListener(this);
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == DISMISS) {
					return;
				}
				super.handleMessage(msg);
			}
		};
		mTextWatcher = new RecipientTextWatcher();
		addTextChangedListener(mTextWatcher);
		mGestureDetector = new GestureDetector(context, this);
		setOnEditorActionListener(this);
		mMaxLines = getLineCount();
	}

	public boolean isRecipientLimitExceeded(){
		RecipientChip[] chips = getSortedRecipients();
		if(chips != null && chips.length >= MAX_CHIPS_PARSED){
			return true;
		}
		
		return false;
	}
	public int getRecipientCount(){
		RecipientChip[] chips = getSortedRecipients();
		if(chips != null){
			return chips.length;
		}
		return 0;
	}
	public boolean checkForInvalidCharacters(String text){
		String[] characters = {"<",">","(",")",";"};
		for(String character:characters){
			if(text.contains(character)){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
		if (action == EditorInfo.IME_ACTION_DONE) {
			boolean invalidEmailsFound = false;
			
			Rfc822Token[] tokens = null;
			if (!TextUtils.isEmpty(getText().toString().trim())) {
				tokens = Rfc822Tokenizer.tokenize(getText().toString());
				if (tokens.length > 0) {
					for (Rfc822Token token : tokens) {
						if (!isValidEmail(token.getAddress())) {
							invalidEmailsFound = true;
						}
					}
					
					int chip_end = getLastChipEnd();
					String str = getText().toString();
					if(chip_end != -1){
						str = str.substring(chip_end,str.length());
					}else{
						str = str.substring(0,str.length());
					}
					if(!TextUtils.isEmpty(str) && checkForInvalidCharacters(str)){
						editorListener.onInvalidRecipientEntered();
						return true;
					}
					
					//check if the entered recipient is already added or not
					String text = getText().toString();
					int tokenStart = mTokenizer.findTokenStart(text,
							getSelectionEnd());
					String sub = text.substring(tokenStart,
							mTokenizer.findTokenEnd(text, tokenStart));
					
					int end = getSelectionEnd();
					int len = length() - 1;
					
					if(sub.trim().equalsIgnoreCase("")){
						if (mTextWatcher != null) {
							removeTextChangedListener(mTextWatcher);
						}
						int start_pos = getLastChipEnd();
						if(start_pos == -1){
							start_pos = 0;
						}
						
						if(end != len){
							getText().replace(start_pos, end, "").toString();
						}
						else{
							getText().replace(start_pos, len, "").toString();
						}
						mHandler.post(mAddTextWatcher);
						editorListener.onInvalidRecipientEntered();
						return true;
					}
					// if already added, show alert
					if(doesThisRecipientAlreadyExists(sub)){
						editorListener.onRecipientAlreadyExist();
						return true;
					}
					
					if (!invalidEmailsFound) {
						// create chips for the recipients entered
						if (commitDefault()) {
							editorListener.onValidationSuccess(getText().toString().trim().substring(0, getText().length() - 2));
							return true;
						}
						if (mSelectedChip != null) {
							clearSelectedChip();
							return true;
						} else if (focusNext()) {
							return true;
						}
						editorListener.onValidationSuccess(getText().toString().trim().substring(0, getText().toString().trim().length() - 1));
					} else {
						// invalid recipients entered
						editorListener.onInvalidRecipientEntered();
					}
				}else{
					editorListener.onInvalidRecipientEntered();
				}
			} else {
				// empty text
				setText("");
				editorListener.onEmptyTextEntered();
			}

		}
		return false;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		InputConnection connection = super.onCreateInputConnection(outAttrs);
		int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
		if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
			// clear the existing action
			outAttrs.imeOptions ^= imeActions;
			// set the DONE action
			outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
		}
		if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
			outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
		}
		// outAttrs.actionLabel = getContext().getString(R.string.done);
		return connection;
	}

	/* package */RecipientChip getLastChip() {
		RecipientChip last = null;
		RecipientChip[] chips = getSortedRecipients();
		if (chips != null && chips.length > 0) {
			last = chips[chips.length - 1];
		}
		return last;
	}

	@Override
	public void onSelectionChanged(int start, int end) {
		// When selection changes, see if it is inside the chips area.
		// If so, move the cursor back after the chips again.
		RecipientChip last = getLastChip();
		if (last != null && start < getSpannable().getSpanEnd(last)) {
			// Grab the last chip and set the cursor to after it.
			setSelection(Math.min(getSpannable().getSpanEnd(last) + 1,
					getText().length()));
		}
		super.onSelectionChanged(start, end);
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (!TextUtils.isEmpty(getText())) {
			super.onRestoreInstanceState(null);
		} else {
			super.onRestoreInstanceState(state);
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		// If the user changes orientation while they are editing, just roll
		// back the selection.
		clearSelectedChip();
		return super.onSaveInstanceState();
	}

	/**
	 * Convenience method: Append the specified text slice to the TextView's
	 * display buffer, upgrading it to BufferType.EDITABLE if it was not already
	 * editable. Commas are excluded as they are added automatically by the
	 * view.
	 */
	@Override
	public void append(CharSequence text, int start, int end) {
		// We don't care about watching text changes while appending.
		if (mTextWatcher != null) {
			removeTextChangedListener(mTextWatcher);
		}
		super.append(text, start, end);
		if (!TextUtils.isEmpty(text) && TextUtils.getTrimmedLength(text) > 0) {
			String displayString = text.toString();
			int separatorPos = displayString.lastIndexOf(COMMIT_CHAR_COMMA);
			// Verify that the separator pos is not within ""; if it is, look
			// past the closing quote. If there is no comma past ", this string
			// will resolve to an error chip.
			if (separatorPos > -1) {
				String parseDisplayString = displayString
						.substring(separatorPos);
				
				int endQuotedTextPos = parseDisplayString
						.indexOf(NAME_WRAPPER_CHAR);
				if (endQuotedTextPos > separatorPos) {
					separatorPos = parseDisplayString.lastIndexOf(
							COMMIT_CHAR_COMMA, endQuotedTextPos);
				}
			}
			if (!TextUtils.isEmpty(displayString)
					&& TextUtils.getTrimmedLength(displayString) > 0) {
				Rfc822Token[] tokens = null;
				tokens = Rfc822Tokenizer.tokenize(displayString);
				
				if (tokens.length > 0) {
					for (Rfc822Token token : tokens) {
						mPendingChipsCount++;
						mPendingChips.add(token.toString());
					}
					
				}
				
			}
		}
		// Put a message on the queue to make sure we ALWAYS handle pending
		// chips.
		if (mPendingChipsCount > 0) {
			postHandlePendingChips();
		}
		mHandler.post(mAddTextWatcher);
	}

	@Override
	public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
		super.onFocusChanged(hasFocus, direction, previous);
		if (!hasFocus) {
			shrink();
		} else {
			expand();
		}
	}

	private int getExcessTopPadding() {
		if (sExcessTopPadding == -1) {
			sExcessTopPadding = (int) (mChipHeight + mLineSpacingExtra);
		}
		return sExcessTopPadding;
	}

	private void scrollBottomIntoView() {
		if (mScrollView != null && mShouldShrink) {
			int[] location = new int[2];
			getLocationOnScreen(location);
			int height = getHeight();
			int currentPos = location[1] + height;
			// Desired position shows at least 1 line of chips below the action
			// bar. We add excess padding to make sure this is always below
			// other
			// content.
			int desiredPos = (int) mChipHeight + mActionBarHeight
					+ getExcessTopPadding();
			if (currentPos > desiredPos) {
				mScrollView.scrollBy(0, currentPos - desiredPos);
			}
		}
	}

	@Override
	public void performValidation() {
		// Do nothing. Chips handles its own validation.
	}

	private void shrink() {
		if (mTokenizer == null) {
			return;
		}
		long contactId = mSelectedChip != null ? mSelectedChip.getEntry()
				.getContactId() : -1;
		if (mSelectedChip != null
				&& contactId != RecipientEntry.INVALID_CONTACT
				&& (!isPhoneQuery() && contactId != RecipientEntry.GENERATED_CONTACT)) {
			clearSelectedChip();
		} else {
			if (getWidth() <= 0) {
				// We don't have the width yet which means the view hasn't been
				// drawn yet
				// and there is no reason to attempt to commit chips yet.
				// This focus lost must be the result of an orientation change
				// or an initial rendering.
				// Re-post the shrink for later.
				mHandler.removeCallbacks(mDelayedShrink);
				mHandler.post(mDelayedShrink);
				return;
			}
			// Reset any pending chips as they would have been handled
			// when the field lost focus.
			if (mPendingChipsCount > 0) {
				postHandlePendingChips();
			} else {
				Editable editable = getText();
				int end = getSelectionEnd();
				int start = mTokenizer.findTokenStart(editable, end);
				RecipientChip[] chips = getSpannable().getSpans(start, end,
						RecipientChip.class);
				if ((chips == null || chips.length == 0)) {
					Editable text = getText();
					int whatEnd = mTokenizer.findTokenEnd(text, start);
					// This token was already tokenized, so skip past the ending
					// token.
					if (whatEnd < text.length() && text.charAt(whatEnd) == ',') {
						whatEnd = movePastTerminators(whatEnd);
					}
					// In the middle of chip; treat this as an edit
					// and commit the whole token.
					int selEnd = getSelectionEnd();
					if (whatEnd != selEnd) {
						handleEdit(start, whatEnd);
					} else {
						commitChip(start, end, editable);
					}
				}
			}
			mHandler.post(mAddTextWatcher);
		}
		createMoreChip();
	}

	private void expand() {
		if (mShouldShrink) {
			setMaxLines(Integer.MAX_VALUE);
		}
		removeMoreChip();
		setCursorVisible(true);
		Editable text = getText();
		setSelection(text != null && text.length() > 0 ? text.length() : 0);
		// If there are any temporary chips, try replacing them now that the
		// user
		// has expanded the field.
	}

	private CharSequence ellipsizeText(CharSequence text, TextPaint paint,
			float maxWidth) {
		paint.setTextSize(mChipFontSize);
		if (maxWidth <= 0 && Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "Max width is negative: " + maxWidth);
		}
		return TextUtils.ellipsize(text, paint, maxWidth,
				TextUtils.TruncateAt.END);
	}

	private Bitmap createSelectedChip(RecipientEntry contact, TextPaint paint,
			Layout layout) {
		// Ellipsize the text so that it takes AT MOST the entire width of the
		// autocomplete text entry area. Make sure to leave space for padding
		// on the sides.
		int height = (int) mChipHeight;
		int deleteWidth = height;
		float[] widths = new float[1];
		paint.getTextWidths(" ", widths);
		CharSequence ellipsizedText = ellipsizeText(
				createChipDisplayText(contact), paint,
				calculateAvailableWidth(true) - deleteWidth - widths[0]);

		// Make sure there is a minimum chip width so the user can ALWAYS
		// tap a chip without difficulty.
		int width = Math.max(
				deleteWidth * 2,
				(int) Math.floor(paint.measureText(ellipsizedText, 0,
						ellipsizedText.length()))
						+ (mChipPadding * 2)
						+ deleteWidth);

		// Create the background of the chip.
		Bitmap tmpBitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(tmpBitmap);
		if (mChipBackgroundPressed != null) {
			mChipBackgroundPressed.setBounds(0, 0, width, height);
			mChipBackgroundPressed.draw(canvas);
			paint.setColor(sSelectedTextColor);
			// Vertically center the text in the chip.
			canvas.drawText(ellipsizedText, 0, ellipsizedText.length(),
					mChipPadding,
					getTextYOffset((String) ellipsizedText, paint, height),
					paint);
			// Make the delete a square.
			Rect backgroundPadding = new Rect();
			mChipBackgroundPressed.getPadding(backgroundPadding);
			mChipDelete.setBounds(width - deleteWidth + backgroundPadding.left,
					0 + backgroundPadding.top, width - backgroundPadding.right,
					height - backgroundPadding.bottom);
			mChipDelete.draw(canvas);
		} else {
			Log.w(TAG,
					"Unable to draw a background for the chips as it was never set");
		}
		return tmpBitmap;
	}

	private Bitmap createUnselectedChip(RecipientEntry contact,
			TextPaint paint, Layout layout, boolean leaveBlankIconSpacer) {
		// Ellipsize the text so that it takes AT MOST the entire width of the
		// autocomplete text entry area. Make sure to leave space for padding
		// on the sides.
		int height = (int) mChipHeight;
		int iconWidth = height;
		float[] widths = new float[1];
		paint.getTextWidths(" ", widths);
		CharSequence ellipsizedText = ellipsizeText(
				createChipDisplayText(contact), paint,
				calculateAvailableWidth(false) - iconWidth - widths[0]);
		// Make sure there is a minimum chip width so the user can ALWAYS
		// tap a chip without difficulty.
		int width = Math.max(
				iconWidth * 2,
				(int) Math.floor(paint.measureText(ellipsizedText, 0,
						ellipsizedText.length()))
						+ (mChipPadding * 2)
						+ iconWidth);

		// Create the background of the chip.
		Bitmap tmpBitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(tmpBitmap);
		Drawable background = getChipBackground(contact);
		if (background != null) {
			background.setBounds(0, 0, width, height);
			background.draw(canvas);

			// Don't draw photos for recipients that have been typed in OR
			// generated on the fly.
			long contactId = contact.getContactId();
			boolean drawPhotos = isPhoneQuery() ? contactId != RecipientEntry.INVALID_CONTACT
					: (contactId != RecipientEntry.INVALID_CONTACT && (contactId != RecipientEntry.GENERATED_CONTACT && !TextUtils
							.isEmpty(contact.getDisplayName())));
			if (drawPhotos) {
				byte[] photoBytes = contact.getPhotoBytes();
				// There may not be a photo yet if anything but the first
				// contact address
				// was selected.
				if (photoBytes == null
						&& contact.getPhotoThumbnailUri() != null) {
					// TODO: cache this in the recipient entry?
					
				}

				Bitmap photo;
				if (photoBytes != null) {
					photo = BitmapFactory.decodeByteArray(photoBytes, 0,
							photoBytes.length);
				} else {
					// TODO: can the scaled down default photo be cached?
					photo = mDefaultContactPhoto;
				}
				// Draw the photo on the left side.
				if (photo != null) {
					RectF src = new RectF(0, 0, photo.getWidth(),
							photo.getHeight());
					Rect backgroundPadding = new Rect();
					mChipBackground.getPadding(backgroundPadding);
					RectF dst = new RectF(width - iconWidth
							+ backgroundPadding.left,
							0 + backgroundPadding.top, width
									- backgroundPadding.right, height
									- backgroundPadding.bottom);
					Matrix matrix = new Matrix();
					matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
					canvas.drawBitmap(photo, matrix, paint);
				}
			} else if (!leaveBlankIconSpacer || isPhoneQuery()) {
				iconWidth = 0;
			}
			paint.setColor(getContext().getResources().getColor(
					android.R.color.black));
			// Vertically center the text in the chip.
			canvas.drawText(ellipsizedText, 0, ellipsizedText.length(),
					mChipPadding,
					getTextYOffset((String) ellipsizedText, paint, height),
					paint);
		} else {
			Log.w(TAG,
					"Unable to draw a background for the chips as it was never set");
		}
		return tmpBitmap;
	}

	/**
	 * Get the background drawable for a RecipientChip.
	 */
	// Visible for testing.
	/* package */Drawable getChipBackground(RecipientEntry contact) {
		return (mValidator != null && mValidator.isValid(contact
				.getDestination())) ? mChipBackground : mInvalidChipBackground;
	}

	private float getTextYOffset(String text, TextPaint paint, int height) {
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		int textHeight = bounds.bottom - bounds.top;
		return height - ((height - textHeight) / 2) - (int) paint.descent();
	}

	private RecipientChip constructChipSpan(RecipientEntry contact, int offset,
			boolean pressed, boolean leaveIconSpace)
			throws NullPointerException {
		if (mChipBackground == null) {
			throw new NullPointerException(
					"Unable to render any chips as setChipDimensions was not called.");
		}
		Layout layout = getLayout();

		TextPaint paint = getPaint();
		float defaultSize = paint.getTextSize();
		int defaultColor = paint.getColor();

		Bitmap tmpBitmap;
		if (pressed) {
			tmpBitmap = createSelectedChip(contact, paint, layout);

		} else {
			tmpBitmap = createUnselectedChip(contact, paint, layout,
					leaveIconSpace);
		}

		// Pass the full text, un-ellipsized, to the chip.
		Drawable result = new BitmapDrawable(getResources(), tmpBitmap);
		result.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
		RecipientChip recipientChip = new RecipientChip(result, contact, offset);
		// Return text to the original size.
		paint.setTextSize(defaultSize);
		paint.setColor(defaultColor);
		return recipientChip;
	}

	/**
	 * Calculate the bottom of the line the chip will be located on using: 1)
	 * which line the chip appears on 2) the height of a chip 3) padding built
	 * into the edit text view
	 */
	private int calculateOffsetFromBottom(int line) {
		// Line offsets start at zero.
		int actualLine = getLineCount() - (line + 1);
		return -((actualLine * ((int) mChipHeight) + getPaddingBottom()) + getPaddingTop())
				+ getDropDownVerticalOffset();
	}

	/**
	 * Get the max amount of space a chip can take up. The formula takes into
	 * account the width of the EditTextView, any view padding, and padding that
	 * will be added to the chip.
	 */
	private float calculateAvailableWidth(boolean pressed) {
		return getWidth() - getPaddingLeft() - getPaddingRight()
				- (mChipPadding * 2);
	}

	private void setChipDimensions(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.RecipientEditTextView, 0, 0);
		Resources r = getContext().getResources();
		mChipBackground = a
				.getDrawable(R.styleable.RecipientEditTextView_chipBackground);
		if (mChipBackground == null) {
			mChipBackground = r
					.getDrawable(R.drawable.recipient_chip_background);
		}
		mChipBackgroundPressed = a
				.getDrawable(R.styleable.RecipientEditTextView_chipBackgroundPressed);
		if (mChipBackgroundPressed == null) {
			mChipBackgroundPressed = r
					.getDrawable(R.drawable.recipient_chip_background_selected);
		}
		mChipDelete = a
				.getDrawable(R.styleable.RecipientEditTextView_chipDelete);
		if (mChipDelete == null) {
			mChipDelete = r.getDrawable(R.drawable.recipient_chip_delete);
		}
		mChipPadding = a.getDimensionPixelSize(
				R.styleable.RecipientEditTextView_chipPadding, -1);
		if (mChipPadding == -1) {
			mChipPadding = (int) r.getDimension(R.dimen.chip_padding);
		}
		mAlternatesLayout = a.getResourceId(
				R.styleable.RecipientEditTextView_chipAlternatesLayout, -1);
		if (mAlternatesLayout == -1) {
			// mAlternatesLayout = R.layout.recipient_chips_alternate_item;
		}

		mDefaultContactPhoto = BitmapFactory.decodeResource(r,
				R.drawable.recipient_chip_ic_contact_picture);

		mChipHeight = a.getDimensionPixelSize(
				R.styleable.RecipientEditTextView_chipHeight, -1);
		if (mChipHeight == -1) {
			mChipHeight = r.getDimension(R.dimen.chip_height);
		}
		mChipFontSize = a.getDimensionPixelSize(
				R.styleable.RecipientEditTextView_chipFontSize, -1);
		if (mChipFontSize == -1) {
			mChipFontSize = r.getDimension(R.dimen.chip_text_size);
		}
		mInvalidChipBackground = a
				.getDrawable(R.styleable.RecipientEditTextView_invalidChipBackground);
		if (mInvalidChipBackground == null) {
			mInvalidChipBackground = r
					.getDrawable(R.drawable.recipient_chip_background_invalid);
		}
		mLineSpacingExtra = context.getResources().getDimension(
				R.dimen.line_spacing_extra);
		TypedValue tv = new TypedValue();
		
		a.recycle();
	}

	// Visible for testing.
	/* package */void setMoreItem(TextView moreItem) {
		mMoreItem = moreItem;
	}

	// Visible for testing.
	/* package */void setChipBackground(Drawable chipBackground) {
		mChipBackground = chipBackground;
	}

	// Visible for testing.
	/* package */void setChipHeight(int height) {
		mChipHeight = height;
	}

	/**
	 * Set whether to shrink the recipients field such that at most one line of
	 * recipients chips are shown when the field loses focus. By default, the
	 * number of displayed recipients will be limited and a "more" chip will be
	 * shown when focus is lost.
	 * 
	 * @param shrink
	 */
	public void setOnFocusListShrinkRecipients(boolean shrink) {
		mShouldShrink = shrink;
	}

	@Override
	public void onSizeChanged(int width, int height, int oldw, int oldh) {
		super.onSizeChanged(width, height, oldw, oldh);
		if (width != 0 && height != 0) {
			if (mPendingChipsCount > 0) {
				postHandlePendingChips();
			} else {
				checkChipWidths();
			}
		}
		// Try to find the scroll view parent, if it exists.
		if (mScrollView == null && !mTriedGettingScrollView) {
			ViewParent parent = getParent();
			while (parent != null && !(parent instanceof ScrollView)) {
				parent = parent.getParent();
			}
			if (parent != null) {
				mScrollView = (ScrollView) parent;
			}
			mTriedGettingScrollView = true;
		}
	}

	private void postHandlePendingChips() {
		mHandler.removeCallbacks(mHandlePendingChips);
		mHandler.post(mHandlePendingChips);
	}

	private void checkChipWidths() {
		// Check the widths of the associated chips.
		RecipientChip[] chips = getSortedRecipients();
		if (chips != null) {
			Rect bounds;
			for (RecipientChip chip : chips) {
				bounds = chip.getDrawable().getBounds();
				if (getWidth() > 0 && bounds.right - bounds.left > getWidth()) {
					// Need to redraw that chip.
					replaceChip(chip, chip.getEntry());
				}
			}
		}
	}

	// Visible for testing.
	/* package */void handlePendingChips() {
		if (getViewWidth() <= 0) {
			// The widget has not been sized yet.
			// This will be called as a result of onSizeChanged
			// at a later point.
			return;
		}
		if (mPendingChipsCount <= 0) {
			return;
		}

		synchronized (mPendingChips) {
			Editable editable = getText();
			// Tokenize!
			if (mPendingChipsCount <= MAX_CHIPS_PARSED) {
				for (int i = 0; i < mPendingChips.size(); i++) {
					String current = mPendingChips.get(i);
					int tokenStart = editable.toString().indexOf(current);
					int tokenEnd  = tokenStart + current.length() + 1;
					
					if (tokenStart >= 0) {
						// When we have a valid token, include it with the token
						// to the left.
						if (tokenEnd < editable.length() - 2
								&& editable.charAt(tokenEnd) == COMMIT_CHAR_COMMA) {
							tokenEnd++;
						}
						createReplacementChip(tokenStart, tokenEnd, editable);
					}
					mPendingChipsCount--;
				}
				sanitizeEnd();
			} else {
				mNoChips = true;
			}

			mPendingChipsCount = 0;
			mPendingChips.clear();
		}
	}

	// Visible for testing.
	/* package */int getViewWidth() {
		return getWidth();
	}

	/**
	 * Remove any characters after the last valid chip.
	 */
	// Visible for testing.
	/* package */void sanitizeEnd() {
		// Don't sanitize while we are waiting for pending chips to complete.
		if (mPendingChipsCount > 0) {
			return;
		}
		// Find the last chip; eliminate any commit characters after it.
		RecipientChip[] chips = getSortedRecipients();
		if (chips != null && chips.length > 0) {
			int end;
			ImageSpan lastSpan;
			mMoreChip = getMoreChip();
			if (mMoreChip != null) {
				lastSpan = mMoreChip;
			} else {
				lastSpan = getLastChip();
			}
			end = getSpannable().getSpanEnd(lastSpan);
			Editable editable = getText();
			int length = editable.length();
			if (length > end) {
				// See what characters occur after that and eliminate them.
				if (Log.isLoggable(TAG, Log.DEBUG)) {
					Log.d(TAG,
							"There were extra characters after the last tokenizable entry."
									+ editable);
				}
				editable.delete(end + 1, length);
			}
		}
	}

	/**
	 * Create a chip that represents just the email address of a recipient. At
	 * some later point, this chip will be attached to a real contact entry, if
	 * one exists.
	 */
	private void createReplacementChip(int tokenStart, int tokenEnd,
			Editable editable) {
		if (alreadyHasChip(tokenStart, tokenEnd)) {
			// There is already a chip present at this location.
			// Don't recreate it.
			return;
		}
		String token = editable.toString().substring(tokenStart, tokenEnd);
		int commitCharIndex = token.trim().lastIndexOf(COMMIT_CHAR_COMMA);
		if (commitCharIndex == token.length() - 1) {
			token = token.substring(0, token.length() - 1);
		}
		RecipientEntry entry = createTokenizedEntry(token);
		if (entry != null) {
			String destText = createAddressText(entry);
			SpannableString chipText = new SpannableString(destText);
			int end = getSelectionEnd();
			int start = mTokenizer != null ? mTokenizer.findTokenStart(
					getText(), end) : 0;
			RecipientChip chip = null;
			try {
				if (!mNoChips) {
					/*
					 * leave space for the contact icon if this is not just an
					 * email address
					 */
					chip = constructChipSpan(
							entry,
							start,
							false,
							TextUtils.isEmpty(entry.getDisplayName())
									|| TextUtils.equals(entry.getDisplayName(),
											entry.getDestination()));
				}
			} catch (NullPointerException e) {
				Log.e(TAG, e.getMessage(), e);
			}
			editable.setSpan(chip, tokenStart, tokenEnd,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			// Add this chip to the list of entries "to replace"
			if (chip != null) {
				if (mTemporaryRecipients == null) {
					mTemporaryRecipients = new ArrayList<RecipientChip>();
				}
				chip.setOriginalText(chipText.toString());
				mTemporaryRecipients.add(chip);
			}
		}
	}

	private static boolean isPhoneNumber(String number) {
		// TODO: replace this function with libphonenumber's isPossibleNumber
		// (see
		// PhoneNumberUtil). One complication is that it requires the sender's
		// region which
		// comes from the CurrentCountryIso. For now, let's just do this simple
		// match.
		if (TextUtils.isEmpty(number)) {
			return false;
		}

		Matcher match = PHONE_PATTERN.matcher(number);
		return match.matches();
	}

	private RecipientEntry createTokenizedEntry(String token) {
		if (TextUtils.isEmpty(token)) {
			return null;
		}
		if (isPhoneQuery() && isPhoneNumber(token)) {
			return RecipientEntry.constructFakeEntry(token);
		}
		Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(token);
		String display = null;
		if (isValid(token) && tokens != null && tokens.length > 0) {
			// If we can get a name from tokenizing, then generate an entry from
			// this.
			display = tokens[0].getName();
			if (!TextUtils.isEmpty(display)) {
				if (!isPhoneQuery()) {
					if (!TextUtils.isEmpty(token)) {
						token = token.trim();
					}
					char charAt = token.charAt(token.length() - 1);
					if (charAt == COMMIT_CHAR_COMMA
							|| charAt == COMMIT_CHAR_SEMICOLON) {
						token = token.substring(0, token.length() - 1);
					}
				}
				return RecipientEntry.constructGeneratedEntry(display, token);
			} else {
				display = tokens[0].getAddress();
				if (!TextUtils.isEmpty(display)) {
					return RecipientEntry.constructFakeEntry(display);
				}
			}
		}
		// Unable to validate the token or to create a valid token from it.
		// Just create a chip the user can edit.
		String validatedToken = null;
		if (mValidator != null && !mValidator.isValid(token)) {
			// Try fixing up the entry using the validator.
			validatedToken = mValidator.fixText(token).toString();
			if (!TextUtils.isEmpty(validatedToken)) {
				if (validatedToken.contains(token)) {
					// protect against the case of a validator with a null
					// domain,
					// which doesn't add a domain to the token
					Rfc822Token[] tokenized = Rfc822Tokenizer
							.tokenize(validatedToken);
					if (tokenized.length > 0) {
						validatedToken = tokenized[0].getAddress();
					}
				} else {
					// We ran into a case where the token was invalid and
					// removed
					// by the validator. In this case, just use the original
					// token
					// and let the user sort out the error chip.
					validatedToken = null;
				}
			}
		}
		// Otherwise, fallback to just creating an editable email address chip.
		return RecipientEntry.constructFakeEntry(!TextUtils
				.isEmpty(validatedToken) ? validatedToken : token);
	}

	private boolean isValid(String text) {
		return mValidator == null ? true : mValidator.isValid(text);
	}

	private String tokenizeAddress(String destination) {
		Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(destination);
		if (tokens != null && tokens.length > 0) {
			return tokens[0].getAddress();
		}
		return destination;
	}

	@Override
	public void setTokenizer(Tokenizer tokenizer) {
		mTokenizer = tokenizer;
		super.setTokenizer(mTokenizer);
	}

	@Override
	public void setValidator(Validator validator) {
		mValidator = validator;
		super.setValidator(validator);
	}

	/**
	 * We cannot use the default mechanism for replaceText. Instead, we override
	 * onItemClickListener so we can get all the associated contact information
	 * including display text, address, and id.
	 */
	@Override
	protected void replaceText(CharSequence text) {
		return;
	}

	/**
	 * Dismiss any selected chips when the back key is pressed.
	 */
	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mSelectedChip != null) {
			//System.out.println("onKeyPreIme");
			clearSelectedChip();
			return true;
		}
		return super.onKeyPreIme(keyCode, event);
	}

	/**
	 * Monitor key presses in this view to see if the user types any commit
	 * keys, which consist of ENTER, TAB, or DPAD_CENTER. If the user has
	 * entered text that has contact matches and types a commit key, create a
	 * chip from the topmost matching contact. If the user has entered text that
	 * has no contact matches and types a commit key, then create a chip from
	 * the text they have entered.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			/*
			 * if (event.hasNoModifiers()) { if (commitDefault()) { return true;
			 * } if (mSelectedChip != null) { clearSelectedChip(); return true;
			 * } else if (focusNext()) { return true; } }
			 */
			break;
		case KeyEvent.KEYCODE_TAB:
			/*
			 * if (event.hasNoModifiers()) { if (mSelectedChip != null) {
			 * clearSelectedChip(); } else { commitDefault(); } if (focusNext())
			 * { return true; } }
			 */
			break;
		}
		return super.onKeyUp(keyCode, event);
	}

	private boolean focusNext() {
		View next = focusSearch(View.FOCUS_DOWN);
		if (next != null) {
			next.requestFocus();
			return true;
		}
		return false;
	}

	/**
	 * Create a chip from the default selection. If the popup is showing, the
	 * default is the first item in the popup suggestions list. Otherwise, it is
	 * whatever the user had typed in. End represents where the the tokenizer
	 * should search for a token to turn into a chip.
	 * 
	 * @return If a chip was created from a real contact.
	 */
	private boolean commitDefault() {
		// If there is no tokenizer, don't try to commit.
		if (mTokenizer == null) {
			return false;
		}
		Editable editable = getText();
		int end = getSelectionEnd();
		int start = mTokenizer.findTokenStart(editable, end);

		if (shouldCreateChip(start, end)) {
			int whatEnd = mTokenizer.findTokenEnd(getText(), start);
			// In the middle of chip; treat this as an edit
			// and commit the whole token.
			whatEnd = movePastTerminators(whatEnd);
			if (whatEnd != getSelectionEnd()) {
				handleEdit(start, whatEnd);
				return true;
			}
			return commitChip(start, end, editable);
		}
		return false;
	}

	private void commitByCharacter() {
		// We can't possibly commit by character if we can't tokenize.
		if (mTokenizer == null) {
			return;
		}
		Editable editable = getText();
		int end = getSelectionEnd();
		int start = mTokenizer.findTokenStart(editable, end);
		if (shouldCreateChip(start, end)) {
			commitChip(start, end, editable);
		}
		setSelection(getText().length());
	}

	private boolean commitChip(int start, int end, Editable editable) {
		ListAdapter adapter = getAdapter();
		if (adapter != null && adapter.getCount() > 0 && enoughToFilter()
				&& end == getSelectionEnd() && !isPhoneQuery()) {
			// choose the first entry.
			submitItemAtPosition(0);
			dismissDropDown();
			return true;
		} else {
			int tokenEnd = mTokenizer.findTokenEnd(editable, start);
			if (editable.length() > tokenEnd + 1) {
				char charAt = editable.charAt(tokenEnd + 1);
				if (charAt == COMMIT_CHAR_COMMA
						|| charAt == COMMIT_CHAR_SEMICOLON) {
					tokenEnd++;
				}
			}
			String text = editable.toString().substring(start, tokenEnd).trim();
			clearComposingText();
			if (text != null && text.length() > 0 && !text.equals(" ")) {
				RecipientEntry entry = createTokenizedEntry(text);
				if (entry != null) {
					QwertyKeyListener.markAsReplaced(editable, start, end, "");
					CharSequence chipText = createChip(entry, false);
					if (chipText != null && start > -1 && end > -1) {
						editable.replace(start, end, chipText);
					}
				}
				// Only dismiss the dropdown if it is related to the text we
				// just committed.
				// For paste, it may not be as there are possibly multiple
				// tokens being added.
				if (end == getSelectionEnd()) {
					dismissDropDown();
				}
				sanitizeBetween();
				return true;
			}
		}
		return false;
	}

	// Visible for testing.
	/* package */void sanitizeBetween() {
		// Don't sanitize while we are waiting for content to chipify.
		if (mPendingChipsCount > 0) {
			return;
		}
		// Find the last chip.
		RecipientChip[] recips = getSortedRecipients();
		if (recips != null && recips.length > 0) {
			RecipientChip last = recips[recips.length - 1];
			RecipientChip beforeLast = null;
			if (recips.length > 1) {
				beforeLast = recips[recips.length - 2];
			}
			int startLooking = 0;
			int end = getSpannable().getSpanStart(last);
			if (beforeLast != null) {
				startLooking = getSpannable().getSpanEnd(beforeLast);
				Editable text = getText();
				if (startLooking == -1 || startLooking > text.length() - 1) {
					// There is nothing after this chip.
					return;
				}
				if (text.charAt(startLooking) == ' ') {
					startLooking++;
				}
			}
			if (startLooking >= 0 && end >= 0 && startLooking < end) {
				getText().delete(startLooking, end);
			}
		}
	}

	private boolean shouldCreateChip(int start, int end) {
		return !mNoChips && hasFocus() && enoughToFilter()
				&& !alreadyHasChip(start, end);
	}

	private boolean alreadyHasChip(int start, int end) {
		if (mNoChips) {
			return true;
		}
		RecipientChip[] chips = getSpannable().getSpans(start, end,
				RecipientChip.class);
		if ((chips == null || chips.length == 0)) {
			return false;
		}
		return true;
	}

	private void handleEdit(int start, int end) {
		if (start == -1 || end == -1) {
			// This chip no longer exists in the field.
			dismissDropDown();
			return;
		}
		// This is in the middle of a chip, so select out the whole chip
		// and commit it.
		Editable editable = getText();
		setSelection(end);
		String text = getText().toString().substring(start, end);
		if (!TextUtils.isEmpty(text)) {
			RecipientEntry entry = RecipientEntry.constructFakeEntry(text);
			QwertyKeyListener.markAsReplaced(editable, start, end, "");
			CharSequence chipText = createChip(entry, false);
			int selEnd = getSelectionEnd();
			if (chipText != null && start > -1 && selEnd > -1) {
				editable.replace(start, selEnd, chipText);
			}
		}
		dismissDropDown();
	}

	/**
	 * If there is a selected chip, delegate the key events to the selected
	 * chip.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mSelectedChip != null && keyCode == KeyEvent.KEYCODE_DEL) {
			removeChip(mSelectedChip);
		}

		return super.onKeyDown(keyCode, event);
	}

	// Visible for testing.
	/* package */Spannable getSpannable() {
		return getText();
	}

	private int getChipStart(RecipientChip chip) {
		return getSpannable().getSpanStart(chip);
	}

	private int getChipEnd(RecipientChip chip) {
		return getSpannable().getSpanEnd(chip);
	}

	/**
	 * Instead of filtering on the entire contents of the edit box, this
	 * subclass method filters on the range from
	 * {@link Tokenizer#findTokenStart} to {@link #getSelectionEnd} if the
	 * length of that range meets or exceeds {@link #getThreshold} and makes
	 * sure that the range is not already a Chip.
	 */
	@Override
	protected void performFiltering(CharSequence text, int keyCode) {
		boolean isCompletedToken = isCompletedToken(text);
		if (enoughToFilter() && !isCompletedToken) {
			int end = getSelectionEnd();
			int start = mTokenizer.findTokenStart(text, end);
			// If this is a RecipientChip, don't filter
			// on its contents.
			Spannable span = getSpannable();
			RecipientChip[] chips = span.getSpans(start, end,
					RecipientChip.class);
			if (chips != null && chips.length > 0) {
				return;
			}
		} else if (isCompletedToken) {
			return;
		}
		super.performFiltering(text, keyCode);
	}

	// Visible for testing.
	/* package */boolean isCompletedToken(CharSequence text) {
		if (TextUtils.isEmpty(text)) {
			return false;
		}
		// Check to see if this is a completed token before filtering.
		int end = text.length();
		int start = mTokenizer.findTokenStart(text, end);
		String token = text.toString().substring(start, end).trim();
		if (!TextUtils.isEmpty(token)) {
			char atEnd = token.charAt(token.length() - 1);
			return atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON;
		}
		return false;
	}

	private void clearSelectedChip() {
		if (mSelectedChip != null) {
			unselectChip(mSelectedChip);
			mSelectedChip = null;
		}
		setCursorVisible(true);
	}

	/**
	 * Monitor touch events in the RecipientEditTextView. If the view does not
	 * have focus, any tap on the view will just focus the view. If the view has
	 * focus, determine if the touch target is a recipient chip. If it is and
	 * the chip is not selected, select it and clear any other selected chips.
	 * If it isn't, then select that chip.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isFocused()) {
			// Ignore any chip taps until this view is focused.
			return super.onTouchEvent(event);
		}
		boolean handled = super.onTouchEvent(event);
		int action = event.getAction();
		boolean chipWasSelected = false;
		if (mSelectedChip == null) {
			mGestureDetector.onTouchEvent(event);
		}
		
		if (action == MotionEvent.ACTION_UP && !chipWasSelected) {
			clearSelectedChip();
		}
		return handled;
	}

	private void scrollLineIntoView(int line) {
		if (mScrollView != null) {
			mScrollView.smoothScrollBy(0, calculateOffsetFromBottom(line));
		}
	}

	// TODO: This algorithm will need a lot of tweaking after more people have
	// used
	// the chips ui. This attempts to be "forgiving" to fat finger touches by
	// favoring
	// what comes before the finger.
	private int putOffsetInRange(int o) {
		int offset = o;
		Editable text = getText();
		int length = text.length();
		// Remove whitespace from end to find "real end"
		int realLength = length;
		for (int i = length - 1; i >= 0; i--) {
			if (text.charAt(i) == ' ') {
				realLength--;
			} else {
				break;
			}
		}

		// If the offset is beyond or at the end of the text,
		// leave it alone.
		if (offset >= realLength) {
			return offset;
		}
		Editable editable = getText();
		while (offset >= 0 && findText(editable, offset) == -1
				&& findChip(offset) == null) {
			// Keep walking backward!
			offset--;
		}
		return offset;
	}

	private int findText(Editable text, int offset) {
		if (text.charAt(offset) != ' ') {
			return offset;
		}
		return -1;
	}

	private RecipientChip findChip(int offset) {
		RecipientChip[] chips = getSpannable().getSpans(0, getText().length(),
				RecipientChip.class);
		// Find the chip that contains this offset.
		for (int i = 0; i < chips.length; i++) {
			RecipientChip chip = chips[i];
			int start = getChipStart(chip);
			int end = getChipEnd(chip);
			if (offset >= start && offset <= end) {
				return chip;
			}
		}
		return null;
	}

	// Visible for testing.
	// Use this method to generate text to add to the list of addresses.
	/* package */String createAddressText(RecipientEntry entry) {
		String display = entry.getDisplayName();
		String address = entry.getDestination();
		if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
			display = null;
		}
		String trimmedDisplayText;
		if (isPhoneQuery() && isPhoneNumber(address)) {
			trimmedDisplayText = address.trim();
		} else {
			if (address != null) {
				// Tokenize out the address in case the address already
				// contained the username as well.
				Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(address);
				if (tokenized != null && tokenized.length > 0) {
					address = tokenized[0].getAddress();
				}
			}
			Rfc822Token token = new Rfc822Token(display, address, null);
			trimmedDisplayText = token.toString().trim();
		}
		int index = trimmedDisplayText.indexOf(",");
		return mTokenizer != null && !TextUtils.isEmpty(trimmedDisplayText)
				&& index < trimmedDisplayText.length() - 1 ? (String) mTokenizer
				.terminateToken(trimmedDisplayText) : trimmedDisplayText;
	
	}

	// Visible for testing.
	// Use this method to generate text to display in a chip.
	/* package */String createChipDisplayText(RecipientEntry entry) {
		String display = entry.getDisplayName();
		String address = entry.getDestination();
		if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
			display = null;
		}
		if (address != null && !(isPhoneQuery() && isPhoneNumber(address))) {
			// Tokenize out the address in case the address already
			// contained the username as well.
			Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(address);
			if (tokenized != null && tokenized.length > 0) {
				address = tokenized[0].getAddress();
			}
		}
		return new Rfc822Token(display, address, null).toString();
	}

	private CharSequence createChip(RecipientEntry entry, boolean pressed) {
		String displayText = createAddressText(entry);
		if (TextUtils.isEmpty(displayText)) {
			return null;
		}
		SpannableString chipText = null;
		// Always leave a blank space at the end of a chip.
		int end = getSelectionEnd();
		int start = mTokenizer.findTokenStart(getText(), end);
		int textLength = displayText.length() - 1;
		chipText = new SpannableString(displayText);
		if (!mNoChips) {
			try {
				RecipientChip chip = constructChipSpan(entry, start, pressed,
						false /* leave space for contact icon */);
				chipText.setSpan(chip, 0, textLength,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				chip.setOriginalText(chipText.toString());
			} catch (NullPointerException e) {
				Log.e(TAG, e.getMessage(), e);
				return null;
			}
		}
		return chipText;
	}

	/**
	 * When an item in the suggestions list has been clicked, create a chip from
	 * the contact information of the selected item.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		submitItemAtPosition(position);
	}

	private void submitItemAtPosition(int position) {
		RecipientEntry entry = createValidatedEntry((RecipientEntry) getAdapter()
				.getItem(position));
		if (entry == null) {
			return;
		}
		clearComposingText();

		int end = getSelectionEnd();
		int start = mTokenizer.findTokenStart(getText(), end);

		Editable editable = getText();
		QwertyKeyListener.markAsReplaced(editable, start, end, "");
		CharSequence chip = createChip(entry, false);
		if (chip != null && start >= 0 && end >= 0) {
			editable.replace(start, end, chip);
		}
		sanitizeBetween();
	}

	private RecipientEntry createValidatedEntry(RecipientEntry item) {
		if (item == null) {
			return null;
		}
		final RecipientEntry entry;
		// If the display name and the address are the same, or if this is a
		// valid contact, but the destination is invalid, then make this a fake
		// recipient that is editable.
		String destination = item.getDestination();
		if (!isPhoneQuery()
				&& item.getContactId() == RecipientEntry.GENERATED_CONTACT) {
			entry = RecipientEntry.constructGeneratedEntry(
					item.getDisplayName(), destination);
		} else if (RecipientEntry.isCreatedRecipient(item.getContactId())
				&& (TextUtils.isEmpty(item.getDisplayName())
						|| TextUtils.equals(item.getDisplayName(), destination) || (mValidator != null && !mValidator
						.isValid(destination)))) {
			entry = RecipientEntry.constructFakeEntry(destination);
		} else {
			entry = item;
		}
		return entry;
	}

	/** Returns a collection of contact Id for each chip inside this View. */
	/* package */Collection<Long> getContactIds() {
		final Set<Long> result = new HashSet<Long>();
		RecipientChip[] chips = getSortedRecipients();
		if (chips != null) {
			for (RecipientChip chip : chips) {
				result.add(chip.getContactId());
			}
		}
		return result;
	}

	/**
	 * Returns a collection of data Id for each chip inside this View. May be
	 * null.
	 */
	/* package */Collection<Long> getDataIds() {
		final Set<Long> result = new HashSet<Long>();
		RecipientChip[] chips = getSortedRecipients();
		if (chips != null) {
			for (RecipientChip chip : chips) {
				result.add(chip.getDataId());
			}
		}
		return result;
	}

	// Visible for testing.
	/* package */RecipientChip[] getSortedRecipients() {
		RecipientChip[] recips = getSpannable().getSpans(0, getText().length(),
				RecipientChip.class);
		ArrayList<RecipientChip> recipientsList = new ArrayList<RecipientChip>(
				Arrays.asList(recips));
		final Spannable spannable = getSpannable();
		Collections.sort(recipientsList, new Comparator<RecipientChip>() {

			@Override
			public int compare(RecipientChip first, RecipientChip second) {
				int firstStart = spannable.getSpanStart(first);
				int secondStart = spannable.getSpanStart(second);
				if (firstStart < secondStart) {
					return -1;
				} else if (firstStart > secondStart) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return recipientsList.toArray(new RecipientChip[recipientsList.size()]);
	}

	// Visible for testing.
	/* package */ImageSpan getMoreChip() {
		MoreImageSpan[] moreSpans = getSpannable().getSpans(0,
				getText().length(), MoreImageSpan.class);
		return moreSpans != null && moreSpans.length > 0 ? moreSpans[0] : null;
	}

	private MoreImageSpan createMoreSpan(int count) {
		String moreText = String.format(mMoreItem.getText().toString(), count);
		TextPaint morePaint = new TextPaint(getPaint());
		morePaint.setTextSize(mMoreItem.getTextSize());
		morePaint.setColor(mMoreItem.getCurrentTextColor());
		int width = (int) morePaint.measureText(moreText)
				+ mMoreItem.getPaddingLeft() + mMoreItem.getPaddingRight();
		int height = getLineHeight();
		Bitmap drawable = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(drawable);
		int adjustedHeight = height;
		Layout layout = getLayout();
		if (layout != null) {
			adjustedHeight -= layout.getLineDescent(0);
		}
		canvas.drawText(moreText, 0, moreText.length(), 0, adjustedHeight,
				morePaint);

		Drawable result = new BitmapDrawable(getResources(), drawable);
		result.setBounds(0, 0, width, height);
		return new MoreImageSpan(result);
	}

	// Visible for testing.
	/* package */void createMoreChipPlainText() {
		// Take the first <= CHIP_LIMIT addresses and get to the end of the
		// second one.
		Editable text = getText();
		int start = 0;
		int end = start;
		for (int i = 0; i < CHIP_LIMIT; i++) {
			end = movePastTerminators(mTokenizer.findTokenEnd(text, start));
			start = end; // move to the next token and get its end.
		}
		// Now, count total addresses.
		start = 0;
		int tokenCount = countTokens(text);
		MoreImageSpan moreSpan = createMoreSpan(tokenCount - CHIP_LIMIT);
		SpannableString chipText = new SpannableString(text.subSequence(end,
				text.length()));
		chipText.setSpan(moreSpan, 0, chipText.length(),
				Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		text.replace(end, text.length(), chipText);
		mMoreChip = moreSpan;
	}

	// Visible for testing.
	/* package */int countTokens(Editable text) {
		int tokenCount = 0;
		int start = 0;
		while (start < text.length()) {
			start = movePastTerminators(mTokenizer.findTokenEnd(text, start));
			tokenCount++;
			if (start >= text.length()) {
				break;
			}
		}
		return tokenCount;
	}

	/**
	 * Create the more chip. The more chip is text that replaces any chips that
	 * do not fit in the pre-defined available space when the
	 * RecipientEditTextView loses focus.
	 */
	// Visible for testing.
	/* package */void createMoreChip() {
		if (mNoChips) {
			createMoreChipPlainText();
			return;
		}

		if (!mShouldShrink) {
			return;
		}
		ImageSpan[] tempMore = getSpannable().getSpans(0, getText().length(),
				MoreImageSpan.class);
		if (tempMore.length > 0) {
			getSpannable().removeSpan(tempMore[0]);
		}
		RecipientChip[] recipients = getSortedRecipients();

		if (recipients == null || recipients.length <= CHIP_LIMIT) {
			mMoreChip = null;
			return;
		}
		Spannable spannable = getSpannable();
		int numRecipients = recipients.length;
		int overage = numRecipients - CHIP_LIMIT;
		MoreImageSpan moreSpan = createMoreSpan(overage);
		mRemovedSpans = new ArrayList<RecipientChip>();
		int totalReplaceStart = 0;
		int totalReplaceEnd = 0;
		Editable text = getText();
		for (int i = numRecipients - overage; i < recipients.length; i++) {
			mRemovedSpans.add(recipients[i]);
			if (i == numRecipients - overage) {
				totalReplaceStart = spannable.getSpanStart(recipients[i]);
			}
			if (i == recipients.length - 1) {
				totalReplaceEnd = spannable.getSpanEnd(recipients[i]);
			}
			if (mTemporaryRecipients == null
					|| !mTemporaryRecipients.contains(recipients[i])) {
				int spanStart = spannable.getSpanStart(recipients[i]);
				int spanEnd = spannable.getSpanEnd(recipients[i]);
				recipients[i].setOriginalText(text.toString().substring(
						spanStart, spanEnd));
			}
			spannable.removeSpan(recipients[i]);
		}
		if (totalReplaceEnd < text.length()) {
			totalReplaceEnd = text.length();
		}
		int end = Math.max(totalReplaceStart, totalReplaceEnd);
		int start = Math.min(totalReplaceStart, totalReplaceEnd);
		SpannableString chipText = new SpannableString(text.subSequence(start,
				end));
		chipText.setSpan(moreSpan, 0, chipText.length(),
				Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		text.replace(start, end, chipText);
		mMoreChip = moreSpan;
		// If adding the +more chip goes over the limit, resize accordingly.
		if (!isPhoneQuery() && getLineCount() > mMaxLines) {
			setMaxLines(getLineCount());
		}
	}

	/**
	 * Replace the more chip, if it exists, with all of the recipient chips it
	 * had replaced when the RecipientEditTextView gains focus.
	 */
	// Visible for testing.
	/* package */void removeMoreChip() {
		if (mMoreChip != null) {
			Spannable span = getSpannable();
			span.removeSpan(mMoreChip);
			mMoreChip = null;
			// Re-add the spans that were removed.
			if (mRemovedSpans != null && mRemovedSpans.size() > 0) {
				// Recreate each removed span.
				RecipientChip[] recipients = getSortedRecipients();
				// Start the search for tokens after the last currently visible
				// chip.
				if (recipients == null || recipients.length == 0) {
					return;
				}
				int end = span.getSpanEnd(recipients[recipients.length - 1]);
				Editable editable = getText();
				for (RecipientChip chip : mRemovedSpans) {
					int chipStart;
					int chipEnd;
					String token;
					// Need to find the location of the chip, again.
					token = (String) chip.getOriginalText();
					// As we find the matching recipient for the remove spans,
					// reduce the size of the string we need to search.
					// That way, if there are duplicates, we always find the
					// correct
					// recipient.
					chipStart = editable.toString().indexOf(token, end);
					end = chipEnd = Math.min(editable.length(), chipStart
							+ token.length());
					// Only set the span if we found a matching token.
					if (chipStart != -1) {
						editable.setSpan(chip, chipStart, chipEnd,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
				mRemovedSpans.clear();
			}
		}
	}

	/**
	 * Show specified chip as selected. If the RecipientChip is just an email
	 * address, selecting the chip will take the contents of the chip and place
	 * it at the end of the RecipientEditTextView for inline editing. If the
	 * RecipientChip is a complete contact, then selecting the chip will change
	 * the background color of the chip, show the delete icon, and a popup
	 * window with the address in use highlighted and any other alternate
	 * addresses for the contact.
	 * 
	 * @param currentChip
	 *            Chip to select.
	 * @return A RecipientChip in the selected state or null if the chip just
	 *         contained an email address.
	 */
	private RecipientChip selectChip(RecipientChip currentChip) {
		if (shouldShowEditableText(currentChip)) {
			CharSequence text = currentChip.getValue();
			Editable editable = getText();
			Spannable spannable = getSpannable();
			int spanStart = spannable.getSpanStart(currentChip);
			int spanEnd = spannable.getSpanEnd(currentChip);
			spannable.removeSpan(currentChip);
			editable.delete(spanStart, spanEnd);
			setCursorVisible(true);
			setSelection(editable.length());
			editable.append(text);
			return constructChipSpan(
					RecipientEntry.constructFakeEntry((String) text),
					getSelectionStart(), true, false);
		} else if (currentChip.getContactId() == RecipientEntry.GENERATED_CONTACT) {
			int start = getChipStart(currentChip);
			int end = getChipEnd(currentChip);
			getSpannable().removeSpan(currentChip);
			RecipientChip newChip;
			try {
				if (mNoChips) {
					return null;
				}
				newChip = constructChipSpan(currentChip.getEntry(), start,
						true, false);
			} catch (NullPointerException e) {
				Log.e(TAG, e.getMessage(), e);
				return null;
			}
			Editable editable = getText();
			QwertyKeyListener.markAsReplaced(editable, start, end, "");
			if (start == -1 || end == -1) {
				Log.d(TAG,
						"The chip being selected no longer exists but should.");
			} else {
				editable.setSpan(newChip, start, end,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			newChip.setSelected(true);
			if (shouldShowEditableText(newChip)) {
				scrollLineIntoView(getLayout().getLineForOffset(
						getChipStart(newChip)));
			}
			
			setCursorVisible(false);
			return newChip;
		} else {
			int start = getChipStart(currentChip);
			int end = getChipEnd(currentChip);
			getSpannable().removeSpan(currentChip);
			RecipientChip newChip;
			try {
				newChip = constructChipSpan(currentChip.getEntry(), start,
						true, false);
			} catch (NullPointerException e) {
				Log.e(TAG, e.getMessage(), e);
				return null;
			}
			Editable editable = getText();
			QwertyKeyListener.markAsReplaced(editable, start, end, "");
			if (start == -1 || end == -1) {
				Log.d(TAG,
						"The chip being selected no longer exists but should.");
			} else {
				editable.setSpan(newChip, start, end,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			newChip.setSelected(true);
			if (shouldShowEditableText(newChip)) {
				scrollLineIntoView(getLayout().getLineForOffset(
						getChipStart(newChip)));
			}
			setCursorVisible(false);
			return newChip;
		}
	}

	private boolean shouldShowEditableText(RecipientChip currentChip) {
		long contactId = currentChip.getContactId();
		return contactId == RecipientEntry.INVALID_CONTACT
				|| (!isPhoneQuery() && contactId == RecipientEntry.GENERATED_CONTACT);
	}

	
	/**
	 * Remove selection from this chip. Unselecting a RecipientChip will render
	 * the chip without a delete icon and with an unfocused background. This is
	 * called when the RecipientChip no longer has focus.
	 */
	private void unselectChip(RecipientChip chip) {
		int start = getChipStart(chip);
		int end = getChipEnd(chip);
		Editable editable = getText();
		mSelectedChip = null;
		if (start == -1 || end == -1) {
			Log.w(TAG,
					"The chip doesn't exist or may be a chip a user was editing");
			setSelection(editable.length());
			commitDefault();
		} else {
			getSpannable().removeSpan(chip);
			QwertyKeyListener.markAsReplaced(editable, start, end, "");
			editable.removeSpan(chip);
			try {
				if (!mNoChips) {
					editable.setSpan(
							constructChipSpan(chip.getEntry(), start, false,
									false), start, end,
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			} catch (NullPointerException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		setCursorVisible(true);
		setSelection(editable.length());
		
	}

	/**
	 * Return whether a touch event was inside the delete target of a selected
	 * chip. It is in the delete target if: 1) the x and y points of the event
	 * are within the delete assset. 2) the point tapped would have caused a
	 * cursor to appear right after the selected chip.
	 * 
	 * @return boolean
	 */
	private boolean isInDelete(RecipientChip chip, int offset, float x, float y) {
		// Figure out the bounds of this chip and whether or not
		// the user clicked in the X portion.
		return chip.isSelected() && offset == getChipEnd(chip);
	}

	/**
	 * Remove the chip and any text associated with it from the
	 * RecipientEditTextView.
	 */
	// Visible for testing.
	/* pacakge */void removeChip(RecipientChip chip) {
		Spannable spannable = getSpannable();
		int spanStart = spannable.getSpanStart(chip);
		int spanEnd = spannable.getSpanEnd(chip);
		Editable text = getText();
		int toDelete = spanEnd;
		boolean wasSelected = chip == mSelectedChip;
		// Clear that there is a selected chip before updating any text.
		if (wasSelected) {
			mSelectedChip = null;
		}
		// Always remove trailing spaces when removing a chip.
		while (toDelete >= 0 && toDelete < text.length()
				&& text.charAt(toDelete) == ' ') {
			toDelete++;
		}
		spannable.removeSpan(chip);
		if (spanStart >= 0 && toDelete > 0) {
			text.delete(spanStart, toDelete);
		}
		if (wasSelected) {
			clearSelectedChip();
		}
	}

	/**
	 * Replace this currently selected chip with a new chip that uses the
	 * contact data provided.
	 */
	// Visible for testing.
	/* package */void replaceChip(RecipientChip chip, RecipientEntry entry) {
		boolean wasSelected = chip == mSelectedChip;
		if (wasSelected) {
			mSelectedChip = null;
		}
		int start = getChipStart(chip);
		int end = getChipEnd(chip);
		getSpannable().removeSpan(chip);
		Editable editable = getText();
		CharSequence chipText = createChip(entry, false);
		if (chipText != null) {
			if (start == -1 || end == -1) {
				Log.e(TAG, "The chip to replace does not exist but should.");
				editable.insert(0, chipText);
			} else {
				if (!TextUtils.isEmpty(chipText)) {
					// There may be a space to replace with this chip's new
					// associated space. Check for it
					int toReplace = end;
					while (toReplace >= 0 && toReplace < editable.length()
							&& editable.charAt(toReplace) == ' ') {
						toReplace++;
					}
					editable.replace(start, toReplace, chipText);
				}
			}
		}
		setCursorVisible(true);
		if (wasSelected) {
			clearSelectedChip();
		}
	}

	/**
	 * Handle click events for a chip. When a selected chip receives a click
	 * event, see if that event was in the delete icon. If so, delete it.
	 * Otherwise, unselect the chip.
	 */
	public void onClick(RecipientChip chip, int offset, float x, float y) {
		if (chip.isSelected()) {
			if (isInDelete(chip, offset, x, y)) {
				removeChip(chip);
			} else {
				clearSelectedChip();
			}
		}
	}

	private boolean chipsPending() {
		return mPendingChipsCount > 0
				|| (mRemovedSpans != null && mRemovedSpans.size() > 0);
	}

	@Override
	public void removeTextChangedListener(TextWatcher watcher) {
		mTextWatcher = null;
		super.removeTextChangedListener(watcher);
	}

	private class RecipientTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			// If the text has been set to null or empty, make sure we remove
			// all the spans we applied.
			if (TextUtils.isEmpty(s)) {
				// Remove all the chips spans.
				Spannable spannable = getSpannable();
				RecipientChip[] chips = spannable.getSpans(0, getText()
						.length(), RecipientChip.class);
				for (RecipientChip chip : chips) {
					spannable.removeSpan(chip);
				}
				if (mMoreChip != null) {
					spannable.removeSpan(mMoreChip);
				}
				return;
			}
			// Get whether there are any recipients pending addition to the
			// view. If there are, don't do anything in the text watcher.
			if (chipsPending()) {
				return;
			}
			// If the user is editing a chip, don't clear it.
			if (mSelectedChip != null && shouldShowEditableText(mSelectedChip)) {
				setCursorVisible(true);
				setSelection(getText().length());
				clearSelectedChip();
			}
			
			if(isRecipientLimitExceeded()){
				
				if (mTextWatcher != null) {
					removeTextChangedListener(mTextWatcher);
				}
				
				int end = s.length();
				int start = end - 1;
				getText().replace(start, end, "").toString();
				editorListener.onRecipientLimitExceeded();
				
				mHandler.post(mAddTextWatcher);
				return;
			}
			
			int length = s.length();
			// Make sure there is content there to parse and that it is
			// not just the commit character.
			if (length > 0) {
				char last;
				int end = getSelectionEnd() == 0 ? 0 : getSelectionEnd() - 1;
				int len = length() - 1;
				if (end != len) {
					last = s.charAt(end);
				} else {
					last = s.charAt(len);
				}
				if (last == COMMIT_CHAR_COMMA) {			
					String text = getText().toString();
					int tokenStart = mTokenizer.findTokenStart(text,
							getSelectionEnd());
					String sub = text.substring(tokenStart,
							mTokenizer.findTokenEnd(text, tokenStart)).trim();
					if (!isValidEmail(tokenizeAddress(sub))) {
						editorListener.onInvalidRecipientEntered();
						if(sub.trim().equalsIgnoreCase("")){
							if (mTextWatcher != null) {
								removeTextChangedListener(mTextWatcher);
							}
							
							int text_end = getSelectionEnd();
							int text_len = length() - 1;
							
							int start_pos = getLastChipEnd();
							if(start_pos == -1){
								start_pos = 0;
							}
							if(text_end != text_len){
								getText().replace(start_pos, text_end, "").toString();
							}
							else{
								getText().replace(start_pos, text_len, "").toString();
							}
							mHandler.post(mAddTextWatcher);
						}
					} else {
						if(!doesThisRecipientAlreadyExists(sub)){
							commitByCharacter();
							scrollBottomIntoView();
						}else{
							editorListener.onRecipientAlreadyExist();
						}
						
					}
				} 
			}
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// This is a delete; check to see if the insertion point is on a
			// space
			// following a chip.
			
			if (before - count == 1) {
				// If the item deleted is a space, and the thing before the
				// space is a chip, delete the entire span.
				int selStart = getSelectionStart();
				RecipientChip[] repl = getSpannable().getSpans(selStart,
						selStart, RecipientChip.class);
				if (repl.length > 0) {
					// There is a chip there! Just remove it.
					Editable editable = getText();
					// Add the separator token.
					int tokenStart = mTokenizer.findTokenStart(editable,
							selStart);
					int tokenEnd = mTokenizer
							.findTokenEnd(editable, tokenStart);
					tokenEnd = tokenEnd + 1;
					if (tokenEnd > editable.length()) {
						tokenEnd = editable.length();
					}
					editable.delete(tokenStart, tokenEnd);
					getSpannable().removeSpan(repl[0]);
				}
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) { 
			
			// Do nothing.
		}
		
		
	}
	
	public boolean doesThisRecipientAlreadyExists(String newRecipient){
		
		RecipientChip[] chips = getSortedRecipients();
		if(chips.length > 0){
			for(RecipientChip chip: chips){
				String recipient =  tokenizeAddress(chip.getValue().toString());
				if(recipient.equalsIgnoreCase(newRecipient)){
					return true;
				}
			}
		}
		
		return false;
		
	}
	
	// Visible for testing.
	/* package */ArrayList<RecipientChip> handlePaste() {
		String text = getText().toString();
		int originalTokenStart = mTokenizer.findTokenStart(text,
				getSelectionEnd());
		String lastAddress = text.substring(originalTokenStart);
		int tokenStart = originalTokenStart;
		int prevTokenStart = 0;
		RecipientChip findChip = null;
		ArrayList<RecipientChip> created = new ArrayList<RecipientChip>();
		if (tokenStart != 0) {
			// There are things before this!
			while (tokenStart != 0 && findChip == null
					&& tokenStart != prevTokenStart) {
				prevTokenStart = tokenStart;
				tokenStart = mTokenizer.findTokenStart(text, tokenStart);
				findChip = findChip(tokenStart);
			}
			if (tokenStart != originalTokenStart) {
				if (findChip != null) {
					tokenStart = prevTokenStart;
				}
				int tokenEnd;
				RecipientChip createdChip;
				while (tokenStart < originalTokenStart) {
					tokenEnd = movePastTerminators(mTokenizer.findTokenEnd(
							getText().toString(), tokenStart));
					commitChip(tokenStart, tokenEnd, getText());
					createdChip = findChip(tokenStart);
					if (createdChip == null) {
						break;
					}
					// +1 for the space at the end.
					tokenStart = getSpannable().getSpanEnd(createdChip) + 1;
					created.add(createdChip);
				}
			}
		}
		// Take a look at the last token. If the token has been completed with a
		// commit character, create a chip.
		if (isCompletedToken(lastAddress)) {
			Editable editable = getText();
			tokenStart = editable.toString().indexOf(lastAddress,
					originalTokenStart);
			commitChip(tokenStart, editable.length(), editable);
			created.add(findChip(tokenStart));
		}
		return created;
	}

	// Visible for testing.
	/* package */int movePastTerminators(int tokenEnd) {
		if (tokenEnd >= length()) {
			return tokenEnd;
		}
		char atEnd = getText().toString().charAt(tokenEnd);
		if (atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON) {
			tokenEnd++;
		}
		// This token had not only an end token character, but also a space
		// separating it from the next token.
		if (tokenEnd < length() && getText().toString().charAt(tokenEnd) == ' ') {
			tokenEnd++;
		}
		return tokenEnd;
	}

	
	/**
	 * MoreImageSpan is a simple class created for tracking the existence of a
	 * more chip across activity restarts/
	 */
	private class MoreImageSpan extends ImageSpan {
		public MoreImageSpan(Drawable b) {
			super(b);
		}
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// Do nothing.
		return false;
	}

	/**
	 * Enables drag-and-drop for chips.
	 */
	public void enableDrag() {
		mDragEnabled = true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// Do nothing.
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// Do nothing.
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// Do nothing.
		return false;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		mCopyAddress = null;
	}

	
	protected boolean isPhoneQuery() {
		/*
		 * return getAdapter() != null && ((BaseRecipientAdapter)
		 * getAdapter()).getQueryType() ==
		 * BaseRecipientAdapter.QUERY_TYPE_PHONE;
		 */
		return false;
	}

	public boolean isValidEmail(String email) {
		String emailExpression = "^(?!.*\\.{2})[A-Z0-9a-z._%+-]+@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
		return Pattern.matches(emailExpression, email);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub	

	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub

	}
	public int getLastChipEnd(){
		return getChipEnd(getLastChip());
	}
}