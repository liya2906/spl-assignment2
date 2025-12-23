package spl.lae;

import org.junit.jupiter.api.Test;
import parser.ComputationNode;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTests {

    private static final double DELTA = 1e-6;

    @Test
    void testSimpleAdd() {
        ComputationNode node = new ComputationNode("+", java.util.List.of(
                new ComputationNode(new double[][]{{1, 2}, {3, 4}}),
                new ComputationNode(new double[][]{{5, 6}, {7, 8}})
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);
        double[][] out = lae.run(node).getMatrix();

        assertEquals(6.0, out[0][0], DELTA);
        assertEquals(12.0, out[1][1], DELTA);
    }

    @Test
    void testNegate() {
        ComputationNode node = new ComputationNode("-", java.util.List.of(
                new ComputationNode(new double[][]{{1, -2}})
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        double[][] out = lae.run(node).getMatrix();

        assertEquals(-1.0, out[0][0], DELTA);
        assertEquals(2.0, out[0][1], DELTA);
    }

    @Test
    void testSimpleMultiply() {
        ComputationNode node = new ComputationNode("*", java.util.List.of(
                new ComputationNode(new double[][]{
                        {1, 2},
                        {3, 4}
                }),
                new ComputationNode(new double[][]{
                        {5, 6},
                        {7, 8}
                })
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);
        double[][] out = lae.run(node).getMatrix();

        assertArrayEquals(new double[]{19, 22}, out[0], DELTA);
        assertArrayEquals(new double[]{43, 50}, out[1], DELTA);
    }

    @Test
    void testMultiplyDimensionMismatch() {
        ComputationNode node = new ComputationNode("*", java.util.List.of(
                new ComputationNode(new double[][]{{1, 2}}),
                new ComputationNode(new double[][]{{1, 2}})
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        assertThrows(IllegalArgumentException.class, () -> lae.run(node));
    }

    @Test
    void testAddWithSingleOperandFails() {
        ComputationNode node = new ComputationNode("+", java.util.List.of(
                new ComputationNode(new double[][]{{1}})
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        assertThrows(IllegalArgumentException.class, () -> lae.run(node));
    }

    @Test
    void testNullRootFails() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        assertThrows(IllegalArgumentException.class, () -> lae.run(null));
    }
}
