package ru.nsu.fit.XMLSerialization.Serializator;

import java.io.Serializable;


public class SimpleBean implements Serializable, Comparable{

    private static double delta = 0.1;
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleBean))
            return false;
        SimpleBean bean = (SimpleBean) obj;
        double diff = Math.abs(vDouble - bean.vDouble);
        return vInt == bean.vInt && diff < delta && vString.equals(bean.vString) && vCharacter.equals(bean.vCharacter);
    }

    @Override
    public String toString() {
        return "vInt=" + vInt + "\t" +
                "vDouble=" + vDouble + "\t" +
                "vString=" + vString + "\t" +
                "vCharacter=" + vCharacter;
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(vInt).hashCode();
    }


    @Override
    public int compareTo(Object o) {
        if (o instanceof SimpleBean){
            return vInt - ((SimpleBean) o).vInt;
        }
        return 0;
    }
}
