package ru.nsu.fit.XMLSerialization;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLSerializator {
  private int id = 0;
  private Queue<Object> queue = new ArrayDeque<>();
  private final StreamResult streamResult;
  private Map<Object, String> parsedObjects;

  public XMLSerializator(OutputStream stream) {
    streamResult = new StreamResult(stream);
  }

  private static final Set<String> primitives =
      new HashSet<>(
          Arrays.asList(
              "byte",
              "short",
              "int",
              "long",
              "float",
              "double",
              "boolean",
              "char",
              "class java.lang.String"));

  private static final Set<String> wrappers =
      new HashSet<>(
          Arrays.asList(
              "class java.lang.Byte",
              "class java.lang.Short",
              "class java.lang.Integer",
              "class java.lang.Long",
              "class java.lang.Float",
              "class java.lang.Double",
              "class java.lang.Boolean",
              "class java.lang.Character"));

  public void write(Object obj) throws NullPointerException {
    if (obj == null) throw new NullPointerException();
    queue.add(obj);
  }

  public void flush() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    Document doc = builder.newDocument();
    Element rootElement = doc.createElementNS("xml_stream", "XML_STREAM");

    Element objectPool = doc.createElement("Object_pool");
    Element objectStream = doc.createElement("Object_stream");
    doc.appendChild(rootElement);
    rootElement.appendChild(objectStream);
    rootElement.appendChild(objectPool);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = transformerFactory.newTransformer();
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
    parsedObjects = new IdentityHashMap<>();

    for (Object x : queue) {
      parseObject(doc, x, objectPool, objectStream);
    }

    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    DOMSource source = new DOMSource(doc);

    try {
      transformer.transform(source, streamResult);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }

    queue.clear();
    id = 0;

  }

  private void parseObject(Document doc, Object obj, Element pool, Element stream) {

    if (parsedObjects.containsKey(obj)) {
      stream.appendChild(createLink(obj, doc));
      return;
    }

    Queue<Object> queue = new ArrayDeque<>();
    queue.add(obj);
    boolean isStream = true;
    parsedObjects.put(obj, String.valueOf(++id));
    do {
      obj = queue.remove();
      Class objType = obj.getClass();
      Element currElement = doc.createElement("bean");
      String parsedId = String.valueOf(parsedObjects.get(obj));
      parsedObjects.put(obj, parsedId);

      currElement.setAttribute("id", parsedId);
      currElement.setAttribute("type", objType.toString());

      if (objType.isArray()) {
        if (!primitives.contains(objType.getComponentType().toString())) {
          Object[] array = (Object[]) obj;
          String[] idInArray = new String[array.length];
          for (int i = 0; i < array.length; ++i) {
            if (parsedObjects.containsKey(array[i])) {
              idInArray[i] = parsedObjects.get(array[i]);
            } else {
              idInArray[i] = String.valueOf(++id);
              parsedObjects.put(array[i], String.valueOf(id));
              if (array[i] == null)
                continue;
              queue.add(array[i]);
            }
          }
          currElement.appendChild(doc.createTextNode(Arrays.toString(idInArray)));
        } else {
          String[] arrayValues = new String[Array.getLength(obj)];
          for (int i = 0; i < Array.getLength(obj); ++i) {
            switch (objType.getComponentType().toString()) {
              case "byte":
                arrayValues[i] = String.valueOf(Array.getByte(obj, i));
                break;
              case "short":
                arrayValues[i] = String.valueOf(Array.getShort(obj, i));
                break;
              case "int":
                arrayValues[i] = String.valueOf(Array.getInt(obj, i));
                break;
              case "long":
                arrayValues[i] = String.valueOf(Array.getLong(obj, i));
                break;
              case "float":
                arrayValues[i] = String.valueOf(Array.getFloat(obj, i));
                break;
              case "double":
                arrayValues[i] = String.valueOf(Array.getDouble(obj, i));
                break;
              case "boolean":
                arrayValues[i] = String.valueOf(Array.getBoolean(obj, i));
                break;
              case "class java.lang.String":
                arrayValues[i] = String.valueOf(Array.get(obj, i));
                break;
              case "char":
                arrayValues[i] = String.valueOf(Array.getChar(obj, i));
                break;
            }
          }
          currElement.appendChild(doc.createTextNode(Arrays.toString(arrayValues)));
        }
      } else {
        if (primitives.contains(objType.toString()) || wrappers.contains(objType.toString())){
          currElement.setTextContent(obj.toString());
        } else {
          for (Field field : obj.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
              continue;
            }
            try {
              field.setAccessible(true);
            } catch (Exception e) {
              continue;
            }
            Type type = field.getType();
            String name = field.getName();
            String value = null;

            if (name.contains("$")) continue;
            Element elem = doc.createElement(name);
            try {
              value = parseObjectValue(field, obj, queue);
              elem.appendChild(doc.createTextNode(value));
            } catch (IllegalAccessException ignored) {}

            // System.out.println(name);

            // System.out.println(type);
            elem.setAttribute("type", type.toString());
            currElement.appendChild(elem);
          }
        }
      }
      if (isStream) {
        stream.appendChild(createLink(obj, doc));
        isStream = false;
      }
      pool.appendChild(currElement);

    } while (queue.size() > 0);
  }

  private Element createLink(Object obj, Document doc) {
    Element elem = doc.createElement("link");
    elem.setAttribute("id", parsedObjects.get(obj));
    return elem;
  }

  private String parseObjectValue(Field field, Object obj, Queue<Object> queue)
      throws IllegalAccessException {
    // if type is primitive or wrapper for primitive
    if (primitives.contains(field.getType().getTypeName())
        || wrappers.contains(field.getType().getTypeName())) {
      return String.valueOf(field.get(obj));
    }
    // if we parsed this object we return saved id

    if (parsedObjects.containsKey(field.get(obj))) {
      return parsedObjects.get(field.get(obj));
    }
    // if obj is null value is null
    if (field.get(obj) == null) {
      return "null";
    }
    System.out.println(field.get(obj));
    queue.add(field.get(obj));
    parsedObjects.put(field.get(obj), String.valueOf(id + 1));
    return String.valueOf(++id);
  }
}
