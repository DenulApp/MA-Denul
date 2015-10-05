package de.velcommuta.denul.crypto;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * Hybrid Cryptography
 */
public class Hybrid {
    // TODO Encryption and decryption should be split into more functions to be more readable
    // Logging tag
    private static final String TAG = "Hybrid";

    // Insert provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
    
    ///// Constants for hybrid encryption header values
    // Version numbers
    protected static final byte VERSION_1 = 0x00;
    // Algorithm identifiers
    protected static final byte ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM = 0x00;

    // Constants for hybrid encryption header lengths
    private static final int BYTES_HEADER_VERSION     = 1;
    private static final int BYTES_HEADER_ALGO        = 1;
    private static final int BYTES_HEADER_SEQNR       = 4;
    private static final int BYTES_HEADER_LENGTH_ASYM = 4;

    // Number of bytes of the complete header
    private static final int BYTES_HEADER = BYTES_HEADER_VERSION + BYTES_HEADER_ALGO
            + BYTES_HEADER_LENGTH_ASYM + BYTES_HEADER_SEQNR;

    // Offsets
    private static final int OFFSET_VERSION     = 0;
    private static final int OFFSET_ALGO        = OFFSET_VERSION + BYTES_HEADER_VERSION;
    private static final int OFFSET_SEQNR       = OFFSET_ALGO + BYTES_HEADER_ALGO;
    private static final int OFFSET_LENGTH_ASYM = OFFSET_SEQNR + BYTES_HEADER_SEQNR;


    ///// Encryption and decryption
    /**
     * Perform a hybrid encryption on the provided data, using AES256 and the provided RSA public key.
     * @param data Data
     * @param pubkey One RSA public key
     * @param seqnr The sequence number of the data packet
     * @return The encrypted data, or null in case of an error
     */
    public static byte[] encryptHybrid(byte[] data, PublicKey pubkey, int seqnr) {
        // Check that the provided data or key are not null
        if (data == null || pubkey == null ) {
            Log.e(TAG, "encryptHybrid: data or public key is null");
            return null;
        }
        /*
        The ordering of encryption operations may seem counter-intuitive, as we are encrypting the
        symmetric key first and then doing the symmetric cryptographic operations. This is due to
        the fact that we are using AES in an AEAD mode, and we'd like to authenticate the header
        as associated data. For this to work, the header has to be complete at the time of the AES
        encryption. So, we could either null the "length of asymmetrically encrypted data"-field,
        or we can just perform the asym. encryption first and do the AES encryption later, and
        thus also prevent any shenanigans with the length field.
        */
        // Generate symmetric secret key
        byte[] sKey = AES.generateAES256Key();
        // Encrypt the secret key asymetrically
        byte[] asymCiphertext;
        try {
            asymCiphertext = RSA.encryptRSA(sKey, pubkey);
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG, "encryptHybrid: IllegalBlocksizeException during asym. encryption, aborting");
            return null;
        }
        // Check that nothing went wrong
        if (asymCiphertext == null) {
            Log.e(TAG, "encryptHybrid: Asymmetric encryption failed, aborting");
            return null;
        }
        // Generate header (which we need for AEAD)
        byte[] header = generateHeader(VERSION_1, ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM,
                asymCiphertext.length, seqnr);
        // symmetrically encrypt data
        byte[] symCiphertext = AES.encryptAES(data, sKey, header);
        // Check that nothing went wrong
        if (symCiphertext == null) {
            Log.e(TAG, "encryptHybrid: Symmetric encryption failed, aborting");
            return null;
        }
        // Encrypt the key

        // Generate output array of proper size
        byte[] output = new byte[header.length + asymCiphertext.length + symCiphertext.length];
        // Write header to output, starting at 0
        System.arraycopy(header, 0, output, 0, header.length);
        // Write asym ciphertext to output, starting after header
        System.arraycopy(asymCiphertext, 0, output, header.length, asymCiphertext.length);
        // Write symCiphertext to output, starting after asymCiphertext
        System.arraycopy(symCiphertext, 0, output, header.length + asymCiphertext.length, symCiphertext.length);
        // Return the output value
        return output;
    }


    /**
     * Decrypts a hybrid-encrypted block of data
     * @param ciphertext hybrid-encrypted data
     * @param privkey private key to decrypt the data with
     * @param seqnr The expected sequence number, or -1, if it should not be verified
     * @return The unencrypted data, as a byte[], or null, if something went wrong
     * @throws BadPaddingException If one of the decryptions throws it. Indicates that the authentication checks failed
     */
    public static byte[] decryptHybrid(byte[] ciphertext, PrivateKey privkey, int seqnr) throws BadPaddingException {
        // Check if the ciphertext and key are actually set
        if (ciphertext == null || privkey == null) {
            Log.e(TAG, "decryptHybrid: One of the inputs is null, aborting");
            return null;
        }
        // Retrieve the header
        byte[] header = getHeader(ciphertext);
        // Perform some sanity checks on the header
        if (header[0] != VERSION_1) {
            Log.e(TAG, "decryptHybrid: Unknown version number");
            throw new BadPaddingException("Unknown version number");
        } else if (header[1] != ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM) {
            Log.e(TAG, "decryptHybrid: Unknown algorithm specification");
            throw new BadPaddingException("Unknown algorithm specification");
        }
        if (seqnr != -1) {
            if (parseSeqNr(header) != seqnr) {
                Log.e(TAG, "decryptHybrid: Wrong sequence number in header");
                throw new BadPaddingException("Wrong sequence number");
            }
        } else {
            Log.d(TAG, "decryptHybrid: Sequence number verification skipped");
        }
        // Parse the length of the asymmetrically encrypted ciphertext block from the header
        int asymCiphertextLength = parseAsymCiphertextLength(header);
        // Perform sanity checks
        if (header.length + asymCiphertextLength > ciphertext.length) {
            Log.e(TAG, "decryptHybrid: Incorrect asymCiphertextLength specified");
            throw new BadPaddingException("Incorrect asymCiphertextLength");
        }
        // Retrieve asymCiphertext
        byte[] asymCiphertext = Arrays.copyOfRange(ciphertext, header.length, header.length + asymCiphertextLength);
        // Retrieve symCiphertext
        byte[] symCiphertext = Arrays.copyOfRange(ciphertext, header.length + asymCiphertextLength, ciphertext.length);
        // Decrypt symmetric key from asymCiphertext
        byte[] symKey;
        try {
            symKey = RSA.decryptRSA(asymCiphertext, privkey);
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG, "decryptHybrid: Illegal Block Size Exception, aborting");
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "decryptHybrid: Array Index out of bounds indicates incorrect length in header, aborting");
            throw new BadPaddingException("Incorrect asymCiphertextLength");
        }
        // Ensure that decryption was successful
        if (symKey == null) {
            Log.e(TAG, "decryptHybrid: Something went wrong during asym. decryption, aborting");
            return null;
        }
        // Decrypt symCiphertext
        byte[] cleartext = AES.decryptAES(symCiphertext, symKey, header);
        // Ensure that decryption was successful
        if (cleartext == null) {
            Log.e(TAG, "decryptHybrid: Something went wrong during sym. decryption, aborting");
            return null;
        }
        // Return decrypted data
        return cleartext;
    }


    ///// Header generation and parsing
    /**
     * Generate a header for a hybrid-encrypted packet
     * @param asymCipherLength Length of the asymmetrically encrypted ciphertext
     * @param version Version number, as byte (use the constants provided by this class)
     * @param algo Algorithm descriptor, as byte (use the constants provided by this class)
     * @param seq The sequence number of the packet
     * @return The header, as a byte[]
     */
    protected static byte[] generateHeader(byte version, byte algo, int asymCipherLength, int seq) {
        // Create a new byte[] for the header
        byte[] header = new byte[BYTES_HEADER];
        // Set version and algorithm
        header[0] = version;
        header[1] = algo;
        // Set sequence number
        byte[] seqNr = ByteBuffer.allocate(BYTES_HEADER_SEQNR).putInt(seq).array();
        System.arraycopy(seqNr, 0, header, OFFSET_SEQNR, seqNr.length);
        // Set length of asymmetrically enciphered ciphertext
        byte[] asymCipherLengthBytes = ByteBuffer.allocate(4).putInt(asymCipherLength).array();
        System.arraycopy(asymCipherLengthBytes, 0, header, OFFSET_LENGTH_ASYM, asymCipherLengthBytes.length);
        return header;
    }


    /**
     * Get the header from an encrypted blob of data
     * @param message The whole message (headers plus encrypted contents)
     * @return The header
     * @throws BadPaddingException If the message is shorter than the header
     */
    protected static byte[] getHeader(byte[] message) throws BadPaddingException {
        if (message.length < BYTES_HEADER) {
            Log.e(TAG, "getHeader: message shorter than header, aborting");
            throw new BadPaddingException("Incorrect header");
        }
        return Arrays.copyOfRange(message, 0, BYTES_HEADER);
    }


    /**
     * Parses the length of the asymmetrically encrypted ciphertext from the header
     * @param header The full header
     * @return The length, as int
     * @throws BadPaddingException If the header has an incorrect length
     */
    protected static int parseAsymCiphertextLength(byte[] header) throws BadPaddingException{
        if (header.length != BYTES_HEADER) {
            Log.e(TAG, "parseAsymCiphertextLength: Malformed header");
            throw new BadPaddingException("Malformed hybrid header");
        }
        return ByteBuffer.wrap(
                Arrays.copyOfRange(header, OFFSET_LENGTH_ASYM, OFFSET_LENGTH_ASYM + BYTES_HEADER_LENGTH_ASYM)
        ).getInt();
    }


    /**
     * Parses the sequence number from the header
     * @param header The full header
     * @return The length, as int
     * @throws BadPaddingException If the header has an incorrect length
     */
    protected static int parseSeqNr(byte[] header) throws BadPaddingException{
        if (header.length != BYTES_HEADER) {
            Log.e(TAG, "parseAsymCiphertextLength: Malformed header");
            throw new BadPaddingException("Malformed hybrid header");
        }
        return ByteBuffer.wrap(
                Arrays.copyOfRange(header, OFFSET_SEQNR, OFFSET_SEQNR + BYTES_HEADER_SEQNR)
        ).getInt();
    }
}
