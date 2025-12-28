package spl.lae;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Data model for JSON test cases.
 * Represents a computation with operator, operands, expected result, and error flag.
 */
public class TestCase {
    
    @JsonProperty("operator")
    private String operator;
    
    @JsonProperty("operands")
    private List<Object> operands;
    
    @JsonProperty("expected")
    private double[][] expected;
    
    @JsonProperty("expectError")
    private Boolean expectError;

    public TestCase() {
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Object> getOperands() {
        return operands;
    }

    public void setOperands(List<Object> operands) {
        this.operands = operands;
    }

    public double[][] getExpected() {
        return expected;
    }

    public void setExpected(double[][] expected) {
        this.expected = expected;
    }

    public boolean isExpectError() {
        return expectError != null && expectError;
    }

    public void setExpectError(Boolean expectError) {
        this.expectError = expectError;
    }
}
