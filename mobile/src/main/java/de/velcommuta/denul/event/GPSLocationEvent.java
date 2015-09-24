package de.velcommuta.denul.event;

import android.location.Location;

import org.joda.time.Instant;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public List<Location> position;
    public List<Instant> timestamp;
    public boolean isInitial = false;

    public GPSLocationEvent(List<Location> pos, List<Instant> time) {
        position = pos;
        timestamp = time;
    }

    public GPSLocationEvent(List<Location> pos, List<Instant> time, boolean initial) {
        isInitial = initial;
        timestamp = time;
        position = pos;
    }
}
