package org.sindaryn.sanda.reflection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

@lombok.Getter
@lombok.Setter
@RequiredArgsConstructor
public class CachedEntityField {
    @NonNull

    private Field field;
    @NonNull
    private boolean isCollectionOrMap;
    @NonNull
    private boolean isNonUpdatable;
}
