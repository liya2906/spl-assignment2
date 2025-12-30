package spl.lae;

import java.io.IOException;
import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {

        LinearAlgebraEngine LAE = new LinearAlgebraEngine(10);
        InputParser inputParser = new InputParser();

        try {
            ComputationNode root = inputParser.parse("example.json");
            ComputationNode result = LAE.run(root);
            OutputWriter.write(result.getMatrix(), "My_out.json");

        } catch (Exception e) {
            OutputWriter.write("Error: " + e.getMessage(), "My_out.json");
        } finally {
            System.out.println(LAE.getWorkerReport());
        }
    }
}