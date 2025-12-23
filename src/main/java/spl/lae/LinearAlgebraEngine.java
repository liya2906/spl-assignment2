package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        if (computationRoot == null) {
            throw new IllegalArgumentException("computation root should not be null");
        }
        computationRoot.associativeNesting();
        ComputationNode curr = computationRoot.findResolvable();
        while (curr != null){
            loadAndCompute(curr);
            curr = computationRoot.findResolvable();
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // load operand matrices, and create compute tasks & submit tasks to executor
        List<ComputationNode> children = node.getChildren();
        if(node.getChildren() == null || children.isEmpty()) {
            throw new IllegalArgumentException("can't compute, node have no children");
        }
        int numChildren=children.size();
        List<Runnable> tasks = null;
        ComputationNodeType type = node.getNodeType();
        if (type==ComputationNodeType.ADD) {
            if (numChildren<2)
                throw new IllegalArgumentException("cannot ADD if there is less than 2 matrices");
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            rightMatrix.loadRowMajor(children.get(1).getMatrix());
            tasks = createAddTasks();
        }
        else if (type==ComputationNodeType.MULTIPLY){
            if (numChildren<2)
                throw new IllegalArgumentException("cannot MULTIPLY if there is less than 2 matrices");
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            rightMatrix.loadColumnMajor(children.get(1).getMatrix());
            tasks = createMultiplyTasks();
        }
        else if (type==ComputationNodeType.NEGATE) {
            if (numChildren!=1)
                throw new IllegalArgumentException("cannot NEGATE if there is 0/2 matrices");
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            tasks = createNegateTasks();
        }
        else { //(type==ComputationNodeType.TRANSPOSE)
            if (numChildren!=1)
                throw new IllegalArgumentException("cannot TRANSPOSE if there is 0/2 matrices");
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            tasks = createTransposeTasks();
        }

        executor.submitAll(tasks);

        //if (type==ComputationNodeType.TRANSPOSE) // todo: check the transpose
            //leftMatrix.loadRowMajor(leftMatrix.readRowMajor());

        node.resolve(leftMatrix.readRowMajor());
    }

    public List<Runnable> createAddTasks() {
        // return tasks that perform row-wise addition
        if (leftMatrix==null || rightMatrix==null)
            throw new IllegalArgumentException("cannot ADD null matrices");
        if(leftMatrix.length()==0 || rightMatrix.length()==0 ||
                leftMatrix.get(0).length()==0 || rightMatrix.get(0).length()==0)
            throw new IllegalArgumentException("cannot ADD empty matrices");
        if (leftMatrix.getOrientation()!= VectorOrientation.ROW_MAJOR ||
            rightMatrix.getOrientation()!= VectorOrientation.ROW_MAJOR)
            throw new IllegalArgumentException("cannot ADD, M1 or M2 is not ROW_MAJOR");
        if (leftMatrix.length()!=rightMatrix.length() ||
                leftMatrix.get(0).length()!=rightMatrix.get(0).length())
            throw new IllegalArgumentException("cannot ADD, mismatch in matrices sizes");

        List<Runnable> tasks = new ArrayList<>();
        for (int i=0; i< leftMatrix.length() ;i++){
            final int row = i;
            Runnable task = () -> leftMatrix.get(row).add(rightMatrix.get(row));
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // return tasks that perform row × matrix multiplication
        if (leftMatrix==null || rightMatrix==null )
            throw new IllegalArgumentException("cannot MULTIPLY, the matrices are null");
        if(leftMatrix.length()==0 || rightMatrix.length()==0 ||
                leftMatrix.get(0).length()==0 || rightMatrix.get(0).length()==0)
            throw new IllegalArgumentException("cannot MULTIPLY, the matrices are empty");
        if (leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR )
            throw new IllegalArgumentException("cannot MULTIPLY, M1 is not ROW_MAJOR");
        if (rightMatrix.getOrientation()!= VectorOrientation.COLUMN_MAJOR)
            throw new IllegalArgumentException("cannot MULTIPLY, M2 is not COLUMN_MAJOR");
        if (leftMatrix.length()!=rightMatrix.length() ||
                leftMatrix.get(0).length()!=rightMatrix.get(0).length())
            throw new IllegalArgumentException("cannot MULTIPLY, mismatch in matrices sizes");

        List<Runnable> tasks = new ArrayList<>();
        for (int i=0; i< leftMatrix.length() ;i++){
            final int index = i;
            Runnable task = () -> leftMatrix.get(index).vecMatMul(rightMatrix);
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // return tasks that negate rows
        if (leftMatrix==null )
            throw new IllegalArgumentException("cannot NEGATE, the matrix is null");
        if (leftMatrix.length()==0 || leftMatrix.get(0).length()==0)
            throw new IllegalArgumentException("cannot NEGATE, the matrix is empty");
        if (leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR)
            throw new IllegalArgumentException("cannot NEGATE, the matrix isn't ROW_MAJOR");

        List<Runnable> tasks = new ArrayList<>();
        for (int i=0; i< leftMatrix.length() ;i++){
            final int index = i;
            Runnable task = () -> leftMatrix.get(index).negate();
            tasks.add(task);
        }
        return tasks;
    }

    // todo: check the transpose
    public List<Runnable> createTransposeTasks() {
        // return tasks that transpose rows
        if (leftMatrix==null )
            throw new IllegalArgumentException("cannot TRANSPOSE, the matrix is null");
        if (leftMatrix.length()==0 || leftMatrix.get(0).length()==0)
            throw new IllegalArgumentException("cannot TRANSPOSE, the matrix is empty");
        if (leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR)
            throw new IllegalArgumentException("cannot TRANSPOSE, the matrix isn't ROW_MAJOR");

        List<Runnable> tasks = new ArrayList<>();
        for (int i=0; i< leftMatrix.length() ;i++){
            final int index = i;
            Runnable task = () -> leftMatrix.get(index).transpose();
            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        // return summary of worker activity
        return executor.getWorkerReport();
    }

    public void shutdown() throws InterruptedException {
        try {
            executor.shutdown();
        }
        catch (InterruptedException e) {
            throw new InterruptedException("Executor shutdown interrupted: " + e.getMessage());
        }

    }

}
