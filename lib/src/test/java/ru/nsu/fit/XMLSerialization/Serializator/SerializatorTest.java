package ru.nsu.fit.XMLSerialization.Serializator;

import org.junit.Test;
import ru.nsu.fit.XMLSerialization.XMLSerializator;

import java.io.*;

public class SerializatorTest {

  @Test
  public void testCyclesSerialization(){
    TestClass testClass = new TestClass();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    XMLSerializator serializator = new XMLSerializator(outputStream);
    serializator.write(testClass);
    serializator.flush();
    System.out.println(outputStream);
    /*
    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializator deserializator = new XMLDeserializator(byteArrayInputStream);*/
  }

  @Test
  public void testArraySerialization(){
    TestClass2 testClass = new TestClass2();
    TestClass2 testClass1 = new TestClass2();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    TestClass2[] objects = new TestClass2[] {testClass, testClass1};

    XMLSerializator serializator = new XMLSerializator(outputStream);
    serializator.write(objects);
    serializator.flush();
    System.out.println(outputStream);
    /*
    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializator deserializator = new XMLDeserializator(byteArrayInputStream);*/
  }
}
