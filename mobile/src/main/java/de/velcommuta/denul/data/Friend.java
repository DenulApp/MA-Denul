package de.velcommuta.denul.data;

/**
 * Data container for the FriendListCursorAdapter
 */
public class Friend {
    public static final int UNVERIFIED = 0;
    public static final int VERIFIED_OK = 1;
    public static final int VERIFIED_FAIL = 2;

    private String mName;
    private int mVerified;
    private int mID;


    /**
     * Empty constructor
     */
    public Friend() {}


    /**
     * Constructor to initialize the Friend to specific values
     * @param name The name
     * @param verified Verification status
     * @param ID The database ID
     */
    public Friend(String name, int verified, int ID) {
        mName = name;
        mVerified = verified;
        mID = ID;
    }

    /**
     * Set the Name of this Friend
     * @param name Name
     */
    public void setName(String name) {
        mName = name;
    }


    /**
     * Get the Name of this friend
     * @return The name as String
     */
    public String getName() {
        return mName;
    }


    /**
     * Set the verification status of this friend
     * @param verified The verification status
     */
    public void setVerified(int verified) {
        mVerified = verified;
    }


    /**
     * Get the verification status of this friend
     * @return The verification status
     */
    public int getVerified() {
        return mVerified;
    }

    /**
     * Get the database ID of the friend
     * @return The ID of the friend in the database
     */
    public int getID() {
        return mID;
    }


    /**
     * Set the database ID of the friend
     * @param id The database id
     */
    public void setID(int id) {
        mID = id;
    }

    public String toString() {
        return mName;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Friend)) return false;
        Friend f = (Friend) o;
        return getName().equals(f.getName())
                && getID() == f.getID()
                && getVerified() == f.getVerified();
    }
}