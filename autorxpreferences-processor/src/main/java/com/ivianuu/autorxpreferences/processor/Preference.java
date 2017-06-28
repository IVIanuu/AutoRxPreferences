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

package com.ivianuu.autorxpreferences.processor;

import com.google.common.base.CaseFormat;
import com.ivianuu.autorxpreferences.annotations.Key;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.VariableElement;

/**
 * @author Manuel Wrage (IVIanuu)
 */
class Preference {
    
    private String fieldName;
    private TypeName typeName;
    private String keyName;
    private boolean isEnum;

    private Preference(String fieldName,
                       TypeName typeName,
                       String keyName,
                       boolean isEnum) {
        this.fieldName = fieldName;
        this.typeName = typeName;
        this.keyName = keyName;
        this.isEnum = isEnum;
    }
    
    String getName() {
        return fieldName;
    }

    TypeName getTypeName() {
        return typeName;
    }

    String getKeyName() {
        return keyName;
    }

    boolean isEnum() {
        return isEnum;
    }

    static Preference create(VariableElement annotatedElement, boolean isEnum) {
        String fieldName = annotatedElement.getSimpleName().toString();
        TypeName typeName = TypeName.get(annotatedElement.asType());

        Key keyAnnotation = annotatedElement.getAnnotation(Key.class);
        String keyName = keyAnnotation.name();
        if (keyName.isEmpty()) {
            // set default key
            keyName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
        }

        return new Preference(fieldName, typeName, keyName, isEnum);
    }
}
