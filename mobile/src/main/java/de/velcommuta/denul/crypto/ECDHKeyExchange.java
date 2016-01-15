package de.velcommuta.denul.crypto;

import android.util.Log;

import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.spec.ECParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;

/**
 * Key exchange using ECDH with Curve25519.
 * Code partially based on these StackExchange posts:
 * Curve25519 parameters: http://stackoverflow.com/a/30014831/1232833
 * Key Exchange logic: http://stackoverflow.com/q/18285073/1232833
 * byte[]-to-PublicKey parsing: http://stackoverflow.com/a/4969415/1232833
 */
public class ECDHKeyExchange implements KeyExchange {
    // Logging Tag
    private static final String TAG = "ECDHKex";

    // KeyAgreement instance
    private KeyAgreement mKeyAgree;
    private KeyPair mKeypair;
    private boolean mPhaseSuccess = false;

    // Insert BouncyCastle provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * Public constructor. Initialize everything to prepare for a Curve25519 key exchange.
     * Curve25519 initialization based on http://stackoverflow.com/a/30014831/1232833
     */
    public ECDHKeyExchange() {
        // Get Curve25519 in X9.62 form
        X9ECParameters ecP = CustomNamedCurves.getByName("curve25519");
        // convert to JCE form
        ECParameterSpec ecSpec = new ECParameterSpec(ecP.getCurve(), ecP.getG(),
                ecP.getN(), ecP.getH(), ecP.getSeed());
        try {
            // Get Keypair generator based on the spec
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH", "SC");
            keyGen.initialize(ecSpec, new SecureRandom());
            // Generate a keypair
            mKeypair = keyGen.generateKeyPair();

            // Get a keyAgreement instance
            mKeyAgree = KeyAgreement.getInstance("ECDH", "SC");
            // Initialize the KeyAgreement
            mKeyAgree.init(mKeypair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            // We need to catch these exception, but they should never occur, as we are bundling
            // a Spongycastle version that includes these algorithms
            Log.e(TAG, "Constructor: NoSuchAlgorithm: ", e);
        } catch (NoSuchProviderException e) {
            Log.e(TAG, "Constructor: NoSuchProvider: ", e);
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Constructor: InvalidAlgorithmParameterException: ", e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Constructor: InvalidKeyException: ", e);
        }
    }


    @Override
    public byte[] getPublicKexData() {
        if (mKeypair != null) {
            return mKeypair.getPublic().getEncoded();
        } else {
            Log.e(TAG, "getPublicKexData: mKeypair == null");
            return null;
        }
    }


    @Override
    public boolean putPartnerKexData(byte[] data) {
        if (mPhaseSuccess) {
            Log.e(TAG, "putPartnerKexData: Already received kex data, ignoring");
            return false;
        }
        // Code based on http://stackoverflow.com/a/4969415/1232833
        // Import data into KeySpec
        X509EncodedKeySpec ks = new X509EncodedKeySpec(data);
        // Prepare a keyFactory
        KeyFactory kfac;
        try {
            // initialize key factory
            kfac = KeyFactory.getInstance("ECDH", "SC");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "putPartnerKexData: NoSuchAlgorithm: ", e);
            return false;
        } catch (NoSuchProviderException e) {
            Log.e(TAG, "putPartnerKexData: NoSuchProvider: ", e);
            return false;
        }

        // Prepare public key variable
        ECPublicKey remotePubkey;
        try {
            // Parse the public key
            remotePubkey = (ECPublicKey) kfac.generatePublic(ks);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "putPartnerKexData: Invalid data received!", e);
            return false;
        } catch (ClassCastException e) {
            Log.e(TAG, "putPartnerKexData: Key data was valid, but no ECPublicKey. ", e);
            return false;
        }

        try {
            mKeyAgree.doPhase(remotePubkey, true);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "putPartnerKexData: Invalid key: ", e);
            return false;
        }
        mPhaseSuccess = true;
        return true;
    }


    @Override
    public byte[] getAgreedKey() {
        if (mPhaseSuccess) {
            return mKeyAgree.generateSecret();
        } else {
            Log.e(TAG, "getAgreedKey: Key agreement has not concluded successfully!");
            return null;
        }
    }


    @Override
    public KeyPair getKeypair() {
        return mKeypair;
    }
}
