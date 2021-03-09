package ru.nsu.fit.XMLSerialization.Serializator;

import org.junit.Test;
import ru.nsu.fit.XMLSerialization.XMLSerializator;

import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class SerializatorTest {
  private static class DummyClassOuter {
    DummyInner a;
    DummyInner b;
    DummyInner c;
    DummyInner d;
    DummyInner e;

    DummyClassOuter() {
      b = new DummyInner();
      c = new DummyInner();
      d = new DummyInner();
      e = new DummyInner();
    }
  }

  private static class DummyInner {}

  @Test
  public void testArraySerialization() {
    TestClass testClass = new TestClass();
    TestClass testClass1 = new TestClass();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLSerializator serializator = new XMLSerializator(outputStream);
    TestClass[] objects = new TestClass[]{testClass, testClass1};

    serializator.write(objects);
    serializator.flush();
    System.out.println(outputStream);
  }
}
