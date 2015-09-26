package de.velcommuta.denul.event;

import android.location.Location;

import org.joda.time.Instant;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public List<Location> mPosition;
    public boolean mInitial = false;

    public GPSLocationEvent(List<Location> pos) {
        mPosition = pos;
    }

    public GPSLocationEvent(List<Location> pos, boolean initial) {
        mInitial = initial;
        mPosition = pos;
    }

    public List<Location> getPosition() {
        return mPosition;
    }

    public boolean isInitial() {
        return mInitial;
    }
}
