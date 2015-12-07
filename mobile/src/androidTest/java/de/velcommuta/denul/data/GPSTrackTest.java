package de.velcommuta.denul.data;

import android.location.Location;

import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.data.proto.DataContainer;
import de.velcommuta.denul.db.LocationLoggingContract;

/**
 * Test cases for the GPSTrack data object (mostly the serialization and deseralization functions)
 */
public class GPSTrackTest extends TestCase {
    /**
     * Test serialization and deserialization functions
     */
    public void testSerializationDeserialization() {
        // Prepare GPSTrack object
        List<Location> loclist = new LinkedList<>();
        for (double i = 0; i < 1; i = i + 0.2) {
            Location loc = new Location("");
            loc.setLatitude(i);
            loc.setLongitude(i);
            loc.setTime((long) i + 100);
            loclist.add(loc);
        }
        String name = "test";
        int mode = LocationLoggingContract.LocationSessions.VALUE_RUNNING;
        GPSTrack testtrack = new GPSTrack(loclist, name, mode);
        // test values
        assertEquals(testtrack.getSessionName(), name);
        assertEquals(testtrack.getModeOfTransportation(), mode);
        assertEquals(testtrack.getPosition(), loclist);
        // Test serialization
        byte[] serialized = testtrack.getByteRepresentation();
        // Deserialize into Wrapper
        DataContainer.Wrapper wrapper;
        try {
            wrapper = DataContainer.Wrapper.parseFrom(serialized);
        } catch (InvalidProtocolBufferException e) {
            fail();
            return;
        }
        assertTrue(wrapper.getShareableCase() == DataContainer.Wrapper.ShareableCase.TRACK);
        GPSTrack testtrack2 = GPSTrack.fromProtobuf(wrapper.getTrack());
        assertEquals(testtrack.getSessionName(), testtrack2.getSessionName());
        assertEquals(testtrack.getType(), testtrack2.getType());
        // Strictly speaking, this is not guaranteed to work. But as the Location object does not
        // implement a non-stupid .equals(), this is the only way. -.-
        List<Location> loclist2 = testtrack2.getPosition();
        for (int i = 0; i < loclist2.size(); i++) {
            Location loc1 = loclist.get(i);
            Location loc2 = loclist2.get(i);
            assertTrue(loc1.getLatitude() == loc2.getLatitude() &&
                loc1.getLongitude() == loc2.getLongitude() &&
                loc1.getTime() == loc2.getTime());
        }
    }
}
