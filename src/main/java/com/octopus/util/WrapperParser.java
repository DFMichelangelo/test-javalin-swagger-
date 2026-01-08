package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Strategy interface for parsing wrapper messages and determining which converter to use.
 * Each .proto wrapper type (e.g., GenericWrapper, VehicleWrapper) needs its own parser.
 *
 * @param <W> the wrapper message type
 */
public interface WrapperParser<W extends Message> {

    /**
     * Parses the wrapper from byte array.
     * Equivalent to: WrapperType.parseFrom(data)
     *
     * @param data the byte array to parse
     * @return the parsed wrapper message
     * @throws InvalidProtocolBufferException if parsing fails
     */
    W parseWrapper(byte[] data) throws InvalidProtocolBufferException;

    /**
     * Determines which message class is contained in this wrapper.
     * This is used to look up the appropriate converter.
     *
     * @param wrapper the wrapper message
     * @return the message class contained in the wrapper, or null if no payload is set
     */
    Class<? extends Message> getPayloadMessageClass(W wrapper);
}
