package de.velcommuta.denul.event;

import android.location.Location;

import de.velcommuta.denul.db.LocationLoggingContract;

import java.util.List;

/**
 * Event to indicate a finished track
 */
public class GPSTrackEvent {
    private List<Location> mPosition;
    private String mSessionName;
    private int mModeOfTransportation;

    /**
     * Constructor to pass a list of positions to the subscriber (without any title)
     * @param pos List of positions
     */
    public GPSTrackEvent(List<Location> pos) {
        mPosition = pos;
    }

    /**
     * Constructor to pass a list of positions and a name for this session to subscribers
     * @param pos List of positions
     * @param name Name of session
     */
    public GPSTrackEvent(List<Location> pos, String name) {
        mPosition = pos;
        mSessionName = name;
    }

    /**
     * Constructor to pass a list of positions, a name, and a mode of transportation (defined in
     * LocationLoggingContract.LocationSessions) to the subscriber
     * @param pos List of positions
     * @param name Name of Session
     * @param mode Code for mode of transportation, as defined in LocationLoggingContract.LocationSessions
     */
    public GPSTrackEvent(List<Location> pos, String name, int mode) {
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
}
