package de.velcommuta.denul.data;

import android.location.Location;
import android.util.Log;

import org.joda.time.DateTimeZone;

import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.data.proto.DataContainer;

/**
 * Event to indicate a finished track
 */
public class GPSTrack implements Shareable {
    private static final String TAG = "GPSTrack";

    private List<Location> mPosition;
    private String mSessionName;
    private int mModeOfTransportation;
    private long mTimestampStart;
    private long mTimestampEnd;
    private String mTimezone;
    private int mId = -1;
    private int mOwner;
    private String mDescription;

    private float mDistance = 0.0f;

    public static final int VALUE_RUNNING = 0;
    public static final int VALUE_CYCLING = 1;

    /**
     * Constructor to pass a list of positions, a name, and a mode of transportation (defined in
     * LocationLoggingContract.LocationSessions) to the subscriber
     * @param pos List of positions
     * @param name Name of Session
     * @param mode Code for mode of transportation, as defined in LocationLoggingContract.LocationSessions
     * @param timestampStart Timestamp of the time at the beginning of the tracking
     * @param timestampEnd Timestamp of the time at the end of the tracking
     * @param timezone The String representation of the timezone, as returned by {@link DateTimeZone#toString()}
     */
    public GPSTrack(List<Location> pos, String name, int mode, long timestampStart, long timestampEnd, String timezone) {
        mPosition = pos;
        mSessionName = name;
        mModeOfTransportation = mode;
        mTimestampStart = timestampStart;
        mTimestampEnd = timestampEnd;
        mTimezone = timezone;
        mOwner = -1;
    }


    /**
     * Constructor to pass a list of positions, a name, and a mode of transportation (defined in
     * LocationLoggingContract.LocationSessions) to the subscriber
     * @param pos List of positions
     * @param name Name of Session
     * @param mode Code for mode of transportation, as defined in LocationLoggingContract.LocationSessions
     * @param timestampStart Timestamp of the time at the beginning of the tracking
     * @param timestampEnd Timestamp of the time at the end of the tracking
     * @param timezone The String representation of the timezone, as returned by {@link DateTimeZone#toString()}
     * @param distance The distance that was run / cycled. If more than one Location was provided in the pos parameter,
     *                 this should be equivalent to the calculated distance between those locations.
     */
    public GPSTrack(List<Location> pos, String name, int mode, long timestampStart, long timestampEnd, String timezone, float distance) {
        mPosition = pos;
        mSessionName = name;
        mModeOfTransportation = mode;
        mTimestampStart = timestampStart;
        mTimestampEnd = timestampEnd;
        mTimezone = timezone;
        mOwner = -1;
        mDistance = distance;
    }

    /**
     * Constructor to pass a list of positions, a name, and a mode of transportation (defined in
     * LocationLoggingContract.LocationSessions) to the subscriber
     * @param pos List of positions
     * @param name Name of Session
     * @param mode Code for mode of transportation, as defined in LocationLoggingContract.LocationSessions
     * @param timestampStart Timestamp of the time at the beginning of the tracking
     * @param timestampEnd Timestamp of the time at the end of the tracking
     * @param timezone The String representation of the timezone, as returned by {@link DateTimeZone#toString()}
     * @param owner The ID of the owner of this track
     */
    public GPSTrack(List<Location> pos, String name, int mode, long timestampStart, long timestampEnd, String timezone, int owner) {
        mPosition = pos;
        mSessionName = name;
        mModeOfTransportation = mode;
        mTimestampStart = timestampStart;
        mTimestampEnd = timestampEnd;
        mTimezone = timezone;
        mOwner = owner;
    }


    /**
     * Constructor to pass a list of positions, a name, and a mode of transportation (defined in
     * LocationLoggingContract.LocationSessions) to the subscriber
     * @param pos List of positions
     * @param name Name of Session
     * @param mode Code for mode of transportation, as defined in LocationLoggingContract.LocationSessions
     * @param timestampStart Timestamp of the time at the beginning of the tracking
     * @param timestampEnd Timestamp of the time at the end of the tracking
     * @param timezone The String representation of the timezone, as returned by {@link DateTimeZone#toString()}
     * @param owner The ID of the owner of this track
     * @param distance The distance that was run / cycled. If more than one Location was provided in the pos parameter,
     *                 this should be equivalent to the calculated distance between those locations.
     */
    public GPSTrack(List<Location> pos, String name, int mode, long timestampStart, long timestampEnd, String timezone, int owner, float distance) {
        mPosition = pos;
        mSessionName = name;
        mModeOfTransportation = mode;
        mTimestampStart = timestampStart;
        mTimestampEnd = timestampEnd;
        mTimezone = timezone;
        mOwner = owner;
        mDistance = distance;
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


    /**
     * Get the timestamp of the beginning of this track
     * @return The timestamp
     */
    public long getTimestamp() {
        return mTimestampStart;
    }


    /**
     * Get the timestamp of the end of the tracking
     * @return The timestamp
     */
    public long getTimestampEnd() {
        return mTimestampEnd;
    }


    /**
     * Get the timezone
     * @return The string representation of the timezone
     */
    public String getTimezone() {
        return mTimezone;
    }


    /**
     * Function to calculate and return the distance run / cycled in this track
     * @return The distance, as a float
     */
    public float getDistance() {
        // Check if a cached value exists
        if (mDistance != 0.0f) return mDistance;
        // Calculate and return distance
        for (int i = 1; i < mPosition.size(); i++) {
            mDistance = mDistance + mPosition.get(i).distanceTo(mPosition.get(i-1));
        }
        return mDistance;
    }

    @Override
    public int getID() {
        return mId;
    }


    @Override
    public void setDescription(String description) {
        mDescription = description;
    }


    @Override
    public String getDescription() {
        return mDescription;
    }


    /**
     * Set the database ID for this GPSTrack
     * @param id The database ID
     */
    public void setID(int id) {
        mId = id;
    }


    public byte[] getByteRepresentation() {
        // Get wrapper and Track builders
        DataContainer.Wrapper.Builder wrapper = DataContainer.Wrapper.newBuilder();
        DataContainer.Track.Builder track = DataContainer.Track.newBuilder();
        // Set the name
        track.setName(mSessionName);
        // Set timestamp and timezone
        track.setTimestampStart(mTimestampStart);
        track.setTimestampEnd(mTimestampEnd);
        track.setDistance(getDistance());
        track.setTimezone(mTimezone);
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
        // Add description
        if (getDescription() != null) {
            track.setDescription(getDescription());
        }
        // Pack in wrapper
        wrapper.setTrack(track);
        // Build, serialize and return wrapper
        return wrapper.build().toByteArray();
    }


    @Override
    public int getOwner() {
        return mOwner;
    }

    public void setOwner(int owner) {
        mOwner = owner;
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
        GPSTrack rv = new GPSTrack(locList, track.getName(), mode, track.getTimestampStart(), track.getTimestampEnd(), track.getTimezone(), track.getDistance());
        rv.setDescription(track.getDescription());
        return rv;
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
              cmptrack.getModeOfTransportation() == getModeOfTransportation() &&
              cmptrack.getTimestamp() == getTimestamp() &&
              cmptrack.getTimestampEnd() == getTimestampEnd() &&
              cmptrack.getTimezone().equals(getTimezone()) &&
              cmptrack.getDescription().equals(getDescription()))) return false;
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
