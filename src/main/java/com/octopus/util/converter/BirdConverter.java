package com.octopus.util.converter;

import com.octopus.model.BirdPojo;
import com.octopus.proto.AnimalProto.Bird;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between BirdPojo (business object) and Bird protobuf message.
 * Only handles POJO <-> Message conversion. Wrapper handling is done by ProtoSerializer.
 */
public class BirdConverter implements ProtobufConverter<BirdPojo, Bird> {

    @Override
    public Bird toMessage(BirdPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("BirdPojo cannot be null");
        }

        return Bird.newBuilder()
                .setName(pojo.getName())
                .setSpecies(pojo.getSpecies())
                .setCanFly(pojo.isCanFly())
                .setWingspanCm(pojo.getWingspanCm())
                .build();
    }

    @Override
    public BirdPojo fromMessage(Bird message) {
        if (message == null) {
            throw new IllegalArgumentException("Bird message cannot be null");
        }

        BirdPojo pojo = new BirdPojo();
        pojo.setName(message.getName());
        pojo.setSpecies(message.getSpecies());
        pojo.setCanFly(message.getCanFly());
        pojo.setWingspanCm(message.getWingspanCm());

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
