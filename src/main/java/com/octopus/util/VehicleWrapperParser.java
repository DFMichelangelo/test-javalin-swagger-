package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.octopus.proto.VehicleProto;
import com.octopus.proto.VehicleProto.VehicleWrapper;

/**
 * WrapperParser implementation for VehicleProto.VehicleWrapper.
 * Demonstrates how to create a serializer for a different .proto file.
 */
public class VehicleWrapperParser implements WrapperParser<VehicleWrapper> {

    @Override
    public VehicleWrapper parseWrapper(byte[] data) throws InvalidProtocolBufferException {
        return VehicleWrapper.parseFrom(data);
    }

    @Override
    public Class<? extends Message> getPayloadMessageClass(VehicleWrapper wrapper) {
        return switch (wrapper.getPayloadCase()) {
            case CAR -> VehicleProto.Car.class;
            case TRUCK -> VehicleProto.Truck.class;
            case MOTORCYCLE -> VehicleProto.Motorcycle.class;
            case PAYLOAD_NOT_SET -> null;
        };
    }
}
