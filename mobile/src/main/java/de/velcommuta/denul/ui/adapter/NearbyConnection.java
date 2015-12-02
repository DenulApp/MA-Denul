package de.velcommuta.denul.ui.adapter;

/**
 * Data holder class
 */
public class NearbyConnection {
    private String mID;
    private String mName;


    /**
     * Constructor
     * @param name Name of the connection partner
     * @param id ID of the connection
     */
    public NearbyConnection(String name, String id) {
        mID = id;
        mName = name;
    }


    /**
     * Getter for the ID
     * @return The ID
     */
    public String getID() {
        return mID;
    }


    /**
     * Getter for the Name
     * @return The name
     */
    public String getName() {
        return mName;
    }
}
