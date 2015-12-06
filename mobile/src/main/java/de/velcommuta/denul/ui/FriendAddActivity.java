package de.velcommuta.denul.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import de.velcommuta.denul.R;
import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.HKDFKeyExpansion;
import de.velcommuta.denul.crypto.KeyExchange;
import de.velcommuta.denul.crypto.KeySet;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.Friend;

import net.sqlcipher.database.SQLiteException;

/**
 * Activity containing the flow for adding a new friend
 */
public class FriendAddActivity extends AppCompatActivity implements
        FriendAddTechSelectionFragment.TechSelectionListener,
        FriendAddNearbyFragment.KexProvider,
        FriendAddVerificationFragment.VerificationListener,
        ServiceConnection {

    private static final String TAG = "FriendAddActivity";

    private KeyExchange mKex;
    private KeySet mKeyset;
    private Friend mFriend;

    private DatabaseServiceBinder mDbBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);
        // Load tech selection fragment
        loadTechSelectFragment();
        bindDbService();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from database service
        unbindService(this);
    }


    /**
     * Load the tech selection fragment
     */
    private void loadTechSelectFragment() {
        Log.d(TAG, "loadTechSelectionFragment: Creating Kex");
        mKex = new ECDHKeyExchange();
        Log.d(TAG, "loadTechSelectionFragment: Creating Kex done");
        Fragment fragment = FriendAddTechSelectionFragment.newInstance();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.friend_add_container, fragment)
                .commit();
    }


    /**
     * Replace the currently active fragment with the Google Nearby fragment
     */
    private void slideInNearbyFragment() {
        // Get new fragment instance
        FriendAddNearbyFragment fr = FriendAddNearbyFragment.newInstance();
        // Perform replacement
        slideForwardReplace(fr);
    }


    /**
     * Replace the currently active fragment with the verification fragment
     */
    private void slideInVerificationFragment() {
        // Get new fragment instance
        FriendAddVerificationFragment fr = FriendAddVerificationFragment.newInstance();
        // Perform replacement
        slideForwardReplace(fr);
    }


    /**
     * Replace a fragment by sliding the old one out to the left, and the new one in from the right.
     * Based on http://stackoverflow.com/a/4819665/1232833
     * @param fragment The new fragment
     */
    private void slideForwardReplace(Fragment fragment) {
        // Get a fragment transition
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // Set up the animations to use
        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        // Perform the replacement
        ft.replace(R.id.friend_add_container, fragment);
        // Add to back stack
        ft.addToBackStack(null);
        // Commit transaction
        ft.commit();
    }


    @Override
    public void techSelection(int tech) {
        if (tech == FriendAddTechSelectionFragment.TECH_NEARBY) {
            slideInNearbyFragment();
        }
    }

    @Override
    public void putKexData(byte[] kex) {
        if (mKex != null) {
            Log.d(TAG, "putKexData: Putting Kex data");
            mKex.putPartnerKexData(kex);
        }
    }

    @Override
    public byte[] getPublicKexData() {
        if (mKex != null) {
            Log.d(TAG, "getPublicKexData: Returning public Key Exchange data");
            return mKex.getPublicKexData();
        } else {
            return null;
        }
    }

    @Override
    public void kexDone(boolean isInitiating) {
        byte[] key = mKex.getAgreedKey();
        mKeyset = new HKDFKeyExpansion(key).expand(isInitiating);
        Log.d(TAG, "kexDone: Generated keys");
        slideInVerificationFragment();
    }


    @Override
    public String getFingerprint() {
        return mKeyset.fingerprint();
    }


    @Override
    public void continueClicked(int verificationStatus, String name) {
        if (name == null || name.equals("") || name.equals("Name")) {
            // The name is not set
            informNameUnset();
            return;
        }
        mFriend = new Friend();
        mFriend.setName(name);
        mFriend.setVerified(verificationStatus);
        if (verificationStatus == Friend.VERIFIED_FAIL) {
            // The verification failed, the fingerprints did not match
            informFailedVerifyOptions();
        } else if (verificationStatus == Friend.UNVERIFIED) {
            // No verification attempt has taken place, ask the user if she is sure
            askConfirmUnverifiedSafe();
        } else {
            // Verification was successful
            saveContactToDatabase();
        }
    }


    /**
     * Save the contact to the database with the current values
     */
    private void saveContactToDatabase() {
        if (mDbBinder != null && mDbBinder.isDatabaseOpen() && mFriend != null && mKeyset != null) {
            try {
                // Insert into database
                mDbBinder.addFriend(mFriend, mKeyset);
                // Log
                Log.d(TAG, "saveContactToDatabase: Saved.");
                // Stop activity, return to friendlist
                finish();
            } catch (SQLiteException e) {
                AlertDialog ConnectionRequestDialog = new AlertDialog.Builder(this)
                        .setTitle("Name taken")
                        .setMessage("The name you have entered is already in use.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Silence
                                // Crickets
                                // Tumbleweed
                            }
                        }).create();
                ConnectionRequestDialog.show();
            }
        }
    }


    /**
     * Helper function to display a message asking the user if she really wants to save a new
     * contact without having verified the fingerprints
     */
    private void informNameUnset() {
        // Prepare an AlertDialog to inform the user
        AlertDialog ConnectionRequestDialog = new AlertDialog.Builder(this)
                .setTitle("Please name your contact")
                .setMessage("You have not entered a name for this contact.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Silence
                        // Crickets
                        // Tumbleweed
                    }
                }).create();
        ConnectionRequestDialog.show();
    }


    /**
     * Helper function to display a message asking the user if she really wants to save a new
     * contact without having verified the fingerprints
     */
    private void askConfirmUnverifiedSafe() {
        // Prepare an AlertDialog to inform the user
        AlertDialog ConnectionRequestDialog = new AlertDialog.Builder(this)
                .setTitle("Save without verification")
                .setMessage("Do you really want to save this friend without verification?") // TODO Placeholder text
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User wants to save anyway => Do it
                        saveContactToDatabase();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User has decided to perform the validation, do nothing
                    }
                }).create();
        ConnectionRequestDialog.show();
    }


    /**
     * Helper function to display a message telling the user that they cannot save a new contact if
     * the verification has explicitly failed, and offering ways to fix the situation
     */
    private void informFailedVerifyOptions() {
        // Prepare an AlertDialog to inform the user
        AlertDialog ConnectionRequestDialog = new AlertDialog.Builder(this)
                .setTitle("Verification failed")
                .setMessage("Verification of your friend failed. You can either try verifying them again, or start over.") // TODO Placeholder text
                .setCancelable(false)
                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User wants to try again. Do nothing => Show the verification dialog again
                    }
                })
                .setNegativeButton("Start over", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mKeyset = null;
                        mFriend = null;
                        mKex = null;
                        loadTechSelectFragment();
                    }
                }).create();
        ConnectionRequestDialog.show();
    }


    /**
     * Get a binder to the database service
     * @return true if the request was sent successfully, false otherwise
     */
    private boolean bindDbService() {
        if (!DatabaseService.isRunning(this)) {
            Log.w(TAG, "bindDbService: Trying to bind to a non-running database service. Aborting");
            return false;
        }
        Intent intent = new Intent(this, DatabaseService.class);
        if (!bindService(intent, this, 0)) {
            Log.e(TAG, "bindDbService: An error occured during binding :(");
            return false;
        } else {
            Log.d(TAG, "bindDbService: Database service binding request sent");
            return true;
        }
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: New service connection received");
        mDbBinder = (DatabaseServiceBinder) iBinder;
        // TODO Debugging code, move to passphrase activity once it is added
        if (!mDbBinder.isDatabaseOpen()) {
            mDbBinder.openDatabase("VerySecureHardcodedPasswordOlolol123");
        }
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i(TAG, "onServiceDisconnected: Lost DB binder");
        mDbBinder = null;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Count: " + getFragmentManager().getBackStackEntryCount());
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
        // Explicitly null the key exchange object
        if (mKex != null) mKex = new ECDHKeyExchange();
    }
}
