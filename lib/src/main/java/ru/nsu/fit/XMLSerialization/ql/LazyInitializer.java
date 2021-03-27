package ru.nsu.fit.XMLSerialization.ql;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.InvocationTargetException;

public class LazyInitializer {
    private LazyInitializer(Class<?> objectClass, ValueLoader loader) {
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> objectClass, ValueLoader loader) {
        MethodHandler handler = (self, overridden, forwarder, args) -> {
            // check that fields are loaded
            return forwarder.invoke(self, args);
        };

        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(objectClass);
        try {
            return (T) factory.create(new Class[]{}, new Object[]{}, handler);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
