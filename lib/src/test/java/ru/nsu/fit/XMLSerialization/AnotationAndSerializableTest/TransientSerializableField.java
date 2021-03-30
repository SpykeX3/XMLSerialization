package ru.nsu.fit.XMLSerialization.AnotationAndSerializableTest;

import org.junit.Assert;
import org.junit.Test;
import ru.nsu.fit.XMLSerialization.XMLDeserializer;
import ru.nsu.fit.XMLSerialization.XMLSerializable;
import ru.nsu.fit.XMLSerialization.XMLSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.util.List;

public class TransientSerializableField {
  @XMLSerializable
  class AnnotatedClass {}

  class SerializableClass implements Serializable {}

  class NotSerializable {}

  @XMLSerializable
  static class testClass {
    AnnotatedClass annotatedClass;
    SerializableClass serializableClass;
    NotSerializable notSerializable;
    transient AnnotatedClass transientAnnotated;
    static AnnotatedClass staticAnnotated;

    testClass(
        AnnotatedClass a1,
        SerializableClass s1,
        NotSerializable n1,
        AnnotatedClass t1,
        AnnotatedClass st1) {
      annotatedClass = a1;
      serializableClass = s1;
      notSerializable = n1;
      transientAnnotated = t1;
      staticAnnotated = st1;
    }
  }

  @Test
  public void testAnnotation() throws InvalidClassException, ClassNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    XMLSerializer serializer = new XMLSerializer(out);
    AnnotatedClass a1 = new AnnotatedClass();
    AnnotatedClass a2 = new AnnotatedClass();
    AnnotatedClass a3 = new AnnotatedClass();
    SerializableClass s1 = new SerializableClass();
    NotSerializable n1 = new NotSerializable();

    serializer.write(new testClass(a1, s1, n1, a2, a3));
    serializer.flush();

    byte[] buffer = out.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);

    Assert.assertNotNull(testClass.staticAnnotated);
    //Set static in test class equals 0 before deserialization, to check deserializer will not overwrite it
    testClass.staticAnnotated = null;
    List<Object> list = deserializer.getDeserializedObjects();
    Assert.assertEquals(1, list.size());

    testClass result = (testClass) list.get(0);

    Assert.assertNull(result.notSerializable);
    Assert.assertNull(result.transientAnnotated);
    Assert.assertNotNull(result.annotatedClass);
    Assert.assertNotNull(result.serializableClass);
    Assert.assertNull(testClass.staticAnnotated);
  }
}
