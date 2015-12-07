package de.velcommuta.denul.data;

/**
 * Interface implemented by all sharable objects (e.g. run tracks, heart rates, ...).
 * Each implementing class SHOULD also have a static fromByteRepresentation function taking a byte[]
 * as returned from {@link Shareable#getByteRepresentation()} and returning a {@link Shareable}
 * object that contains the data from the byte[] representation.
 */
public interface Shareable {
    int SHAREABLE_TRACK = 0;
    int SHAREABLE_STEPCOUNT = 1;

    /**
     * Function to indicate which type the implementing class is. One of the SHAREABLE_* constants
     * defined in the {@link Shareable} interface
     * @return One of the SHAREABLE_* constants indicating which type the object is of
     */
    int getType();

    /**
     * Generate and return a byte[]-representation of the object.
     * TODO State which Protobuf construct should be used
     * @return A byte[]-representation of the object
     */
    byte[] getByteRepresentation();
}
