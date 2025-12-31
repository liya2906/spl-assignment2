package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        // Read lock: reading shared data while other threads may also read,
        // but no thread may write at the same time.
        readLock();
        try {
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        // Read-only access → read lock
        // Length depends on the vector array which may be replaced later.
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // Read-only access → read lock
        // Orientation is shared state and may be modified by transpose()
        readLock();
        try {
            return orientation;
        } finally {
            readUnlock();
        }
    }

    public void writeLock() {
        this.lock.writeLock().lock();
    }

    public void writeUnlock() {
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        this.lock.readLock().unlock();
    }


    public void transpose() {
        /*
         * Transpose changes the orientation of the vector.
         * This is a write operation → requires write lock.
         */
        writeLock();
        try {
            switch (orientation) {
                case ROW_MAJOR -> orientation = VectorOrientation.COLUMN_MAJOR;
                case COLUMN_MAJOR -> orientation = VectorOrientation.ROW_MAJOR;
            }
        } finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        // add two vectors
        if (other == null)
            throw new IllegalArgumentException("cant add, other vector is null");

         /*
         * This vector is modified → write lock
         * Other vector is only read → read lock
         */
        this.writeLock();
        other.readLock();
        try {
            if (vector.length != other.length()) {
                throw new IllegalArgumentException("the vectors are not in the same size");
            }
            if (other.orientation != this.orientation) {
                throw new IllegalArgumentException("Vectors must have the same orientation to add.");
            }
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = this.vector[i] + other.vector[i];
            }

        } finally {
            other.readUnlock();
            this.writeUnlock();
        }
    }

    public void negate() {
        // negate vector
        /*
         * Negation modifies the vector contents,
         * therefore requires exclusive write access.
         */
        this.writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = -vector[i];
            }
        } finally {
            this.writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        // compute dot product (row · column)

        /*
         * Dot product only reads from both vectors,
         * so both are protected using read locks.
         */
        this.readLock();
        other.readLock();
        try {
            if (this.orientation != VectorOrientation.ROW_MAJOR)
                throw new IllegalArgumentException(" this_vector is not ROW_MAJOR, we cant multiply vectors");
            if (other.orientation != VectorOrientation.COLUMN_MAJOR)
                throw new IllegalArgumentException("other_vector is not COLUMN_MAJOR, we cant multiply vectors");
            if (this.vector.length != other.length()) {
                throw new IllegalArgumentException("invalid dimensions , we cant multiply vectors");
            }
            double output = 0;
            for (int i = 0; i < this.vector.length; i++) {
                output = output + (this.vector[i] * other.vector[i]);
            }
            return output;

        } finally {
            other.readUnlock();
            this.readUnlock();
        }

    }

    public void vecMatMul(SharedMatrix matrix) {
        // compute row-vector × matrix
        if (matrix == null) {
            throw new IllegalArgumentException("matrix is null, we cant multiply matrix with vector");
        }
        if (matrix.length() == 0 || matrix.get(0).length() == 0) {
            throw new IllegalArgumentException("Matrix is empty, we cant multiply matrix with vector");
        }
        if (matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException(" matrix invalid orientation, we cant multiply matrix with vector");
        }

        /*
         * Phase 1: Read-only access to the vector
         * Computes the result into a temporary array.
         */
        this.readLock();
        double[] result = new double[matrix.length()];
        try {

            if (this.length() != matrix.get(0).length()) {
                throw new IllegalArgumentException("  mismatch dimensions , we cant multiply matrix with vector");
            }
            if (this.orientation != VectorOrientation.ROW_MAJOR)
                throw new IllegalArgumentException(" vector invalid orientation, we cant multiply matrix with vector");

            for (int i = 0; i < matrix.length(); i++) {
                result[i] = this.dot(matrix.get(i));
            }
        } finally {
            this.readUnlock();
        }
        /*
         * Phase 2: Write back the result
         * Requires exclusive write access.
         */
        this.writeLock();
        try{
            this.vector = result;

        } finally {
            this.writeUnlock();
        }
    }
}

