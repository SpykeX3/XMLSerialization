package ru.nsu.fit.XMLSerialization;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;

import static org.junit.Assert.fail;

public class Sandbox {
    public static class DummyClass {

        static int A = 100500;

        String greeting;
        int a;
        int b;
        AnotherDummyClass obj;

        public DummyClass() {
            greeting = "Hi!";
            a = 1;
            b = 2;
            obj = new AnotherDummyClass();
        }

        public DummyClass(String greet) {
            greeting = greet;
            a = 1;
            b = 2;
            obj = null;
        }

        void sayHello() {
            System.out.println(greeting);
        }

        void increase() {
            a++;
            b++;
        }
    }

    public static class AnotherDummyClass {
        int c;
        int d;

        public AnotherDummyClass() {
            c = 1;
            d = 2;
        }

        boolean isEvenSum() {
            return ((c + d) & 1) == 0;
        }
    }

    public static DummyClass proxyTestClass() {
        MethodHandler handler = (self, overridden, forwarder, args) -> {
            if (overridden.getName().equals("sayHello")) {
                System.out.println("Hello from proxy!");
                forwarder.invoke(self, args);
                ((DummyClass) self).greeting = "123";
                forwarder.invoke(self, args);
                return null;
            }
            return forwarder.invoke(self, args);
        };

        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(DummyClass.class);
        try {
            return (DummyClass) factory.create(new Class[]{String.class}, new Object[]{"Oh, hello there!"}, handler);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }

    @Test
    public void testProxy() {
        var test = proxyTestClass();
        test.sayHello();
    }


    /*@Test
    public void testQueryFieldFinder() {
        AnotherDummyClass other = new AnotherDummyClass();
        QueryBuilder qb = new QueryBuilder( new Predicate<Object>() {
            @Override
            public boolean test(Object o) {
                DummyClass target = (DummyClass) o;
                target.increase();
                return outerFunction(target.a, target.b)
                        && target.greeting.equals("test_value") && target.a < 40 && target.b < other.c || target.obj.isEvenSum();
            }
        });
        qb.printDebugInfo();
    }

    private boolean outerFunction(int a, int b) {
        return a > b;
    }*/
}
