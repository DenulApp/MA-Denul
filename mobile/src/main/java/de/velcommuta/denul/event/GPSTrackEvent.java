package de.velcommuta.denul.event;

import android.location.Location;

import org.joda.time.Instant;

import java.util.List;

/**
 * Event to indicate a finished track
 */
public class GPSTrackEvent {
    private List<Location> mPosition;
    private List<Instant> mTimestamp;
    private String mSessionName;

    public GPSTrackEvent(List<Location> pos, List<Instant> time) {
        mPosition = pos;
        mTimestamp = time;
    }

    public GPSTrackEvent(List<Location> pos, List<Instant> time, String name) {
        mPosition = pos;
        mTimestamp = time;
        mSessionName = name;
    }


    public List<Location> getPosition() {
        return mPosition;
    }

    public List<Instant> getTimestamp() {
        return mTimestamp;
    }

    public String getSessionName() {
        return mSessionName;
    }
}
