package ru.nsu.fit.XMLSerialization;

import org.w3c.dom.Node;

import java.io.InvalidClassException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ArrayFiller {
    private XMLDeserializer parent;
    private Map<String, ArrayFillingFunction> fillers = Map.of(
            "I", (arr, pos, token) -> Array.set(arr, pos, Integer.valueOf(token)),
            "S", (arr, pos, token) -> Array.set(arr, pos, Short.valueOf(token)),
            "J", (arr, pos, token) -> Array.set(arr, pos, Long.valueOf(token)),
            "F", (arr, pos, token) -> Array.set(arr, pos, Float.valueOf(token)),
            "D", (arr, pos, token) -> Array.set(arr, pos, Double.valueOf(token)),
            "B", (arr, pos, token) -> Array.set(arr, pos, Byte.valueOf(token)),
            "Z", (arr, pos, token) -> Array.set(arr, pos, Boolean.valueOf(token)),
            "C", (arr, pos, token) -> Array.set(arr, pos, token.charAt(0))
    );

    public ArrayFiller(XMLDeserializer parent) {
        this.parent = parent;
    }

    void fillArray(int id, Node bean) throws InvalidClassException, ClassNotFoundException {
        String content = bean.getTextContent();
        content = content.substring(1, content.length() - 1);
        Object array = parent.deserializedObjects.get(id);
        String[] tokens = content.split(", ");
        String type = parent.getBeanTypeName(id);
        if (bean.getAttributes().getNamedItem("length").getTextContent().equals("0"))
            return;
        if (PrimitiveTypes.isObjectArray(type)) {
            fillObjectArray(array, tokens);
        } else {
            type = type.replaceFirst("class \\[", "");
            fillPrimitiveArray(array, tokens, type);
        }
    }

    private void fillObjectArray(Object array, String[] tokens) throws InvalidClassException, ClassNotFoundException {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("null")) continue;
            int valueId = Integer.parseInt(tokens[i]);
            if (!parent.deserializedObjects.containsKey(valueId)) {
                parent.instantiateObject(parent.objectPool.get(valueId));
            }
            Array.set(array, i, parent.deserializedObjects.get(valueId));
        }
    }

    private void fillPrimitiveArray(Object array, String[] tokens, String type) throws InvalidClassException, ClassNotFoundException {
        ArrayFillingFunction filler = fillers.get(type);
        for (int i = 0; i < tokens.length; i++) {
            filler.setArrayCell(array, i, tokens[i]);
        }
    }

    void deserializeArrayChildren(int id, Node bean) throws InvalidClassException, ClassNotFoundException {
        String content = bean.getTextContent();
        if (bean.getAttributes().getNamedItem("length").getTextContent().equals("0"))
            return;
        String[] tokens = content.substring(1, content.length() - 1).split(", ");
        Object array = parent.deserializedObjects.get(id);
        String type = parent.getBeanTypeName(id);
        if (PrimitiveTypes.isArrayOfArrays(type)) {
            for (String token : tokens) {
                int valueId = Integer.parseInt(token);
                deserializeArrayChildren(valueId, parent.objectPool.get(valueId));
            }
            return;
        }
        for (String token : tokens) {
            if (token.equals("null")) {
                continue;
            }
            int valueId = Integer.parseInt(token);
            parent.smartDeserialize(valueId);
        }
    }

    List<Integer> getObjectArrayContent(int id) {
        Node bean = parent.objectPool.get(id);
        String content = bean.getTextContent();
        if (bean.getAttributes().getNamedItem("length").getTextContent().equals("0"))
            return Collections.emptyList();
        String[] tokens = content.substring(1, content.length() - 1).split(", ");
        return Arrays.stream(tokens).filter(t -> !t.equals("null")).map(Integer::parseInt).collect(Collectors.toList());
    }
}
