package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Interface for converting between POJOs and protobuf messages.
 * Implement this interface to handle conversion between business objects (POJOs)
 * and protobuf wire format using protocol builders.
 *
 * @param <P> the POJO type (business object)
 * @param <M> the protobuf Message type
 */
public interface ProtobufConverter<P, M extends Message> {

    /**
     * Serializes a POJO to byte array by:
     * 1. Converting POJO to protobuf message
     * 2. Wrapping in GenericWrapper using protocol builders
     * 3. Serializing to bytes
     *
     * @param pojo the business object to serialize
     * @return byte array representation of the GenericWrapper
     */
    byte[] serialize(P pojo);

    /**
     * Deserializes byte array to POJO by:
     * 1. Parsing GenericWrapper from bytes
     * 2. Extracting protobuf message
     * 3. Converting protobuf message to POJO
     *
     * @param data the byte array to deserialize
     * @return the deserialized POJO
     * @throws InvalidProtocolBufferException if deserialization fails
     */
    P deserialize(byte[] data) throws InvalidProtocolBufferException;

    /**
     * Gets the POJO class this converter handles.
     *
     * @return the POJO class
     */
    Class<P> getPojoClass();

    /**
     * Gets the protobuf Message class this converter handles.
     *
     * @return the Message class
     */
    Class<M> getMessageClass();
}
