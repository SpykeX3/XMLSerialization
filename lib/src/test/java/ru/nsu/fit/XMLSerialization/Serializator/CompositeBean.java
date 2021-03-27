package ru.nsu.fit.XMLSerialization.Serializator;

public class CompositeBean {
    SimpleBean first;
    SimpleBean second;

    public CompositeBean() {
        Counting.addOne(this.getClass());
    }

    public CompositeBean(SimpleBean bean1, SimpleBean bean2) {
        first = bean1;
        second = bean2;
    }
}
