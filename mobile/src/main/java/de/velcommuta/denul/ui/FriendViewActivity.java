package de.velcommuta.denul.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.velcommuta.denul.R;
import de.velcommuta.denul.crypto.KeySet;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.Friend;
import de.velcommuta.denul.util.FriendManagement;

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
    private ImageView mFriendQr;
    private ImageView mFriendVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_show);
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
    }




    /**
     * Load the information about the friend from the database and display it
     */
    private void loadFriendInformation() {
        mFriend = FriendManagement.getFriendById(mFriendId, mDbBinder);
        mKeyset = FriendManagement.getKeySetForFriend(mFriend, mDbBinder);
        mFriendName.setText(mFriend.getName());
        switch (mFriend.getVerified()) {
            case Friend.VERIFIED_OK:
                mFriendVerification.setImageDrawable(getResources().getDrawable(R.drawable.ic_circle));
                mFriendVerification.getDrawable().setTint(getResources().getColor(android.R.color.holo_green_light));
                break;
            case Friend.UNVERIFIED:
                mFriendVerification.setImageDrawable(getResources().getDrawable(R.drawable.ic_circle));
                mFriendVerification.getDrawable().setTint(getResources().getColor(android.R.color.holo_orange_light));
                break;
            case Friend.VERIFIED_FAIL:
                mFriendVerification.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning));
                mFriendVerification.getDrawable().setTint(getResources().getColor(android.R.color.holo_red_dark));
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
}
