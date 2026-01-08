package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic protobuf serialization/deserialization with automatic type detection.
 * Works with POJOs (Plain Old Java Objects) and converts them to/from protobuf format.
 * Uses the wrapper's oneof field to determine message type.
 *
 * This class is parameterized by wrapper type (W), allowing it to work with any .proto wrapper.
 *
 * Usage:
 * 1. Create serializer: ProtoSerializer<GenericWrapper> serializer = new ProtoSerializer<>(new AnimalWrapperParser());
 * 2. Register converters: serializer.registerConverter(new DogConverter());
 * 3. Serialize: byte[] data = serializer.serialize(dogPojo);
 * 4. Deserialize: DogPojo dog = (DogPojo) serializer.deserialize(data);
 *
 * @param <W> the wrapper message type (e.g., GenericWrapper, VehicleWrapper)
 */
public class ProtoSerializer<W extends Message> {

    private final WrapperParser<W> wrapperParser;
    private final Map<Class<?>, ProtobufConverter<?, ? extends Message>> convertersByPojoClass = new HashMap<>();
    private final Map<Class<? extends Message>, ProtobufConverter<?, ? extends Message>> convertersByMessageClass = new HashMap<>();

    /**
     * Creates a new ProtoSerializer for a specific wrapper type.
     *
     * @param wrapperParser the parser that knows how to handle the wrapper type
     */
    public ProtoSerializer(WrapperParser<W> wrapperParser) {
        if (wrapperParser == null) {
            throw new IllegalArgumentException("WrapperParser cannot be null");
        }
        this.wrapperParser = wrapperParser;
    }

    /**
     * Registers a protobuf converter that handles POJO to/from protobuf conversion.
     *
     * @param converter the protobuf converter to register
     * @param <P>       the POJO type
     * @param <M>       the protobuf Message type
     */
    public <P, M extends Message> void registerConverter(ProtobufConverter<P, M> converter) {
        if (converter == null) {
            throw new IllegalArgumentException("Converter cannot be null");
        }

        convertersByPojoClass.put(converter.getPojoClass(), converter);
        convertersByMessageClass.put(converter.getMessageClass(), converter);
    }

    /**
     * Serializes a POJO to byte array.
     * Uses the registered converter to convert POJO to protobuf message,
     * wrap in the wrapper type, and serialize.
     *
     * @param pojo the POJO (business object) to serialize
     * @param <T>  the POJO type
     * @return byte array representation of the wrapper
     */
    public <T> byte[] serialize(T pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("POJO cannot be null");
        }

        ProtobufConverter<?, ? extends Message> converter = convertersByPojoClass.get(pojo.getClass());

        if (converter == null) {
            throw new IllegalArgumentException(
                    "Type not registered: " + pojo.getClass().getName() +
                    ". Use registerConverter() to register this type.");
        }

        ProtobufConverter<T, ? extends Message> typedConverter = castConverter(converter);
        return typedConverter.serialize(pojo);
    }

    /**
     * Deserializes a byte array by automatically detecting the type from the wrapper.
     * Reads the wrapper's oneof field to determine which type it contains,
     * then uses the appropriate converter to return the POJO.
     *
     * @param data the byte array to deserialize (wrapper bytes)
     * @return the deserialized POJO (business object)
     * @throws InvalidProtocolBufferException if deserialization fails
     */
    public Object deserialize(byte[] data) throws InvalidProtocolBufferException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        // Parse the wrapper to determine the actual type
        W wrapper = wrapperParser.parseWrapper(data);
        Class<? extends Message> messageClass = wrapperParser.getPayloadMessageClass(wrapper);

        if (messageClass == null) {
            throw new InvalidProtocolBufferException("Wrapper has no payload set");
        }

        // Get converter for the message type
        ProtobufConverter<?, ? extends Message> converter = convertersByMessageClass.get(messageClass);

        if (converter == null) {
            throw new IllegalArgumentException(
                    "No converter registered for message type: " + messageClass.getName());
        }

        return converter.deserialize(data);
    }

    /**
     * Checks if a converter is registered for the given POJO class.
     *
     * @param pojoClass the POJO class to check
     * @return true if a converter is registered
     */
    public boolean isConverterRegistered(Class<?> pojoClass) {
        return convertersByPojoClass.containsKey(pojoClass);
    }

    /**
     * Gets the registered converter for a specific POJO type.
     *
     * @param pojoClass the POJO class
     * @param <P>       the POJO type
     * @return the registered converter, or null if not registered
     */
    public <P> ProtobufConverter<P, ? extends Message> getConverter(Class<P> pojoClass) {
        ProtobufConverter<?, ? extends Message> converter = convertersByPojoClass.get(pojoClass);
        if (converter == null) {
            return null;
        }
        return castConverter(converter);
    }

    /**
     * Clears all registered converters.
     */
    public void clearRegistry() {
        convertersByPojoClass.clear();
        convertersByMessageClass.clear();
    }

    /**
     * Gets the number of registered converters.
     *
     * @return the number of registered converters
     */
    public int getRegistrySize() {
        return convertersByPojoClass.size();
    }

    @SuppressWarnings("unchecked")
    private <T> ProtobufConverter<T, ? extends Message> castConverter(
            ProtobufConverter<?, ? extends Message> converter) {
        // This cast is safe because:
        // 1. Converters are registered with a specific POJO class via getPojoClass()
        // 2. We only retrieve converters using that same class as the key
        // 3. The type system guarantees the converter handles that POJO type
        return (ProtobufConverter<T, ? extends Message>) converter;
    }
}

