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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity lets the user enter their phone number (it's unique so works for us)
 * and then starts the contacts app showing the contact with that number if it exists,
 * otherwise they have the option to create a new contact.
 */
public class ContactInfo extends Activity {
	
	private static final String TAG = "ContactInfo";
    private static TextView number;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Initialize activity
        setContentView(R.layout.contact_info);

        // Add formatting to number field
        number = (TextView) findViewById(R.id.number);
        number.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        
        // Display contact's name if a contact has been set.
    	Beam.settings = getSharedPreferences(Beam.PREFERENCE_FILENAME, MODE_PRIVATE);
    	if (Beam.settings.contains(Beam.CONTACT_NAME))
    	{
        	String name = Beam.settings.getString(Beam.CONTACT_NAME, "Anonymous");
    		((TextView) findViewById(R.id.contactName)).setText(name);
    	}
    }
    
    /**
     * Searches for a contact by (given) phone number
     * and retrieves the contact's vCard-formatted data.
     * Called when user hits the 'Search Contacts' button.
     * @param view
     */
    public void searchContact(View view)
    {
    	// This commented code shows or creates the contact
    	// (if contact doesn't exist) inside the Contacts/People app.
    	// Worst-case scenario, we beam contact straight from the open contact card.

        //Uri uri = Uri.fromParts("tel", number.getText().toString(), "");
        //Log.d(TAG, "uri for intent: [" + uri.toString() + "]");   
        //startActivity(new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, uri));
    	
    	// String phoneNumber = "289-697-1010"; // For testing
    	String phoneNumber = number.getText().toString();
    	if (phoneNumber.isEmpty())
    	{
    		toast("Please enter a phone number.\nTry your own number!");
    		return;
    	}
    	String[] projection = new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup.LOOKUP_KEY };
    	Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
    	
    	Cursor c = getContentResolver().query(lookupUri, projection, null, null, null);
    	Log.d(TAG, DatabaseUtils.dumpCursorToString(c)); // dump contents of cursor
    	
    	// Verify that phone number returns info and move to the first row returned.
    	if (c != null && c.moveToFirst())
    	{
    		String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)); // DISPLAY_NAME_PRIMARY will use other info if their name is not available
    		String lookupKey = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
    		c.close();
    		
    		// Display contact's name (or other identifying info if no name).
    		((TextView) findViewById(R.id.contactName)).setText(name);
    		toast(R.string.contact_chosen_feedback);
    		
    		Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);     
    		
    		try
    		{
        	    AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(uri, "r"); // is context. needed?
        	    FileInputStream fis = fd.createInputStream();
        	    byte[] b = new byte[(int) fd.getDeclaredLength()];
        	    fis.read(b);
        	    Beam.VCARD = new String(b);
        	    Log.d(TAG, Beam.VCARD);
    		}
    		catch (FileNotFoundException e) { e.printStackTrace(); }
    		catch (IOException e) { e.printStackTrace(); }
    		
    		setContact(lookupKey, name);
    	}
    	else
    	{
    		toast(R.string.contact_not_found);
    	}
    }
    
    /**
     * Saves a piece of identifying info from the chosen contact,
     * so we can remember who it is for the next use of app.
     */
    public void setContact(String lookupKey, String name)
    {
    	// Get the application settings and open the editor.
    	Beam.settings = getSharedPreferences(Beam.PREFERENCE_FILENAME, MODE_PRIVATE);
    	SharedPreferences.Editor prefEditor = Beam.settings.edit();
 
    	// Save the lookupKey as an application preference.
		if (!lookupKey.isEmpty()) prefEditor.putString(Beam.LOOKUP_ID, lookupKey);
		
    	// Save name as an application preference.
		if (!name.isEmpty()) prefEditor.putString(Beam.CONTACT_NAME, name);

		// Commit the changes to the preferences.
		prefEditor.commit();
		Log.i(TAG, "Saved: " + lookupKey);
		Log.i(TAG, "Saved: " + name);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
    
    // TODO: move all toast methods to their own class

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