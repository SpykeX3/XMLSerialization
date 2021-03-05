package ru.nsu.fit.XMLSerialization;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLSerializator {
  private int id = 0;
  private static final List<String> primAndWrappers =
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
          "java.lang.String");

  public void createXMLDocument(Stream<Object> inputStream, StreamResult stream) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();

      Document doc = builder.newDocument();
      Element rootElement = doc.createElementNS("xml_steam", "XML_STEAM");

      Element objectPull = doc.createElement("Object_pull");
      Element objectSteam = doc.createElement("Object_steam");
      doc.appendChild(rootElement);
      rootElement.appendChild(objectSteam);
      rootElement.appendChild(objectPull);

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      Map<Object, Map.Entry<Integer, String>> parsedObjects = new IdentityHashMap<>();

      inputStream.forEach(x -> parseObject(doc, x, objectPull, objectSteam, parsedObjects));

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      DOMSource source = new DOMSource(doc);

      transformer.transform(source, stream);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void parseObject(
      Document doc,
      Object obj,
      Element pull,
      Element steam,
      Map<Object, Map.Entry<Integer, String>> parsedObjects) {

    Queue<Object> queue = new ArrayDeque<>();
    queue.add(obj);
    boolean isSteam = true;

    do {
      obj = queue.remove();
      String className = obj.getClass().toString().substring(6);
      className = className.replaceAll("\\$", "--subclass--");
      Element currObject = doc.createElement(className);
      if (parsedObjects.containsKey(obj)) {
        currObject.setAttribute("id", String.valueOf(parsedObjects.get(obj).getKey()));
      } else {
        currObject.setAttribute("id", String.valueOf(id));
      }
      for (Field field : obj.getClass().getDeclaredFields()) {
        try {
          field.setAccessible(true);
          Type type = field.getType();
          String name = field.getName();
          name = name.replaceAll("\\$", "--subclass--");
          String value = " ";

          if (primAndWrappers.contains(type.getTypeName())) {
            value = String.valueOf(field.get(obj));
          } else {
            if (!parsedObjects.containsKey(field.get(obj))) {
              value = String.valueOf(++id);
              queue.add(field.get(obj));
              parsedObjects.put(field.get(obj), Map.entry(id, ""));
            } else {
              Map.Entry<Integer, String> currObj = parsedObjects.get(field.get(obj));
              int currId = currObj.getKey();
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
        steam.appendChild(currObject);
        isSteam = false;
      } else {
        pull.appendChild(currObject);
      }
    } while (queue.size() > 0);
  }
}
