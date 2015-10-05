package de.velcommuta.denul.event;

/**
 * A general-purpose EventBus event used to request information from services
 */
public class ServiceRequestEvent {
    public static final int SERVICE_DATABASE  = 0;
    public static final int SERVICE_PEDOMETER = 1;
    public static final int SERVICE_GPS       = 2;

    public static final int REQUEST_STATUS    = 0;
    public static final int REQUEST_UPDATE    = 1;

    private int mService;
    private int mRequest;


    /**
     * Constructor for event
     * @param service The service to query
     * @param request The type of request
     */
    public ServiceRequestEvent(int service, int request) {
        mService = service;
        mRequest = request;
    }


    /**
     * Return the contained Service identifier
     * @return The service identifier
     */
    public int getService() {
        return mService;
    }


    /**
     * Return the contained request identifier
     * @return The request identifier
     */
    public int getRequest() {
        return mRequest;
    }
}
