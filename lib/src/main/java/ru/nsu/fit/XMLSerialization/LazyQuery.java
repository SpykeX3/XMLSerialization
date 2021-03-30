package ru.nsu.fit.XMLSerialization;

import com.google.common.annotations.Beta;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;


import javax.annotation.Nullable;
import java.io.InvalidClassException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Beta
class LazyQuery<T> {
    private final XMLDeserializer parentDeserializer;
    private final Multimap<String, String> fieldsUsedInClasses = HashMultimap.create();
    private final Queue<String> methodQueue = new ArrayDeque<>();
    private final Set<String> processedMethods = new HashSet<>();
    private final String desiredClass;
    private final Predicate<T> predicate;
    private final Set<Integer> initializedIds = new HashSet<>();

    LazyQuery(XMLDeserializer caller, Predicate<T> predicate, Class<T> clazz) {
        this.predicate = predicate;
        this.parentDeserializer = caller;
        Method test = Arrays.stream(predicate.getClass().getDeclaredMethods())
                .filter(m -> m.getName().equals("test"))
                .findFirst().orElseThrow(() -> new RuntimeException("No test method found"));
        this.desiredClass = clazz.getName();
        deepDependencyLookup(desiredClass, test);
    }

    void printDebugInfo() {
        System.out.println("Fields used:");
        fieldsUsedInClasses.asMap().forEach((key, value) -> {
            System.out.println("From " + key + ":");
            value.forEach(System.out::println);
            System.out.println();
        });

    }

    //TODO fix name collisions
    private void getFields(String expectedClassName, Method method) {
        getFields(method.getDeclaringClass().getName(), method.getName(), null);
    }

    private void getFields(String className, String methodName, @Nullable String signature) {
        ClassPool cPool = ClassPool.getDefault();
        cPool.insertClassPath(new ClassClassPath(this.getClass()));
        CtMethod test;
        try {
            CtClass ctClass = cPool.get(className);
            if (signature == null) {
                test = Arrays.stream(ctClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals(methodName))
                        .findFirst().orElseThrow(() -> new RuntimeException("No such method found:" + methodName + " in class " + ctClass.getName()));
            } else {
                test = ctClass.getMethod(methodName, signature);
            }

        } catch (
                NotFoundException e) {
            throw new RuntimeException(e);
        }
        Objects.requireNonNull(test);

        try {
            test.instrument(new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (!f.isStatic()) {
                        fieldsUsedInClasses.put("class " + f.getClassName(), f.getFieldName());
                    }
                    super.edit(f);
                }

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    String id = m.getClassName() + " " + m.getMethodName() + " " + m.getSignature();
                    if (!processedMethods.contains(id)) {
                        methodQueue.add(id);
                        processedMethods.add(id);
                    }
                    super.edit(m);
                }
            });
        } catch (
                CannotCompileException e) {
            throw new RuntimeException(e);
        }

    }

    private void deepDependencyLookup(String expectedClassName, Method method) {
        getFields(expectedClassName, method);
        while (!methodQueue.isEmpty()) {
            String mid = methodQueue.poll();
            processedMethods.add(mid);
            String[] identifiers = mid.split(" ");
            //System.out.println(mid);
            getFields(identifiers[0], identifiers[1],identifiers[2]);
        }
    }

    boolean isSubtype(String target, String current) {
        if (target.equals(current)) return true;
        if (target.startsWith("class ")) target = target.replace("class ", "");
        if (current.startsWith("class ")) current = current.replace("class ", "");
        try {
            Class<?> targetClass = Class.forName(target);
            Class<?> currentClass = Class.forName(current);
            return targetClass.isAssignableFrom(currentClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void lazyInit(int id) throws InvalidClassException, ClassNotFoundException {
        initializedIds.add(id);
        Map<String, Integer> fieldIds = parentDeserializer.getCompositeFields(id);
        Set<String> serializeNext = new HashSet<>(fieldsUsedInClasses.get(parentDeserializer.getBeanTypeName(id)));
        parentDeserializer.deserializeObjectFields(id, serializeNext);
        String type = parentDeserializer.getBeanTypeName(id);
        if (PrimitiveTypes.isObjectArray(type)) {
            for (Integer valueId : parentDeserializer.aFiller.getObjectArrayContent(id)) {
                lazyInit(valueId);
            }
            return;
        }
        for (Map.Entry<String, Integer> entry : fieldIds.entrySet()) {
            if (!serializeNext.contains(entry.getKey()) || initializedIds.contains(entry.getValue())) continue;
            lazyInit(entry.getValue());
        }
    }

    List<Integer> findIDs() throws InvalidClassException, ClassNotFoundException {
        Set<String> subtypes = parentDeserializer.getObjectIDStream().stream()
                .map(parentDeserializer::getBeanTypeName)
                .filter(type -> isSubtype(desiredClass, type))
                .collect(Collectors.toSet());

        List<Integer> candidates = parentDeserializer.getObjectIDStream().stream()
                .filter(id -> subtypes.contains(parentDeserializer.getBeanTypeName(id)))
                .collect(Collectors.toList());
        for (Integer candidate : candidates) {
            lazyInit(candidate);
        }
        return candidates.stream()
                .filter(id -> predicate.test((T) parentDeserializer.getDeserializedObject(id)))
                .collect(Collectors.toList());
    }
}
