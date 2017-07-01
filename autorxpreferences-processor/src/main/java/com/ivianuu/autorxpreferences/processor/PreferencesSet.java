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

import android.support.annotation.NonNull;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.ivianuu.autorxpreferences.annotations.Preferences;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreElements.getPackage;

/**
 * @author Manuel Wrage (IVIanuu)
 */
final class PreferencesSet {

    private static final ClassName CONTEXT = ClassName.get("android.content", "Context");
    private static final ClassName GSON = ClassName.get("com.google.gson", "Gson");
    private static final ClassName ILLEGAL_STATE_EXCEPTION = ClassName.get("java.lang", "IllegalStateException");
    private static final ClassName PREFERENCE_MANAGER = ClassName.get("android.preference", "PreferenceManager");
    private static final ClassName SHARED_PREFERENCES = ClassName.get("android.content", "SharedPreferences");
    private static final ClassName RX_SHARED_PREFERENCES = ClassName.get("com.f2prateek.rx.preferences2", "RxSharedPreferences");
    private static final ClassName PREFERENCE = ClassName.get("com.f2prateek.rx.preferences2", "Preference");
    private static final ClassName CONVERTER = ClassName.get("com.f2prateek.rx.preferences2.Preference", "Converter");

    private TypeName targetTypeName;
    private ClassName preferenceClassName;
    private boolean expose;
    private String preferencesName;
    private ImmutableList<Preference> preferences;

    private List<ParameterizedTypeName> converters = new ArrayList<>();

    private PreferencesSet(TypeName targetTypeName,
                           ClassName preferenceClassName,
                           boolean expose,
                           String preferencesName,
                           ImmutableList<Preference> preferences) {

        this.targetTypeName = targetTypeName;
        this.preferenceClassName = preferenceClassName;
        this.expose = expose;
        this.preferencesName = preferencesName;
        this.preferences = preferences;
    }

    JavaFile brewJava() {
        return JavaFile.builder(preferenceClassName.packageName(), createType())
                .addFileComment("Generated code. Do not modify!")
                .build();
    }

    private TypeSpec createType() {
        TypeSpec.Builder result = TypeSpec.classBuilder(preferenceClassName.simpleName())
                .addModifiers(Modifier.FINAL)
                .superclass(targetTypeName);

        // add public
        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        // shared preferences field
        result.addField(createSharedPreferencesField());

        // rx preferences field
        result.addField(createRxPreferencesField());

        // Base constructor
        MethodSpec.Builder constructor = createBaseConstructor();

        // create methods
        result.addMethod(createContextOnlyCreateMethod());
        result.addMethod(createContextAndGsonCreateMethod());

        // clear method
        result.addMethod(createClearMethod());

        // add methods for preferences
        for (Preference preference : preferences) {
            if (isSharedPreferencesSupportedType(preference)) {
                // default preference methods
                result.addMethod(createPreferenceGetterMethod(preference));
            } else if (preference.isEnum()) {
                // enum method
                result.addMethod(createEnumGetterMethod(preference));
            } else {
                // custom object

                // if we have no converter for this type add it
                if (!converters.contains(getConverterType(preference))) {
                    result.addType(createObjectConverter(preference));
                    result.addField(createConverterField(preference));

                    constructor.addStatement(
                            "this.$L = new $L(gson)", getConverterFieldName(preference), getConverterTypeName(preference));

                    converters.add(getConverterType(preference));
                }

                result.addMethod(createObjectGetterMethod(preference));
            }
        }

        // add constructor
        result.addMethod(constructor.build());

        return result.build();
    }

    private FieldSpec createSharedPreferencesField() {
        return FieldSpec.builder(SHARED_PREFERENCES, "sharedPreferences", Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private FieldSpec createRxPreferencesField() {
        return FieldSpec.builder(RX_SHARED_PREFERENCES, "rxSharedPreferences", Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private MethodSpec.Builder createBaseConstructor() {
        MethodSpec.Builder result = MethodSpec.constructorBuilder()
                .addParameter(CONTEXT, "context")
                .addParameter(GSON, "gson")
                .addModifiers(Modifier.PRIVATE);

        if (preferencesName.isEmpty()) {
            // use default
            result.addStatement(
                    "this.sharedPreferences = $T.getDefaultSharedPreferences(context)", PREFERENCE_MANAGER);
        } else {
            // use the preference name
            result.addStatement(
                    "$T sharedPreferences = context.getSharedPreferences($S, Context.MODE_PRIVATE)", SHARED_PREFERENCES, preferencesName);
        }

        result.addStatement("this.rxSharedPreferences = RxSharedPreferences.create(sharedPreferences)");

        return result;
    }

    private MethodSpec createContextOnlyCreateMethod() {
        ParameterSpec contextParam = ParameterSpec.builder(CONTEXT, "context")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder("create").addModifiers(Modifier.STATIC)
                .addParameter(contextParam)
                .addStatement("return create(context, new $T())", GSON)
                .returns(preferenceClassName);

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        return result.build();
    }

    private MethodSpec createContextAndGsonCreateMethod() {
        ParameterSpec contextParam = ParameterSpec.builder(CONTEXT, "context")
                .addAnnotation(NonNull.class)
                .build();

        ParameterSpec gsonParam = ParameterSpec.builder(GSON, "gson")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder("create").addModifiers(Modifier.STATIC)
                .addParameter(contextParam)
                .addParameter(gsonParam)
                .addStatement("return new $T(context, gson)", preferenceClassName)
                .returns(preferenceClassName);

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        return result.build();
    }

    private MethodSpec createClearMethod() {
        MethodSpec.Builder result = MethodSpec.methodBuilder("clear")
                .addStatement("sharedPreferences.edit().clear().apply()");
        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        return result.build();
    }

    private MethodSpec createPreferenceGetterMethod(Preference preference) {
        String name = preference.getName();

        MethodSpec.Builder result = MethodSpec.methodBuilder(getGetterMethodName(preference))
                .addAnnotation(NonNull.class)
                .returns(getRxPreferenceType(preference));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        result.beginControlFlow("if ($L != null)", name)
                .addStatement("return rxSharedPreferences.$L($S, $L)", getGetterMethodPrefix(preference), preference.getKeyName(), name)
                .nextControlFlow("else")
                .addStatement("return rxSharedPreferences.$L($S)", getGetterMethodPrefix(preference), preference.getKeyName())
                .endControlFlow();

        return result.build();
    }

    private MethodSpec createEnumGetterMethod(Preference preference) {
        MethodSpec.Builder result = MethodSpec.methodBuilder(getGetterMethodName(preference))
                .addAnnotation(NonNull.class)
                .returns(preference.getTypeName())
                .returns(getRxPreferenceType(preference));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        String exceptionText = preference.getName() + " has no default value";

        result.beginControlFlow("if ($L == null)", preference.getName())
                .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, exceptionText)
                .endControlFlow();

        result.addStatement(
                "return rxSharedPreferences.getEnum($S, $L, $T.class)", preference.getKeyName(), preference.getName(), preference.getTypeName());

        return result.build();
    }

    private MethodSpec createObjectGetterMethod(Preference preference) {
        MethodSpec.Builder result = MethodSpec.methodBuilder(getGetterMethodName(preference))
                .addAnnotation(NonNull.class)
                .returns(getRxPreferenceType(preference));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        String name = preference.getName();
        String exceptionText = name + " has no default value";

        result.beginControlFlow("if($L == null)", name)
                .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, exceptionText)
                .endControlFlow();

        result.addStatement("return rxSharedPreferences.getObject($S, $L, $L)", preference.getKeyName(), preference.getName(), getConverterFieldName(preference));

        return result.build();
    }

    private TypeSpec createObjectConverter(Preference preference) {
        String className = getConverterTypeName(preference);
        TypeName type;

        // get the raw type
        if (preference.getTypeName() instanceof ParameterizedTypeName) {
            type = ((ParameterizedTypeName) preference.getTypeName()).rawType;
        } else {
            type = preference.getTypeName();
        }

        TypeSpec.Builder result = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(getConverterType(preference))
                .addField(GSON, "gson", Modifier.PRIVATE, Modifier.FINAL);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(GSON, "gson")
                .addStatement("this.gson = gson")
                .build();

        result.addMethod(constructor);

        MethodSpec deserializeMethod = MethodSpec.methodBuilder("deserialize")
                .addAnnotation(NonNull.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(String.class), "serialized")
                .addStatement("return gson.fromJson(serialized, $T.class)", type)
                .returns(preference.getTypeName())
                .build();

        result.addMethod(deserializeMethod);

        MethodSpec serializeMethod = MethodSpec.methodBuilder("serialize")
                .addAnnotation(NonNull.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(preference.getTypeName(), "value")
                .addStatement("return gson.toJson(value)")
                .returns(String.class)
                .build();

        result.addMethod(serializeMethod);

        return result.build();
    }

    private FieldSpec createConverterField(Preference preference) {
        ParameterizedTypeName converterType = getConverterType(preference);
        String converterName = getConverterFieldName(preference);
        return FieldSpec.builder(converterType, converterName, Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private ParameterizedTypeName getRxPreferenceType(Preference preference) {
        return ParameterizedTypeName.get(PREFERENCE, preference.getTypeName());
    }

    private ParameterizedTypeName getConverterType(Preference preference) {
        return ParameterizedTypeName.get(CONVERTER, preference.getTypeName());
    }

    private String getConverterTypeName(Preference preference) {
        return CaseFormat.LOWER_CAMEL.to(
                CaseFormat.UPPER_CAMEL, ClassName.bestGuess(preference.getTypeName().toString()).simpleName() + "Converter");
    }

    private String getConverterFieldName(Preference preference) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getConverterTypeName(preference));
    }

    private String getGetterMethodName(Preference preference) {
        String preferenceName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, preference.getName());
        return CaseFormat.UPPER_CAMEL.to(
                CaseFormat.LOWER_CAMEL, "Get" + preferenceName);
    }

    private String getGetterMethodPrefix(Preference preference) {
        TypeName typeName = preference.getTypeName();
        if (TypeName.get(Boolean.class).equals(typeName)) {
            return "getBoolean";
        } else if (TypeName.get(String.class).equals(typeName)) {
            return "getString";
        } else if (TypeName.get(Integer.class).equals(typeName)) {
            return "getInteger";
        } else if (TypeName.get(Float.class).equals(typeName)) {
            return "getFloat";
        } else if (TypeName.get(Long.class).equals(typeName)) {
            return "getLong";
        } else if (ParameterizedTypeName.get(Set.class, String.class).equals(typeName)) {
            return "getStringSet";
        } else {
            throw new IllegalArgumentException("unsupported type");
        }
    }

    private boolean isSharedPreferencesSupportedType(Preference preference) {
        TypeName typeName = preference.getTypeName();
        return TypeName.get(Boolean.class).equals(typeName)
                || TypeName.get(String.class).equals(typeName)
                || TypeName.get(Integer.class).equals(typeName)
                || TypeName.get(Float.class).equals(typeName)
                || TypeName.get(Long.class).equals(typeName)
                || ParameterizedTypeName.get(Set.class, String.class).equals(typeName);
    }

    static Builder newBuilder(TypeElement enclosingElement) {
        Preferences preferencesAnnotation = enclosingElement.getAnnotation(Preferences.class);
        TypeMirror typeMirror = enclosingElement.asType();

        TypeName targetType = TypeName.get(typeMirror);
        if (targetType instanceof ParameterizedTypeName) {
            targetType = ((ParameterizedTypeName) targetType).rawType;
        }

        String packageName = getPackage(enclosingElement).getQualifiedName().toString();
        String className = enclosingElement.getQualifiedName().toString().substring(
                packageName.length() + 1).replace('.', '$');
        ClassName bindingClassName = ClassName.get(packageName, className + preferencesAnnotation.classNameSuffix());

        return new Builder(targetType, bindingClassName, preferencesAnnotation.expose(), preferencesAnnotation.preferenceName());
    }

    static final class Builder {

        private TypeName targetTypeName;
        private ClassName preferenceClassName;
        private boolean expose;

        private String preferencesName;

        private List<Preference> preferences = new ArrayList<>();

        private Builder(TypeName targetTypeName,
                        ClassName preferenceClassName,
                        boolean expose,
                        String preferencesName) {
            this.targetTypeName = targetTypeName;
            this.preferenceClassName = preferenceClassName;
            this.expose = expose;
            this.preferencesName = preferencesName;
        }

        Builder addPreference(Preference preference) {
            preferences.add(preference);
            return this;
        }

        PreferencesSet build() {
            return new PreferencesSet(
                    targetTypeName, preferenceClassName, expose, preferencesName, ImmutableList.copyOf(preferences));
        }
    }
}
