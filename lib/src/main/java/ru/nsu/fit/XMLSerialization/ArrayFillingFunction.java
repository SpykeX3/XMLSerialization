package ru.nsu.fit.XMLSerialization;

@FunctionalInterface
public interface ArrayFillingFunction {
    void setArrayCell(Object array, int pos, String token);
}
