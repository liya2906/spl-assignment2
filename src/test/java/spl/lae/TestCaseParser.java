package spl.lae;

import com.fasterxml.jackson.databind.ObjectMapper;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON test files and builds ComputationNode trees.
 */
public class TestCaseParser {

    private final ObjectMapper objectMapper;

    public TestCaseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse a JSON file into a TestCase object.
     */
    public TestCase parseJsonFile(File file) throws IOException {
        return objectMapper.readValue(file, TestCase.class);
    }

    /**
     * Build a ComputationNode tree from a TestCase.
     */
    public ComputationNode buildComputationTree(TestCase testCase) {
        if (testCase.getOperator() == null) {
            throw new IllegalArgumentException("Test case must have an operator");
        }

        ComputationNodeType nodeType = mapOperator(testCase.getOperator());
        List<ComputationNode> children = new ArrayList<>();

        if (testCase.getOperands() != null) {
            for (Object operand : testCase.getOperands()) {
                children.add(parseOperand(operand));
            }
        }

        return new ComputationNode(nodeType, children);
    }

    /**
     * Recursively parse an operand which can be either:
     * - A 2D array (matrix) represented as List<List<Number>>
     * - A nested operation represented as Map
     */
    @SuppressWarnings("unchecked")
    private ComputationNode parseOperand(Object operand) {
        if (operand instanceof List) {
            // It's a matrix (2D array)
            List<List<?>> matrixList = (List<List<?>>) operand;
            double[][] matrix = convertToMatrix(matrixList);
            return new ComputationNode(matrix);
        } else if (operand instanceof Map) {
            // It's a nested operation
            Map<String, Object> nestedOp = (Map<String, Object>) operand;
            String operator = (String) nestedOp.get("operator");
            List<Object> nestedOperands = (List<Object>) nestedOp.get("operands");

            ComputationNodeType nodeType = mapOperator(operator);
            List<ComputationNode> children = new ArrayList<>();

            if (nestedOperands != null) {
                for (Object nestedOperand : nestedOperands) {
                    children.add(parseOperand(nestedOperand));
                }
            }

            return new ComputationNode(nodeType, children);
        } else {
            throw new IllegalArgumentException("Unknown operand type: " + operand.getClass());
        }
    }

    /**
     * Convert a List<List<Number>> to double[][].
     */
    private double[][] convertToMatrix(List<List<?>> matrixList) {
        if (matrixList == null || matrixList.isEmpty()) {
            return new double[0][0];
        }

        int rows = matrixList.size();
        int cols = matrixList.get(0).size();
        double[][] matrix = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            List<?> row = matrixList.get(i);
            for (int j = 0; j < row.size(); j++) {
                Object value = row.get(j);
                if (value instanceof Number) {
                    matrix[i][j] = ((Number) value).doubleValue();
                } else {
                    throw new IllegalArgumentException("Matrix contains non-numeric value: " + value);
                }
            }
        }

        return matrix;
    }

    /**
     * Map operator string to ComputationNodeType.
     */
    private ComputationNodeType mapOperator(String operator) {
        switch (operator) {
            case "+":
                return ComputationNodeType.ADD;
            case "*":
                return ComputationNodeType.MULTIPLY;
            case "-":
                return ComputationNodeType.NEGATE;
            case "T":
                return ComputationNodeType.TRANSPOSE;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }
}
