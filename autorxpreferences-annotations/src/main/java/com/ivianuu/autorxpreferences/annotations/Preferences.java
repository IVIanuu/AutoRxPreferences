package com.ivianuu.autorxpreferences.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Creates a auto preference class
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Preferences {
    /**
     * preferences name.
     * if empty, default SharedPreferences is picked.
     */
    String preferenceName() default "";

    /**
     * The suffix of the created class for example
     * If the annotated classes name is Preferences and the suffix is '_'
     * The generated classes name will be Preferences_
     */
    String classNameSuffix() default "_";
}