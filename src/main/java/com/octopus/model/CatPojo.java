package com.octopus.model;

/**
 * Plain Old Java Object (POJO) for Cat.
 * This is the business object that applications work with,
 * separate from the protobuf-generated classes.
 */
public class CatPojo {
    private String name;
    private String color;
    private int age;
    private boolean isIndoor;

    public CatPojo() {
    }

    public CatPojo(String name, String color, int age, boolean isIndoor) {
        this.name = name;
        this.color = color;
        this.age = age;
        this.isIndoor = isIndoor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isIndoor() {
        return isIndoor;
    }

    public void setIndoor(boolean indoor) {
        isIndoor = indoor;
    }

    @Override
    public String toString() {
        return "CatPojo{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", age=" + age +
                ", isIndoor=" + isIndoor +
                '}';
    }
}
