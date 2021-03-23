package ru.nsu.fit.XMLSerialization.ql;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.w3c.dom.Document;
import javassist.*;
import org.w3c.dom.Node;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class QueryBuilder {
    private final Map<Integer, Object> objectPool = new HashMap<>();
    private Multimap<String, String> fieldsUsedInClasses = HashMultimap.create();
    private Queue<String> methodQueue = new ArrayDeque<>();
    private Set<String> processedMethods = new HashSet<>();

    public QueryBuilder(Document doc, Class<?> expectedClass, Predicate<Object> predicate) {
        Method test = Arrays.stream(predicate.getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals("test"))
                .findFirst().orElseThrow(() -> new RuntimeException("No test method found"));
        deepDependencyLookup(expectedClass, test);
        /*try {
            //createProxy(null, expectedClass.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    public void printDebugInfo() {
        System.out.println("Fields used:");
        fieldsUsedInClasses.asMap().forEach((key, value) -> {
            System.out.println("From " + key + ":");
            value.forEach(System.out::println);
            System.out.println();
        });

    }

    //TODO fix name collisions
    private Set<String> getFields(Class<?> expectedClass, Method method) {
        return getFields(expectedClass.getName(), method.getDeclaringClass().getName(), method.getName());
    }

    private Set<String> getFields(String expectedClassName, String className, String methodName) {
        ClassPool cPool = ClassPool.getDefault();
        cPool.insertClassPath(new ClassClassPath(this.getClass()));
        CtMethod test;
        try {
            CtClass ctClass = cPool.get(className);
            test = Arrays.stream(ctClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst().orElseThrow(() -> new RuntimeException("No such method found:" + methodName + " in class " + ctClass.getName()));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        Objects.requireNonNull(test);

        try {
            test.instrument(new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (!f.isStatic()) {
                        fieldsUsedInClasses.put(f.getClassName(), f.getFieldName());
                    }
                    super.edit(f);
                }

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (!processedMethods.contains(m.getClassName() + " " + m.getMethodName() + " " + m.getSignature())) {
                        methodQueue.add(m.getClassName() + " " + m.getMethodName() + " " + m.getSignature());
                    }
                    super.edit(m);
                }
            });
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
        return new HashSet<>(fieldsUsedInClasses.get(expectedClassName));
    }

    private void deepDependencyLookup(Class<?> expectedClass, Method method) {
        getFields(expectedClass, method);
        while (!methodQueue.isEmpty()) {
            String mid = methodQueue.poll();
            processedMethods.add(mid);
            String[] identifiers = mid.split(" ");
            System.out.println(mid);
            getFields(identifiers[0], identifiers[0], identifiers[1]);
        }
    }

    private Object createProxy(Node object, String className) throws ClassNotFoundException {
        //for each field
        Class<?> clazz = Class.forName(className);
        MethodHandler handler = (self, overridden, forwarder, args) -> {
            // check that fields are loaded
            return forwarder.invoke(self, args);
        };

        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(clazz);
        try {
            Object proxy = factory.create(new Class[]{}, new Object[]{}, handler);

            for (String fieldName : fieldsUsedInClasses.get(className)) {
                System.out.println("Setting value for " + fieldName);
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                switch (f.getType().getTypeName()){
                    case "int":
                        f.set(proxy,100500);
                    default:
                        System.out.println(f.getType().getTypeName());
                        f.set(proxy, null);
                }
            }
            return clazz.cast(proxy);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}
