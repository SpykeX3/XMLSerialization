package ru.nsu.fit.XMLSerialization.Serializator;

import java.util.HashMap;
import java.util.Map;

public class Counting {
    private static Map<Class<?>, Integer> counters = new HashMap<>();

    public static void addOne(Class<?> clazz) {
        counters.putIfAbsent(clazz, 0);
        counters.compute(clazz, (k, v) -> v + 1);
    }

    public static int getCount(Class<?> clazz) {
        return counters.getOrDefault(clazz, 0);
    }
}
