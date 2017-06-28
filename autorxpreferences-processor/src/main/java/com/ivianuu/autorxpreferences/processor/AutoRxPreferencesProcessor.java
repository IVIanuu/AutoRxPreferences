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

import com.google.auto.service.AutoService;
import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class AutoRxPreferencesProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Key.class.getCanonicalName());
        types.add(Preferences.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Map<TypeElement, PreferencesSet> preferencesMap = findAndParseTargets(roundEnvironment);

        for (Map.Entry<TypeElement, PreferencesSet> entry : preferencesMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            PreferencesSet preferencesSet = entry.getValue();

            JavaFile javaFile = preferencesSet.brewJava();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Unable to write file for type %s: %s", typeElement, e.getMessage());
            }
        }

        return false;
    }

    private Map<TypeElement, PreferencesSet> findAndParseTargets(RoundEnvironment roundEnvironment) {
        Map<TypeElement, PreferencesSet> preferencesSetMap = new HashMap<>();

        for (Element element : roundEnvironment.getElementsAnnotatedWith(Preferences.class)) {
            TypeElement typeElement = (TypeElement) element;
            if (typeElement.getModifiers().contains(Modifier.FINAL)) {
                error(typeElement, "%s cannot be final", typeElement.getSimpleName().toString());
            }

            PreferencesSet.Builder preferenceSetBuilder = PreferencesSet.newBuilder(typeElement);

            // loop trough all preferences
            for (Element field : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
                // not annotated
                if (field.getAnnotation(Key.class) == null) continue;

                VariableElement variableElement = (VariableElement) field;

                // no private fields
                if (variableElement.getModifiers().contains(Modifier.PRIVATE)) {
                    error(variableElement, "%s cannot be private", variableElement.getSimpleName().toString());
                }

                // no primitives
                if (TypeName.get(field.asType()).isPrimitive()) {
                    error(variableElement,
                            "primitive types are not allowed. %s is a primitive type",
                            variableElement.getSimpleName().toString());
                }

                // add and create preference
                Preference preference = Preference.create(variableElement, isEnum(variableElement));

                preferenceSetBuilder.addPreference(preference);
            }

            preferencesSetMap.put(typeElement, preferenceSetBuilder.build());
        }

        return preferencesSetMap;
    }

    private boolean isEnum(Element element) {
        TypeElement typeElement = (TypeElement) typeUtils.asElement(element.asType());
        TypeMirror type = typeElement.getSuperclass();
        if (type.getKind() == TypeKind.NONE) {
            return false;
        }
        typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement != null && typeElement.toString().equals(Enum.class.getCanonicalName());
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }

}
