package com.octopus.util.converter;

import com.octopus.model.CatPojo;
import com.octopus.proto.AnimalProto.Cat;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between CatPojo (business object) and Cat protobuf message.
 * Only handles POJO <-> Message conversion. Wrapper handling is done by ProtoSerializer.
 */
public class CatConverter implements ProtobufConverter<CatPojo, Cat> {

    @Override
    public Cat toMessage(CatPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("CatPojo cannot be null");
        }

        return Cat.newBuilder()
                .setName(pojo.getName())
                .setColor(pojo.getColor())
                .setAge(pojo.getAge())
                .setIsIndoor(pojo.isIndoor())
                .build();
    }

    @Override
    public CatPojo fromMessage(Cat message) {
        if (message == null) {
            throw new IllegalArgumentException("Cat message cannot be null");
        }

        CatPojo pojo = new CatPojo();
        pojo.setName(message.getName());
        pojo.setColor(message.getColor());
        pojo.setAge(message.getAge());
        pojo.setIndoor(message.getIsIndoor());

        return pojo;
    }

    @Override
    public Class<CatPojo> getPojoClass() {
        return CatPojo.class;
    }

    @Override
    public Class<Cat> getMessageClass() {
        return Cat.class;
    }
}
