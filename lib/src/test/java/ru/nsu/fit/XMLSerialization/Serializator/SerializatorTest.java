package ru.nsu.fit.XMLSerialization.Serializator;

import org.junit.Test;
import ru.nsu.fit.XMLSerialization.Library;
import ru.nsu.fit.XMLSerialization.LibraryTest;
import ru.nsu.fit.XMLSerialization.XMLSerializator;

import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SerializatorTest {

    @Test
    public void testAnnotationOnClass() {
        TestClass testClass = new TestClass();
        TestClass testClass1 = new TestClass();
        XMLSerializator serializator = new XMLSerializator();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult streamResult = new StreamResult(outputStream);
        List<Object> objects = new ArrayList<>();
        objects.add(testClass);
        objects.add(testClass1);

        serializator.createXMLDocument(objects.stream(), streamResult);
        System.out.println(streamResult.getOutputStream());
    }
}
