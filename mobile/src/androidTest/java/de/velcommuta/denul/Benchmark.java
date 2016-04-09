package de.velcommuta.denul;

import android.util.Log;

import junit.framework.TestCase;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.nio.ByteBuffer;
import java.util.Random;

import de.velcommuta.denul.crypto.AESSharingEncryption;
import de.velcommuta.denul.crypto.HKDFKeyExpansion;
import de.velcommuta.denul.crypto.IdentifierDerivation;
import de.velcommuta.denul.crypto.KeyExpansion;
import de.velcommuta.denul.crypto.SHA256IdentifierDerivation;
import de.velcommuta.denul.crypto.SharingEncryption;
import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.libvicbf.VICBF;

/**
 * Benchmarks.
 * This class will produce AssertionErrors to display the results of the benchmark - make sure
 * to exclude it from any serious test suites.
 */
public class Benchmark extends TestCase {

    /**
     * Benchmark random identifier generation
     */
    public void testRandomIdGeneration() {
        int iterations = 10000;
        DescriptiveStatistics stats = new DescriptiveStatistics();

        IdentifierDerivation deriv = new SHA256IdentifierDerivation();
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            deriv.generateRandomIdentifier();
            long dur = System.nanoTime() - start;
            stats.addValue(dur);
            System.gc();
        }
        double mean = stats.getMean();
        double median = stats.getPercentile(50);
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double min = stats.getMin();
        double max = stats.getMax();
        fail("RandomIDGeneration " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
    }

    /**
     * Benchmark data block encryption
     */
    public void testDataBlockEncryption() {
        int iterations = 10000;
        DescriptiveStatistics stats = new DescriptiveStatistics();

        SharingEncryption enc = new AESSharingEncryption();
        IdentifierDerivation deriv = new SHA256IdentifierDerivation();
        for (int i = 0; i < iterations; i++) {
            Shareable ss = new ShareableStub();
            TokenPair ident = deriv.generateRandomIdentifier();
            long start = System.nanoTime();
            enc.encryptShareable(ss, 0, ident);
            long dur = System.nanoTime() - start;
            stats.addValue(dur);
            System.gc();
        }
        double mean = stats.getMean();
        double median = stats.getPercentile(50);
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double min = stats.getMin();
        double max = stats.getMax();
        fail("DataBlockEncryption " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
    }

    /**
     * Benchmark random identifier generation
     */
    public void testTargetIdGeneration() {
        int iterations = 10000;
        DescriptiveStatistics stats = new DescriptiveStatistics();
        Random rnd = new Random();
        SHA256IdentifierDerivation deriv = new SHA256IdentifierDerivation();
        for (int i = 0; i < iterations; i++) {
            Shareable ss = new ShareableStub();
            byte[] secret = new byte[16];
            rnd.nextBytes(secret);
            KeyExpansion ke = new HKDFKeyExpansion(secret);
            KeySet kp = ke.expand(true);
            long start = System.nanoTime();
            deriv.generateOutboundIdentifier(kp);
            long dur = System.nanoTime() - start;
            stats.addValue(dur);
            System.gc();
        }
        double mean = stats.getMean();
        double median = stats.getPercentile(50);
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double min = stats.getMin();
        double max = stats.getMax();
        fail("TargetIdGeneration " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
    }

    /**
     * Benchmark random identifier generation
     */
    public void testKeyBlockEncryption() {
        int iterations = 10000;
        DescriptiveStatistics stats = new DescriptiveStatistics();
        Random rnd = new Random();
        AESSharingEncryption enc = new AESSharingEncryption();
        SHA256IdentifierDerivation deriv = new SHA256IdentifierDerivation();
        for (int i = 0; i < iterations; i++) {
            Shareable ss = new ShareableStub();
            byte[] secret = new byte[16];
            rnd.nextBytes(secret);
            KeyExpansion ke = new HKDFKeyExpansion(secret);
            KeySet kp = ke.expand(true);
            TokenPair ident = deriv.generateRandomIdentifier();
            DataBlock db = enc.encryptShareable(ss, 0, ident);

            long start = System.nanoTime();
            enc.encryptKeysAndIdentifier(db, kp);
            long dur = System.nanoTime() - start;
            stats.addValue(dur);
            System.gc();
        }
        double mean = stats.getMean();
        double median = stats.getPercentile(50);
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double min = stats.getMin();
        double max = stats.getMax();
        fail("KeyBlockEncryption " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
    }


    /**
     * Benchmark VICBF
     */
    public void testVICBFInsertPerformance() {
        for (int k = 2; k <= 5; k++) {
            for (int i = 10000; i <= 100000; i += 10000) {
                VICBF v = new VICBF(i, k);
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (int j = 0; j <= 10000; j++) {
                    byte[] input = ByteBuffer.allocate(4).putInt(j).array();
                    long start = System.nanoTime();
                    v.insert(input);
                    long dur = System.nanoTime() - start;
                    stats.addValue(dur);
                }
                double mean = stats.getMean();
                double median = stats.getPercentile(50);
                double q1 = stats.getPercentile(25);
                double q3 = stats.getPercentile(75);
                double min = stats.getMin();
                double max = stats.getMax();
                Log.d("VICBFInsert", k + " " + i + " " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
            }
        }
    }


    /**
     * Test query performance of VICBF
     */
    public void testVICBFQueryPerformance() {
        for (int k = 2; k <= 5; k++) {
            for (int i = 10000; i <= 100000; i += 10000) {
                VICBF v = new VICBF(i, k);
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (int j = 0; j <= 10000; j++) {
                    byte[] input = ByteBuffer.allocate(4).putInt(j).array();
                    v.insert(input);
                }
                for (int j = 0; j <= 10000; j++) {
                    byte[] input = ByteBuffer.allocate(4).putInt(j).array();
                    long start = System.nanoTime();
                    v.query(input);
                    long dur = System.nanoTime() - start;
                    stats.addValue(dur);
                }
                double mean = stats.getMean();
                double median = stats.getPercentile(50);
                double q1 = stats.getPercentile(25);
                double q3 = stats.getPercentile(75);
                double min = stats.getMin();
                double max = stats.getMax();
                Log.d("VICBFQueryP", k + " " + i + " " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
            }
        }
        for (int k = 2; k <= 5; k++) {
            for (int i = 10000; i <= 100000; i += 10000) {
                VICBF v = new VICBF(i, k);
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (int j = 0; j <= 10000; j++) {
                    byte[] input = ByteBuffer.allocate(4).putInt(j).array();
                    v.insert(input);
                }
                for (int j = 10001; j <= 20001; j++) {
                    byte[] input = ByteBuffer.allocate(4).putInt(j).array();
                    long start = System.nanoTime();
                    v.query(input);
                    long dur = System.nanoTime() - start;
                    stats.addValue(dur);
                }
                double mean = stats.getMean();
                double median = stats.getPercentile(50);
                double q1 = stats.getPercentile(25);
                double q3 = stats.getPercentile(75);
                double min = stats.getMin();
                double max = stats.getMax();
                Log.d("VICBFQueryN", k + " " + i + " " + mean + " " + median + " " + q1 + " " + q3 + " " + min + " " + max);
            }
        }
    }

    private class ShareableStub implements Shareable {
        private byte[] ser;


        /**
         * Your docstrings have no power here
         */
        public ShareableStub() {
            Random rnd = new Random();
            ser = new byte[10240];
            rnd.nextBytes(ser);
        }
        @Override
        public int getType() {
            return 0;
        }


        @Override
        public int getOwner() {
            return 0;
        }


        @Override
        public void setOwner(int owner) {

        }


        @Override
        public int getID() {
            return 0;
        }


        @Override
        public void setDescription(String description) {

        }


        @Override
        public String getDescription() {
            return null;
        }


        @Override
        public byte[] getByteRepresentation(int granularity) {
            return ser;
        }


        @Override
        public int getGranularityDescriptor() {
            return 0;
        }
    }
}
