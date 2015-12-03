package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import de.velcommuta.denul.R;
import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.HKDFKeyExpansion;
import de.velcommuta.denul.crypto.KeyExchange;
import de.velcommuta.denul.crypto.KeySet;

/**
 * Activity containing the flow for adding a new friend
 */
public class FriendAddActivity extends AppCompatActivity implements
        FriendAddTechSelectionFragment.TechSelectionListener,
        FriendAddNearbyFragment.KexProvider{

    private static final String TAG = "FriendAddActivity";

    private KeyExchange mKex;
    private KeySet mKeyset;

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
    }
}
