package ru.nsu.fit.XMLSerialization.Serializator;

import org.junit.Test;

import static org.junit.Assert.*;

import ru.nsu.fit.XMLSerialization.XMLDeserializer;
import ru.nsu.fit.XMLSerialization.XMLSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SerializerTest {

  static Random rnd = new Random();

  private void assertEquality(Object expected, Object actual) {
    if (expected == null && actual == null) {
      return;
    }
    assertNotNull(expected);
    assertEquals(expected.getClass(), actual.getClass());
    if (expected.getClass().isArray()) {
      assertArraysEquality(expected, actual);
    } else if (expected.getClass().equals(Double.class)) {
      assertEquals((double) expected, (double) actual, 0.1);
    } else if (expected.getClass().equals(Float.class)) {
      assertEquals((float) expected, (float) actual, 0.1);
    } else {
      assertEquals(expected, actual);
    }
  }

  private void assertArraysEquality(Object arrayExpected, Object actual) {
    if (arrayExpected == null && actual == null) {
      return;
    }
    assertTrue(actual.getClass().isArray());
    assertTrue(arrayExpected.getClass().isArray());
    assertEquals(arrayExpected.getClass(), actual.getClass());
    assertEquals(arrayExpected.getClass().getComponentType(), actual.getClass().getComponentType());
    int len = Array.getLength(arrayExpected);
    assertEquals(len, Array.getLength(actual));
    for (int i = 0; i < len; i++) {
      Object expectedObj = Array.get(arrayExpected, i);
      Object actualObj = Array.get(actual, i);
      assertEquality(expectedObj, actualObj);
    }
  }

  private SimpleBean randomBean() {
    return new SimpleBean(
        rnd.nextInt(),
        rnd.nextInt(),
        String.valueOf(rnd.nextInt()),
        String.valueOf(rnd.nextInt()).charAt(0));
  }

  @Test
  public void testCyclesSerialization() throws InvalidClassException, ClassNotFoundException {
    TestClass testClass = new TestClass(new TestClass2[] {new TestClass2(), new TestClass2()});
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    XMLSerializer serializator = new XMLSerializer(outputStream);
    serializator.write(testClass);
    serializator.flush();

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializator = new XMLDeserializer(byteArrayInputStream);
    List<?> result = deserializator.getDeserializedObjects();
    assertEquals(1, result.size());
    Object obj = result.get(0);
    assertTrue(obj instanceof TestClass);
    TestClass deserialized = (TestClass) obj;
    assertEquals(testClass.Wzuuuuh, deserialized.Wzuuuuh);
  }

  @Test
  public void testObjectArraySerialization() throws ClassNotFoundException, InvalidClassException {
    SimpleBean bean1 = new SimpleBean(1, 1f, "1", '1');
    SimpleBean bean2 = new SimpleBean(2, 2f, "2", '2');
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    SimpleBean[] objects = new SimpleBean[] {bean1, bean2, null};

    XMLSerializer serializer = new XMLSerializer(outputStream);
    serializer.write(objects);
    serializer.flush();
    // System.out.println(outputStream);

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);
    List<?> result = deserializer.getDeserializedObjects();
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof SimpleBean[]);
    SimpleBean[] deserialized = (SimpleBean[]) result.get(0);
    assertEquality(objects, deserialized);
  }

  @Test
  public void testPrimitiveArraySerialization()
      throws ClassNotFoundException, InvalidClassException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<Object> arrays =
        List.of(
            new int[] {0, 1, 2},
            new String[] {"String, one", "String, two", null},
            new float[] {1f, 2f, 3f},
            new char[] {'a', 'b', 'c'},
            new byte[] {1, 2, 3},
            new short[] {1, 2, 3},
            new boolean[] {true, false, true},
            new double[] {1d, 2d, 3d},
            new long[] {1L, 2L, 3L});
    XMLSerializer serializer = new XMLSerializer(outputStream);
    arrays.forEach(serializer::write);
    serializer.flush();
    // System.out.println(outputStream);

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);
    List<Object> result = deserializer.getDeserializedObjects();
    assertEquals(arrays.size(), result.size());
    for (int i = 0; i < result.size(); i++) {
      assertArraysEquality(arrays.get(i), result.get(i));
    }
  }

  @Test
  public void testArrayOfArraysSerialization()
      throws ClassNotFoundException, InvalidClassException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<Object> arrays =
        List.of(
            new int[][] {new int[] {1, 2}, new int[] {3, 4}},
            new SimpleBean[][] {new SimpleBean[] {new SimpleBean(1, 1, "1", '1'), null}});
    XMLSerializer serializer = new XMLSerializer(outputStream);
    arrays.forEach(serializer::write);
    serializer.flush();
    // System.out.println(outputStream);

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);
    List<Object> result = deserializer.getDeserializedObjects();
    assertEquals(arrays.size(), result.size());
    for (int i = 0; i < result.size(); i++) {
      assertArraysEquality(arrays.get(i), result.get(i));
    }
  }

  @Test
  public void testCollections() throws ClassNotFoundException, InvalidClassException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    HashSet<SimpleBean> hashSet = new HashSet<>();
    hashSet.add(randomBean());
    hashSet.add(randomBean());
    hashSet.add(randomBean());

    List<SimpleBean> linkedList = new LinkedList<>();
    linkedList.add(randomBean());
    linkedList.add(randomBean());
    linkedList.add(randomBean());
    linkedList.add(randomBean());

    List<SimpleBean> arrayList = new ArrayList<>();
    arrayList.add(randomBean());
    arrayList.add(randomBean());
    arrayList.add(randomBean());

    Queue<SimpleBean> arrayDeque = new ArrayDeque<>();
    arrayDeque.add(randomBean());
    arrayDeque.add(randomBean());

    List<SimpleBean> vector = new Vector<>();
    vector.add(randomBean());
    vector.add(randomBean());

    List<Object> arrays =
        List.of(
            List.of(1, 2, 3),
            Map.of(1, randomBean(), 2, randomBean()),
            hashSet,
            linkedList,
            arrayList,
            arrayDeque);
    XMLSerializer serializer = new XMLSerializer(outputStream);
    arrays.forEach(serializer::write);
    serializer.flush();
    // System.out.println(outputStream);

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);
    List<Object> result = deserializer.getDeserializedObjects();
    assertEquals(arrays.size(), result.size());
    for (int i = 0; i < result.size() - 1; i++) {
      assertEquality(arrays.get(i), result.get(i));
    }
    ArrayDeque<SimpleBean> arrayDeque1 = (ArrayDeque<SimpleBean>) result.get(result.size()-1);
    assertEquality(arrayDeque1.size(), arrayDeque.size());
    for (int i = 0; i < arrayDeque1.size()-1; ++i){
      assertEquality(arrayDeque.peek(), arrayDeque1.peek());
    }
  }

  @Test
  public void testLazyFilter() throws InvalidClassException, ClassNotFoundException {
    CompositeBean compositeMatching =
        new CompositeBean(new SimpleBean(1, 1f, "1", '1'), new SimpleBean(2, 2f, "2", '2'));
    CompositeBean compositeNotMatching =
        new CompositeBean(new SimpleBean(5, 5f, "I will not match =(", '-'), new SimpleBean());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLSerializer serializator = new XMLSerializer(outputStream);
    serializator.write(compositeMatching);
    serializator.write(compositeMatching);
    serializator.write(compositeNotMatching);
    serializator.write(compositeNotMatching);
    serializator.flush();
    int instancesCreated = Counting.getCount(SimpleBean.class);

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);

    List<CompositeBean> matchedBeans =
        deserializer.filter(
            new Predicate<CompositeBean>() {
              @Override
              public boolean test(CompositeBean compositeBean) {
                return compositeBean.first.vInt == 1;
              }
            },
            CompositeBean.class);
    // check that only necessary instances was created
    assertEquals(instancesCreated + 3, Counting.getCount(SimpleBean.class));
    assertEquals(2, matchedBeans.size());
    matchedBeans.forEach(m -> assertEquals(1, m.first.vInt));
    matchedBeans.forEach(m -> assertEquals(2, m.second.vInt));
  }

  @Test
  public void testLazyFilterArrays1() throws InvalidClassException, ClassNotFoundException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLSerializer serializator = new XMLSerializer(outputStream);
    SimpleBean good = new SimpleBean(1, 1, "1", '1');
    SimpleBean bad = new SimpleBean(-100, -100, "z", 'z');
    CompositeArrayBean c1 = new CompositeArrayBean(new SimpleBean[] {good, bad});
    CompositeArrayBean c2 = new CompositeArrayBean(new SimpleBean[] {bad, good});
    CompositeArrayBean c3 = new CompositeArrayBean(new SimpleBean[] {bad, bad, bad});
    serializator.write(c1);
    serializator.write(c2);
    serializator.write(c3);
    serializator.flush();

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);

    List<CompositeArrayBean> matchedBeans =
        deserializer.filter(
            new Predicate<>() {
              @Override
              public boolean test(CompositeArrayBean compositeBean) {
                return compositeBean.array.length % 2 == 0;
              }
            },
            CompositeArrayBean.class);
    assertEquals(2, matchedBeans.size());
    assertEquals(c1, matchedBeans.get(0));
    assertEquals(c2, matchedBeans.get(1));
  }

  @Test
  public void testLazyFilterArrays2() throws InvalidClassException, ClassNotFoundException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLSerializer serializator = new XMLSerializer(outputStream);
    SimpleBean good = new SimpleBean(1, 1, "1", '1');
    SimpleBean bad = new SimpleBean(-100, -100, "z", 'z');
    CompositeArrayBean c1 = new CompositeArrayBean(new SimpleBean[] {good, bad});
    CompositeArrayBean c2 = new CompositeArrayBean(new SimpleBean[] {bad, good});
    CompositeArrayBean c3 = new CompositeArrayBean(new SimpleBean[] {bad, bad, bad});
    serializator.write(c1);
    serializator.write(c2);
    serializator.write(c3);
    serializator.flush();

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);

    List<CompositeArrayBean> matchedBeans =
        deserializer.filter(
            new Predicate<CompositeArrayBean>() {
              @Override
              public boolean test(CompositeArrayBean compositeBean) {
                return compositeBean.array[0].vInt < 0;
              }
            },
            CompositeArrayBean.class);
    assertEquals(2, matchedBeans.size());
    assertEquals(c2, matchedBeans.get(0));
    assertEquals(c3, matchedBeans.get(1));
  }

  @Test
  public void testLazyFilterMixedClasses() throws InvalidClassException, ClassNotFoundException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLSerializer serializator = new XMLSerializer(outputStream);
    SimpleBean good = new SimpleBean(1, 1, "1", '1');
    SimpleBean bad = new SimpleBean(-100, -100, "z", 'z');
    CompositeArrayBean c1 = new CompositeArrayBean(new SimpleBean[] {good, bad});
    CompositeArrayBean c2 = new CompositeArrayBean(new SimpleBean[] {bad, good});
    CompositeArrayBean c3 = new CompositeArrayBean(new SimpleBean[] {bad, bad, bad});
    serializator.write(c2);
    serializator.write(randomBean());
    serializator.write(c1);
    serializator.write(randomBean());
    serializator.write(randomBean());
    serializator.write(c3);
    serializator.write(randomBean());
    serializator.flush();

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);

    List<CompositeArrayBean> matchedBeans =
        deserializer.filter(
            new Predicate<CompositeArrayBean>() {
              @Override
              public boolean test(CompositeArrayBean compositeBean) {
                return compositeBean.array[0].vInt < 0;
              }
            },
            CompositeArrayBean.class);
    assertEquals(2, matchedBeans.size());
    assertEquals(c2, matchedBeans.get(0));
    assertEquals(c3, matchedBeans.get(1));
  }

  @Test
  public void testLazyFilterCollections() throws InvalidClassException, ClassNotFoundException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    XMLSerializer serializator = new XMLSerializer(outputStream);
    SimpleBean good = new SimpleBean(1, 1, "1", '1');
    SimpleBean bad = new SimpleBean(-100, -100, "z", 'z');
    ArrayList<Integer> arrayList = new ArrayList();
    arrayList.add(1);
    arrayList.add(666);
    arrayList.add(3);
    serializator.write(arrayList);
    serializator.write(List.of(1, 2, 3, 666));
    serializator.flush();

    byte[] buffer = outputStream.toByteArray();
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    int instancesCreated = Counting.getCount(SimpleBean.class);
    XMLDeserializer deserializer = new XMLDeserializer(byteArrayInputStream);

    List<ArrayList> matchedBeans =
        deserializer.filter(
            new Predicate<>() {
              @Override
              public boolean test(ArrayList target) {
                return target.get(1).equals(666);
              }
            },
            ArrayList.class);
    assertEquals(instancesCreated, Counting.getCount(SimpleBean.class));
    assertEquals(1, matchedBeans.size());
    assertEquals(arrayList, matchedBeans.get(0));
  }

}
