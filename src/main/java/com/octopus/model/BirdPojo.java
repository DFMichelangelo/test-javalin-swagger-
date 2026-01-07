package com.octopus.model;

/**
 * Plain Old Java Object (POJO) for Bird.
 * This is the business object that applications work with,
 * separate from the protobuf-generated classes.
 */
public class BirdPojo {
    private String name;
    private String species;
    private boolean canFly;
    private double wingspanCm;

    public BirdPojo() {
    }

    public BirdPojo(String name, String species, boolean canFly, double wingspanCm) {
        this.name = name;
        this.species = species;
        this.canFly = canFly;
        this.wingspanCm = wingspanCm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public boolean isCanFly() {
        return canFly;
    }

    public void setCanFly(boolean canFly) {
        this.canFly = canFly;
    }

    public double getWingspanCm() {
        return wingspanCm;
    }

    public void setWingspanCm(double wingspanCm) {
        this.wingspanCm = wingspanCm;
    }

    @Override
    public String toString() {
        return "BirdPojo{" +
                "name='" + name + '\'' +
                ", species='" + species + '\'' +
                ", canFly=" + canFly +
                ", wingspanCm=" + wingspanCm +
                '}';
    }
}
