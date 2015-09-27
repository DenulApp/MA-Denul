package de.velcommuta.denul.event;

import android.location.Location;

import java.util.List;

/**
 * GPS Location Event for EventBus (https://github.com/greenrobot/EventBus)
 */
public class GPSLocationEvent {
    private List<Location> mPosition;
    private long mChronobase;

    /**
     * Constructor to set up the Message with the Location list
     * @param pos List of locations that should be passed to subscribers
     * @param chronobase SystemClock.elapsedRealtime() at the time of the first GPS fix
     */
    public GPSLocationEvent(List<Location> pos, long chronobase) {
        mPosition = pos;
        mChronobase = chronobase;
    }

    /**
     * Get access to the list of positions passed with the event
     * @return List of positions
     */
    public List<Location> getPosition() {
        return mPosition;
    }

    /**
     * Get the chronometer base
     * @return Base of the chronometer, in elapsed Milliseconds since boot
     */
    public long getChronoBase() {
        return mChronobase;
    }
}
