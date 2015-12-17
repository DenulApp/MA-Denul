package de.velcommuta.denul.data;

import de.velcommuta.denul.data.proto.DataContainer;

/**
 * Helper class to unwrap {@link Shareable} objects from a {@link de.velcommuta.denul.data.proto.DataContainer.Wrapper}.
 */
public class ShareableUnwrapper {
    /**
     * Unwrap the wrapper, returning the Shareable
     * @param wrapper The wrapper to unwrap
     * @return The unwrapped Shareable
     */
    public static Shareable unwrap(DataContainer.Wrapper wrapper) {
        if (wrapper.getShareableCase() == DataContainer.Wrapper.ShareableCase.TRACK) {
            return GPSTrack.fromProtobuf(wrapper.getTrack());
        } else {  // TODO Add new Shareables here
            return null;
        }
    }
}
