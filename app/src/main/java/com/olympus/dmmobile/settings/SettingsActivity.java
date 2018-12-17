
package com.olympus.dmmobile.settings;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.DisplayMetrics;
import android.widget.CheckBox;

import com.olympus.dmmobile.DMActivity;
import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.DatabaseHandler;
import com.olympus.dmmobile.R;
import com.olympus.dmmobile.log.ExceptionReporter;

import java.util.Locale;


/**
 * Class used to show the settings screen
 *
 * @version 1.0.1
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, ServerOptionsTapListener {

    private ExceptionReporter mReporter;            //Error Logger
    private PreferenceScreen mSendOptions;
    private PreferenceScreen mVcvaScreen;
    private ListPreference mSendList;
    private Preference mRecFormatScreen;
    private Preference mRecFormatPref;
    private Preference mAudioQuality;
    private CheckBoxPreference mVcvaCheck;
    private Preference mKeepSentItems;
    private Preference mOnLaunchShow;
    private ListPreference mLanguagePref;
    private CheckBoxPreference mBluetooth;
    private SeekBarPreference mVcvaSeekbar;
    private Preference mWorktypePref;
    private Preference mPrivacyPolicyPrference;
    private Preference mLegalPrference;
    private Preference mAbout;
    private PreferenceCategory mVcvaLevelCat;
    private CheckBoxPreference mVcvaPref;
    private CheckBoxPreference mFlasairPref;
    private EditTextPreference mAuthorPref;
    private CheckBoxPreference mPushToTalkPref;
    private int mSendValue = 1;
    private String mRecValue = null;
    private String mAuthorValue = null;
    private int mKeepSentVal = 7;
    private int mOnLaunchVal = 1;
    private int mLanguageVal;
    private int mVcvaVal;
    private boolean mVcvaCheckVal = false;
    private boolean mPTTCheckVal = false;
    private Locale mlocale;
    private AlertDialog.Builder mAlertDialog;
    private SharedPreferences mSharedPref;
    private SharedPreferences mSharedPrefCheckbox;
    private DatabaseHandler mDBHandler;
    private DMApplication dmApplication = null;
    private ServerOptionsTapEventHandler serverOptionsTapHandler;
    private CheckBox customCheckBox;
    public static final String mPREFERENCES = "Checkbox";
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mReporter = ExceptionReporter.register(this);
        super.onCreate(savedInstanceState);
        // initialize tap event handler for server options button
        serverOptionsTapHandler = new ServerOptionsTapEventHandler(this);
        addPreferencesFromResource(R.xml.settings_preference);
        if(DMApplication.COMINGFROM.equalsIgnoreCase("flash_air"))

        dmApplication = (DMApplication) getApplication();
        dmApplication.setContext(this);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setCurrentLanguage(dmApplication.getCurrentLanguage());
        setTitle(getResources().getString(R.string.Settings_setting));

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefCheckbox = getSharedPreferences(mPREFERENCES, Context.MODE_PRIVATE);
        editor = mSharedPrefCheckbox.edit();
        mSendList = (ListPreference) findPreference(getResources().getString(R.string.send_key));
        mSendOptions = (PreferenceScreen) (findPreference(getResources().getString(R.string.send_option_key)));
        mVcvaScreen = (PreferenceScreen) findPreference(getResources().getString(R.string.vcva_screen_key));
        mRecFormatScreen = findPreference(getResources().getString(R.string.rec_format_key));
        mRecFormatPref = (Preference) findPreference(getResources().getString(R.string.formats_list_key));
        mAudioQuality = (Preference) findPreference(getResources().getString(R.string.audio_quality_key));
        mVcvaCheck = (CheckBoxPreference) findPreference(getResources().getString(R.string.vcva_key));
        mVcvaSeekbar = (SeekBarPreference) findPreference(getResources().getString(R.string.vcva_seekbar_key));
        mKeepSentItems = findPreference(getResources().getString(R.string.keep_sent_items_key));
        mOnLaunchShow = findPreference(getResources().getString(R.string.onlaunch_show_key));
        mLanguagePref = (ListPreference) findPreference(getResources().getString(R.string.language_key));
        mWorktypePref = findPreference(getResources().getString(R.string.worktype_list_key));
        mPrivacyPolicyPrference = findPreference("privacy_policy");
        mLegalPrference = findPreference(getResources().getString(R.string.legal_notices_key));
        mAbout = findPreference(getResources().getString(R.string.about_key));
        mPushToTalkPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.push_to_talk_key));
        mVcvaLevelCat = (PreferenceCategory) findPreference("vcva_level_category");
        mVcvaPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.vcva_key));
        mSendValue = Integer.parseInt(mSharedPref.getString(getResources().getString(R.string.send_key), "1"));
        mBluetooth = (CheckBoxPreference) findPreference(getResources().getString(R.string.blutooth_input_key));
        mFlasairPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.use_flashair_key));
        mAuthorPref = (EditTextPreference) findPreference(getResources().getString(R.string.author_key));
        mVcvaCheckVal = mSharedPref.getBoolean(getResources().getString(R.string.vcva_key), false);
        mVcvaVal = mSharedPref.getInt(getResources().getString(R.string.vcva_seekbar_key), 5);

        enableVcvaSeekbar(mVcvaCheckVal);
        setVcvaScreenSummary(mVcvaCheckVal);
        setSendOption(mSendValue);
        mKeepSentVal = Integer.parseInt(mSharedPref.getString(getResources().getString(R.string.keep_sent_items_key), "7"));
        mOnLaunchVal = Integer.parseInt(mSharedPref.getString(getResources().getString(R.string.onlaunch_show_key), "2"));
        mLanguageVal = Integer.parseInt(mSharedPref.getString(getResources().getString(R.string.language_key), "1"));

        setKeepsentItems(mKeepSentVal);
        setOnlaunchOption(mOnLaunchVal);
        mLanguagePref.setSummary(setLnguage(mLanguageVal));

        mAudioQuality.setSummary(mSharedPref.getString(getResources().getString(R.string.audio_quality_key), getResources().getString(R.string.Settings_Standard)));
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
            mBluetooth.setEnabled(true);
        } else {
            mBluetooth.setEnabled(false);
        }

        mSendOptions.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                if (mSendValue == 1) {
                    Intent emailIntent = new Intent(SettingsActivity.this, EmailOptionsActivity.class);
                    startActivity(emailIntent);
                    finish();
                } else if (mSendValue == 2) {
                    // if the communication is to server, listen for single/quadruple
                    // taps on server options button
                    if (serverOptionsTapHandler.isTapListening()) {
                        serverOptionsTapHandler.listenTapEvents();
                        serverOptionsTapHandler.onTapEvent();
                    }
                }
                return false;
            }
        });
        mWorktypePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent workTypeIntent = new Intent(SettingsActivity.this, WorktypeActivity.class);
                startActivity(workTypeIntent);
                return false;
            }
        });
        mPrivacyPolicyPrference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent legalIntent = new Intent(SettingsActivity.this, PrivacyPolicyActivity.class);
                startActivity(legalIntent);
                return false;
            }
        });
        mLegalPrference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent legalIntent = new Intent(SettingsActivity.this, LegalNoticesActivity.class);
                startActivity(legalIntent);
                return false;
            }
        });
        mAbout.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                promptSettings();
                return false;
            }
        });

        changeFormats();
        enableAuthor();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(getResources().getString(R.string.send_key))) {
            mSendValue = Integer.parseInt(prefs.getString(getResources().getString(R.string.send_key), "1"));
            setSendOption(mSendValue);
            changeFormats();
            enableAuthor();
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.formats_list_key))) {
            mRecValue = prefs.getString(getResources().getString(R.string.formats_list_key), "");
            mRecFormatPref.setSummary(mRecValue);
            mRecFormatScreen.setSummary(mRecValue);
            getListView().invalidate();
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.audio_quality_key))) {
            mAudioQuality.setSummary(prefs.getString(getResources().getString(R.string.audio_quality_key), ""));
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.vcva_key))) {
            mVcvaCheckVal = prefs.getBoolean(getResources().getString(R.string.vcva_key), false);
            mVcvaVal = prefs.getInt(getResources().getString(R.string.vcva_seekbar_key), 5);
            //System.out.println("mVcvaVal   "+mVcvaVal);
            if (mVcvaVal == 9) {
                Editor editor = prefs.edit();
                editor.putInt(getResources().getString(R.string.vcva_seekbar_key), 5);
                editor.commit();
            }
            enableVcvaSeekbar(mVcvaCheckVal);
            setVcvaScreenSummary(mVcvaCheckVal);
            getListView().invalidateViews();
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.keep_sent_items_key))) {
            mDBHandler = dmApplication.getDatabaseHandler();
            mKeepSentVal = Integer.parseInt(prefs.getString(getResources().getString(R.string.keep_sent_items_key), "7"));
            setKeepsentItems(mKeepSentVal);
            mDBHandler.deleteDcitationFromKeepSent(mKeepSentVal);
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.onlaunch_show_key))) {
            mOnLaunchVal = Integer.parseInt(prefs.getString(getResources().getString(R.string.onlaunch_show_key), "1"));
            setOnlaunchOption(mOnLaunchVal);
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.language_key))) {
            mLanguageVal = Integer.parseInt(prefs.getString(getResources().getString(R.string.language_key), "1"));
            changeLanguage();
            mLanguagePref.setSummary(setLnguage(mLanguageVal));

        } else if (key.equalsIgnoreCase(getResources().getString(R.string.push_to_talk_key))) {
            mPTTCheckVal = prefs.getBoolean(getResources().getString(R.string.push_to_talk_key), false);
            if (mPTTCheckVal) {
                mVcvaScreen.setSummary("OFF");
                mVcvaPref.setChecked(false);
            }
        } else if (key.equalsIgnoreCase(getResources().getString(R.string.vcva_seekbar_key))) {
            mVcvaVal = prefs.getInt(getResources().getString(R.string.vcva_seekbar_key), 5);
            //System.out.println("mVcvaVal   changed   "+mVcvaVal);
            if (mVcvaVal == 9) {
                mVcvaCheck.setChecked(false);
                enableVcvaSeekbar(false);
                setVcvaScreenSummary(false);
                getListView().invalidateViews();
            }
        }

    }

    /**
     * method to start about activity
     */
    private void promptSettings() {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        setCurrentLanguage(dmApplication.getCurrentLanguage());
        mAuthorPref = (EditTextPreference) findPreference(getResources().getString(R.string.author_key));
        changeFormats();
        enableAuthor();
    }

    /**
     * method to set the send option
     *
     * @param value get from preference to set send option
     */
    private void setSendOption(int value) {

        if (value == 1) {
            mSendList.setSummary(getResources().getString(R.string.Settings_via_email));
            mSendOptions.setTitle(getResources().getString(R.string.sendoption_email));
        } else if (value == 2) {
            mSendList.setSummary(getResources().getString(R.string.Settings_to_server));
            mSendOptions.setTitle(getResources().getString(R.string.sendoption_server));
        }

    }

    /**
     * method to set the onlaunch option
     *
     * @param value get from preference to set onlaunch option
     */
    private void setOnlaunchOption(int value) {
        if (value == 1) {
            mOnLaunchShow.setSummary(getResources().getString(R.string.Settings_newRecording));
        } else if (value == 2) {
            mOnLaunchShow.setSummary(getResources().getString(R.string.Settings_ListRecording));
        }
    }

    /**
     * method to enable or disable the seekbar according to vcva value
     *
     * @param checkVal is to check whether vcva is checked or not
     */
    private void enableVcvaSeekbar(boolean checkVal) {
        if (checkVal) {

            mVcvaScreen.addPreference(mVcvaLevelCat);
            mVcvaScreen.addPreference(mVcvaSeekbar);
        } else {

            mVcvaScreen.removePreference(mVcvaLevelCat);
            mVcvaScreen.removePreference(mVcvaSeekbar);
        }
    }

    /**
     * method to change the audio format according to server or email options in settings
     */
    private void changeFormats() {
        mRecValue = mSharedPref.getString(getResources().getString(R.string.formats_list_key), "AMR");
        if (mSendValue == 1) {

            mRecFormatScreen.setSelectable(true);
            mRecFormatPref.setSummary(mRecValue);
            mRecFormatScreen.setSummary(mRecValue);
            mRecFormatScreen.setWidgetLayoutResource(R.layout.custom_image);
        } else if (mSendValue == 2) {
            int dssFormat = Integer.parseInt(mSharedPref.getString(getResources().getString(R.string.Audio_Format_key), "0"));
            mRecFormatScreen.setSelectable(false);
            mRecFormatScreen.setSummary(DMApplication.getDssType(dssFormat));
            mRecFormatScreen.setWidgetLayoutResource(0);
        }
    }

    /**
     * method to enable or disable author field in settings according to server or email options in settings
     */

    private void enableAuthor() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mAuthorValue = pref.getString(getResources().getString(R.string.author_key), "AUTHOR");
        if (mSendValue == 1) {
            if (mAuthorValue.equalsIgnoreCase("AUTHOR"))
                mAuthorPref.setText("");
            else
                mAuthorPref.setText(mAuthorValue);
            mAuthorPref.getEditText().setEnabled(true);

        } else if (mSendValue == 2) {
            mAuthorPref.setText(mAuthorValue);
            mAuthorPref.getEditText().setEnabled(false);
        }
    }

    /**
     * method to set vcva is checked or not
     *
     * @param vcvaCheckVal is a boolean value to check whether vcva is checked or not
     */
    private void setVcvaScreenSummary(boolean vcvaCheckVal) {
        if (vcvaCheckVal) {
            mVcvaScreen.setSummary(getResources().getString(R.string.ON));
            mPushToTalkPref.setChecked(false);
        } else
            mVcvaScreen.setSummary(getResources().getString(R.string.OFF));
    }

    /**
     * method to find the locale from sharedpreference
     *
     * @param Value is the value in shared preference used to get locale
     */
    private String setLnguage(int Value) {
        String Language = null;
        if (Value == 1) {
            Language = "English";
        } else if (Value == 2) {
            Language = "Deutsch";
        } else if (Value == 3) {
            Language = "Français";
        } else if (Value == 4) {
            Language = "Español";
        } else if (Value == 5) {
            Language = "Svenska";
        } else if (Value == 6) {
            Language = "Ceský";
        }
        return Language;


    }

    /**
     * method to show alert when the language is changed
     */
    public void changeLanguage() {
        try {
            mAlertDialog = new AlertDialog.Builder(SettingsActivity.this);
            mAlertDialog.setMessage(getResources().getString(R.string.Settings_Language));
            mAlertDialog.setPositiveButton(getResources().getString(R.string.Dictate_Alert_Ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            mAlertDialog.show();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    /**
     * method to keep Sent items that will be stored for the selected time period.
     *
     * @param value is the storage period for sent items get from preference
     */
    public void setKeepsentItems(int value) {
        if (value == 1)
            mKeepSentItems.setSummary(getResources().getString(R.string.Setting_LastDay));
        else if (value == 2)
            mKeepSentItems.setSummary(getResources().getString(R.string.Setting_3day));
        else if (value == 3)
            mKeepSentItems.setSummary(getResources().getString(R.string.Setting_Lastweek));
        else if (value == 4)
            mKeepSentItems.setSummary(getResources().getString(R.string.Setting_Last2weeks));
        else if (value == 5)
            mKeepSentItems.setSummary(getResources().getString(R.string.Setting_Lastmonth));
        else if (value == 6)
            mKeepSentItems.setSummary(getResources().getString(R.string.Setting_Always));

    }

    /**
     * method used to set the language for the current activity
     *
     * @param lang is the locale get from sharedpreference
     */
    private void setLocale(String lang) {

        mlocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = mlocale;
        res.updateConfiguration(conf, dm);

    }

    /**
     * method to find the locale from sharedpreference
     *
     * @param value is the value in shared preference used to get locale
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


    @Override
    public void onServerOptionsClicked(boolean developerOptionsEnabled) {
        Intent serverOptionsIntent = new Intent(SettingsActivity.this, ServerOptionsActivity.class);
        if (developerOptionsEnabled) {
            serverOptionsIntent.putExtra(ServerOptionsActivity.ENABLE_DEVELOPER_OPTIONS, true);
        } else {
            serverOptionsIntent.putExtra(ServerOptionsActivity.ENABLE_DEVELOPER_OPTIONS, false);
        }
        startActivity(serverOptionsIntent);
        finish();

    }
}
