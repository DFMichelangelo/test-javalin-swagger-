package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.octopus.proto.AnimalProto;
import com.octopus.proto.AnimalProto.GenericWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * WrapperParser implementation for AnimalProto.GenericWrapper.
 * Handles parsing, wrapping, and payload extraction for animal-related messages.
 */
public class AnimalWrapperParser implements WrapperParser<GenericWrapper> {

    private final Map<Class<? extends Message>, Function<Message, GenericWrapper>> wrappers = new HashMap<>();

    public AnimalWrapperParser() {
        // Register wrapping functions for each message type
        registerWrapper(AnimalProto.Dog.class, msg ->
                GenericWrapper.newBuilder().setDog((AnimalProto.Dog) msg).build());

        registerWrapper(AnimalProto.Cat.class, msg ->
                GenericWrapper.newBuilder().setCat((AnimalProto.Cat) msg).build());

        registerWrapper(AnimalProto.Bird.class, msg ->
                GenericWrapper.newBuilder().setBird((AnimalProto.Bird) msg).build());

        registerWrapper(AnimalProto.Fish.class, msg ->
                GenericWrapper.newBuilder().setFish((AnimalProto.Fish) msg).build());

        registerWrapper(AnimalProto.AnimalMessage.class, msg ->
                GenericWrapper.newBuilder().setAnimalMessage((AnimalProto.AnimalMessage) msg).build());
    }

    private void registerWrapper(Class<? extends Message> messageClass,
                                  Function<Message, GenericWrapper> wrapper) {
        wrappers.put(messageClass, wrapper);
    }

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

    @Override
    public Message getPayloadMessage(GenericWrapper wrapper) {
        return switch (wrapper.getPayloadCase()) {
            case DOG -> wrapper.getDog();
            case CAT -> wrapper.getCat();
            case BIRD -> wrapper.getBird();
            case FISH -> wrapper.getFish();
            case ANIMAL_MESSAGE -> wrapper.getAnimalMessage();
            case PAYLOAD_NOT_SET -> null;
        };
    }

    @Override
    public <M extends Message> GenericWrapper wrapMessage(M message) {
        Function<Message, GenericWrapper> wrapper = wrappers.get(message.getClass());

        if (wrapper == null) {
            throw new IllegalArgumentException(
                    "Unknown message type: " + message.getClass().getName());
        }

        return wrapper.apply(message);
    }
}
