package org.sindaryn.sanda.reflection;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.val;

import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface IReflectionTools {
    static Class<?> primitiveTypeOrEnum(Class<?> clazz) {
        switch (clazz.getSimpleName()){
            case "String": return String.class;
            case "Double": return Double.class;
            case "Float" : return Float.class;
            case "Long"  : return Long.class;
            case "Integer": return Integer.class;
            case "Short" : return Short.class;
            case "Character" : return Character.class;
            case "Byte" : return Byte.class;
            case "Boolean" : return Boolean.class;
            case "LocalDateTime" : return LocalDateTime.class;
            case "LocalDate" : return LocalDate.class;
            case "URL" : return URL.class;
            case "BigDecimal" : return BigDecimal.class;
        }
        if(clazz.isEnum())
            return Enum.class;
        if(URL.class.equals(clazz)) return URL.class;
        return null;
    }
    //reflectively fetch all fields of given class, including inherited fields.

    default Iterable<Field> getClassFields(){
        return getClassFields(this.getClass());
    }

    static Collection<Field> getClassFields(@NonNull Class<?> startClass) {
        List<Field> currentClassFields = Lists.newArrayList(startClass.getDeclaredFields());
        Class<?> parentClass = startClass.getSuperclass();
        if (parentClass != null) {
            List<Field> parentClassFields =
                    (List<Field>) getClassFields(parentClass);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }

    static Collection<Method> getPublicMethodsOf(@NonNull Class<?> startClass) {
        List<Method> currentClassMethods = Lists.newArrayList(startClass.getMethods());
        Class<?> parentClass = startClass.getSuperclass();
        if (parentClass != null) {
            List<Method> parentClassFields =
                    (List<Method>) getPublicMethodsOf(parentClass);
            currentClassMethods.addAll(parentClassFields);
        }
        return currentClassMethods;
    }

    default Object getCurrentField(Field field){
        try {
            field.setAccessible(true);
            return field.get(this);
        }catch (Exception e){
            return null;
        }
    }

    static Collection<Object> getDefaultInstanceOfType(Class<?> classType){

        Collection<Object> instances = new ArrayList<>();
        Constructor[] cons = Class.class.getDeclaredConstructors();
        try {
            cons[0].setAccessible(true);
            instances.add(cons[0].newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return instances;
    }
    static Class<?> getIdType(Class<?> clazz){
        val fields = getClassFields(clazz);
        for (Field field : fields){
            if(field.isAnnotationPresent(EmbeddedId.class)){
                return field.getType();
            }
        }
        throw new RuntimeException("No @Id field found for type " + clazz.getSimpleName());
    }
    static boolean isEmbeddedEntity(Field field, ReflectionCache reflectionCache){
        if(!field.getDeclaringClass().isAnnotationPresent(Entity.class)) return false;
        Class<?> type = field.getType();
        if (primitiveTypeOrEnum(type) != null
                || isCollectionOrMapOfPrimitives(field.getName(), field.getDeclaringClass().getSimpleName(), reflectionCache))
            return false;
        else
            return !isId(field);
    }

    static boolean isId(Field type){
        return type.getAnnotation(EmbeddedId.class) != null || type.getAnnotation(Id.class) != null;
    }

    static boolean isCollectionOrMapOfPrimitives(String fieldName, String declaringClassName, ReflectionCache reflectionCache) {
        fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        val temp =
                reflectionCache
                        .getCachedEntityTypes()
                        .get(declaringClassName);
        try {
            CachedEntityField field =
                    reflectionCache
                            .getCachedEntityTypes()
                            .get(declaringClassName)
                            .getFields()
                            .get(fieldName);
            boolean isCollectionOrMap = field.isCollectionOrMap();
            if(!isCollectionOrMap) return false;
            return field.getField().isAnnotationPresent(ElementCollection.class);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    static Object genDefaultInstance(Class<?> clazz){
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
}
