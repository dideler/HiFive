/**
 * Copyright (C) 2012 Team High Five!
 * Mobile Computing - Final Project
 * @author Dennis Ideler (ideler.dennis@gmail.com)
 * @author Chris Stinson (chris.the.stinson@gmail.com) 
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
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.Settings;
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
    TextView statusbar;
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
        statusbar = (TextView) findViewById(R.id.textView);
        
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        	
    	// Check if Android Beam is enabled.
        // else if (!mNfcAdapter.isNdefPushEnabled()) // Available in API 16 and up.
        //{
        //	toast(R.string.beam_disabled);
        //}
        
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
    		loadVcard(lookupKey);
    	}
    	// NOTE: NDEF callbacks moved to onResume.
    }


    /**
     * Implementation for the CreateNdefMessageCallback interface.
     * NDEFMessage contains the contact as vCard-formatted data.
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event)
    {
    	return new NdefMessage(new NdefRecord[] { getContactRecord() });
    }
    
    /**
     * Generates an NdefRecord corresponding to the current contact, as defined in
     * the Beam.VCARD static variable.
     * @return NdefRecord containing the contact info to beam
     */
    private NdefRecord getContactRecord()
    {
    	if (VCARD.isEmpty())  // If no contact is set...
    	{
    		// Send an "empty" contact (vcard).
    		// On receiving device, user will be prompted create a new contact.
    		return null;
    	}
    	else  // Send the set contact.
    	{
    		byte[] uriField = VCARD.getBytes(Charset.forName("US-ASCII"));
    		byte[] payload = new byte[uriField.length + 1];  // Add 1 for the URI Prefix.
    		System.arraycopy(uriField, 0, payload, 1, uriField.length);  // Append URI to payload.
    		NdefRecord nfcRecord = new NdefRecord(
    				NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);
    		return nfcRecord;
    	}
    }

    /**
     * Implementation for the OnNdefPushCompleteCallback interface.
     * A handler is needed to send messages to the activity when this
     * callback occurs, because it happens from a binder thread.
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0)
    {
    	mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)  // User-defined message code.
            {
            	case MESSAGE_SENT:
            		toast(R.string.beamed);
	        		Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE) ;
	        		if (vibe.hasVibrator()) vibe.vibrate(500);
	        		break;
            }
        }
    };

    @Override
    public void onResume()
    {
    	super.onResume();
    	mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    	statusbar.setText(R.string.contact_set);
        if (mNfcAdapter == null)  // Check for available NFC Adapter.
        {
            statusbar.setText(R.string.nfc_not_available);
        }
        else if (!mNfcAdapter.isEnabled()) // Check if NFC is enabled.
        {
        	statusbar.setText(R.string.nfc_disabled);
        }
        else // good to go!
        {
            if (VCARD.isEmpty())  // Ensure contact has been set.
            {
            	statusbar.setText(R.string.forgot_set_contact);
            	//toast(R.string.choose_contact);
        		//changeContactInfo();
            }
            else if (mNfcAdapter != null)
            {
            	// Register callback to set NDEF message
                mNfcAdapter.setNdefPushMessageCallback(this, this);
                
                // Register callback to listen for message-sent success
                mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
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
    
    /** Clears the status bar */
    public void clearStatus()
    {
    	statusbar.setText("");
    }
}
