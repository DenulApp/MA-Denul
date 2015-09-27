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
    private int mModeOfTransportation = LocationLoggingContract.LocationSessions.VALUE_RUNNING;

    public GPSTrackEvent(List<Location> pos) {
        mPosition = pos;
    }

    public GPSTrackEvent(List<Location> pos, String name) {
        mPosition = pos;
        mSessionName = name;
    }

    public GPSTrackEvent(List<Location> pos, String name, int mode) {
        mPosition = pos;
        mSessionName = name;
        mModeOfTransportation = mode;
    }


    public List<Location> getPosition() {
        return mPosition;
    }

    public String getSessionName() {
        return mSessionName;
    }

    public int getModeOfTransportation() {
        return mModeOfTransportation;
    }
}
