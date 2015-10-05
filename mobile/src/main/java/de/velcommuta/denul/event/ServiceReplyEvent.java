package de.velcommuta.denul.event;

/**
 * A general-purpose EventBus event used to request information from services
 */
public class ServiceReplyEvent {
    public static final int SERVICE_DATABASE  = 0;
    public static final int SERVICE_PEDOMETER = 1;
    public static final int SERVICE_GPS       = 2;

    public static final int REPLY_STATUS_ONLINE   = 0;
    public static final int REPLY_UPDATE_COMPLETE = 1;
    public static final int REPLY_UPDATE_FAILED   = 2;

    private int mService;
    private int mReply;


    /**
     * Constructor for event
     * @param service The service the reply comes from
     * @param reply The type of reply
     */
    public ServiceReplyEvent(int service, int reply) {
        mService = service;
        mReply = reply;
    }


    /**
     * Return the contained Service identifier
     * @return The service identifier
     */
    public int getService() {
        return mService;
    }


    /**
     * Return the contained reply identifier
     * @return The reply identifier
     */
    public int getReply() {
        return mReply;
    }
}
