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
        readLock();
        try {
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
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

    // todo: check the transpose
    public void transpose() {
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

        this.readLock();
        double[] result = new double[matrix.length()];
        try {

            if (this.length() != matrix.length()) {
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
        this.writeLock();
        try{
            this.vector = result;

        } finally {
            this.writeUnlock();
        }
    }
}

