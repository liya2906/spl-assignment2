package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTests {

    private static final double DELTA = 1e-6;

    // =========================
    // SharedVector
    // =========================

    @Test
    void testVectorInitAndGet() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertEquals(3, v.length());
        assertEquals(1.0, v.get(0), DELTA);
        assertEquals(3.0, v.get(2), DELTA);
    }

    @Test
    void testVectorTransposeTwice() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void testVectorAdd() {
        SharedVector v1 = new SharedVector(new double[]{5, 6}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        v1.add(v2);

        assertEquals(6.0, v1.get(0), DELTA);
        assertEquals(8.0, v1.get(1), DELTA);
        assertEquals(1.0, v2.get(0), DELTA); // unchanged
    }

    @Test
    void testVectorAddWrongOrientation() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
    }

    @Test
    void testVectorNegate() {
        SharedVector v = new SharedVector(new double[]{1, -3}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1.0, v.get(0), DELTA);
        assertEquals(3.0, v.get(1), DELTA);
    }

    @Test
    void testDotProduct() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);
        assertEquals(11.0, row.dot(col), DELTA);
    }

    @Test
    void testDotProductWrongOrientation() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    // =========================
    // SharedMatrix
    // =========================

    @Test
    void testMatrixReadRowMajor() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);

        double[][] out = m.readRowMajor();
        assertEquals(1.0, out[0][0], DELTA);
        assertEquals(4.0, out[1][1], DELTA);
    }

    @Test
    void testMatrixLoadColumnMajor() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(data);

        double[][] out = m.readRowMajor();
        assertArrayEquals(new double[]{1, 2}, out[0], DELTA);
        assertArrayEquals(new double[]{3, 4}, out[1], DELTA);
    }

    @Test
    void testVecMatMul() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{3, 4}, {5, 6}});

        v.vecMatMul(m);

        assertEquals(13.0, v.get(0), DELTA);
        assertEquals(16.0, v.get(1), DELTA);
    }

    // =========================
    // Concurrency
    // =========================

    @Test
    @Timeout(5)
    void testConcurrentVectorAdd() throws InterruptedException {
        SharedVector base = new SharedVector(new double[]{0}, VectorOrientation.ROW_MAJOR);
        SharedVector one = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            pool.submit(() -> {
                base.add(one);
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1000.0, base.get(0), DELTA);
    }
}
