package com.havronius.gwt.reflection.bean.client;

import com.google.gwt.core.client.GWT;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BeanClass<T> implements Iterable<BeanProperty<T,?>> {

    private static BeanClasses classes = GWT.create(BeanClasses.class);

    public static BeanClass forName(String string) {
        return classes.forName(string);
    }

    @SuppressWarnings("unchecked")
    public static <T> BeanClass<T> get(java.lang.Class clazz) {
        return forName(clazz.getName());
    }

    private String name;

    private Map<String, BeanProperty<T,?>> properties = new LinkedHashMap<>(32);

    public BeanClass(String name) {
        this.name = name;
        fillProperties();
    }

    protected void addProperty(BeanProperty<T,?> property) {
        properties.put(property.getName(), property);
    }

    public String getName() {
        return name;
    }

    public BeanProperty<T,?> getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Iterator<BeanProperty<T,?>> iterator() {
        return properties.values().iterator();
    }

    abstract protected void fillProperties();
}
