package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.octopus.proto.AnimalProto.GenericWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple, fast protobuf serialization/deserialization with automatic type detection.
 * Works with POJOs (Plain Old Java Objects) and converts them to/from protobuf format.
 * Uses GenericWrapper's oneof field to determine message type.
 *
 * Usage:
 * 1. Register converters: ProtoSerializer.registerConverter(new DogConverter());
 * 2. Serialize: byte[] data = ProtoSerializer.serialize(dogPojo);
 * 3. Deserialize: DogPojo dog = (DogPojo) ProtoSerializer.deserialize(data);
 */
public class ProtoSerializer {

    private static final Map<Class<?>, ProtobufConverter<?, ? extends Message>> convertersByPojoClass = new HashMap<>();
    private static final Map<Class<? extends Message>, ProtobufConverter<?, ? extends Message>> convertersByMessageClass = new HashMap<>();

    /**
     * Registers a protobuf converter that handles POJO to/from protobuf conversion.
     *
     * @param converter the protobuf converter to register
     * @param <P>       the POJO type
     * @param <M>       the protobuf Message type
     */
    public static <P, M extends Message> void registerConverter(ProtobufConverter<P, M> converter) {
        if (converter == null) {
            throw new IllegalArgumentException("Converter cannot be null");
        }

        convertersByPojoClass.put(converter.getPojoClass(), converter);
        convertersByMessageClass.put(converter.getMessageClass(), converter);
    }

    /**
     * Serializes a POJO to byte array.
     * Uses the registered converter to convert POJO to protobuf message,
     * wrap in GenericWrapper, and serialize.
     *
     * @param pojo the POJO (business object) to serialize
     * @return byte array representation of the GenericWrapper
     */
    public static byte[] serialize(Object pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("POJO cannot be null");
        }

        @SuppressWarnings("unchecked")
        ProtobufConverter<Object, ? extends Message> converter =
            (ProtobufConverter<Object, ? extends Message>) convertersByPojoClass.get(pojo.getClass());

        if (converter == null) {
            throw new IllegalArgumentException(
                    "Type not registered: " + pojo.getClass().getName() +
                    ". Use registerConverter() to register this type.");
        }

        return converter.serialize(pojo);
    }

    /**
     * Automatically deserializes a byte array without specifying the type.
     * Reads the GenericWrapper's oneof field to determine which type it contains,
     * then uses the appropriate converter to return a POJO.
     *
     * @param data the byte array to deserialize (GenericWrapper bytes)
     * @return the deserialized POJO (business object)
     * @throws InvalidProtocolBufferException if deserialization fails
     */
    public static Object deserialize(byte[] data) throws InvalidProtocolBufferException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // First, determine which converter to use by parsing the wrapper
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);

        // Get the appropriate converter based on the oneof field
        ProtobufConverter<?, ? extends Message> converter = switch (wrapper.getPayloadCase()) {
            case DOG -> convertersByMessageClass.get(com.octopus.proto.AnimalProto.Dog.class);
            case CAT -> convertersByMessageClass.get(com.octopus.proto.AnimalProto.Cat.class);
            case BIRD -> convertersByMessageClass.get(com.octopus.proto.AnimalProto.Bird.class);
            case FISH -> convertersByMessageClass.get(com.octopus.proto.AnimalProto.Fish.class);
            case ANIMAL_MESSAGE -> convertersByMessageClass.get(com.octopus.proto.AnimalProto.AnimalMessage.class);
            case PAYLOAD_NOT_SET -> throw new InvalidProtocolBufferException(
                    "GenericWrapper has no payload set");
        };

        if (converter == null) {
            throw new IllegalArgumentException(
                    "No converter registered for payload type: " + wrapper.getPayloadCase());
        }

        // Use the converter to deserialize to POJO
        return converter.deserialize(data);
    }

    /**
     * Checks if a converter is registered for the given POJO class.
     *
     * @param pojoClass the POJO class to check
     * @return true if a converter is registered
     */
    public static boolean isConverterRegistered(Class<?> pojoClass) {
        return convertersByPojoClass.containsKey(pojoClass);
    }

    /**
     * Gets the registered converter for a specific POJO type.
     *
     * @param pojoClass the POJO class
     * @param <P>       the POJO type
     * @param <M>       the protobuf Message type
     * @return the registered converter, or null if not registered
     */
    @SuppressWarnings("unchecked")
    public static <P, M extends Message> ProtobufConverter<P, M> getConverter(Class<P> pojoClass) {
        return (ProtobufConverter<P, M>) convertersByPojoClass.get(pojoClass);
    }

    /**
     * Clears all registered converters.
     */
    public static void clearRegistry() {
        convertersByPojoClass.clear();
        convertersByMessageClass.clear();
    }

    /**
     * Gets the number of registered converters.
     *
     * @return the number of registered converters
     */
    public static int getRegistrySize() {
        return convertersByPojoClass.size();
    }
}

