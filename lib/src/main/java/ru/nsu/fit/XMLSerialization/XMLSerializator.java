package ru.nsu.fit.XMLSerialization;

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

  private static final Set<String> primAndWrappers =
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
              "java.lang.Byte",
              "java.lang.Short",
              "java.lang.Integer",
              "java.lang.Long",
              "java.lang.Float",
              "java.lang.Double",
              "java.lang.Boolean",
              "java.lang.Character",
              "java.lang.String"));

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
    Element rootElement = doc.createElementNS("xml_stream", "XML_STEAM");

    Element objectPull = doc.createElement("Object_pool");
    Element objectSteam = doc.createElement("Object_stream");
    doc.appendChild(rootElement);
    rootElement.appendChild(objectSteam);
    rootElement.appendChild(objectPull);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = transformerFactory.newTransformer();
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
    parsedObjects = new IdentityHashMap<>();

    for (Object x : queue) {
      parseObject(doc, x, objectPull, objectSteam);
    }

    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    DOMSource source = new DOMSource(doc);

    try {
      transformer.transform(source, streamResult);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }

    queue = new ArrayDeque<>();
    id = 0;
  }

  private void parseObject(Document doc, Object obj, Element pull, Element stream) {

    if (parsedObjects.containsKey(obj)) {
      stream.appendChild(createLink(obj, doc));
      return;
    }

    Queue<Object> queue = new ArrayDeque<>();
    queue.add(obj);
    boolean isSteam = true;
    parsedObjects.put(obj, String.valueOf(++id));
    do {
      obj = queue.remove();

      Element currElement = doc.createElement("bean");

      String parsedId = String.valueOf(parsedObjects.get(obj));
      parsedObjects.put(obj, parsedId);

      currElement.setAttribute("id", parsedId);
      currElement.setAttribute("type", obj.getClass().toString());

      if (obj.getClass().isArray()){
        Object[] array = (Object[]) obj;
        String[] idInArray = new String[array.length];
        for (int i = 0; i < array.length; ++i){
          if (parsedObjects.containsKey(array[i])){
            idInArray[i] = parsedObjects.get(array[i]);
          }
          else{
            idInArray[i] = String.valueOf(++id);
            parsedObjects.put(array[i], String.valueOf(id));
            queue.add(array[i]);
          }
        }
        currElement.appendChild(doc.createTextNode(Arrays.toString(idInArray)));
      } else {
        for (Field field : obj.getClass().getDeclaredFields()) {
          field.setAccessible(true);
          Type type = field.getType();
          String name = field.getName();
          String value = null;

          try {
            value = parseObjectValue(field, obj, queue);
          } catch (IllegalAccessException ignored) {
          }

          Element elem = doc.createElement(name);
          elem.setAttribute("type", type.toString());
          elem.appendChild(doc.createTextNode(value));
          currElement.appendChild(elem);
        }
      }
      if (isSteam) {
        stream.appendChild(createLink(obj, doc));
        isSteam = false;
      }
      pull.appendChild(currElement);

    } while (queue.size() > 0);
  }

  private Element createLink(Object obj, Document doc){
    Element elem = doc.createElement("link");
    elem.setAttribute("id", parsedObjects.get(obj));
    return elem;
  }

  private String parseObjectValue(Field field, Object obj, Queue<Object> queue)
      throws IllegalAccessException {
    // if type is primitive or wrapper for primitive
    if (primAndWrappers.contains(field.getType().getTypeName())) {
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
    queue.add(field.get(obj));
    parsedObjects.put(field.get(obj), String.valueOf(id + 1));
    return String.valueOf(++id);
  }
}
