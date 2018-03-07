package com.example.homework.albert.alberthomework

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast

const val SELECT_CONTACT = 1
const val READ_CONTACTS_PERMISSION = 2
const val SEND_SMS = 3

class LandingActivity : AppCompatActivity() {

    private lateinit var inviteFriendsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        inviteFriendsButton = findViewById(R.id.invite_friends_button)
        inviteFriendsButton.setOnClickListener {
            requestPermissionToReadContacts()
        }
    }

    /**
     * Permission result callback function
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == READ_CONTACTS_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestForContactPick()
            } else {
                Toast.makeText(this, R.string.contact_read_permission_request_denied_text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Intent result callback function. requestCode
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SELECT_CONTACT -> {
                if (resultCode == RESULT_OK) {
                    extractPhoneNumber(data)
                } else {
                    Toast.makeText(this, R.string.must_select_contact_to_invite, Toast.LENGTH_SHORT).show()
                }
            }
            SEND_SMS -> {
                AlertDialog.Builder(this).apply {
                    setMessage(getString(R.string.albert_share_alert_text))
                    setPositiveButton(R.string.ok, null)
                    create()
                    show()
                }
            }
        }
    }

    /**
     * Creates an intent for a contact selection. Selected contact will be retrieved
     * on 'onActivityResult' method.
     */
    private fun requestForContactPick() {
        val contactPick = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(contactPick, SELECT_CONTACT)
    }

    /**
     * Requests permission to read contacts. This is necessary to extract phone number from
     * contact.
     */
    private fun requestPermissionToReadContacts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), READ_CONTACTS_PERMISSION)
        } else {
            requestForContactPick()
        }
    }

    /**
     * Opens default sms application to send sms to 'phoneNumber' with a default sms body.
     */
    private fun openDefaultSmsApp(phoneNumber: String) {
        val uri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", getString(R.string.albert_default_share_text))
        }
        startActivityForResult(intent, SEND_SMS)
    }

    /**
     * Helper function to extract phone number from selected contact.
     */
    private fun extractPhoneNumber(data: Intent?) {
        data?.let {
            val returnUri = it.data
            val cursor = contentResolver.query(returnUri, null, null, null, null)

            cursor?.let {
                it.moveToNext()
                val idColumnIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val contactID = it.getString(idColumnIndex)

                val hasPhoneNumberColumnIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val stringHasPhoneNumber = it.getString(hasPhoneNumberColumnIndex)

                if (stringHasPhoneNumber == "1") {
                    val cursorNum = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID, null, null)

                    //Get the first phone number
                    cursorNum?.let {
                        if (it.moveToNext()) {
                            val phoneNumberColumnIndex = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val stringNumber = cursorNum.getString(phoneNumberColumnIndex)
                            openDefaultSmsApp(stringNumber)
                            Log.d("LandingActivity", "Number $stringNumber")
                        }
                        it.close()
                    }
                } else {
                    Toast.makeText(this, R.string.no_phone_number_error_text, Toast.LENGTH_SHORT).show()
                }
                it.close()
            }
            cursor.close()
        }
    }
}
