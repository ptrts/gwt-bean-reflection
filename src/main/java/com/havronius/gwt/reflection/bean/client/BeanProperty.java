package com.havronius.gwt.reflection.bean.client;

public abstract class BeanProperty<T, V> {

    protected String name;

    protected Class<V> type;

    public BeanProperty(String name, Class<V> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<V> getType() {
        return type;
    }

    abstract public V getValue(T instance);

    abstract public void setValue(T instance, V value);
}
