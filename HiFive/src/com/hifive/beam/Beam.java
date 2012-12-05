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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
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
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
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
    public static SharedPreferences settings;
	private static final String TAG = "Beam";
    private static final int MESSAGE_SENT = 1;
    public static final int PREF_REQUEST_CODE = 13;
    public static final String PREFERENCE_FILENAME = "ContactsPrefs";
    public static final String LOOKUP_ID = "ContactID";
    public static final String CONTACT_NAME = "ContactName";
    public static String VCARD = ""; // Not final because it can change during runtime.
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

    	settings = getSharedPreferences(PREFERENCE_FILENAME, MODE_PRIVATE);
        mInfoText = (TextView) findViewById(R.id.textView);
        
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null)  // Check for available NFC Adapter.
        {
            mInfoText.setText(R.string.nfc_not_available);
        }
        else  // Good to go!
        {
        	// Check if NFC is enabled.
        	// TODO: make it a notification (https://github.com/TeamHi5/HiFive/issues/9)
        	// TODO: put in its own function
        	if (!mNfcAdapter.isEnabled())
            {
        	    toast(R.string.nfc_disabled);
            }
        	
        	// Check if Android Beam is enabled.
            // else if (!mNfcAdapter.isNdefPushEnabled()) // Available in API 16 and up.
            //{
            //	toast(R.string.beam_disabled);
            //}
        	
        	// TODO: Verify this stuff is still executed if user is brought to ContactInfo activity on startup.
        	//		 Otherwise we should put this section in a method so we can repeat calls to it.
        	
        	// Delete all preferences -- for testing only!
        	getSharedPreferences(PREFERENCE_FILENAME, 0).edit().clear().commit();
            
        	// Load lookupKey from saved preferences.
        	String lookupKey = settings.getString(LOOKUP_ID, "No ID found!");
        	Log.i(TAG, lookupKey);
        	
        	// Bring user to the ContactInfo activity if no preferences saved.
        	if (!settings.contains(LOOKUP_ID))
        	{
        		toast(R.string.choose_contact);
        		changeContactInfo();
        	}
        	else  // Load vCard data.
        	{
        		loadVcard(lookupKey); // TODO: test it's working
        	}
        	// NOTE: NDEF callbacks moved to onResume.
        }
    }


    /**
     * Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event)
    {
    	NdefMessage msg = new NdefMessage(new NdefRecord[] { getContactRecord() });
//        NdefMessage msg = new NdefMessage(
//                new NdefRecord[] { createMimeRecord(
//                        "application/com.example.android.beam", text.getBytes())
//         /**
//          * The Android Application Record (AAR) is commented out. When a device
//          * receives a push with an AAR in it, the application specified in the AAR
//          * is guaranteed to run. The AAR overrides the tag dispatch system.
//          * You can add it back in to guarantee that this
//          * activity starts when receiving a beamed message. For now, this code
//          * uses the tag dispatch system.
//          */
//          //,NdefRecord.createApplicationRecord("com.example.android.beam")
//                    
//        });
     
    	// NDEFMessage contains the contact as vCard-formatted data.
        return msg;
    }
    
    /**
     * Generates an NdefRecord corresponding to the current contact, as defined in
     * the Beam.VCARD static variable.
     * @return NdefRecord containing the contact info to beam
     */
    private NdefRecord getContactRecord()
    {
    	if (VCARD.isEmpty())
    	{
    		return null; // TODO: test what happens on beam attempt
    	}
    	else
    	{
    		byte[] uriField = VCARD.getBytes(Charset.forName("US-ASCII"));
            byte[] payload = new byte[uriField.length + 1];              //add 1 for the URI Prefix
            System.arraycopy(uriField, 0, payload, 1, uriField.length);  //appends URI to payload
            NdefRecord nfcRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);
            return nfcRecord;
    	}
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
    	
        if (settings != null && !settings.contains(LOOKUP_ID)) // TODO: might be better to check if vcard is empty
        {
        	toast(R.string.forgot_set_contact); // temporary
        	//toast(R.string.choose_contact);
    		//changeContactInfo();
        }
        else if (mNfcAdapter != null)
        {
        	// set callbacks, create message, etc.
        	// Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            // Register callback to listen for message-sent success
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
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
            	timedToast(R.string.info, 15000);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /** Called from within this class. */
    public void changeContactInfo() {
    	startActivity(new Intent(this, ContactInfo.class));
    }
    
    /** Called when the user clicks the Send button. */
    public void changeContactInfo(View view) {
    	startActivity(new Intent(this, ContactInfo.class));
    }
    
    /**
     * Load a contact's vCard data based on their phone number.
     * Repeat code from ContactInfo class. We can refactor code later.
     */
    public void loadVcard(String lookupKey)
    {
    	Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);     

    	try
    	{
    	    AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(uri, "r");
    	    FileInputStream fis = fd.createInputStream();
    	    byte[] b = new byte[(int) fd.getDeclaredLength()];
    	    fis.read(b);
    	    VCARD = new String(b);
    	    Log.d(TAG, VCARD);
    	}
    	catch (FileNotFoundException e) { e.printStackTrace(); }
    	catch (IOException e) { e.printStackTrace(); }
    }
    
    // TODO: move all toast methods to their own class
    
    /**
     * Calls our toast method with a string.
     * Allows for us to do `toast(R.string.foo)`.
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
    
    /**
     * Calls our timedToast method with a string.
     * Allows for us to do `timedToast(R.string.foo, 10000)`.
     * @param id
     * @param milliseconds
     */
    public void timedToast(int id, long milliseconds)
    {
    	timedToast(getString(id), milliseconds);
    }
    
    /**
     * Workaround to extend the time of a toast.
     * http://stackoverflow.com/a/7173248/72321
     * @param message
     * @param milliseconds
     */
    public void timedToast(String message, long milliseconds)
    {
    	final Toast tag = Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT);
    	tag.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
    	tag.show();
    	new CountDownTimer(15000, 1000)
    	{
    	    public void onTick(long millisUntilFinished) {tag.show();}
    	    public void onFinish() {tag.show();}
    	}.start();
    }
}
