package ru.nsu.fit.XMLSerialization.Serializator;

public class TestClass {
    private final TestClass2 testClass20 = new TestClass2();
    Integer Wzuuuuh = 11;
    //TestClass2[] arrayOfObjects;

    TestClass() {
        testClass20.setTestClass(this);
    }

    TestClass(TestClass2[] array) {
        this();
        //arrayOfObjects = array;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestClass))
            return false;
        TestClass tc = (TestClass) obj;
        return testClass20.equals(tc.testClass20) && Wzuuuuh.equals(tc.Wzuuuuh);
    }
}
