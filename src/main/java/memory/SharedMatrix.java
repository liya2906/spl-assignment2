package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // initialize empty matrix
        vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        SharedVector[] tempVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            tempVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        vectors = tempVectors;
    }

    public void loadRowMajor(double[][] matrix) {
        // replace internal data with new raw-major matrix
        SharedVector[] tempVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            tempVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        vectors = tempVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // replace internal data with new column-major matrix

        SharedVector[] tempVectors = new SharedVector[matrix[0].length];
        for (int i = 0; i < tempVectors.length; i++) {
            double[] tempVector = new double[matrix.length];
            for (int j = 0; j < tempVector.length; j++) {
                tempVector[j] = matrix[j][i];
            }
            tempVectors[i] = new SharedVector(tempVector, VectorOrientation.COLUMN_MAJOR);
        }
        vectors = tempVectors;
    }

    public double[][] readRowMajor() {
        // return matrix contents as a row-major double[][]

        SharedVector[] vecs = this.vectors; // By storing this.vectors in a local reference,
        // I ensure that later tasks to the matrix’s vectors field
        // do not change the actual array being operated on

        if (vecs == null)
            throw new IllegalArgumentException("current SharedMatrix is empty");

        if (vecs.length == 0 || vecs[0].length()==0)
            return new double[0][0];

        acquireAllVectorReadLocks(vecs);
        try {

            // the SharedMatrix orientation is ROW_MAJOR
            if (vecs[0].getOrientation() == VectorOrientation.ROW_MAJOR) {
                double[][] output = new double[vecs.length][vecs[0].length()];
                for (int i = 0; i < output.length; i++) {
                    for (int j = 0; j < output[i].length; j++) {
                        output[i][j] = vecs[i].get(j);
                    }
                }
                return output;
            }

            // the SharedMatrix orientation is COLUMN_MAJOR
            else {
                double[][] output = new double[vecs[0].length()][vecs.length];
                for (int i = 0; i < output.length; i++) {
                    for (int j = 0; j < output[i].length; j++) {
                        output[i][j] = vecs[j].get(i);
                    }
                }
                return output;
            }
        } finally {
            releaseAllVectorReadLocks(vecs);
        }
    }

    public SharedVector get(int index) {
        // return vector at index

        if (index >= this.vectors.length || index < 0)
            throw new IllegalArgumentException("index out of bounds");
        return vectors[index];

    }

    public int length() {
        // return number of stored vectors
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        // return orientation
        if (vectors == null || vectors.length == 0 || vectors[0].length()==0)
            throw new IllegalArgumentException("current SharedMatrix is empty");
        return this.vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // acquire read lock for each vector
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // release read locks
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // acquire write lock for each vector
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // release write locks
        for (int i = 0; i < vecs.length; i++) {
            vecs[i].writeUnlock();
        }
    }
}
