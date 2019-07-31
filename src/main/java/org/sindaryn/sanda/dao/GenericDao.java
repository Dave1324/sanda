package org.sindaryn.sanda.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GenericDao<TID , T /*extends PersistableEntity<T, TID>*/>
        extends JpaRepository<T, TID>, JpaSpecificationExecutor<T> {
}
