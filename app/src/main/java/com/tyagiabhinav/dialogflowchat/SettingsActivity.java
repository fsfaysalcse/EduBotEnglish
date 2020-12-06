package com.tyagiabhinav.dialogflowchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tyagiabhinav.dialogflowchat.utility.Constants;
import com.tyagiabhinav.dialogflowchat.utility.SharedPref;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    RadioButton radioButton;
    RadioButton radioButton2;

    String language = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        RadioGroup group = findViewById(R.id.langgroup);

        radioButton = findViewById(R.id.radioButton);
        radioButton2 = findViewById(R.id.radioButton2);

        String get_lang = SharedPref.getKey(this, Constants.LANG);

        if (get_lang.equals("bn-BD")) {
            radioButton2.setChecked(true);
            radioButton.setChecked(false);
        } else {
            radioButton2.setChecked(false);
            radioButton.setChecked(true);
        }

        findViewById(R.id.okbtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // get selected radio button from radioGroup
                int selectedId = group.getCheckedRadioButtonId();

                // find the radiobutton by returned id

                if (selectedId == radioButton2.getId()) {
                    SharedPref.putKey(getApplicationContext(), Constants.LANG, "bn-BD");
                    LocaleHelper.setLocale(getApplicationContext(), "bn");
                } else {
                    SharedPref.putKey(getApplicationContext(), Constants.LANG, "en-US");
                    LocaleHelper.setLocale(getApplicationContext(), "en-US");
                }

                Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


}