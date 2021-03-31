package ru.nsu.fit.XMLSerialization.Serializator;

import java.util.Arrays;

public class CompositeArrayBean {
    SimpleBean[] array;
    SimpleBean anotherSB;

    CompositeArrayBean(SimpleBean[] array) {
        this.array = array;
        this.anotherSB = new SimpleBean(1, 2, "3", '4');
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompositeArrayBean)) return false;
        CompositeArrayBean other = (CompositeArrayBean) obj;
        return anotherSB.equals(other.anotherSB) && Arrays.equals(array, other.array);
    }
}
