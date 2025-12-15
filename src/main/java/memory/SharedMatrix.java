package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
    }

    public SharedMatrix(double[][] matrix) {
        SharedVector[] tempVectors = new SharedVector[matrix.length];
        for (int i =0; i< matrix.length; i++){
            tempVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        vectors = tempVectors;
    }

    public void loadRowMajor(double[][] matrix) {
        SharedVector[] tempVectors = new SharedVector[matrix.length];
        for (int i =0; i< matrix.length; i++){
            tempVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        vectors= tempVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        return null;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return null;
    }

    public int length() {
        // TODO: return number of stored vectors
        return 0;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return null;
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
    }
}
