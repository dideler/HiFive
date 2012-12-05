/*
 * Copyright (C) 2011 The Android Open Source Project
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

/**
 * Mobile Computing - Final Project
 * @author Dennis Ideler
 * @author Chris Stinson
 * 
 * Using the open source Android Beam demo as our foundation.
 */

package com.hifive.beam;

import java.nio.charset.Charset;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.Time;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class Beam extends Activity implements
	CreateNdefMessageCallback, OnNdefPushCompleteCallback
{
    NfcAdapter mNfcAdapter;
    TextView mInfoText;
    private static final int MESSAGE_SENT = 1;
    public static final int PREF_REQUEST_CODE = 13;
    public static final String PREFERENCE_FILENAME = "ContactsPrefs";
    public static final String LOOKUP_ID = "userContactID"; // TODO: phone number vs lookup key?

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Load lookupKey from saved preferences.
        SharedPreferences settings = getSharedPreferences(PREFERENCE_FILENAME, MODE_PRIVATE);
    	String lookupKey = settings.getString(LOOKUP_ID, "No ID found!");
    	toast(lookupKey);

        mInfoText = (TextView) findViewById(R.id.textView);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null)
        {
            mInfoText.setText("NFC is not available on this device.");
        }
        else if (!mNfcAdapter.isEnabled()) // TODO: make it a notification
        {
            Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show();
    	    //toast(R.string.nfc_disabled);
        }
        // else if (!mNfcAdapter.isNdefPushEnabled()) // Available in API 16 and up.
        //{
        //	toast(R.string.beam_disabled);
        //}
        else  // Good to go!
        {
            // Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            // Register callback to listen for message-sent success
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
    }


    /**
     * Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        Time time = new Time();
        time.setToNow();
        String text = ("Beam me up!\n\n" +
                "Beam Time: " + time.format("%H:%M:%S"));
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { createMimeRecord(
                        "application/com.example.android.beam", text.getBytes())
         /**
          * The Android Application Record (AAR) is commented out. When a device
          * receives a push with an AAR in it, the application specified in the AAR
          * is guaranteed to run. The AAR overrides the tag dispatch system.
          * You can add it back in to guarantee that this
          * activity starts when receiving a beamed message. For now, this code
          * uses the tag dispatch system.
          */
          //,NdefRecord.createApplicationRecord("com.example.android.beam")
        });
        return msg;
    }

    /**
     * Implementation for the OnNdefPushCompleteCallback interface
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
    	toast(R.string.beamed);
		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) ;
		vibe.vibrate(500);
    	// TODO: Is handler (and NfcEvent param) necessary for us? 
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SENT:
                Toast.makeText(getApplicationContext(), "Message sent!", Toast.LENGTH_LONG).show();
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
        // TODO: check if NFC & Android Beam are still enabled
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        mInfoText.setText(new String(msg.getRecords()[0].getPayload()));
    }

    /**
     * Creates a custom MIME type encapsulated in an NDEF record
     *
     * @param mimeType
     */
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If NFC is not available, we won't be needing this menu
        if (mNfcAdapter == null) {
            return super.onCreateOptionsMenu(menu);
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_beam_settings: // Android Beam setting
                startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
                return true;
            case R.id.menu_nfc_settings: // Wireless (which includes NFC) setting
            	startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            	return true;
            case R.id.menu_help:
            	// First check if user has NFC disabled and notify them if so.
            	if (!mNfcAdapter.isEnabled())
            	    toast(R.string.nfc_disabled);
            	
            	// Workaround to extend toast popup time to approximately 10 seconds.
            	// http://stackoverflow.com/a/7173248/72321
            	final Toast tag = Toast.makeText(getBaseContext(), R.string.info, Toast.LENGTH_SHORT);
            	tag.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
            	tag.show();
            	new CountDownTimer(15000, 1000)
            	{
            	    public void onTick(long millisUntilFinished) {tag.show();}
            	    public void onFinish() {tag.show();}
            	}.start();
                
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /** Called when the user clicks the Send button (via android:onClick in xml.)  */
    public void changeContactInfo(View view) {
    	// Intent objects are used to provide runtime binding between separate components (e.g. starting another activity).
    	// Intents can also carry a bundle of data to the given activity. 
    	// The second argument refers to the class of the component that the intent will be delivered to.
    	// You can also have _implicit_ intents, where the desired component is not specified (for interacting with other apps).
    	Intent intent = new Intent(this, ContactInfo.class);
    	startActivity(intent);
    }
    
    /**
     * Calls our toast method with a string. Allows for us to do `toast(R.string.foo)`.
     * @param id
     */
    public void toast(int id) {
		toast(getString(id));
	}
    
    /**
     * Toast simplification.
     * @param message
     */
    public void toast(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
		toast.show();
	}
}
