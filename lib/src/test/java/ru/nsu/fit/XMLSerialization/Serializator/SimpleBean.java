package ru.nsu.fit.XMLSerialization.Serializator;

public class SimpleBean {
    int vInt;
    double vDouble;
    String vString;
    Character vCharacter;

    public SimpleBean() {
        Counting.addOne(this.getClass());
    }

    public SimpleBean(int vInt, double vDouble, String vString, Character vCharacter) {
        this();
        this.vInt = vInt;
        this.vDouble = vDouble;
        this.vString = vString;
        this.vCharacter = vCharacter;
    }
}
