package de.velcommuta.denul.event;

import android.location.Location;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    public List<Location> mPosition;

    /**
     * Constructor to set up the Message with the Location list
     * @param pos List of locations that should be passed to subscribers
     */
    public GPSLocationEvent(List<Location> pos) {
        mPosition = pos;
    }

    /**
     * Get access to the list of positions passed with the event
     * @return List of positions
     */
    public List<Location> getPosition() {
        return mPosition;
    }
}
