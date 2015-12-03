package de.velcommuta.denul.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import de.velcommuta.denul.R;
import de.velcommuta.denul.crypto.KeySet;


/**
 * Fragment to display a verification screen
 */
public class FriendAddVerificationFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "FriendAddVerif";

    private VerificationListener mListener;
    private ImageView mQrCodeView;
    private Button mScanButton;
    private String mFingerprint;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StepCountFragment.
     */
    public static FriendAddVerificationFragment newInstance() {
        return new FriendAddVerificationFragment();
    }


    /**
     * Required empty constructor
     */
    public FriendAddVerificationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_friend_verify, container, false);
        // Grab references to buttons, views
        mQrCodeView = (ImageView) v.findViewById(R.id.addfriend_step3_verify_qrcode);
        mScanButton = (Button) v.findViewById(R.id.addfriend_step3_verify_scanbutton);
        // Set up onClickListener
        mScanButton.setOnClickListener(this);
        // Load the fingerprint, generate QRCode
        loadFingerprintQrCode();
        return v;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (VerificationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement TechSelectionListener");
        }
    }


    /**
     * Load the fingerprint and render it in the ImageView.
     * QRCode generation code adapted from http://stackoverflow.com/a/25283174/1232833
     */
    private void loadFingerprintQrCode() {
        // Get the fingerprint from the hosting activity
        mFingerprint = mListener.getFingerprint();
        // Get a new QRCodeWriter to generate our QRCode
        QRCodeWriter writer = new QRCodeWriter();
        // Detect the activity's background color to match the background color of the QR code to.
        // Color detection adapted from http://stackoverflow.com/a/3668872/1232833
        TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[] {android.R.attr.colorBackground});
        int backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();
        try {
            // Get a BitMatrix representing the QRCode
            BitMatrix bitMatrix = writer.encode(mFingerprint, BarcodeFormat.QR_CODE, 512, 512);
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
            mQrCodeView.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    /**
     * Verify a scanned fingerprint
     * @param fingerprint The scanned fingerprint
     * TODO Do something useful with it
     */
    private void verifyFingerprint(String fingerprint) {
        if (fingerprint.equals(mFingerprint)) {
            Toast.makeText(getActivity(), "Equal", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Not Equal", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mScanButton) {
            // Scan button clicked.
            // Scanning code adapted from the zxing-android-embedded sample
            IntentIntegrator integrator = IntentIntegrator.forFragment(this);
            integrator.setBeepEnabled(false);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.initiateScan();
        }
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
     * Interface for communication with the hosting activity
     */
    public interface VerificationListener {
        /**
         * Get the Fingerprint of the agreed-upon {@link KeySet} from the hosting activity
         * @return The Fingerprint, as string
         */
        String getFingerprint();

        /**
         * Indicate to the hosting activity that the user has clicked the "continue" button
         * @param verified True if the fingerprint has been successfully verified, false otherwise
         */
        void continueClicked(boolean verified);
    }
}
