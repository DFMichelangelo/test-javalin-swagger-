package com.octopus.util.converter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.model.DogPojo;
import com.octopus.proto.AnimalProto.Dog;
import com.octopus.proto.AnimalProto.GenericWrapper;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between DogPojo (business object) and Dog protobuf message.
 * Handles serialization and deserialization using protocol builders.
 */
public class DogConverter implements ProtobufConverter<DogPojo, Dog> {

    @Override
    public byte[] serialize(DogPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("DogPojo cannot be null");
        }

        // Convert POJO to protobuf message
        Dog dogProto = Dog.newBuilder()
                .setName(pojo.getName())
                .setBreed(pojo.getBreed())
                .setAge(pojo.getAge())
                .setIsTrained(pojo.isTrained())
                .build();

        // Wrap in GenericWrapper using protocol builder
        GenericWrapper wrapper = GenericWrapper.newBuilder()
                .setDog(dogProto)
                .build();

        return wrapper.toByteArray();
    }

    @Override
    public DogPojo deserialize(byte[] data) throws InvalidProtocolBufferException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // Parse GenericWrapper and extract Dog protobuf message
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);

        if (!wrapper.hasDog()) {
            throw new InvalidProtocolBufferException("GenericWrapper does not contain a Dog");
        }

        Dog dogProto = wrapper.getDog();

        // Convert protobuf message to POJO
        DogPojo pojo = new DogPojo();
        pojo.setName(dogProto.getName());
        pojo.setBreed(dogProto.getBreed());
        pojo.setAge(dogProto.getAge());
        pojo.setTrained(dogProto.getIsTrained());

        return pojo;
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

