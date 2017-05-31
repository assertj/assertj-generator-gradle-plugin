package org.assertj.generator.gradle.util

import com.google.common.base.Preconditions
import org.gradle.api.Action

/**
 * Represents a type that is either one value or another of types {@code L} and {@code R}
 * respectively. 
 * 
 * @param L Left type
 * @param R Right type
 */
abstract class Either<L, R> implements Serializable {
    
    protected Object value
    
    protected Either(Object value) {
        this.value = Preconditions.checkNotNull(value, "value can not be null")
    }

    /**
     * {@code true} if a "right" value
     * @return {@code true} if a "right" value
     */
    abstract boolean isRightValue()

    /**
     * Get the value as Left
     * @return
     */
    abstract R getRight()

    Either<L, R> mapRight(Action<? extends R> action) {
        this
    }
    
    /**
     * {@code true} if a "left" value
     * @return {@code true} if a "left" value
     */
    abstract boolean isLeftValue()

    /**
     * Get the value as Left
     * @return
     */
    abstract L getLeft()

    Either<L, R> mapLeft(Action<? extends L> action) {
        this
    }
    
    /**
     * Creates a new "left" either
     * @param value value to store
     * @return New "left" instance
     */
    static <L, R> Either<L, R> left(L value) {
        new Left(value)
    }

    /**
     * Creates a new "right" either
     * @param value value to store
     * @return New "right" instance
     */
    static <L, R> Either<L, R> right(R value) {
        new Right(value)
    }
    
    static class Left<L, R> extends Either<L, R> implements Serializable {
        
        protected Left(L value) {
            super(value)
        }

        @Override
        boolean isRightValue() {
            false
        }

        @Override
        boolean isLeftValue() {
            true
        }

        @Override
        L getLeft() {
            (L)value
        }

        @Override
        Either<L, R> mapLeft(Action<? extends L> action) {
            action.execute(left)
            
            this
        }

        @Override
        R getRight() {
            throw new IllegalStateException("Can not get right element from left")
        }

        @Override
        String toString() {
            return "Left[" + value + "]"
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(value)
        }

        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            value = (L)input.readObject()
        }
    }

    static class Right<L, R> extends Either<L, R> implements Serializable {

        protected Right(R value) {
            super(value)
        }

        @Override
        boolean isRightValue() {
            true
        }

        @Override
        boolean isLeftValue() {
            false
        }

        @Override
        L getLeft() {
            throw new IllegalStateException("Can not get left element from right")
        }

        @Override
        R getRight() {
            (R)value
        }

        @Override
        Either<L, R> mapRight(Action<? extends R> action) {
            action.execute(right)
            this
        }

        @Override
        String toString() {
            return "Right[" + value + "]"
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(value)
        }
        
        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            value = (R)input.readObject()
        }
    }

    
}
