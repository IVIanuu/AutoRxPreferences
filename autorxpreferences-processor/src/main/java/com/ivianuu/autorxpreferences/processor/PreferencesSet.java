package com.ivianuu.autorxpreferences.processor;

import android.support.annotation.NonNull;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
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
class PreferencesSet {

    private static final String CONTEXT_TYPE = "android.content.Context";
    private static final String GSON_TYPE = "com.google.gson.Gson";
    private static final String PREFERENCE_MANAGER_TYPE = "android.preference.PreferenceManager";
    private static final String SHARED_PREFERENCES_TYPE = "android.content.SharedPreferences";
    private static final String RX_SHARED_PREFERENCES_TYPE = "com.f2prateek.rx.preferences2.RxSharedPreferences";
    private static final String RX_PREFERENCE_TYPE = "com.f2prateek.rx.preferences2.Preference";
    private static final String RX_CONVERTER_TYPE = "com.f2prateek.rx.preferences2.Preference.Converter";

    private TypeName targetTypeName;
    private ClassName preferenceClassName;
    private boolean expose;
    private String preferencesName;
    private List<Preference> preferences;

    private PreferencesSet(TypeName targetTypeName,
                           ClassName preferenceClassName,
                           boolean expose,
                           String preferencesName,
                           List<Preference> preferences) {
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

        // rx preferences field
        result.addField(createGsonField());
        result.addField(createRxPreferencesField());

        // constructor
        result.addMethod(createConstructor());

        // create methods
        result.addMethod(createContextOnlyCreateMethod());
        result.addMethod(createContextAndGsonCreateMethod());

        // prefs getter
        result.addMethod(createGetRxSharedPreferencesMethod());

        // add methods for preferences
        for (Preference preference : preferences) {
            if (isSharedPreferencesSupportedType(preference)) {
                // default preference methods
                result.addMethod(createPreferenceGetterMethod(preference));
                result.addMethod(createPreferenceGetterWithDefaultMethod(preference));
            } else if (preference.isEnum()) {
                // enum method
                result.addMethod(createEnumGetterMethod(preference));
                result.addMethod(createEnumGetterWithDefaultMethod(preference));
            } else {
                // custom object
                result.addType(createPreferenceConverter(preference));
                result.addMethod(createObjectGetterMethod(preference));
                result.addMethod(createObjectGetterWithDefaultMethod(preference));
                result.addMethod(createObjectGetterWithAdapterAndDefaultMethod(preference));
            }
        }

        return result.build();
    }

    private FieldSpec createRxPreferencesField() {
        return FieldSpec.builder(
                ClassName.bestGuess(RX_SHARED_PREFERENCES_TYPE), "rxSharedPreferences", Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private FieldSpec createGsonField() {
        return FieldSpec.builder(
                ClassName.bestGuess(GSON_TYPE), "gson", Modifier.PRIVATE, Modifier.FINAL)
                .build();
    }

    private MethodSpec createConstructor() {
        MethodSpec.Builder result = MethodSpec.constructorBuilder()
                .addParameter(ClassName.bestGuess(CONTEXT_TYPE), "context")
                .addParameter(ClassName.bestGuess(GSON_TYPE), "gson")
                .addModifiers(Modifier.PRIVATE);

        if (preferencesName.isEmpty()) {
            // use default
            result.addStatement(
                    SHARED_PREFERENCES_TYPE + " sharedPreferences = "
                            + PREFERENCE_MANAGER_TYPE + ".getDefaultSharedPreferences(context)");
        } else {
            // use the preference name
            result.addStatement(
                    SHARED_PREFERENCES_TYPE + " sharedPreferences = getSharedPreferences("
                            + preferencesName + ", Context.MODE_PRIVATE)");
        }

        result.addStatement("this.gson = gson");
        result.addStatement("this.rxSharedPreferences = RxSharedPreferences.create(sharedPreferences)");
        return result.build();
    }

    private MethodSpec createContextOnlyCreateMethod() {
        ParameterSpec contextParam = ParameterSpec.builder(ClassName.bestGuess(CONTEXT_TYPE), "context")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder("create").addModifiers(Modifier.STATIC)
                .addParameter(contextParam)
                .addStatement("return new " + preferenceClassName.simpleName() +"(context, new Gson())")
                .returns(preferenceClassName);

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        return result.build();
    }

    private MethodSpec createContextAndGsonCreateMethod() {
        ParameterSpec contextParam = ParameterSpec.builder(ClassName.bestGuess(CONTEXT_TYPE), "context")
                .addAnnotation(NonNull.class)
                .build();

        ParameterSpec gsonParam = ParameterSpec.builder(ClassName.bestGuess(GSON_TYPE), "gson")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder("create").addModifiers(Modifier.STATIC)
                .addParameter(contextParam)
                .addParameter(gsonParam)
                .addStatement("return new " + preferenceClassName.simpleName() +"(context, gson)")
                .returns(preferenceClassName);

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        return result.build();
    }

    private MethodSpec createGetRxSharedPreferencesMethod() {
        MethodSpec.Builder result = MethodSpec.methodBuilder("getRxSharedPreferences")
                .addAnnotation(NonNull.class)
                .addStatement("return rxSharedPreferences")
                .returns(ClassName.bestGuess(RX_SHARED_PREFERENCES_TYPE));
        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        return result.build();
    }

    private MethodSpec createPreferenceGetterMethod(Preference preference) {
        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        result.beginControlFlow("if (" + preference.getName() + " != null)")
                .addStatement("return " + preference.getName() + "(" + preference.getName() + ")")
                .nextControlFlow("else")
                .addStatement("return rxSharedPreferences."
                        + getGetterMethodPrefix(preference) + "(\"" + preference.getKeyName() + "\")")
                .endControlFlow();

        return result.build();
    }

    private MethodSpec createPreferenceGetterWithDefaultMethod(Preference preference) {
        ParameterSpec defaultValueParam = ParameterSpec.builder(preference.getTypeName(), "defaultValue")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .addParameter(defaultValueParam)
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        result.addStatement("return rxSharedPreferences." + getGetterMethodPrefix(preference)
                + "(\"" + preference.getKeyName() + "\", defaultValue)");

        return result.build();
    }

    private MethodSpec createEnumGetterMethod(Preference preference) {
        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .returns(preference.getTypeName())
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        String exceptionText = preference.getName() + " has no default value";

        result.beginControlFlow("if(" + preference.getName() + " == null)")
                .addStatement("throw new java.lang.IllegalStateException(\"" + exceptionText + "\")")
                .endControlFlow();

        result.addStatement(
                "return rxSharedPreferences.getEnum(\""
                        + preference.getKeyName() + "\", " + preference.getName() + ", " + preference.getTypeName() + ".class)");

        return result.build();
    }

    private MethodSpec createEnumGetterWithDefaultMethod(Preference preference) {
        ParameterSpec defaultValueParam = ParameterSpec.builder(preference.getTypeName(), "defaultValue")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .addParameter(defaultValueParam)
                .returns(preference.getTypeName())
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        result.addStatement(
                "return rxSharedPreferences.getEnum(\""
                        + preference.getKeyName() + "\", defaultValue, " + preference.getTypeName() + ".class)");

        return result.build();
    }

    private MethodSpec createObjectGetterMethod(Preference preference) {
        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        String exceptionText = preference.getName() + " has no default value";

        result.beginControlFlow("if(" + preference.getName() + " == null)")
                .addStatement("throw new java.lang.IllegalStateException(\"" + exceptionText + "\")")
                .endControlFlow();

        result.addStatement("return " + preference.getName() + "(" + preference.getName() + ", new "
                + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, preference.getName() + "Converter" + "(gson))"));

        return result.build();
    }

    private MethodSpec createObjectGetterWithDefaultMethod(Preference preference) {
        ParameterSpec defaultValueParam = ParameterSpec.builder(preference.getTypeName(), "defaultValue")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .addParameter(defaultValueParam)
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        result.addStatement("return " + preference.getName() + "(defaultValue, new "
                + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, preference.getName() + "Converter" + "(gson))"));

        return result.build();
    }

    private MethodSpec createObjectGetterWithAdapterAndDefaultMethod(Preference preference) {
        ParameterSpec defaultValueParam = ParameterSpec.builder(preference.getTypeName(), "defaultValue")
                .addAnnotation(NonNull.class)
                .build();

        ParameterSpec converterParam = ParameterSpec.builder(ParameterizedTypeName.get(
                ClassName.bestGuess(RX_CONVERTER_TYPE), preference.getTypeName()), "converter")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder result = MethodSpec.methodBuilder(preference.getName())
                .addAnnotation(NonNull.class)
                .addParameter(defaultValueParam)
                .addParameter(converterParam)
                .returns(ParameterizedTypeName.get(ClassName.bestGuess(RX_PREFERENCE_TYPE), preference.getTypeName()));

        if (expose) {
            result.addModifiers(Modifier.PUBLIC);
        }

        result.addStatement("return rxSharedPreferences.getObject(\"" + preference.getKeyName() + "\", defaultValue, converter)");

        return result.build();
    }

    private TypeSpec createPreferenceConverter(Preference preference) {
        String className = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, preference.getName() + "Converter");
        TypeName type;

        // get the raw type
        if (preference.getTypeName() instanceof ParameterizedTypeName) {
            type = ((ParameterizedTypeName) preference.getTypeName()).rawType;
        } else {
            type = preference.getTypeName();
        }

        TypeSpec.Builder result = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.bestGuess(RX_CONVERTER_TYPE), preference.getTypeName()))
                .addField(ClassName.bestGuess(GSON_TYPE), "gson", Modifier.PRIVATE, Modifier.FINAL);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.bestGuess(GSON_TYPE), "gson")
                .addStatement("this.gson = gson")
                .build();

        result.addMethod(constructor);

        MethodSpec deserializeMethod = MethodSpec.methodBuilder("deserialize")
                .addAnnotation(NonNull.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(String.class), "serialized")
                .addStatement("return gson.fromJson(serialized, " + type.toString() + ".class)")
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

    static class Builder {

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
            return new PreferencesSet(targetTypeName, preferenceClassName, expose, preferencesName, preferences);
        }
    }
}
