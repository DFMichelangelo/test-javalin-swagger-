package com.octopus.util.converter;

import com.octopus.model.FishPojo;
import com.octopus.proto.AnimalProto.Fish;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between FishPojo (business object) and Fish protobuf message.
 * Only handles POJO <-> Message conversion. Wrapper handling is done by ProtoSerializer.
 */
public class FishConverter implements ProtobufConverter<FishPojo, Fish> {

    @Override
    public Fish toMessage(FishPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("FishPojo cannot be null");
        }

        return Fish.newBuilder()
                .setName(pojo.getName())
                .setType(pojo.getType())
                .setLengthCm(pojo.getLengthCm())
                .setWaterType(pojo.getWaterType())
                .build();
    }

    @Override
    public FishPojo fromMessage(Fish message) {
        if (message == null) {
            throw new IllegalArgumentException("Fish message cannot be null");
        }

        return new FishPojo(
                message.getName(),
                message.getType(),
                message.getLengthCm(),
                message.getWaterType()
        );
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
