package de.velcommuta.denul.service;

/**
 * This interface can be implemented by activities / fragments that want to receive a notification
 * if a service has new data available.
 */
public interface UpdateListener {
    /**
     * Notify the class that an update is availble
     */
    void update();
}
