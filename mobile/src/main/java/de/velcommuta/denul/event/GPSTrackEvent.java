package de.velcommuta.denul.event;

import android.location.Location;

import java.util.List;

/**
 * Event to indicate a finished track
 */
public class GPSTrackEvent {
    private List<Location> mPosition;
    private String mSessionName;

    public GPSTrackEvent(List<Location> pos) {
        mPosition = pos;
    }

    public GPSTrackEvent(List<Location> pos, String name) {
        mPosition = pos;
        mSessionName = name;
    }


    public List<Location> getPosition() {
        return mPosition;
    }

    public String getSessionName() {
        return mSessionName;
    }
}
