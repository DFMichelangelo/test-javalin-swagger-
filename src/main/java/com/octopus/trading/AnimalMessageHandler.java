package com.octopus.trading;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.proto.AnimalProto.*;

public class AnimalMessageHandler {

    public void handleMessage(byte[] data) throws InvalidProtocolBufferException {
        AnimalMessage msg = AnimalMessage.parseFrom(data);

        switch (msg.getAnimalTypeCase()) {
            case DOG -> processDog(msg.getDog());
            case CAT -> processCat(msg.getCat());
            case BIRD -> processBird(msg.getBird());
            case FISH -> processFish(msg.getFish());
            case ANIMALTYPE_NOT_SET ->
                    throw new IllegalStateException("Empty message");
        }
    }

    private void processDog(Dog dog) {
        System.out.printf("Dog - Name: %s, Breed: %s, Age: %d, Trained: %s%n",
                dog.getName(),
                dog.getBreed(),
                dog.getAge(),
                dog.getIsTrained() ? "Yes" : "No");
    }

    private void processCat(Cat cat) {
        System.out.printf("Cat - Name: %s, Color: %s, Age: %d, Indoor: %s%n",
                cat.getName(),
                cat.getColor(),
                cat.getAge(),
                cat.getIsIndoor() ? "Yes" : "No");
    }

    private void processBird(Bird bird) {
        System.out.printf("Bird - Name: %s, Species: %s, Can Fly: %s, Wingspan: %.1f cm%n",
                bird.getName(),
                bird.getSpecies(),
                bird.getCanFly() ? "Yes" : "No",
                bird.getWingspanCm());
    }

    private void processFish(Fish fish) {
        System.out.printf("Fish - Name: %s, Type: %s, Length: %.1f cm, Water: %s%n",
                fish.getName(),
                fish.getType(),
                fish.getLengthCm(),
                fish.getWaterType());
    }
}
