package com.octopus.util.converter;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.model.CatPojo;
import com.octopus.proto.AnimalProto.Cat;
import com.octopus.proto.AnimalProto.GenericWrapper;
import com.octopus.util.ProtobufConverter;

/**
 * Converter between CatPojo (business object) and Cat protobuf message.
 * Handles serialization and deserialization using protocol builders.
 */
public class CatConverter implements ProtobufConverter<CatPojo, Cat> {

    @Override
    public byte[] serialize(CatPojo pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("CatPojo cannot be null");
        }

        // Convert POJO to protobuf message
        Cat catProto = Cat.newBuilder()
                .setName(pojo.getName())
                .setColor(pojo.getColor())
                .setAge(pojo.getAge())
                .setIsIndoor(pojo.isIndoor())
                .build();

        // Wrap in GenericWrapper uPortfolioOptimizer.iosing protocol builder
        GenericWrapper wrapper = GenericWrapper.newBuilder()
                .setCat(catProto)
                .build();

        return wrapper.toByteArray();
    }

    @Override
    public CatPojo deserialize(byte[] data) throws InvalidProtocolBufferException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // Parse GenericWrapper and extract Cat protobuf message
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);

        if (!wrapper.hasCat()) {
            throw new InvalidProtocolBufferException("GenericWrapper does not contain a Cat");
        }

        Cat catProto = wrapper.getCat();

        // Convert protobuf message to POJO
        CatPojo pojo = new CatPojo();
        pojo.setName(catProto.getName());
        pojo.setColor(catProto.getColor());
        pojo.setAge(catProto.getAge());
        pojo.setIndoor(catProto.getIsIndoor());

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

