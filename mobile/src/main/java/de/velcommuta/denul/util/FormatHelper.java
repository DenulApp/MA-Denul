package de.velcommuta.denul.util;

/**
 * Formatting helper functions
 */
public class FormatHelper {
    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
    /**
     * Helper function to convert a byte[] into a hex string
     * @param bytes The byte[]
     * @return A hexadecimal string representation of the byte[]
     * Source: http://stackoverflow.com/a/9855338/1232833
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
