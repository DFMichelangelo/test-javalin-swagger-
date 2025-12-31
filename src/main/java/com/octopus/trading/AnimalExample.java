package com.octopus.trading;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.proto.AnimalProto.*;

public class AnimalExample {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        AnimalMessageHandler handler = new AnimalMessageHandler();

        // Esempio 1: Dog
        System.out.println("=== Esempio 1: Dog ===");
        AnimalMessage dogMsg = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setDog(Dog.newBuilder()
                        .setName("Max")
                        .setBreed("Golden Retriever")
                        .setAge(3)
                        .setIsTrained(true)
                        .build())
                .build();
        handler.handleMessage(dogMsg.toByteArray());

        // Esempio 2: Cat
        System.out.println("\n=== Esempio 2: Cat ===");
        AnimalMessage catMsg = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setCat(Cat.newBuilder()
                        .setName("Luna")
                        .setColor("Gray")
                        .setAge(2)
                        .setIsIndoor(true)
                        .build())
                .build();
        handler.handleMessage(catMsg.toByteArray());

        // Esempio 3: Bird
        System.out.println("\n=== Esempio 3: Bird ===");
        AnimalMessage birdMsg = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setBird(Bird.newBuilder()
                        .setName("Tweety")
                        .setSpecies("Canary")
                        .setCanFly(true)
                        .setWingspanCm(15.5)
                        .build())
                .build();
        handler.handleMessage(birdMsg.toByteArray());

        // Esempio 4: Fish
        System.out.println("\n=== Esempio 4: Fish ===");
        AnimalMessage fishMsg = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setFish(Fish.newBuilder()
                        .setName("Nemo")
                        .setType("Clownfish")
                        .setLengthCm(8.5)
                        .setWaterType("SALT")
                        .build())
                .build();
        handler.handleMessage(fishMsg.toByteArray());

        // Esempio 5: Controllo del tipo di animale
        System.out.println("\n=== Esempio 5: Controllo tipo animale ===");
        System.out.println("Tipo animale dogMsg: " + dogMsg.getAnimalTypeCase());
        System.out.println("Ha Dog? " + dogMsg.hasDog());
        System.out.println("Ha Cat? " + dogMsg.hasCat());

        // Esempio 6: Serializzazione e deserializzazione
        System.out.println("\n=== Esempio 6: Serializzazione ===");
        byte[] serialized = catMsg.toByteArray();
        System.out.println("Dimensione messaggio serializzato: " + serialized.length + " bytes");
        AnimalMessage deserialized = AnimalMessage.parseFrom(serialized);
        System.out.println("Cat deserializzato: " + deserialized.getCat().getName());
    }
}
