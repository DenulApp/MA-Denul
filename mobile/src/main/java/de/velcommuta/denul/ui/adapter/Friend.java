package de.velcommuta.denul.ui.adapter;

import net.sqlcipher.Cursor;

import de.velcommuta.denul.db.FriendContract;

/**
 * Data container for the FriendListCursorAdapter
 */
public class Friend {
    private String mName;
    private int mVerified;
    private int mID;


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


    /**
     * Return a Friend object from the current values of the cursor
     *
     * @param c The cursor
     * @return A friend object with these values
     * TODO Move this into the getFriends function of the database to avoid using DB-specific knowledge
     */
    public static Friend fromCursor(Cursor c) {
        Friend f = new Friend();
        f.setName(c.getString(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_FRIEND)));
        f.setVerified(c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_VERIFIED)));
        f.setID(c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList._ID)));
        return f;
    }
}