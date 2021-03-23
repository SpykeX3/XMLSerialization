package ru.nsu.fit.XMLSerialization;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.misc.Unsafe;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class XMLDeserializator {

  HashMap<Integer, Object> parsedObjects = new HashMap<>();
  HashMap<Integer, Node> objectPool = new HashMap<>();

  ArrayList<Integer> objectStream = new ArrayList<>();

  public XMLDeserializator(InputStream inputStream)
      throws InvalidClassException, ClassNotFoundException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return;
    }
    Document doc;
    try {
      doc = builder.parse(inputStream);
    } catch (SAXException | IOException e) {
      e.printStackTrace();
      return;
    }
    doc.getDocumentElement().normalize();
    Node pool = doc.getElementsByTagName("Object_pool").item(0);
    Node stream = doc.getElementsByTagName("Object_stream").item(0);
    createXMLObjectPool(pool);
    createStreamIDList(stream);

    System.out.println(Arrays.toString(objectPool.entrySet().toArray()));

    for (Node node : objectPool.values()) {
      parseObject(node);
    }

    System.out.println(Arrays.toString(parsedObjects.entrySet().toArray()));
    for (Integer id : objectPool.keySet()) {
      parseObjectFields(id);
    }
    System.out.println(Arrays.toString(parsedObjects.entrySet().toArray()));
  }

  private void createXMLObjectPool(Node pool) {
    NodeList poolNodes = pool.getChildNodes();
    for (int tag = 0; tag < poolNodes.getLength(); ++tag) {
      Node bean = poolNodes.item(tag);
      if (bean.getAttributes() == null) continue;
      int id = Integer.parseInt(bean.getAttributes().getNamedItem("id").getTextContent());
      objectPool.put(id, bean);
    }
  }

  private void createStreamIDList(Node stream) {
    NodeList streamNodes = stream.getChildNodes();
    for (int tag = 0; tag < streamNodes.getLength(); ++tag) {
      Node link = streamNodes.item(tag);
      if (link.getAttributes() == null) continue;
      int id = Integer.parseInt(link.getAttributes().getNamedItem("id").getTextContent());
      objectStream.add(id);
    }
  }

  private void parseObjectFields(int id) throws ClassNotFoundException {
    Node bean = objectPool.get(id);
    Object parsedObject = parsedObjects.get(id);
    Class<?> clazz = parsedObject.getClass();

    for (Field field : clazz.getDeclaredFields()) {
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      field.setAccessible(true);

      String name = field.getName();
      for (int i = 0; i < bean.getChildNodes().getLength(); ++i) {
        if (bean.getChildNodes().item(i).getNodeName().equals(name)) {
          Node fieldNode = bean.getChildNodes().item(i);
          String fieldType = fieldNode.getAttributes().getNamedItem("type").getTextContent();
          try {

            if (fieldType.contains("class ")) {
              setObjectField(field, parsedObject, fieldNode);
              break;
            }

            setPrimitiveField(field, parsedObject, fieldNode);
            break;
          } catch (IllegalAccessException e) {
            break;
          }
        }
      }
    }
  }

  private void setPrimitiveField(Field field, Object parsedObject, Node bean)
      throws IllegalAccessException {
    switch (field.getType().toString()) {
      case ("byte"):
        field.set(parsedObject, Byte.valueOf(bean.getTextContent()));
        break;
      case ("int"):
        field.set(parsedObject, Integer.valueOf(bean.getTextContent()));
        break;
      case ("float"):
        field.set(parsedObject, Float.valueOf(bean.getTextContent()));
        break;
      case ("double"):
        field.set(parsedObject, Double.valueOf(bean.getTextContent()));
        break;
      case ("short"):
        field.set(parsedObject, Short.valueOf(bean.getTextContent()));
        break;
      case ("long"):
        field.set(parsedObject, Long.valueOf(bean.getTextContent()));
        break;
      case ("boolean"):
        field.set(parsedObject, Boolean.valueOf(bean.getTextContent()));
        break;
      case ("char"):
        field.set(parsedObject, bean.getTextContent().toCharArray()[0]);
        break;
    }
  }

  private void setObjectField(Field field, Object parsedObject, Node bean)
      throws IllegalAccessException {

    if (bean.getTextContent().equals("null")) {
      field.set(parsedObject, null);
    }
    int fieldId = Integer.parseInt(bean.getTextContent());
    field.set(parsedObject, parsedObjects.get(fieldId));
  }

  private void parseObject(Node bean) throws ClassNotFoundException, InvalidClassException {
    String type = bean.getAttributes().getNamedItem("type").getTextContent();
    int id = Integer.parseInt(bean.getAttributes().getNamedItem("id").getTextContent());
    if (type.contains("class ")) {
      type = type.replaceAll("class ", "");
    }

    Class<?> clazz = Class.forName(type);
    int length = 0;
    // clazz.newInstance();
    Object createdObject = null;

    if (clazz.isArray()) {
      length = Integer.parseInt(bean.getAttributes().getNamedItem("length").getTextContent());
      Class<?> arrayType = clazz.getComponentType();
      createdObject = Array.newInstance(arrayType, length);
      parsedObjects.put(id, createdObject);
      return;
    }
    if (clazz.equals(String.class)){
      parsedObjects.put(id, bean.getTextContent());
      return;
    }

    for (Constructor<?> cons : clazz.getConstructors()) {
      try {
        cons.setAccessible(true);
        if (cons.getParameterCount() == 0) {
          createdObject = cons.newInstance();
          parsedObjects.put(id, createdObject);
          return;
        }
        if (clazz.isArray() && cons.getParameterCount() == 1) {
          createdObject = cons.newInstance(length);
          parsedObjects.put(id, createdObject);
          return;
        }
      } catch (IllegalAccessException|InstantiationException|InvocationTargetException  e) {
        throw new RuntimeException(e);
      }
    }

    Unsafe unsafe;
    try {
      Field f = null;
      f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      unsafe = (Unsafe) f.get(null);
    } catch (NoSuchFieldException | IllegalAccessException ignored) { // absolutely impossible
      return;
    }

    try {
      createdObject = unsafe.allocateInstance(clazz);
      parsedObjects.put(id, createdObject);
    } catch (InstantiationException e) {
      throw new InvalidClassException(e.toString());
    }
  }

  /* todo:
  public boolean hasNext(){
      return inputStream.
  }*/
  /*
  public Object readObject(){

  }
  */

}
