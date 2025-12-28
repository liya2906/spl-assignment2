package spl.lae;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import parser.ComputationNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that run all JSON test cases from the resources folder.
 * Each JSON file represents a computation that should either:
 * - Execute successfully and match the expected result
 * - Throw an exception (if expectError is true)
 */
class JsonIntegrationTest {

    private LinearAlgebraEngine engine;
    private final TestCaseParser parser = new TestCaseParser();

    @AfterEach
    void tearDown() throws InterruptedException {
        if (engine != null) {
            engine.shutdown();
            engine = null;
        }
    }

    @TestFactory
    Stream<DynamicTest> testAllJsonFiles() {
        File resourcesDir = new File("resources");
        
        if (!resourcesDir.exists() || !resourcesDir.isDirectory()) {
            fail("Resources directory not found: " + resourcesDir.getAbsolutePath());
        }

        File[] jsonFiles = resourcesDir.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            fail("No JSON files found in resources directory");
        }

        Arrays.sort(jsonFiles);

        List<DynamicTest> tests = new ArrayList<>();
        
        for (File jsonFile : jsonFiles) {
            DynamicTest test = DynamicTest.dynamicTest(
                jsonFile.getName(),
                () -> runTestCase(jsonFile)
            );
            tests.add(test);
        }

        return tests.stream();
    }

    private void runTestCase(File jsonFile) throws Exception {
        // Create a fresh engine for each test
        engine = new LinearAlgebraEngine(4);

        TestCase testCase;
        try {
            testCase = parser.parseJsonFile(jsonFile);
        } catch (IOException e) {
            fail("Failed to parse JSON file " + jsonFile.getName() + ": " + e.getMessage());
            return;
        }

        if (testCase.isExpectError()) {
            // Test should throw an exception
            assertThrows(Exception.class, () -> {
                ComputationNode root = parser.buildComputationTree(testCase);
                engine.run(root);
            }, "Expected " + jsonFile.getName() + " to throw an exception");
        } else {
            // Test should execute successfully
            try {
                ComputationNode root = parser.buildComputationTree(testCase);
                ComputationNode result = engine.run(root);
                
                assertNotNull(result, "Result should not be null for " + jsonFile.getName());
                
                double[][] actualMatrix = result.getMatrix();
                double[][] expectedMatrix = testCase.getExpected();
                
                if (expectedMatrix != null) {
                    assertMatrixEquals(expectedMatrix, actualMatrix, jsonFile.getName());
                } else {
                    // If no expected result provided, just verify it doesn't throw
                    assertNotNull(actualMatrix, "Result matrix should not be null for " + jsonFile.getName());
                }
                
            } catch (Exception e) {
                fail("Unexpected exception in " + jsonFile.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    private void assertMatrixEquals(double[][] expected, double[][] actual, String fileName) {
        assertNotNull(actual, "Actual matrix is null for " + fileName);
        assertEquals(expected.length, actual.length, 
            "Row count mismatch in " + fileName + ": expected " + expected.length + " but got " + actual.length);
        
        for (int i = 0; i < expected.length; i++) {
            assertNotNull(actual[i], "Row " + i + " is null in " + fileName);
            assertEquals(expected[i].length, actual[i].length,
                "Column count mismatch in row " + i + " of " + fileName);
            
            assertArrayEquals(expected[i], actual[i], 0.0001,
                "Row " + i + " mismatch in " + fileName + 
                "\nExpected: " + Arrays.toString(expected[i]) + 
                "\nActual: " + Arrays.toString(actual[i]));
        }
    }
}
