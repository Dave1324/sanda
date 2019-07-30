package org.sindaryn.sanda.reflection;

import lombok.Getter;
import lombok.val;
import org.reflections.Reflections;
import org.springframework.aop.framework.Advised;
import org.springframework.stereotype.Component;
import org.sindaryn.sanda.mutations.IEquatable;
import org.sindaryn.sanda.persistence.PersistableEntity;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.*;

@Component
public class ReflectionService implements IReflectionTools {

    private Reflections reflectionsHelper;
    @Getter
    private Map<String, CachedEntityType> cachedEntityTypes;

    public Collection<Field> getFieldsOf(Class<?> classTypeToken) {
        return getFieldsOf(classTypeToken, new ArrayList<>());
    }

    public Collection<Field> getFieldsOf(
            Class<?> classTypeToken,
            Collection<String> excludedFields) {
        Collection<CachedEntityField> cachedEntityFields = cachedEntityTypes.get(classTypeToken.getSimpleName()).getFields().values();
        List<Field> fields = new ArrayList<>();
        cachedEntityFields.forEach(cachedEntityField -> fields.add(cachedEntityField.getField()));
        fields.removeIf(field -> isExcludedField(field, excludedFields));
        return fields;
    }

    public Collection<String> getFieldNamesOf(Class<?> classTypeToken,
                                              Collection<String> excludedFields) {
        Collection<String> fieldNames = cachedEntityTypes.get(classTypeToken.getSimpleName()).getFields().keySet();
        for (String fieldName : fieldNames) {
            if (excludedFields.contains(fieldName))
                fieldNames.remove(fieldName);
        }
        return fieldNames;
    }

    private boolean isExcludedField(Field field, Collection<String> excludedFields) {
        for (String excludedField : excludedFields) {
            if (excludedField.equals(field.getName()))
                return true;
        }
        return false;
    }

    public Collection<Class<?>> getImplementationTypesOf(Class<?> superClass) {
        return new ArrayList<>(reflectionsHelper.getSubTypesOf(superClass));
    }

    @PostConstruct
    private void init() {
        reflectionsHelper = new Reflections("com.loanmower");
        cachedEntityTypes = new HashMap<>();
        Collection<Class<? extends PersistableEntity>> dataModelEntityTypes =
                reflectionsHelper.getSubTypesOf(PersistableEntity.class);
        for (Class<?> currentType : dataModelEntityTypes) {
            if (currentType.isAnnotationPresent(Entity.class) || currentType.isAnnotationPresent(Table.class))
                cachedEntityTypes.put(
                        currentType.getSimpleName(),
                        new CachedEntityType(
                                currentType,
                                IReflectionTools.getClassFields(currentType),
                                IReflectionTools.getPublicMethodsOf(currentType)));
        }
        /*reflectionService = this;*/
    }

    public Class<?> getEntityType(String name) {
        val type = cachedEntityTypes.get(name).getClazz();
        val primitiveType = IReflectionTools.primitiveTypeOrEnum(type);
        return primitiveType != null ? primitiveType : cachedEntityTypes.get(name).getClazz();
    }

    public Object getEmbeddedEntity(Object hostEntity, String targetName) {
        Collection<Field> hostEntityFields = getFieldsOf(hostEntity.getClass());
        for (Field field : hostEntityFields) {
            if (field.getName().equals(targetName)) {
                field.setAccessible(true);
                try {
                    return field.get(hostEntity);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Cannot find field " + targetName + " within " + hostEntity.getClass().getSimpleName());
    }

    public Object invokeMethodOf(Object targetEntity, String targetMethodName, Object[] args) {
        return getEntityType(targetEntity).invokePublicMethod(targetMethodName, targetEntity, args);
    }

    public Object invokeMethodOf(Object targetEntity, String targetMethodName) {
        return invokeMethodOf(targetEntity, targetMethodName, null);
    }

    private CachedEntityType getEntityType(Object targetEntityType) {
        return cachedEntityTypes.get(targetEntityType.getClass().getSimpleName());
    }

    public String nameOf(final Object target) {
        return typeOf(target).getCanonicalName();
    }

    private boolean targetClassIsProxied(final Object target) {

        return target.getClass().getCanonicalName().contains("$Proxy");
    }

    public Class<?> typeOf(Object target) {

        if (targetClassIsProxied(target)) {
            Advised advised = (Advised) target;

            try {

                val result = advised.getTargetSource().getTarget().getClass();
                val entityType = advised.getTargetSource().getTarget();
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        val result = target.getClass();
        return result;
    }

    public Collection<String> getNonUpdatableFields(Class<? extends IEquatable> clazz) {
        val entityType = cachedEntityTypes.get(clazz.getSimpleName());
        Collection<String> nonUpdatableFieldNames = new ArrayList<>();
        entityType.getNonUpdatableFields().forEach(field -> nonUpdatableFieldNames.add(field.getField().getName()));
        return nonUpdatableFieldNames;
    }
}
