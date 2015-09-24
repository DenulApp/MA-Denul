package de.velcommuta.denul.event;

import com.google.android.gms.maps.model.LatLng;

import org.joda.time.Instant;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public LatLng position;
    public Instant timestamp;

    public GPSLocationEvent(LatLng pos) {
        position = pos;
        timestamp = Instant.now();
    }
}
