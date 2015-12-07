package de.velcommuta.denul.data;

import android.location.Location;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.data.proto.DataContainer;
import de.velcommuta.denul.db.LocationLoggingContract;

/**
 * Event to indicate a finished track
 */
public class GPSTrack implements Shareable {
    private static final String TAG = "GPSTrack";

    private List<Location> mPosition;
    private String mSessionName;
    private int mModeOfTransportation;

    /**
     * Constructor to pass a list of positions, a name, and a mode of transportation (defined in
     * LocationLoggingContract.LocationSessions) to the subscriber
     * @param pos List of positions
     * @param name Name of Session
     * @param mode Code for mode of transportation, as defined in LocationLoggingContract.LocationSessions
     */
    public GPSTrack(List<Location> pos, String name, int mode) {
        mPosition = pos;
        mSessionName = name;
        mModeOfTransportation = mode;
    }


    /**
     * @return Saved list of positions
     */
    public List<Location> getPosition() {
        return mPosition;
    }

    /**
     * @return Defined name of session, if any
     */
    public String getSessionName() {
        return mSessionName;
    }

    /**
     * @return Defined mode of transportation, if any
     */
    public int getModeOfTransportation() {
        return mModeOfTransportation;
    }


    @Override
    public int getType() {
        return SHAREABLE_TRACK;
    }


    @Override
    public byte[] getByteRepresentation() {
        // Get wrapper and Track builders
        DataContainer.Wrapper.Builder wrapper = DataContainer.Wrapper.newBuilder();
        DataContainer.Track.Builder track = DataContainer.Track.newBuilder();
        // Set the name
        track.setName(mSessionName);
        // Set the mode of transportation
        switch (mModeOfTransportation) {
            case LocationLoggingContract.LocationSessions.VALUE_CYCLING:
                track.setMode(DataContainer.Track.ModeOfTransport.MODE_CYCLING);
                break;
            case LocationLoggingContract.LocationSessions.VALUE_RUNNING:
                track.setMode(DataContainer.Track.ModeOfTransport.MODE_RUNNING);
                break;
            default:
                Log.e(TAG, "getByteRepresentation: Unknown Mode of transportation, setting to running");
                track.setMode(DataContainer.Track.ModeOfTransport.MODE_RUNNING);
                break;
        }
        // Add the location entries
        for (Location cLoc : mPosition) {
            DataContainer.Track.Entry.Builder entry = DataContainer.Track.Entry.newBuilder();
            entry.setTimestamp(cLoc.getTime());
            entry.setLat(cLoc.getLatitude());
            entry.setLng(cLoc.getLongitude());
            track.addTrack(entry);
        }
        // Pack in wrapper
        wrapper.setTrack(track);
        // Build, serialize and return wrapper
        return wrapper.build().toByteArray();
    }


    /**
     * Deserialization function to deserialize a {@link de.velcommuta.denul.data.proto.DataContainer.Track}
     * representing a GPSTrack into a GPSTrack
     * @param track The protobuf representation of a GPSTrack (without the Wrapper)
     * @return The GPSTrack object represented by the passed {@link de.velcommuta.denul.data.proto.DataContainer.Track}
     */
    public static GPSTrack fromProtobuf(DataContainer.Track track) {
        List<Location> locList = new LinkedList<>();
        for (DataContainer.Track.Entry entry : track.getTrackList()) {
            // Create empty location object
            Location loc = new Location("");
            // Set Latitude, Longitude, timestamp
            loc.setLatitude(entry.getLat());
            loc.setLongitude(entry.getLng());
            loc.setTime(entry.getTimestamp());
            // Add to list
            locList.add(loc);
        }
        // Translate Mode of Transportation to constant used in code
        int mode;
        if (track.getMode() == DataContainer.Track.ModeOfTransport.MODE_CYCLING) {
            mode = LocationLoggingContract.LocationSessions.VALUE_CYCLING;
        } else if (track.getMode() == DataContainer.Track.ModeOfTransport.MODE_RUNNING) {
            mode = LocationLoggingContract.LocationSessions.VALUE_RUNNING;
        } else {
            Log.e(TAG, "fromProtobuf: Unknown Mode of transport, defaulting to running");
            mode = LocationLoggingContract.LocationSessions.VALUE_RUNNING;
        }
        return new GPSTrack(locList, track.getName(), mode);
    }
}
