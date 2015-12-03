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
        mQrCodeView = (ImageView) v.findViewById(R.id.addfriend_step3_verify_qrcode);
        mScanButton = (Button) v.findViewById(R.id.addfriend_step3_verify_scanbutton);
        mScanButton.setOnClickListener(this);
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
        mFingerprint = mListener.getFingerprint();
        QRCodeWriter writer = new QRCodeWriter();
        TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[] {android.R.attr.colorBackground});
        int backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();
        try {
            BitMatrix bitMatrix = writer.encode(mFingerprint, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : backgroundColor);
                }
            }
            mQrCodeView.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    /**
     * Verify a scanned fingerprint
     * @param fingerprint The scanned fingerprint
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
            IntentIntegrator integrator = IntentIntegrator.forFragment(this);
            integrator.setBeepEnabled(false);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.initiateScan();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.w(TAG, "onActivityResult: Barcode scan was cancelled");
            } else {
                verifyFingerprint(result.getContents());
            }
        } else {
            Toast.makeText(getActivity(), "Scan cancelled", Toast.LENGTH_SHORT).show();
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
