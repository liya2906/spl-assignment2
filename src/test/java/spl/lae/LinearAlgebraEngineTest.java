package spl.lae;

import org.junit.jupiter.api.Test;
import parser.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTest {

    private LinearAlgebraEngine engine;

    // ========================
    // 1. Constructor & Initialization
    // ========================

    @Test
    void testConstructor_ValidThreadCount_CreatesEngine() {
        engine = new LinearAlgebraEngine(4);
        assertNotNull(engine);
    }

    @Test
    void testConstructor_SingleThread_CreatesEngine() {
        engine = new LinearAlgebraEngine(1);
        assertNotNull(engine);
    }

    @Test
    void testConstructor_ZeroThreads_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine = new LinearAlgebraEngine(0);
        });
    }

    @Test
    void testConstructor_NegativeThreads_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine = new LinearAlgebraEngine(-5);
        });
    }

    // ========================
    // 2. run(ComputationNode)
    // ========================

    @Test
    void testRun_NullComputationRoot_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(null);
        });
    }

    @Test
    void testRun_SimpleAddition_ReturnsResolvedNode() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(left, right));

        ComputationNode result = engine.run(root);
        
        assertNotNull(result);
        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        double[][] expected = {{6, 8}, {10, 12}};
        assertMatrixEquals(expected, result.getMatrix());
    }

    @Test
    void testRun_SimpleMultiplication_ReturnsCorrectResult() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{2, 0}, {1, 3}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(left, right));

        ComputationNode result = engine.run(root);
        
        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        double[][] expected = {{4, 6}, {10, 12}};
        assertMatrixEquals(expected, result.getMatrix());
    }

    @Test
    void testRun_NestedExpression_ResolvesCorrectly() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{1, 1}, {1, 1}};
        double[][] m3 = {{2, 2}, {2, 2}};
        
        ComputationNode leaf1 = new ComputationNode(m1);
        ComputationNode leaf2 = new ComputationNode(m2);
        ComputationNode add = new ComputationNode(ComputationNodeType.ADD, List.of(leaf1, leaf2));
        ComputationNode leaf3 = new ComputationNode(m3);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(add, leaf3));

        ComputationNode result = engine.run(root);
        
        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        double[][] expected = {{4, 5}, {6, 7}};
        assertMatrixEquals(expected, result.getMatrix());
    }

    // ========================
    // 3. ADD Operation
    // ========================

    @Test
    void testAdd_CorrectAddition_2x2Matrices() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{10, 20}, {30, 40}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(left, right));

        engine.run(root);
        
        double[][] expected = {{11, 22}, {33, 44}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testAdd_CorrectAddition_3x3Matrices() {
        engine = new LinearAlgebraEngine(4);
        double[][] m1 = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        double[][] m2 = {{9, 8, 7}, {6, 5, 4}, {3, 2, 1}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(left, right));

        engine.run(root);
        
        double[][] expected = {{10, 10, 10}, {10, 10, 10}, {10, 10, 10}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testAdd_LessThanTwoChildren_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        
        ComputationNode child = new ComputationNode(m1);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(child));

        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(root);
        });
    }

    // ========================
    // 4. MULTIPLY Operation
    // ========================

    @Test
    void testMultiply_CorrectMultiplication_2x2Matrices() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(left, right));

        engine.run(root);
        
        double[][] expected = {{19, 22}, {43, 50}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testMultiply_IdentityMatrix_ReturnsOriginal() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{2, 3}, {4, 5}};
        double[][] identity = {{1, 0}, {0, 1}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(identity);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(left, right));

        engine.run(root);
        
        assertMatrixEquals(m1, root.getMatrix());
    }

    @Test
    void testMultiply_LessThanTwoChildren_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        
        ComputationNode child = new ComputationNode(m1);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(child));

        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(root);
        });
    }

    // ========================
    // 5. NEGATE Operation
    // ========================

    @Test
    void testNegate_PositiveValues_BecomesNegative() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        
        ComputationNode child = new ComputationNode(m1);
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, List.of(child));

        engine.run(root);
        
        double[][] expected = {{-1, -2}, {-3, -4}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testNegate_MixedValues_NegatesCorrectly() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, -2}, {-3, 4}};
        
        ComputationNode child = new ComputationNode(m1);
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, List.of(child));

        engine.run(root);
        
        double[][] expected = {{-1, 2}, {3, -4}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testNegate_ZeroChildren_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, List.of());

        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(root);
        });
    }

    @Test
    void testNegate_TwoChildren_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, List.of(left, right));

        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(root);
        });
    }

    // ========================
    // 6. TRANSPOSE Operation
    // ========================

    @Test
    void testTranspose_2x2Matrix_TransposesCorrectly() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        
        ComputationNode child = new ComputationNode(m1);
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, List.of(child));

        engine.run(root);
        
        double[][] expected = {{1, 3}, {2, 4}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testTranspose_3x2Matrix_TransposesCorrectly() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}, {5, 6}};
        
        ComputationNode child = new ComputationNode(m1);
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, List.of(child));

        engine.run(root);
        
        double[][] expected = {{1, 3, 5}, {2, 4, 6}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testTranspose_ZeroChildren_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, List.of());

        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(root);
        });
    }

    @Test
    void testTranspose_TwoChildren_ThrowsException() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, List.of(left, right));

        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(root);
        });
    }

    // ========================
    // 7. Edge Cases
    // ========================


    @Test
    void testMixedOperations_AddThenMultiply_ReturnsCorrectResult() {
        engine = new LinearAlgebraEngine(4);
        double[][] m1 = {{1, 0}, {0, 1}};
        double[][] m2 = {{1, 1}, {1, 1}};
        double[][] m3 = {{2, 0}, {0, 2}};
        
        ComputationNode leaf1 = new ComputationNode(m1);
        ComputationNode leaf2 = new ComputationNode(m2);
        ComputationNode add = new ComputationNode(ComputationNodeType.ADD, List.of(leaf1, leaf2));
        ComputationNode leaf3 = new ComputationNode(m3);
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(add, leaf3));

        engine.run(root);
        
        double[][] expected = {{4, 2}, {2, 4}};
        assertMatrixEquals(expected, root.getMatrix());
    }

    @Test
    void testTwoIndependentEngines_WorkCorrectly() {
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{1, 1}, {1, 1}};
        
        // First run with new engine
        engine = new LinearAlgebraEngine(2);
        ComputationNode left1 = new ComputationNode(m1);
        ComputationNode right1 = new ComputationNode(m2);
        ComputationNode root1 = new ComputationNode(ComputationNodeType.ADD, List.of(left1, right1));
        engine.run(root1);
        
        double[][] expected1 = {{2, 3}, {4, 5}};
        assertMatrixEquals(expected1, root1.getMatrix());

        // Second run with new engine instance
        engine = new LinearAlgebraEngine(2);
        ComputationNode left2 = new ComputationNode(m1);
        ComputationNode right2 = new ComputationNode(m2);
        ComputationNode root2 = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(left2, right2));
        engine.run(root2);
        
        double[][] expected2 = {{3, 3}, {7, 7}};
        assertMatrixEquals(expected2, root2.getMatrix());
    }

    // ========================
    // 8. Worker Integration
    // ========================

    @Test
    void testGetWorkerReport_ReturnsNonEmptyString() {
        engine = new LinearAlgebraEngine(2);
        String report = engine.getWorkerReport();
        assertNotNull(report);
        assertFalse(report.isEmpty());
    }

    @Test
    void testGetWorkerReport_ContainsWorkerInfo() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{1, 1}, {1, 1}};
        
        ComputationNode left = new ComputationNode(m1);
        ComputationNode right = new ComputationNode(m2);
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, List.of(left, right));
        engine.run(root);

        String report = engine.getWorkerReport();
        assertTrue(report.contains("Worker"));
        assertTrue(report.contains("Busy") || report.contains("Fatigue"));
    }

    @Test
    void testRun_ShutsDownExecutor_AfterCompletion() {
        engine = new LinearAlgebraEngine(2);
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{1, 1}, {1, 1}};
        
        // First run completes successfully
        ComputationNode left1 = new ComputationNode(m1);
        ComputationNode right1 = new ComputationNode(m2);
        ComputationNode root1 = new ComputationNode(ComputationNodeType.ADD, List.of(left1, right1));
        engine.run(root1);
        
        double[][] expected1 = {{2, 3}, {4, 5}};
        assertMatrixEquals(expected1, root1.getMatrix());

        // Attempting to run again with the same engine should fail
        // because the executor was shut down inside run()
        ComputationNode left2 = new ComputationNode(m1);
        ComputationNode right2 = new ComputationNode(m2);
        ComputationNode root2 = new ComputationNode(ComputationNodeType.MULTIPLY, List.of(left2, right2));
        
        assertThrows(RuntimeException.class, () -> {
            engine.run(root2);
        });
    }

    // ========================
    // Helper Methods
    // ========================

    private void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length, "Row count mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], 0.0001, "Row " + i + " mismatch");
        }
    }
}
