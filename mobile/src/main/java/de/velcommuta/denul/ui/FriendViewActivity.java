package de.velcommuta.denul.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import de.velcommuta.denul.R;
import de.velcommuta.denul.crypto.KeySet;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.Friend;
import de.velcommuta.denul.util.FriendManager;

/**
 * Activity to show details about a specific user
 */
public class FriendViewActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "FriendViewActivity";

    private DatabaseServiceBinder mDbBinder;
    private int mFriendId;
    private Friend mFriend;
    private KeySet mKeyset;

    private TextView mFriendName;
    private TextView mFriendVerificationWarning;
    private ImageView mFriendQr;
    private ImageView mFriendVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_show);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        requestDatabaseBinder();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            mFriendId = b.getInt("friend-id");
        } else {
            Log.e(TAG, "onCreate: No Bundle passed, returning");
            finish();
        }
        mFriendName = (TextView) findViewById(R.id.friend_show_name);
        mFriendVerification = (ImageView) findViewById(R.id.friend_show_verification);
        mFriendQr = (ImageView) findViewById(R.id.friend_show_qr);
        mFriendVerificationWarning = (TextView) findViewById(R.id.friend_show_verification_warning);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_friend_view, menu);
        return true;
    }

    /**
     * Load the information about the friend from the database and display it
     */
    private void loadFriendInformation() {
        mFriend = FriendManager.getFriendById(mFriendId, mDbBinder);
        mKeyset = FriendManager.getKeySetForFriend(mFriend, mDbBinder);
        mFriendName.setText(mFriend.getName());
        switch (mFriend.getVerified()) {
            case Friend.VERIFIED_OK:
                mFriendVerification.setImageDrawable(getResources().getDrawable(R.drawable.ic_circle));
                mFriendVerification.getDrawable().setTint(getResources().getColor(android.R.color.holo_green_light));
                mFriendVerificationWarning.setVisibility(View.GONE);
                break;
            case Friend.UNVERIFIED:
                mFriendVerification.setImageDrawable(getResources().getDrawable(R.drawable.ic_circle));
                mFriendVerification.getDrawable().setTint(getResources().getColor(android.R.color.holo_orange_light));
                mFriendVerificationWarning.setVisibility(View.GONE);
                break;
            case Friend.VERIFIED_FAIL:
                mFriendVerification.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning));
                mFriendVerification.getDrawable().setTint(getResources().getColor(android.R.color.holo_red_dark));
                mFriendVerificationWarning.setText(R.string.friend_verify_warn);
                mFriendVerificationWarning.setVisibility(View.VISIBLE);
                // TODO Give a more obvious error message, too
                break;
        }
        // Get a new QRCodeWriter to generate our QRCode
        QRCodeWriter writer = new QRCodeWriter();
        // Detect the activity's background color to match the background color of the QR code to.
        // Color detection adapted from http://stackoverflow.com/a/3668872/1232833
        // QRCode generation code adapted from http://stackoverflow.com/a/25283174/1232833
        TypedArray array = getTheme().obtainStyledAttributes(new int[] {android.R.attr.colorBackground});
        int backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();
        try {
            // Get a BitMatrix representing the QRCode
            BitMatrix bitMatrix = writer.encode(mKeyset.fingerprint(), BarcodeFormat.QR_CODE, 512, 512);
            // Convert the bit matrix to a bitmap representation
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : backgroundColor);
                }
            }
            // Display the bitmap
            mFriendQr.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    /**
     * Request a binder to the database service
     */
    private void requestDatabaseBinder() {
        if (!DatabaseService.isRunning(this)) {
            Log.w(TAG, "bindDbService: Trying to bind to a non-running database service. Aborting");
        }
        Intent intent = new Intent(this, DatabaseService.class);
        if (!bindService(intent, this, 0)) {
            Log.e(TAG, "bindDbService: An error occured during binding :(");
        } else {
            Log.d(TAG, "bindDbService: Database service binding request sent");
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
        loadFriendInformation();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: Lost DB service");
        mDbBinder = null;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_delete:
                askDeleteConfirm();
                return true;
            case R.id.action_rename:
                performRename();
                return true;
            case R.id.action_scan:
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setBeepEnabled(false);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.initiateScan();
                return true;
        }
        return false;
    }


    /**
     * Ask the user to confirm the deletion request, and perform the deletion if it was confirmed
     */
    private void askDeleteConfirm() {
        // Prepare a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the values, build and show the dialog
        builder.setMessage("Delete this contact?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FriendManager.deleteFriend(mFriend, mDbBinder);
                        Toast.makeText(FriendViewActivity.this, "Contact deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Convert to IntentResult
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // Check if the parsing has succeeded
        if (result != null) {
            // Check if the result contains a scanned value
            if (result.getContents() == null) {
                // Value is null => Scan was cancelled
                Log.w(TAG, "onActivityResult: Barcode scan was cancelled");
            } else {
                // Value is not null, pass it to the verification function
                verifyFingerprint(result.getContents());
            }
        }
    }


    /**
     * Verify if a scanned fingerprint matches the expected fingerprint
     * @param fingerprint The scanned fingerprint
     */
    private void verifyFingerprint(String fingerprint) {
        if (fingerprint.equals(mKeyset.fingerprint())) {
            mFriend.setVerified(Friend.VERIFIED_OK);
            Toast.makeText(FriendViewActivity.this, "Friend Verified", Toast.LENGTH_SHORT).show();
        } else {
            mFriend.setVerified(Friend.VERIFIED_FAIL);
            Toast.makeText(FriendViewActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
        }
        // Perform update in the database
        FriendManager.updateFriend(mFriend, mDbBinder);
        // Reload friend
        loadFriendInformation();
    }


    /**
     * Rename the Friend and write the updated friend to the database
     */
    private void performRename() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText newName = new EditText(this);
        newName.setText(mFriend.getName());
        builder.setView(newName);
        builder.setTitle("Enter a new Name:");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selectedName = newName.getText().toString().trim();
                if (selectedName.equals("")) {
                    Toast.makeText(FriendViewActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                // if the name has not changed, do nothing
                if (selectedName.equals(mFriend.getName())) return;
                // check if the name is available
                if (FriendManager.isNameAvailable(selectedName, mDbBinder)) {
                    // Name is available. Update Friend object
                    mFriend.setName(selectedName);
                    // Update database
                    FriendManager.updateFriend(mFriend, mDbBinder);
                    // Reload
                    loadFriendInformation();
                } else {
                    Toast.makeText(FriendViewActivity.this, "Name already taken", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set cancel buttel
        builder.setNegativeButton("Cancel", null);
        // Create and show the dialog
        builder.create().show();
    }
}
