package ru.nsu.fit.XMLSerialization;

import com.google.common.annotations.Beta;
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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class XMLDeserializer {

    HashMap<Integer, Object> deserializedObjects = new HashMap<>();
    HashSet<Integer> deserializedIDs = new HashSet<>();
    HashMap<Integer, Node> objectPool = new HashMap<>();

    ArrayList<Integer> objectStream = new ArrayList<>();
    List<Object> cachedDeserializedObjects;

    ArrayFiller aFiller;

    /**
     * Create new XMLDeserializator. Parse document from input stream.
     *
     * @param inputStream - stream should be filled by XMLSerializator.
     */
    public XMLDeserializer(InputStream inputStream) {
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
            throw new RuntimeException(e);
        }

        aFiller = new ArrayFiller(this);
        doc.getDocumentElement().normalize();
        Node pool = doc.getElementsByTagName("Object_pool").item(0);
        Node stream = doc.getElementsByTagName("Object_stream").item(0);
        createXMLObjectPool(pool);
        createStreamIDList(stream);
    }

    /**
     * Method returns a list of objects from the document in the order in which they were written.
     *
     * @return list - ArrayList with deserialized objects.
     * @throws InvalidClassException  - if class from XMLDocument can't be created.
     * @throws ClassNotFoundException - if class from XMLDocument can't be allocated.
     */
    public List<Object> getDeserializedObjects() throws InvalidClassException, ClassNotFoundException {
        if (cachedDeserializedObjects != null) {
            return cachedDeserializedObjects;
        }
        deserializeObjects();
        List<Object> res = new ArrayList<>();
        for (Integer id : objectStream) {
            res.add(deserializedObjects.get(id));
        }
        cachedDeserializedObjects = res;
        return res;
    }

    private void deserializeObjects() throws InvalidClassException, ClassNotFoundException {
        for (Node node : objectPool.values()) {
            instantiateObject(node);
        }

        //System.out.println(Arrays.toString(deserializedObjects.entrySet().toArray()));
        for (Integer id : objectPool.keySet()) {
            deserializeObject(id);
        }
    }

    private void deserializeObject(int id) throws InvalidClassException, ClassNotFoundException {
        if (deserializedIDs.contains(id)) {
            return;
        }
        if (!deserializedObjects.containsKey(id)) {
            instantiateObject(objectPool.get(id));
        }
        deserializedIDs.add(id);
        deserializeObjectFields(id);
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

    /**
     * Deserialize all fields of an object by id.
     *
     * @param id identifier of the object in the object pool
     */
    void deserializeObjectFields(int id) throws InvalidClassException, ClassNotFoundException {
        Object parsedObject = deserializedObjects.get(id);
        Class<?> clazz = parsedObject.getClass();

        deserializeObjectFields(id, Arrays.stream(clazz.getDeclaredFields())
                .filter(df -> !java.lang.reflect.Modifier.isStatic(df.getModifiers()))
                .map(Field::getName).collect(Collectors.toSet()));
    }

    /**
     * Deserialize specified fields of an object by id.
     *
     * @param id         identifier of the object in the object pool
     * @param fieldNames set of field names to fill values
     */
    void deserializeObjectFields(int id, Set<String> fieldNames) throws InvalidClassException, ClassNotFoundException {
        Node bean = objectPool.get(id);
        if (!deserializedObjects.containsKey(id)) {
            instantiateObject(bean);
        }
        Object parsedObject = deserializedObjects.get(id);
        Class<?> clazz = parsedObject.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            String name = field.getName();

            if (!fieldNames.contains(name)) {
                continue;
            }
            field.setAccessible(true);
            // TODO: remove one loop. One would be enough
            for (int i = 0; i < bean.getChildNodes().getLength(); ++i) {
                if (bean.getChildNodes().item(i).getNodeName().equals(name)) {
                    Node fieldNode = bean.getChildNodes().item(i);
                    String fieldType = fieldNode.getAttributes().getNamedItem("type").getTextContent();
                    try {
                        if (fieldType.startsWith("class ")) {
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
            throws IllegalAccessException, InvalidClassException, ClassNotFoundException {

        if (bean.getTextContent().equals("null")) {
            field.set(parsedObject, null);
            return;
        }
        int fieldId = Integer.parseInt(bean.getTextContent());
        if (!deserializedObjects.containsKey(fieldId)) {
            instantiateObject(objectPool.get(fieldId));
        }
        field.set(parsedObject, deserializedObjects.get(fieldId));
    }


    void instantiateObject(Node bean) throws ClassNotFoundException, InvalidClassException {
        String type = getBeanTypeName(bean);
        int id = Integer.parseInt(bean.getAttributes().getNamedItem("id").getTextContent());
        if (deserializedObjects.containsKey(id))
            return;
        if (type.startsWith("class ")) {
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
            deserializedObjects.put(id, createdObject);
            aFiller.fillArray(id, bean);
            return;
        }
        if (clazz.equals(String.class)) {
            deserializedObjects.put(id, bean.getTextContent());
            return;
        }

        for (Constructor<?> cons : clazz.getConstructors()) {
            try {
                cons.setAccessible(true);
                if (cons.getParameterCount() == 0) {
                    createdObject = cons.newInstance();
                    deserializedObjects.put(id, createdObject);
                    return;
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
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
            deserializedObjects.put(id, createdObject);
        } catch (InstantiationException e) {
            throw new InvalidClassException(e.toString());
        }
    }

    List<Integer> getObjectIDStream() {
        return objectStream;
    }

    String getBeanTypeName(int id) {
        if (!objectPool.containsKey(id)) {
            throw new IndexOutOfBoundsException("No bean with id '" + id + "' was found");
        }
        return getBeanTypeName(objectPool.get(id));
    }

    Object getDeserializedObject(int id) {
        if (!deserializedObjects.containsKey(id)) {
            throw new IndexOutOfBoundsException("No bean with id '" + id + "' was found");
        }
        return deserializedObjects.get(id);
    }

    Map<String, Integer> getCompositeFields(int id) {
        Map<String, Integer> fieldTypes = new HashMap<>();
        Node node = objectPool.get(id);
        NodeList fieldList = node.getChildNodes();
        for (int i = 0; i < fieldList.getLength(); ++i) {
            Node field = fieldList.item(i);
            if (field.getAttributes() == null) continue;
            String clazz = field.getAttributes().getNamedItem("type").getTextContent();
            if (!PrimitiveTypes.isPrimitive(clazz)) {
                //TODO fix when arrays will be added
                fieldTypes.put(field.getNodeName(), Integer.valueOf(field.getTextContent()));
            }
        }
        return fieldTypes;
    }

    private String getBeanTypeName(Node bean) {
        return bean.getAttributes().getNamedItem("type").getTextContent();
    }

    @Beta
    public <T> List<T> filter(Predicate<T> predicate, Class<T> classType) throws InvalidClassException, ClassNotFoundException {
        LazyQuery<T> lazyQuery = new LazyQuery<>(this, predicate, classType);
        List<Integer> matched = lazyQuery.findIDs();
        for (Integer integer : matched) {
            smartDeserialize(integer);
        }
        return matched.stream()
                .map(id -> (T) deserializedObjects.get(id)).collect(Collectors.toList());
    }

    void smartDeserialize(int id) throws InvalidClassException, ClassNotFoundException {
        if (deserializedIDs.contains(id)) {
            return;
        }
        //TODO process arrays
        deserializeObject(id);
        if (PrimitiveTypes.isObjectArray(getBeanTypeName(id))) {
            aFiller.deserializeArrayChildren(id, objectPool.get(id));
        }
        for (Map.Entry<String, Integer> entry : getCompositeFields(id).entrySet()) {
            smartDeserialize(entry.getValue());
        }
    }
}
