package com.octopus.util.converter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.model.FishPojo;
import com.octopus.proto.AnimalProto.Fish;
import com.octopus.proto.AnimalProto.GenericWrapper;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between FishPojo (business object) and Fish protobuf message.
 * Handles serialization and deserialization using protocol builders.
 */
public class FishConverter implements ProtobufConverter<FishPojo, Fish> {

    @Override
    public byte[] serialize(FishPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("FishPojo cannot be null");
        }

        // Convert POJO to protobuf message
        Fish fishProto = Fish.newBuilder()
                .setName(pojo.getName())
                .setType(pojo.getType())
                .setLengthCm(pojo.getLengthCm())
                .setWaterType(pojo.getWaterType())
                .build();

        // Wrap in GenericWrapper using protocol builder
        GenericWrapper wrapper = GenericWrapper.newBuilder()
                .setFish(fishProto)
                .build();

        return wrapper.toByteArray();
    }

    @Override
    public FishPojo deserialize(byte[] data) throws InvalidProtocolBufferException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // Parse GenericWrapper and extract Fish protobuf message
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);

        if (!wrapper.hasFish()) {
            throw new InvalidProtocolBufferException("GenericWrapper does not contain a Fish");
        }

        Fish fishProto = wrapper.getFish();

        // Convert protobuf message to POJO
        FishPojo pojo = new FishPojo();
        pojo.setName(fishProto.getName());
        pojo.setType(fishProto.getType());
        pojo.setLengthCm(fishProto.getLengthCm());
        pojo.setWaterType(fishProto.getWaterType());

        return pojo;
    }

    @Override
    public Class<FishPojo> getPojoClass() {
        return FishPojo.class;
    }

    @Override
    public Class<Fish> getMessageClass() {
        return Fish.class;
    }
}

