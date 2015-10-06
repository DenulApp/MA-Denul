package de.velcommuta.denul.event;

/**
 * EventBus event to indicate database availability
 */
public class DatabaseAvailabilityEvent {
    public static final int STARTED = 0;
    public static final int OPENED  = 1;
    public static final int CLOSED  = 2;
    public static final int STOPPED = 3;

    private int mStatus;


    /**
     * Instantiate a new DatabaseAvailabilityEvent for EventBus
     * @param status The status code that the event should carry
     */
    public DatabaseAvailabilityEvent(int status) {
        mStatus = status;
    }


    /**
     * Retrieve the status code the event carries
     * @return The status code
     */
    public int getStatus() {
        return mStatus;
    }
}
