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

    public static final int VALUE_RUNNING = 0;
    public static final int VALUE_CYCLING = 1;

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
            case VALUE_CYCLING:
                track.setMode(DataContainer.Track.ModeOfTransport.MODE_CYCLING);
                break;
            case VALUE_RUNNING:
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
            mode = VALUE_CYCLING;
        } else if (track.getMode() == DataContainer.Track.ModeOfTransport.MODE_RUNNING) {
            mode = VALUE_RUNNING;
        } else {
            Log.e(TAG, "fromProtobuf: Unknown Mode of transport, defaulting to running");
            mode = VALUE_RUNNING;
        }
        return new GPSTrack(locList, track.getName(), mode);
    }


    /**
     * Check if two GPS-Tracks are equal. Two tracks are equal if all their fields and all Locations
     * match,
     * @param cmp The GPSTrack to compare to
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object cmp) {
        // Check if comparable object is a GPSTrack
        if (!(cmp instanceof GPSTrack)) return false;
        // Cast it to GPSTrack
        GPSTrack cmptrack = (GPSTrack) cmp;
        // Check if session name and mode of transportation match
        if (!(cmptrack.getSessionName().equals(getSessionName()) &&
              cmptrack.getModeOfTransportation() == getModeOfTransportation())) return false;
        // Check if the location lists have the same length
        if (cmptrack.getPosition().size() != getPosition().size()) return false;
        // Check if the locations match
        for (Location cmploc : cmptrack.getPosition()) {
            boolean rv = false;
            for (Location myLoc : getPosition()) {
                if (cmploc.getLatitude() == myLoc.getLatitude() &&
                        cmploc.getLatitude() == myLoc.getLongitude() &&
                        cmploc.getTime() == myLoc.getTime()) {
                    rv = true;
                    break;
                }
            }
            if (!rv) return false;
        }
        return true;
    }
}
