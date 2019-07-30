package org.sindaryn.sanda.reflection;

import lombok.val;
import org.sindaryn.sanda.annotations.IEquatableIgnore;
import org.sindaryn.sanda.annotations.IEquatableIgnores;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@lombok.Getter
public class CachedEntityType {

    private Class<?> clazz;
    private Object defaultInstance;
    private Map<String, CachedEntityField> fields;
    private Map<String, Method> publicMethods;

    public Object invokePublicMethod(String methodName, Object entityInstance, Object[] args){
        try {
            Method targetMethod = getMethod(methodName);
            if(args != null && !(args.length == 0))
                return targetMethod.invoke(entityInstance, args);
            else return targetMethod.invoke(entityInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setFieldValue(String targetFieldName, Object value, Object entityInstance){
        Field targetField = getField(targetFieldName);
        targetField.setAccessible(true);
        try {
            if(fields.get(targetFieldName).isCollectionOrMap()){
                value = adaptIterableType(targetField, value);
            }
            targetField.set(entityInstance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object adaptIterableType(Field targetField, Object value) {
        Class<?> targetFieldType = targetField.getType();
        if(targetFieldType.equals(Set.class)){
            return new HashSet<>((Collection<Object>)value);
        }
        if(targetFieldType.equals(List.class)){
            return new ArrayList<>((Collection<Object>)value);
        }
        return value;
    }

    private Method getMethod(String methodName){
        Method method = publicMethods.get(methodName);
        if(method == null)
            throw new RuntimeException("Method by getName " + methodName + " not found");
        return method;
    }

    private Field getField(String fieldName){
        CachedEntityField field = fields.get(fieldName);
        if(field == null)
            throw new RuntimeException("Field by name of " + fieldName + " not found in " + this.clazz.getSimpleName());
        return field.getField();
    }

    public CachedEntityType(Class<?> clazz, Collection<Field> fields, Collection<Method> publicMethods) {
        this.clazz = clazz;
        this.fields = new HashMap<>();
        fields.forEach(field -> {
            boolean isCollectionOrMap =
                    Iterable.class.isAssignableFrom(field.getType()) ||
                            Map.class.isAssignableFrom(field.getType());
            boolean isNonUpdatable = isIEquatableIgnored(field);
            this.fields.put(field.getName(), new CachedEntityField(field, isCollectionOrMap, isNonUpdatable));
        });
        this.publicMethods = new HashMap<>();
        publicMethods.forEach(publicMethod -> this.publicMethods.put(publicMethod.getName(), publicMethod));
        this.defaultInstance = genDefaultInstance();
    }

    private Object genDefaultInstance(){
        Constructor[] cons = clazz.getDeclaredConstructors();
        try {
            for(Constructor constructor : cons){
                if(constructor.getParameterCount() == 0){
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                }
            }
            throw new RuntimeException("No default constructor found for " + clazz.getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<CachedEntityField> getNonUpdatableFields(){
        val _fields = new ArrayList<>(fields.values());
        _fields.removeIf(_field -> !_field.isNonUpdatable());
        return _fields;
    }

    private boolean isIEquatableIgnored(Field field){
        return field.isAnnotationPresent(IEquatableIgnore.class) || isInIEquatableIgnoresList(field);
    }

    private boolean isInIEquatableIgnoresList(Field field){
        IEquatableIgnores iEquatableIgnores = clazz.getAnnotation(IEquatableIgnores.class);
        if(iEquatableIgnores == null) return false;
        for (String value : iEquatableIgnores.value()) {
            if(value.equals(field.getName())) return true;
        }
        return false;
    }

}
