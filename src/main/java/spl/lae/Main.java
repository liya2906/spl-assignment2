package spl.lae;

import java.io.IOException;
import java.text.ParseException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.err.println("Error: Missing arguments.");
            return;
        }
        int numThreads = Integer.parseInt(args[0]);
        String inputPath = args[1];
        String outputPath = args[2];
        LinearAlgebraEngine lae = new LinearAlgebraEngine(numThreads);
        InputParser inputParser = new InputParser();
        try {
            ComputationNode root = inputParser.parse(inputPath);
            ComputationNode res = lae.run(root);
            OutputWriter.write(res.getMatrix(), outputPath);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            OutputWriter.write("Error: " + e.getMessage(), outputPath);
        } finally {
            System.out.println(lae.getWorkerReport());
        }

    }
}