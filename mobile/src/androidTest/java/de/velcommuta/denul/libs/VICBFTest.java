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

    /**
     * Test inserting data into the VICBF and querying it afterwards
     */
    @Test
    public void test_insert_query() {
        VICBF test = new VICBF(10000, 3);
        assertEquals(test.getSize(), 0);
        assertFalse(test.query("deadbeef"));
        test.insert("deadbeef");
        assertEquals(test.getSize(), 1);
        assertTrue(test.query("deadbeef"));
        assertFalse(test.query("deafbeet"));
    }


    /**
     * Test inserting data and deleting it afterwards
     */
    @Test
    public void test_insert_delete() {
        VICBF test = new VICBF(10000, 3);
        test.insert("decafbad");
        assertEquals(test.getSize(), 1);
        assertTrue(test.query("decafbad"));
        try {
            test.remove("decafbad");
        } catch (Exception e) {
            assertTrue(false);
        }
        assertFalse(test.query("decafbad"));
        assertEquals(test.getSize(), 0);
    }


    /**
     * Test that deleting data that was never inserted does not work
     */
    @Test
    public void test_delete_not_inserted() {
        VICBF test = new VICBF(10000, 3);
        try {
            test.remove("decafbad");
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
    }


    /**
     * Test that deleting only deletes the target value
     */
    @Test
    public void test_multi_insert_one_delete() {
        VICBF test = new VICBF(10000, 3);
        test.insert("decafbad");
        test.insert("deadbeef");
        test.insert("carebearstare");
        try {
            test.remove("decafbad");
        } catch (Exception e) {
            assertTrue(false);
        }
        assertFalse(test.query("decafbad"));
        assertTrue(test.query("deadbeef"));
        assertTrue(test.query("carebearstare"));
    }


    /**
     * Regression test to ensure an old deletion bug does not creep back in
     */
    @Test
    public void test_delete_regression_1() {
        // This test aims to check a specific bordercase in the deletion routine
        // that could result in an inconsistent VICBF when triggered.
        VICBF test = new VICBF(10000, 3);
        test.insert("106");
        test.insert("771");
        try {
            test.remove("132");
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
        try {
            test.remove("106");
        } catch (Exception e) {
            assertFalse(true);
        }
        assertTrue(test.query("771"));
    }


    /**
     * Test for the deserialization routine for full dumps
     * @throws IOException If the function throws an IOException
     */
    @Test
    public void test_deserialize_python_full() throws IOException {
        // Serialization of VICBF with 1000 slots, containing all numbers from 0 to 500
        String ser = "03000003e8000001f44806100a0009070612000c050000000b0400040507060f0d00160c1200000c0504040905050f00050400100c0005110700070c0d120e0011000b0d0517000b090600040009060c0b071d0e0004141000061006140700060506140c14090b0d1109050c09040c060c070004120005040b060e0005050a050f0c070507211a0606090006150a0c0d0c000000040b0005090a060a0004050006130505070000170b0d0b08090006061115000f070d0900051a070008060b10050008001211000711060c0800040e0f040507000d060b0c0012060005000418000008141206071a0000040f00040600000704000005071014050b180a0c0411000b000b051205050b0b0d0b100004000606141a1200000006120e07000c000b00130e040a040500000a000700000d0c0008050908000807000c0e10070507001c0f0c0d0d070e17111a18060c0000050b09050e11180c0007050405060d00040b00000b0906000e190c000b000b00140e0d06000c171100040e1100050000000c090a06000c0b0b0c0e0b00210c000c0b0607060c0e09250b0406000d0413000c0808000c002210040b05070400060b00140a050c050c0d05170a04200016000a1006080705070012120a14041100100007070e000905120b07000005000404040806040d0000190504060b060e04000900050007051305050607060e101907080000050008130b001205050700001911040600060b0f1c100016070b0b041b0505040b0b040d16070000140700120500040704000c15070e0a26070c0a0d060a000914001c00051007000d0015040e160513041a06000405040005060412000c000d061507050a07050b0009120d1d070f07000a0709050b0000071e0018040f160a0700050b120d13000b181b0019000b0009130a0e0515090705150a10000005050005000d1304001305000006040608060c000c0e0c001407140e0e070c05060906060011000d0f00080b000b06070507040d12000a000006060b0819140006071106000900060011000d070000060b04040b0f000a000a000900001904001105040a0d150a000b1106060b000000050c17000006050a041822050007000713041117110e0c04000c0b15060410050005000c0e0c0000070004050700000409060407070604040a00040f00101700060505050b0515050c0e050b0011040504000d00080c0b13000b1b04060b0d040a07070b000705000610050b06050004041004050f00000f000404060004000a0000050c050900050f000007050b04070e00000d070b04070a07130005050d000b0d05070a050c07000c0009080d050b0e000f13000c001207000a04110c13000e141606000008051507060c0400160b040500040000090b0c05110b040e060700";
        VICBF v = VICBF.deserialize(ser);
        for (int i = 0; i < 500; i++) {
            assertTrue(v.query(Integer.toString(i)));
        }
        assertEquals(v.getSize(), 500);
        assertFalse(v.query("501"));
        v.insert("501");
        assertTrue(v.query("501"));
        assertEquals(v.getSize(), 501);
    }


    /**
     * Test the deserialization routine for partial dumps
     * @throws IOException If the function throws an IOException
     */
    @Test
    public void test_deserialize_python_partial() throws IOException {
        // Serialization of VICBF with 10000 slots, containing numbers 123 and 126
        String ser = "830000271000000002482663070f850419ab0701b20525b505069a07";
        VICBF v = VICBF.deserialize(ser);
        assertTrue(v.query("123"));
        assertTrue(v.query("126"));
        assertFalse(v.query("1337"));
        assertEquals(v.getSize(), 2);
        v.insert("1337");
        assertTrue(v.query("1337"));
        assertEquals(v.getSize(), 3);
    }
}
