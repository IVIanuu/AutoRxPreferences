package com.ivianuu.autorxpreferences.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Creates a access method for the key
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Key {
    /**
     * name for the preference field.
     * if empty, use lower-cased variable name as its preference field name
     *
     * @return preference field name
     */
    String name() default "";
}