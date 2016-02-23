package com.havronius.gwt.reflection.bean.client;

import java.util.HashMap;
import java.util.Map;

abstract class BeanClasses {

    private Map<String, BeanClass> map = new HashMap<>(255);

    public BeanClass forName(String name) {
        return map.get(name);
    }

    public BeanClasses() {
        fill();
    }

    /**
     * Добавление класса. Для использования в имплементациях {@link this#fill()}
     * @param clazz класс, который надо добавить
     */
    protected void addClass(BeanClass clazz) {
        map.put(clazz.getName(), clazz);
    }

    abstract protected void fill();
}
