/*
 * Copyright 2017 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    /**
     * whether to make generated classes public or not
     *
     * true if generated class should be public. If false, the class will be package private.
     */
    boolean expose() default true;
}