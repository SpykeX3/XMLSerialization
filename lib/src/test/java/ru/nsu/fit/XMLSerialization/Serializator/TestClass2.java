package ru.nsu.fit.XMLSerialization.Serializator;


import java.util.ArrayList;
import java.util.List;

public class TestClass2 {
    private List<Integer> la;
    private TestClass testClass;
    private String string = "lallala";

    public TestClass2() {
        la = new ArrayList<>();
    }

    public void setTestClass(TestClass testClass) {
        la.add(5);
        la.add(4);
        this.testClass = testClass;
    }

    public void setString(String str) {
        string = str;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestClass2))
            return false;
        TestClass2 other = (TestClass2) obj;
        boolean b = string.equals(other.string) && testClass.equals(other.testClass);
        return b;
    }
}