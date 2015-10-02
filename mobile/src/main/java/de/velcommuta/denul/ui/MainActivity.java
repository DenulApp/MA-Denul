package de.velcommuta.denul.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import de.velcommuta.denul.R;
import de.velcommuta.denul.db.SecureDbHelper;
import de.velcommuta.denul.db.VaultContract;
import de.velcommuta.denul.service.PedometerService;
import de.velcommuta.denul.util.Crypto;

/**
 * Main Activity - Launched on startup of the application
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        StartScreenFragment.OnFragmentInteractionListener,
        StepCountFragment.OnFragmentInteractionListener,
        HeartRateFragment.OnFragmentInteractionListener
{
    private SQLiteDatabase mSecureDatabaseHandler;

    public static final String INTENT_GPS_TRACK = "intent-gps-track";

    public static final String TAG = "MainActivity";

    ///// Activity lifecycle management
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // FIXME This may have to be moved to the new launch activity if it is ever changed
            // Load SQLCipher libs
            SQLiteDatabase.loadLibs(this);
            // Configure default EventBus instance
            //EventBus.builder().logNoSubscriberMessages(false)
            //        .sendNoSubscriberEvent(false)
            //        .installDefaultEventBus();
        }
        // Prepare DB and get handler
        // TODO If I ever add a different launch activity with a passphrase prompt, this will have to be moved
        new DbInitTask().execute(this);

        // Launch pedometer service if it is not running
        if (!isPedometerServiceRunning()) {
            String pubkey = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).getString(getString(R.string.preferences_keystore_rsapub), null);
            if (pubkey != null) {
                startPedometerService();
            } else {
                // We don't have a keypair ready! Let's generate one and start the service afterwards
                new KeypairGenerationTask().execute();
            }
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (getIntent().getAction().equals(INTENT_GPS_TRACK) && savedInstanceState == null) {
            Log.d(TAG, "onCreate: Forwarded to GPS tracking by Intent");
            loadTrackFragment();
        } else if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: No specific fragment requested, using default");
            loadHomeFragment();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSecureDatabaseHandler != null) mSecureDatabaseHandler.close();
        mSecureDatabaseHandler = null;
    }

    ///// Navigation callbacks
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_start) {
            loadHomeFragment();
        } else if (id == R.id.nav_tracks) {
            // Intent intent = new Intent(this, TrackDisplayActivity.class);
            // startActivity(intent);
            loadTrackFragment();
        } else if (id == R.id.nav_share) {
            // TODO implement
        } else if (id == R.id.nav_menu) {
            // TODO implement
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    ///// Fragment callbacks
    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    ///// GUI Utility functions
    /**
     * Load the homescreen fragment
     */
    private void loadHomeFragment() {
        Fragment fragment = StartScreenFragment.newInstance();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    /**
     * Load the GPS tracking fragment
     */
    private void loadTrackFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = TrackRunFragment.newInstance();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    ///// Utility functions
    /**
     * Checks if the Pedometer Service is running
     * @return True if the service is running, false otherwise
     */
    private boolean isPedometerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("de.velcommuta.denul.service.PedometerService".equals(service.service.getClassName())) { // TODO Update if
                return true; // Package name matches, our service is running
            }
        }
        return false; // No matching package name found => Our service is not running
    }

    /**
     * Starts the pedometer service
     */
    private void startPedometerService() {
        Intent intent = new Intent(this, PedometerService.class);
        startService(intent);
    }

    /**
     * Setter for the SQLiteDatabase, required by AsyncTask that creates it
     * @param helper The SQLiteDatabase handler
     */
    public void setSecureDatabaseHandler(SQLiteDatabase helper) {
        mSecureDatabaseHandler = helper;
    }

    /**
     * Getter for the LocationDatabaseHandler for the GPS Location Track database
     * @return SQLiteDatabase for GPS tracks
     */
    protected SQLiteDatabase getSecureDatabaseHandler() {
        return mSecureDatabaseHandler;
    }

    /**
     * Setter for the generated RSA keypair
     * @param keypair The generated KeyPair
     */
    @SuppressLint("CommitPrefEdits")
    private void setGeneratedKeypair(KeyPair keypair) {
        if (keypair == null) return; // If the keypair is null, something went wrong. Do nothing.
        Log.d(TAG, "setGeneratedKeypair: Got keypair, saving to database");
        if (mSecureDatabaseHandler != null) {
            // Begin a database transaction
            mSecureDatabaseHandler.beginTransaction();
            // Prepare database entry for the private key
            ContentValues keyEntry = new ContentValues();
            // Retrieve private key
            PrivateKey priv = keypair.getPrivate();
            // Set type to private RSA key
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_TYPE, VaultContract.KeyStore.TYPE_RSA_PRIV);
            // Add the actual key to the insert
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_BYTES, Crypto.encodeKey(priv));
            // Insert the values into the database
            mSecureDatabaseHandler.insert(VaultContract.KeyStore.TABLE_NAME, null, keyEntry);

            // Perform the same steps for the public key (as a backup)
            keyEntry = new ContentValues();
            PublicKey pub = keypair.getPublic();
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_TYPE, VaultContract.KeyStore.TYPE_RSA_PUB);
            String encodedPub = Crypto.encodeKey(pub);
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_BYTES,encodedPub);

            mSecureDatabaseHandler.insert(VaultContract.KeyStore.TABLE_NAME, null, keyEntry);

            // Finish the transaction
            mSecureDatabaseHandler.endTransaction();
            Log.d(TAG, "setGeneratedKeypair: Saved to database. Saving public key to SharedPreference");

            // Get a SharedPreferences.Editor
            SharedPreferences.Editor edit = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).edit();
            // Add the key and save
            edit.putString(getString(R.string.preferences_keystore_rsapub), encodedPub);
            // Set the sequence number to zero (used for limited freshness tests of encrypted data)
            edit.putInt(getString(R.string.preferences_keystore_seqnr), 0);
            edit.commit();
            Log.d(TAG, "setGeneratedKeypair: Saved into SharedPreference");

            if (!isPedometerServiceRunning()) {
                startPedometerService();
            }
        } else {
            Log.e(TAG, "setGeneratedKeypair: No open database handle found. Discarding keypair");
            Toast.makeText(MainActivity.this, "Could not save generated keys", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * AsyncTask to open the database and get a handler
     */
    private class DbInitTask extends AsyncTask<Context, Void, SQLiteDatabase> {
        private final String TAG = "DbInitTask";

        @Override
        protected SQLiteDatabase doInBackground(Context... ctx) {
            if (ctx.length == 0) return null;
            SecureDbHelper dbh = new SecureDbHelper(ctx[0]);
            Log.d(TAG, "doInBackground: Init DB - started");
            SQLiteDatabase db = dbh.getWritableDatabase("VerySecureHardcodedPasswordOlolol123"); // TODO Replace with proper password prompt

            // TODO Verify and fix contents of database (inconsistent session etc)
            Log.d(TAG, "doInBackground: Init DB - done");
            return db;
        }

        @Override
        protected void onPostExecute(SQLiteDatabase hlp) {
            setSecureDatabaseHandler(hlp);
        }
    }

    private class KeypairGenerationTask extends AsyncTask<Void,Void,KeyPair> {
        private final String TAG = "KeypairGenerationTask";
        private final int KEYSIZE = 4096;

        @Override
        protected KeyPair doInBackground(Void... v) {
            Log.d(TAG, "doInBackground: Beginning Keypair generation");
            return Crypto.generateRSAKeypair(KEYSIZE);
        }

        @Override
        protected void onPostExecute(KeyPair keys) {
            setGeneratedKeypair(keys);
        }
    }
}
