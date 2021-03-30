package ru.nsu.fit.XMLSerialization.AnotationAndSerializableTest;

import org.junit.Test;
import ru.nsu.fit.XMLSerialization.XMLSerializable;
import ru.nsu.fit.XMLSerialization.XMLSerializer;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import static org.junit.Assert.*;

public class AnnotationTest {
  private class SerializableClass implements Serializable {}

  @XMLSerializable
  private class AnnotatedClass {}

  private class NotSerializable {}

  @Test
  public void testAnnotation() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    XMLSerializer serializer = new XMLSerializer(out);
    serializer.write(new AnnotatedClass());
  }

  @Test
  public void testSerializable() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    XMLSerializer serializer = new XMLSerializer(out);
    serializer.write(new SerializableClass());
  }

  @Test
  public void testNotSerializable() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    XMLSerializer serializer = new XMLSerializer(out);
    assertThrows(RuntimeException.class, () -> serializer.write(new NotSerializable()));
  }
}
