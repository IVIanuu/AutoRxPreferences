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
