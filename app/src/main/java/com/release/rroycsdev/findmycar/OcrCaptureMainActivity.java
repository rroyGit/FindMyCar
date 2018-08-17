/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.release.rroycsdev.findmycar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * recognizes text.
 */
public class OcrCaptureMainActivity extends Activity implements View.OnClickListener {

    // Use a compound button so either checkbox or switch widgets work.
    private CompoundButton autoFocus;
    private CompoundButton useFlash;
    private TextView statusMessage;
    private TextView textValue;

    private TextInputEditText textInputEditText;
    private TextInputLayout textInputLayout;
    private AppCompatButton saveButton;


    private static final int RC_OCR_CAPTURE = 9003;
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_capture_main);
        sharedPreferences = getSharedPreferences("CarImageText", MODE_PRIVATE);

        statusMessage = (TextView)findViewById(R.id.status_message);
        textValue = (TextView)findViewById(R.id.text_value);

        autoFocus = (CompoundButton) findViewById(R.id.auto_focus);
        useFlash = (CompoundButton) findViewById(R.id.use_flash);

        textInputEditText = findViewById(R.id.editText);
        textInputLayout = findViewById(R.id.textInput);
        saveButton = findViewById(R.id.save_button);


        findViewById(R.id.read_text).setOnClickListener(this);
        findViewById(R.id.read_text2).setOnClickListener(this);
        findViewById(R.id.save_button).setOnClickListener(this);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.read_text) {
            // launch Ocr capture activity.
            Intent intent = new Intent(this, OcrCaptureActivity.class);
            intent.putExtra(OcrCaptureActivity.AutoFocus, autoFocus.isChecked());
            intent.putExtra(OcrCaptureActivity.UseFlash, useFlash.isChecked());

            startActivityForResult(intent, RC_OCR_CAPTURE);
        }else if(v.getId() == R.id.read_text2){
            textInputLayout.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.VISIBLE);
        }else if(v.getId() == R.id.save_button){
            String textResult = textInputEditText.getText().toString();

            statusMessage.setText(R.string.ocr_success);
            textValue.setText(textResult);

            Intent textData = new Intent();
            textData.putExtra("String", textResult);
            setResult(CommonStatusCodes.SUCCESS, textData);
            setSharedPref(textResult);

            String toastMsg = "Got it, now processing..." ;
            Toast toast = Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 300);
            toast.show();

            new Handler().postDelayed(this::finish,2500);
        }

    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_OCR_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    String text = data.getStringExtra(OcrCaptureActivity.TextBlockObject);
                    statusMessage.setText(R.string.ocr_success);
                    textValue.setText(text);
                    Log.d(TAG, "Text read: " + text);

                    Intent textData = new Intent();
                    textData.putExtra("String", text);
                    setResult(CommonStatusCodes.SUCCESS, textData);
                    setSharedPref(text);
                    finish();


                } else {
                    statusMessage.setText(R.string.ocr_failure);
                    Log.d(TAG, "No Text captured, intent data is null");
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.ocr_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setSharedPref(String text){
        SharedPreferences.Editor editor= sharedPreferences.edit();
        editor.putString("myText", text);
        editor.apply();
    }
}
