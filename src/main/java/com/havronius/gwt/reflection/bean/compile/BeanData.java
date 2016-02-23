package com.havronius.gwt.reflection.bean.compile;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeanData implements Iterable<BeanData.PropertyData> {

    public class PropertyData {

        String name;

        JType type;

        JMethod getter;

        JMethod setter;
    }

    JClassType type;

    Map<String, PropertyData> properties;

    public BeanData(JClassType type) {

        this.type = type;

        this.properties = new LinkedHashMap<>();

        final Pattern SETTER_PATTERN = Pattern.compile("^set(\\p{Upper})(\\w*)$");
        final Pattern GETTER_PATTERN = Pattern.compile("^get(\\p{Upper})(\\w*)$");
        final Pattern BOOLEAN_GETTER_PATTERN = Pattern.compile("^(?:get|is)(\\p{Upper})(\\w*)$");

        final String BOOLEAN_CLASS_NAME = Boolean.class.getName();
        final String BOOLEAN_PRIMITIVE_NAME = boolean.class.getName();

        JMethod[] methods = type.getMethods();

        // Ищем геттеры
        for (JMethod method : methods) {

            JType returnType = method.getReturnType();
            JType[] parameterTypes = method.getParameterTypes();

            if (returnType != JPrimitiveType.VOID && parameterTypes.length == 0) {

                Pattern pattern;
                String returnTypeName = returnType.getQualifiedSourceName();
                if (returnTypeName.equals(BOOLEAN_PRIMITIVE_NAME) || returnTypeName.equals(BOOLEAN_CLASS_NAME)) {
                    pattern = BOOLEAN_GETTER_PATTERN;
                } else {
                    pattern = GETTER_PATTERN;
                }

                String propertyName = getPropertyName(method, pattern);

                if (propertyName != null) {
                    JType propertyType = returnType;
                    tryAddGetter(propertyName, propertyType, method);
                }
            }
        }

        // Ищем сеттеры
        for (JMethod method : methods) {

            JType returnType = method.getReturnType();
            JType[] parameterTypes = method.getParameterTypes();

            if (returnType == JPrimitiveType.VOID && parameterTypes.length == 1) {
                String propertyName = getPropertyName(method, SETTER_PATTERN);
                if (propertyName != null) {
                    JType propertyType = parameterTypes[0];
                    tryAddSetter(propertyName, propertyType, method);
                }
            }
        }
    }

    private String getPropertyName(JMethod method, Pattern pattern) {

        String getName = method.getName();

        Matcher matcher = pattern.matcher(getName);

        if (matcher.find()) {
            String firstLetter = matcher.group(1);
            String rest = matcher.group(2);
            return firstLetter.toLowerCase() + rest;
        }

        return null;
    }

    private void tryAddGetter(String propertyName, JType propertyType, JMethod method) {

        if (!properties.containsKey(propertyName)) {

            PropertyData propertyData = new PropertyData();
            propertyData.name = propertyName;
            propertyData.type = propertyType;
            propertyData.getter = method;

            properties.put(propertyName, propertyData);
        }
    }

    private void tryAddSetter(String propertyName, JType propertyType, JMethod method) {

        PropertyData propertyData = properties.get(propertyName);

        if (propertyData == null) {

            propertyData = new PropertyData();
            propertyData.name = propertyName;
            propertyData.type = propertyType;
            propertyData.setter = method;

            properties.put(propertyName, propertyData);

        } else if (propertyData.type.equals(propertyType)) {
            propertyData.setter = method;
        }
    }

    @Override
    public Iterator<PropertyData> iterator() {
        return properties.values().iterator();
    }
}
