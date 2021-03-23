package ru.nsu.fit.XMLSerialization.Serializator;

import ru.nsu.fit.XMLSerialization.XMLSerializable;

import java.util.ArrayList;
import java.util.List;

@XMLSerializable
public class TestClass2 {
    private List<Integer> la;
    private TestClass testClass;
    private String string = "lallala";

    public TestClass2(){
        la = new ArrayList<>();
    }
    public void setTestClass(TestClass testClass) {
        la.add(5);
        la.add(4);
        this.testClass = testClass;
    }
}