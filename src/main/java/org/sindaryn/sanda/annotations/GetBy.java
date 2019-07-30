package org.sindaryn.sanda.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Target({FIELD, TYPE})
@Retention(SOURCE)
public @interface GetBy {
}
