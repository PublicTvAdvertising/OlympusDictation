package com.olympus.dmmobile.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.DatabaseHandler;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;
import com.olympus.dmmobile.utils.chips.RecipientEditTextView;
import com.olympus.dmmobile.utils.chips.RecipientEditorListener;

/**
 * AddRecipientActivity is the activity which performs add/edit operations of Recipients.
 *
 * @version 1.2.0
 */
public class AddRecipientActivity extends Activity implements
        RecipientEditorListener {
    private boolean isContactPermission = false;
    private SharedPreferences pref;
    private ExceptionReporter mReporter; // Error Logger
    private RecipientEditTextView editTextRecipientEmail;    // custom textview to show recipients as bubble
    private static final int CONTACT_PICKER_RESULT = 1001;    // request code to handle contact picker

    private String recipient_email = "";
    private String recipient_name = "";
    int edit_recipient_id;
    String edit_recipient_email;
    boolean isEditRecipientMode = false;
    DatabaseHandler dbHandler;
    ImageButton add;
    boolean isEditAlreadySelectedItem = false;
    DMApplication dmApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mReporter = ExceptionReporter.register(this);
        super.onCreate(savedInstanceState);
        dbHandler = ((DMApplication) getApplication()).getDatabaseHandler();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.add_recipient_activity);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dmApplication = (DMApplication) getApplication();

        //initialize the custom edittext
        editTextRecipientEmail = (RecipientEditTextView) findViewById(R.id.text_recipient_email);
        editTextRecipientEmail.setTokenizer(new Rfc822Tokenizer());

        // check the intent if the activity is started in edit mode
        isEditRecipientMode = getIntent().getBooleanExtra("email_edit", false);

        if (isEditRecipientMode) {
            // get the details of recipient opened for editing
            edit_recipient_id = getIntent().getIntExtra("id", 1);
            edit_recipient_email = getIntent().getStringExtra("email");
            isEditAlreadySelectedItem = getIntent().getBooleanExtra(
                    "isEditAlreadySelectedItem", false);
            editTextRecipientEmail.append(edit_recipient_email);
            // set the title for edit mode
            setTitle(getResources().getString(R.string.edit_recipients));
        } else {
            // set the title for add recipient
            setTitle(getResources().getString(R.string.add_recipients));
        }

        // touch listener for the button to add recipients
        editTextRecipientEmail.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (editTextRecipientEmail.getRight() - editTextRecipientEmail
                            .getCompoundDrawables()[DRAWABLE_RIGHT].getBounds()
                            .width())) {
                        // check if maximum recipients limit has reached or not
                        if (!editTextRecipientEmail.isRecipientLimitExceeded()) {

                            // get the end position of last chip. getLastChipEnd() returns -1, if there are no chips
                            int chip_end = editTextRecipientEmail
                                    .getLastChipEnd();

                            // get the length the text
                            int text_length = editTextRecipientEmail.getText()
                                    .length();

                            if (!checkForInvalidEmails(editTextRecipientEmail
                                    .getText().toString())) {
                                // create chip for the valid email

                                if (chip_end != -1) {    // there are chips available in the edit text

                                    if (chip_end < text_length) {
                                        // get the new text after the last chip
                                        String newText = editTextRecipientEmail
                                                .getText()
                                                .subSequence(chip_end,
                                                        text_length).toString()
                                                .trim();
                                        if (editTextRecipientEmail
                                                .checkForInvalidCharacters(newText)) {
                                            onInvalidRecipientEntered();
                                            return true;
                                        }
                                        if (!TextUtils.isEmpty(newText)) {
                                            // validate the new text
                                            if (!isSameChipExists(newText)) {
                                                // replace the new text and create chip
                                                newText += ",";
                                                editTextRecipientEmail
                                                        .getText()
                                                        .replace(
                                                                chip_end,
                                                                text_length,
                                                                newText,
                                                                0,
                                                                newText.length());
                                                showContactPicker();
                                            } else {
                                                onRecipientAlreadyExist();
                                            }
                                        } else {
                                            // if there is no text entered after last chip,therefore start the contact picker activity
                                            showContactPicker();
                                        }

                                    } else if (chip_end == text_length) {
                                        showContactPicker();
                                    }
                                } else {
                                    // no chips available in the edit text

                                    String newText = editTextRecipientEmail
                                            .getText().toString().trim();        // get the new text

                                    // check for invalid characters in the new text
                                    if (editTextRecipientEmail
                                            .checkForInvalidCharacters(newText)) {
                                        onInvalidRecipientEntered();
                                        return true;
                                    }
                                    if (!TextUtils.isEmpty(newText)) {
                                        // if the new text is not empty, create chip
                                        if (!isSameChipExists(newText)) {
                                            newText += ",";
                                            editTextRecipientEmail.getText()
                                                    .replace(0, text_length,
                                                            newText);
                                            // show contact picker after creating chip
                                            showContactPicker();
                                        } else {
                                            onRecipientAlreadyExist();

                                        }
                                    } else {
                                        // there is no text to create chip, therefore show the contact picker
                                        showContactPicker();
                                    }

                                }

                            } else {
                                onInvalidRecipientEntered();

                            }
                        } else {
                            onRecipientLimitExceeded();
                        }
                        return true;
                    }
                }
                return false;
            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * method to display invalid email alert
     *
     * @param invalidEmails invalid email list
     */
    private void showInvalidEmailDialog(String[] invalidEmails) {
        int invalidEmailNo = invalidEmails.length;
        String msg = getResources().getString(
                R.string.invalid_recipient_email_alert_message);
        for (int i = 0; i < invalidEmailNo && invalidEmails[i] != null; msg += invalidEmails[i]
                + "\n", i++)
            ;
        String title = getResources().getString(R.string.Alert);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(
                        getResources().getString(R.string.Dictate_Alert_Ok),
                        null).show();
    }

    /**
     * method to display email validation dialog with given title and message
     *
     * @param title   title of dialog
     * @param message message to display
     */
    public void showEmailValidationDialog(String title, String message) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        getResources().getString(R.string.Dictate_Alert_Ok),
                        null).show();
        // showKeyboard();

    }

    /**
     * method to display contact application for selecting a recipient
     */
    private void showContactPicker() {
        pref = getSharedPreferences("Conpermissions", MODE_PRIVATE);
        isContactPermission = pref.getBoolean("Conperm", false);
        if (checkContactPermission() && isContactPermission) {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK);
            contactPickerIntent
                    .setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
//					pref = getSharedPreferences("Conpermissions", MODE_PRIVATE);
//					isContactPermission = pref.getBoolean("Conperm", false);
//					if(checkContactPermission()&&isContactPermission){
                    String[] invalidEmailList = null;
                    Cursor cursor = null;
                    String name = "";
                    String email = "";
                    boolean validEmailsFound = false;
                    boolean invalidEmailsFound = false;
                    boolean alreadyExist = false;
                    int recipientCount = 0;
                    int validEmailCount = 0;
                    try {

                        Uri result = data.getData();    // get the result from intent
                        String id = result.getLastPathSegment();
                        // get the data from the contact content provider with the selected recipient ID
                        cursor = this.getContentResolver().query(Email.CONTENT_URI,
                                null, Email._ID + "=?", new String[]{id}, null);

                        int nameColIdx =

                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? cursor
                                        .getColumnIndex(Email.DISPLAY_NAME_PRIMARY)
                                        : cursor.getColumnIndex(Email.DISPLAY_NAME_PRIMARY);
                        int emailColIdx = cursor.getColumnIndex(Email.DATA);
                        cursor.moveToFirst();

                        int i = 0;
                        int j = 0;

                        invalidEmailList = new String[cursor.getCount()];
                        recipient_email = "";

                        recipientCount = editTextRecipientEmail.getRecipientCount();

                        while (i < cursor.getCount()) {
                            name = cursor.getString(nameColIdx);
                            email = cursor.getString(emailColIdx);
                            // if name and email are same, clear name
                            if (name.equalsIgnoreCase(email)) {
                                name = "";
                            }

                            String SelectedContactNode = "";

                            if (!doesThisRecipientAlreadyExists(email)) {
                                alreadyExist = false;
                                if (isValidEmail(email)) {
                                    if (name.length() > 0) {
                                        // if there is name for selected contact
                                        SelectedContactNode = name + " <" + email
                                                + ">,";
                                        recipient_email += SelectedContactNode;
                                        recipient_name = name;
                                    } else {
                                        // name is not available for selected
                                        // contact
                                        SelectedContactNode = "<" + email + ">,";
                                        recipient_email += SelectedContactNode;
                                    }
                                    validEmailsFound = true;
                                    validEmailCount++;
                                } else {
                                    // selected recipient is invalid
                                    invalidEmailsFound = true;
                                    invalidEmailList[j] = email;
                                    j++;

                                }

                            } else {
                                // the selected recipient already exists
                                alreadyExist = true;
                            }

                            cursor.moveToNext();
                            i++;

                        }

                    } catch (Exception e) {

                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                        if (validEmailsFound) {
                            // check if maximum recipients limit is reached or not
                            if (!((recipientCount + validEmailCount) > RecipientEditTextView.MAX_CHIPS_PARSED)) {
                                // append the selected recipient as chip to the edittext
                                editTextRecipientEmail.append(recipient_email);
                                showKeyboard();
                            } else {
                                onRecipientLimitExceeded();
                            }
                            if (invalidEmailsFound) {
                                showInvalidEmailDialog(invalidEmailList);
                            }

                        } else {
                            if (!alreadyExist) {
                                if (invalidEmailList != null
                                        && invalidEmailList.length >= 1) {
                                    showInvalidEmailDialog(invalidEmailList);
                                } else {
                                    // no email available for the selected recipient
                                    showEmailValidationDialog(
                                            getResources()
                                                    .getString(R.string.Alert),
                                            getResources().getString(
                                                    R.string.email_not_available));
                                }
                            } else {
                                // email already exists
                                showEmailValidationDialog(
                                        getResources().getString(R.string.Alert),
                                        getResources().getString(
                                                R.string.recipient_already_exist));
                            }

                        }

                    }
                    break;
                //}
            }
        } else {
        }
    }

    /**
     * method to invoke keyboard manually
     */
    private void showKeyboard() {
        (new Handler()).postDelayed(new Runnable() {

            public void run() {
                editTextRecipientEmail.dispatchTouchEvent(MotionEvent.obtain(
                        SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN, 0, 0, 0));
                editTextRecipientEmail.dispatchTouchEvent(MotionEvent.obtain(
                        SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }, 200);
    }

    /**
     * method to check whether the given recipient already exists or not
     *
     * @param newRecipient recipient to check
     * @return returns true if the given recipient already exists, else false
     */
    private boolean doesThisRecipientAlreadyExists(String newRecipient) {
        ArrayList<String> alreadySelectedRecipients = getSortedArrayOfRecipients(editTextRecipientEmail
                .getText().toString());
        for (String recipient : alreadySelectedRecipients) {
            if (recipient.equalsIgnoreCase(newRecipient)) {
                return true;
            }
        }

        return false;
    }

    /**
     * method to check whether any chip exists with the given recipient
     *
     * @param newRecipient recipient to check
     * @return returns true if chip already exists
     */
    private boolean isSameChipExists(String newRecipient) {
        return editTextRecipientEmail
                .doesThisRecipientAlreadyExists(newRecipient);
    }

    /**
     * method to check whether the given email is valid or not
     *
     * @param email email to validate
     * @return returns true if the given email is valid
     */
    public boolean isValidEmail(String email) {
        String emailExpression = "^(?!.*\\.{2})[A-Z0-9a-z._%+-]+@[A-Za-z0-9]+[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        return Pattern.matches(emailExpression, email);
    }

    /**
     * method used to set result and returns to the started activity
     */
    protected void finishAndSetResult() {
        Bundle result = new Bundle();
        result.putString("name", recipient_name);
        result.putString("email", recipient_email);
        Intent intent = new Intent();
        intent.putExtras(result);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onInvalidRecipientEntered() {
        showEmailValidationDialog(
                getResources().getString(R.string.Settings_Invalid_Email),
                getResources().getString(R.string.enter_valid_email_address));
        // showKeyboard();
    }

    @Override
    public void onEmptyTextEntered() {
        showEmailValidationDialog(getResources().getString(R.string.Alert),
                getResources().getString(R.string.choose_email_address));

    }

    @Override
    public void onRecipientAlreadyExist() {
        showEmailValidationDialog(getResources().getString(R.string.Alert),
                getResources().getString(R.string.recipient_already_exist));

    }

    @Override
    public void onBackPressed() {
        if (isEditRecipientMode) {
            Bundle result = new Bundle();
            result.putBoolean("isEditAlreadySelectedItem", false);
            Intent intent = new Intent();
            intent.putExtras(result);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onValidationSuccess(String recipientData) {
        if (isRecipientAlreadyExists(recipientData)) {
            if (isEditRecipientMode
                    && edit_recipient_email.substring(0,
                    edit_recipient_email.length() - 1)
                    .equalsIgnoreCase(recipientData)) {
                // no changes has been made to recipient in edit mode
                Bundle result = new Bundle();
                result.putBoolean("isEditAlreadySelectedItem", false);
                result.putBoolean("edit_success", false);
                Intent intent = new Intent();
                intent.putExtras(result);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                // the edited recipient already exists
                onRecipientAlreadyExist();
            }
            return;
        }
        if (isEditRecipientMode) {
            // update the edited recipient in db
            dbHandler.updateRecipient(edit_recipient_id, recipientData);
            if (isEditAlreadySelectedItem) {
                // update selected recipient in shared preference,
                // if the edited recipient is currently selected one
                saveSelectedRecipientToPreference("", recipientData);
                setRecipientForceUpdate(true);
            }
        }
        Bundle result = new Bundle();
        result.putBoolean("isEditAlreadySelectedItem",
                isEditAlreadySelectedItem);
        result.putBoolean("edit_success", true);
        result.putString("name", "");
        result.putInt("id", edit_recipient_id);
        result.putString("email", recipientData);

        Intent intent = new Intent();
        intent.putExtras(result);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * method which set value in shared preference to update the recipients forcefully
     *
     * @param status value to set the state
     */
    private void setRecipientForceUpdate(boolean status) {
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(AddRecipientActivity.this);
        SharedPreferences.Editor mEditor = pref.edit();
        mEditor.putBoolean(Recipient.RECIPIENT_FORCE_UPDATE_TAG, status);
        mEditor.commit();
    }

    @Override
    public void onRecipientLimitExceeded() {
        showEmailValidationDialog(getResources().getString(R.string.Alert),
                getResources().getString(R.string.Settings_Max_Recipients));
    }

    /**
     * method to parse the recipients from the given string and return a sorted array
     *
     * @param recipient string with set of recipients
     * @return array list of recipients
     */
    private ArrayList<String> getSortedArrayOfRecipients(String recipient) {
        Rfc822Token[] tokens = null;
        ArrayList<String> recipientArray = new ArrayList<String>();
        if (!TextUtils.isEmpty(recipient)) {
            tokens = Rfc822Tokenizer.tokenize(recipient);
            if (tokens.length > 0) {
                for (Rfc822Token token : tokens) {
                    recipientArray.add(token.getAddress());
                }
                Collections.sort(recipientArray);
            }

        }
        return recipientArray;
    }

    /**
     * method to parse the given string and check for invalid emails
     *
     * @param text string with set of recipients
     * @return returns true if any invalid emails found
     */
    private boolean checkForInvalidEmails(String text) {
        Rfc822Token[] tokens = null;
        if (!TextUtils.isEmpty(text)) {
            tokens = Rfc822Tokenizer.tokenize(text);
            if (tokens.length > 0) {
                for (Rfc822Token token : tokens) {
                    if (!isValidEmail(token.getAddress())) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * utility method to compare two given arrays
     *
     * @param firstArray  first array to compare
     * @param secondArray second array to compare
     * @return returns true if both arrays are equal
     */
    private boolean compareTwoArray(ArrayList<String> firstArray,
                                    ArrayList<String> secondArray) {
        int firstArrayLength = firstArray.size();
        ;
        int secondArrayLength = secondArray.size();

        if (firstArrayLength != secondArrayLength) {
            return false;
        }

        for (int i = 0; i < firstArrayLength; i++) {
            if (!firstArray.get(i).equalsIgnoreCase(secondArray.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * method to check whether the given recipient already exist in the database or not
     *
     * @param recipient recipient to check
     * @return returns true, if the recipient already exists in database
     */
    private boolean isRecipientAlreadyExists(String recipient) {
        ArrayList<String> recipientArray = getSortedArrayOfRecipients(recipient);
        DatabaseHandler dbHandler = dmApplication.getDatabaseHandler();
        Cursor c = dbHandler.getAllRecipient();
        if (c != null && c.getCount() >= 1) {
            c.moveToFirst();
            for (int index = 0; index < c.getCount(); index++) {
                String recipientItem = c.getString(c
                        .getColumnIndex(Recipient.EMAIL_COLUMN));
                if (compareTwoArray(recipientArray,
                        getSortedArrayOfRecipients(recipientItem))) {
                    return true;
                }
                c.moveToNext();
            }

        }
        return false;
    }

    /**
     * method to save the selected recipient in shared preference
     *
     * @param name  name of the selected recipient
     * @param email email of the selected recipient
     */
    private void saveSelectedRecipientToPreference(String name, String email) {
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(AddRecipientActivity.this);
        SharedPreferences.Editor mEditor = pref.edit();
        mEditor.putString(Recipient.SELECTED_RECIPIENT_NAME_TAG, name);
        mEditor.putString(Recipient.SELECTED_RECIPIENT_EMAIL_TAG, email);
        mEditor.commit();
    }

    public boolean checkContactPermission() {
        boolean check=true;
        int permissiontakeCamera = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);
        int[] perm = {permissiontakeCamera};
        String[] stringPerm = {Manifest.permission.READ_CONTACTS};
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
                        //set to never ask again
                        Log.e("set to never ask again", permission);
                        somePermissionsForeverDenied = true;
                    }
                }
            }
            if (somePermissionsForeverDenied) {
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Permissions Required")
                        .setMessage("To excute this action, tap SETTINGS go to App info> Permissions, then allow the following permission and try again\n\nPermission(Contacts)")
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
            pref = getSharedPreferences("Conpermissions", MODE_PRIVATE);
            SharedPreferences.Editor editor = getSharedPreferences("Conpermissions", MODE_PRIVATE).edit();
            editor.putBoolean("Conperm", true);
            editor.commit();
            isContactPermission = true;
        }
    }

}
