package org.sindaryn.sanda.persistence;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.sindaryn.sanda.annotations.IEquatableIgnore;
import org.sindaryn.sanda.mutations.IEquatable;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
@NoArgsConstructor
public class PersistableEntity<T, TID >
        implements IEquatable<T>, Serializable {
    @EmbeddedId
    @NonNull
    @Getter(onMethod_ = {@GraphQLIgnore})
    @Column(name = "id", unique=true, nullable=false, updatable=false)
    private TID id;
    @GraphQLQuery(name = "id")
    public String secureId(){
        return id.toString();
    }
    @Getter(onMethod_ = {@GraphQLIgnore})
    @IEquatableIgnore
    private Boolean isFirstPersist = true;
    @Getter(onMethod_ = {@GraphQLIgnore})
    @IEquatableIgnore
    private Boolean isArchived = false;
    @Getter(onMethod_ = {@GraphQLIgnore})
    @IEquatableIgnore
    @Version
    private Long version = 0L;
    @IEquatableIgnore
    private final LocalDateTime createdAt = LocalDateTime.now();
    @PrePersist
    public void init(){
        if(isFirstPersist){
            initId();
            customFirstTimeInit();
            isFirstPersist = false;
        }
    }
    @Override
    public final boolean equals(final Object o) {
        return isEqualTo(o);
    }
    protected void customFirstTimeInit(){}
    public void initId() {
        throw new RuntimeException("void initId() in PersistentEntity<TID, T> must be overridden by child class");
    }
}
