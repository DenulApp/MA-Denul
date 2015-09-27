package de.velcommuta.denul.event;

import android.location.Location;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public List<Location> mPosition;

    public GPSLocationEvent(List<Location> pos) {
        mPosition = pos;
    }

    public List<Location> getPosition() {
        return mPosition;
    }
}
