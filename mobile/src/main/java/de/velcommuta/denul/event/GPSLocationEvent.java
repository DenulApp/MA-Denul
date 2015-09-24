package de.velcommuta.denul.event;

import com.google.android.gms.maps.model.LatLng;

import org.joda.time.Instant;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public List<LatLng> position;
    public List<Instant> timestamp;
    public boolean isInitial = false;

    public GPSLocationEvent(List<LatLng> pos, List<Instant> time) {
        position = pos;
        timestamp = time;
    }

    public GPSLocationEvent(List<LatLng> pos, List<Instant> time, boolean initial) {
        isInitial = initial;
        timestamp = time;
        position = pos;
    }
}
