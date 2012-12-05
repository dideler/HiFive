package com.hifive.beam;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity lets the user enter their phone number (it's unique so works for us)
 * and then starts the contacts app showing the contact with that number if it exists,
 * otherwise they have the option to create a new contact.
 */
public class ContactInfo extends Activity {
	
	private static final String TAG = "ContactPicker";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Initialize activity
        setContentView(R.layout.contact_info);

        // Add formatting to number field
        final TextView number = (TextView) findViewById(R.id.number);
        number.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        // Handle button click by searching contacts
        ((Button) findViewById(R.id.search)).setOnClickListener(new OnClickListener()
        {
        	/**
        	 * Searches for a contact by (given) phone number
        	 * and retrieves the contact's vCard-formatted data.
        	 */
            public void onClick(View view)
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
            		
            		Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);     
            		
            		try
            		{
	            	    AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(uri, "r"); // TODO: is context. needed?
	            	    FileInputStream fis = fd.createInputStream();
	            	    byte[] b = new byte[(int) fd.getDeclaredLength()];
	            	    fis.read(b);
	            	    String vCard = new String(b);
	            	    Log.d(TAG, vCard);
            		}
            		catch (FileNotFoundException e) { e.printStackTrace(); }
            		catch (IOException e) { e.printStackTrace(); }
            	}
            	else
            	{
            		toast("I couldn't find a contact with that phone number. Want to try another number?");
            	}
            }
        });
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