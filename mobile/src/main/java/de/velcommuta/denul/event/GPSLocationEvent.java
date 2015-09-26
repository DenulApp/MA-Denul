package de.velcommuta.denul.event;

import android.location.Location;

import org.joda.time.Instant;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public List<Location> mPosition;
    public List<Instant> mTimestamp;
    public boolean mInitial = false;

    public GPSLocationEvent(List<Location> pos, List<Instant> time) {
        mPosition = pos;
        mTimestamp = time;
    }

    public GPSLocationEvent(List<Location> pos, List<Instant> time, boolean initial) {
        mInitial = initial;
        mTimestamp = time;
        mPosition = pos;
    }

    public List<Location> getPosition() {
        return mPosition;
    }

    public List<Instant> getTimestamps() {
        return mTimestamp;
    }

    public boolean isInitial() {
        return mInitial;
    }
}
