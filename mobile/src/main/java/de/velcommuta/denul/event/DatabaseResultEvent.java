package de.velcommuta.denul.event;

/**
 * Indicate that something happened during a database access - be it a success or an error
 */
public class DatabaseResultEvent {
    private String mMessage;

    /**
     * Public constructor to set up the Message
     * @param message The message that should be passed to subscribers
     */
    public DatabaseResultEvent(String message) {
        mMessage = message;
    }

    /**
     * Retrieve the message contained in the event
     * @return The message that was set during initialization.
     */
    public String getMessage() {
        return mMessage;
    }
}
