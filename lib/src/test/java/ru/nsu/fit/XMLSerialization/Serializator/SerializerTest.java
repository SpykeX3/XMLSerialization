package ru.nsu.fit.XMLSerialization.Serializator;

import org.junit.Test;

import static org.junit.Assert.*;

import ru.nsu.fit.XMLSerialization.XMLDeserializer;
import ru.nsu.fit.XMLSerialization.XMLSerializer;

import java.io.*;
import java.util.List;
import java.util.function.Predicate;

public class SerializerTest {

    @Test
    public void testCyclesSerialization() throws InvalidClassException, ClassNotFoundException {
        TestClass testClass = new TestClass(new TestClass2[]{new TestClass2(), new TestClass2()});
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        XMLSerializer serializator = new XMLSerializer(outputStream);
        serializator.write(testClass);
        serializator.flush();
        System.out.println(outputStream);

        byte[] buffer = outputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
        XMLDeserializer deserializator = new XMLDeserializer(byteArrayInputStream);
        List<?> result = deserializator.getDeserializedObjects();
        assertEquals(1, result.size());
        Object obj = result.get(0);
        assertTrue(obj instanceof TestClass);
        TestClass deserialized = (TestClass) obj;
        assertEquals(testClass.Wzuuuuh, deserialized.Wzuuuuh);
        //assertArrayEquals(testClass.arrayOfObjects, deserialized.arrayOfObjects);
    }

    @Test
    public void testArraySerialization() throws ClassNotFoundException, InvalidClassException {
        TestClass2 testClass = new TestClass2();
        TestClass2 testClass1 = new TestClass2();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TestClass2[] objects = new TestClass2[]{testClass, testClass1};

        XMLSerializer serializator = new XMLSerializer(outputStream);
        serializator.write(objects);
        serializator.flush();
        //System.out.println(outputStream);

        byte[] buffer = outputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
        XMLDeserializer deserializator = new XMLDeserializer(byteArrayInputStream);
    }

    @Test
    public void testLazyFilter() throws InvalidClassException, ClassNotFoundException {
        CompositeBean compositeMatching = new CompositeBean(new SimpleBean(1, 1f, "1", '1'),
                new SimpleBean(2, 2f, "2", '2'));
        CompositeBean compositeNotMatching = new CompositeBean(new SimpleBean(5, 5f, "I will not match =(", '-'),
                new SimpleBean());
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

        List<CompositeBean> matchedBeans = deserializer.filter(new Predicate<CompositeBean>() {
            @Override
            public boolean test(CompositeBean compositeBean) {
                return compositeBean.first.vInt == 1;
            }
        });
        // check that only necessary instances was created
        assertEquals(instancesCreated + 3, Counting.getCount(SimpleBean.class));
        assertEquals(2, matchedBeans.size());
        matchedBeans.forEach(m -> assertEquals(1, m.first.vInt));
        matchedBeans.forEach(m -> assertEquals(2, m.second.vInt));
    }
}
