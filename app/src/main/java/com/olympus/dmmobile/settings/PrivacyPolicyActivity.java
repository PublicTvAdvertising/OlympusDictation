package com.olympus.dmmobile.settings;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.olympus.dmmobile.R;

public class PrivacyPolicyActivity extends Activity {

    TextView privacyPolicyContent;
    Button close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);
        this.setTitle(getString(R.string.privacy_policy_title));
        privacyPolicyContent = (TextView)findViewById(R.id.privacy_content);
        close = (Button)findViewById(R.id.close_privacy_policy);

        Spanned spanned = Html.fromHtml(getString(R.string.privacy_policy_content));
        privacyPolicyContent.setText(spanned);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
