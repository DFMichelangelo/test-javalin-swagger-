package com.octopus.util.converter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.model.BirdPojo;
import com.octopus.proto.AnimalProto.Bird;
import com.octopus.proto.AnimalProto.GenericWrapper;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between BirdPojo (business object) and Bird protobuf message.
 * Handles serialization and deserialization using protocol builders.
 */
public class BirdConverter implements ProtobufConverter<BirdPojo, Bird> {

    @Override
    public byte[] serialize(BirdPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("BirdPojo cannot be null");
        }

        // Convert POJO to protobuf message
        Bird birdProto = Bird.newBuilder()
                .setName(pojo.getName())
                .setSpecies(pojo.getSpecies())
                .setCanFly(pojo.isCanFly())
                .setWingspanCm(pojo.getWingspanCm())
                .build();

        // Wrap in GenericWrapper using protocol builder
        GenericWrapper wrapper = GenericWrapper.newBuilder()
                .setBird(birdProto)
                .build();

        return wrapper.toByteArray();
    }

    @Override
    public BirdPojo deserialize(byte[] data) throws InvalidProtocolBufferException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // Parse GenericWrapper and extract Bird protobuf message
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);

        if (!wrapper.hasBird()) {
            throw new InvalidProtocolBufferException("GenericWrapper does not contain a Bird");
        }

        Bird birdProto = wrapper.getBird();

        // Convert protobuf message to POJO
        BirdPojo pojo = new BirdPojo();
        pojo.setName(birdProto.getName());
        pojo.setSpecies(birdProto.getSpecies());
        pojo.setCanFly(birdProto.getCanFly());
        pojo.setWingspanCm(birdProto.getWingspanCm());

        return pojo;
    }

    @Override
    public Class<BirdPojo> getPojoClass() {
        return BirdPojo.class;
    }

    @Override
    public Class<Bird> getMessageClass() {
        return Bird.class;
    }
}

