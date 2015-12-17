package de.velcommuta.denul.data;

import android.location.Location;

import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import org.joda.time.Instant;

import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.data.proto.DataContainer;

/**
 * Test cases for the GPSTrack data object (mostly the serialization and deseralization functions)
 */
public class GPSTrackTest extends TestCase {
    /**
     * Test serialization and deserialization functions
     */
    public void testSerializationDeserializationFine() {
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
        int mode = GPSTrack.VALUE_RUNNING;
        GPSTrack testtrack = new GPSTrack(loclist, name, mode, new Instant().getMillis(), new Instant().getMillis(), "Europe/Berlin");
        testtrack.setDescription("This is a description. Fancy.");
        // test values
        assertEquals(testtrack.getSessionName(), name);
        assertEquals(testtrack.getModeOfTransportation(), mode);
        assertEquals(testtrack.getPosition(), loclist);
        assertEquals(testtrack.getDescription(), "This is a description. Fancy.");
        // Test serialization
        byte[] serialized = testtrack.getByteRepresentation(Shareable.GRANULARITY_FINE);
        // Deserialize into Wrapper
        DataContainer.Wrapper wrapper;
        try {
            wrapper = DataContainer.Wrapper.parseFrom(serialized);
        } catch (InvalidProtocolBufferException e) {
            fail();
            return;
        }
        // test deserialization
        assertTrue(wrapper.getShareableCase() == DataContainer.Wrapper.ShareableCase.TRACK);
        GPSTrack testtrack2 = GPSTrack.fromProtobuf(wrapper.getTrack());
        // Test if the values still match
        assertEquals(testtrack.getSessionName(), testtrack2.getSessionName());
        assertEquals(testtrack.getType(), testtrack2.getType());
        assertEquals(testtrack.getTimestamp(), testtrack2.getTimestamp());
        assertEquals(testtrack.getTimestampEnd(), testtrack2.getTimestampEnd());
        assertEquals(testtrack.getTimezone(), testtrack2.getTimezone());
        assertEquals(testtrack.getDistance(), testtrack2.getDistance());
        assertEquals(testtrack.getDescription(), testtrack2.getDescription());
        // Locations do not specify a non-stupid .equals(), so this is the only way to check :/
        for (Location cmploc : testtrack.getPosition()) {
            boolean rv = false;
            for (Location myLoc : testtrack2.getPosition()) {
                if (cmploc.getLatitude() == myLoc.getLatitude() &&
                        cmploc.getLatitude() == myLoc.getLongitude() &&
                        cmploc.getTime() == myLoc.getTime()) {
                    rv = true;
                    break;
                }
            }
            if (!rv) fail();
        }
        // Check if the equals operator comes to the same conclusion.
        assertTrue(testtrack.equals(testtrack2));
    }


    /**
     * Test serialization and deserialization functions
     */
    public void testSerializationDeserializationCoarse() {
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
        int mode = GPSTrack.VALUE_RUNNING;
        GPSTrack testtrack = new GPSTrack(loclist, name, mode, new Instant().getMillis(), new Instant().getMillis(), "Europe/Berlin");
        testtrack.setDescription("This is a description. Fancy.");
        // test values
        assertEquals(testtrack.getSessionName(), name);
        assertEquals(testtrack.getModeOfTransportation(), mode);
        assertEquals(testtrack.getPosition(), loclist);
        assertEquals(testtrack.getDescription(), "This is a description. Fancy.");
        // Test serialization
        byte[] serialized = testtrack.getByteRepresentation(Shareable.GRANULARITY_COARSE);
        // Deserialize into Wrapper
        DataContainer.Wrapper wrapper;
        try {
            wrapper = DataContainer.Wrapper.parseFrom(serialized);
        } catch (InvalidProtocolBufferException e) {
            fail();
            return;
        }
        // test deserialization
        assertTrue(wrapper.getShareableCase() == DataContainer.Wrapper.ShareableCase.TRACK);
        GPSTrack testtrack2 = GPSTrack.fromProtobuf(wrapper.getTrack());
        // Test if the values still match
        assertEquals(testtrack.getSessionName(), testtrack2.getSessionName());
        assertEquals(testtrack.getType(), testtrack2.getType());
        assertEquals(testtrack.getTimestamp(), testtrack2.getTimestamp());
        assertEquals(testtrack.getTimestampEnd(), testtrack2.getTimestampEnd());
        assertEquals(testtrack.getTimezone(), testtrack2.getTimezone());
        assertEquals(testtrack.getDistance(), testtrack2.getDistance());
        assertEquals(testtrack.getDescription(), testtrack2.getDescription());
        // As locations have not been serialized, we should only check if the List is empty
        assertTrue(testtrack2.getPosition().size() == 0);
    }
}
