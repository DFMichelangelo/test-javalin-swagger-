package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.octopus.proto.VehicleProto;
import com.octopus.proto.VehicleProto.VehicleWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * WrapperParser implementation for VehicleProto.VehicleWrapper.
 * Demonstrates how to create a serializer for a different .proto file.
 */
public class VehicleWrapperParser implements WrapperParser<VehicleWrapper> {

    private final Map<Class<? extends Message>, Function<Message, VehicleWrapper>> wrappers = new HashMap<>();

    public VehicleWrapperParser() {
        // Register wrapping functions for each message type
        registerWrapper(VehicleProto.Car.class, msg ->
                VehicleWrapper.newBuilder().setCar((VehicleProto.Car) msg).build());

        registerWrapper(VehicleProto.Truck.class, msg ->
                VehicleWrapper.newBuilder().setTruck((VehicleProto.Truck) msg).build());

        registerWrapper(VehicleProto.Motorcycle.class, msg ->
                VehicleWrapper.newBuilder().setMotorcycle((VehicleProto.Motorcycle) msg).build());
    }

    private void registerWrapper(Class<? extends Message> messageClass,
                                  Function<Message, VehicleWrapper> wrapper) {
        wrappers.put(messageClass, wrapper);
    }

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

    @Override
    public Message getPayloadMessage(VehicleWrapper wrapper) {
        return switch (wrapper.getPayloadCase()) {
            case CAR -> wrapper.getCar();
            case TRUCK -> wrapper.getTruck();
            case MOTORCYCLE -> wrapper.getMotorcycle();
            case PAYLOAD_NOT_SET -> null;
        };
    }

    @Override
    public <M extends Message> VehicleWrapper wrapMessage(M message) {
        Function<Message, VehicleWrapper> wrapper = wrappers.get(message.getClass());

        if (wrapper == null) {
            throw new IllegalArgumentException(
                    "Unknown message type: " + message.getClass().getName());
        }

        return wrapper.apply(message);
    }
}
