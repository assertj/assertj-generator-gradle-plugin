package org.assertj.generator.gradle.util

import com.google.common.base.Preconditions
import com.google.common.reflect.TypeToken
import org.codehaus.groovy.control.ConfigurationException
import org.gradle.api.Action

import java.lang.reflect.Field

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

    /**
     * Mixin used for sharing common behaviour regarding Either properties
     */
    trait EitherPropertyMixin {

        /**
         * Override the `setProperty()` so that we can assign either side of an "Either" type
         */
        void setProperty(String name, Object value) {
            def prop
            try {
                prop = this.metaClass.getMetaProperty(name)
            } catch (MissingPropertyException ignore) {
                throw new ConfigurationException("Property with name ${name} does not exist for ${this.metaClass.class}, check spelling.")
            }

            if (prop && value && Either.class.isAssignableFrom(prop.type)) {
                // handle the either type
                TypeToken<?> base = TypeToken.of(getClass())

                Field field = base.getRawType().getDeclaredField(prop.name)
                Preconditions.checkNotNull(field, "Property name is wrong? %s", prop)

                def eitherToken = base.resolveType(field.genericType)
                def leftToken = eitherToken.resolveType(Either.class.typeParameters[0])
                def rightToken = eitherToken.resolveType(Either.class.typeParameters[1])

                if (leftToken.isSupertypeOf(value.class)) {
                    // it's a "left"
                    // IntelliJ is WRONG with this inspection, due to being a mixin it needs FQN
                    //noinspection UnnecessaryQualifiedReference
                    this.metaClass.setProperty(this, name, Either.left(value))
                } else if (rightToken.isSupertypeOf(value.class)) {
                    // IntelliJ is WRONG with this inspection, due to being a mixin it needs FQN
                    //noinspection UnnecessaryQualifiedReference 
                    this.metaClass.setProperty(this, name, Either.right(value))
                } else {
                    throw new ClassCastException(String.format("Can not assign type into %s %s", value.class, prop.class))
                }
            } else {
                this.metaClass.setProperty(this, name, value)
            }
        }

        /**
         * Traverse across all properties extracting the {@link Either} types that have the correct "left" type and the
         * value associated. 
         *
         * @param leftClazz Type of the left parameter to extract
         * @return Possibly empty list of values
         */
        def <L> List<L> getLeftProperties(Class<L> leftClazz) {
            TypeToken<? extends EitherPropertyMixin> base = (TypeToken<? extends EitherPropertyMixin>)TypeToken.of(getClass())

            this.metaClass.properties.findAll { prop -> Either.class.isAssignableFrom(prop.type) }
                    .findAll { prop ->
                Field f = base.rawType.getDeclaredField(prop.name)
                def leftToken = base.resolveType(f.genericType).resolveType(Either.class.typeParameters[0])
                leftToken.isSubtypeOf(leftClazz)
            }
            .collect { (Either<L, ?>)this.metaClass.getProperty(this, it.name) }
            .findAll { it && it.leftValue }
            .collect { v -> v.left }
        }


        /**
         * Traverse across all properties extracting the {@link Either} types that have the correct "right" type and the
         * value associated. 
         *
         * @param leftClazz Type of the right parameter to extract
         * @return Possibly empty list of values
         */
        def <R> List<R> getRightProperties(Class<R> value) {
            TypeToken<? extends EitherPropertyMixin> base = (TypeToken<? extends EitherPropertyMixin>)TypeToken.of(getClass())

            metaClass.properties.findAll { prop -> Either.class.isAssignableFrom(prop.type) }
                    .findAll { prop ->
                Field f = base.rawType.getDeclaredField(prop.name)
                def rightToken = base.resolveType(f.genericType).resolveType(Either.class.typeParameters[1])
                value.isAssignableFrom(rightToken.rawType)
            }
            .collect { (Either<?, R>)this.metaClass.getProperty(this, it.name) }
            .findAll { it && it.rightValue }
            .collect { v -> v.right }
        }
    }


}
