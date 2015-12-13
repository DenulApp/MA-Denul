package de.velcommuta.denul.libs;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import de.velcommuta.libvicbf.VICBF;

/**
 * Test cases for the VICBF library, copied over from the original library to make sure they still
 * work on Android
 */
public class VICBFTest {

    @Test
    public void test_insert_query() {
        VICBF test = new VICBF(10000, 3);
        assertEquals(test.getSize(), 0);
        assertFalse(test.query("deadbeef".getBytes()));
        test.insert("deadbeef".getBytes());
        assertEquals(test.getSize(), 1);
        assertTrue(test.query("deadbeef".getBytes()));
        assertFalse(test.query("deafbeet".getBytes()));
    }

    @Test
    public void test_insert_delete() {
        VICBF test = new VICBF(10000, 3);
        test.insert("decafbad".getBytes());
        assertEquals(test.getSize(), 1);
        assertTrue(test.query("decafbad".getBytes()));
        try {
            test.remove("decafbad".getBytes());
        } catch (Exception e) {
            assertTrue(false);
        }
        assertFalse(test.query("decafbad".getBytes()));
        assertEquals(test.getSize(), 0);
    }


    @Test
    public void test_delete_not_inserted() {
        VICBF test = new VICBF(10000, 3);
        try {
            test.remove("decafbad".getBytes());
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void test_multi_insert_one_delete() {
        VICBF test = new VICBF(10000, 3);
        test.insert("decafbad".getBytes());
        test.insert("deadbeef".getBytes());
        test.insert("carebearstare".getBytes());
        try {
            test.remove("decafbad".getBytes());
        } catch (Exception e) {
            assertTrue(false);
        }
        assertFalse(test.query("decafbad".getBytes()));
        assertTrue(test.query("deadbeef".getBytes()));
        assertTrue(test.query("carebearstare".getBytes()));
    }

    @Test
    public void test_delete_regression_1() {
        // This test aims to check a specific bordercase in the deletion routine
        // that could result in an inconsistent VICBF when triggered.
        VICBF test = new VICBF(10000, 3);
        test.insert("106".getBytes());
        test.insert("771".getBytes());
        try {
            test.remove("132".getBytes());
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
        try {
            test.remove("106".getBytes());
        } catch (Exception e) {
            assertFalse(true);
        }
        assertTrue(test.query("771".getBytes()));
    }

    @Test
    public void test_deserialize_python_full() throws IOException {
        // Serialization of VICBF with 1000 slots, containing all numbers from 0 to 500
        String ser = "03000003e8000001f44806100b000a070612000c050000000e040004050706110b001609120000090504040905050f000504000d0b00050f0700070a0e1014001100080e0516000b09060004000a060c0b041f0c0004111000060f06140700060505140c17090b0e1009040c090509060c070005100006060c060d0004050b050f0c070507211a060609000413090c0e0d000000040d00040909060a0004060006130505060000170c0b0d0809000606120e000e040d0a00071b07000b070c1005000a001011000712060c0b00040e0f040507000d070b0c001004000500071700000814110607190000041200040500000706000004070f11070b190c0c0510000c000b051005050b0b0e0b1000040006070f1b1200000006120e07000c000c00110e040a0405000008000700000a0c0009050908000804000c0e1007070700190f110a0f040e16111a18050c0000040a0a050e131a0e0007060405060d00040a00000b09050010170d000c000b0013100b06000a190f0007110f00050000000c0b0a06000b0d0b09110b001e0b000c0a0607060b110d250c050600090413000e0808000c002510040e04070400060900110a050a050c0d05170804200015000a10060a0505060012120a14041000140007070e000904120b07000005000404040a04040d0000160504060b060e04000c00050007051405070607060e0f1d0709000005000a130b001205060400001b10050400060b101a100013070b0b04180505040b0d040d13070000110700120700040705000b15070f0a27070c0d0c060a000914001c00071004000d0013060e160513071b06000405040005060513000b000a061607040a07060c000c12091f060e07000a0709050b0000071c0018060e160a0700070b150d11000b181a0017000b0009150c0d04140a0705160b11000005050006000d1404001205000006040608060c000c0f09001407120e0e070c0506090606000e000d0f000809000b06070507050d12000a000006050b0919120006071106000900060011000b040000070d04050b0f000a000c000900001904000f05040a0d150a000c1306060a000000060c16000006050a04181f070007000512041015110e0c04000c0b14060410070005000a0b0a0000070004060500000409070407070604040a000412000f1a00060505050b0514050c0e05090013040504000a00080c0a13000a1d06060913060b07060b000705000611050c06050007041204050f00000f000404060004000a0000050c050900070f000007050b04070b00000d070904070a06120004050b000b0d05040a050907000c000a0810050a13000f13000c001205000b040f0815000d141604000009051507060a0400160b0406000700000b0d0b050f0b040e060600";
        VICBF v = VICBF.deserialize(ser);
        for (int i = 0; i < 500; i++) {
            assertTrue(v.query(Integer.toString(i).getBytes()));
        }
        assertEquals(v.getSize(), 500);
        assertFalse(v.query("501".getBytes()));
        v.insert("501".getBytes());
        assertTrue(v.query("501".getBytes()));
        assertEquals(v.getSize(), 501);
    }

    @Test
    public void test_deserialize_python_partial() throws IOException {
        // Serialization of VICBF with 1000 slots, containing numbers 123 and 126
        String ser = "83000003e80000000248023b0703cd05028d0401b20502b207033b07";
        VICBF v = VICBF.deserialize(ser);
        assertTrue(v.query("123".getBytes()));
        assertTrue(v.query("126".getBytes()));
        assertFalse(v.query("1337".getBytes()));
        assertEquals(v.getSize(), 2);
        v.insert("1337".getBytes());
        assertTrue(v.query("1337".getBytes()));
        assertEquals(v.getSize(), 3);
    }
}
