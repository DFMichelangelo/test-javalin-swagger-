package com.octopus.util.converter;

import com.octopus.model.DogPojo;
import com.octopus.proto.AnimalProto.Dog;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between DogPojo (business object) and Dog protobuf message.
 * Only handles POJO <-> Message conversion. Wrapper handling is done by ProtoSerializer.
 */
public class DogConverter implements ProtobufConverter<DogPojo, Dog> {

    @Override
    public Dog toMessage(DogPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("DogPojo cannot be null");
        }

        return Dog.newBuilder()
                .setName(pojo.getName())
                .setBreed(pojo.getBreed())
                .setAge(pojo.getAge())
                .setIsTrained(pojo.isTrained())
                .build();
    }

    @Override
    public DogPojo fromMessage(Dog message) {
        if (message == null) {
            throw new IllegalArgumentException("Dog message cannot be null");
        }

        return new DogPojo(
                message.getName(),
                message.getBreed(),
                message.getAge(),
                message.getIsTrained()
        );
    }

    @Override
    public Class<DogPojo> getPojoClass() {
        return DogPojo.class;
    }

    @Override
    public Class<Dog> getMessageClass() {
        return Dog.class;
    }
}

