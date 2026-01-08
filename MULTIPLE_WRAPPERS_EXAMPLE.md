# Using ProtoSerializer with Multiple Wrapper Types

## Overview

`ProtoSerializer` is now fully generic and can work with any protobuf wrapper type. This allows you to have multiple `.proto` files with different wrappers in the same project.

## Architecture

The key components are:

1. **`ProtoSerializer<W>`** - Generic serializer parameterized by wrapper type
2. **`WrapperParser<W>`** - Strategy interface for parsing wrapper-specific logic
3. **Concrete WrapperParser implementations** - One per `.proto` wrapper type

## Example: Two Different Wrapper Types

### 1. Animals Wrapper (animals.proto)

```protobuf
message GenericWrapper {
  oneof payload {
    Dog dog = 1;
    Cat cat = 2;
    Bird bird = 3;
  }
}
```

### 2. Vehicles Wrapper (vehicles.proto)

```protobuf
message VehicleWrapper {
  oneof payload {
    Car car = 1;
    Truck truck = 2;
    Motorcycle motorcycle = 3;
  }
}
```

## Usage

### Step 1: Create WrapperParser for each wrapper type

**AnimalWrapperParser.java:**
```java
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
            case PAYLOAD_NOT_SET -> null;
        };
    }
}
```

**VehicleWrapperParser.java:**
```java
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
```

### Step 2: Create separate serializer instances

```java
// Animal serializer
ProtoSerializer<GenericWrapper> animalSerializer =
    new ProtoSerializer<>(new AnimalWrapperParser());
animalSerializer.registerConverter(new DogConverter());
animalSerializer.registerConverter(new CatConverter());
animalSerializer.registerConverter(new BirdConverter());

// Vehicle serializer
ProtoSerializer<VehicleWrapper> vehicleSerializer =
    new ProtoSerializer<>(new VehicleWrapperParser());
vehicleSerializer.registerConverter(new CarConverter());
vehicleSerializer.registerConverter(new TruckConverter());
vehicleSerializer.registerConverter(new MotorcycleConverter());
```

### Step 3: Use the serializers

```java
// Serialize animals
DogPojo dog = new DogPojo("Max", "Golden Retriever", 3, true);
byte[] dogBytes = animalSerializer.serialize(dog);

// Deserialize animals - type automatically detected
Object result = animalSerializer.deserialize(dogBytes);
if (result instanceof DogPojo deserializedDog) {
    // Use the dog
}

// Serialize vehicles
CarPojo car = new CarPojo("Toyota", "Camry", 2024, "Blue");
byte[] carBytes = vehicleSerializer.serialize(car);

// Deserialize vehicles - type automatically detected
Object carResult = vehicleSerializer.deserialize(carBytes);
if (carResult instanceof CarPojo deserializedCar) {
    // Use the car
}
```

## Type Safety

The API is simple and automatic:

### Serialize
```java
DogPojo dog = new DogPojo("Max", "Golden Retriever", 3, true);
byte[] data = serializer.serialize(dog);  // Type inferred from object
```

### Deserialize
```java
Object result = serializer.deserialize(data);  // Type automatically detected from wrapper
if (result instanceof DogPojo dog) {
    // Type-safe extraction using pattern matching
}
```

Both serialize and deserialize infer types automatically:
- **Serialize**: Type inferred from the object's class
- **Deserialize**: Type detected from the wrapper's oneof field

## Benefits

1. **Multiple wrapper types** - Each `.proto` file can have its own wrapper
2. **Type safety** - Generic methods provide compile-time type checking
3. **Clean separation** - Each domain (animals, vehicles) has its own serializer
4. **Easy to extend** - Just create a new WrapperParser for new `.proto` files
5. **No wrapper coupling** - ProtoSerializer doesn't hardcode any specific wrapper

## Migration from Old API

**Old (static methods):**
```java
ProtoSerializer.registerConverter(new DogConverter());
byte[] data = ProtoSerializer.serialize(dog);
Object pojo = ProtoSerializer.deserialize(data);
```

**New (instance-based, generic, automatic):**
```java
ProtoSerializer<GenericWrapper> serializer = new ProtoSerializer<>(new AnimalWrapperParser());
serializer.registerConverter(new DogConverter());
byte[] data = serializer.serialize(dog);
Object pojo = serializer.deserialize(data);
// Use instanceof for type-safe extraction
if (pojo instanceof DogPojo dog) {
    // Work with dog
}
```

The new API:
- Uses instance methods instead of static methods
- Accepts any wrapper type via the `WrapperParser` parameter
- Automatically infers types during serialization from the object
- Automatically detects types during deserialization from the wrapper's oneof field
- No explicit Class parameters needed anywhere!
