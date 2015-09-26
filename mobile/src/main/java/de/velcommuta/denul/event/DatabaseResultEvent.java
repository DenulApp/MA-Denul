package de.velcommuta.denul.event;

/**
 * Indicate that something happened during a database access - be it a success or an error
 */
public class DatabaseResultEvent {
    private String mMessage;

    public DatabaseResultEvent(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
}
