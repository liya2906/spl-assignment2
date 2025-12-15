package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        computationRoot.associativeNesting();
        ComputationNode curr = computationRoot.findResolvable();
        while (curr != null){
            loadAndCompute(curr);
            curr = computationRoot.findResolvable();
        }

        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        List<ComputationNode> children = node.getChildren();
        leftMatrix =  new SharedMatrix(children.get(0).getMatrix());
        rightMatrix =  new SharedMatrix(children.get(1).getMatrix());


        // TODO: create compute tasks & submit tasks to executor
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        return null;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        return null;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        return null;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        return null;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return null;
    }
}
