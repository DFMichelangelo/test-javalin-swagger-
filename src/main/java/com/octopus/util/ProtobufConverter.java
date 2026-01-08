package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Interface for converting between POJOs and protobuf messages.
 * Converters only handle POJO <-> Message conversion.
 * Wrapper handling is done by ProtoSerializer.
 *
 * @param <P> the POJO type (business object)
 * @param <M> the protobuf Message type
 */
public interface ProtobufConverter<P, M extends Message> {

    /**
     * Converts a POJO to a protobuf message.
     *
     * @param pojo the business object to convert
     * @return the protobuf message
     */
    M toMessage(P pojo);

    /**
     * Converts a protobuf message to a POJO.
     *
     * @param message the protobuf message to convert
     * @return the deserialized POJO
     */
    P fromMessage(M message);

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
