
package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedVectorTest {

    private static final double DELTA = 1e-9;

    // Helper to read all elements from a SharedVector into a plain array
    private static double[] toArray(SharedVector v) {
        int n = v.length();
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            out[i] = v.get(i);
        }
        return out;
    }

    @Test
    void initialization_createsRowVectorWithExpectedLengthAndValues() {
        double[] values = new double[] { 1.0, 2.0, 3.0 };
        SharedVector vec = new SharedVector(values.clone(), VectorOrientation.ROW_MAJOR);

        assertEquals(3, vec.length(), "length should match provided array length");
        assertArrayEquals(values, toArray(vec), DELTA);
    }

    @Test
    void initialization_createsColumnVectorcWithExpectedLengthAndValues() {
        double[] values = new double[] { 1.0, 2.0, 3.0 };
        SharedVector vec = new SharedVector(values.clone(), VectorOrientation.COLUMN_MAJOR);

        assertEquals(3, vec.length(), "length should match provided array length");
        assertArrayEquals(values, toArray(vec), DELTA);
    }

    @Test
    void getOrientation_returnsGivenOrientation() {
        SharedVector row = new SharedVector(new double[] { 1.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[] { 2.0 }, VectorOrientation.COLUMN_MAJOR);

        assertEquals(VectorOrientation.ROW_MAJOR, row.getOrientation());
        assertEquals(VectorOrientation.COLUMN_MAJOR, col.getOrientation());
    }

    @Test
    void getOrientation_isConsistentAcrossCalls() {
        SharedVector vec = new SharedVector(new double[] { 0.0, 0.0 }, VectorOrientation.COLUMN_MAJOR);

        // call multiple times to ensure consistent return
        assertEquals(VectorOrientation.COLUMN_MAJOR, vec.getOrientation());
        assertEquals(VectorOrientation.COLUMN_MAJOR, vec.getOrientation());
    }

    @Test
    void emptyVector_hasLengthZero_andIndexAccessThrows() {
        SharedVector empty = new SharedVector(new double[] {}, VectorOrientation.ROW_MAJOR);

        assertEquals(0, empty.length());
        assertArrayEquals(new double[] {}, toArray(empty), DELTA);

        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(-1));
    }

    @Test
    void invalidIndexAccess_throwsIndexOutOfBounds() {
        SharedVector vec = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);

        assertThrows(IndexOutOfBoundsException.class, () -> vec.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vec.get(2)); // equal to length
        assertThrows(IndexOutOfBoundsException.class, () -> vec.get(100));
    }

    @Test
    void add_sameOrientation_modifiesLeftVectorWithElementWiseSum() {
        SharedVector a = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[] { 3.0, 4.0 }, VectorOrientation.ROW_MAJOR);

        a.add(b); // method mutates 'a' in-place

        assertArrayEquals(new double[] { 4.0, 6.0 }, toArray(a), DELTA);
        // 'b' should remain unchanged
        assertArrayEquals(new double[] { 3.0, 4.0 }, toArray(b), DELTA);
        // orientation preserved for the mutated vector
        assertEquals(VectorOrientation.ROW_MAJOR, a.getOrientation());
    }

    @Test
    void add_null_throwsIllegalArgumentException() {
        SharedVector a = new SharedVector(new double[] { 1.0 }, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.add(null));
    }

    @Test
    void add_differentOrientations_throwsIllegalArgumentException() {
        SharedVector row = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> row.add(col));
        assertThrows(IllegalArgumentException.class, () -> col.add(row));
    }

    @Test
    void addingRowToColumn_isNotAllowed_andThrows_withOrientationMessage() {
        SharedVector row = new SharedVector(new double[] { 5.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector column = new SharedVector(new double[] { 5.0 }, VectorOrientation.COLUMN_MAJOR);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> row.add(column));
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        assertTrue(
                msg.contains("orientation") || msg.contains("row") || msg.contains("column")
                        || msg.contains("different"),
                "exception message should indicate incompatible orientations");
    }

    // Tests for dot() method

    @Test
    void testDot_ValidComputation() {
        SharedVector rowVector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { 4.0, 5.0, 6.0 }, VectorOrientation.COLUMN_MAJOR);

        double result = rowVector.dot(colVector);

        assertEquals(32.0, result, DELTA); // 1*4 + 2*5 + 3*6 = 32
    }

    @Test
    void testDot_ZeroVectors() {
        SharedVector rowVector = new SharedVector(new double[] { 0.0, 0.0, 0.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { 0.0, 0.0, 0.0 }, VectorOrientation.COLUMN_MAJOR);

        double result = rowVector.dot(colVector);

        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testDot_NegativeValues() {
        SharedVector rowVector = new SharedVector(new double[] { -1.0, -2.0, -3.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { 4.0, 5.0, 6.0 }, VectorOrientation.COLUMN_MAJOR);

        double result = rowVector.dot(colVector);

        assertEquals(-32.0, result, DELTA); // -1*4 + -2*5 + -3*6 = -32
    }

    @Test
    void testDot_MixedValues() {
        SharedVector rowVector = new SharedVector(new double[] { 1.0, -2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { -4.0, 5.0, -6.0 }, VectorOrientation.COLUMN_MAJOR);

        double result = rowVector.dot(colVector);

        assertEquals(-32.0, result, DELTA); // 1*-4 + -2*5 + 3*-6 = -32
    }

    @Test
    void testDot_SingleElement() {
        SharedVector rowVector = new SharedVector(new double[] { 5.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { 3.0 }, VectorOrientation.COLUMN_MAJOR);

        double result = rowVector.dot(colVector);

        assertEquals(15.0, result, DELTA);
    }

    @Test
    void testDot_DoesNotMutateInputs() {
        double[] rowData = { 1.0, 2.0, 3.0 };
        double[] colData = { 4.0, 5.0, 6.0 };
        SharedVector rowVector = new SharedVector(rowData.clone(), VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(colData.clone(), VectorOrientation.COLUMN_MAJOR);

        rowVector.dot(colVector);

        for (int i = 0; i < rowVector.length(); i++) {
            assertEquals(rowData[i], rowVector.get(i), DELTA);
            assertEquals(colData[i], colVector.get(i), DELTA);
        }
    }

    @Test
    void testDot_NullVector_ThrowsException() {
        SharedVector rowVector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);

        assertThrows(NullPointerException.class, () -> {
            rowVector.dot(null);
        });
    }

    @Test
    void testDot_IncompatibleDimensions_ThrowsException() {
        SharedVector rowVector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { 4.0, 5.0 }, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> {
            rowVector.dot(colVector);
        });
    }

    @Test
    void testDot_ThisNotRowMajor_ThrowsException() {
        SharedVector colVector1 = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.COLUMN_MAJOR);
        SharedVector colVector2 = new SharedVector(new double[] { 4.0, 5.0, 6.0 }, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> {
            colVector1.dot(colVector2);
        });
    }

    @Test
    void testDot_OtherNotColumnMajor_ThrowsException() {
        SharedVector rowVector1 = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector rowVector2 = new SharedVector(new double[] { 4.0, 5.0, 6.0 }, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> {
            rowVector1.dot(rowVector2);
        });
    }

    // Tests for negate() method

    @Test
    void testNegate_ValidComputation() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);

        vector.negate();

        assertEquals(-1.0, vector.get(0), DELTA);
        assertEquals(-2.0, vector.get(1), DELTA);
        assertEquals(-3.0, vector.get(2), DELTA);
    }

    @Test
    void testNegate_NegativeValues() {
        SharedVector vector = new SharedVector(new double[] { -1.0, -2.0, -3.0 }, VectorOrientation.COLUMN_MAJOR);

        vector.negate();

        assertEquals(1.0, vector.get(0), DELTA);
        assertEquals(2.0, vector.get(1), DELTA);
        assertEquals(3.0, vector.get(2), DELTA);
    }

    @Test
    void testNegate_MixedValues() {
        SharedVector vector = new SharedVector(new double[] { 1.0, -2.0, 3.0, -4.0 }, VectorOrientation.ROW_MAJOR);

        vector.negate();

        assertEquals(-1.0, vector.get(0), DELTA);
        assertEquals(2.0, vector.get(1), DELTA);
        assertEquals(-3.0, vector.get(2), DELTA);
        assertEquals(4.0, vector.get(3), DELTA);
    }

    @Test
    void testNegate_ZeroVector() {
        SharedVector vector = new SharedVector(new double[] { 0.0, 0.0, 0.0 }, VectorOrientation.ROW_MAJOR);

        vector.negate();

        assertEquals(0.0, vector.get(0), DELTA);
        assertEquals(0.0, vector.get(1), DELTA);
        assertEquals(0.0, vector.get(2), DELTA);
    }

    @Test
    void testNegate_SingleElement() {
        SharedVector vector = new SharedVector(new double[] { 5.0 }, VectorOrientation.COLUMN_MAJOR);

        vector.negate();

        assertEquals(-5.0, vector.get(0), DELTA);
    }

    @Test
    void testNegate_DoubleMutation() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);

        vector.negate();
        vector.negate();

        assertEquals(1.0, vector.get(0), DELTA);
        assertEquals(2.0, vector.get(1), DELTA);
        assertEquals(3.0, vector.get(2), DELTA);
    }

    @Test
    void testNegate_PreservesOrientation() {
        SharedVector rowVector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.COLUMN_MAJOR);

        rowVector.negate();
        colVector.negate();

        assertEquals(VectorOrientation.ROW_MAJOR, rowVector.getOrientation());
        assertEquals(VectorOrientation.COLUMN_MAJOR, colVector.getOrientation());
    }

    @Test
    void testNegate_PreservesLength() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0, 4.0 }, VectorOrientation.ROW_MAJOR);
        int originalLength = vector.length();

        vector.negate();

        assertEquals(originalLength, vector.length());
    }

    // Tests for vecMatMul() method

    @Test
    void testVecMatMul_ValidComputation_2x2() {
        // Vector: [1, 2] (row)
        // Matrix: [[1, 3], (2 rows, 2 columns)
        // [2, 4]]
        // Result: [1*1 + 2*2, 1*3 + 2*4] = [5, 11]
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 3.0 },
                { 2.0, 4.0 }
        });

        vector.vecMatMul(matrix);

        assertEquals(2, vector.length());
        assertEquals(5.0, vector.get(0), DELTA);
        assertEquals(11.0, vector.get(1), DELTA);
    }

    @Test
    void testVecMatMul_ValidComputation_3x2() {
        // Vector: [1, 2, 3] (row)
        // Matrix: [[1, 4], (3 rows, 2 columns)
        // [2, 5],
        // [3, 6]]
        // Result: [1*1 + 2*2 + 3*3, 1*4 + 2*5 + 3*6] = [14, 32]
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 4.0 },
                { 2.0, 5.0 },
                { 3.0, 6.0 }
        });

        vector.vecMatMul(matrix);

        assertEquals(2, vector.length());
        assertEquals(14.0, vector.get(0), DELTA);
        assertEquals(32.0, vector.get(1), DELTA);
    }

    @Test
    void testVecMatMul_SingleElement() {
        // Vector: [5] (row)
        // Matrix: [[3]] (1 row, 1 column)
        // Result: [5*3] = [15]
        SharedVector vector = new SharedVector(new double[] { 5.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] { { 3.0 } });

        vector.vecMatMul(matrix);

        assertEquals(1, vector.length());
        assertEquals(15.0, vector.get(0), DELTA);
    }

    @Test
    void testVecMatMul_ZeroVector() {
        // Vector: [0, 0] (row)
        // Matrix: [[1, 3], (2 rows, 2 columns)
        // [2, 4]]
        // Result: [0, 0]
        SharedVector vector = new SharedVector(new double[] { 0.0, 0.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 3.0 },
                { 2.0, 4.0 }
        });

        vector.vecMatMul(matrix);

        assertEquals(2, vector.length());
        assertEquals(0.0, vector.get(0), DELTA);
        assertEquals(0.0, vector.get(1), DELTA);
    }

    @Test
    void testVecMatMul_NegativeValues() {
        // Vector: [-1, -2] (row)
        // Matrix: [[3, 5], (2 rows, 2 columns)
        // [4, 6]]
        // Result: [-1*3 + -2*4, -1*5 + -2*6] = [-11, -17]
        SharedVector vector = new SharedVector(new double[] { -1.0, -2.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 3.0, 5.0 },
                { 4.0, 6.0 }
        });

        vector.vecMatMul(matrix);

        assertEquals(2, vector.length());
        assertEquals(-11.0, vector.get(0), DELTA);
        assertEquals(-17.0, vector.get(1), DELTA);
    }

    @Test
    void testVecMatMul_MixedValues() {
        // Vector: [1, -2, 3] (row)
        // Matrix: [[2, -1], (3 rows, 2 columns)
        // [-3, 4],
        // [1, -2]]
        // Result: [1*2 + -2*-3 + 3*1, 1*-1 + -2*4 + 3*-2] = [11, -15]
        SharedVector vector = new SharedVector(new double[] { 1.0, -2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 2.0, -1.0 },
                { -3.0, 4.0 },
                { 1.0, -2.0 }
        });

        vector.vecMatMul(matrix);

        assertEquals(2, vector.length());
        assertEquals(11.0, vector.get(0), DELTA);
        assertEquals(-15.0, vector.get(1), DELTA);
    }

    @Test
    void testVecMatMul_ChangesVectorLength() {
        // Vector: [1, 2, 3] (row)
        // Matrix: [[1, 5, 9, 13], (3 rows, 4 columns)
        // [2, 6, 10, 14],
        // [3, 7, 11, 15]]
        // Result length should be 4
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 5.0, 9.0, 13.0 },
                { 2.0, 6.0, 10.0, 14.0 },
                { 3.0, 7.0, 11.0, 15.0 }
        });

        int originalLength = vector.length();
        vector.vecMatMul(matrix);

        assertEquals(4, vector.length());
        assertTrue(vector.length() > originalLength);
    }

    @Test
    void testVecMatMul_ReducesVectorLength() {
        // Vector: [1, 2, 3, 4] (row)
        // Matrix: [[1, 3], (4 rows, 2 columns)
        // [2, 4],
        // [5, 7],
        // [6, 8]]
        // Result length should be 2
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0, 4.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 3.0 },
                { 2.0, 4.0 },
                { 5.0, 7.0 },
                { 6.0, 8.0 }
        });

        int originalLength = vector.length();
        vector.vecMatMul(matrix);

        assertEquals(2, vector.length());
        assertTrue(vector.length() < originalLength);
    }

    @Test
    void testVecMatMul_PreservesOrientation() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 3.0 },
                { 2.0, 4.0 }
        });

        vector.vecMatMul(matrix);

        assertEquals(VectorOrientation.ROW_MAJOR, vector.getOrientation());
    }

    @Test
    void testVecMatMul_NullMatrix_ThrowsException() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(null);
        });
    }

    @Test
    void testVecMatMul_EmptyMatrix_ThrowsException() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix emptyMatrix = new SharedMatrix();

        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(emptyMatrix);
        });
    }

    @Test
    void testVecMatMul_IncompatibleDimensions_VectorTooShort_ThrowsException() {
        // Vector: [1, 2] (length 2)
        // Matrix: 3 rows × 2 columns
        // Incompatible: vector length (2) ≠ matrix rows (3)
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 4.0 },
                { 2.0, 5.0 },
                { 3.0, 6.0 }
        });

        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        });
    }

    @Test
    void testVecMatMul_IncompatibleDimensions_VectorTooLong_ThrowsException() {
        // Vector: [1, 2, 3] (length 3)
        // Matrix: 2 rows × 2 columns
        // Incompatible: vector length (3) ≠ matrix rows (2)
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 3.0 },
                { 2.0, 4.0 }
        });

        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        });
    }

    @Test
    void testVecMatMul_VectorNotRowMajor_ThrowsException() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(new double[][] {
                { 1.0, 3.0 },
                { 2.0, 4.0 }
        });

        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        });
    }

    @Test
    void testVecMatMul_MatrixNotColumnMajor_ThrowsException() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0 }, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadRowMajor(new double[][] {
                { 1.0, 2.0 },
                { 3.0, 4.0 }
        });

        assertThrows(IllegalArgumentException.class, () -> {
            vector.vecMatMul(matrix);
        });
    }
    // Tests for transpose() method

    @Test
    void testTranspose_RowToColumn() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);

        vector.transpose();

        assertEquals(VectorOrientation.COLUMN_MAJOR, vector.getOrientation());
    }

    @Test
    void testTranspose_ColumnToRow() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.COLUMN_MAJOR);

        vector.transpose();

        assertEquals(VectorOrientation.ROW_MAJOR, vector.getOrientation());
    }

    @Test
    void testTranspose_DoubleMutation() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        VectorOrientation original = vector.getOrientation();

        vector.transpose();
        vector.transpose();

        assertEquals(original, vector.getOrientation());
    }

    @Test
    void testTranspose_PreservesValues() {
        double[] data = { 1.0, 2.0, 3.0, 4.0 };
        SharedVector vector = new SharedVector(data.clone(), VectorOrientation.ROW_MAJOR);

        vector.transpose();

        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], vector.get(i), DELTA);
        }
    }

    @Test
    void testTranspose_PreservesLength() {
        SharedVector vector = new SharedVector(new double[] { 1.0, 2.0, 3.0 }, VectorOrientation.ROW_MAJOR);
        int originalLength = vector.length();

        vector.transpose();

        assertEquals(originalLength, vector.length());
    }

    @Test
    void testTranspose_SingleElement() {
        SharedVector vector = new SharedVector(new double[] { 5.0 }, VectorOrientation.ROW_MAJOR);

        vector.transpose();

        assertEquals(VectorOrientation.COLUMN_MAJOR, vector.getOrientation());
        assertEquals(5.0, vector.get(0), DELTA);
    }

    @Test
    void testTranspose_ZeroVector() {
        SharedVector vector = new SharedVector(new double[] { 0.0, 0.0, 0.0 }, VectorOrientation.COLUMN_MAJOR);

        vector.transpose();

        assertEquals(VectorOrientation.ROW_MAJOR, vector.getOrientation());
        assertEquals(0.0, vector.get(0), DELTA);
    }

    @Test
    void testTranspose_NegativeValues() {
        SharedVector vector = new SharedVector(new double[] { -1.0, -2.0, -3.0 }, VectorOrientation.ROW_MAJOR);

        vector.transpose();

        assertEquals(VectorOrientation.COLUMN_MAJOR, vector.getOrientation());
        assertEquals(-1.0, vector.get(0), DELTA);
        assertEquals(-2.0, vector.get(1), DELTA);
        assertEquals(-3.0, vector.get(2), DELTA);
    }

}
