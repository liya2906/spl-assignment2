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

    public void transpose() {
        writeLock();
        try {
            switch (orientation) {
                case ROW_MAJOR -> orientation = VectorOrientation.COLUMN_MAJOR;
                case COLUMN_MAJOR -> orientation = VectorOrientation.ROW_MAJOR;
            }
        }
        finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        this.writeLock();
        other.readLock();
        try {
            if (vector.length != other.length()) {
                throw new IllegalArgumentException("the vectors are not in the same size");
            }
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = this.vector[i] + other.vector[i];
            }

        } finally {
            this.writeUnlock();
            other.readUnlock();


        }
    }

    public void negate() {
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
        this.readLock();
        other.readLock();
        try {
            if (this.orientation != VectorOrientation.ROW_MAJOR)
                throw new IllegalArgumentException(" this_vector is not ROW_MAJOR, we cant multiply vectors");
            if(other.getOrientation() != VectorOrientation.COLUMN_MAJOR )
                throw new IllegalArgumentException( "other_vector is not COLUMN_MAJOR, we cant multiply vectors");
            if (this.vector.length != other.length()) {
                throw new IllegalArgumentException("invalid dimensions , we cant multiply vectors");
            }
            double output = 0;
            for (int i = 0; i < this.vector.length; i++) {
                output = output + (this.vector[i] * other.vector[i]);
            }
            return output;

        } finally {
            this.readUnlock();
            other.readUnlock();


        }

    }

    public void vecMatMul(SharedMatrix matrix) {
        //this.writeLock();
        //for (int i = 0; i < matrix.length(); i++) {
           // matrix.get(i).readLock();

        this.readLock();
        try {

            if (this.length() != matrix.length()) {
                throw new IllegalArgumentException("  mismatch dimensions , we cant multiply matrix with vector");
            }
            if (this.orientation != VectorOrientation.ROW_MAJOR)
                throw new IllegalArgumentException(" vector invalid orientation, we cant multiply matrix with vector");
        }
        finally {
            this.readUnlock();
        }

        if(matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException(" matrix invalid orientation, we cant multiply matrix with vector");
        }

        double[] result = new double[matrix.length()];
        for (int i = 0; i < matrix.length(); i++) {
            result[i] = this.dot(matrix.get(i));

        }
        this.writeLock();
        try{
        this.vector = result;
        }
        finally {
            this.writeUnlock();


//            for (int i = 0; i < matrix.length(); i++) {
//                matrix.get(i).readUnlock();
            }

        }
    }

