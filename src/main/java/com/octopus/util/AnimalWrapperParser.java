package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.octopus.proto.AnimalProto;
import com.octopus.proto.AnimalProto.GenericWrapper;

/**
 * WrapperParser implementation for AnimalProto.GenericWrapper.
 * Handles parsing and payload type detection for animal-related messages.
 */
public class AnimalWrapperParser implements WrapperParser<GenericWrapper> {

    @Override
    public GenericWrapper parseWrapper(byte[] data) throws InvalidProtocolBufferException {
        return GenericWrapper.parseFrom(data);
    }

    @Override
    public Class<? extends Message> getPayloadMessageClass(GenericWrapper wrapper) {
        return switch (wrapper.getPayloadCase()) {
            case DOG -> AnimalProto.Dog.class;
            case CAT -> AnimalProto.Cat.class;
            case BIRD -> AnimalProto.Bird.class;
            case FISH -> AnimalProto.Fish.class;
            case ANIMAL_MESSAGE -> AnimalProto.AnimalMessage.class;
            case PAYLOAD_NOT_SET -> null;
        };
    }
}
