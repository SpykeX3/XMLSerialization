package ru.nsu.fit.XMLSerialization.AnotationAndSerializableTest;

import org.junit.Assert;
import org.junit.Test;
import ru.nsu.fit.XMLSerialization.XMLDeserializer;
import ru.nsu.fit.XMLSerialization.XMLSerializable;
import ru.nsu.fit.XMLSerialization.XMLSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.util.List;

public class TransientSerializableField {
  @XMLSerializable
  class AnnotatedClass {}

  @XMLSerializable
  static class testClass {
    AnnotatedClass annotatedClass;
    transient AnnotatedClass transientAnnotated;
    static AnnotatedClass staticAnnotated;

    testClass(
        AnnotatedClass a1,
        AnnotatedClass t1,
        AnnotatedClass st1) {
      annotatedClass = a1;
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

    serializer.write(new testClass(a1, a2, a3));
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

    Assert.assertNull(result.transientAnnotated);
    Assert.assertNotNull(result.annotatedClass);
    Assert.assertNull(testClass.staticAnnotated);
  }
}
