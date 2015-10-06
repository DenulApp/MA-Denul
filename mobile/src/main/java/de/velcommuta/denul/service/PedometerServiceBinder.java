package de.velcommuta.denul.service;

import org.joda.time.DateTime;

import java.util.Hashtable;

/**
 * Interface for the pedometer service binder
 */
public interface PedometerServiceBinder {
    /**
     * Get the current sum of steps for today
     * @return The current sum of steps, as int
     */
    int getSumToday();

    /**
     * Get todays cache of events (steps per hour)
     * @return The Hashtable with todays events
     */
    Hashtable<DateTime, Long> getToday();

    /**
     * Add an update listener to the service
     * @param listener The listener instance
     */
    void addUpdateListener(UpdateListener listener);

    /**
     * Remove an existing updateListener
     * @param listener The listener instance
     */
    void removeUpdateListener(UpdateListener listener);
}
