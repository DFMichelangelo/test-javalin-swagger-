package com.octopus.model;

/**
 * Plain Old Java Object (POJO) for Fish.
 * This is the business object that applications work with,
 * separate from the protobuf-generated classes.
 */
public class FishPojo {
    private String name;
    private String type;
    private double lengthCm;
    private String waterType;

    public FishPojo() {
    }

    public FishPojo(String name, String type, double lengthCm, String waterType) {
        this.name = name;
        this.type = type;
        this.lengthCm = lengthCm;
        this.waterType = waterType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(double lengthCm) {
        this.lengthCm = lengthCm;
    }

    public String getWaterType() {
        return waterType;
    }

    public void setWaterType(String waterType) {
        this.waterType = waterType;
    }

    @Override
    public String toString() {
        return "FishPojo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", lengthCm=" + lengthCm +
                ", waterType='" + waterType + '\'' +
                '}';
    }
}
