package ru.nsu.fit.XMLSerialization;

import java.io.OutputStream;
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

  private static final Set<String> primAndWrappers =
      new HashSet<>(Arrays.asList(
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


  public void write(Object obj) throws NullPointerException{
    if (obj == null) throw new NullPointerException();
    queue.add(obj);
  }

  public void flush(OutputStream stream) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();

      Document doc = builder.newDocument();
      Element rootElement = doc.createElementNS("xml_stream", "XML_STEAM");

      Element objectPull = doc.createElement("Object_pool");
      Element objectSteam = doc.createElement("Object_stream");
      doc.appendChild(rootElement);
      rootElement.appendChild(objectSteam);
      rootElement.appendChild(objectPull);

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      Map<Object, Integer> parsedObjects = new IdentityHashMap<>();

      for (Object x : queue) {
        parseObject(doc, x, objectPull, objectSteam, parsedObjects);
      }

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      DOMSource source = new DOMSource(doc);

      StreamResult result = new StreamResult(stream);
      transformer.transform(source, result);

      queue = new ArrayDeque<>();
      id = 0;
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    }
  }

  private void parseObject(
      Document doc, Object obj, Element pull, Element stream, Map<Object, Integer> parsedObjects) {

    Queue<Object> queue = new ArrayDeque<>();
    queue.add(obj);
    boolean isSteam = true;

    do {
      obj = queue.remove();
      String className = obj.getClass().toString();
      Element currObject;
      if (parsedObjects.containsKey(obj)) {
        currObject = doc.createElement("id" + String.valueOf(parsedObjects.get(obj)));
      } else {
        currObject = doc.createElement("id" + String.valueOf(++id));
      }
      currObject.setAttribute("type", className);
      for (Field field : obj.getClass().getDeclaredFields()) {
        try {
          field.setAccessible(true);
          Type type = field.getType();
          String name = field.getName();
          String value = " ";

          if (primAndWrappers.contains(type.getTypeName())) {
            value = String.valueOf(field.get(obj));
          } else {
            if (!parsedObjects.containsKey(field.get(obj))) {
              if (field.get(obj) != null) {
                queue.add(field.get(obj));
                value = String.valueOf(++id);
              }
              else{
                value = "null";
              }
              parsedObjects.put(field.get(obj), id);
            } else {
              int currId = parsedObjects.get(field.get(obj));
              value = String.valueOf(currId);
            }
          }

          Element elem = doc.createElement(name);
          elem.setAttribute("type", type.toString());
          elem.appendChild(doc.createTextNode(value));
          currObject.appendChild(elem);

        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      if (isSteam) {
        stream.appendChild(currObject);
        isSteam = false;
      } else {
        pull.appendChild(currObject);
      }
    } while (queue.size() > 0);
  }
}
