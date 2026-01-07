package com.octopus.model;

/**
 * Plain Old Java Object (POJO) for Dog.
 * This is the business object that applications work with,
 * separate from the protobuf-generated classes.
 */
public class DogPojo {
    private String name;
    private String breed;
    private int age;
    private boolean isTrained;

    public DogPojo() {
    }

    public DogPojo(String name, String breed, int age, boolean isTrained) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.isTrained = isTrained;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isTrained() {
        return isTrained;
    }

    public void setTrained(boolean trained) {
        isTrained = trained;
    }

    @Override
    public String toString() {
        return "DogPojo{" +
                "name='" + name + '\'' +
                ", breed='" + breed + '\'' +
                ", age=" + age +
                ", isTrained=" + isTrained +
                '}';
    }
}
