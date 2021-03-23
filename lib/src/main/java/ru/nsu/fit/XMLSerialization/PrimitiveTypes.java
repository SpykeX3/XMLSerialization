package ru.nsu.fit.XMLSerialization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class PrimitiveTypes {
    private static final Set<String> primitives =
            new HashSet<>(
                    Arrays.asList(
                            "byte",
                            "short",
                            "int",
                            "long",
                            "float",
                            "double",
                            "boolean",
                            "char",
                            "class java.lang.String"));

    private static final Set<String> wrappers =
            new HashSet<>(
                    Arrays.asList(
                            "class java.lang.Byte",
                            "class java.lang.Short",
                            "class java.lang.Integer",
                            "class java.lang.Long",
                            "class java.lang.Float",
                            "class java.lang.Double",
                            "class java.lang.Boolean",
                            "class java.lang.Character"));


    static boolean isPrimitive(String type){
        return primitives.contains(type);
    }

    static boolean isWrapper(String type){
        return primitives.contains(type);
    }
}
