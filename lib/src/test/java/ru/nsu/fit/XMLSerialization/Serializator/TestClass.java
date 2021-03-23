package ru.nsu.fit.XMLSerialization.Serializator;

import ru.nsu.fit.XMLSerialization.XMLSerializable;

@XMLSerializable
public class TestClass {
    private final TestClass2 testClass20 = new TestClass2();
    private TestClass2 testClass21 = testClass20;
    private final TestClass2 testClass22 = new TestClass2();
    final Integer Wzuuuuh = 11;

    TestClass(){
       /* testClass20.setTestClass(this);
        testClass22.setTestClass(this);*/
    }
}
