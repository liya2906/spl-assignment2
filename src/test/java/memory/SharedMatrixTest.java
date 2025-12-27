package memory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SharedMatrixTest {

    private static final double DELTA = 1e-9;
    // Tests for SharedMatrix Construction & Initialization

@Test
void testEmptyConstructor_CreatesEmptyMatrix() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertEquals(0, matrix.length());
}

@Test
void testConstructorWithArray_CreatesRowMajorMatrix() {
    double[][] data = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    };
    
    SharedMatrix matrix = new SharedMatrix(data);
    
    assertEquals(2, matrix.length());
    assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
}

@Test
void testConstructorWithArray_StoresCorrectValues() {
    double[][] data = {
        {1.0, 2.0},
        {3.0, 4.0}
    };
    
    SharedMatrix matrix = new SharedMatrix(data);
    
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
    assertEquals(2.0, matrix.get(0).get(1), DELTA);
    assertEquals(3.0, matrix.get(1).get(0), DELTA);
    assertEquals(4.0, matrix.get(1).get(1), DELTA);
}

@Test
void testConstructorWithSingleRow() {
    double[][] data = {{1.0, 2.0, 3.0}};
    
    SharedMatrix matrix = new SharedMatrix(data);
    
    assertEquals(1, matrix.length());
    assertEquals(3, matrix.get(0).length());
}

@Test
void testConstructorWithSingleColumn() {
    double[][] data = {{1.0}, {2.0}, {3.0}};
    
    SharedMatrix matrix = new SharedMatrix(data);
    
    assertEquals(3, matrix.length());
    assertEquals(1, matrix.get(0).length());
}

@Test
void testLoadRowMajor_InitializesCorrectly() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0},
        {3.0, 4.0}
    };
    
    matrix.loadRowMajor(data);
    
    assertEquals(2, matrix.length());
    assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
}

@Test
void testLoadRowMajor_StoresCorrectValues() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    };
    
    matrix.loadRowMajor(data);
    
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
    assertEquals(2.0, matrix.get(0).get(1), DELTA);
    assertEquals(3.0, matrix.get(0).get(2), DELTA);
    assertEquals(4.0, matrix.get(1).get(0), DELTA);
}

@Test
void testLoadRowMajor_ReplacesExistingData() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
    
    matrix.loadRowMajor(new double[][]{{3.0, 4.0, 5.0}});
    
    assertEquals(1, matrix.length());
    assertEquals(3, matrix.get(0).length());
    assertEquals(3.0, matrix.get(0).get(0), DELTA);
}

@Test
void testLoadColumnMajor_InitializesCorrectly() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0},
        {3.0, 4.0}
    };
    
    matrix.loadColumnMajor(data);
    
    assertEquals(2, matrix.length());
    assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
}

@Test
void testLoadColumnMajor_ConvertsRowsToColumns() {
    // Input interpreted as:
    // Row 0: [1, 2, 3]
    // Row 1: [4, 5, 6]
    // Stored as:
    // Column 0: [1, 4]
    // Column 1: [2, 5]
    // Column 2: [3, 6]
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    };
    
    matrix.loadColumnMajor(data);
    
    assertEquals(3, matrix.length()); // 3 columns
    assertEquals(2, matrix.get(0).length()); // each column has 2 elements
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
    assertEquals(4.0, matrix.get(0).get(1), DELTA);
    assertEquals(2.0, matrix.get(1).get(0), DELTA);
    assertEquals(5.0, matrix.get(1).get(1), DELTA);
}

@Test
void testLoadColumnMajor_SingleRowCreatesMultipleColumns() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {{1.0, 2.0, 3.0}};
    
    matrix.loadColumnMajor(data);
    
    assertEquals(3, matrix.length());
    assertEquals(1, matrix.get(0).length());
}

@Test
void testLoadColumnMajor_ReplacesExistingData() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadRowMajor(new double[][]{{1.0, 2.0}});
    
    matrix.loadColumnMajor(new double[][]{{3.0, 4.0, 5.0}});
    
    assertEquals(3, matrix.length());
    assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
}

// Tests for readRowMajor()

@Test
void testReadRowMajor_WithRowMajorMatrix() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    };
    matrix.loadRowMajor(data);
    
    double[][] result = matrix.readRowMajor();
    
    assertArrayEquals(data, result);
}

@Test
void testReadRowMajor_WithColumnMajorMatrix() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    };
    matrix.loadColumnMajor(data);
    
    double[][] result = matrix.readRowMajor();
    
    assertArrayEquals(data, result);
}

@Test
void testReadRowMajor_SingleRow() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {{1.0, 2.0, 3.0}};
    matrix.loadRowMajor(data);
    
    double[][] result = matrix.readRowMajor();
    
    assertArrayEquals(data, result);
}

@Test
void testReadRowMajor_SingleColumn() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {{1.0}, {2.0}, {3.0}};
    matrix.loadRowMajor(data);
    
    double[][] result = matrix.readRowMajor();
    
    assertArrayEquals(data, result);
}

@Test
void testReadRowMajor_SquareMatrix() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {1.0, 2.0},
        {3.0, 4.0}
    };
    matrix.loadRowMajor(data);
    
    double[][] result = matrix.readRowMajor();
    
    assertArrayEquals(data, result);
}

@Test
void testReadRowMajor_NegativeValues() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {
        {-1.0, -2.0},
        {-3.0, -4.0}
    };
    matrix.loadRowMajor(data);
    
    double[][] result = matrix.readRowMajor();
    
    assertArrayEquals(data, result);
}

@Test
void testReadRowMajor_EmptyMatrix_ReturnsEmpty() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadRowMajor(new double[0][0]);
    
    double[][] result = matrix.readRowMajor();
    
    assertEquals(0, result.length);
}

@Test
void testReadRowMajor_DoesNotMutateOriginal() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
    matrix.loadRowMajor(data);
    
    double[][] result = matrix.readRowMajor();
    result[0][0] = 999.0;
    
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
}

// Tests for get(int index)

@Test
void testGet_ValidIndex_ReturnsCorrectVector() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{
        {1.0, 2.0},
        {3.0, 4.0}
    });
    
    SharedVector vector = matrix.get(0);
    
    assertEquals(1.0, vector.get(0), DELTA);
    assertEquals(2.0, vector.get(1), DELTA);
}

@Test
void testGet_LastIndex_ReturnsCorrectVector() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{
        {1.0, 2.0},
        {3.0, 4.0}
    });
    
    SharedVector vector = matrix.get(1);
    
    assertEquals(3.0, vector.get(0), DELTA);
    assertEquals(4.0, vector.get(1), DELTA);
}

@Test
void testGet_NegativeIndex_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
    
    assertThrows(IllegalArgumentException.class, () -> {
        matrix.get(-1);
    });
}

@Test
void testGet_IndexEqualToLength_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
    
    assertThrows(IllegalArgumentException.class, () -> {
        matrix.get(1);
    });
}

@Test
void testGet_IndexGreaterThanLength_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
    
    assertThrows(IllegalArgumentException.class, () -> {
        matrix.get(10);
    });
}

@Test
void testGet_OnEmptyMatrix_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertThrows(IllegalArgumentException.class, () -> {
        matrix.get(0);
    });
}

// Tests for length()

@Test
void testLength_EmptyMatrix_ReturnsZero() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertEquals(0, matrix.length());
}

@Test
void testLength_SingleRow_ReturnsOne() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
    
    assertEquals(1, matrix.length());
}

@Test
void testLength_MultipleRows_ReturnsCorrectCount() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{
        {1.0, 2.0},
        {3.0, 4.0},
        {5.0, 6.0}
    });
    
    assertEquals(3, matrix.length());
}

@Test
void testLength_ColumnMajor_ReturnsNumberOfColumns() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadColumnMajor(new double[][]{
        {1.0, 2.0, 3.0},
        {4.0, 5.0, 6.0}
    });
    
    assertEquals(3, matrix.length());
}

@Test
void testLength_AfterReload_ReturnsNewLength() {
    SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0, 2.0}});
    
    matrix.loadRowMajor(new double[][]{
        {1.0},
        {2.0},
        {3.0}
    });
    
    assertEquals(3, matrix.length());
}

// Tests for getOrientation()

@Test
void testGetOrientation_RowMajor_ReturnsRowMajor() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadRowMajor(new double[][]{{1.0, 2.0}});
    
    assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
}

@Test
void testGetOrientation_ColumnMajor_ReturnsColumnMajor() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadColumnMajor(new double[][]{{1.0, 2.0}});
    
    assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
}

@Test
void testGetOrientation_EmptyMatrix_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertThrows(IllegalArgumentException.class, () -> {
        matrix.getOrientation();
    });
}

@Test
void testGetOrientation_AfterReload_ReturnsNewOrientation() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadRowMajor(new double[][]{{1.0, 2.0}});
    
    matrix.loadColumnMajor(new double[][]{{1.0, 2.0}});
    
    assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
}

// Tests for Invalid / Edge Cases

@Test
void testConstructorWithNull_ThrowsException() {
    assertThrows(NullPointerException.class, () -> {
        new SharedMatrix(null);
    });
}

@Test
void testLoadRowMajor_Null_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertThrows(NullPointerException.class, () -> {
        matrix.loadRowMajor(null);
    });
}

@Test
void testLoadColumnMajor_Null_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertThrows(NullPointerException.class, () -> {
        matrix.loadColumnMajor(null);
    });
}

@Test
void testLoadRowMajor_EmptyArray_CreatesEmptyMatrix() {
    SharedMatrix matrix = new SharedMatrix();
    
    matrix.loadRowMajor(new double[0][0]);
    
    assertEquals(0, matrix.length());
}

@Test
void testLoadColumnMajor_EmptyArray_ThrowsException() {
    SharedMatrix matrix = new SharedMatrix();
    
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
        matrix.loadColumnMajor(new double[0][0]);
    });
}

@Test
void testReadRowMajor_OnEmptyMatrix_ReturnsEmpty() {
    SharedMatrix matrix = new SharedMatrix();
    matrix.loadRowMajor(new double[0][0]);
    
    double[][] result = matrix.readRowMajor();
    
    assertEquals(0, result.length);
}

@Test
void testConstructor_DoesNotMutateInput() {
    double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
    double[][] original = {{1.0, 2.0}, {3.0, 4.0}};
    
    SharedMatrix matrix = new SharedMatrix(data);
    data[0][0] = 999.0;
    
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
    assertArrayEquals(original[0], new double[]{1.0, 2.0});
}

@Test
void testLoadRowMajor_DoesNotMutateInput() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
    
    matrix.loadRowMajor(data);
    data[0][0] = 999.0;
    
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
}

@Test
void testLoadColumnMajor_DoesNotMutateInput() {
    SharedMatrix matrix = new SharedMatrix();
    double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
    
    matrix.loadColumnMajor(data);
    data[0][0] = 999.0;
    
    assertEquals(1.0, matrix.get(0).get(0), DELTA);
}

}
