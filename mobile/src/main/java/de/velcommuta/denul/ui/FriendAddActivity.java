package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
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

    private DatabaseServiceBinder mDbBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);
        // Prepare Key Exchange object
        Log.d(TAG, "onCreate: Creating Kex");
        mKex = new ECDHKeyExchange();
        Log.d(TAG, "onCreate: Creating Kex done");
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
        // TODO Implement proper backstack behaviour, it's broken at the moment
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
    public void continueClicked(int verificationStatus) {
        if (verificationStatus == FriendAddVerificationFragment.VERIFY_FAIL) {
            // The verification failed, the fingerprints did not match
            // TODO Display error with option to redo everything or abort
        } else if (verificationStatus == FriendAddVerificationFragment.VERIFY_NOT_DONE) {
            // No verification attempt has taken place
            // TODO Display warning with option to continue without verification
        } else {
            // Verification was successful
            if (mDbBinder != null && mDbBinder.isDatabaseOpen()) {
                Friend contact = new Friend();
                contact.setVerified(verificationStatus);
                contact.setName(mKeyset.fingerprint());  // TODO Replace with proper nickname
                mDbBinder.addFriend(contact, mKeyset);
                Log.d(TAG, "continueClicked: Successfully inserted contact into database");
                finish();
            } else {
                Log.e(TAG, "continueClicked: Database unavailable");
            }
        }
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
}
