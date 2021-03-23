package ru.nsu.fit.XMLSerialization.Serializator;

import ru.nsu.fit.XMLSerialization.XMLSerializable;

@XMLSerializable
public class TestClass {
  private final TestClass2 testClass20 = new TestClass2();
  Integer Wzuuuuh = 11;

  TestClass() {
    testClass20.setTestClass(this);
  }
}
