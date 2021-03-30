package ru.nsu.fit.XMLSerialization.Serializator;

import ru.nsu.fit.XMLSerialization.XMLSerializable;

@XMLSerializable
public class CompositeBean {
    SimpleBean first;
    SimpleBean second;
    int val;

    public CompositeBean() {
        Counting.addOne(this.getClass());
    }

    public CompositeBean(SimpleBean bean1, SimpleBean bean2) {
        this();
        first = bean1;
        second = bean2;
    }

    public CompositeBean(SimpleBean bean1, SimpleBean bean2, int val) {
        this();
        first = bean1;
        second = bean2;
        this.val = val;
    }
}
