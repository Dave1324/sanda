package org.sindaryn.sanda.mutations;

import lombok.val;
import org.sindaryn.sanda.reflection.ReflectionCache;

import javax.persistence.ElementCollection;
import java.lang.reflect.Field;
import java.util.Collection;

import static org.sindaryn.sanda.reflection.IReflectionTools.getClassFields;

public interface IEquatable<T> {
    @SuppressWarnings("unchecked")
    default void setEqualTo(T other, ReflectionCache reflectionCache){
        Collection<String> nonUpdatableFields =
                reflectionCache.getNonUpdatableFields(this.getClass());
        Collection<Field> fields =
                reflectionCache
                        .getFieldsOf(
                                this.getClass(),
                                nonUpdatableFields);
        for(Field currentField : fields){
            try {
                currentField.setAccessible(true);
                Object sourceField = currentField.get(other);
                Object targetField = currentField.get(this);
                //if field value is null, there's nothing to update to
                if(sourceField == null) continue;
                //if field is an embedded entity, we need to recursively update all of its fields
                if(isEmbeddedIEquatable(currentField))
                    ((IEquatable) targetField).setEqualTo(sourceField, reflectionCache);
                    //if field is a collection, that's outside of this use case,
                else if(isAssignable(currentField))
                    //else, (...finally) update field value
                    currentField.set(this, sourceField);
                /*else throw new RuntimeException(
                                "Cannot find @IEquatable implementation for field " + currentField.getName() +
                                " in " + this.getClass().getSimpleName());*/
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    default boolean isAssignable(Field currentField) {
        return !Iterable.class.isAssignableFrom(currentField.getType()) || currentField.isAnnotationPresent(ElementCollection.class);
    }

    default boolean isEqualTo(final Object o){
        try {
            if(this == o)
                return true;
            val fields = getClassFields(this.getClass());
            for(Field field : fields){
                field.setAccessible(true);
                if(bothAreNull(field, o)) continue;
                if(oneIsNull(field, o))
                    return false;
                if(isIEquatableAndNotEqual(o, field)){
                    return false;
                }else{
                    Object thisFieldValue = field.get(this);
                    Object otherFieldValue = field.get(o);
                    if(!thisFieldValue.equals(otherFieldValue))
                        return false;
                }
            }
            return true;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    default boolean isIEquatableAndNotEqual(Object o, Field field) throws IllegalAccessException {
        boolean isIEquatable = field.get(this) instanceof IEquatable;
        if(!isIEquatable) return false;
        return ((IEquatable)field.get(this)).isEqualTo(field.get(o));
    }

    default boolean oneIsNull(Field field, Object o){
        try {
            Object thisFieldValue = field.get(this);
            boolean thisIsNull = thisFieldValue == null;
            Object otherFieldValue = field.get(o);
            boolean otherIsNull = otherFieldValue == null;
            return thisIsNull ^ otherIsNull;//XOR
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    default boolean bothAreNull(Field field, Object o){
        try {
            return field.get(this) == null && field.get(o) == null;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    default boolean isEmbeddedIEquatable(Field field) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(this) instanceof IEquatable;
    }
}
