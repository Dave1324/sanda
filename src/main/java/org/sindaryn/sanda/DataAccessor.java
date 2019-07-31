package org.sindaryn.sanda;

import lombok.val;
import org.sindaryn.sanda.reflection.ReflectionCache;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.sindaryn.sanda.StaticUtils.toPascalCase;

@Component
@SuppressWarnings("unchecked")
public class DataAccessor<T> {

    @Autowired
    private ReflectionCache reflectionCache;
    @Autowired
    private List<? extends GenericDao> daos;
    private Map<String, GenericDao> daoMap;
    @PostConstruct
    private void init(){
        daoMap = new HashMap<>();
        daos.forEach(dao -> {
            String entityName = extractEntityName(dao);
            if(entityName != null)
                daoMap.put(entityName, dao);
        });
    }
    private String extractEntityName(GenericDao dao) {
        val interfaces = ((Advised)dao).getProxiedInterfaces();
        String daoName = "";
        for(Class<?> interface_ : interfaces){
            if(interface_.getSimpleName().contains("Dao")){
                daoName = interface_.getSimpleName();
                break;
            }
        }
        int endIndex = daoName.indexOf("Dao");
        return endIndex != -1 ? daoName.substring(0, endIndex) : null;
    }

    
    public List<T> findAll(Class<T> clazz) {
        return daoMap.get(clazz.getSimpleName()).findAll();
    }

    public List<T> findAll(Class<T> clazz, Sort sort) {
        return daoMap.get(clazz.getSimpleName()).findAll(sort);
    }

    
    public Page<T> findAll(Class<T> clazz, Pageable pageable) {
        return daoMap.get(clazz.getSimpleName()).findAll(pageable);
    }

    
    public List<T> findAllById(Class<T> clazz, Iterable<Object> iterable) {
        return daoMap.get(clazz.getSimpleName()).findAllById(iterable);
    }

    
    public long count(Class<T> clazz) {
        return daoMap.get(clazz.getSimpleName()).count();
    }

    
    public void deleteById(Class<T> clazz, Object id) {
        daoMap.get(clazz.getSimpleName()).deleteById(id);
    }

    
    public void delete(T t) {
        daoMap.get(t.getClass().getSimpleName()).delete(t);
    }

    
    public void deleteAll(Iterable<? extends T> iterable) {
        long size = StreamSupport.stream(iterable.spliterator(), false).count();
        if(size <= 0) return;
        String clazzName = iterable.iterator().next().getClass().getSimpleName();
        daoMap.get(clazzName).deleteAll(iterable);
    }

    
    public void deleteAll(Class<T> clazz) {
        daoMap.get(clazz.getSimpleName()).deleteAll();
    }

    
    public <S extends T> S save(S s) {
        return (S) daoMap.get(s.getClass().getSimpleName()).save(s);
    }

    
    public <S extends T> List<S> saveAll(Iterable<S> iterable) {
        long size = StreamSupport.stream(iterable.spliterator(), false).count();
        if(size <= 0) return new ArrayList<>();
        String clazzName = iterable.iterator().next().getClass().getSimpleName();
        return daoMap.get(clazzName).saveAll(iterable);
    }

    public Optional<T> findById(Class<T> clazz, Object id) {
        return daoMap.get(clazz.getSimpleName()).findById(id);
    }
    
    public boolean existsById(Class<T> clazz, Object id) {
        return daoMap.get(clazz.getSimpleName()).existsById(id);
    }
    
    public void flush(Class<T> clazz) {
        daoMap.get(clazz.getSimpleName()).flush();
    }

    
    public <S extends T> S saveAndFlush(S s) {
        String clazzName = s.getClass().getSimpleName();
        return (S) daoMap.get(clazzName).saveAndFlush(s);
    }

    public void deleteInBatch(Iterable<T> iterable) {
        long size = StreamSupport.stream(iterable.spliterator(), false).count();
        if(size <= 0) return;
        String clazzName = iterable.iterator().next().getClass().getSimpleName();
        daoMap.get(clazzName).deleteInBatch(iterable);
    }

    
    public void deleteAllInBatch(Class<?> clazz) {
        daoMap.get(clazz.getSimpleName()).deleteAllInBatch();
    }

    
    public T getOne(Class<T> clazz, Object id) {
        return (T) daoMap.get(clazz.getSimpleName()).getOne(id);
    }

    
    public <S extends T> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    
    public <S extends T> List<S> findAll(Example<S> example) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).findAll(example);
    }

    
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).findAll(example, sort);
    }

    
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).findAll(example, pageable);
    }

    
    public <S extends T> long count(Example<S> example) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).count(example);
    }

    
    public <S extends T> boolean exists(Example<S> example) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).exists(example);
    }

    public T getBy(Class<T> clazz, String attributeName, String attributeValue){
        try{
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Class<?>[] params = new Class<?>[]{String.class};
            Method methodToInvoke = dao.getClass().getMethod("findBy" + toPascalCase(attributeName), params);
            return (T) methodToInvoke.invoke(dao, new Object[]{attributeValue});
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public List<T> getAllBy(Class<T> clazz, String attributeName, String attributeValue){
        try{
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Class<?>[] params = new Class<?>[]{String.class};
            Method methodToInvoke = dao.getClass().getMethod("findAllBy" + toPascalCase(attributeName), params);
            return (List<T>) methodToInvoke.invoke(dao, new Object[]{attributeValue});
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
