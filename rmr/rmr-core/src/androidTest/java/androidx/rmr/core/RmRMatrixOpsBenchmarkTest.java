package androidx.rmr.core;

import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class RmRMatrixOpsBenchmarkTest {
    private static final String TAG = "RmRMatrixOpsBenchmark";
    private static final int WARMUP = 20;
    private static final int ITERATIONS = 120;

    @Test
    public void benchmarkMatrixOpsEvidence() {
        RmRMatrix left = seededMatrix(64, 64, 1.0);
        RmRMatrix right = seededMatrix(64, 64, 2.0);
        RmRMatrix vectorA = seededMatrix(1024, 1, 3.0);
        RmRMatrix vectorB = seededMatrix(1024, 1, 4.0);
        RmRMatrix transposeOut = new RmRMatrix(64, 64);
        RmRMatrix multiplyOut = new RmRMatrix(64, 64);

        Stats multiplyStats = measure(() -> RmRMatrixOps.multiply(left, right));
        Stats multiplyIntoStats = measure(() -> RmRMatrixOps.multiplyInto(left, right, multiplyOut));
        Stats transposeStats = measure(() -> RmRMatrixOps.transposeInto(left, transposeOut));
        Stats dotStats = measure(() -> RmRMatrixOps.vectorDot(vectorA, vectorB));

        Log.i(TAG, environmentLine());
        Log.i(TAG, multiplyStats.toLine("multiply"));
        Log.i(TAG, multiplyIntoStats.toLine("multiplyInto"));
        Log.i(TAG, transposeStats.toLine("transposeInto"));
        Log.i(TAG, dotStats.toLine("vectorDot"));

        assertTrue(multiplyStats.avgNs > 0);
        assertTrue(multiplyIntoStats.avgNs > 0);
        assertTrue(transposeStats.avgNs > 0);
        assertTrue(dotStats.avgNs > 0);
    }

    private static String environmentLine() {
        return String.format(
                Locale.US,
                "runtime=Android ABI=%s simd=%s model=%s sdk=%d warmup=%d iterations=%d",
                Arrays.toString(Build.SUPPORTED_ABIS),
                RmRHardware.getSimdLevel(),
                Build.MODEL,
                Build.VERSION.SDK_INT,
                WARMUP,
                ITERATIONS);
    }

    private static Stats measure(Runnable runnable) {
        for (int i = 0; i < WARMUP; i++) {
            runnable.run();
        }
        long[] timesNs = new long[ITERATIONS];
        long[] allocBytes = new long[ITERATIONS];

        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < ITERATIONS; i++) {
            long usedBefore = runtime.totalMemory() - runtime.freeMemory();
            long start = System.nanoTime();
            runnable.run();
            long elapsed = System.nanoTime() - start;
            long usedAfter = runtime.totalMemory() - runtime.freeMemory();
            timesNs[i] = elapsed;
            allocBytes[i] = Math.max(0L, usedAfter - usedBefore);
        }
        return Stats.from(timesNs, allocBytes);
    }

    private static RmRMatrix seededMatrix(int rows, int cols, double seed) {
        RmRMatrix matrix = new RmRMatrix(rows, cols);
        double value = seed;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                matrix.set(r, c, value);
                value = (value * 1.0001) + 0.01;
            }
        }
        return matrix;
    }

    private static final class Stats {
        final long avgNs;
        final long p50Ns;
        final long p95Ns;
        final long avgAllocBytes;

        private Stats(long avgNs, long p50Ns, long p95Ns, long avgAllocBytes) {
            this.avgNs = avgNs;
            this.p50Ns = p50Ns;
            this.p95Ns = p95Ns;
            this.avgAllocBytes = avgAllocBytes;
        }

        static Stats from(long[] samplesNs, long[] allocBytes) {
            long[] sorted = samplesNs.clone();
            Arrays.sort(sorted);
            long totalNs = 0;
            long totalAlloc = 0;
            for (int i = 0; i < samplesNs.length; i++) {
                totalNs += samplesNs[i];
                totalAlloc += allocBytes[i];
            }
            return new Stats(
                    totalNs / samplesNs.length,
                    percentile(sorted, 0.50),
                    percentile(sorted, 0.95),
                    totalAlloc / allocBytes.length);
        }

        private static long percentile(long[] sorted, double p) {
            int idx = (int) Math.ceil((sorted.length - 1) * p);
            return sorted[Math.min(sorted.length - 1, Math.max(0, idx))];
        }

        String toLine(String op) {
            return String.format(Locale.US,
                    "%s avg_ns=%d p50_ns=%d p95_ns=%d avg_alloc_bytes=%d",
                    op, avgNs, p50Ns, p95Ns, avgAllocBytes);
        }
    }
}
